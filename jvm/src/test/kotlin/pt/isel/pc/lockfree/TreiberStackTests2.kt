package pt.isel.pc.lockfree

import org.junit.jupiter.api.Assertions.assertEquals
import pt.isel.pc.sketches.leic51n.lockfree.TreiberStack
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.fail

class TreiberStackTests2 {

    @Test
    fun `stress test`() {
        val nOfThreads = 12
        val nOfReps = 10_000
        val stack = TreiberStack<Int>()
        val queue = LinkedBlockingQueue<Int>()
        repeat(nOfThreads) { index ->
            Thread.ofPlatform().start {
                var counter = 0
                val threadValue = index + 1
                repeat(nOfReps) {
                    stack.push(threadValue)
                    counter += threadValue
                    stack.push(2 * threadValue)
                    counter += 2 * threadValue
                    val res = stack.pop()
                    if (res != null) {
                        counter -= res
                    }
                }
                queue.put(counter)
            }
        }
        var counterReportedByThreads = 0
        repeat(nOfThreads) {
            val threadCounter = queue.poll(60, TimeUnit.SECONDS)
                ?: fail("timeout")
            counterReportedByThreads += threadCounter
        }
        var counterInsideStack = 0
        while (true) {
            val item = stack.pop() ?: break
            counterInsideStack += item
        }
        assertEquals(counterReportedByThreads, counterInsideStack)
    }
}