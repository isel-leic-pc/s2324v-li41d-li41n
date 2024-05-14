package pt.isel.pc.sketches.leic51d.coroutines

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.resume

class Semaphore4(
    initialUnits: Int,
) {
    private val lock = ReentrantLock()
    private var units = initialUnits

    private data class Request(
        var continuation: CancellableContinuation<Unit>?,
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
        val request = Request(null, false)
        var requestNode: NodeLinkedList.Node<Request>? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                lock.withLock {
                    if (units > 0) {
                        units -= 1
                        request.isDone = true
                        continuation.resume(Unit)
                    } else {
                        request.continuation = continuation
                        requestNode = requests.enqueue(
                            request
                        )
                    }
                }
            }
        } catch (ex: CancellationException) {
            lock.withLock {
                if (request.isDone) {
                    return
                }
                requests.remove(requestNode!!)
                throw ex
            }
        }
    }
}