package pt.isel.pc.basics

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.test.assertEquals

private val log = LoggerFactory.getLogger(ThreadBasicsTests::class.java)
private fun threadMethod() {
    val name = Thread.currentThread().name
    log.info("Running on thread '{}'", name)
    Thread.sleep(Duration.ofSeconds(2).toMillis())
}

class ThreadBasicsTests {

    @Test
    fun `thread create and synchronization`() {
        var anInteger = 0
        val th = Thread {
            anInteger = 1
        }
        th.start()
        th.join()
        assertEquals(1, anInteger)
    }

    @Test
    fun `creates start and join with new thread`() {
        val name = Thread.currentThread().name
        log.info("Starting test on thread '{}'", name)

        // We create a thread by creating a Thread object, passing in a method reference
        // Notice that we aren't calling `threadMethod`; we are passing a *reference* to the method into the
        // `Thread` constructor.
        val th = Thread(::threadMethod)
        log.info("New thread created but not yet started")

        // By default, threads create by directly calling the constructors are not ready to run after they are created.
        // Only after Thread#start is called is the thread considered in the "ready" state.
        th.start()
        log.info("New thread started, waiting for it to end")

        // The Thread#join can be used to synchronize with the thread termination.
        // Thread#join will only return after
        // - the *referenced* thread ends
        // - or the *calling* thread is interrupted
        // - or the optional timeout elapses

        th.join()
        log.info("New thread ended, finishing test")
    }
    // When running this example, notice:
    // - The log messages contain the thread name in bracket.
    // - The test method is started on a "main" (or "Test worker") thread.
    // - However the log inside `threadMethod` is issued on a "Thread-" thread

    @Test
    fun `we can have multiple threads running the same method`() {
        val name = Thread.currentThread().name
        log.info("Starting test on thread '{}'", name)

        // We can create multiples threads referencing the same method
        val ths = listOf(
            Thread(::threadMethod),
            Thread(::threadMethod),
            Thread(::threadMethod)
        )
        ths.forEach { thread -> thread.start() }
        log.info("New threads started, waiting for them to end")

        ths.forEach { thread -> thread.join() }
        log.info("New threads ended, finishing test")
    }

    @Test
    fun `create thread using a lambda`() {
        val localVariableOfMainThread = 42
        log.info("Starting test on thread '{}'", Thread.currentThread().name)

        // Threads can be created by providing a lambda expression (here we use Kotlin's trailing lambda syntax)
        // Note that a lambda expression can use variables from the *enclosing scope*,
        // such as `localVariableOfMainThread`
        // This is simultaneously useful and dangerous, since those *local* variables will now
        // be accessible from *multiple* threads.
        val th = Thread {
            // Notice how in this thread we are using a local variable from a different thread,
            // (the main thread).
            log.info(
                "Running on thread '{}', localVariableOfMainThread = {}",
                Thread.currentThread().name,
                localVariableOfMainThread
            )
            Thread.sleep(Duration.ofSeconds(2).toMillis())
        }
        th.start()
        th.join()
        log.info("New thread ended, finishing test")
    }

    @Test
    fun `create thread using a lambda and a mutable shared variable`() {
        var localVariableOfMainThread = 42
        log.info("Starting test on thread '{}'", Thread.currentThread().name)

        // Threads can be created by providing a lambda expression
        // Note that a lambda expression can use variables from the *enclosing scope*,
        // such as `localVariableOfMainThread`
        // This is simultaneously useful and dangerous, since those *local* variables will now
        // be accessible from *multiple* threads.
        val th = Thread {
            // Notice how in this thread we are using a local variable from a different thread,
            // (the main thread).
            log.info(
                "Running on thread '{}', localVariableOfMainThread = {}",
                Thread.currentThread().name,
                localVariableOfMainThread
            )
            Thread.sleep(Duration.ofSeconds(2).toMillis())
        }
        th.start()
        // Here we mutate `localVariableOfMainThread` "at the same time" the created thread observes that field
        // So, what will be the observed value? 42 or 43?
        // Do we have any guarantees that the same value will be observed for all runs of this test?
        localVariableOfMainThread = 43
        th.join()
        log.info("New thread ended, finishing test")
    }

    // Threads can also be defined by deriving from the Thread class (this is the JVM after all)
    internal class MyThread : Thread() {
        override fun run() {
            log.info("Running on thread MyThread - '{}'", currentThread().name)
            sleep(Duration.ofSeconds(2).toMillis())
        }
    }

    @Test
    fun `create thread using derived classes`() {
        log.info("Starting test on thread '{}'", Thread.currentThread().name)
        val th = MyThread()
        th.start()
        th.join()
        log.info("New thread ended, finishing test")
    }

    // All the above techniques create *platform* threads.
    // To create *virtual* threads:
    // - Use the Thread#startVirtualThread.
    // - Obtain a `Thread.Builder` that creates virtual threads (e.g. via Thread.ofVirtual)
    //   and use of the methods in this interface
    @Test
    fun `create virtual thread`() {
        val th1 = Thread.startVirtualThread {
            log.info("Am I a virtual thread? {}", Thread.currentThread().isVirtual)
        }
        val builder: Thread.Builder = Thread.ofVirtual()
        val th2 = builder.start {
            log.info("Am I also a virtual thread? {}", Thread.currentThread().isVirtual)
        }
        th1.join()
        th2.join()
    }

    @Test
    fun `virtual threads cannot be set to be non-daemon`() {
        val builder: Thread.Builder = Thread.ofVirtual()
        val th1 = builder.unstarted {
            log.info("Am I still a daemon thread? {}", Thread.currentThread().isDaemon)
        }
        log.info("th1.isDaemon: {}", th1.isDaemon)
        try {
            th1.setDaemon(false)
        } catch (ex: IllegalArgumentException) {
            log.info("Cannot set daemon to false")
        }
        th1.start()
        th1.join()
    }
}