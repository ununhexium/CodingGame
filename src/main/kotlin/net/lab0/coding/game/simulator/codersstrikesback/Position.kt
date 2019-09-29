package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Output

interface Position : Output {
  val x: Int
  val y: Int
  override fun getStdout() = "$x $y"
}
