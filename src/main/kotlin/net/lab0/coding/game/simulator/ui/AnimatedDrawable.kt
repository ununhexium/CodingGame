package net.lab0.coding.game.simulator.ui

import javafx.animation.KeyFrame
import net.lab0.coding.game.simulator.codersstrikesback.RacerDrawContext

interface AnimatedDrawable<T> where T: Context<T> {
  val keyFrames: Array<KeyFrame>
  fun drawWith(context: T)
}
