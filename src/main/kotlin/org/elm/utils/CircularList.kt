package org.elm.utils

class CircularList<out T>(val list: List<T>) {

    private val maxIndex = list.size - 1

    private var index: Int = 0

    fun get(): T = list[index]

    fun set(i: Int) {
        index = i
    }

    fun next(): T {
        safelyAdd(1)
        return get()
    }

    fun prev(): T {
        safelyAdd(-1)
        return get()
    }

    private fun safelyAdd(n: Int) {
        index += n
        when {
            index > maxIndex -> index = 0
            index < 0 -> index = maxIndex
        }
    }

    fun isEmpty(): Boolean = list.isEmpty()

}

/*
fun main() {
    val circularList = CircularList(listOf("a", "b", "c"))
    println(circularList.get())
    println(circularList.next())
    println(circularList.next())
    println(circularList.next())
    println(circularList.prev())
    println(circularList.prev())
    println(circularList.prev())
    println(circularList.prev())
}
*/
