package com.unciv.json

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.CivConstructions
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.city.City
import com.unciv.logic.city.CityConstructions
import com.unciv.logic.city.CityStats
import com.unciv.logic.city.managers.CityEspionageManager
import com.unciv.logic.city.managers.CityExpansionManager
import com.unciv.logic.city.managers.CityPopulationManager
import com.unciv.logic.city.managers.CityReligionManager
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.mapunit.MapUnitCache
import com.unciv.logic.map.mapunit.UnitPromotions
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.automation.civilization.BarbarianEncampment
import com.unciv.logic.automation.civilization.BarbarianManager
import com.unciv.logic.civilization.ExploredRegion
import com.unciv.logic.civilization.diplomacy.DiplomacyManager
import com.unciv.logic.civilization.managers.EspionageManager
import com.unciv.logic.civilization.managers.GoldenAgeManager
import com.unciv.logic.civilization.managers.GreatPersonManager
import com.unciv.logic.civilization.managers.PolicyManager
import com.unciv.logic.civilization.managers.ReligionManager
import com.unciv.logic.civilization.managers.RuinsManager
import com.unciv.logic.civilization.managers.TechManager
import com.unciv.logic.civilization.managers.VictoryManager
import com.unciv.logic.civilization.managers.quests.QuestManager
import com.unciv.models.Religion
import com.unciv.models.Spy
import com.unciv.models.metadata.GameSettings
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.metadata.Player
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.nation.Nation
import com.unciv.models.ruleset.tech.TechColumn
import com.unciv.models.ruleset.tech.Technology
import com.unciv.models.ruleset.tile.Terrain
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.platform.PlatformCapabilities
import java.lang.reflect.Modifier

/** TeaVM-only repairs for GDX reflection metadata and nested ruleset data. */
object WebJsonFallback {
    fun markTransientFields(json: Json) {
        val graphTypes = listOf(
            GameInfo::class.java, BarbarianEncampment::class.java, BarbarianManager::class.java,
            City::class.java, CityConstructions::class.java, CityStats::class.java,
            CityEspionageManager::class.java, CityExpansionManager::class.java,
            CityPopulationManager::class.java, CityReligionManager::class.java,
            CivConstructions::class.java, Civilization::class.java, DiplomacyManager::class.java,
            ExploredRegion::class.java, EspionageManager::class.java, GoldenAgeManager::class.java,
            GreatPersonManager::class.java, PolicyManager::class.java, QuestManager::class.java,
            ReligionManager::class.java, RuinsManager::class.java, TechManager::class.java,
            VictoryManager::class.java, MapUnit::class.java, MapUnitCache::class.java,
            UnitPromotions::class.java, Tile::class.java, TileMap::class.java,
            GameSettings::class.java, GameSetupInfo::class.java, Player::class.java,
            Religion::class.java, Nation::class.java, Terrain::class.java, BaseUnit::class.java,
            Spy::class.java,
        )
        for (type in graphTypes) runCatching {
            for (field in type.declaredFields) {
                if (Modifier.isTransient(field.modifiers)) {
                    runCatching { json.setDeprecated(type, field.name, true) }
                }
            }
        }
    }

    class Vector2Serializer : Json.Serializer<Vector2> {
        override fun write(json: Json, vector: Vector2, knownType: Class<*>?) {
            json.writeObjectStart()
            json.writeValue("x", vector.x, Float::class.java)
            json.writeValue("y", vector.y, Float::class.java)
            json.writeObjectEnd()
        }

        override fun read(json: Json, jsonData: com.badlogic.gdx.utils.JsonValue, type: Class<*>?): Vector2 {
            fun coordinate(name: String): Float {
                val value = jsonData.get(name) ?: return 0f
                return runCatching { (value.get("value") ?: value).asFloat() }.getOrDefault(0f)
            }
            return Vector2(coordinate("x"), coordinate("y"))
        }
    }

    /** TeamVM can lose nested Technology objects while reading TechColumn[]. */
    fun hydrateRulesetTechs(ruleset: Ruleset, file: FileHandle) {
        if (PlatformCapabilities.current.backgroundThreadPools || ruleset.technologies.isNotEmpty()) return
        val root = runCatching { JsonReader().parse(file) }.getOrNull() ?: return
        ruleset.techColumns.clear()
        var columnNode = root.child
        while (columnNode != null) {
            val column = TechColumn().apply {
                columnNumber = columnNode.getInt("columnNumber", 0)
                era = columnNode.getString("era", "")
                techCost = columnNode.getInt("techCost", 0)
                buildingCost = columnNode.getInt("buildingCost", -1)
                wonderCost = columnNode.getInt("wonderCost", -1)
            }
            var techNode = columnNode.get("techs")?.child
            while (techNode != null) {
                val technology = Technology().apply {
                    name = techNode.getString("name", "")
                    cost = techNode.getInt("cost", 0).let { if (it == 0) column.techCost else it }
                    row = techNode.getInt("row", 0)
                    quote = techNode.getString("quote", "")
                    originRuleset = ruleset.name
                }
                technology.column = column
                readStrings(techNode.get("prerequisites"), technology.prerequisites)
                readStrings(techNode.get("uniques"), technology.uniques)
                column.techs += technology
                if (technology.name.isNotBlank()) ruleset.technologies[technology.name] = technology
                techNode = techNode.next
            }
            ruleset.techColumns += column
            columnNode = columnNode.next
        }
    }

    private fun readStrings(node: com.badlogic.gdx.utils.JsonValue?, target: MutableCollection<String>) {
        var value = node?.child
        while (value != null) {
            value.asString().takeIf { it.isNotBlank() }?.let { target += it }
            value = value.next
        }
    }
}
