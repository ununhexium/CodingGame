import Direction.DOWN
import Direction.LEFT
import Direction.RIGHT
import Direction.UP
import TurnType.MOVE
import TurnType.PUSH
import java.util.Random
import java.util.Scanner

/**
 *
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


val DEBUG_PARSING = false
val DEBUG_MOVE = true
val DEBUG_PATHFINDING = true

interface Strategy {
  fun push(game: Game)
  fun move(game: Game)
}

interface TileLike {
  fun withPosition(position: PositionLike): TileWithPositionLike =
      if (this is TileWithPosition) this else TileWithPosition(this, position)

  val up: Boolean
  val right: Boolean
  val down: Boolean
  val left: Boolean
  var item: Item?
}

interface PositionLike {
  val row: Int
  val col: Int

  fun translatedPositionTo(direction: Direction): PositionLike =
      when (direction) {
        UP -> Position(this.row - 1, this.col)
        DOWN -> Position(this.row + 1, this.col)
        LEFT -> Position(this.row, this.col - 1)
        RIGHT -> Position(this.row, this.col + 1)
      }
}

interface TileWithPositionLike : TileLike, PositionLike {
  val tile: TileLike
  val position: PositionLike

  fun samePositionAs(other: PositionLike) =
      this.row == other.row && this.col == other.col

  fun asPosition(): PositionLike = this.position

  fun asTile(): TileLike = this.tile
}

object Strategies {
  var current = StochasticWalk

  /**
   * With the approbation of the Ministry of Silly Walks.
   */
  object StochasticWalk : Strategy {
    override fun push(game: Game) {
      debug("Push")
      push(Dice.nextInt(7), Direction.values().pickOne())
    }

    override fun move(game: Game) {
      with(game) {
        debug("Move")
        val possible = board.getPossibleDirections(me.toPosition())

        debugMove("Possible directions: $possible")

        if (possible.isEmpty()) pass()
        else move(Dice.pickOne(possible))
      }
    }
  }
}


/**
 * Help the Christmas elves fetch presents in a magical labyrinth!
 **/
fun main(args: Array<String>) {
  justCurious()

  val input = Scanner(System.`in`)

  // game loop
  while (true) {
    val game = parseGame(input)

    /*
     * Each game turn alternates between a PUSH turn and a MOVE turn.
     * The first turn is always a PUSH turn.
     */
    debug("Actions")
    with(Strategies.current) {
      when (game.turnType) {
        PUSH -> push(game)
        MOVE -> move(game)
      }
    }
  }
}

private fun parseGame(input: Scanner): Game {
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

  val game = Game(turnType, board, me, foe, quests)
  return game
}

data class Game(
    val turnType: TurnType,
    val board: Board,
    val me: Player,
    val foe: Player,
    val quests: List<Quest>
)

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
    override val up: Boolean,
    override val right: Boolean,
    override val down: Boolean,
    override val left: Boolean
) : TileLike {
  /**
   * Some tiles have items on them.
   */
  override var item: Item? = null
    set(that: Item?) {
      if (item == null) field = that
      else throw IllegalStateException("Wait wait wait, what are you doing there !? >_<'")
    }

  companion object {
    val plus = TileBuilder().openAll().build()
    val minus = TileBuilder().open(LEFT, RIGHT).build()
    val pipe = TileBuilder().open(UP, DOWN).build()
  }

  constructor(tileString: String) : this(
      tileString[0] == '1',
      tileString[1] == '1',
      tileString[2] == '1',
      tileString[3] == '1'
  )

  fun canMoveTowards(direction: Direction, tile: Tile) =
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

  fun cloned(): Tile =
      Tile(this.up, this.right, this.down, this.left)
}

data class TileWithPosition(override val tile: TileLike, override val position: PositionLike) :
    TileWithPositionLike, TileLike by tile, PositionLike by position

/**
 * The board contains square tiles with paths on them.
 * A path can lead to one of the four directions (UP, RIGHT, DOWN and LEFT).
 */
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

  /**
   * Each player can choose to push any row or column on the board.
   * Rows can only be pushed horizontally (LEFT or RIGHT),
   * while columns can only be pushed vertically (UP or DOWN).
   *
   * If both players push the same row or column, no matter the direction, nothing happens.
   *
   * If push commands intersect (one is horizontal and the other one vertical),
   * the row is pushed first, followed by the column.
   * Otherwise, they get pushed simultaneously.
   * => Yeah, well, otherwise it doesn't matter anyway -_-
   *
   * TODO If a player is on a tile which gets pushed out of the map, the player is wrapped on the other end of the line.
   */
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

  fun getAvailableSpaceDirections(position: PositionLike) =
      getAvailableSpaceDirections(position.row, position.col)

  /**
   * What is in principle available, given the board and only '+' tiles
   */
  fun getAvailableSpaceDirections(row: Int, col: Int): Set<Direction> {
    val set = Direction.values().toMutableSet()

    if (row == 0) set.remove(UP)
    if (row == this.grid.lastIndex) set.remove(DOWN)
    if (col == 0) set.remove(LEFT)
    if (col == this.grid.first().lastIndex) set.remove(RIGHT)

    return set
  }

  /**
   * Where it's possible to move, given the tiles arround the given position
   */
  fun getPossibleMoves(from: PositionLike): List<TileWithPositionLike> {
    val fromTile = this[from]
    return this.getAvailableSpaceDirections(from).mapNotNull {
      val to = from.translatedPositionTo(it)
      val targetTile = this[to]
      if (fromTile.canMoveTowards(it, targetTile)) targetTile.withPosition(to) else null
    }
  }

  /**
   * Where it's possible to move, given the tiles arround the given position
   */
  fun getPossibleDirections(from: PositionLike): List<Direction> {
    val fromTile = this[from]
    return this.getAvailableSpaceDirections(from).filter {
      val to = from.translatedPositionTo(it)
      val targetTile = this[to]
      fromTile.canMoveTowards(it, targetTile)
    }
  }

  operator fun get(position: PositionLike) =
      this.grid[position.row][position.col]

  fun getAccessibleTiles(start: Position): Set<TileWithPositionLike> {
    val accessible = mutableSetOf<TileWithPositionLike>()
    val visited = mutableSetOf<Tile>()
    val todo = mutableListOf<TileWithPositionLike>()
    val startTile = this[start]
    todo.add(startTile.withPosition(start))

    while (todo.isNotEmpty()) {
      if (todo.size > 49) {
        throw IllegalStateException("To infinity and beyond!")
      }
      debugPathfinding("Path finding todo: ${todo.map(TileWithPositionLike::asTile)}")
      val current = todo.removeAt(todo.lastIndex)
      debugPathfinding("Now at " + current.position)
      accessible.add(current)
      visited.add(current.asTile() as Tile)
      val possibleMoves = this.getPossibleMoves(current)
      val surroundingTiles = possibleMoves.filter { tp ->
        val tile = tp.asTile()
        visited.none { it === tile }
      }
      todo.addAll(surroundingTiles)
    }

    return accessible
  }
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

/**
 *      Each quest corresponds to an item on the board.
 *      To complete a quest, a player must move to the tile containing the corresponding item.
 *      The quest must be revealed to be able to complete it.
 */
data class Quest(val questItemName: String, val questPlayerId: PlayerId)

enum class Direction {
  UP,
  RIGHT,
  DOWN,
  LEFT
}

/**
 * Builds a new tile. Defaults to all closed.
 */
class TileBuilder(
    var up: Boolean = false,
    var down: Boolean = false,
    var left: Boolean = false,
    var right: Boolean = false
) {

  fun build() = Tile(up, right, down, left)

  fun openAll(): TileBuilder {
    open(UP)
    open(DOWN)
    open(LEFT)
    open(RIGHT)
    return this
  }

  fun open(vararg direction: Direction): TileBuilder {
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

  fun close(vararg direction: Direction): TileBuilder {
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

fun push(index: Int, direction: Direction) =
    println("PUSH $index $direction")

fun move(vararg direction: Direction) =
    println("MOVE ${direction.joinToString(" ") { it.toString() }}")

fun pass() =
    println("PASS")

data class Position(override val row: Int, override val col: Int) : PositionLike {

}

infix fun Int.X(that: Int) =
    Position(this, that)

object Dice : Random() {
  fun <T> pickOne(possible: List<T>): T =
      possible[this.nextInt(possible.size)]
}

fun <T> Array<T>.pickOne() = this[Dice.nextInt(this.size)]
fun <T> List<T>.pickOne() = this[Dice.nextInt(this.size)]

fun debug(s: String) = System.err.println(s)
fun debugPathfinding(s: String) {
  if (DEBUG_PATHFINDING) {
    debug(s)
  }
}

fun debugParsing(s: String) {
  if (DEBUG_PARSING) debug(s)
}

fun debugMove(s: String) {
  if (DEBUG_MOVE) debug(s)
}

fun justCurious() {
  debug("CPU count: " + Runtime.getRuntime().availableProcessors())
}

data class Node<T>(val t: T)
    where T : TileWithPositionLike {
  val children = mutableListOf<Node<T>>()
}