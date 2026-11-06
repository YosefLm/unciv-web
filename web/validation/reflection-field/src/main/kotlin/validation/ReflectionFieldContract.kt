package validation

import com.badlogic.gdx.graphics.glutils.FileTextureData
import com.unciv.logic.map.mapgenerator.MapResourceSetting
import com.unciv.models.ruleset.unique.Countables
import com.unciv.models.ruleset.unique.DeprecatedUniqueType
import com.unciv.models.ruleset.unique.UniqueType

object ReflectionFieldContract {
    @JvmStatic
    fun main(args: Array<String>) {
        val data = FileTextureData(null, null, null, false)
        val pixmap = FileTextureData::class.java.getDeclaredField("pixmap")
        check(pixmap.name == "pixmap")
        pixmap.isAccessible = true
        check(pixmap.get(data) == null)
        pixmap.set(data, null)
        check(pixmap.get(data) == null)

        check(MapResourceSetting.activeLabels() == listOf("Sparse", "Default", "Abundant"))
        check(Countables.valueOf("CityStates").getDeprecationAnnotation() != null)
        check(DeprecatedUniqueType.valueOf("FoodConsumptionBySpecialists").getDeprecationAnnotation() != null)
        // Current UniqueType has no deprecated constants; its reflective lookup must return null.
        check(UniqueType.Stats.getDeprecationAnnotation() == null)
        println("Unciv reflective field contract passed")
    }
}
