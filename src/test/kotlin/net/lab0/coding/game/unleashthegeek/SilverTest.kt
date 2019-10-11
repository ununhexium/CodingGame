package net.lab0.coding.game.unleashthegeek

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

internal class SilverTest {

  data class TestRobot(
      override val pos: Silver.Position,
      override val id: Int = 0,
      override val load: Silver.Type = Silver.Type.NONE
  ) : Silver.Robot<TestRobot> {
    override fun copyAndUpdate(update: TestRobot): TestRobot {
      TODO("not implemented")
    }
  }

  fun pos(x: Int, y: Int) = TestRobot(Silver.Position(x, y))

  @Test
  fun `can generate empty heat map`() {
    val a = Silver.Arena()

    println("Computation time = ${measureNanoTime { a.getHeatMap() } / 1000 / 1000}")

    val heatMap = a.getHeatMap()
    assertThat(heatMap).hasSize(30*15)
    assertThat(heatMap.first()).hasSize(15)
    assertThat(heatMap.flatten()).hasSize(30*15)
    heatMap.flatten().forEach {
      assertThat(it).isEqualTo(0)
    }
  }

  @Test
  fun `can generate simple heat map`() {
    val a = Silver.Arena()
    a.cells[2][2].trap = true

    println("Computation time = ${measureNanoTime { a.getHeatMap() } / 1000 / 1000}")

    val heatMap = a.getHeatMap()
    assertThat(heatMap[2][2]).isEqualTo(5)
  }

  @Test
  fun `can generate danger heat map`() {
    val a = Silver.Arena()
    a.cells[2][2].danger = true

    println("Computation time = ${measureNanoTime { a.getHeatMap() } / 1000 / 1000}")

    val heatMap = a.getHeatMap()
    assertThat(heatMap[2][2]).isEqualTo(1)
    assertThat(heatMap[1][2]).isEqualTo(1)
    assertThat(heatMap[2][1]).isEqualTo(1)
    assertThat(heatMap[3][2]).isEqualTo(1)
    assertThat(heatMap[2][3]).isEqualTo(1)
  }

  @Test
  fun `position to string`() {
    assertThat("${Silver.Position(1, 2)}").isEqualTo("1 2")
  }

  @Test
  fun `surrounding positions`() {
    val ps = Silver.Position(2, 2).inRadius(1)
    assertThat(ps).hasSize(5)
    assertThat(ps).containsExactlyInAnyOrder(
        Silver.Position(2,2),
        Silver.Position(1,2),
        Silver.Position(2,1),
        Silver.Position(3,2),
        Silver.Position(2,3)
    )
  }
}