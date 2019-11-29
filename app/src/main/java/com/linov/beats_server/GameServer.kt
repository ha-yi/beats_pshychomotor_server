package com.linov.beats_server

import android.util.Log.e
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
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
        e("WS", "closed ${conn?.remoteSocketAddress?.address} $code $remote")
        val ips = connections.map { it.remoteSocketAddress.address.hostAddress }
        mapClient.keys.mapNotNull { it.takeIf { ips.contains(it) } }.forEach {
            mapClient.remove(it)
        }
        mapClient.remove(conn?.remoteSocketAddress?.address?.hostAddress)
        updateClientLiveData()
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        e("SOCKET", "message: $message")
        val type = object : TypeToken<GameCommand<*>>() {}.type

        val from = conn?.remoteSocketAddress?.address?.hostAddress
        val data = Gson().fromJson<GameCommand<*>>(message, type)
        data?.let {
            when (it.code) {
                ADD_USER_INFO -> saveUserInformation(Gson().toJson(it.data), from)
                ADD_GAME_DATA -> {
                    addPersonalBoard(Gson().toJson(it.data), from)
                }
                ADD_GROUP_GAME_DATA -> {
                    connections?.forEach {
                        if (it.remoteSocketAddress.address.hostAddress != from) {
                            it.send(message)
                        }
                    }
                    addGroupBoard(Gson().toJson(it.data), from)
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

    private fun onGroupBoard(from: String?, ready: Boolean) {
        mapClient[from]?.onGroupBoard = ready
        updateClientLiveData()
    }

    private fun readyForGroup(from: String?, ready: Boolean) {
        e("SERVER", " readyForGroup $from $ready")
        mapClient[from]?.groupReady = ready
        updateClientLiveData()
    }

    private fun addGroupBoard(data: String, from: String?) {
        val board = Gson().fromJson<Board>(data, Board::class.java)
        FirebaseHelper.saveBoardGroup(board)
    }

    private fun addPersonalBoard(data: String, from: String?) {
        val board = Gson().fromJson<Board>(data, Board::class.java)
        FirebaseHelper.saveBoardPersonal(from ?: "", board)
    }

    private fun saveUserInformation(data: String, hostAddress: String?) {
        e("SOCKET", "data: $data")
        val user = Gson().fromJson<UserModel>(data, UserModel::class.java)
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