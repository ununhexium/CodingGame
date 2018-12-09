package net.lab0.coding.game.xmasrush

import Board
import Direction.DOWN
import Direction.LEFT
import Direction.RIGHT
import Direction.UP
import Tile
import TileBuilder
import TileWithPositionLike
import X
import pickOne

object Helpers {

  fun asciiGridToTiles(grid: String): List<List<Tile>> =
      grid.split("\n").map { row ->
        row.map {
          with(TileBuilder()) {
            when (it) {
              ' ' -> this
              '.' -> this
              '╷' -> open(UP)
              '╵' -> open(DOWN)
              '╴' -> open(LEFT)
              '╶' -> open(RIGHT)
              '┘' -> open(UP, LEFT)
              '└' -> open(UP, RIGHT)
              '┐' -> open(DOWN, LEFT)
              '┌' -> open(DOWN, RIGHT)
              '│' -> open(UP, DOWN)
              '─' -> open(LEFT, RIGHT)
              '┬' -> openAll().close(UP)
              '┴' -> openAll().close(DOWN)
              '├' -> openAll().close(LEFT)
              '┤' -> openAll().close(RIGHT)
              '┼' -> openAll()
              else -> throw IllegalStateException("Squiggly lines are not supported")
            }
          }.build()
        }
      }

  fun asciiToSelection(board: Board, grid: String, selection: Char): Set<TileWithPositionLike> =
      with(mutableListOf<TileWithPositionLike>()) {
        grid.split("\n").mapIndexed { rowIdx, row ->
          row.mapIndexed { colIdx, char ->
            if (char == selection) {
              add(board[rowIdx X colIdx].withPosition(rowIdx X colIdx))
            }
          }
        }
        this.toSet()
      }

  fun asciiGridGenerator(size: Int): String {
    val sources = ".╷╵╴╶┘└┐┌│─┬┴├┤┼"
    val used = sources.drop(5)

    return (1..size).map {
      (1..size).map {
        used.toList().pickOne()
      }
    }.joinToString("\n") {
      it.joinToString("")
    }
  }
}


