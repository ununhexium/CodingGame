package net.lab0.coding.game.nospoon2

data class Quad(
    val up: Pair<Int, Int>?,
    val down: Pair<Int, Int>?,
    val left: Pair<Int, Int>?,
    val right: Pair<Int, Int>?
) {
    constructor(vararg pairs: Pair<Int, Int>?) :
        this(pairs[0], pairs[1], pairs[2], pairs[3])

    val all
        get() = listOf(up, down, left, right)
}
