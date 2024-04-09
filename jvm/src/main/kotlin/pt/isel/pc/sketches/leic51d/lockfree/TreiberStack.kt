package pt.isel.pc.sketches.leic51d.lockfree

import java.util.concurrent.atomic.AtomicReference

class TreiberStack<T> {

    private class Node<T>(
        val value: T,
        val next: Node<T>?,
    )

    private val head: AtomicReference<Node<T>?> = AtomicReference()

    fun push(value: T) {
        while (true) {
            val observedHead = head.get()
            val node = Node<T>(value, observedHead)
            if (head.compareAndSet(observedHead, node)) {
                return
            }
        }
    }

    fun pop(): T? {
        while (true) {
            val observedHead = head.get()
            val observedFirst = observedHead ?: return null
            if (head.compareAndSet(observedHead, observedFirst.next)) {
                return observedFirst.value
            }
        }
    }
}