package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.lib.IntVec
import net.lab0.coding.game.simulator.lib.Vector

interface Speed: Vector<Int> {
  companion object {
    operator fun invoke(x:Int, y:Int): Speed =
        object: Vector<Int> by IntVec(x,y), Speed {
          override val x = x
          override val y = y
        }
  }
}
