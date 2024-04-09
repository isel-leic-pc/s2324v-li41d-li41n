// To access the *non-public* Continuation API
// ONLY for learning purposes
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package pt.isel.pc.loom

import jdk.internal.vm.Continuation
import jdk.internal.vm.ContinuationScope
import org.slf4j.LoggerFactory
import kotlin.test.Test

class ContinuationTests {

    @Test
    fun first() {
        val scope = ContinuationScope("the-scope")
        val runnable: Runnable = Runnable {
            logger.info("step 1")
            Continuation.yield(scope)
            logger.info("step 2")
            Continuation.yield(scope)
            logger.info("step 3")
        }

        val c = Continuation(scope, runnable)
        while (!c.isDone) {
            c.run()
            logger.info("After run")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContinuationTests::class.java)
    }
}