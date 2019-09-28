package net.lab0.coding.game.xmasrush

import Xmas.Direction
import Xmas.X
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

internal class PositionTest {
  @TestFactory
  fun `can find the direction between 2 elements`(): Iterable<DynamicTest> {
    return Xmas.Direction.values().map {
      dynamicTest("#it") {
        val start = 1 X 1
        val end = start.translatedPositionTo(it)
        assertThat(start.directionTo(end)).isEqualTo(it)
      }
    } + dynamicTest("Distance != 1 -> Exception") {
      assertThrows<Exception> {
        val start = 1 X 1
        val end = 0 X 0
        start.directionTo(end)
      }
    }
  }
}