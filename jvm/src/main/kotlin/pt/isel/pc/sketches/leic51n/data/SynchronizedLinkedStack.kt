package pt.isel.pc.sketches.leic51n.data

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SynchronizedLinkedStack<T> {

    private class Node<T>(val item: T, val next: Node<T>?)

    // mutable
    private val lock: Lock = ReentrantLock()
    private var head: Node<T>? = null
    private var size: Int = 0

    fun push(value: T) = lock.withLock {
        head = Node(item = value, next = head)
        size += 1
    }

    fun pop(): T? = lock.withLock {
        val observedHead = head ?: return null
        head = observedHead.next
        size -= 1
        return observedHead.item
    }

    val isEmpty: Boolean
        get() {
            lock.lock()
            val res = head == null
            lock.unlock()
            return res
        }
}