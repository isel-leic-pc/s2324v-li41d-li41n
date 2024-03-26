package pt.isel.pc.sketches.leic51d.sync

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Simple thread pool with a dynamic number of worker threads.
 * - Initially the pool starts with zero worker threads.
 * - Worker threads are created on-demand until reaching maxThreads.
 * - Worker threads are terminated if there isn't any work item available.
 */
class VerySimpleThreadPool(
    private val maxThreads: Int,
) {
    private val lock = ReentrantLock()
    private var nOfThreads: Int = 0
    private var workItems = NodeLinkedList<Runnable>()

    init {
        require(maxThreads > 0) { "maxThreads must be greater than zero" }
    }

    fun execute(workItem: Runnable) {
        lock.withLock {
            if (nOfThreads < maxThreads) {
                Thread.ofPlatform().start {
                    workItem.run()
                }
                nOfThreads += 1
            } else {
                workItems.enqueue(workItem)
            }
        }
    }

    private fun getNextWorkItem(): GetNextWorkItemResult {
        lock.withLock {
            if (workItems.notEmpty) {
                return GetNextWorkItemResult.WorkItem(workItems.pull().value)
            } else {
                nOfThreads -= 1
                return GetNextWorkItemResult.Exit
            }
        }
    }

    private fun workerThreadLoop(initialWorkItem: Runnable) {
        var workItem = initialWorkItem
        while (true) {
            safeRun(workItem)
            workItem = when (val res = getNextWorkItem()) {
                is GetNextWorkItemResult.WorkItem -> res.item
                GetNextWorkItemResult.Exit -> return
            }
        }
    }

    private sealed interface GetNextWorkItemResult {
        data class WorkItem(val item: Runnable) : GetNextWorkItemResult
        data object Exit : GetNextWorkItemResult
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VerySimpleThreadPool::class.java)
        private fun safeRun(runnable: Runnable) {
            try {
                runnable.run()
            } catch (ex: Throwable) {
                logger.warn("Unexpected exception, ignoring it to keeping worker thread alive")
                // ignore
            }
        }
    }
}