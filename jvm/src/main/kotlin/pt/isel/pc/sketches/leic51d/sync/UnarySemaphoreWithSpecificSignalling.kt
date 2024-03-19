package pt.isel.pc.sketches.leic51d.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class UnarySemaphoreWithSpecificSignalling(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0) { "Initial units must not be negative" }
    }

    private val lock = ReentrantLock()
    private var units = initialUnits
    private val requesters = NodeLinkedList<Condition>()

    fun tryAcquire(timeout: Long, timeoutUnits: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            if (units > 0 && requesters.empty) {
                units -= 1
                return true
            }
            // wait-path
            var timeoutInNanos = timeoutUnits.toNanos(timeout)
            // condition where this thread and *only* this thread awaits
            val selfCondition = lock.newCondition()
            val selfNode = requesters.enqueue(selfCondition)
            while (true) {
                try {
                    timeoutInNanos = selfCondition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    requesters.remove(selfNode)
                    signalIfNeeded()
                    throw ex
                }
                if (units > 0 && requesters.isHeadNode(selfNode)) {
                    units -= 1
                    requesters.remove(selfNode)
                    signalIfNeeded()
                    return true
                }
                if (timeoutInNanos <= 0) {
                    requesters.remove(selfNode)
                    signalIfNeeded()
                    return false
                }
            }
        }
    }

    fun release() = lock.withLock {
        units += 1
        signalIfNeeded()
    }

    private fun signalIfNeeded() {
        if (units > 0) {
            val headNode = requesters.headNode
            headNode?.value?.signal()
        }
    }
}