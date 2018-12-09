package net.lab0.coding.game.xmasrush

import Board
import Direction.DOWN
import Direction.LEFT
import Direction.RIGHT
import Direction.UP
import Game
import Item
import Player
import PlayerId
import PlayerId.IT
import PlayerId.ME
import Position
import Push
import Quest
import QuestBook
import TileWithPositionLike
import TurnType
import X
import asDirections
import org.assertj.core.api.Assertions.assertThat
import org.funktionale.currying.curried
import org.funktionale.currying.uncurried
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import pick
import pickOne

internal class GameTest {
  @TestFactory
  fun `can tell if 1 item is on the edge of the board`(): List<DynamicTest> {
    val size = 3
    return (0 until size).map { row ->
      (0 until size).map { col ->
        // given
        val game = createGame(size)
        val position = row X col

        game.board.getEdgeTiles().filter {
          it.asPosition() != position
        }.pickOne().item = Item("Trick", IT)
        game.board[position].item = Item("A", ME)

        // when
        val itemsOnEdges: List<TileWithPositionLike> = game.getAnyQuestItemOnTheEdge(PlayerId.ME)

        // then
        dynamicTest("Item@$position") {
          if (row == 0 || col == 0 || row == 2 || col == 2) {
            assertThat(itemsOnEdges).hasSize(1)
          }
          else {
            assertThat(itemsOnEdges).isEmpty()
          }
        }
      }
    }.flatten()
  }

  @RepeatedTest(10)
  fun `can tell if 3 items are on the edge of the board`() {
    val size = 7
    val game = createGame(size)
    val mine = game.board.getEdgeTiles().pick(3)
    val its = game.board.getEdgeTiles().filter {
      it !in mine
    }.pick(3)

    val myItems = (0 until 3).map {
      Item("I$it", ME)
    }
    mine.forEachIndexed { idx, tile ->
      tile.item = myItems[idx]
    }
    its.forEachIndexed { idx, tile ->
      tile.item = Item("I$idx", IT)
    }

    val items = game.getAnyQuestItemOnTheEdge(ME)
    assertThat(items).hasSize(3)
    assertThat(items.map { it.item }).containsExactlyInAnyOrderElementsOf(myItems)
  }

  private fun createGame(size: Int): Game {
    return GameBuilder()
        .withGrid(
            ("┼".repeat(size) + "\n").repeat(size).dropLast(1)
        ).withMyPosition(
            0 X 0
        ).withItsPosition(
            1 X 1
        ).addQuest(
            Quest("A", ME)
        ).build()
  }

  class GameBuilder(
      private var _turnType: TurnType? = null,
      private var _grid: String? = null,
      private var _me: Player? = null,
      private var _myPosition: Position? = null,
      private var _itsPosition: Position? = null,
      private var _items: MutableList<Pair<Item, Position>> = mutableListOf(),
      private var _quests: MutableList<Quest> = mutableListOf()
  ) {

    fun duplicate(): GameBuilder {
      return GameBuilder(
          _turnType,
          _grid,
          _me,
          _myPosition,
          _itsPosition,
          _items.toMutableList(),
          _quests.toMutableList()
      )
    }

    fun withGrid(grid: String): GameBuilder {
      this._grid = grid
      return this
    }

    fun withMe(me: Player): GameBuilder {
      _me = me
      return this
    }

    fun withMyPosition(position: Position): GameBuilder {
      _myPosition = position
      return this
    }

    fun withItsPosition(position: Position): GameBuilder {
      _itsPosition = position
      return this
    }

    fun addItem(item: Item, position: Position): GameBuilder {
      _items.add(item to position)
      return this
    }

    fun addQuest(quest: Quest): GameBuilder {
      _quests.add(quest)
      return this
    }

    fun build(): Game {
      val board = Board(Helpers.asciiGridToTiles(_grid ?: Helpers.asciiGridGenerator(7)))
      val turnType = _turnType ?: TurnType.values().pickOne()

      _items.forEach {
        board[it.second].item = it.first
      }

      val myPosition = _myPosition ?: board.randomPosition()
      val me = _me ?: Player(0/*TODO*/, myPosition, board[myPosition])

      val itsPosition = _itsPosition ?: board.randomPosition()
      val foe = _me ?: Player(0/*TODO*/, itsPosition, board[itsPosition])
      return Game(turnType, board, me, foe, QuestBook(_quests))
    }
  }

  @TestFactory
  fun `can list reachable items for player`(): Iterable<DynamicTest> {
    val grid = """
      >┼┼.┴│.┐
      >┤│.┘┼┬.
      >└│┬├┘│├
      >┘┐┼┬└└├
      >┤┘└┐┘┼┴
      >┐┤─┬┌┐┴
      >├┬└┤└┘└
    """.trimMargin(">")

    val board = Board(Helpers.asciiGridToTiles(grid))
    val reachableAndMine = Item("reachable", ME)
    val unReachableAndMine = Item("unreachable", ME)
    val reachableAndNotMine = Item("reachable it's", IT)
    val gameBase = GameBuilder()
        .withGrid(grid)
        .withMyPosition(0 X 0)
        .addItem(reachableAndMine, 2 X 1)
        .addItem(reachableAndNotMine, 2 X 0)
        .addItem(unReachableAndMine, 4 X 4)


    val noQuest = dynamicTest("Cannot reach items which are not in the quest") {
      val accessible = gameBase.duplicate().build().getAccessibleItems(ME)
      assertThat(accessible).isEmpty()
    }

    val questSetToReachableItem = dynamicTest(
        "Can reach an item that is in the quest and in range"
    ) {
      val builder = gameBase.duplicate().addQuest(Quest("reachable", ME))
      val accessible = builder.build().getAccessibleItems(ME)
      assertThat(accessible.map { it.tile.item }).containsExactly(reachableAndMine)
    }

    val questSetToUnreachableItem = dynamicTest(
        "Cannot reach an item that is in the quest and out of range"
    ) {
      val builder = gameBase.duplicate().addQuest(Quest("unreachable", ME))
      val accessible = builder.build().getAccessibleItems(ME)
      assertThat(accessible).isEmpty()
    }

    return listOf(noQuest, questSetToReachableItem, questSetToUnreachableItem)
  }

  val tile = { board: Board, position: Position ->
    board.getWithPosition(position)
  }

  val verticalDownLink = { board: Board, fromRow: Int, onColumn: Int ->
    val t = tile.curried()(board)
    t(fromRow X onColumn) to t((fromRow + 1) X onColumn)
  }

  val horizontalRightLink = { board: Board, onRow: Int, fromCol: Int ->
    val t = tile.curried()(board)
    t(onRow X fromCol) to t(onRow X (fromCol + 1))
  }

  @Test
  fun `can transform the 1x1 grid to a graph`() {
    val grid = """
      >┼
    """.trimMargin(">")

    val board = Board(Helpers.asciiGridToTiles(grid))
    val graph = board.graph

    val h = horizontalRightLink.curried()(board).uncurried()
    val v = verticalDownLink.curried()(board).uncurried()
    assertThat(graph.edges).isEmpty()
  }

  @Test
  fun `can transform the 1x2 grid to a graph`() {
    val grid = """
      >┼┼
    """.trimMargin(">")

    val board = Board(Helpers.asciiGridToTiles(grid))
    val graph = board.graph

    val h = horizontalRightLink.curried()(board).uncurried()
    val v = verticalDownLink.curried()(board).uncurried()
    val expected = processAsEdges(
        listOf(
            h(0, 0)
        )
    )
    val actual = graph.edges
    assertThat(actual).isEqualTo(expected)
  }

  private fun <T> processAsEdges(listOf: List<Pair<T, T>>):
      Map<T, Set<T>> {
    return listOf.flatMap {
      // also contains the reverse direction
      listOf(
          it.first to it.second,
          it.second to it.first
      )
    }.groupBy({ it.first }) {
      it.second
    }.mapValues {
      it.value.toSet()
    }
  }

  @Test
  fun `can transform the 2x2 grid to a graph`() {
    val grid = """
      >┼┼
      >┼┼
    """.trimMargin(">")

    val board = Board(Helpers.asciiGridToTiles(grid))
    val graph = board.graph

    val h = horizontalRightLink.curried()(board).uncurried()
    val v = verticalDownLink.curried()(board).uncurried()
    val expected = processAsEdges(
        listOf(
            h(0, 0),
            h(1, 0),
            v(0, 0),
            v(0, 1)
        )
    )
    val actual = graph.edges
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `can transform the grid to a graph`() {
    val grid = """
      >┼────┐
      >┤┬┐┼┬┘
      >│└┘│..
      >┼──┘..
      >┴────┴
    """.trimMargin(">")

    val board = Board(Helpers.asciiGridToTiles(grid))

    val h = horizontalRightLink.curried()(board).uncurried()
    val v = verticalDownLink.curried()(board).uncurried()

    val expected = processAsEdges(
        listOf(
            // vertical links for big path
            v(0, 0),
            v(1, 0),
            v(2, 0),
            v(3, 0),
            v(0, 5),
            v(1, 3),
            v(2, 3),
            // vertical links for square
            v(1, 1),
            v(1, 2),
            // horizontal links for big path
            h(0, 0),
            h(0, 1),
            h(0, 2),
            h(0, 3),
            h(0, 4),
            h(1, 3),
            h(1, 4),
            h(3, 0),
            h(3, 1),
            h(3, 2),
            h(4, 0),
            h(4, 1),
            h(4, 2),
            h(4, 3),
            h(4, 4),
            // horizontal square links
            h(1, 1),
            h(2, 1)
        )
    )
    assertThat(board.graph.edges.keys).containsExactlyInAnyOrderElementsOf(expected.keys)
    assertThat(board.graph.edges).isEqualTo(expected)
  }

  @Test
  fun `find the shortest path to the target`() {
    val canMoveSomethingElse = """
      >┼─────┐
      >┤..┘┼┬┘
      >│.┬├│..
      >│..┬├└.
      >┼───┘..
      >│.....┐
      >┴─────┴
    """.trimMargin(">")

    val item = Item("A", ME)
    val position = 4 X 4

    val game = GameBuilder()
        .withGrid(canMoveSomethingElse)
        .withMyPosition(0 X 0)
        .addItem(item, position)
        .addQuest(Quest(item.name, ME))
        .build()

    val propositions = game.shortestPath(ME, position).asDirections()
    assertThat(propositions).containsExactlyInAnyOrderElementsOf(
        listOf(DOWN, DOWN, DOWN, DOWN, RIGHT, RIGHT, RIGHT, RIGHT)
    )
  }

  @Test
  fun `does not fail on impossible path`() {
    val canMoveSomethingElse = """
      >┼└
    """.trimMargin(">")

    val position = 0 X 1

    val game = GameBuilder()
        .withGrid(canMoveSomethingElse)
        .withMyPosition(0 X 0)
        .build()

    val path = game.shortestPath(ME, position)
    assertThat(path).isEmpty()
    val propositions = path.asDirections()
    assertThat(propositions).isEmpty()
  }

  @Test
  fun `does not run in circles`() {
    val canMoveSomethingElse = """
      >┼┼└
      >┼┼└
    """.trimMargin(">")

    val position = 1 X 2

    val game = GameBuilder()
        .withGrid(canMoveSomethingElse)
        .withMyPosition(0 X 0)
        .build()

    val path = game.shortestPath(ME, position)
    assertThat(path).isEmpty()
    val propositions = path.asDirections()
    assertThat(propositions).isEmpty()
  }

  @Test
  fun `find a push which doesn't touch the path to a tile`() {
    val canMoveSomethingElse = """
      >┼..┴│..
      >┤..┘┼┬.
      >│.┬├┘│.
      >│..┬└└.
      >┼───┘..
      >│.....┐
      >┴─────┴
    """.trimMargin(">")

    val position = 4 X 4

    val game = GameBuilder()
        .withGrid(canMoveSomethingElse)
        .withMyPosition(0 X 0)
        .build()

    val path = game.shortestPath(ME, position)
    val propositions: List<Push> = game.pushNotOnPath(path)
    assertThat(propositions).containsExactlyInAnyOrderElementsOf(
        listOf(
            Push(5, RIGHT),
            Push(5, LEFT),
            Push(6, RIGHT),
            Push(6, LEFT),
            Push(5, UP),
            Push(5, DOWN),
            Push(6, UP),
            Push(6, DOWN)
        )
    )
  }

  @Test
  fun `find a push which doesn't break the path to a tile`() {
    // TODO: split in a separate function
    // TODO: add case where the player can be pushed to the other side
    val mustChangeTheExistingPath = """
      >┼┼.┴│.┐
      >┤│.┘┼┬.
      >││┬├┘│├
      >│┐┼┬└└├
      >│┘└┐┘┼┴
      >│┤─┬┌┐┴
      >└──────
    """.trimMargin(">")

    val item = Item("A", ME)
    val position = 6 X 6

    val game = GameBuilder()
        .withGrid(mustChangeTheExistingPath)
        .withMyPosition(0 X 0)
        .addItem(item, position)
        .addQuest(Quest("A", ME))
        .build()

    val path = game.shortestPath(ME, position)
    // when
    val propositions: List<Push> = game.pushNotOnPath(path)

    // it lists all the possible path-changing but preserving moves
    assertThat(propositions).containsExactlyInAnyOrderElementsOf(
        listOf(
            Push(6, RIGHT),
            Push(6, LEFT),
            Push(5, UP),
            Push(5, DOWN),
            Push(6, UP),
            Push(6, DOWN)
        )
    )
  }


}
