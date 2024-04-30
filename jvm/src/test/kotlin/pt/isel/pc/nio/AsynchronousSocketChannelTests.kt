package pt.isel.pc.nio

import java.nio.channels.CompletionHandler
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test

class AsynchronousSocketChannelTests {

    @Test
    fun synchronousGet() {
        val response = getHttpBin("/get")
        println(response)
    }

    @Test
    fun asynchronousGetUsingStateMachine() {
        val countDownLatch = CountDownLatch(1)
        getHttpBin(
            "/get",
            object : CompletionHandler<String, Void?> {
                override fun completed(result: String, attachment: Void?) {
                    println("completed: $result")
                    countDownLatch.countDown()
                }

                override fun failed(exc: Throwable, attachment: Void?) {
                    println("failed: ${exc.message}")
                    countDownLatch.countDown()
                }
            }
        )
        countDownLatch.await()
    }

    @Test
    fun asynchronousGetUsingCoroutines() {
        val countDownLatch = CountDownLatch(1)
        val func = ::suspendGetHttpBin as (String, Continuation<String>) -> Any?
        func(
            "/get",
            object : Continuation<String> {
                override val context: CoroutineContext
                    get() = EmptyCoroutineContext

                override fun resumeWith(result: Result<String>) {
                    if (result.isSuccess) {
                        println("completed: ${result.getOrNull()}")
                    } else {
                        println("failed: ${result.exceptionOrNull()}")
                    }
                    countDownLatch.countDown()
                }
            }
        )
        countDownLatch.await()
    }
}