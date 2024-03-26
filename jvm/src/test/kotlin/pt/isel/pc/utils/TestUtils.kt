package pt.isel.pc.utils

import java.time.Instant
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun spinUntilTimedWait(th: Thread, timeout: Duration = 1.seconds) {
    val deadline = Instant.now().plusMillis(timeout.inWholeMilliseconds)
    while (th.state != Thread.State.TIMED_WAITING) {
        Thread.yield()
        if (Instant.now().isAfter(deadline)) {
            throw TimeoutException("spinUntilTimedWait exceeded timeout")
        }
    }
}