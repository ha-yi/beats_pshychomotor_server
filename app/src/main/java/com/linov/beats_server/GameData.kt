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

