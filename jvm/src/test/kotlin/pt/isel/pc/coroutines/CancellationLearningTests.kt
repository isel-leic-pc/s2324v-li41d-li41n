package pt.isel.pc.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertFails

class CancellationLearningTests {

    @Test
    fun second() {
        assertFails {
            runBlocking {
                val job = launch {
                    computeSomethingThatNeedsToBeClose().use {
                        logger.info("Using the thing")
                    }
                }
                launch {
                    while (true) {
                        logState(job)
                        delay(100)
                    }
                }
                delay(1500)
                job.cancel()
                delay(1000)
                this.cancel()
            }
        }
    }

    @Test
    fun first() {
        assertFails {
            runBlocking {
                val job = launch {
                    try {
                        ourDelay(1000)
                    } catch (ex: CancellationException) {
                        logger.info("CancellationException")
                    }
                }
                launch {
                    while (true) {
                        logState(job)
                        delay(100)
                    }
                }
                delay(500)
                job.cancel()
                delay(1000)
                this.cancel()
            }
        }
    }

    class SomethingThatNeedsToBeClosed : AutoCloseable {
        override fun close() {
            logger.info("closed!")
        }
    }

    companion object {
        private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        private val logger = LoggerFactory.getLogger(CancellationLearningTests::class.java)
        suspend fun ourDelay(durationInMs: Long) {
            // suspendCoroutine<Unit> { continuation ->
            suspendCancellableCoroutine { continuation ->
                val scheduledFuture = scheduledExecutor.schedule(
                    {
                        logger.info("Before resume call")
                        continuation.resume(Unit)
                        logger.info("After resume call")
                    },
                    durationInMs,
                    TimeUnit.MILLISECONDS
                )
                continuation.invokeOnCancellation {
                    logger.info("invokeOnCancellation")
                    scheduledFuture.cancel(false)
                }
            }
        }

        suspend fun computeSomethingThatNeedsToBeClose():
            SomethingThatNeedsToBeClosed =
            suspendCancellableCoroutine<SomethingThatNeedsToBeClosed> { continuation ->
                scheduledExecutor.schedule(
                    {
                        val theThing = SomethingThatNeedsToBeClosed()
                        continuation.resume(theThing) {
                            logger.info("Closing because it was cancelled")
                            theThing.close()
                        }
                    },
                    1000,
                    TimeUnit.MILLISECONDS
                )
            }
    }

    fun logState(job: Job) {
        logger.info(
            "isActive: {}, isCancelled: {}, isCompleted: {}",
            job.isActive,
            job.isCancelled,
            job.isCompleted
        )
    }
}