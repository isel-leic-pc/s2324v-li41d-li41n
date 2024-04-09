package pt.isel.pc.lockfree

import org.junit.jupiter.api.Assertions.assertEquals
import pt.isel.pc.sketches.leic51d.lockfree.TreiberStack
import kotlin.test.Test

class TreiberStackTests {

    @Test
    fun `stress test`() {
        val nOfThreads = 20
        val nOfInsertions = 100_000
        val stack = TreiberStack<Int>()
        val ths = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfInsertions) {
                    stack.push(0)
                }
            }
        }
        ths.forEach { it.join() }
        var counter = 0
        while (stack.pop() != null) {
            counter += 1
        }
        assertEquals(nOfThreads * nOfInsertions, counter)
    }
}