package pt.isel.pc.sketches.leic51n.coroutines

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Semaphore(
    initialUnits: Int,
) {
    private var units: Int = initialUnits
    private val lock = ReentrantLock()

    private data class Request(
        val continuation: Continuation<Unit>,
    )

    private val requests = NodeLinkedList<Request>()

    fun release() {
        val continuation = lock.withLock {
            if (requests.notEmpty) {
                val head = requests.pull()
                head.value.continuation
            } else {
                units += 1
                null
            }
        }
        continuation?.resume(Unit)
    }

    suspend fun acquire() {
        suspendCoroutine<Unit> { continuation ->
            lock.withLock {
                if (units > 0) {
                    units -= 1
                    continuation.resume(Unit)
                } else {
                    requests.enqueue(Request(continuation))
                }
            }
        }
    }
}