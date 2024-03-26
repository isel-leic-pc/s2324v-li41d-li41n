package pt.isel.pc.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

// TODO still needs to be tested
class FairNArySemaphoreUsingKernelStyle(
    initialUnits: Long,
) {

    init {
        require(initialUnits >= 0) {
            "Number of initial units must be greater than zero"
        }
    }

    private val lock = ReentrantLock()

    data class AcquireRequest(
        var isDone: Boolean = false,
        val units: Long,
        val condition: Condition,
    ) {
        init {
            require(units > 0) { "requested units must be greater than zero" }
        }
    }

    private var units = initialUnits
    private val requests = NodeLinkedList<AcquireRequest>()

    fun release(releasedUnits: Long) = lock.withLock {
        units += releasedUnits
        completeAllPossible()
    }

    fun tryAcquire(requestedUnits: Long, timeout: Duration): Boolean {
        lock.withLock {
            var remainingTimeInNanos = timeout.inWholeNanoseconds
            // fast-path
            // available units and no other previous thread waiting
            if (units >= requestedUnits && requests.empty) {
                units -= requestedUnits
                return true
            }
            // wait-path
            val selfNode = requests.enqueue(
                AcquireRequest(
                    units = requestedUnits,
                    condition = lock.newCondition()
                )
            )
            while (true) {
                try {
                    remainingTimeInNanos = selfNode.value.condition.awaitNanos(remainingTimeInNanos)
                } catch (e: InterruptedException) {
                    if (selfNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    requests.remove(selfNode)
                    completeAllPossible()
                    throw e
                }
                if (selfNode.value.isDone) {
                    return true
                }
                if (remainingTimeInNanos <= 0) {
                    requests.remove(selfNode)
                    completeAllPossible()
                    return false
                }
            }
        }
    }

    private fun completeAllPossible() {
        while (requests.headCondition { units >= it.units }) {
            val headRequest = requests.pull()
            headRequest.value.isDone = true
            headRequest.value.condition.signal()
            units -= headRequest.value.units
        }
    }
}