package pt.isel.pc.loom

import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class VirtualThreadTests {

    @Test
    fun `race with platform and virtual thread creation`() {
        val nOfThreads = 100_000
        val t1 = Thread.ofPlatform().start {
            createLoop("platform", nOfThreads, 100, Thread.ofPlatform())
        }
        val t2 = Thread.ofPlatform().start {
            createLoop("virtual", nOfThreads, 10_000, Thread.ofVirtual())
        }
        t1.join()
        t2.join()
    }

    private fun createLoop(name: String, nOfThreads: Int, reportInterval: Int, builder: Thread.Builder) {
        val threads = mutableListOf<Thread>()
        repeat(nOfThreads) { index ->
            if (index % reportInterval == 0) {
                logger.info("Created {} {} threads", index, name)
            }
            val t = builder.start {
                Thread.sleep(100)
            }
            threads.add(t)
        }
        threads.forEach { it.join() }
    }

    @Test
    fun `chain message through threads`() {
        val nOfThreads = 500
        logger.info("Time using plat threads = {}", chainLoop(nOfThreads, Thread.ofPlatform()))
        logger.info("Time using virt threads = {}", chainLoop(nOfThreads, Thread.ofVirtual()))
        logger.info("Time using plat threads = {}", chainLoop(nOfThreads, Thread.ofPlatform()))
        logger.info("Time using virt threads = {}", chainLoop(nOfThreads, Thread.ofVirtual()))
        logger.info("Time using plat threads = {}", chainLoop(nOfThreads, Thread.ofPlatform()))
        logger.info("Time using virt threads = {}", chainLoop(nOfThreads, Thread.ofVirtual()))
    }

    private fun chainLoop(nOfThreads: Int, builder: Thread.Builder): Long {
        val queue = LinkedBlockingQueue<String>()
        val countDownLatch = CountDownLatch(nOfThreads)
        val threads = List(nOfThreads) {
            builder.start {
                val message = queue.take()
                queue.put(message)
                countDownLatch.countDown()
            }
        }
        Thread.sleep(2000)
        queue.put("hello")
        val start = System.nanoTime()
        countDownLatch.await()
        val stop = System.nanoTime()
        threads.forEach { it.join() }
        return stop - start
    }

    @Test
    fun `cpu intensive loops`() {
        val nOfThreads = 40
        val counters = Array<AtomicInteger>(nOfThreads) {
            AtomicInteger()
        }
        val deadline = Instant.now().plusMillis(4000)
        val threads = List(nOfThreads) { index ->
            Thread.ofVirtual().start {
                while (Instant.now() < deadline) {
                    repeat(10) {
                        counters[index].incrementAndGet()
                    }
                }
            }
        }
        threads.forEach { it.join() }
        counters.forEachIndexed { index, counter ->
            logger.info("Counter {} has value {}", index, counter)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VirtualThreadTests::class.java)
    }
}