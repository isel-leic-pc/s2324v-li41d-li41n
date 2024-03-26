package pt.isel.pc.utils

import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias TestFunction = (Int, () -> Boolean) -> Unit

class TestHelper(
    duration: Duration,
) {

    private val deadline = Instant.now().plusMillis(duration.inWholeMilliseconds)

    private val failures = ConcurrentLinkedQueue<AssertionError>()
    private val errors = ConcurrentLinkedQueue<Exception>()
    private val threads = ConcurrentLinkedQueue<Thread>()

    private fun isDone() = Instant.now().isAfter(deadline)

    private fun createAndStart(index: Int, block: TestFunction) {
        val th = Thread {
            try {
                block(index, this::isDone)
            } catch (e: InterruptedException) {
                // ignore
            } catch (e: AssertionError) {
                failures.add(e)
            } catch (e: Exception) {
                errors.add(e)
            }
        }
        th.start()
        threads.add(th)
    }

    fun thread(block: () -> Unit): Thread {
        val th = Thread {
            try {
                block()
            } catch (e: InterruptedException) {
                // ignore
            } catch (e: AssertionError) {
                failures.add(e)
            } catch (e: Exception) {
                errors.add(e)
            }
        }
        th.start()
        threads.add(th)
        return th
    }

    fun createAndStartMultiple(nOfThreads: Int, block: TestFunction) =
        repeat(nOfThreads) { createAndStart(it, block) }

    @Throws(InterruptedException::class)
    fun join() {
        val deadlineForJoin = deadline.plusMillis(2000)
        for (th in threads) {
            val timeout = (deadlineForJoin.epochSecond - Instant.now().epochSecond).seconds
            th.join(timeout.inWholeMilliseconds)
            if (th.isAlive) {
                throw AssertionError("Thread '$th' did not end in the expected time")
            }
        }
        if (!failures.isEmpty()) {
            throw failures.peek()
        }
        if (!errors.isEmpty()) {
            throw errors.peek()
        }
    }
}