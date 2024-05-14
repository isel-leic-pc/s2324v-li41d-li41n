package pt.isel.pc.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.Test
import kotlin.test.assertFails

class LockingAndSuspendFunctionsLearningTests {

    @Test
    fun first() {
        assertFails {
            runBlocking(Dispatchers.Default) {
                repeat(5) { rep ->
                    launch {
                        val lock = ReentrantLock()
                        lock.lock()
                        try {
                            logger.info("{} Before delay, on thread {}", rep, Thread.currentThread())
                            delay(1000)
                            logger.info("{} After delay, on thread {}", rep, Thread.currentThread())
                        } finally {
                            lock.unlock()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun second() {
        runBlocking(Dispatchers.Default) {
            repeat(5) { rep ->
                launch {
                    // IS NOT REENTRANT
                    val mutex = Mutex()
                    mutex.lock()
                    try {
                        logger.info("{} Before delay, on thread {}", rep, Thread.currentThread())
                        delay(1000)
                        logger.info("{} After delay, on thread {}", rep, Thread.currentThread())
                    } finally {
                        mutex.unlock()
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LockingAndSuspendFunctionsLearningTests::class.java)
    }
}