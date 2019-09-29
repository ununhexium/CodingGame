package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Output

/**
 * @param x for the coordinates of checkpoint
 * @param y for the coordinates of checkpoint
 */
data class Checkpoint(override val x: Int, override val y: Int) : Position
