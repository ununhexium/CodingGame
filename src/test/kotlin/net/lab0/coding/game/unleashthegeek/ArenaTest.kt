package net.lab0.coding.game.unleashthegeek

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import Silver.Arena
import Silver.Cell
import org.assertj.core.api.Assertions.assertThat

internal class ArenaTest {
  @Test
  fun `on update, the arena can use new cell data`() {
    val arena = Silver.Arena(2, 2)
    assertThat(arena.cells).hasSize(4)

    arena.updateCells(
        listOf(
            listOf(
                Cell(Silver.Position(0, 0), null, false, false, false, false),
                Cell(Silver.Position(1, 0), 1, false, false, false, false)
            ),
            listOf(
                Cell(Silver.Position(0, 1), 2, false, false, false, false),
                Cell(Silver.Position(1, 1), 3, false, false, false, false)
            )
        )
    )
  }
}