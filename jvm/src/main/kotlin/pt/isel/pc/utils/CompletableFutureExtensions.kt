package pt.isel.pc.utils

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> CompletableFuture<T>.await(): T = suspendCoroutine { continuation ->
    this.handle { res, ex ->
        if (ex != null) {
            continuation.resumeWithException(ex)
        } else {
            continuation.resume(res)
        }
    }
}