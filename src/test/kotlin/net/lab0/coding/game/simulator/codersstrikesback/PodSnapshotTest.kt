package net.lab0.coding.game.simulator.codersstrikesback

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PodSnapshotTest {
  @Test
  fun `can output nominal pod situtation line`() {
    val o = PodSnapshot(1,2, 3,4,5, 0)

    assertThat(o.getStdout()).isEqualTo("1 2 3 4 5 0")
  }
}