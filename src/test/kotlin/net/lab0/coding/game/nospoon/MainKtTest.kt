package net.lab0.coding.game.nospoon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MainKtTest {
  companion object {
    fun String.prepare() =
        this.trimMargin().split("\n")
  }

  @Test
  fun `Can parse`() {
    val grid = parse(
        """
      |00.
      |000
      |.00
    """.prepare()
    )

    assertThat(grid.size).isEqualTo(3)
    assertThat(grid[0].size).isEqualTo(3)
    assertThat(grid[0]).isEqualTo(listOf(true, true, false))
  }

  @Test
  fun `Can find to the right`() {
    val grid = parse(
        """
      |0...0...
    """.prepare()
    )
    val (x1, y1) = findToRight(grid, 0, 0)
    assertThat(x1).isEqualTo(4)
    assertThat(y1).isEqualTo(0)

    val (x2, y2) = findToRight(grid, 4, 0)
    assertThat(x2).isEqualTo(-1)
    assertThat(y2).isEqualTo(-1)
  }

  @Test
  fun `Can find to the bottom`() {
    val grid = parse(
        """
      |0
      |.
      |.
      |.
      |0
      |.
      |.
      |.
    """.prepare()
    )
    val (x1, y1) = findToBottom(grid, 0, 0)
    assertThat(x1).isEqualTo(0)
    assertThat(y1).isEqualTo(4)

    val (x2, y2) = findToBottom(grid, 0, 4)
    assertThat(x2).isEqualTo(-1)
    assertThat(y2).isEqualTo(-1)
  }

  @Test
  fun `Can solve simplest`() {
    val grid = parse(
        """
      |00
      |0.
    """.prepare()
    )

    val solution = solve(grid)
    assertThat(solution).isEqualTo(
        listOf(
            listOf(0, 0, 1, 0, 0, 1),
            listOf(1, 0, -1, -1, -1, -1),
            listOf(0, 1, -1, -1, -1, -1)
        )
    )
  }

  @Test
  fun `Can solve simple`() {
    val grid = parse(
        """
      |0.0
      |...
      |0..
    """.prepare()
    )

    val solution = solve(grid)
    assertThat(solution).isEqualTo(
        listOf(
            listOf(0, 0, 2, 0, 0, 2),
            listOf(2, 0, -1, -1, -1, -1),
            listOf(0, 2, -1, -1, -1, -1)
        )
    )
  }
}