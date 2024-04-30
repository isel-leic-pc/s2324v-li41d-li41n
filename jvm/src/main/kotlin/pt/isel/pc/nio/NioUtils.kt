package pt.isel.pc.nio

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.CompletableFuture

fun AsynchronousSocketChannel.connect2(address: SocketAddress): CompletableFuture<Void> {
    val completableFuture = CompletableFuture<Void>()
    try {
        this.connect(
            address,
            Unit,
            object : CompletionHandler<Void, Unit> {
                override fun completed(result: Void?, attachment: Unit) {
                    completableFuture.complete(result)
                }

                override fun failed(exc: Throwable, attachment: Unit) {
                    completableFuture.completeExceptionally(exc)
                }
            }
        )
    } catch (ex: Throwable) {
        completableFuture.completeExceptionally(ex)
    }
    return completableFuture
}

fun AsynchronousSocketChannel.read2(dst: ByteBuffer): CompletableFuture<Int> {
    val completableFuture = CompletableFuture<Int>()
    try {
        this.read(
            dst,
            Unit,
            object : CompletionHandler<Int, Unit> {
                override fun completed(result: Int, attachment: Unit) {
                    completableFuture.complete(result)
                }

                override fun failed(exc: Throwable, attachment: Unit) {
                    completableFuture.completeExceptionally(exc)
                }
            }
        )
    } catch (ex: Throwable) {
        completableFuture.completeExceptionally(ex)
    }
    return completableFuture
}

fun AsynchronousSocketChannel.write2(src: ByteBuffer): CompletableFuture<Int> {
    val completableFuture = CompletableFuture<Int>()
    try {
        this.write(
            src,
            Unit,
            object : CompletionHandler<Int, Unit> {
                override fun completed(result: Int, attachment: Unit) {
                    completableFuture.complete(result)
                }

                override fun failed(exc: Throwable, attachment: Unit) {
                    completableFuture.completeExceptionally(exc)
                }
            }
        )
    } catch (ex: Throwable) {
        completableFuture.completeExceptionally(ex)
    }
    return completableFuture
}