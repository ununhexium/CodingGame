package net.lab0.coding.game.tutorial

import java.util.Scanner

fun main(args : Array<String>) {
  val input = Scanner(System.`in`)

  data class Ennemy(val name:String, val distance:Double)

  // game loop
  while (true) {
    val ennemies = mutableListOf<Ennemy>()
    ennemies.add(
        Ennemy(input.next(), input.nextDouble())
    )
    ennemies.add(
        Ennemy(input.next(), input.nextDouble())
    )

    val target = ennemies.sortedBy {
      it.distance
    }.first().name

    println(target)
  }
}