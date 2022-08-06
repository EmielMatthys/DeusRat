package com.example.deusrat.client

import android.system.ErrnoException
import com.example.deusrat.WolFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.lang.RuntimeException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.reflect.KSuspendFunction2

class TcpAuthenticator  {

    private val server = ServerData("192.168.0.220", 5432)

    fun connect(f: (String) -> Unit)
    {
        val executor = Executors.newFixedThreadPool(1)
        executor.execute {
            f(">Sending message to server: " + server.handshakeSend)
            var attempt = 50;
            var response = ""
            f(">Waiting for response...")
            while (attempt > 0)
            {
                try {
                    Socket(server.ip, server.port).use { client ->
                        client.outputStream.write(server.handshakeSend.toByteArray())
                        client.getInputStream().bufferedReader().use {
                            response = it.readText().substring(0..2) // Trailing zero-bytes are note trimmed for some reason.
                            f(">Received following response from server: $response responselength=${response.length}")
                            attempt = 0
                        }
                    }
                } catch (e: Exception) {
                    attempt--
                    Thread.sleep(100);
                }
            }
            if (response.startsWith(server.handshakeResponse)) {
                f(">Successfully logged in!.")
            }
            else {
                f(">Failed to log in. Response=$response")
            }
        }
    }
}