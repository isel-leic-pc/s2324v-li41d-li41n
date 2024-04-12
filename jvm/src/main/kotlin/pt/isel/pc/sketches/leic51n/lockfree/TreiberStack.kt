package pt.isel.pc.sketches.leic51n.lockfree

import java.util.concurrent.atomic.AtomicReference

class TreiberStack<T> {

    private class Node<T>(
        val value: T,
        val next: Node<T>?,
    )

    private val head = AtomicReference<Node<T>?>()

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
            val observedHead = head.get() ?: return null
            val next = observedHead.next
            if (head.compareAndSet(observedHead, next)) {
                return observedHead.value
            }
        }
    }
}