package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Game
import java.lang.IllegalStateException
import kotlin.random.Random

class CodersStrikesBack : Game<RawInitialisationOutput, RawInputBlock, RawOutputBlock> {

  val random = Random
  var initialised = false

  val laps = random.nextInt(1, 5)
  val checkpoints = (1..random.nextInt(2, 9)).map {
    Checkpoint(
        random.nextInt(0, 16000),
        random.nextInt(0, 9000)
    )
  }

  val myPod1 = RawPodSituation(
      
  )

  override fun initialise(): RawInitialisationOutput {
    check(!initialised) { "Only 1 init per game allowed" }

    initialised = true

    return RawInitialisationOutput(laps, checkpoints)
  }

  override fun computeTick(input: RawInputBlock): RawOutputBlock {
    TODO("not implemented")
  }
}
