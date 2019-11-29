package com.linov.beats_server.models

import com.google.firebase.firestore.FirebaseFirestore
import com.linov.beats_server.Board
import com.linov.beats_server.BoardFirebase
import com.linov.beats_server.GameServer
import java.util.*

object FirebaseHelper {
    var serverID: String = ""
    var testLoc: String = ""
    private var uuidStr = UUID.randomUUID().toString()

    private val daySession: String by lazy {
        Calendar.getInstance().let {
            "${it[Calendar.YEAR]}-${it[Calendar.MONTH]}-${it[Calendar.DAY_OF_MONTH]}"
        }
    }

    private fun db() = FirebaseFirestore.getInstance()
        .collection("beats_sessions")
        .document(daySession)
        .collection("servers")
        .document("$serverID-$uuidStr")

    fun initializeServerData(ip: String) {
        db().set(
            mapOf(
                "ip" to ip,
                "serverID" to serverID,
                "uuid" to uuidStr,
                "testLocation" to testLoc
            )
        )
    }

    fun saveUser(ip: String, userModel: UserModel) {
        db().collection("member")
            .document(ip)
            .set(userModel)
    }

    fun saveBoardPersonal(ip: String, board: Board) {
        db().collection("boards")
            .document(ip)
            .collection("tasks")
            .document(board.taskId.toString())
            .set(BoardFirebase.fromBoard(board))
    }

    fun saveBoardGroup(board: Board) {
        db().collection("boards")
            .document("GROUP")
            .collection("tasks")
            .document(board.taskId.toString())
            .set(BoardFirebase.fromBoard(board))
    }
}