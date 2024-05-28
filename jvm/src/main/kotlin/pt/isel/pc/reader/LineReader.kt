package pt.isel.pc.reader

import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CoderResult

/**
 * Provides a suspend `readLine` interface on top of a suspend function that read bytes
 */
class LineReader(
    // The underlying read function´
    bufferLength: Int = 1024,
    private val reader: suspend (ByteBuffer) -> Int,
) {
    private val byteBuffer = ByteBuffer.allocate(bufferLength)
    private val charBuffer = CharBuffer.allocate(bufferLength)
    private val decoder = Charsets.UTF_8.newDecoder()
    private val lineParser = LineParser()
    private var isEndOfInput: Boolean = false

    init {
        require(bufferLength > 1)
    }

    suspend fun readLine(): String? {
        while (true) {
            val maybeLine = lineParser.poll()
            if (maybeLine != null) {
                return maybeLine
            }
            if (isEndOfInput) {
                return null
            }
            val readLen = reader(byteBuffer)
            isEndOfInput = readLen == 0
            byteBuffer.flip()
            when (val decodingResult = decoder.decode(byteBuffer, charBuffer, isEndOfInput)) {
                CoderResult.UNDERFLOW -> {
                    // underflows are expected
                }

                else -> decodingResult.throwException()
            }
            byteBuffer.compact()
            if (byteBuffer.position() == byteBuffer.limit()) {
                throw IllegalStateException("Buffer length is not enough to decode.")
            }
            charBuffer.flip()
            lineParser.offer(charBuffer)
            charBuffer.clear()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LineReader::class.java)
    }
}