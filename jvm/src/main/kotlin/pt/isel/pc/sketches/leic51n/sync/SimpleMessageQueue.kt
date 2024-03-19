package pt.isel.pc.sketches.leic51n.sync

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
        val messageToInsert: T,
        var isDone: Boolean = false,
    )

    private val insertionRequests = NodeLinkedList<InsertionRequest<T>>()

    data class RetrievalRequest<T>(
        val condition: Condition,
        var retrievedMessage: T? = null,
    ) {
        val isDone: Boolean
            // assuming message is never null
            get() = retrievedMessage != null
    }

    private val retrievalRequests = NodeLinkedList<RetrievalRequest<T>>()

    @Throws(InterruptedException::class)
    fun tryInsert(message: T, timeout: Long, timeoutUnit: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            if (retrievalRequests.notEmpty) {
                val headRetrievalRequest = retrievalRequests.pull()
                headRetrievalRequest.value.retrievedMessage = message
                headRetrievalRequest.value.condition.signal()
                return true
            }
            // wait-path
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            val myRequest = insertionRequests.enqueue(
                InsertionRequest(
                    condition = lock.newCondition(),
                    messageToInsert = message
                )
            )
            while (true) {
                try {
                    timeoutInNanos = myRequest.value.condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    // check if success
                    if (myRequest.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    insertionRequests.remove(myRequest)
                    // nothing more to do because insertion cancellation does not
                    // create the conditions to complete another request
                    throw ex
                }
                // check if success
                if (myRequest.value.isDone) {
                    return true
                }
                // check if timeout
                if (timeoutInNanos <= 0) {
                    insertionRequests.remove(myRequest)
                    // nothing more to do because insertion cancellation does not
                    // create the conditions to complete another request
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
                val headInsertionRequest = insertionRequests.pull()
                headInsertionRequest.value.isDone = true
                headInsertionRequest.value.condition.signal()
                return headInsertionRequest.value.messageToInsert
            }
            // wait-path
            var timeoutInNanos = timeoutUnit.toNanos(timeout)
            val myRequest = retrievalRequests.enqueue(
                RetrievalRequest(
                    condition = lock.newCondition()
                )
            )
            while (true) {
                try {
                    timeoutInNanos = myRequest.value.condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    // check for success
                    if (myRequest.value.isDone) {
                        Thread.currentThread().interrupt()
                        return myRequest.value.retrievedMessage
                    }
                    retrievalRequests.remove(myRequest)
                    throw ex
                }
                // check success
                if (myRequest.value.isDone) {
                    // we know for sure that retrievedMessage is not null
                    // because isDone is true
                    return myRequest.value.retrievedMessage!!
                }
                // check timeout
                if (timeoutInNanos <= 0) {
                    retrievalRequests.remove(myRequest)
                    // nothing more to do because retrieval cancellation does not
                    // create the conditions to complete another request
                    return null
                }
            }
        }
    }
}