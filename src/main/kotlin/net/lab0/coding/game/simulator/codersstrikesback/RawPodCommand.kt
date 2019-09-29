package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.ParseableInput
import net.lab0.coding.game.simulator.api.Input

data class RawPodCommand(val targetX: Int, val targetY: Int, val thrust: String) : Input<RawPodCommand> {
  companion object : ParseableInput<RawPodCommand> {
    /**
     * Two lines:
     * 2 integers for the target coordinates of your pod
     * followed by thrust, the acceleration to give your pod, or by
     * SHIELD to activate the shields, or by
     * BOOST for an acceleration burst. One line per pod.
     */
    override fun parseStdin(input: String): RawPodCommand {
      val split = input.split(' ')
      require(split.size <= 3) {
        "Require 3 elements. Example: ${RawPodCommand(1, 2, "3")}"
      }
      return RawPodCommand(split[0].toInt(), split[1].toInt(), split[2])
    }
  }
}