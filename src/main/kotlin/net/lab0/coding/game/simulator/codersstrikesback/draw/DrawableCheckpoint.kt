package net.lab0.coding.game.simulator.codersstrikesback.draw

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.paint.Color
import javafx.util.Duration
import net.lab0.coding.game.simulator.codersstrikesback.Checkpoint
import net.lab0.coding.game.simulator.codersstrikesback.RacerDrawContext
import net.lab0.coding.game.simulator.ui.AnimatedDrawable

class DrawableCheckpoint(val checkpoint: Checkpoint): AnimatedDrawable<RacerDrawContext> {
  private val xProperty = SimpleDoubleProperty()
  private val yProperty = SimpleDoubleProperty()

  override val keyFrames
    get() = arrayOf(
        KeyFrame(
            Duration.ZERO,
            KeyValue(xProperty, checkpoint.x),
            KeyValue(yProperty, checkpoint.y)
        ),
        KeyFrame(
            Duration.INDEFINITE,
            KeyValue(xProperty, checkpoint.x),
            KeyValue(yProperty, checkpoint.y)
        )
    )

  override fun drawWith(context: RacerDrawContext) {
    with(context) {
      val radius = checkpoint.radius.toDouble() * ratio
      gc.fill = Color.WHITESMOKE
      gc.fillOval(
          xProperty.doubleValue() * ratio - radius,
          checkpoint.y * ratio - radius,
          2 * radius,
          2 * radius
      )
      gc.fill = Color.BLACK
      gc.fillText("${checkpoint.x} ${checkpoint.y}", checkpoint.x.toDouble() * ratio, checkpoint.y.toDouble() * ratio)
    }
  }
}
