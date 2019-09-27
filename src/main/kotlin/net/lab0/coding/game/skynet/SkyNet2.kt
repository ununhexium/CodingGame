package net.lab0.coding.game.skynet

import java.util.*

object SkyNet2 {

  data class Segment(val distance: Int, val from: String?)

  /**
   * TODO: find the node such that:
   * the distance between the node and skynet is N
   * the number of gateways connecting to that node is N (or less)
   *
   */
  fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val N = input.nextInt() // the total number of nodes in the level, including the gateways
    val L = input.nextInt() // the number of links
    val E = input.nextInt() // the number of exit gateways

    val links = (0 until L).flatMap {
      /*
       * Considering node ids as names.
       * Makes it simpler to follow what's going on in path finding.
       */
      val N1 = input.nextInt().toString() // N1 and N2 defines a link between these nodes
      val N2 = input.nextInt().toString()
      listOf(N1 to N2, N2 to N1)
    }.toMutableList()

    val gateways = (0 until E).map {
      val EI = input.nextInt().toString() // the index of a gateway node
      EI
    }

    // game loop
    while (true) {
      val SI = input.nextInt().toString() // The index of the node on which the Skynet agent is positioned this turn

      val shortestPaths = gateways.mapNotNull {
        path(links, it, SI)
      }.groupBy {
        it.size
      }.toSortedMap().entries.first().value


      val first2Nodes = if (shortestPaths.size == 1) {
        shortestPaths.first().take(2)
      } else {
        /*
         * there may be more than a single shortest path.
         * If such is the case, block the path for which the 1-before-last node is the most frequent
         */
        // how many paths have the same number of source node
        shortestPaths
            .groupBy { it[1] } // how many paths have the same number of source node
            .entries.maxBy { it.value.size }!! // take the one with the highest node appearance count
            .value.first().take(2)
      }

      val toBlock = if (first2Nodes != null) {
        first2Nodes[0] to first2Nodes[1]
      } else {
        links.first {
          it.first in gateways
        }
      }

      links.remove(toBlock)
      links.remove(toBlock.flip())

      // Example: 0 1 are the indices of the nodes you wish to sever the link between
      println(toBlock.string())
    }
  }

  /**
   * Dijkstra, assuming all paths have a weight of 1
   */
  fun path(links: List<Pair<String, String>>, start: String, end: String): List<String>? {
    // special case that would otherwise break the algorithm below
    if (start == end) {
      return listOf()
    }

    // the shortest distance to each of the nodes
    val shortest = mutableMapOf(start to Segment(0, null))

    // elements to be explored
    val todo = mutableListOf(start)

    while (todo.isNotEmpty()) {
      val current = todo.first()
      val currentCost = shortest[current]!!.distance + 1
      links
          .filter { it.first == current }
          .map { it.second }
          .distinct()
          .filter {
            // keep the better paths
            val target = shortest[it]
            target == null || target.distance > currentCost
          }
          .forEach {
            shortest[it] = Segment(currentCost, current)
            todo.add(it)
          }

      todo.removeAt(0)
    }

    val endSegment = shortest[end]
    return if (endSegment == null) {
      null
    } else {
      var source = endSegment.from
      val path = mutableListOf(end)
      while (source != null) {
        path.add(0, source)
        source = shortest[source]!!.from
      }
      return path
    }
  }

  private fun <A, B> Pair<A, B>.string() = "$first $second"
  private fun <A> Pair<A, A>.flip(): Pair<A, A> = second to first
}


