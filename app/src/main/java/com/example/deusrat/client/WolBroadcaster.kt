package com.example.deusrat.client

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class WolBroadcaster(val log: (String) -> Unit, val callback: (Boolean) -> Unit) {

    private val server = InetSocketAddress("192.168.0.255", 9)


    companion object {
        private val executor = Executors.newFixedThreadPool(1)
        private val datagram = wolPacket("fc:34:97:14:92:b3")

        fun wolPacket(targetMac: String) : ByteArray {
            val ret = ByteArray(102) {0xFF.toByte()}
            val parsed = targetMac.split(':').map { Integer.parseInt(it, 16).toByte() }
            for (i in 0..95) ret[i+6] = parsed[i % 6]
            return ret
        }
    }

    fun connect() {
        executor.execute {
            try {
                connect0()
            } catch (e: Exception) {
                log(">Failed to send WOL packet")
                log(">Err: $e")
                callback(false)
                return@execute
            }
            log(">Successfully sent packet to broadcast address for MAC")
            callback(true)
        }
    }

    private fun connect0() {
        DatagramSocket().use { client ->
            client.broadcast = true
            client.connect(server)
            client.send(DatagramPacket(datagram, datagram.size))
        }
    }

}