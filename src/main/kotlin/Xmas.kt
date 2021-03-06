import Xmas.Direction.DOWN
import Xmas.Direction.LEFT
import Xmas.Direction.RIGHT
import Xmas.Direction.UP
import Xmas.PlayerId.IT
import Xmas.PlayerId.ME
import Xmas.TurnType.MOVE
import Xmas.TurnType.PUSH
import java.util.LinkedList
import java.util.Random
import java.util.Scanner
import kotlin.math.abs

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

TODO
can 2 players be on the same tile
 */

object Xmas {

  val DEBUG_PARSING = false
  val DEBUG_MOVE = true
  val DEBUG_PATHFINDING = true

  val MAX_PATH_SIZE = 20

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

    fun distanceTo(other: PositionLike) =
        abs(other.row - this.row) + abs(other.col - this.col)

    /**
     * Relative position starting from this, going to other.
     * The gap may only be one tile.
     */
    fun directionTo(other: PositionLike): Direction {
      fun thrower(): Nothing {
        throw IllegalArgumentException("Can only compute distances to adjacent positions")
      }

      if (this.distanceTo(other) != 1) {
        thrower()
      }

      return when {
        this.row == other.row -> when (other.col - this.col) {
          -1 -> LEFT
          1 -> RIGHT
          else -> thrower()
        }
        this.col == other.col -> when (other.row - this.row) {
          -1 -> UP
          1 -> DOWN
          else -> thrower()
        }
        else -> thrower()
      }
    }

    fun positionOnly(): PositionLike
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
    var current = BestEffort

    /**
     * With the approbation of the Ministry of Silly Walks.
     */
    object StochasticWalk : Strategy {
      override fun push(game: Game) {
        debug("Push")
        pushCommand(Dice.nextInt(7), Direction.values().pickOne())
      }

      override fun move(game: Game) {
        with(game) {
          debug("Move")
          val possible = board.getPossibleDirections(me.toPosition())

          debugMove("Possible directions: $possible")

          if (possible.isEmpty()) pass()
          else moveCommand(Dice.pickOne(possible))
        }
      }
    }

    /**
     * Go as close as possible to the objective
     */
    object BestEffort : Strategy {
      override fun push(game: Game) {
        // if can go to any quest item
        val accessibleItems = game.getAccessibleItems(PlayerId.ME)
        if (accessibleItems.isNotEmpty()) {
          val path = game.shortestPath(PlayerId.ME, accessibleItems.first())
          val pushes = game.pushNotOnPath(path)
          if (pushes.isEmpty()) {
            // can't push without disturbing target path. Go for random move.
            // TODO: find non disturbing pushes
            pushCommand(Dice.nextInt(7), Direction.values().pickOne())
          } else {
            pushCommand(pushes.first())
          }
          // find a way to push stuff so it doesn't disturb the path
          // game.pushNotOnPathTo(accessibleItems.first())
        }
        pushCommand(Dice.nextInt(7), Direction.values().pickOne())
      }

      override fun move(game: Game) {
        val activeQuest = game.questBook.quests.first()
        val target = game.board.getTileWithQuestItem(activeQuest).asPosition()
        debugPathfinding("Going from ${game.me.position} to $target")
        val path = game.board.moveAsCloseAsPossible(
            game.me,
            target
        )
        debugPathfinding("Path: " + path.joinToString(" -> ") { it.positionOnly().toString() })
        if (path.isEmpty()) {
          pass()
        } else {
          moveCommand(path.asDirections())
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
    val it = parsePlayer(input)

    debugParsing("Items")
    val numItems = input.nextInt() // the total number of items available on board and on player tiles
    for (i in 0 until numItems) {
      val itemName = input.next()
      val itemX = input.nextInt()
      val itemY = input.nextInt()
      val itemPlayerId = input.nextInt()
      val item = Item(itemName, PlayerId.from(itemPlayerId))

      when (itemX) {
        -1 -> me.tile.item = item
        -2 -> it.tile.item = item
        else -> board.grid[itemY][itemX].item = item
      }
    }

    debugParsing("Quests")
    val numQuests = input.nextInt() // the total number of revealed quests for both players
    val quests = (1..numQuests).map {
      Quest(input.next(), PlayerId.from(input.nextInt()))
    }

    val game = Game(turnType, board, me, it, QuestBook(quests))
    return game
  }

  data class Game(
      val turnType: TurnType,
      val board: Board,
      val me: Player,
      val it: Player,
      val questBook: QuestBook
  ) {

    fun getAnyQuestItemOnTheEdge(player: PlayerId): List<TileWithPositionLike> =
        board.getEdgeTiles().filter {
          it.item?.playerId == player
        }

    fun getAccessibleItems(playerId: PlayerId): List<TileWithPositionLike> =
        board.getAccessibleTiles(positionOf(playerId)).allElements().filter {
          it.tile.item != null
        }.filter {
          it.tile.item?.name in questBook.myQuestsObjectNames
        }

    fun positionOf(playerId: PlayerId) =
        when (playerId) {
          ME -> me.position
          IT -> it.position
        }

    fun pushNotOnPath(path: List<PositionLike>): List<Push> {
      val usedRows = path.map { it.row }.distinct()
      val usedCols = path.map { it.col }.distinct()
      val rowPush = (0..board.lastRow).filter { it !in usedRows }
      val colPush = (0..board.lastCol).filter { it !in usedCols }
      return rowPush.flatMap { idx -> Direction.horizontal.map { Push(idx, it) } } +
          colPush.flatMap { idx -> Direction.vertical.map { Push(idx, it) } }
    }

    fun shortestPath(playerId: PlayerId, position: PositionLike): List<PositionLike> {
      val path = board.shortestPath(
          positionOf(playerId),
          position
      )

      return path
    }
  }

  fun Iterable<PositionLike>.asDirections(): List<Direction> {
    return this.zipWithNext().map {
      it.first.directionTo(it.second)
    }
  }

  private fun parsePlayer(input: Scanner): Player {
    val numCards = input.nextInt()
    val col = input.nextInt()
    val row = input.nextInt()
    return Player(
        numCards,
        row X col,
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

    val gridWithPosition by lazy {
      grid.mapIndexed { rowIdx, row ->
        row.mapIndexed { colIdx, tile ->
          tile.withPosition(rowIdx X colIdx)
        }
      }
    }

    val graph: Graph<TileWithPositionLike> by lazy {
      val graph = Graph<TileWithPositionLike>()
      gridWithPosition.flatMap { row ->
        row.map { tile ->
          tile to getPossibleAdjacentMoves(tile)
        }
      }.forEach {
        val from = it.first
        it.second.forEach { to ->
          graph.addEdge(from, to)
        }
      }
      graph
    }

    val height by lazy {
      grid.size
    }

    val lastRow
      get() = height - 1

    val width by lazy {
      grid.first().size
    }

    val lastCol
      get() = width - 1

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

    fun getAvailableAdjacentDirections(position: PositionLike) =
        getAvailableAdjacentDirections(position.row, position.col)

    /**
     * What is in principle available, given the board and only '+' tiles
     */
    fun getAvailableAdjacentDirections(row: Int, col: Int): Set<Direction> {
      val set = Direction.values().toMutableSet()

      if (row == 0) set.remove(UP)
      if (row == this.grid.lastIndex) set.remove(DOWN)
      if (col == 0) set.remove(LEFT)
      if (col == this.grid.first().lastIndex) set.remove(RIGHT)

      return set
    }

    /**
     * Where it's possible to move to a directly adjacent tile, given the tiles around the given position.
     */
    fun getPossibleAdjacentMoves(from: PositionLike): List<TileWithPositionLike> {
      val fromTile = this[from]
      return this.getAvailableAdjacentDirections(from).mapNotNull {
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
      return this.getAvailableAdjacentDirections(from).filter {
        val to = from.translatedPositionTo(it)
        val targetTile = this[to]
        fromTile.canMoveTowards(it, targetTile)
      }
    }

    operator fun get(position: PositionLike) =
        this.grid[position.row][position.col]

    fun getWithPosition(position: PositionLike) =
        this.gridWithPosition[position.row][position.col]

    /**
     * All the accessible tiles, as far as it's possible to go
     * TODO: limit to 20 steps
     */
    val accessibleTilesCache = mutableMapOf<PositionLike, Node<TileWithPositionLike>>()

    fun getAccessibleTiles(start: PositionLike): Node<TileWithPositionLike> {
      return accessibleTilesCache.computeIfAbsent(start) {
        internalGetAccessibleTiles(start)
      }
    }

    private fun internalGetAccessibleTiles(start: PositionLike): Node<TileWithPositionLike> {
      val startTile = this[start]
      val s = startTile.withPosition(start)
      val root = Node(s)
      val todo = this.getPossibleAdjacentMoves(s).filter {
        it !in root
      }.map {
        root to it
      }.toMutableList()

      while (todo.isNotEmpty()) {
        val (parentNode, current) = todo.removeAt(0)
        parentNode.children.add(Node(current))
        todo.addAll(
            this.getPossibleAdjacentMoves(current).filter {
              it !in root
            }.map {
              root to it
            }.toMutableList()
        )
      }

      return root
    }

    // TODO old school loop for performance
    fun getTileWithQuestItem(activeQuest: Quest): TileWithPositionLike =
        this.grid.mapIndexed { rowIdx, row ->
          row.mapIndexedNotNull { colIdx, tile ->
            if (tile.item?.name == activeQuest.questItemName) {
              tile.withPosition(rowIdx X colIdx)
            } else null
          }
        }.flatten().first()

    fun moveAsCloseAsPossible(
        start: PositionLike,
        target: PositionLike
    ): List<PositionLike> {
      val accessible = getAccessibleTiles(start)
      debugPathfinding("Accessible: ${accessible.allElements().map { it.asPosition() }}")
      // TODO: may be more than 1 closest tile
      val closest = accessible.allElements().minBy {
        it.distanceTo(target)
      }
      debugPathfinding("Closest tile: $closest")

      return if (closest != null) {
        shortestPath(start, closest)
      } else {
        listOf()
      }

    }

    fun getEdgeTiles(): List<TileWithPositionLike> =
        this.gridWithPosition.first() +
            this.gridWithPosition.last() +
            this.gridWithPosition.drop(1).dropLast(1).flatMap {
              listOf(it.first(), it.last())
            }

    fun randomPosition(): Position =
        (0..lastRow).toList().pickOne() X (0..lastCol).toList().pickOne()

    fun shortestPath(start: PositionLike, end: PositionLike): List<PositionLike> {
      return Dijkstra
          .path(
              this.graph,
              getWithPosition(start),
              getWithPosition(end)
          )
    }
  }

  data class Player(
      /**
       * the total number of quests for a player (hidden and revealed)
       */
      val numPlayerCards: Int, val position: Position, val tile: Tile
  ) : PositionLike by position {
    fun toPosition() = position
  }

  enum class PlayerId {
    ME,
    IT // rename P2
    //
    ;

    val id
      get() = this.ordinal

    companion object {
      fun from(i: Int) = PlayerId.values()[i]
    }
  }

  data class Item(val name: String, val playerId: PlayerId)

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
    //
    ;

    companion object {
      val horizontal by lazy {
        listOf(LEFT, RIGHT)
      }

      val vertical by lazy {
        listOf(UP, DOWN)
      }
    }
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

  fun pushCommand(index: Int, direction: Direction) =
      println("PUSH $index $direction")

  fun pushCommand(push: Push) =
      println("PUSH ${push.index} ${push.direction}")

  fun moveCommand(vararg direction: Direction) =
      moveCommand(direction.toList())

  fun moveCommand(directions: List<Direction>) {
    if (directions.isEmpty()) pass()
    else println("MOVE ${directions.joinToString(" ") { it.toString() }}")
  }

  fun pass() =
      println("PASS")

  data class Position(override val row: Int, override val col: Int) : PositionLike {
    override fun positionOnly() = this
  }

  infix fun Int.X(that: Int) =
      Position(this, that)

  object Dice : Random() {
    fun <T> pickOne(possible: List<T>): T =
        possible[this.nextInt(possible.size)]
  }

  fun <T> Array<T>.pickOne() = this[Dice.nextInt(this.size)]
  fun <T> List<T>.pickOne() = this[Dice.nextInt(this.size)]
  fun <T> List<T>.pick(n: Int): List<T> {
    val available = this.toMutableList()
    val picked = mutableListOf<T>()

    (1..n).onEach {
      val index = Dice.nextInt(available.size)
      val t = available.removeAt(index)
      picked.add(t)
    }

    return picked
  }

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

  data class Node<T>(val value: T) {
    val children = mutableListOf<Node<T>>()
    fun size(): Int {
      return 1 + children.map {
        it.size()
      }.sum()
    }

    fun allElements(): List<T> {
      return listOf(this.value) + this.children.flatMap { it.allElements() }
    }
  }

  operator fun <T> Node<T>.contains(element: T): Boolean {
    return when {
      this.value == element -> true
      children.isNotEmpty() -> this.children.any { it.contains(element) }
      else -> false
    }
  }

  data class QuestBook(val quests: List<Quest>) {
    val myQuests by lazy {
      quests.filter { it.questPlayerId == ME }
    }
    val myQuestsObjectNames by lazy {
      myQuests.map { it.questItemName }
    }
    val itsQuests by lazy {
      quests.filter { it.questPlayerId == IT }
    }

  }

  data class Push(val index: Int, val direction: Direction)


  class Graph<T> where T : PositionLike {
    val internalEdges = HashMap<T, MutableSet<T>>() // map<from, to>

    val edges: Map<T, Set<T>>
      get() = internalEdges

    val nodes
      get() = internalEdges.keys as Set<T>

    fun addEdge(a: T, b: T) {
      if (hasEdge(a, b)) {
        return // skip
      } else {
        internalEdges.computeIfAbsent(a) {
          mutableSetOf()
        }.add(b)

        internalEdges.computeIfAbsent(b) {
          mutableSetOf()
        }.add(a)
      }
    }

    private fun hasEdge(a: T, b: T) =
        internalEdges[a]?.contains(b) ?: false

    fun adjacentNodes(node: T): Set<T> {
      return edges[node] ?: setOf()
    }

    fun edgeValue(a: T, b: T) =
        if (internalEdges[a]?.contains(b) ?: false) 1 else 50
  }


  object Dijkstra {
    private val INF = Integer.MAX_VALUE

    fun <T : PositionLike> path(g: Graph<T>, a: T, b: T): List<T> {
      val nodes = g.nodes
      if (b !in nodes) return listOf() // impossible

      val toExplore = nodes.filter {
        it != b
      }.mapTo(LinkedList()) {
        it
      }
//    println("Preparing to browse " + toExplore.joinToString())

      val distances = LinkedHashMap<T, Pair<T, Int>>()
      distances[a] = a to 0

      var current: T = a
      var safeguard = 100
      while (distances[b] == null && safeguard-- >= 0) {
        debugPathfinding("Currently at $current")

        val nonVisitedAdjacentNodes = g.adjacentNodes(current)

        nonVisitedAdjacentNodes.forEach {
          val distance = g.edgeValue(current, it) + distances[current]!!.second
          if (distance < distances[it]?.second ?: INF) {
            distances[it] = current to distance
          }
        }

        debugPathfinding("Best distance: " + distances[b])
        toExplore.remove(current)
        current = distances.filter {
          it.key in toExplore
        }.minBy {
          it.value.second
        }?.key ?: continue
      }

      current = b
      val path = mutableListOf(current)
      while (current != a) {
        val previous = distances[current]
        path.add(previous!!.first)
        current = previous.first
      }
      path.reverse()

      return path
    }
  }

}