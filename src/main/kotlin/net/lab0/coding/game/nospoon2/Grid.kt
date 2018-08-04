package net.lab0.coding.game.nospoon2

class Grid(
    val nodes: Nodes
) {
    /**
     * When 2 nodes are linked
     */
    val links: MutableList<Pair<Node, Node>> = mutableListOf()
    /**
     * When the space is used by an existing link
     */
    val busyIndex: MutableList<MutableList<Boolean>> = nodes.map { list ->
        list.map { node ->
            node != null
        }.toMutableList()
    }.toMutableList()

    val isFullyLinked
        get() = with(links.flatMap {
            listOf(it.first, it.second)
        }.groupBy {
            it
        }.map {
            it.key.links == it.value.size
        }) {
            this.isNotEmpty() && all { it }
        }

    fun node(x: Int, y: Int) = this.nodes[y][x]
        ?: throw IllegalArgumentException("No node at $x,$y")

    fun node(pair: Pair<Int, Int>) = this.nodes[pair.second][pair.first]
        ?: throw IllegalArgumentException("No node at ${pair.first},${pair.second}")

    fun busy(x: Int, y: Int) = this.busyIndex[y][x]

    fun setBusy(x: Int, y: Int) {
        this.busyIndex[y][x] = true
    }

    fun link(a: Node, b: Node) {
        if (a.x != b.x && a.y != b.y) {
            throw IllegalArgumentException(
                "Nodes must be on the same line or on the same column"
            )
        }

        links.add(a to b)
        if (a.x == b.x) {
            autoRange(a.y, b.y).forEach {
                setBusy(a.x, it)
            }
        }
        if (a.y == b.y) {
            autoRange(a.x, b.x).forEach {
                setBusy(it, a.y)
            }
        }
    }

    fun firstNode() =
        this.nodes.first { line ->
            line
                .mapNotNull { it }
                .any {
                    it.links > 0
                }
        }.mapNotNull { it }.first {
            it.links > 0
        }.asPair
}
