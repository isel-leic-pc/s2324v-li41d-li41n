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
 * Uses kernel-style.
 */
class UnarySemaphoreWithFairnessAndKernelStyle(
    initialUnits: Int,
) {

    private val lock = ReentrantLock()
    private var units = initialUnits

    // Represents a acquire request that needed to wait
    data class Request(
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private val requestQueue = NodeLinkedList<Request>()

    fun release() = lock.withLock {
        if (requestQueue.notEmpty) {
            // kernel-style: the thread providing the unit completes the work for the requesting thread
            // namely, removing the node from the queue and decrementing the units.
            val headNode = requestQueue.pull()
            headNode.value.isDone = true
            headNode.value.condition.signal()
        } else {
            units += 1
        }
    }

    fun tryAcquire(timeout: Long, timeoutUnit: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            // with kernel-style it is not possible for the unary semaphore to simultaneously have
            // units and awaiting requests.
            if (units > 0) {
                units -= 1
                return true
            }
            val selfNode = requestQueue.enqueue(
                Request(
                    condition = lock.newCondition()
                )
            )
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            while (true) {
                try {
                    timeoutInNanos = selfNode.value.condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    if (selfNode.value.isDone) {
                        // re-set interrupt status
                        Thread.currentThread().interrupt()
                        return true
                    }
                    requestQueue.remove(selfNode)
                    throw ex
                }
                if (selfNode.value.isDone) {
                    // kernel-style: no need to do anything more
                    return true
                }
                if (timeoutInNanos <= 0) {
                    requestQueue.remove(selfNode)
                    return false
                }
            }
        }
    }
}