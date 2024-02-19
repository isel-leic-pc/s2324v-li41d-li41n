package pt.isel.pc.apps.echoserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

private fun main() {
    EchoServer0SingleThreaded().run("0.0.0.0", 8080)
}

class EchoServer0SingleThreaded {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EchoServer0SingleThreaded::class.java)
        private const val EXIT_LINE = "exit"
    }

    fun run(address: String, port: Int) {
        ServerSocket().use { serverSocket ->
            serverSocket.bind(InetSocketAddress(address, port))
            logger.info("server socket bound to {}:{}", address, port)
            acceptLoop(serverSocket)
        }
    }

    private fun acceptLoop(serverSocket: ServerSocket) {
        while (true) {
            val socket = serverSocket.accept()
            logger.info("client socket accepted, remote address is {}", socket.inetAddress.hostAddress)
            echoLoop(socket)
        }
    }

    private fun echoLoop(socket: Socket) {
        val clientNo = newClientNumber()
        var lineNo = 0
        try {
            socket.use {
                socket.getInputStream().bufferedReader().use { reader ->
                    socket.getOutputStream().bufferedWriter().use { writer ->
                        writer.writeLine("Hi! You are client number %s", clientNo.toString())
                        while (true) {
                            val line = reader.readLine()
                            if (line == null || line == EXIT_LINE) {
                                writer.writeLine("Bye.")
                                socket.close()
                                return
                            }
                            logger.info(
                                "Received line '{}', echoing it back",
                                line
                            )
                            writer.writeLine("%d: %s", lineNo++, line.uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.warn("Connection ended with IO error: {}", e.message)
        }
    }

    private var clientNoCounter = 1
    private fun newClientNumber(): Int {
        val clientNo = clientNoCounter
        clientNoCounter += 1
        return clientNo
    }
}