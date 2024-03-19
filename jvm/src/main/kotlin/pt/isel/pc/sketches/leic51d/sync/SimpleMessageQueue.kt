package pt.isel.pc.sketches.leic51d.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * FIFO message queue, with zero capacity and unary insertion and retrieval.
 * Uses kernel-style.
 */
class SimpleMessageQueue<T : Any> {

    private val lock = ReentrantLock()

    data class InsertionRequest<T>(
        val condition: Condition,
        val message: T,
        var isDone: Boolean,
    )

    data class RetrievalRequest<T>(
        val condition: Condition,
        var message: T?,
    ) {
        val isDone: Boolean
            get() = message != null
    }

    private val insertionRequests = NodeLinkedList<InsertionRequest<T>>()
    private val retrievalRequests = NodeLinkedList<RetrievalRequest<T>>()

    @Throws(InterruptedException::class)
    fun tryInsert(message: T, timeout: Long, timeoutUnit: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            if (retrievalRequests.notEmpty) {
                val firstRetrievalRequest = retrievalRequests.pull()
                firstRetrievalRequest.value.message = message
                firstRetrievalRequest.value.condition.signal()
                return true
            }
            // wait-path
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            val selfNode = insertionRequests.enqueue(
                InsertionRequest(
                    condition = lock.newCondition(),
                    message = message,
                    isDone = false
                )
            )
            while (true) {
                try {
                    timeoutInNanos = selfNode.value.condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    if (selfNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    insertionRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    throw ex
                }
                // check for success
                if (selfNode.value.isDone) {
                    return true
                }
                // check for timeout
                if (timeoutInNanos <= 0) {
                    insertionRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryRetrieve(timeout: Long, timeoutUnit: TimeUnit): T? {
        lock.withLock {
            // fast-path
            if (insertionRequests.notEmpty) {
                val firstInsertionRequest = insertionRequests.pull()
                firstInsertionRequest.value.isDone = true
                firstInsertionRequest.value.condition.signal()
                return firstInsertionRequest.value.message
            }

            // wait-path
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            val selfNode = retrievalRequests.enqueue(
                RetrievalRequest(
                    condition = lock.newCondition(),
                    message = null
                )
            )
            while (true) {
                try {
                    timeoutInNanos = selfNode.value.condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    if (selfNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return selfNode.value.message
                    }
                    retrievalRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    throw ex
                }
                // check for success
                if (selfNode.value.isDone) {
                    return selfNode.value.message
                }
                // check for timeout
                if (timeoutInNanos <= 0) {
                    retrievalRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    return null
                }
            }
        }
    }
}