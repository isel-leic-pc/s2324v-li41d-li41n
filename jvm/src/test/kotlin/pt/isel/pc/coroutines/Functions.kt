package pt.isel.pc.coroutines

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.Instant

suspend fun f1(msg: String): Int {
    val now = Instant.now()
    logger.info("f1 started: $msg")
    delay(1000)
    logger.info("f1 in the middle, {}", now)
    delay(1000)
    logger.info("f1 ending")
    return 42
}

private val logger = LoggerFactory.getLogger("Functions")