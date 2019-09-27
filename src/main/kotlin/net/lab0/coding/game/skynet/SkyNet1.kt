package net.lab0.coding.game.skynet

import java.util.*
import java.io.*
import java.math.*

object SkyNet1 {
  fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val N = input.nextInt() // the total number of nodes in the level, including the gateways
    val L = input.nextInt() // the number of links
    val E = input.nextInt() // the number of exit gateways

    val links = (0 until L).flatMap {
      val N1 = input.nextInt() // N1 and N2 defines a link between these nodes
      val N2 = input.nextInt()
      listOf(N1 to N2, N2 to N1)
    }

    val gateways = (0 until E).map {
      val EI = input.nextInt() // the index of a gateway node
      EI
    }

    // game loop
    while (true) {
      val SI = input.nextInt() // The index of the node on which the Skynet agent is positioned this turn

      val toBlock = links.filter { it.first == SI }.firstOrNull { it.second in gateways } ?: links.first()

      // Example: 0 1 are the indices of the nodes you wish to sever the link between
      println(toBlock.string())
    }
  }

  private fun Pair<Int, Int>.string(): String {
    return "$first $second"
  }
}