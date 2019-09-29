package net.lab0.coding.game.simulator.api

interface Output {
  /**
   * The output that the game provides on each game tick.
   */
  fun getStdout(): String
}