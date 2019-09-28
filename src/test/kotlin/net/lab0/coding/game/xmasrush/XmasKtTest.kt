package net.lab0.coding.game.xmasrush

import Xmas.Direction.DOWN
import Xmas.Direction.LEFT
import Xmas.Direction.RIGHT
import Xmas.Direction.UP
import Xmas.X
import Xmas.asDirections
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class XmasKtTest {
  @Test
  fun `can convert from positions to directions`() {
    val positions = listOf(
        1 X 1,
        0 X 1,
        0 X 2,
        1 X 2,
        2 X 2,
        2 X 1,
        2 X 0,
        1 X 0,
        0 X 0,
        0 X 1,
        1 X 1
    )

    val expected = listOf(
        UP,
        RIGHT,
        DOWN,
        DOWN,
        LEFT,
        LEFT,
        UP,
        UP,
        RIGHT,
        DOWN
    )

    val actual = positions.asDirections()
    assertThat(actual).hasSize(positions.size - 1)
    assertThat(actual).isEqualTo(expected)
  }
}