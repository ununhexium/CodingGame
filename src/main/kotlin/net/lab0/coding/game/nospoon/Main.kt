package net.lab0.coding.game.nospoon

import java.util.Scanner

typealias Grid = List<List<Boolean>>

/**
 * https://www.codingame.com/ide/puzzle/there-is-no-spoon-episode-1
 */
fun main(args: Array<String>) {
  val input = Scanner(System.`in`)
  val width = input.nextInt() // the number of cells on the X axis
  val height = input.nextInt() // the number of cells on the Y axis
  if (input.hasNextLine()) {
    input.nextLine()
  }
  val raw = (0 until height).map {
    input.nextLine()
  }

  val grid = parse(raw)
  val result = solve(grid)

  // Three coordinates: a node, its right neighbor, its bottom neighbor
  result.forEach {it: List<Int> ->
    println(it.joinToString(separator = " "))
  }
}

fun parse(raw: List<String>) =
    raw.map {
      it.map {
        when (it) {
          '0' -> true
          '.' -> false
          else -> IllegalStateException("Nope :/")
        } as Boolean
      }
    }

fun solve(grid: Grid) =
    grid.mapIndexed { y, line ->
      line.mapIndexedNotNull { x, _ ->
        if (grid[y][x]) {
          val (x2, y2) = findToRight(grid, x, y)
          val (x3, y3) = findToBottom(grid, x, y)
          listOf(x, y, x2, y2, x3, y3)
        }
        else null
      }
    }.flatMap { it }

fun findToBottom(grid: Grid, x: Int, y: Int) =
    with(grid.map { it[x] }.indexOfFirstAfter(y) { it }) {
      if (this == -1) -1 to -1
      else x to this
    }

fun findToRight(grid: Grid, x: Int, y: Int) =
    with(grid[y].indexOfFirstAfter(x) { it }) {
      if (this == -1) -1 to -1
      else this to y
    }

fun <T> List<T>.indexOfFirstAfter(index: Int, predicate: (T) -> Boolean) =
    with(this.drop(index + 1).indexOfFirst(predicate)) {
      if (this == -1) this
      else this + index + 1
    }
