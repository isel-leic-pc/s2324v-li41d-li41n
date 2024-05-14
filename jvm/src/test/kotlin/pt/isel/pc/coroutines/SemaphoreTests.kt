package pt.isel.pc.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import pt.isel.pc.sketches.leic51d.coroutines.Semaphore3
import pt.isel.pc.sketches.leic51d.coroutines.Semaphore4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail

class SemaphoreTests {

    @Test
    fun `cancellation on fast path with incorrect implementation`() {
        assertFails {
            runBlocking {
                val semaphore = Semaphore3(1)
                this.cancel()
                try {
                    semaphore.acquire()
                    fail("acquire should end with exception")
                } catch (ex: CancellationException) {
                    logger.info("On catch, units are {}", semaphore.getCurrentUnits())
                    assertEquals(0, semaphore.getCurrentUnits())
                }
            }
        }
    }

    @Test
    fun `cancellation on fast path with correct implementation`() {
        assertFails {
            runBlocking {
                val semaphore = Semaphore4(1)
                this.cancel()
                try {
                    semaphore.acquire()
                    logger.info("After acquire, units are {}", semaphore.getCurrentUnits())
                    assertEquals(0, semaphore.getCurrentUnits())
                } catch (ex: CancellationException) {
                    fail("acquire should not throw exception")
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SemaphoreTests::class.java)
    }
}