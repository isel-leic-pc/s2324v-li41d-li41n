package pt.isel.pc.sketches.leic51d.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class UnarySemaphoreWithFairness(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0) { "Initial units must be non-negative." }
    }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var units = initialUnits
    private val requesters = NodeLinkedList<Thread>()

    fun tryAcquire(timeout: Long, timeoutUnits: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            if (units > 0 && requesters.empty) {
                units -= 1
                return true
            }
            // wait-path
            var timeoutInNanos = timeoutUnits.toNanos(timeout)
            val selfNode: NodeLinkedList.Node<Thread> = requesters.enqueue(Thread.currentThread())
            while (true) {
                try {
                    timeoutInNanos = condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    requesters.remove(selfNode)
                    signalAllIfNeeded()
                    throw ex
                }
                if (units > 0 && requesters.isHeadNode(selfNode)) {
                    units -= 1
                    requesters.remove(selfNode)
                    signalAllIfNeeded()
                    return true
                }
                if (timeoutInNanos <= 0) {
                    requesters.remove(selfNode)
                    signalAllIfNeeded()
                    return false
                }
            }
        }
    }

    fun release() {
        lock.withLock {
            units += 1
            signalAllIfNeeded()
        }
    }

    private fun signalAllIfNeeded() {
        if (units > 0 && requesters.notEmpty) {
            condition.signalAll()
        }
    }
}