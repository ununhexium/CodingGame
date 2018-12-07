import Direction.DOWN
import Direction.LEFT
import Direction.RIGHT
import Direction.UP
import TurnType.MOVE
import TurnType.PUSH
import java.util.Random
import java.util.Scanner
import kotlin.math.abs

/**
 *

Note: The player's id is always 0 and the opponent's 1.
Output for one PUSH game turn

Output for one MOVE game turn

MOVE direction where direction can be UP, DOWN, LEFT or RIGHT.
PASS to skip moving this turn.

A MOVE can contain up to 20 directions, each direction separated by a space  .
Example: MOVE LEFT UP RIGHT will make the player move left, then up, then right.
Constraints
board width = 7
board height = 7
numPlayerCards = 1
0 ≤ numItems ≤ 2
0 ≤ numQuests ≤ 2

Response time for the first turn ≤ 1s
Response time per turn ≤ 50ms
 */

object Dice : Random() {
  fun <T> pickOne(possible: List<T>): T =
      possible[this.nextInt(possible.size)]
}

fun <T> Array<T>.pickOne() = this[Dice.nextInt(this.size)]

val DEBUG_PARSING = false
val DEBUG_MOVE = true

fun debug(s: String) = System.err.println(s)
fun debugParsing(s: String) {
  if (DEBUG_PARSING) debug(s)
}

fun debugMove(s: String) {
  if (DEBUG_MOVE) debug(s)
}

fun doPush() {
  debug("Push")
  push(Dice.nextInt(7), Direction.values().pickOne())
}

fun doMove(board: Board, player: Player) {
  debug("Move")
  val directions = board.getAvailableMoveDirections(player.row, player.col)

  debugMove("Available directions: $directions")
  val possible = directions.filter {
    val from = player.row X player.col
    val to = player.toPosition().translatedPositionTo(it)
    board[from].moveTowards(it, board[to])
  }

  debugMove("Possible directions: $possible")

  if (possible.isEmpty()) pass()
  else move(Dice.pickOne(possible))
}

/**
 * Help the Christmas elves fetch presents in a magical labyrinth!
 **/
fun main(args: Array<String>) {
  val input = Scanner(System.`in`)

  // game loop
  while (true) {
    debugParsing("Loop")
    val turnType = if (input.nextInt() == 0) PUSH else MOVE

    input.nextLine()
    debugParsing("Board")
    val gridInput = (1..7).joinToString("\n") {
      val row = input.nextLine()
      debugParsing(row)
      row
    }

    debugParsing("Grid input string $gridInput")
    val board = Board(gridInput)

    debugParsing("Players")
    val me = parsePlayer(input)
    val foe = parsePlayer(input)

    debugParsing("Items")
    val numItems = input.nextInt() // the total number of items available on board and on player tiles
    for (i in 0 until numItems) {
      val itemName = input.next()
      val itemX = input.nextInt()
      val itemY = input.nextInt()
      val itemPlayerId = input.nextInt()
      val item = Item(itemName, itemPlayerId)

      when (itemX) {
        -1 -> me.tile.item = item
        -2 -> foe.tile.item = item
        else -> board.grid[itemY][itemX].item = item
      }
    }

    debugParsing("Quests")
    val numQuests = input.nextInt() // the total number of revealed quests for both players
    val quests = (1..numQuests).map {
      Quest(input.next(), PlayerId.from(input.nextInt()))
    }

    // Write an action using println()
    debugParsing("Actions")
    when (turnType) {
      PUSH -> doPush()
      MOVE -> doMove(board, me)
    }
  }
}

private fun parsePlayer(input: Scanner): Player {
  return Player(
      input.nextInt(),
      input.nextInt(),
      input.nextInt(),
      Tile(input.next())
  )
}

enum class TurnType {
  PUSH,
  MOVE
}

data class Tile(
    val up: Boolean,
    val right: Boolean,
    val down: Boolean,
    val left: Boolean,
    var item: Item? = null
) {

  companion object {
    /**
     * Builds a new tile. Defaults to all closed.
     */
    class Builder(
        var up: Boolean = false,
        var down: Boolean = false,
        var left: Boolean = false,
        var right: Boolean = false
    ) {

      fun build() = Tile(up, right, down, left)

      fun allOpen(): Builder {
        open(UP)
        open(DOWN)
        open(LEFT)
        open(RIGHT)
        return this
      }

      fun open(vararg direction: Direction): Builder {
        direction.forEach {
          when (it) {
            UP -> up = true
            DOWN -> down = true
            LEFT -> left = true
            RIGHT -> right = true
          }
        }
        return this
      }

      fun close(vararg direction: Direction): Builder {
        direction.forEach {
          when (it) {
            UP -> up = false
            DOWN -> down = false
            LEFT -> left = false
            RIGHT -> right = false
          }
        }

        return this
      }
    }

    val plus = Builder().allOpen().build()
    val minus = Builder().open(LEFT, RIGHT).build()
    val pipe = Builder().open(UP, DOWN).build()
  }

  constructor(tileString: String) : this(
      tileString[0] == '1',
      tileString[1] == '1',
      tileString[2] == '1',
      tileString[3] == '1'
  )

  fun moveTowards(direction: Direction, tile: Tile) =
      when (direction) {
        UP -> moveUpTowards(tile)
        DOWN -> moveDownTo(tile)
        LEFT -> moveLeftTo(tile)
        RIGHT -> moveRightTo(tile)
      }

  fun moveUpTowards(tile: Tile) =
      this.up && tile.down

  fun moveDownTo(tile: Tile) =
      this.down && tile.up

  fun moveRightTo(tile: Tile) =
      this.right && tile.left

  fun moveLeftTo(tile: Tile) =
      this.left && tile.right
}

data class Board(val grid: List<List<Tile>>) {
  constructor(input: String) : this(
      input.split('\n').map { line ->
        debugParsing("Tile line $line")
        line.split(' ').map {
          debugParsing("Tile $it")
          Tile(it)
        }
      }
  )

  fun push(
      index: Int,
      direction: Direction,
      tile: Tile
  ): Pair<Board, Tile> {
    return when (direction) {
      UP -> pushUp(index, tile)
      DOWN -> pushDown(index, tile)
      LEFT -> pushLeft(index, tile)
      RIGHT -> pushRight(index, tile)
    }
  }

  private fun pushRight(rowIndex: Int, tile: Tile): Pair<Board, Tile> {
    val size = this.grid.size
    return Board(
        (0 until size).map {
          when (it) {
            rowIndex -> (listOf(tile) + this.grid[it]).take(size)
            else -> this.grid[it]
          }
        }
    ) to this.grid[rowIndex].last()
  }

  private fun pushLeft(rowIndex: Int, tile: Tile): Pair<Board, Tile> {
    val size = this.grid.size
    return Board(
        (0 until size).map {
          when (it) {
            rowIndex -> (this.grid[it] + tile).takeLast(size)
            else -> this.grid[it]
          }
        }
    ) to this.grid[rowIndex].first()
  }

  private fun pushDown(colIndex: Int, pushingTile: Tile): Pair<Board, Tile> {
    val size = this.grid.first().size
    return Board(
        this.grid.mapIndexed { rowIndex, row ->
          row.mapIndexed { idx, tile ->
            when (idx) {
              colIndex -> if (rowIndex == 0) pushingTile else this.grid[rowIndex - 1][idx]
              else -> tile
            }
          }
        }
    ) to this.grid.last()[colIndex]
  }

  private fun pushUp(colIndex: Int, pushingTile: Tile): Pair<Board, Tile> {
    val size = this.grid.first().size
    return Board(
        this.grid.mapIndexed { rowIndex, row ->
          row.mapIndexed { idx, tile ->
            when (idx) {
              colIndex -> if (rowIndex == size - 1) pushingTile else this.grid[rowIndex + 1][idx]
              else -> tile
            }
          }
        }
    ) to this.grid.first()[colIndex]
  }

  fun getAvailableMoveDirections(row: Int, col: Int): Set<Direction> {
    val set = Direction.values().toMutableSet()

    if (row == 0) set.remove(UP)
    if (row == this.grid.lastIndex) set.remove(DOWN)
    if (col == 0) set.remove(LEFT)
    if (col == this.grid.first().lastIndex) set.remove(RIGHT)

    return set
  }

  fun canMove(from: Position, to: Position): Boolean {
    return false
  }

  private fun moveByExactly1Tile(
      from: Position,
      to: Position
  ) = (abs(from.row - to.row) + abs(from.col - to.col)) == 1

  inline operator fun get(position: Position) =
      this.grid[position.row][position.col]
}

data class Player(
    /**
     * the total number of quests for a player (hidden and revealed)
     */
    val numPlayerCards: Int, val x: Int, val y: Int, val tile: Tile
) {
  fun toPosition() = Position(this.row, this.col)

  val row
    get() = y
  val col
    get() = x
}

enum class PlayerId {
  ME,
  FOE
  //
  ;

  val id
    get() = this.ordinal

  companion object {
    fun from(i: Int) = PlayerId.values()[i]
  }
}

data class Item(val name: String, val id: Int)

data class Quest(val questItemName: String, val questPlayerId: PlayerId)

enum class Direction {
  UP,
  RIGHT,
  DOWN,
  LEFT
}

fun push(index: Int, direction: Direction) =
    println("PUSH $index $direction")

fun move(vararg direction: Direction) =
    println("MOVE ${direction.joinToString(" ") { it.toString() }}")

fun pass() =
    println("PASS")

data class Position(val row: Int, val col: Int) {
  fun translatedPositionTo(direction: Direction) =
      when (direction) {
        UP -> Position(this.row - 1, this.col)
        DOWN -> Position(this.row + 1, this.col)
        LEFT -> Position(this.row, this.col - 1)
        RIGHT -> Position(this.row, this.col + 1)
      }
}

infix fun Int.X(that: Int) =
    Position(this, that)