package com.linov.beats_server

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log.e
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.linov.beats_server.models.FirebaseHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_input.view.*
import java.net.NetworkInterface
import java.util.*
import android.app.ActivityManager
import android.content.Context
import android.os.CountDownTimer
import android.view.*
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private val defaultIPAddr = "192.168.43.1"
    var groupTaskCounter = 0

    private val server by lazy {
        GameServer {
            runOnUiThread {
                renderIPAddress()
            }
        }
    }

    val timer = object : CountDownTimer(120000, 1000) {
        override fun onFinish() {
            server.sendTimeut()
        }

        override fun onTick(m: Long) {
            val minutes = m / 1000 / 60
            val seconds = m / 1000 % 60
            txtGroupGameStatus.text = "Sisa waktu pengerjaan:\n $minutes menit $seconds detik."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG)
        setContentView(R.layout.activity_main)

        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = SimpleAdapter()
        recycler.adapter = adapter

        server.clients.observe(this, androidx.lifecycle.Observer {
            adapter.updateItems(it)
            txtTerhubung.text = "${it.size} Client Terhubung"
            if (it.isEmpty()) {
                btnStartGroupTest.visibility = View.GONE
                btnStartNextTask.visibility = View.GONE
                txtGroupGameStatus.visibility = View.GONE
                return@Observer
            }
            if (it.map { it.groupReady }.all { it }) {
                btnStartGroupTest.visibility = View.VISIBLE
            } else {
                btnStartGroupTest.visibility = View.GONE
            }
            if (it.map { it.onGroupBoard }.all { it }) {
                btnStartNextTask.visibility = View.VISIBLE
                btnStartGroupTest.visibility = View.GONE
                txtGroupGameStatus.visibility = View.VISIBLE
            } else {
                btnStartNextTask.visibility = View.GONE
                txtGroupGameStatus.visibility = View.GONE
            }
        })

        btnShutdown.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tutup App")
                .setMessage("Menutup app akan memutuskan koneksi ke client.")
                .setPositiveButton("Tutup") { di, _ ->
                    di.dismiss()
                    finish()
                }.setNegativeButton("Batal") { di, _ ->
                    di.dismiss()
                }.show()
        }

        btnStartGroupTest.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Memulai Group Test")
                .setMessage("Pastikan semua peserta yang terhubung dalam session ini sudah siap untuk mengikuti group test.")
                .setPositiveButton("Mulai") { di, _ ->
                    groupTaskCounter = -1
                    server.broadcast(Gson().toJson(GameCommand(START_GROUP_GAME, "start")))
                    txtGroupGameStatus.text = "Siap untuk task baru."
                    di.dismiss()
                }.setNegativeButton("Batal") { di, _ ->
                    di.dismiss()
                }.show()
        }

        btnStartNextTask.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Jalankan Task Berikutnya")
                .setMessage("Pastikan semua peserta Sudah siap untuk mengerjakan task berikutnya.")
                .setPositiveButton("Mulai") { di, _ ->
                    if (groupTaskCounter <= 9) {
                        groupTaskCounter += 1
                        server.startNextTask(groupTaskCounter)
                    } else {
                        Snackbar.make(btnStartNextTask, "Task sudah habis", Snackbar.LENGTH_LONG).show()
                    }
                    if (groupTaskCounter < 10) {
                        startCountDown()
                    }
                    di.dismiss()
                }.setNegativeButton("Batal") { di, _ ->
                    di.dismiss()
                }.show()
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_input, null, false)

        val alert = AlertDialog.Builder(this)
            .setTitle("Masukkan ID Group")
            .setView(view)
            .setCancelable(false)
            .create()

        view.btnSimpan.setOnClickListener {
            if(view.inputIDGroup.text.toString().length < 4) {
                    view.inputIDGroup.error = "ID Group tidak boleh kurang dari 4"
                return@setOnClickListener
            }
            FirebaseHelper.serverID = view.inputIDGroup.text.toString()
            FirebaseHelper.testLoc = view.inputIDLokasi.text.toString()
            server.start()
            alert.dismiss()
        }
        alert.show()
    }

    private fun startCountDown() {
        timer.cancel()
        timer.start()
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
            FirebaseHelper.initializeServerData(it)
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

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Tutup?")
            .setMessage("Menutup aplikasi ini akan menjadikan server mati dan tidak dapat melakukan sinkronisasi data dengan client yang sudah terhubung. Yakin tutup??")
            .setPositiveButton("batal") { di, _ ->
                di.dismiss()
            }.setNegativeButton("tutup") { di, _ ->
                super.onBackPressed()
                di.dismiss()
            }.show()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onPause() {
        super.onPause()
        val activityManager = applicationContext
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.moveTaskToFront(taskId, 0)
    }
}
