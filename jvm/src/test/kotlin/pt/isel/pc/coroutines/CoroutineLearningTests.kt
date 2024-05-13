package pt.isel.pc.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test

class CoroutineLearningTests {

    @Test()
    fun cancellationExample3() {
        runBlocking(Dispatchers.Default) {
            launch {
                launch {
                    delay(200)
                    throw CancellationException()
                }
                try {
                    delay(1000)
                } catch (ex: CancellationException) {
                    logger.info("CancellationException on outer coroutine")
                }
            }
            delay(2000)
        }
    }

    @Test()
    fun cancellationExample2() {
        runBlocking(Dispatchers.Default) {
            val job = launch {
                launch {
                    try {
                        delay(2000)
                    } catch (ex: CancellationException) {
                        logger.info("CancellationException on inner coroutine")
                    }
                }
                try {
                    delay(1000)
                } catch (ex: CancellationException) {
                    logger.info("CancellationException on outer coroutine")
                }
            }
            delay(500)
            job.cancel()
        }
    }

    @Test()
    fun cancellationExample1() {
        runBlocking(Dispatchers.Default) {
            val job = launch {
                // Thread.sleep(1000)
                val now = Instant.now().toEpochMilli()
                while (true) {
                    try {
                        if (Instant.now().toEpochMilli() - now > 1000) {
                            break
                        }
                        delay(1000)
                    } catch (ex: CancellationException) {
                        // ignore
                    }
                }
                // throw Exception()
            }
            var counter = 1
            while (true) {
                logger.info(
                    "isActive: {}, isCompleted: {}, isCancelled: {}",
                    job.isActive,
                    job.isCompleted,
                    job.isCancelled
                )
                if (job.isCompleted) {
                    break
                }
                Thread.sleep(50)
                if (++counter == 4) {
                    job.cancel()
                }
            }
        }
    }

    @Test
    fun third() {
        logger.info("test starting")
        // coroutine builder
        runBlocking {
            logger.info("first coroutine starting")
            repeat(1) {
                // coroutine builder
                launch {
                    logger.info("Inner coroutine starting {}", it)
                    // delay(1000)
                    suspendDelay(1000)
                    logger.info("Inner coroutine ending {}", it)
                }
            }
            logger.info("first coroutine ending")
        }
        logger.info("test ending")
    }

    suspend fun suspendDelay(duration: Long) {
        logger.info("Before suspendCoroutine")
        val res = suspendCoroutine { continuation ->
            logger.info("Inside suspendCoroutine function")
            scheduledExecutor2.schedule(
                {
                    logger.info("Calling continuation")
                    continuation.resume(42)
                    logger.info("After calling resume")
                },
                duration,
                TimeUnit.MILLISECONDS
            )
        }
        logger.info("After suspendCoroutine: {}", res)
    }

    private val scheduledExecutor2: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    @Test
    fun second() {
        logger.info("Test starting, not in a coroutine")
        val counter = AtomicInteger()
        runBlocking {
            logger.info("Start of coroutine")
            repeat(1_000) {
                launch {
                    logger.info("Start of child coroutine {}", it)
                    delay(1000)
                    counter.incrementAndGet()
                    logger.info("End of child coroutine {}", it)
                }
            }
            // Thread.sleep(500)
            // ourDelay(500)
            logger.info("End of coroutine's function")
        }
        logger.info("Test ending, {}", counter)
    }

    suspend fun ourDelay(duration: Long) {
        logger.info("Before delay")
        suspendCoroutine { continuation ->
            logger.info("Before schedule")
            scheduledExecutor.schedule({
                logger.info("Calling continuation")
                continuation.resume(Unit)
            }, duration, TimeUnit.MILLISECONDS)
            logger.info("After schedule")
        }
        logger.info("After delay")
    }

    private val scheduledExecutor =
        Executors.newSingleThreadScheduledExecutor()

    @Test
    fun first() {
        logger.info("starting test")
        runBlocking {
            repeat(1_000_000) {
                launch {
                    // logger.info("starting coroutine")
                    delay(1000)
                    // Thread.sleep(1000)
                    // logger.info("ending coroutine")
                }
            }
            logger.info("after repeat")
        }
        logger.info("ending test")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CoroutineLearningTests::class.java)
    }
}