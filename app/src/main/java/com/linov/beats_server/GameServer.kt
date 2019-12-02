package com.linov.beats_server

import android.util.Log.e
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import com.google.gson.reflect.TypeToken
import com.linov.beats_server.models.FirebaseHelper
import com.linov.beats_server.models.UserModel
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.*

/**
 * Created by Hayi Nukman at 2019-11-26
 * https://github.com/ha-yi
 */

class GameServer(
    val listener: (InetSocketAddress) -> Unit
) : WebSocketServer(InetSocketAddress(9999)) {
    val mapClient: MutableMap<String?, Clients> = mutableMapOf()

    val clients: MutableLiveData<List<Clients>> by lazy {
        MutableLiveData<List<Clients>>()
    }

    private var gson: Gson = GsonBuilder()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .create()

    var groupBoard: Board = Board()

    fun startNextTask(task: Int) {
        groupBoard = Board(taskId = task)
        broadcast(Gson().toJson(GameCommand(START_GROUP_TASK, task)))
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        conn?.send("connected")
        e("SOCKET", "Client connected ${conn?.remoteSocketAddress?.address?.hostAddress}")
        conn?.remoteSocketAddress?.address?.hostAddress?.let {
            mapClient[it] = Clients(
                it,
                "unknown",
                false,
                false
            )
        }

        updateClientLiveData()
    }

    private fun updateClientLiveData() {
        clients.postValue(mapClient.values.map { it })
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        e("WS", "closed ${conn?.remoteSocketAddress?.address?.hostAddress} $code $remote")
        conn?.remoteSocketAddress?.address?.hostAddress?.let {
            mapClient.remove(it)
        }
        updateClientLiveData()
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        e("SOCKET", "message: $message")
        val type = object : TypeToken<GameCommand<*>>() {}.type

        val from = conn?.remoteSocketAddress?.address?.hostAddress
        val data = gson.fromJson<GameCommand<*>>(message, type)
        data?.let {
            when (it.code) {
                ADD_USER_INFO -> saveUserInformation(gson.toJson(it.data), from)
                ADD_GAME_DATA -> {
                    addPersonalBoard(gson.toJson(it.data), from)
                }
                ADD_GROUP_TILE -> {
                    val tile = gson.fromJson<TileInfo>(gson.toJson(it.data), TileInfo::class.java)
                    val prepare = gson.toJson(GameCommand(ADD_GROUP_TILE, tile))
                    broadcast(prepare)
                    addGroupTile(tile, from)
                }
                ADD_GROUP_GAME_DATA -> {
                    var datastr = gson.toJson(it.data)
                    val board = gson.fromJson<Board>(datastr, Board::class.java)
                    var prepare = gson.toJson(GameCommand(ADD_GROUP_GAME_DATA, board))
                    broadcast(prepare)
                    addGroupBoard(gson.toJson(it.data), from)
                }

                READY_FOR_GROUP -> readyForGroup(from, true)
                NOT_READY_FOR_GROUP -> readyForGroup(from, false)
                ON_GROUP_BOARD -> onGroupBoard(from, true)
                LEAVE_GROUP_BOARD -> onGroupBoard(from, false)

                else -> {

                }
            }
        }
    }

    private fun addGroupTile(tile: TileInfo, from: String?) {
        groupBoard.add(tile)
        FirebaseHelper.saveBoardGroup(groupBoard)
    }

    private fun onGroupBoard(from: String?, ready: Boolean) {
        mapClient[from]?.onGroupBoard = ready
        mapClient[from]?.groupReady = false
        updateClientLiveData()
    }

    private fun readyForGroup(from: String?, ready: Boolean) {
        e("SERVER", " readyForGroup $from $ready")
        mapClient[from]?.groupReady = ready
        updateClientLiveData()
    }

    private fun addGroupBoard(data: String, from: String?) {
        val board = gson.fromJson<Board>(data, Board::class.java)
        FirebaseHelper.saveBoardGroup(board)
    }

    private fun addPersonalBoard(data: String, from: String?) {
        val board = gson.fromJson<Board>(data, Board::class.java)
        FirebaseHelper.saveBoardPersonal(from ?: "", board)
    }

    private fun saveUserInformation(data: String, hostAddress: String?) {
        e("SOCKET", "data: $data")
        val user = gson.fromJson<UserModel>(data, UserModel::class.java)
        FirebaseHelper.saveUser(hostAddress ?: "NA", user)
        mapClient[hostAddress]?.name = user?.name ?: ""
        updateClientLiveData()
    }

    override fun onStart() {
        e("SERVER", "server started....")
        listener.invoke(address)
        connectionLostTimeout = 100
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        e("WS", "error $ex")
    }

    fun sendTimeut() {
        val prepare = gson.toJson(GameCommand(ADD_GROUP_TIMEOUT, "-"))
        broadcast(prepare)
    }

    companion object {
        val serverUUID = UUID.randomUUID().toString()
    }
}


data class Clients(
    var ip: String,
    var name: String?,
    var isPersonal: Boolean,
    var groupReady: Boolean,
    var onGroupBoard: Boolean = false
)