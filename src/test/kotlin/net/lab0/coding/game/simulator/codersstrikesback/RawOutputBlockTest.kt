package net.lab0.coding.game.simulator.codersstrikesback

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RawOutputBlockTest {
  @Test
  fun `can output nominal pods situation block`() {
    val o = RawOutputBlock(
        RawPodSituation(1, 2, 3, 4, 5, 6),
        RawPodSituation(7, 8, 9, 10, 11, 12),
        RawPodSituation(13, 14, 15, 16, 17, 18),
        RawPodSituation(19, 20, 21, 22, 23, 24)
    )

    assertThat(o.getStdout()).isEqualTo(
        """
          |1 2 3 4 5 6
          |7 8 9 10 11 12
          |13 14 15 16 17 18
          |19 20 21 22 23 24
        """.trimMargin()
    )
  }
}