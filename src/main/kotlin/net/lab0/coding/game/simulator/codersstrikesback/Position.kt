package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Output
import net.lab0.coding.game.simulator.lib.Vector

interface Position : Output, Vector<Int> {
  override fun getStdout() = "$x $y"
}
