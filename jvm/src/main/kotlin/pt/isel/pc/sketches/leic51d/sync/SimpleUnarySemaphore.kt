package pt.isel.pc.sketches.leic51d.sync

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SimpleUnarySemaphore(
    initialUnits: Int,
) {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var units = initialUnits

    init {
        require(initialUnits >= 0) { "initial units must not be negative." }
    }

    fun release() = lock.withLock {
        units += 1
        condition.signal()
    }

    @Throws(InterruptedException::class)
    fun tryAcquire(timeout: Long, timeoutUnit: TimeUnit): Boolean {
        lock.withLock {
            var remainingTimeoutInNanos = timeoutUnit.toNanos(timeout)
            while (units == 0) {
                if (remainingTimeoutInNanos < 0) {
                    return false
                }
                try {
                    remainingTimeoutInNanos = condition.awaitNanos(remainingTimeoutInNanos)
                } catch (e: InterruptedException) {
                    if (units > 0) {
                        condition.signal()
                    }
                    throw e
                }
            }
            units -= 1
            return true
        }
    }
}