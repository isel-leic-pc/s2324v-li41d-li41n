package pt.isel.pc.lockfree

import org.junit.jupiter.api.Assertions.assertEquals
import pt.isel.pc.sketches.leic51d.lockfree.ModuloCounter
import kotlin.test.Test

class ModuleCounterTests {

    @Test
    fun `stress test`() {
        val nOfThreads = 21L
        val nOfReps = 1234
        val counter = ModuloCounter(5)
        val ths = List(nOfThreads.toInt()) {
            Thread.ofPlatform().start {
                repeat(nOfReps) {
                    counter.incAndGetPreviousValue()
                }
            }
        }
        ths.forEach { it.join() }
        assertEquals(nOfThreads * nOfReps % 5, counter.get().toLong())
    }
}