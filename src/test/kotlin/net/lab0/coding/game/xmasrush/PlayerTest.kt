package net.lab0.coding.game.xmasrush

import Xmas.Direction.DOWN
import Xmas.Direction.LEFT
import Xmas.Direction.RIGHT
import Xmas.Direction.UP
import Xmas.X
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PlayerTest {
  @Test
  fun `can translate player's position`() {
    val position = 1 X 1
    assertThat(position.translatedPositionTo(UP)).isEqualTo(0 X 1)
    assertThat(position.translatedPositionTo(DOWN)).isEqualTo(2 X 1)
    assertThat(position.translatedPositionTo(LEFT)).isEqualTo(1 X 0)
    assertThat(position.translatedPositionTo(RIGHT)).isEqualTo(1 X 2)
  }
}
