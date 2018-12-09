package net.lab0.coding.game.xmasrush

import Board
import Game
import Item
import Player
import PlayerId
import PlayerId.IT
import PlayerId.ME
import Position
import Quest
import QuestBook
import TileWithPositionLike
import TurnType
import X
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.RepeatedTest
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

  @RepeatedTest(100)
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
      val board = Board(Helpers.asciiToTiles(_grid ?: Helpers.asciiGridGenerator(7)))
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

    val board = Board(Helpers.asciiToTiles(grid))
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
}