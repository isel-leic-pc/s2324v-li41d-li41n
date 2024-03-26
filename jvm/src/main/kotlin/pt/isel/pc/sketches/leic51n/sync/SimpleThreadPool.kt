package pt.isel.pc.sketches.leic51n.sync

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A simple thread pool
 * - [maxThreads] defines the maximum number of threads.
 * - Starts with no threads, i.e., threads created on-demand.
 * - Threads have zero time-to-live, end if not additional work items are available
 */
class SimpleThreadPool(
    private val maxThreads: Long,
) {
    private val lock = ReentrantLock()
    private val workItems = NodeLinkedList<Runnable>()

    // Invariant: nOfThreads <= maxThreads
    private var nOfThreads = 0

    init {
        require(maxThreads > 0) { "maxThreads must be greater than zero" }
    }

    fun execute(workItem: Runnable) {
        when (val res = executeCore(workItem)) {
            is PostExecuteAction.CreateThread -> Thread.ofPlatform().start {
                workerThreadLoop(res.r)
            }

            PostExecuteAction.None -> {}
        }
    }

    private fun executeCore(workItem: Runnable): PostExecuteAction =
        lock.withLock {
            if (nOfThreads < maxThreads) {
                nOfThreads += 1
                PostExecuteAction.CreateThread(workItem)
            } else {
                workItems.enqueue(workItem)
                PostExecuteAction.None
            }
        }

    private sealed interface PostExecuteAction {
        data object None : PostExecuteAction
        data class CreateThread(val r: Runnable) : PostExecuteAction
    }

    private fun getNextItem(): Runnable? {
        lock.withLock {
            if (workItems.notEmpty) {
                val workItem = workItems.pull()
                return workItem.value
            } else {
                nOfThreads -= 1
                return null
            }
        }
    }

    private fun workerThreadLoop(firstRunnable: Runnable) {
        var runnable: Runnable = firstRunnable
        while (true) {
            safeRun(runnable)
            runnable = getNextItem() ?: return
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleThreadPool::class.java)
        private fun safeRun(runnable: Runnable) {
            try {
                runnable.run()
            } catch (ex: Exception) {
                logger.warn("Work item threw exception '{}', ignoring it", ex.message)
                // ignoring exception because the worker thread must not end
            }
        }
    }
}