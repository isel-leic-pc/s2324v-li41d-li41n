package pt.isel.pc.sketches.leic51d.lockfree

import java.util.concurrent.atomic.AtomicInteger

class ModuloCounter(
    val modulo: Int,
) {

    private val counter = AtomicInteger()

    fun incAndGetPreviousValue(): Int {
        while (true) {
            val observedValue = counter.get()
            val nextValue = if (observedValue + 1 < modulo) {
                observedValue + 1
            } else {
                0
            }
            if (counter.compareAndSet(observedValue, nextValue)) {
                return observedValue
            }
        }
    }

    fun get(): Int = counter.get()
}