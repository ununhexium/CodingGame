package net.lab0.coding.game.nospoon2

import net.lab0.tools.delegated.SetOnce
import java.util.Scanner

class ThereIsNoSpoon2 {

    var width: Int by SetOnce()
    var height: Int by SetOnce()

    fun main(args: Array<String>) {
        val input = Scanner(System.`in`)
        width = input.nextInt() // the number of cells on the X axis
        height = input.nextInt() // the number of cells on the Y axis
        if (input.hasNextLine()) {
            input.nextLine()
        }
        val raw = (0 until height).map {
            input.nextLine()
        }

        val grid = Grid(parse(raw))
    }

    fun parse(raw: List<String>) =
        raw.mapIndexed { y, line ->
            line.mapIndexed { x, char ->
                when (char) {
                    in '1'..'8' -> Node(x, y, char.toInt() - '0'.toInt())
                    '.' -> null
                    else -> throw IllegalStateException("Nope :/")
                }
            }
        }

    fun makePair(x: Int, y: Int) =
        if (x == -1 || y == -1) null
        else x to y

    fun Nodes.column(index: Int) =
        this.map {
            it[index]
        }

    fun Nodes.line(index: Int) =
        this[index]

    fun findNeighbours(grid: Grid, x: Int, y: Int): Quad {
        val up = grid.nodes.column(x).indexOfLastBefore(y) { it != null }
        val down = grid.nodes.column(x).indexOfFirstAfter(y) { it != null }
        val left = grid.nodes.line(y).indexOfLastBefore(x) { it != null }
        val right = grid.nodes.line(y).indexOfFirstAfter(x) { it != null }

        val upPair = makePair(x, up)
        val downPair = makePair(x, down)
        val leftPair = makePair(left, y)
        val rightPair = makePair(right, y)

        val start = x to y
        val candidates = listOf(
            upPair, downPair, leftPair, rightPair
        ).map {
            when {
                it == null -> null
                crossesExistingLink(grid, start, it) -> null
                else -> it
            }
        }

        return Quad(*candidates.toTypedArray())
    }

    fun crossesExistingLink(
        grid: Grid,
        start: Pair<Int, Int>,
        end: Pair<Int, Int>
    ) =
        when {
            start.first == end.first ->
                autoRange(start.second, end.second).shrinkBy(1, 1).map {
                    grid.busy(start.first, it)
                }.any { it }
            start.second == end.second ->
                autoRange(start.first, end.first).shrinkBy(1, 1).map {
                    grid.busy(it, start.second)
                }.any { it }
            else -> throw IllegalArgumentException("Start and end must have common X or Y")
        }

    fun buildTree(
        grid: Grid,
        startAt: Pair<Int, Int> = grid.firstNode()
    ): Boolean {

    }
}

