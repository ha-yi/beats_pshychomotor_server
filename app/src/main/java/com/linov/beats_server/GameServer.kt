package com.linov.beats_server

import android.util.Log.e
import androidx.lifecycle.MutableLiveData
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

/**
 * Created by Hayi Nukman at 2019-11-26
 * https://github.com/ha-yi
 */

class GameServer(
    val listener: (InetSocketAddress) -> Unit
) : WebSocketServer(InetSocketAddress(9999)) {

    val clients: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>()
    }


    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        conn?.send("connected")
        e("SOCKET", "Client connected ${conn?.remoteSocketAddress?.address?.hostAddress}")
        clients.postValue(connections.map { it.remoteSocketAddress.address.hostAddress })
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        clients.postValue(connections.map { it.remoteSocketAddress.address.hostAddress })
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        e("SOCKET", "message: $message")
        conn?.send("anda mengirim $message")

        // todo save data to firebase to prevent data loss..
    }

    override fun onStart() {
        e("SERVER", "server started....")
        listener.invoke(address)
        connectionLostTimeout = 100
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        e("WS", "error $ex")
    }
}