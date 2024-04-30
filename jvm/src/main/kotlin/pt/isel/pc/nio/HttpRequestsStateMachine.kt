package pt.isel.pc.nio

import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class HttpRequestsStateMachine(
    private val continuation: CompletionHandler<String, Void?>,
    private val path: String,
) {
    // Instance fields
    private lateinit var socketChannel: AsynchronousSocketChannel
    private lateinit var requestString: String
    private lateinit var requestByteBuffer: ByteBuffer
    private var remainingBytesToWrite = 0
    private lateinit var responseByteArray: ByteArray
    private lateinit var responseByteBuffer: ByteBuffer
    private lateinit var responseBytes: ByteArrayOutputStream
    private lateinit var completeResponseArray: ByteArray

    fun start() {
        run(state0())
    }

    private fun run(nextState: NextState) {
        var state = nextState
        while (true) {
            logger.info("Dispatcher calling state: {}", state)
            try {
                state = when (state) {
                    NextState.State1 -> state1()
                    NextState.State2 -> state2()
                    is NextState.State3 -> state3(state.writtenBytesLen)
                    NextState.State4 -> state4()
                    NextState.State5 -> state5()
                    is NextState.State6 -> state6(state.readBytesLen)
                    NextState.State7 -> state7()
                    is NextState.Finally -> stateFinally(state.error)
                    NextState.Suspend -> break
                    NextState.Done -> break
                }
            } catch (ex: Exception) {
                if (state !is NextState.Finally) {
                    stateFinally(ex)
                } else {
                    continuation.failed(ex, null)
                    break
                }
            }
        }
    }

    private fun state0(): NextState {
        socketChannel = AsynchronousSocketChannel.open()
        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4)
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4)

        return startAsynchronous(
            { success -> NextState.State1 },
            { exc -> NextState.Finally(exc) },
            { continuation ->
                socketChannel.connect(
                    InetSocketAddress("httpbin.org", 80),
                    null,
                    continuation
                )
            }
        )
    }

    private fun state1(): NextState {
        requestString = "GET $path HTTP/1.1\r\nConnection:close\r\nHost:httpbin.org\r\n\r\n"
        requestByteBuffer = ByteBuffer.wrap(requestString.toByteArray())
        remainingBytesToWrite = requestByteBuffer.limit()
        return if (remainingBytesToWrite != 0) {
            NextState.State2
        } else {
            NextState.State4
        }
    }

    private fun state2(): NextState =
        startAsynchronous(
            { value -> NextState.State3(value) },
            { error -> NextState.Finally(error) },
            { completionHandler ->
                socketChannel.write(requestByteBuffer, null, completionHandler)
            }
        )

    private fun state3(writtenBytesLen: Int): NextState {
        logger.info("Write {}", writtenBytesLen)
        remainingBytesToWrite -= writtenBytesLen
        return if (remainingBytesToWrite != 0) {
            NextState.State2
        } else {
            NextState.State4
        }
    }

    private fun state4(): NextState {
        responseByteArray = ByteArray(8)
        responseByteBuffer = ByteBuffer.wrap(responseByteArray)
        responseBytes = ByteArrayOutputStream()
        return NextState.State5
    }

    private fun state5(): NextState =
        startAsynchronous(
            { NextState.State6(it) },
            { NextState.Finally(it) },
            { completionHandler ->
                socketChannel.read(responseByteBuffer, null, completionHandler)
            }
        )

    private fun state6(readBytesLen: Int): NextState {
        logger.info("Read {}", readBytesLen)
        if (readBytesLen < 0) {
            return NextState.State7
        }
        responseBytes.write(responseByteArray, 0, readBytesLen)
        responseByteBuffer.clear()
        return NextState.State5
    }

    private fun state7(): NextState {
        completeResponseArray = responseBytes.toByteArray()
        return NextState.Finally(null)
    }

    private fun stateFinally(exc: Throwable?): NextState {
        socketChannel.close()
        if (exc != null) {
            continuation.failed(exc, null)
        } else {
            continuation.completed(
                String(completeResponseArray, 0, completeResponseArray.size),
                null
            )
        }
        return NextState.Done
    }

    private fun <T> startAsynchronous(
        nextStateForResult: (T) -> NextState,
        nextStateForFailure: (Throwable) -> NextState,
        block: (CompletionHandler<T, Void?>) -> Unit,
    ): NextState {
        val completionHandler = object : CompletionHandler<T, Void?> {
            override fun completed(result: T, attachment: Void?) {
                val holder = tls.get()
                if (holder != null) {
                    holder.value = result
                } else {
                    run(nextStateForResult(result))
                }
            }

            override fun failed(exc: Throwable, attachment: Void?) {
                val holder = tls.get()
                if (holder != null) {
                    holder.error = exc
                } else {
                    run(nextStateForFailure(exc))
                }
            }
        }
        val holder = Holder()
        tls.set(holder)
        try {
            block(completionHandler)
        } catch (ex: Throwable) {
            tls.set(null)
            return nextStateForFailure(ex)
        }
        tls.set(null)
        val value = holder.value
        val error = holder.error
        return if (value != null) {
            logger.info("Completed synchronously")
            nextStateForResult(value as T)
        } else if (error != null) {
            nextStateForFailure(error)
        } else {
            NextState.Suspend
        }
    }

    private sealed interface NextState {
        data object Suspend : NextState
        data object Done : NextState
        data object State1 : NextState
        data object State2 : NextState
        data class State3(val writtenBytesLen: Int) : NextState
        data object State4 : NextState
        data object State5 : NextState
        data class State6(val readBytesLen: Int) : NextState
        data object State7 : NextState
        data class Finally(val error: Throwable?) : NextState
    }

    private class Holder {
        var value: Any? = null
        var error: Throwable? = null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HttpRequestsStateMachine::class.java)
        private val tls = ThreadLocal<Holder?>()
    }
}

fun getHttpBin(path: String, continuation: CompletionHandler<String, Void?>) {
    val stateMachine = HttpRequestsStateMachine(
        continuation,
        path
    )
    stateMachine.start()
}