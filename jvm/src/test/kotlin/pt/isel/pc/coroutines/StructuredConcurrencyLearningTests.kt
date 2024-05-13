package pt.isel.pc.coroutines

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class StructuredConcurrencyLearningTests {

    @Test
    fun `cancelling a child coroutine`() {
        runBlocking(Dispatchers.Default) {
            val deferred: Deferred<String> = async {
                // Thread.sleep(1000)
                ourDelay(1000)
                logger.info("returning success - hello")
                "hello"
            }
            val observer = launch {
                while (true) {
                    logState(deferred)
                    delay(100)
                }
            }
            delay(500)
            deferred.cancel()
            try {
                val res = deferred.await()
                logger.info("success: {}", res)
            } catch (ex: Throwable) {
                logger.info("exception: {}", ex.message)
            }
            delay(1000)
            observer.cancel()
        }
    }

    private fun logState(job: Job) {
        logger.info(
            "isActive:{}, isCancelled:{}, isCompleted:{}",
            job.isActive,
            job.isCancelled,
            job.isCompleted
        )
    }

    @Test
    fun first() {
        assertThrows<Exception> {
            runBlocking {
                val job = launch {
                    launch {
                        delay(1000)
                        logger.info("child coroutine's function ended")
                    }
                    delay(500)
                    logger.info("coroutine's function ending with exception")
                    throw Exception()
                }
                while (true) {
                    logger.info(
                        "isActive: {}, isCompleted: {}",
                        job.isActive,
                        job.isCompleted
                    )
                    if (job.isCompleted) {
                        break
                    }
                    delay(100)
                }
            }
        }
    }

    class SomethingThatNeedsToBeClosed() : AutoCloseable {
        init {
            logger.info("SomethingThatNeedsToBeClosed created")
        }

        override fun close() {
            logger.info("close was called")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StructuredConcurrencyLearningTests::class.java)
        private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        private suspend fun ourDelay(durationInMs: Long) =
            suspendCancellableCoroutine { continuation ->
                scheduledExecutor.schedule(
                    {
                        logger.info("scheduler callback starting")
                        val theThing = SomethingThatNeedsToBeClosed()
                        continuation.resume(theThing) {
                            logger.info("closing on continuation action")
                            theThing.close()
                        }
                        logger.info("scheduler callback ending")
                    },
                    durationInMs,
                    TimeUnit.MILLISECONDS
                )
                continuation.invokeOnCancellation {
                    // scheduledFuture.cancel(true)
                }
            }.use {
                logger.info("Using {}", it)
            }
    }
}