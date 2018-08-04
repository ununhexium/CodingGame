package net.lab0.coding.game.nospoon2

import kotlin.math.max
import kotlin.math.min

typealias Nodes = List<List<Node?>>

fun <T> List<T>.indexOfFirstAfter(index: Int, predicate: (T) -> Boolean) =
    with(this.subList(index + 1, this.size).indexOfFirst(predicate)) {
        if (this == -1) this
        else this + index + 1
    }

fun <T> List<T>.indexOfLastBefore(index: Int, predicate: (T) -> Boolean) =
    this.subList(0, index).indexOfLast(predicate)


fun autoRange(a: Int, b: Int) = (min(a, b)..max(a, b))

fun IntRange.shrinkBy(start:Int, end:Int) = (this.start + start..this.endInclusive - end)
