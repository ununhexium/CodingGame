package net.lab0.coding.game.xmasrush

import net.lab0.coding.game.xmasrush.Direction.DOWN
import net.lab0.coding.game.xmasrush.Direction.LEFT
import net.lab0.coding.game.xmasrush.Direction.RIGHT
import net.lab0.coding.game.xmasrush.Direction.UP
import net.lab0.coding.game.xmasrush.TurnType.MOVE
import net.lab0.coding.game.xmasrush.TurnType.PUSH
import java.util.Scanner

/**
 *


Note: The player's id is always 0 and the opponent's 1.
Output for one PUSH game turn

PUSH id direction where id is between 0 and 6, and direction can be UP, DOWN, LEFT or RIGHT.

Example: PUSH 3 UP will push the third column upwards.
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

fun debug(s: String) = System.err.println(s)

/**
 * Help the Christmas elves fetch presents in a magical labyrinth!
 **/
fun main(args: Array<String>) {
    val input = Scanner(System.`in`)

    // game loop
    while (true) {
        val turnType = if (input.nextInt() == 0) PUSH else MOVE

        val gridInput = (1..7).map {
            Tile(input.nextLine())
        }.joinToString("\n")

        val board = Board(gridInput)

        val me = parsePlayer(input)
        val foe = parsePlayer(input)

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

        val numQuests = input.nextInt() // the total number of revealed quests for both players
        val quests = (1..numQuests).map {
            Quest(input.next(), input.nextInt())
        }

        // Write an action using println()

        when (turnType) {
            PUSH -> doPush()
            MOVE -> doMove()
        }

        println("PUSH 3 RIGHT") // PUSH <id> <direction> | MOVE <direction> | PASS
    }
}

fun doPush() {

}

fun doMove() {
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
    constructor(tileString: String) : this(
        tileString[0] == '1',
        tileString[1] == '1',
        tileString[2] == '1',
        tileString[3] == '1'
    )
}

data class Board(val grid: List<List<Tile>>) {
    constructor(input: String) : this(
        input.split('\n').map { line ->
            line.split(' ').map {
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

    private fun pushRight(rowIndex: Int, tile:Tile): Pair<Board, Tile> {
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

    private fun pushLeft(rowIndex: Int, tile:Tile): Pair<Board, Tile> {
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

    private fun pushDown(colIndex: Int, tile:Tile): Pair<Board, Tile> {
        val size = this.grid.first().size
        return Board(
            this.grid.map { row ->
                row.mapIndexed { idx, tile ->
                    when (idx) {
                        colIndex -> this.grid[(size + idx - 1) % size][idx]
                        else -> tile
                    }
                }
            }
        ) to this.grid.last()[colIndex]
    }

    private fun pushUp(colIndex: Int, tile:Tile): Pair<Board, Tile> {
        val size = this.grid.first().size
        return Board(
            this.grid.map { row ->
                row.mapIndexed { idx, tile ->
                    when (idx) {
                        colIndex -> this.grid[(idx + 1) % size][idx]
                        else -> tile
                    }
                }
            }
        ) to this.grid.first()[colIndex]
    }


}

data class Player(
    /**
     * the total number of quests for a player (hidden and revealed)
     */
    val numPlayerCards: Int, val x: Int, val y: Int, val tile: Tile
)

enum class PlayId {
    ME,
    FOE
}

data class Item(val name: String, val id: Int)

data class Quest(val questItemName: String, val questPlayerId: Int)

enum class Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT
}
