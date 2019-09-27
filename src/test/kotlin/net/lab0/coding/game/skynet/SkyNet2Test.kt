package net.lab0.coding.game.skynet

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SkyNet2Test {
  @Test
  fun minimal() {
    val links = listOf(
        "a" to "b",
        "b" to "a"
    )
    assertThat(SkyNet2.path(links, "a", "b")).isEqualTo(
        listOf("a","b")
    )
  }

  @Test
  fun loop() {
    val links = listOf(
        "a" to "b",
        "b" to "c"
    ).bidirectional()
    assertThat(SkyNet2.path(links, "a", "b")).isEqualTo(
        listOf("a","b")
    )
  }

  @Test
  fun `medium graph`() {
    val links = listOf(
        "A" to "B",
        "A" to "C",
        "A" to "C",
        "B" to "A",
        "B" to "E",
        "C" to "A",
        "C" to "D",
        "C" to "D",
        "D" to "H",
        "D" to "C",
        "D" to "E",
        "E" to "B",
        "E" to "C",
        "E" to "D",
        "E" to "F",
        "F" to "E",
        "F" to "G",
        "F" to "M",
        "G" to "F",
        "G" to "H",
        "G" to "I",
        "H" to "D",
        "H" to "G",
        "H" to "J",
        "I" to "G",
        "I" to "J",
        "J" to "H",
        "J" to "I",
        "J" to "K",
        "K" to "J",
        "K" to "L",
        "L" to "K",
        "M" to "F",
        "M" to "N",
        "N" to "M",
        "N" to "O",
        "O" to "N",
        "O" to "P",
        "P" to "O",
        "Y" to "Z"
    )
    assertThat(SkyNet2.path(links, "A", "K")).isEqualTo(
        listOf("A", "C", "D", "H", "J", "K")
    )
  }

  private fun <A> List<Pair<A,A>>.bidirectional(): List<Pair<A,A>> =
      this.flatMap{ listOf(it, it.second to it.first) }
}
