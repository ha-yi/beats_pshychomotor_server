package com.linov.beats_server


/**
 * Created by Hayi Nukman at 2019-11-26
 * https://github.com/ha-yi
 */

val CONNECTED = "connected"
val NEW_CLIENT = "new member"


data class GameData(
    val gameType: Int,
    val timestamp: Long,
    val uid: String
)


val ADD_USER_INFO = 1
val ADD_GAME_DATA = 2
val ADD_GROUP_GAME_DATA = 3
val START_PERSONAL_GAME = 5
val START_GROUP_GAME = 6
val START_PERSONAL_TASK = 7
val START_GROUP_TASK = 8

val READY_FOR_GROUP = 9
val NOT_READY_FOR_GROUP = 91
val ON_GROUP_BOARD = 92
val LEAVE_GROUP_BOARD = 93

data class GameCommand<T>(
    var code: Int,
    var data: T
)

data class TileInfo(
    var x: Int = 0,
    var y: Int = 0,
    var color: String = "W",
    var timestamp: Long = 0,
    var userID: String? = null
)

data class Board(
    var taskId: Int = 0,
    var col: Int = 20,
    var grid: List<List<TileInfo>> = listOf()
)


class BoardFirebase {
    var taskId: Int = 0
    var col: Int = 20
    var grid: MutableMap<String, List<TileInfo>> = mutableMapOf()

    companion object {
        fun fromBoard(b: Board): BoardFirebase {
            return BoardFirebase().apply {
                taskId = b.taskId
                col = b.col
                b.grid.forEachIndexed { index, list ->
                    this.grid[index.toString()] = list
                }
            }
        }
    }
}