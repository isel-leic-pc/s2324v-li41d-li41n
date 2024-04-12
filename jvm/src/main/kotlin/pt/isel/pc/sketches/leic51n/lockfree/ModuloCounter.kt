package pt.isel.pc.sketches.leic51n.lockfree

import java.util.concurrent.atomic.AtomicInteger

class ModuloCounter(
    private val modulo: Int,
) {
    private val counter = AtomicInteger(0)
    fun incrementAndGet(): Int {
        while (true) {
            val observedValue = counter.get()
            val newValue = if (observedValue + 1 == modulo) {
                0
            } else {
                observedValue + 1
            }
            if (counter.compareAndSet(observedValue, newValue)) {
                return newValue
            }
        }
    }

    val value: Int
        get() = counter.get()
}