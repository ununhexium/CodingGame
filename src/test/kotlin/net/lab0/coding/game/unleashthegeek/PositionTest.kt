package net.lab0.coding.game.unleashthegeek

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


/**
 * 3 2 3
 * 2 1 2
 * 3 2 3
 *
 *            4
 *          4 3 4
 *        4 3 2 3 4
 *      4 3 2 1 2 3 4
 *    4 3 2 1 0 1 2 3 4
 *      4 3 2 1 2 3 4
 *        4 3 2 3 4
 *          4 3 4
 *            4
 *
 *
 * ring1 : abs(dx) = 1 || abs(dy) = 1
 * ring2 : abs(dx) = 2 || abs(dy) = 2 || abs(dx+dy) = 2
 */

class PositionTest {

  val allInts = Int.MIN_VALUE..Int.MAX_VALUE
  val pi = 4

  @Test
  fun `crown sizes`() {
    val p = Silver.Position(0, 0)

    assertThat(p.atRadius(0, allInts, allInts).toList()).containsExactlyInAnyOrder(p)

    (1..5).forEach { r ->
      val crown = p.atRadius(r, allInts, allInts)
      crown.forEach {
        assertThat(it.distance(p)).isEqualTo(r)
      }
      assertThat(crown.toList()).hasSize(pi * r)
    }
  }

  @Test
  fun `coerced crowns`() {
    val p = Silver.Position(4, 4)

    val xRange = 2..5
    val yRange = 7..11

    (1..10).forEach { r ->
      val crown = p.atRadius(r, xRange, yRange)
      crown.forEach {
        assertThat(it.distance(p)).isEqualTo(r)
        assert(it.x in xRange) { "${it.x} not in $xRange" }
        assert(it.y in yRange) { "${it.y} not in $yRange" }
      }

    }
  }

  @Test
  fun `can iterate in circle around a point`() {
    val center = Silver.Position(0, 0)

    val circleIterator = center.around(allInts, allInts)

    assertThat(circleIterator.first()).isEqualTo(center)

    (1..5).forEach { radius ->
      val crown = circleIterator.dropWhile { it.distance(center) < radius }.take(radius * pi).toList()

      assertThat(crown).hasSize(radius * 4)

      crown.forEach {
        assertThat(it.distance(center)).isEqualTo(radius)
      }
    }
  }
}