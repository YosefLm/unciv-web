package com.unciv.logic.multiplayer

import com.badlogic.gdx.files.FileHandle
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.GameInfoPreview
import com.unciv.logic.event.EventBus
import com.unciv.utils.debug
import java.time.Instant

/**
 * Web-specific multiplayer cache keyed by path. TeaVM does not provide a
 * stable identity/hash implementation for every FileHandle instance, while
 * a path is the actual save key used by the web file backend.
 */
class MultiplayerFiles {
    internal val files = UncivGame.Current.files
    val savedGames: MutableMap<String, MultiplayerGamePreview> = mutableMapOf()

    private fun keyOf(fileHandle: FileHandle): String = fileHandle.path()

    fun updateSavesFromFiles() {
        val saves = files.getMultiplayerSaves().toList()
        val saveByKey = saves.associateBy { keyOf(it) }

        val removedKeys = savedGames.keys - saveByKey.keys
        for (key in removedKeys) savedGames.remove(key)

        val newSaves = saveByKey.filterKeys { it !in savedGames }.values
        for (saveFile in newSaves) addGame(saveFile)
    }

    fun deleteGame(multiplayerGamePreview: MultiplayerGamePreview) {
        deleteGame(multiplayerGamePreview.fileHandle)
    }

    private fun deleteGame(fileHandle: FileHandle) {
        files.deleteSave(fileHandle)
        val game = savedGames.remove(keyOf(fileHandle)) ?: return
        debug("Deleting game %s with id %s", fileHandle.name(), game.preview?.gameId)
    }

    fun addGame(newGame: GameInfo) {
        val newGamePreview = newGame.asPreview()
        addGame(newGamePreview, newGamePreview.gameId)
    }

    fun addGame(preview: GameInfoPreview, saveFileName: String) {
        val fileHandle = files.saveMultiplayerGamePreview(preview, saveFileName)
        addGame(fileHandle, preview)
    }

    private fun addGame(fileHandle: FileHandle, preview: GameInfoPreview? = null) {
        debug("Adding game %s", fileHandle.name())
        val game = MultiplayerGamePreview(fileHandle, preview, if (preview != null) Instant.now() else null)
        savedGames[keyOf(fileHandle)] = game
    }

    fun getGameByName(name: String): MultiplayerGamePreview? =
        savedGames.values.firstOrNull { it.name == name }

    fun getGameByGameId(gameId: String): MultiplayerGamePreview? =
        savedGames.values.firstOrNull { it.preview?.gameId == gameId }

    fun changeGameName(game: MultiplayerGamePreview, newName: String, onException: (Exception?) -> Unit) {
        debug("Changing game %s to %s", game.name, newName)
        val oldPreview = game.preview ?: throw game.error!!
        val oldLastUpdate = game.getLastUpdate()
        val oldName = game.name

        val newFileHandle = files.saveMultiplayerGamePreview(oldPreview, newName, onException)
        val newGame = MultiplayerGamePreview(newFileHandle, oldPreview, oldLastUpdate)
        savedGames[keyOf(newFileHandle)] = newGame

        savedGames.remove(keyOf(game.fileHandle))
        files.deleteSave(game.fileHandle)
        EventBus.send(MultiplayerGameNameChanged(oldName, newName))
    }
}
