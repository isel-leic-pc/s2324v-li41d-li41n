package pt.isel.pc.sketches.leic51n.sync

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A unary semaphore with timeout and interruption capability.
 */
class SimpleUnarySemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0) { "initial units must be non-negative." }
    }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var units: Int = initialUnits

    fun release() = lock.withLock {
        units += 1
        condition.signal()
    }

    @Throws(InterruptedException::class)
    fun tryAcquire(timeout: Long, timeoutUnit: TimeUnit): Boolean {
        lock.withLock {
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            while (units == 0) {
                // Needs to be after ensuring units is not greater than zero.
                if (timeoutInNanos <= 0) {
                    return false
                }
                try {
                    timeoutInNanos = condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    if (units > 0) {
                        condition.signal()
                    }
                    throw ex
                }
            }
            units -= 1
            return true
        }
    }
}