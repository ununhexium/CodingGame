package net.lab0.coding.game.nospoon2

data class Node(val x:Int, val y:Int, val links: Int) {
    val asPair
        get() = x to y
}
