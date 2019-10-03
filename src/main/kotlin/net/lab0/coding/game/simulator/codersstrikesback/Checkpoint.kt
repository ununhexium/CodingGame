package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.lib.IntVec
import net.lab0.coding.game.simulator.lib.Vector

/**
 * @param x for the coordinates of checkpoint
 * @param y for the coordinates of checkpoint
 */
data class Checkpoint(override val x: Int, override val y: Int, val id: Int, val radius: Int = 600) :
    Position, Vector<Int> by IntVec(x, y)
