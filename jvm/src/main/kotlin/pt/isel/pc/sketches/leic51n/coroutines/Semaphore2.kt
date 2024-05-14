package pt.isel.pc.sketches.leic51n.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class Semaphore2(
    initialUnits: Int,
) {
    private var units: Int = initialUnits
    private val lock = ReentrantLock()

    private data class Request(
        var isDone: Boolean,
        val continuation: Continuation<Unit>,
    )

    private val requests = NodeLinkedList<Request>()

    fun release() {
        val continuation = lock.withLock {
            if (requests.notEmpty) {
                val head = requests.pull()
                head.value.isDone = true
                head.value.continuation
            } else {
                units += 1
                null
            }
        }
        continuation?.resume(Unit)
    }

    suspend fun acquire() {
        var isFastPath = false
        var requestNode: NodeLinkedList.Node<Request>? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                lock.withLock {
                    if (units > 0) {
                        units -= 1
                        continuation.resume(Unit)
                        isFastPath = true
                    } else {
                        requestNode = requests.enqueue(
                            Request(false, continuation)
                        )
                    }
                }
            }
        } catch (ex: CancellationException) {
            if (isFastPath) {
                // fast-path, success, nothing to remove
                return
            }
            // if null, then no fast-path OR wait-path
            val observedRequestNode = requestNode ?: throw ex
            // else, wait-path
            lock.withLock {
                if (observedRequestNode.value.isDone) {
                    return
                }
                requests.remove(observedRequestNode)
                throw ex
            }
        }
    }
}