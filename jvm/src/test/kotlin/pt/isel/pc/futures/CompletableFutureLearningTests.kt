package pt.isel.pc.futures

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch

class CompletableFutureLearningTests {

    @Test
    fun first() {
        val countDownLatch = CountDownLatch(1)
        val f0: CompletableFuture<Int> = CompletableFuture<Int>()
        val f1: CompletionStage<Double> = f0.thenApplyAsync {
            logger.info("Running continuation on f0")
            it * 2.0
        }
        val f2: CompletionStage<String> = f1.thenApply {
            logger.info("Running continuation on f1")
            it.toString()
        }
        f2.handle { _, _ ->
            logger.info("Running continuation on f2")
            countDownLatch.countDown()
        }
        Thread.ofPlatform().start {
            Thread.sleep(500)
            logger.info("Before complete")
            f0.complete(42)
            logger.info("After complete")
        }
        logger.info("Waiting on countDownLatch")
        countDownLatch.await()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CompletableFutureLearningTests::class.java)
    }
}