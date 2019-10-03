package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Output

data class RawOutputBlock(
    val myPods: List<PodSnapshot>,
    val itsPods: List<PodSnapshot>
) : Output {
  /**
   * Input for one game turn
   * First 2 lines: Your two pods.
   * Next 2 lines: The opponent's pods.
   */
  override fun getStdout(): String {
    return """
      |${myPods.joinToString("\n") { it.getStdout() }}
      |${itsPods.joinToString("\n") { it.getStdout() }}
    """.trimMargin()
  }
}