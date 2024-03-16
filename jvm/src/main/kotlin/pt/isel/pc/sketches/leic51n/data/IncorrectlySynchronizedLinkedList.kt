package pt.isel.pc.sketches.leic51n.data

import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class IncorrectlySynchronizedLinkedList<T> : Iterable<T> {

    private val lock = ReentrantLock()
    private val list = LinkedList<T>()

    fun add(elem: T) = lock.withLock {
        list.add(elem)
    }

    fun removeFirst(): T? = lock.withLock {
        return list.removeFirst()
    }

    // Incorrect
    override fun iterator(): Iterator<T> = lock.withLock {
        return list.iterator()
    }
}