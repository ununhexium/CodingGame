package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Input
import net.lab0.coding.game.simulator.api.ParseableInput

data class RawInputBlock(val line1: RawPodCommand, val line2: RawPodCommand) : Input<RawInputBlock> {

  companion object : ParseableInput<RawInputBlock> {
    override fun parseStdin(input: String): RawInputBlock {
      val split = input.split('\n')
      return RawInputBlock(
          RawPodCommand.parseStdin(split[0]),
          RawPodCommand.parseStdin(split[1])
      )
    }
  }
}
