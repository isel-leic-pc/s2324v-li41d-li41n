package pt.isel.pc.nio

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.await
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

fun getHttpBin(path: String): String {
    // -- SO
    val socketChannel = AsynchronousSocketChannel.open()
    try {
        // For demo purposes, we set the send and receive buffers with a rather small values
        // This will mean that writing the request and reading the response will not be accomplished in a single operation
        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4)
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4)

        socketChannel.connect(InetSocketAddress("httpbin.org", 80))
            .get() // <--- Blocking

        // -- S1
        val requestString = "GET $path HTTP/1.1\r\nConnection:close\r\nHost:httpbin.org\r\n\r\n"
        val requestByteBuffer = ByteBuffer.wrap(requestString.toByteArray())
        var remainingBytesToWrite = requestByteBuffer.limit()
        while (remainingBytesToWrite != 0) {
            // -- S2
            val writtenBytesLen = socketChannel.write(requestByteBuffer)
                .get() // <--- Blocking
            // -- S3
            logger.info("Write {}", writtenBytesLen)
            remainingBytesToWrite -= writtenBytesLen
        }
        // -- S4
        val responseByteArray = ByteArray(8)
        val responseByteBuffer = ByteBuffer.wrap(responseByteArray)
        val responseBytes = ByteArrayOutputStream()
        while (true) {
            // -- S5
            val readBytesLen = socketChannel.read(responseByteBuffer)
                .get() // <--- Blocking
            // -- S6
            logger.info("Read {}", readBytesLen)
            if (readBytesLen < 0) {
                break
            }
            responseBytes.write(responseByteArray, 0, readBytesLen)
            responseByteBuffer.clear()
        }
        // -- S7
        val completeResponseArray = responseBytes.toByteArray()
        return String(completeResponseArray, 0, completeResponseArray.size)
    } finally {
        // Finally
        socketChannel.close()
    }
}

suspend fun suspendGetHttpBin(path: String): String {
    val socketChannel = AsynchronousSocketChannel.open()
    try {
        // For demo purposes, we set the send and receive buffers with a rather small values
        // This will mean that writing the request and reading the response will not be accomplished in a single operation
        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4)
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4)

        socketChannel.connect2(InetSocketAddress("httpbin.org", 80)).await()
        val requestString = "GET $path HTTP/1.1\r\nConnection:close\r\nHost:httpbin.org\r\n\r\n"
        val requestByteBuffer = ByteBuffer.wrap(requestString.toByteArray())
        var remainingBytesToWrite = requestByteBuffer.limit()
        while (remainingBytesToWrite != 0) {
            val writtenBytesLen = socketChannel.write2(requestByteBuffer).await()
            logger.info("Write {}", writtenBytesLen)
            remainingBytesToWrite -= writtenBytesLen
        }
        val responseByteArray = ByteArray(8)
        val responseByteBuffer = ByteBuffer.wrap(responseByteArray)
        val responseBytes = ByteArrayOutputStream()
        while (true) {
            val readBytesLen = socketChannel.read2(responseByteBuffer).await()
            logger.info("Read {}", readBytesLen)
            if (readBytesLen < 0) {
                break
            }
            responseBytes.write(responseByteArray, 0, readBytesLen)
            responseByteBuffer.clear()
        }
        val completeResponseArray = responseBytes.toByteArray()
        return String(completeResponseArray, 0, completeResponseArray.size)
    } finally {
        socketChannel.close()
    }
}

private val logger = LoggerFactory.getLogger("HttpRequest")