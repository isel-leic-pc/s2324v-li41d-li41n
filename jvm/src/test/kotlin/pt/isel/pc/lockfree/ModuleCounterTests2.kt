package pt.isel.pc.lockfree

import org.junit.jupiter.api.Assertions.assertTrue
import pt.isel.pc.sketches.leic51n.lockfree.ModuloCounter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ModuleCounterTests2 {

    @Test
    fun `stress test`() {
        val nOfThreads = 20
        val nOfReps = 100_000
        val modulo = 5
        val counter = ModuloCounter(modulo)
        val countDownLatch = CountDownLatch(nOfThreads)
        repeat(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) {
                    counter.incrementAndGet()
                }
                countDownLatch.countDown()
            }
        }
        while (!countDownLatch.await(0, TimeUnit.SECONDS)) {
            val observedCounter = counter.value
            assertTrue(observedCounter in 0..<modulo)
        }
        assertEquals(nOfThreads * nOfReps % modulo, counter.value)
    }
}