package net.lab0.coding.game.xmasrush

import Xmas.Board
import Xmas.Direction.DOWN
import Xmas.Direction.LEFT
import Xmas.Direction.RIGHT
import Xmas.Direction.UP
import Xmas.Tile
import Xmas.TileBuilder
import Xmas.TileWithPositionLike
import Xmas.X
import Xmas.pickOne

object Helpers {

  fun asciiGridToTiles(grid: String): List<List<Xmas.Tile>> =
      grid.split("\n").map { row ->
        row.map {
          with(Xmas.TileBuilder()) {
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

  fun asciiToSelection(board: Xmas.Board, grid: String, selection: Char): Set<Xmas.TileWithPositionLike> =
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


