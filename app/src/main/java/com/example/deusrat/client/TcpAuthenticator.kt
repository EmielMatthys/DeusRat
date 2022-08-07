package com.example.deusrat.client

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

class TcpAuthenticator  {

    private val server = ServerData("192.168.0.220", 5432)
    private val TIMEOUT = 5000
    private val SLEEP   = 10L
    private val executor = Executors.newFixedThreadPool(1)


    fun connect(log: (String) -> Unit, callback: (Boolean) -> Unit )
    {
        executor.execute {
            log(">Sending message to server: " + server.handshakeSend)
            var attempt = TIMEOUT/ SLEEP.toInt()
            var response = ""
            log(">Waiting for response...")
            while (attempt > 0)
            {
                try {
                    Socket().use { client ->
                        client.connect(InetSocketAddress(server.ip, server.port), TIMEOUT)
                        client.outputStream.write(server.handshakeSend.toByteArray())
                        client.getInputStream().bufferedReader().use {
                            response = it.readText().substring(0..2) // Trailing zero-bytes are note trimmed for some reason.
                            log(">Received response from server: $response")
                            attempt = 0
                        }
                    }
                } catch(e: SocketTimeoutException) {
                    attempt = 0
                    log(">err: $e")
                } catch (e: Exception) {
                    attempt--
                    Thread.sleep(SLEEP)
                }
            }
            if (response.startsWith(server.handshakeResponse)) {
                log(">Successfully logged in!.")
                callback(true)
            }
            else {
                log(">Failed to log in.")
                callback(false)
            }
        }
    }
}