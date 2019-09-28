package net.lab0.coding.game.coderstrikeback

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import Silver.Vec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.math.PI

internal class SilverTest {
  @TestFactory
  fun `angle computation`(): Iterable<DynamicTest> {
    data class TestCase(val vec1: Vec, val vec2: Vec, val angle: Int)

    val vecsAndAngle = listOf(
        TestCase(Vec(1, 1), Vec(-2, 0), +135),
        TestCase(Vec(1, 1), Vec(1, -1), -90),
        TestCase(Vec(1, -1), Vec(1, 1), +90),
        TestCase(Vec(-1, 1), Vec(-1, -1), +90),
        TestCase(Vec(-1, -1), Vec(-1, 1), -90)
    )

    return vecsAndAngle.map {
      DynamicTest.dynamicTest(
          "${it.vec1} -> ${it.vec2}"
      ) {
        assertThat(
            (it.vec1.angleTo(it.vec2) / PI * 180).toInt()
        ).isEqualTo(
            it.angle
        )
      }
    }
  }

  @TestFactory
  fun `rotate vector`(): Iterable<DynamicTest> {
    data class TestCase(val vec: Vec, val rot: Int, val result: Vec)

    return listOf(
        TestCase(Vec(1, 0), +90, Vec(0, 1)),
        TestCase(Vec(3, 3), +45, Vec(0, 4)),
        TestCase(Vec(0, 1), +90, Vec(-1, 0)),
        TestCase(Vec(1, 0), -90, Vec(0, -1)),
        TestCase(Vec(3, -3), -45, Vec(0, -4)),
        TestCase(Vec(0, -1), -90, Vec(-1, 0))
    ).map {
      DynamicTest.dynamicTest(
          "${it.vec} @ ${it.rot} = ${it.result}"
      ) {
        assertThat(
            it.vec.rotate(it.rot / 180.0 * PI)
        ).isEqualTo(
            it.result
        )
      }
    }
  }

  @Test
  fun `whyyyyy`() {
    println(Vec(x=-5735, y=-1074).rotate(-0.8))
  }
}