package net.lab0.coding.game.simulator.codersstrikesback

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CodersStrikeBackTest {
  @Test
  fun `can generate a map`() {
    val game = CodersStrikeBack()

    val (init, out) = game.initialize()

    assertThat(init.laps).isGreaterThanOrEqualTo(1)
    assertThat(init.checkpoints).hasSize(init.checkpointCount)

    // 2 ≤ checkpointCount ≤ 8
    assertThat(init.checkpointCount).isGreaterThanOrEqualTo(2)
    assertThat(init.checkpointCount).isLessThanOrEqualTo(8)

    init.checkpoints.forEach {
      assertThat(it.x).isGreaterThanOrEqualTo(0)
      assertThat(it.x).isLessThanOrEqualTo(16000)

      assertThat(it.y).isGreaterThanOrEqualTo(0)
      assertThat(it.y).isLessThanOrEqualTo(9000)
    }
  }
}