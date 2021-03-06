package net.lab0.coding.game.xmasrush

import Xmas.Board
import Xmas.Direction.DOWN
import Xmas.Direction.LEFT
import Xmas.Direction.RIGHT
import Xmas.Direction.UP
import Xmas.Item
import Xmas.PlayerId.ME
import Xmas.Position
import Xmas.PositionLike
import Xmas.Quest
import Xmas.Tile
import Xmas.TileWithPositionLike
import Xmas.X
import net.lab0.coding.game.xmasrush.GameTest.GameBuilder
import net.lab0.coding.game.xmasrush.Helpers.asciiGridToTiles
import net.lab0.coding.game.xmasrush.Helpers.asciiToSelection
import org.assertj.core.api.Assertions.assertThat
import org.funktionale.currying.curried
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class BoardTest {
  companion object {
    val standard = (0..3).map { y ->
      (0..3).map { x ->
        x + 4 * y
      }
    }.joinToString("\n") { row ->
      row.joinToString(" ") {
        it.toString(2).padStart(4, '0')
      }
    }

    val pushTile = Xmas.Tile(false, false, false, false)

    val xMark = 'X'
  }

  @Test
  fun `read a 4x4 grid of tiles`() {
    val board = Xmas.Board(standard)
    assertThat(board.grid[0]).isEqualTo(
        listOf(Xmas.Tile("0000"), Xmas.Tile("0001"), Xmas.Tile("0010"), Tile("0011"))
    )
    assertThat(board.grid[3][3]).isEqualTo(Tile("1111"))
  }

  @TestFactory
  fun `push right`(): Iterable<DynamicTest> {

    val old = Board(standard)
    val (new, tile) = old.push(0, RIGHT, pushTile)

    return (1..3).map {
      dynamicTest("Row $it doesn't move") {
        assertThat(new.grid[it]).isEqualTo(old.grid[it])
      }
    } + dynamicTest("Row 0 shifted to the right") {
      // E -> A B C D
      val oldRow = old.grid[0]
      // E A B C -> D
      val newRow = new.grid[0]

      assertThat(newRow[0]).isSameAs(pushTile)
      assertThat(newRow[1]).isSameAs(oldRow[0])
      assertThat(newRow[2]).isSameAs(oldRow[1])
      assertThat(newRow[3]).isSameAs(oldRow[2])
      assertThat(tile).isSameAs(oldRow[3])
    }
  }

  @TestFactory
  fun `push left`(): Iterable<DynamicTest> {

    val old = Board(standard)
    val (new, tile) = old.push(0, LEFT, pushTile)

    return (1..3).map {
      dynamicTest("Row $it doesn't move") {
        assertThat(new.grid[it]).isEqualTo(old.grid[it])
      }
    } + dynamicTest("Row 0 shifted left") {
      // A B C D <- E
      val oldRow = old.grid[0]
      // A <- B C D E
      val newRow = new.grid[0]
      assertThat(newRow[0]).isSameAs(oldRow[1])
      assertThat(newRow[1]).isSameAs(oldRow[2])
      assertThat(newRow[2]).isSameAs(oldRow[3])
      assertThat(newRow[3]).isSameAs(pushTile)
      assertThat(tile).isSameAs(oldRow[0])
    }
  }

  val rowGetter = { board: Board, col: Int, row: Int ->
    board.grid[row][col]
  }.curried()

  val asCol = { board: Board, col: Int ->
    board.grid.map {
      it[col]
    }
  }.curried()

  @TestFactory
  fun `push up`(): Iterable<DynamicTest> {
    val old = Board(standard)
    val (new, tile) = old.push(0, UP, pushTile)

    return (1..3).map {
      dynamicTest("Column $it doesn't move") {
        assertThat(asCol(new)(it)).isEqualTo(asCol(old)(it))
      }
    } + dynamicTest("Column 0 shifted up") {
      /*
       * ```
       *   A
       *   ^
       * A B
       * B C
       * C D
       * D E
       * ^
       * E
       * ```
       */
      val oldGet = rowGetter(old)(0)
      val newGet = rowGetter(new)(0)
      assertThat(newGet(0)).isSameAs(oldGet(1))
      assertThat(newGet(1)).isSameAs(oldGet(2))
      assertThat(newGet(2)).isSameAs(oldGet(3))
      assertThat(newGet(3)).isSameAs(pushTile)
      assertThat(tile).isSameAs(oldGet(0))
    }
  }

  @TestFactory
  fun `push down`(): Iterable<DynamicTest> {
    val old = Board(standard)
    val (new, tile) = old.push(0, DOWN, pushTile)

    return (1..3).map {
      dynamicTest("Column $it doesn't move") {
        assertThat(asCol(new)(it)).isEqualTo(asCol(old)(it))
      }
    } + dynamicTest("Column 0 shifted down") {
      /*
       * ```
       * E
       * v
       * A E
       * B A
       * C B
       * D C
       *   v
       *   D
       * ```
       */
      val oldGet = rowGetter(old)(0)
      val newGet = rowGetter(new)(0)
      assertThat(newGet(0)).isSameAs(pushTile)
      assertThat(newGet(1)).isSameAs(oldGet(0))
      assertThat(newGet(2)).isSameAs(oldGet(1))
      assertThat(newGet(3)).isSameAs(oldGet(2))
      assertThat(tile).isSameAs(oldGet(3))
    }
  }

  // TODO: test the player's movements

  @TestFactory
  fun `cant move outside the grid`(): Iterable<DynamicTest> {
    val board = Board(
        (0..2).map {
          (0..2).map {
            Tile.plus
          }
        }
    )

    val movements = listOf(
        Position(0, 0) to setOf(DOWN, RIGHT),
        Position(2, 0) to setOf(UP, RIGHT),
        Position(0, 2) to setOf(DOWN, LEFT),
        Position(2, 2) to setOf(UP, LEFT)
    )

    return movements.map {
      dynamicTest(
          "At [${it.first.row};${it.first.col}] can go to ${it.second.joinToString()}"
      ) {
        assertThat(
            board.getAvailableAdjacentDirections(it.first.row, it.first.col)
        ).containsExactlyInAnyOrder(*it.second.toTypedArray())
      }
    }
  }

  val tester = { input: String, coordinates: String, code: Char, position: Position ->
    val board = Board(asciiGridToTiles(input))
    val selection = asciiToSelection(board, coordinates, code)
    println("Selection:")
    println(showSelection(coordinates, code))
    val accessible: Set<TileWithPositionLike> = board.getAccessibleTiles(position).allElements()
        .toSet()
    println("Accessible:")
    println(showAccessible(accessible))
    assertThat(accessible).containsExactlyInAnyOrderElementsOf(selection)
  }

  @TestFactory
  fun `can list accessible tiles - generated grid`(): List<DynamicTest> {
    // ╵ 	╶ 	└ 	╷ 	│ 	┌ 	├ 	╴ 	┘ 	─ 	┴ 	┐ 	┤ 	┬ 	┼

    val input = """
      > ┼┼.┴│.┐
      > ┤│.┘┼┬.
      > └│┬├┘│├
      > ┘┐┼┬└└├
      > ┤┘└┐┘┼┴
      > ┐┤─┬┌┐┴
      > ├┬└┤└┘└
    """.trimMargin("> ")

    println("Grid:")
    println(input)

    val coordinates = """
      |11  3 2
      |11  33
      |11 333
      |     3
      |
      |  5544
      |  5544
    """.trimMargin()

    return listOf(
        dynamicTest("Selection 1") {
          tester(input, coordinates, '1', 0 X 0)
        },
        dynamicTest("Selection 2") {
          tester(input, coordinates, '2', 0 X 6)
        },
        dynamicTest("Selection 3") {
          tester(input, coordinates, '3', 0 X 4)
        },
        dynamicTest("Selection 4") {
          tester(input, coordinates, '4', 5 X 4)
        },
        dynamicTest("Selection 5") {
          tester(input, coordinates, '5', 5 X 3)
        }
    )
  }

  @Test
  fun `check reachability 1`() {
    val grid = """
      >┼┼.┴│.┐
      >┤│.┘┼┬.
      >└│┬├┘│├
      >┘┐┼┬└└├
      >┤┘└┐┘┼┴
      >┐┤─┬┌┐┴
      >├┬└┤└┘└
    """.trimMargin(">")

    val accessible = """
      |11
      |11
      |11
      |
      |
      |
      |
    """.trimMargin()

    dynamicTest("Selection 1") {
      tester(grid, accessible, '1', 0 X 0)
    }
  }

  private fun showSelection(coordinates: String, code: Char, size: Int = 7) =
      addIndexes(
          coordinates.split("\n").map { row ->
            row.toList().map {
              if (it == code) xMark else ' '
            }.joinToString("")
          }.joinToString("\n") {
            it.padEnd(size, ' ')
          }
      )

  private fun showAccessible(accessible: Set<TileWithPositionLike>, size: Int = 7) =
      addIndexes(
          (0 until size).toList().joinToString("\n") { row ->
            (0 until size).toList().joinToString("") { col ->
              val accessibleHere = accessible.any { it.samePositionAs(row X col) }
              if (accessibleHere) xMark.toString() else " "
            }
          }
      )

  private fun addIndexes(rectangle: String): String {
    val rows = rectangle.split("\n")
    val size = rows[0].length
    val header = (" " + (0 until size).joinToString("") { it.toString() })

    val prefixed = rows.mapIndexed { idx, row ->
      idx.toString() + row
    }.joinToString("\n")

    return header + "\n" + prefixed
  }

  @TestFactory
  fun `can get edge tiles`(): Iterable<DynamicTest> {
    val grid = """
      >┬┬┬┌┘┼│
      >┘└┴├┼┴─
      >├├└├├│└
      >─┼─└─└┐
      >┘┌┴├│┬┌
      >┐└┼┤┘┴┐
      >┬├─┴┐─┼
    """.trimMargin(">")

    val board = Board(asciiGridToTiles(grid))
    val edgeTiles: List<TileWithPositionLike> = board.getEdgeTiles()

    val sizeTest = listOf(
        dynamicTest("Have all tiles") {
          assertThat(edgeTiles.toSet()).hasSize(6 * 4)
        }
    )
    val positionTests = edgeTiles.map {
      dynamicTest("Tile@${it.asPosition()}") {
        assertThat(
            listOf(
                it.asPosition().row == 0,
                it.asPosition().col == 0,
                it.asPosition().row == board.lastRow,
                it.asPosition().col == board.lastCol
            ).any {
              it
            }
        ).isTrue()
      }
    }

    return sizeTest + positionTests
  }

  @Test
  fun `can move as close as possible to an item`() {
    val grid = """
      >┼..┴│..
      >┤..┘┼┬.
      >│.┬├┘│.
      >│..┬└└.
      >┼───┘..
      >│.....┐
      >┴─────┴
    """.trimMargin(">")

    val item = Item("A", ME)
    val position = 4 X 6

    val game = GameBuilder()
        .withGrid(grid)
        .withMyPosition(0 X 0)
        .addItem(item, position)
        .addQuest(Quest(item.name, ME))
        .build()

    val path: List<PositionLike> = game.board.moveAsCloseAsPossible(game.me, position)
    assertThat(path.size).isLessThanOrEqualTo(20)
    assertThat(path.last().row).isEqualTo(5)
    assertThat(path.last().col).isEqualTo(6)
  }
}
