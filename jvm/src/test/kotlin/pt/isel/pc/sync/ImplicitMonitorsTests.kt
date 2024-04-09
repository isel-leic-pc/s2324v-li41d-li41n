package pt.isel.pc.sync

import org.slf4j.LoggerFactory
import kotlin.test.Test

class ImplicitMonitorsTests {

    class AClassWithSynchronizedMethods {

        @Synchronized
        fun f1() {
            logger.warn("starting f1")
            Thread.sleep(500)
            logger.warn("ending f1")
        }

        @Synchronized
        fun f2() {
            logger.warn("starting f2")
            Thread.sleep(500)
            logger.warn("ending f2")
        }

        fun f3() {
            synchronized(this) {
                logger.warn("starting f3")
                Thread.sleep(500)
                logger.warn("ending f3")
            }
        }
    }

    @Test
    fun `using synchronized methods`() {
        val theObject = AClassWithSynchronizedMethods()
        val t1 = Thread.ofPlatform().start {
            theObject.f1()
        }
        val t2 = Thread.ofPlatform().start {
            theObject.f2()
        }
        val t3 = Thread.ofPlatform().start {
            theObject.f3()
        }
        t1.join()
        t2.join()
        t3.join()
    }

    class AClassUsingIntrincicMonitors {

        private val monitor = Object()

        fun demoNotify() {
            synchronized(monitor) {
                logger.info("notifyAll")
                monitor.notifyAll()
            }
        }

        fun demoWait() {
            synchronized(monitor) {
                logger.info("Before wait")
                monitor.wait()
                logger.info("After wait")
            }
        }
    }

    @Test
    fun `using intrinsic monitor`() {
        val theObject = AClassUsingIntrincicMonitors()

        val t1 = Thread.ofPlatform().start {
            theObject.demoWait()
        }
        Thread.sleep(500)
        theObject.demoNotify()
        t1.join()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImplicitMonitorsTests::class.java)
    }
}