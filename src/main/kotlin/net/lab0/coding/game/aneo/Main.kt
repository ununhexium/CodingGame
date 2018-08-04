package net.lab0.coding.game.aneo

import java.util.Scanner

/**
 * https://www.codingame.com/ide/puzzle/aneo
 */
fun main(args: Array<String>) {
  val input = Scanner(System.`in`)
  val speed = input.nextInt()
  val lightCount = input.nextInt()

  class Light(val distance: Int, val duration: Int)

  (0 until lightCount).map {
    Light(input.nextInt(), input.nextInt())
  }

  println("answer")
}
