package net.lab0.coding.game.simulator.codersstrikesback

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RawInputBlockTest {
  @Test
  fun `can read two lines`() {
    val s = """
      |1 2 3
      |4 5 6
    """.trimMargin()

    val i = RawInputBlock.parseStdin(s)

    assertThat(i.line1).isEqualTo(RawPodCommand(1, 2, "3"))
    assertThat(i.line2).isEqualTo(RawPodCommand(4, 5, "6"))
  }

  // TODO: complain if there is more than 2 lines
}