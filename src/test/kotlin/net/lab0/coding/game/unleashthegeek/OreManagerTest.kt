package net.lab0.coding.game.unleashthegeek

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OreManagerTest {
  @Test
  fun `can count ore`() {
    val c = Silver.Clock(0)
    val dm = Silver.TrapManager(c)
    val om = Silver.OreManager(c,dm)

    assertThat(om.getKnownOres()).hasSize(0)

    om.update(2,2, 1)

    assertThat(om.getKnownOres()).hasSize(1)

    om.update(2,2, 3)

    assertThat(om.getKnownOres()).hasSize(3)

    om.update(2,3, 1)

    assertThat(om.getKnownOres()).hasSize(4)

    assertThat(om.getKnownOres()).containsExactlyInAnyOrder(
        Silver.Position(2,2),
        Silver.Position(2,2),
        Silver.Position(2,2),
        Silver.Position(2,3)
    )
  }
}