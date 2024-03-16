package pt.isel.pc.sketches.leic51n.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Semaphore providing fairness on acquisition by completing those requests
 * in a First In First Out (FIFO) schedule.
 * Uses a [Condition] per awaiting request to avoid the signal all.
 */
class SimpleUnarySemaphoreWithFairnessAndSpecificSignalling(
    initialUnits: Int,
) {

    private val lock = ReentrantLock()

    // No per-instance Condition
    private var units: Int = initialUnits
    private val requestQueue = NodeLinkedList<Condition>()

    fun release() = lock.withLock {
        units += 1
        signalIfNeeded()
    }

    @Throws(InterruptedException::class)
    fun tryAcquire(timeout: Long, timeoutUnit: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            if (units > 0 && requestQueue.empty) {
                units -= 1
                return true
            }
            // wait-path
            val selfNode = requestQueue.enqueue(lock.newCondition())
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            while (true) {
                try {
                    timeoutInNanos = selfNode.value.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    requestQueue.remove(selfNode)
                    signalIfNeeded()
                    throw ex
                }
                if (units > 0 && requestQueue.isHeadNode(selfNode)) {
                    requestQueue.remove(selfNode)
                    units -= 1
                    signalIfNeeded()
                    return true
                }
                if (timeoutInNanos <= 0) {
                    requestQueue.remove(selfNode)
                    signalIfNeeded()
                    return false
                }
            }
        }
    }

    private fun signalIfNeeded() {
        if (units > 0 && requestQueue.notEmpty) {
            requestQueue.headNode?.value?.signal()
        }
    }
}