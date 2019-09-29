package net.lab0.coding.game.simulator.codersstrikesback

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class RawInitialisationOutputTest {
  @Test
  fun `can output nominal initialization block`() {
    val i = RawInitialisationOutput(
        1, Checkpoint(4, 5), Checkpoint(6, 7)
    )

    assertThat(i.getStdout()).isEqualTo(
        """
          |1
          |2
          |4 5
          |6 7
        """.trimMargin()
    )
  }
}