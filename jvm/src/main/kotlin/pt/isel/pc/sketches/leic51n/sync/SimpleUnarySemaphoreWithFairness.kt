package pt.isel.pc.sketches.leic51n.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Semaphore providing fairness on acquisition by completing those requests
 * in a First In First Out (FIFO) schedule.
 */
class SimpleUnarySemaphoreWithFairness(
    initialUnits: Int,
) {

    init {
        require(initialUnits >= 0) { "initial units must be non-negative." }
    }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var units: Int = initialUnits
    private val requestQueue = NodeLinkedList<Unit>()

    fun release() = lock.withLock {
        units += 1
        signalAllIfNeeded()
    }

    @Throws(InterruptedException::class)
    fun tryAcquire(timeout: Long, timeoutUnit: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            // cannot use units if there are already other pending requests
            if (units > 0 && requestQueue.empty) {
                units -= 1
                return true
            }
            // wait-path
            val selfNode = requestQueue.enqueue(Unit)
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            while (true) {
                try {
                    timeoutInNanos = condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    requestQueue.remove(selfNode)
                    signalAllIfNeeded()
                    throw ex
                }
                // can only use the units if at the head of the pending requests queue
                if (units > 0 && requestQueue.isHeadNode(selfNode)) {
                    requestQueue.remove(selfNode)
                    units -= 1
                    signalAllIfNeeded()
                    return true
                }
                if (timeoutInNanos <= 0) {
                    requestQueue.remove(selfNode)
                    signalAllIfNeeded()
                    return false
                }
            }
        }
    }

    private fun signalAllIfNeeded() {
        // does not acquire lock because is private, i.e., it is called when the lock is already held.
        if (units > 0 && requestQueue.notEmpty) {
            condition.signalAll()
        }
    }
}