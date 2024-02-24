package pt.isel.pc.basics

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val log = LoggerFactory.getLogger(ThreadingHazardsTests::class.java)

// Number of threads used on each test
private const val N_OF_THREADS = 10

// Number of repetitions performed by each thread
private const val N_OF_REPS = 1000000

private val threadBuilder: Thread.Builder = Thread.ofPlatform()

/**
 * Test class illustrating common errors due to the use of shared mutable date
 * by more than one thread.
 */
class ThreadingHazardsTests {

    /**************************************************************************
     * This test illustrates the problem of mutating a shared integer,
     * namely the loss of increments.
     */
    // mutable counter
    private var simpleCounter = 0

    private fun incrementSimpleCounter() {
        repeat(N_OF_REPS) {
            simpleCounter += 1
        }
    }

    @Test
    fun `loosing increments - using separate method`() {
        // when: creating N_OF_THREADS that each increment simpleCounter N_OF_REPS times
        val threads = List(N_OF_THREADS) {
            threadBuilder.start(this::incrementSimpleCounter)
        }
        // and: waiting for these threads to end
        threads.forEach(Thread::join)

        // then: the final counter value is NOT N_OF_THREADS * N_OF_REPS
        assertNotEquals(N_OF_THREADS * N_OF_REPS, simpleCounter)
    }

    @Test
    fun `loosing increments - using lambda`() {
        // when: creating N_OF_THREADS that each increment simpleCounter N_OF_REPS times
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                // note that this code runs in a different thread
                repeat(N_OF_REPS) {
                    simpleCounter += 1
                }
            }
        }

        // and: waiting for these threads to end
        threads.forEach(Thread::join)

        // then: the final counter value is NOT N_OF_THREADS * N_OF_REPS
        assertNotEquals(N_OF_THREADS * N_OF_REPS, simpleCounter)
    }

    /**************************************************************************
     * This test illustrates the hazards associated to
     * mutable data sharing between threads, in this case insertions into a linked list.
     */
    // Just a simple stack using a linked list
    class SimpleLinkedStack<T> {

        private class Node<T>(val item: T, val next: Node<T>?)

        // mutable
        private var head: Node<T>? = null

        fun push(value: T) {
            head = Node(item = value, next = head)
        }

        fun pop(): T? {
            val observedHead = head ?: return null
            head = observedHead.next
            return observedHead.item
        }

        val isEmpty: Boolean
            get() = head == null
    }

    // Note:  `nonThreadSafeList` reference is immutable, however the referenced data structure is mutable
    private val nonThreadSafeList = SimpleLinkedStack<Int>()

    @Test
    fun `loosing items on a linked list`() {
        // when: creating N_OF_THREADS that each insert '1' N_OF_REPS times on the list
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                // note that this code runs in a different thread
                repeat(N_OF_REPS) {
                    nonThreadSafeList.push(1)
                }
            }
        }

        // and: waiting for these threads to end
        threads.forEach(Thread::join)

        // and: counting the number of elements in the list
        var acc = 0
        while (!nonThreadSafeList.isEmpty) {
            val elem = nonThreadSafeList.pop()
            checkNotNull(elem)
            acc += elem
        }

        // then: the number of elements is NOT N_OF_THREADS * N_OF_REPS
        assertNotEquals(N_OF_THREADS * N_OF_REPS, acc)
    }

    /**************************************************************************
     * This test illustrates another problem when sharing
     * mutable data structures, even if they have some data synchronization.
     * In this case, the problem is typically called a 'check-then-act' hazard
     * and happens because the shared state can change between
     * the 'check' and the 'act'
     */

    // Here the map is "thread-safe" and the counter is also "thread-safe"
    private val map: MutableMap<Int, AtomicInteger> =
        Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())

    @Test
    fun `loosing increments with a synchronized map and atomics`() {
        // when: creating N_OF_THREADS that each increment the counters associated to the keys [1, N_OF_THREADS[
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                repeat(N_OF_REPS) { key ->
                    val data = map[key]
                    if (data == null) {
                        map[key] = AtomicInteger(1)
                    } else {
                        data.incrementAndGet()
                    }
                }
            }
        }

        // and: waiting for all threads to end
        threads.forEach(Thread::join)

        // and: summing the counter for all the keys
        val totalCount = map.values
            .map { it.get() }
            .reduce { acc, elem ->
                acc + elem
            }

        // then: the sum is NOT N_OF_THREADS * N_OF_REPS
        assertNotEquals(N_OF_THREADS * N_OF_REPS, totalCount)
    }

    // Using `computeIfAbsent` to have an atomic check-then-act
    private val concurrentMap: MutableMap<Int, AtomicInteger> = ConcurrentHashMap()

    @Test
    fun `NOT loosing increments with a ConcurrentHashMap and computeIfAbsent`() {
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                (0 until N_OF_REPS).forEach { key ->
                    concurrentMap.computeIfAbsent(key) {
                        AtomicInteger(0)
                    }.incrementAndGet()
                }
            }
        }

        threads.forEach(Thread::join)

        val totalCount = concurrentMap.values
            .map { it.get() }
            .reduce { acc, elem ->
                acc + elem
            }

        assertEquals(N_OF_THREADS * N_OF_REPS, totalCount)
    }

    /**************************************************************************
     * This test illustrates the problem of mutating a shared integer,
     * namely the loss of increments.
     * This happens even if the shared counter is marked as volatile,
     * which doesn't ensure atomicity of increments.
     */
    // mutable volatile counter
    @Volatile
    var simpleVolatileCounter = 0

    @Test
    fun `loosing increments of volatile field`() {
        val threads = List(N_OF_THREADS) {
            threadBuilder.start {
                // note that this code runs in a different thread
                repeat(N_OF_REPS) {
                    simpleVolatileCounter += 1
                }
            }
        }

        threads.forEach(Thread::join)
        assertNotEquals(N_OF_THREADS * N_OF_REPS, simpleVolatileCounter)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun checkRequirements() {
            // These tests fail more frequently if running on system with only 1 processor (e.g. CI)
            val nOfProcessors = Runtime.getRuntime().availableProcessors()
            log.info("Available processors: {}", nOfProcessors)
            assumeTrue(
                nOfProcessors > 1,
                "Requires a minimum number of processors, otherwise the failure rate is high"
            )
            log.info("Requirements are fulfilled")
        }
    }
}