package net.lab0.coding.game.simulator.codersstrikesback

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RawPodCommandTest {
  @Test
  fun `can parse nominal input`() {
    // given
    val s = "1 2 3"

    val i = RawPodCommand.parseStdin(s)

    assertThat(i.targetX).isEqualTo(1)
    assertThat(i.targetY).isEqualTo(2)
    assertThat(i.thrust).isEqualTo("3")
  }

  @Test
  fun `can parse boost input`() {
    // given
    val s = "1 2 BOOST"

    val i = RawPodCommand.parseStdin(s)

    assertThat(i.targetX).isEqualTo(1)
    assertThat(i.targetY).isEqualTo(2)
    assertThat(i.thrust).isEqualTo("BOOST")
  }

  @Test
  fun `can parse shield input`() {
    // given
    val s = "1 2 SHIELD"

    val i = RawPodCommand.parseStdin(s)

    assertThat(i.targetX).isEqualTo(1)
    assertThat(i.targetY).isEqualTo(2)
    assertThat(i.thrust).isEqualTo("SHIELD")
  }
}