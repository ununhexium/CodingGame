package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Output

/**
 * Initialization input
 * @param laps Line 1 : the number of laps to complete the race.
 * @param checkpointCount Line 2 : the number of checkpoints in the circuit.
 * @param checkpoints `checkpointCount` checkpoints
 */
data class RawInitialisationOutput(
    val laps: Int,
    val checkpoints: List<Checkpoint>
) : Output {
  constructor(laps: Int, vararg checkpoints: Checkpoint) : this(laps, checkpoints.toList())

  val checkpointCount get() = checkpoints.size

  override fun getStdout() = """
    |$laps
    |$checkpointCount
    |${checkpoints.joinToString("\n") { it.getStdout() }}
  """.trimMargin()
}
