package net.lab0.coding.game.xmasrush

import Board
import Direction.DOWN
import Direction.LEFT
import Direction.RIGHT
import Direction.UP
import Position
import Tile
import X
import org.assertj.core.api.Assertions.assertThat
import org.funktionale.currying.curried
import org.junit.jupiter.api.DynamicTest
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

    val pushTile = Tile(false, false, false, false)
  }

  @Test
  fun `read a 4x4 grid of tiles`() {
    val board = Board(standard)
    assertThat(board.grid[0]).isEqualTo(
        listOf(Tile("0000"), Tile("0001"), Tile("0010"), Tile("0011"))
    )
    assertThat(board.grid[3][3]).isEqualTo(Tile("1111"))
  }

  @TestFactory
  fun `push right`(): Iterable<DynamicTest> {

    val old = Board(standard)
    val (new, tile) = old.push(0, RIGHT, pushTile)

    return (1..3).map {
      DynamicTest.dynamicTest("Row $it doesn't move") {
        assertThat(new.grid[it]).isEqualTo(old.grid[it])
      }
    } + DynamicTest.dynamicTest("Row 0 shifted to the right") {
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
      DynamicTest.dynamicTest("Row $it doesn't move") {
        assertThat(new.grid[it]).isEqualTo(old.grid[it])
      }
    } + DynamicTest.dynamicTest("Row 0 shifted left") {
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
      DynamicTest.dynamicTest("Column $it doesn't move") {
        assertThat(asCol(new)(it)).isEqualTo(asCol(old)(it))
      }
    } + DynamicTest.dynamicTest("Column 0 shifted up") {
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
      DynamicTest.dynamicTest("Column $it doesn't move") {
        assertThat(asCol(new)(it)).isEqualTo(asCol(old)(it))
      }
    } + DynamicTest.dynamicTest("Column 0 shifted down") {
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
      DynamicTest.dynamicTest(
          "At [${it.first.row};${it.first.col}] can go to ${it.second.joinToString()}"
      ) {
        assertThat(
            board.getAvailableMoveDirections(it.first.row, it.first.col)
        ).containsExactlyInAnyOrder(*it.second.toTypedArray())
      }
    }
  }

  @TestFactory
  fun `can move by 1 tile`(): Iterable<DynamicTest> {
    val board = Board(
        (0..1).map {
          (0..1).map {
            Tile.plus
          }
        }
    )

    return listOf(
        DynamicTest.dynamicTest("to the left"){
          board.canMove(0 X 0, 0 X 1)
        },
        DynamicTest.dynamicTest("to the right"){
          board.canMove(0 X 1, 0 X 0)
        },
        DynamicTest.dynamicTest("up"){
          board.canMove(1 X 0, 0 X 0)
        },
        DynamicTest.dynamicTest("down"){
          board.canMove(0 X 0, 1 X 0)
        }
    )
  }
}
