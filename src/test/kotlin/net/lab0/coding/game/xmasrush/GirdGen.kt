package net.lab0.coding.game.xmasrush

import Xmas.pickOne

fun main(args: Array<String>) {
  val sources = " ╷╵╴╶┘└┐┌│─┬┴├┤┼"
  val used = sources.drop(5)
  val size = 7
  val count = 10

  (1..count).onEach {
    val visual = (1..size).map { row ->
      (1..size).map { col ->
        used.toList().pickOne()
      }
    }.joinToString("\n", postfix = "\n\n${"-".repeat(size)}\n") {
      it.joinToString("")
    }

    println(visual)
  }

}
