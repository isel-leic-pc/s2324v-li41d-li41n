package pt.isel.pc.apps.echoserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.Socket

private val logger: Logger = LoggerFactory.getLogger("client")

private fun main() {
    val client = EchoClient(InetSocketAddress("127.0.0.1", 8080))
    val threads = mutableListOf<Thread>()
    repeat(20_000) {
        val thread = Thread.startVirtualThread {
            client.run()
        }
        threads.add(thread)
        Thread.sleep(1)
    }
    logger.info("Waiting for all threads to end.")
    threads.forEach {
        it.join()
    }
    logger.info("Client application is ending.")
}

class EchoClient(
    private val address: InetSocketAddress,
) {
    fun run() {
        Socket().use { socket ->
            socket.connect(address)
            socket.getInputStream().bufferedReader().use { reader ->
                socket.getOutputStream().bufferedWriter().use { writer ->
                    val welcomeLine = reader.readLine()
                    expect("welcome line") {
                        welcomeLine.startsWith("Hi! You are client number")
                    }
                    repeat(5) { counter ->
                        writer.writeLine("message $counter")
                        Thread.sleep(1000)
                        val echoLine = reader.readLine()
                        expect("echo line") {
                            echoLine.startsWith("$counter: MESSAGE $counter")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private fun expect(msg: String, predicate: () -> Boolean) {
            if (!predicate()) {
                println("Expectation failed: $msg")
            }
        }
    }
}