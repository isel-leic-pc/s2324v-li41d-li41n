package pt.isel.pc.sketches.leic51d.coroutines

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Semaphore(
    initialUnits: Int,
) {
    private val lock = ReentrantLock()
    private var units = initialUnits

    private data class Request(
        val continuation: Continuation<Unit>,
        var isDone: Boolean,
    )

    private val requests = NodeLinkedList<Request>()

    /**
     * Releases (i.e. adds) one unit to the semaphore
     */
    fun release() {
        val continuation: Continuation<Unit>? = lock.withLock {
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
        suspendCoroutine<Unit> { continuation ->
            lock.withLock {
                if (units > 0) {
                    units -= 1
                    continuation.resume(Unit)
                } else {
                    requests.enqueue(Request(continuation, false))
                }
            }
        }
    }
}