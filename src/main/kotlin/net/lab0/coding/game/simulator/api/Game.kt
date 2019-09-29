package net.lab0.coding.game.simulator.api

interface Game<INIT, IN, OUT> where IN : Input<IN>, OUT : Output, INIT : Output {
  /**
   * Initializes the game and output the initial situation
   */
  fun initialise(): INIT

  /**
   * Computes all the required operations for a single tick
   */
  fun computeTick(input: IN): OUT
}
