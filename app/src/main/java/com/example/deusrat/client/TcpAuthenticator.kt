package com.example.deusrat.client

import android.system.ErrnoException
import com.example.deusrat.WolFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import kotlin.reflect.KSuspendFunction2

class TcpAuthenticator  {

    private val server = ServerData("192.168.0.220", 5432)
    private val TIMEOUT = 5000
    private val SLEEP   = 100L

    fun connect(f: (String) -> Unit)
    {
        val executor = Executors.newFixedThreadPool(1)
        executor.execute {
            f(">Sending message to server: " + server.handshakeSend)
            var attempt = TIMEOUT/ SLEEP.toInt()
            var response = ""
            f(">Waiting for response...")
            while (attempt > 0)
            {
                try {
                    Socket().use { client ->
                        client.connect(InetSocketAddress(server.ip, server.port), TIMEOUT)
                        client.outputStream.write(server.handshakeSend.toByteArray())
                        client.getInputStream().bufferedReader().use {
                            response = it.readText().substring(0..2) // Trailing zero-bytes are note trimmed for some reason.
                            f(">Received following response from server: $response responselength=${response.length}")
                            attempt = 0
                        }
                    }
                } catch(e: SocketTimeoutException) {
                    attempt = 0
                    f(">err: $e")
                } catch (e: Exception) {
                    attempt--
                    Thread.sleep(SLEEP)
                }
            }
            if (response.startsWith(server.handshakeResponse)) {
                f(">Successfully logged in!.")
            }
            else {
                f(">Failed to log in.")
            }
        }
    }
}