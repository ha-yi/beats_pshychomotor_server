package com.linov.beats_server

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log.e
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import java.net.NetworkInterface
import java.util.*

class MainActivity : AppCompatActivity() {
    private val defaultIPAddr = "192.168.43.1"

    private val server by lazy {
        GameServer {
            renderIPAddress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        server.start()

        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = SimpleAdapter()
        recycler.adapter = adapter

        server.clients.observe(this, androidx.lifecycle.Observer {
            adapter.updateItems(it)
        })
    }

    private fun renderIPAddress() {
        val ip = getIPAddress().also {
            it.forEach {
                e("IP", it)
            }
        }
        val strIP = when (ip.size) {
            0 -> null
            1 -> ip.firstOrNull()
            else -> if (ip.contains(defaultIPAddr)) {
                defaultIPAddr
            } else {
                ip.firstOrNull()
            }
        }
        strIP?.let {
            txtIPAddress.text = it
            val generator = QRGEncoder(it, null, QRGContents.Type.TEXT, 500)
            val bitmap = generator.encodeAsBitmap()
            imgBarcode.setImageBitmap(bitmap)
        }
    }

    override fun onDestroy() {
        server.stop()
        super.onDestroy()
    }

    private fun getIPAddress(): List<String> {
        try {
            var interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            return interfaces.mapNotNull {
                it.inetAddresses.toList().mapNotNull {
                    if (it.isLoopbackAddress) null
                    else it.hostAddress.takeIf { !it.contains(":") && it.startsWith("192.") }
                }
            }.flatten()

        } catch (er: java.lang.Exception) {
            e("ERROR", er.toString())
        }
        return listOf()
    }
}
