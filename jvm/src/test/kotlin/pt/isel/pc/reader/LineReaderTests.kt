package pt.isel.pc.reader

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals

class LineReaderTests {

    @Test
    fun first() = runBlocking {
        val s = "Hello\n\rWorld\r\nOlá\r\rMundo\n\n\r\r"
        (1..s.length).forEach { step ->
            logger.info("Running for step = {}", step)
            val testReader = TestReader(s, step)
            val lineReader = LineReader(16) { testReader.read(it) }
            assertEquals("Hello", lineReader.readLine())
            assertEquals("World", lineReader.readLine())
            assertEquals("Olá", lineReader.readLine())
            assertEquals("", lineReader.readLine())
            assertEquals("Mundo", lineReader.readLine())
            assertEquals("", lineReader.readLine())
            assertEquals("", lineReader.readLine())
        }
    }

    class TestReader(
        inputString: String,
        private val step: Int,
    ) {
        init {
            require(step > 0)
        }

        private val bytes = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(inputString))

        suspend fun read(buf: ByteBuffer) = suspendCoroutine<Int> { continuation ->
            val len = minOf(step, bytes.remaining(), buf.remaining())
            repeat(len) {
                buf.put(bytes.get())
            }
            continuation.resume(len)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LineReaderTests::class.java)
    }
}