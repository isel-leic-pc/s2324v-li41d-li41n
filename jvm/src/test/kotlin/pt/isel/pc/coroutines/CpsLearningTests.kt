package pt.isel.pc.coroutines

import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test

class CpsLearningTests {

    @Test
    fun first() {
        val countDownLatch = CountDownLatch(1)
        val cpsF1 = ::f1 as (String, Continuation<Int>) -> Any
        val res = cpsF1(
            "hello",
            object : Continuation<Int> {
                override val context: CoroutineContext
                    get() = EmptyCoroutineContext

                override fun resumeWith(result: Result<Int>) {
                    logger.info("result: {}", result)
                    countDownLatch.countDown()
                }
            }
        )
        logger.info("cpsF1 returned {}", res)
        if (res !is Int) {
            countDownLatch.await()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CpsLearningTests::class.java)
    }
}