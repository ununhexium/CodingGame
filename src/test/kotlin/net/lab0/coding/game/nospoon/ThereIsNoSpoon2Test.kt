package net.lab0.coding.game.nospoon

import net.lab0.coding.game.nospoon2.Grid
import net.lab0.coding.game.nospoon2.Node
import net.lab0.coding.game.nospoon2.ThereIsNoSpoon2
import net.lab0.coding.game.nospoon2.indexOfFirstAfter
import net.lab0.coding.game.nospoon2.indexOfLastBefore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ThereIsNoSpoon2Test {
    companion object {
        fun String.prepare(): List<String> {
            val list = this.trimMargin().split("\n")
            spoon.height = list.size
            spoon.width = list[0].length
            return list
        }

        lateinit var spoon: ThereIsNoSpoon2

        val cross = """
          |..1...
          |......
          |1.4..1
          |......
          |..1...
        """
        val crossLine = """
          |..1..
          |.....
          |1...1
          |.....
          |..1..
        """
    }

    @BeforeEach
    fun `before each`() {
        spoon = ThereIsNoSpoon2()
    }

    private fun List<String>.parsed() = Grid(
        spoon.parse(this)
    )

    @Test
    fun `can parse`() {
        val input = """
          |12345
          |678..
        """.prepare()

        val output = spoon.parse(input)
        assertThat(output).isEqualTo(
            listOf(
                listOf(1, 2, 3, 4, 5).mapIndexed { i, links ->
                    Node(i, 0, links)
                },
                listOf(6, 7, 8).mapIndexed { i, links ->
                    Node(i, 1, links)
                } + listOf(null, null)
            )
        )
    }

    @Test
    fun `doesn't break on no neighbor`() {
        val grid = """
          |...
          |.1.
          |...
        """.prepare().parsed()

        val around = spoon.findNeighbours(grid, 1, 1)
        around.all.forEach {
            assertThat(it).isNull()
        }
    }

    @Test
    fun `index of first first after`() {
        val list = listOf(false, false, true, false, false)
        assertThat(
            list.indexOfFirstAfter(2) { it }
        ).isEqualTo(-1)
        assertThat(
            list.indexOfFirstAfter(1) { it }
        ).isEqualTo(2)
        assertThat(
            list.indexOfFirstAfter(0) { it }
        ).isEqualTo(2)
    }

    @Test
    fun `index of first last before`() {
        val list = listOf(false, false, true, false, false)
        assertThat(
            list.indexOfLastBefore(2) { it }
        ).isEqualTo(-1)
        assertThat(
            list.indexOfLastBefore(3) { it }
        ).isEqualTo(2)
        assertThat(
            list.indexOfLastBefore(4) { it }
        ).isEqualTo(2)
    }

    @Test
    fun `finds neighbors in all directions`() {
        val grid = cross.prepare().parsed()

        val around = spoon.findNeighbours(grid, 2, 2)

        assertThat(around.up).isEqualTo(2 to 0)
        assertThat(around.down).isEqualTo(2 to 4)
        assertThat(around.left).isEqualTo(0 to 2)
        assertThat(around.right).isEqualTo(5 to 2)

        // doesn't break in corners
        val aroundTopLeft = spoon.findNeighbours(grid, 0, 0)
        assertThat(aroundTopLeft.up).isNull()
        assertThat(aroundTopLeft.down).isEqualTo(0 to 2)
        assertThat(aroundTopLeft.left).isNull()
        assertThat(aroundTopLeft.right).isEqualTo(2 to 0)

        val aroundBottomRight = spoon.findNeighbours(grid, 5, 4)
        assertThat(aroundBottomRight.up).isEqualTo(5 to 2)
        assertThat(aroundBottomRight.down).isNull()
        assertThat(aroundBottomRight.left).isEqualTo(2 to 4)
        assertThat(aroundBottomRight.right).isNull()
    }

    @Test
    fun `can link`() {
        val grid = cross.prepare().parsed()
        val a = grid.node(0, 2)
        val b = grid.node(2, 2)
        grid.link(a, b)

        val copy = grid.busyIndex.map {
            it.map {
                it
            }.toMutableList()
        }.toMutableList()

        assertThat(grid.links.last()).isEqualTo(a to b)
        copy[2][1] = true
        assertThat(grid.busyIndex).isEqualTo(copy)
    }

    @Test
    fun `can not cross lines when finding neighbours`() {
        val grid = crossLine.prepare().parsed()
        val a = grid.node(0, 2)
        val b = grid.node(4, 2)
        grid.link(a, b)

        val around = spoon.findNeighbours(grid, 2, 0)

        // null because can't cross the horizontal line
        assertThat(around.down).isNull()

        assertThat(spoon.crossesExistingLink(grid, 0 to 1, 5 to 1)).isFalse()
        assertThat(spoon.crossesExistingLink(grid, 1 to 4, 4 to 4)).isTrue()
    }

    @Test
    fun `fully linked`() {
        val grid = cross.prepare().parsed()

        assertThat(grid.isFullyLinked).isFalse()

        // link everything manually
        listOf(
            Pair(0 to 2, 2 to 2),
            Pair(5 to 2, 2 to 2),
            Pair(2 to 0, 2 to 2),
            Pair(2 to 4, 2 to 2)
        ).forEach {
            grid.link(
                grid.node(it.first),
                grid.node(it.second)
            )
        }

        assertThat(grid.isFullyLinked).isTrue()
    }

    @Test
    fun `can build tree`() {
        val grid = cross.prepare().parsed()
        val found = spoon.buildTree(grid)
    }
}

