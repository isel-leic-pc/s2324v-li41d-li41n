package pt.isel.pc.sketches.leic51d.coroutines

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.resume

class Semaphore3(
    initialUnits: Int,
) {
    private val lock = ReentrantLock()
    private var units = initialUnits

    private data class Request(
        val continuation: CancellableContinuation<Unit>,
        var isDone: Boolean,
    )

    private val requests = NodeLinkedList<Request>()

    /**
     * Releases (i.e. adds) one unit to the semaphore
     */
    fun release() {
        val continuation: CancellableContinuation<Unit>? = lock.withLock {
            if (requests.notEmpty) {
                val request = requests.pull()
                request.value.isDone = true
                request.value.continuation
            } else {
                units += 1
                null
            }
        }
        continuation?.resume(Unit)
    }

    /**
     * Acquires one unit from the semaphore
     */
    suspend fun acquire() {
        var requestNode: NodeLinkedList.Node<Semaphore3.Request>? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                lock.withLock {
                    if (units > 0) {
                        units -= 1
                        continuation.resume(Unit)
                    } else {
                        requestNode = requests.enqueue(
                            Request(continuation, false)
                        )
                    }
                }
            }
        } catch (ex: CancellationException) {
            val observedNode = requestNode ?: throw ex
            lock.withLock {
                if (observedNode.value.isDone) {
                    return
                }
                requests.remove(observedNode)
                throw ex
            }
        }
    }
}