package pt.isel.pc.nio

import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import kotlin.test.Test

class AsynchronousSocketChannelLearningTests {

    @Test
    fun `using synchronous IO`() {
        try {
            val socket = Socket()
            val address = InetSocketAddress("httpbin.org", 80)
            socket.connect(address)
            val requestString = "GET /get HTTP/1.1\r\nHost:httpbin.org\r\n\r\n"
            val requestBytes = requestString.toByteArray()
            socket.getOutputStream().write(requestBytes)
            val responseBytes = ByteArray(1024)
            val readLen = socket.getInputStream().read(responseBytes, 0, responseBytes.size)
            val responseString = String(responseBytes, 0, readLen)
            println(responseString)
        } catch (ex: Throwable) {
            println(ex.message)
        }
    }

    @Test
    fun `using asynchronous IO`() {
        val socket = AsynchronousSocketChannel.open()
        val address = InetSocketAddress("httpbin.org", 80)
        val veryLastFuture = socket.connect2(address)
            .thenCompose {
                val requestString = "GET /get HTTP/1.1\r\nHost:httpbin.org\r\n\r\n"
                val requestByteBuffer = ByteBuffer.wrap(requestString.toByteArray())
                socket.write2(requestByteBuffer)
            }
            .thenCompose {
                val responseByteBuffer = ByteBuffer.allocate(1024)
                socket.read2(responseByteBuffer)
                    .thenApply { responseLen ->
                        val responseBytes = responseByteBuffer.array()
                        val responseString = String(responseBytes, 0, responseLen)
                        responseString
                    }
            }
            .handle { res, ex ->
                if (ex != null) {
                    println("Error: ${ex.message}")
                } else {
                    println(res)
                }
            }
        // waits for the future representing the complete pipeline to complete
        veryLastFuture.get()
    }
}