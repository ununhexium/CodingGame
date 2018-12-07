package net.lab0.coding.game.xmasrush

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class TileTest {
  @TestFactory
  fun `can parse a tile`() =
      listOf(
          DynamicTest.dynamicTest("0000") {
            val tile = Tile("0000")
            assertThat(tile.up).isFalse()
            assertThat(tile.right).isFalse()
            assertThat(tile.down).isFalse()
            assertThat(tile.left).isFalse()
          },
          DynamicTest.dynamicTest("1000") {
            val tile = Tile("1000")
            assertThat(tile.up).isTrue()
            assertThat(tile.right).isFalse()
            assertThat(tile.down).isFalse()
            assertThat(tile.left).isFalse()
          },
          DynamicTest.dynamicTest("0100") {
            val tile = Tile("0100")
            assertThat(tile.up).isFalse()
            assertThat(tile.right).isTrue()
            assertThat(tile.down).isFalse()
            assertThat(tile.left).isFalse()
          },
          DynamicTest.dynamicTest("0010") {
            val tile = Tile("0010")
            assertThat(tile.up).isFalse()
            assertThat(tile.right).isFalse()
            assertThat(tile.down).isTrue()
            assertThat(tile.left).isFalse()
          },
          DynamicTest.dynamicTest("0001") {
            val tile = Tile("0001")
            assertThat(tile.up).isFalse()
            assertThat(tile.right).isFalse()
            assertThat(tile.down).isFalse()
            assertThat(tile.left).isTrue()
          }
      )

  @Test
  fun `test vertical movement`() {
    assertThat(Tile.minus.moveUpTo(Tile.minus)).isFalse()
    assertThat(Tile.minus.moveUpTo(Tile.pipe)).isFalse()
    assertThat(Tile.pipe.moveUpTo(Tile.minus)).isFalse()
    assertThat(Tile.pipe.moveUpTo(Tile.pipe)).isTrue()


    assertThat(Tile.minus.moveDownTo(Tile.minus)).isFalse()
    assertThat(Tile.minus.moveDownTo(Tile.pipe)).isFalse()
    assertThat(Tile.pipe.moveDownTo(Tile.minus)).isFalse()
    assertThat(Tile.pipe.moveDownTo(Tile.pipe)).isTrue()
  }

  @Test
  fun `test horizontal movement`() {
    assertThat(Tile.minus.moveRightTo(Tile.minus)).isTrue()
    assertThat(Tile.minus.moveRightTo(Tile.pipe)).isFalse()
    assertThat(Tile.pipe.moveRightTo(Tile.minus)).isFalse()
    assertThat(Tile.pipe.moveRightTo(Tile.pipe)).isFalse()


    assertThat(Tile.minus.moveLeftTo(Tile.minus)).isTrue()
    assertThat(Tile.minus.moveLeftTo(Tile.pipe)).isFalse()
    assertThat(Tile.pipe.moveLeftTo(Tile.minus)).isFalse()
    assertThat(Tile.pipe.moveLeftTo(Tile.pipe)).isFalse()
  }
}
