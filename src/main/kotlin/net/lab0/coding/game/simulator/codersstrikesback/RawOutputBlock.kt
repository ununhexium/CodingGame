package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Output

data class RawOutputBlock(
    val myPod1: RawPodSituation,
    val myPod2: RawPodSituation,
    val itsPod1: RawPodSituation,
    val itsPod2: RawPodSituation
) : Output {
  /**
   * Input for one game turn
   * First 2 lines: Your two pods.
   * Next 2 lines: The opponent's pods.
   */
  override fun getStdout(): String {
    return """
      |${myPod1.getStdout()}
      |${myPod2.getStdout()}
      |${itsPod1.getStdout()}
      |${itsPod2.getStdout()}
    """.trimMargin()
  }
}