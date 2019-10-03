package net.lab0.coding.game.simulator.codersstrikesback.draw

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.paint.Color
import javafx.util.Duration
import net.lab0.coding.game.simulator.codersstrikesback.PodSnapshot
import net.lab0.coding.game.simulator.codersstrikesback.RacerDrawContext
import net.lab0.coding.game.simulator.lib.IntVec
import net.lab0.coding.game.simulator.ui.AnimatedDrawable
import kotlin.math.PI

// TODO: list of pod in order to show an animation
class DrawablePod(private val pods: List<PodSnapshot>) : AnimatedDrawable<RacerDrawContext> {
  private val xProperty = SimpleDoubleProperty()
  private val yProperty = SimpleDoubleProperty()
  private val angleProperty = SimpleDoubleProperty()
  private val radius = pods.first().radius // should not change

  override val keyFrames by lazy {
    pods.mapIndexed { idx, it ->
      KeyFrame(
          Duration.seconds(idx.toDouble()),
          KeyValue(xProperty, it.x),
          KeyValue(yProperty, it.y),
          KeyValue(angleProperty, it.angle / 180.0 * PI)
      )
    }.toTypedArray()
  }

  override fun drawWith(context: RacerDrawContext) {
    with(context) {
      gc.fill = Color.ORANGE
      val xPod = xProperty.get() * ratio
      val yPod = yProperty.get() * ratio
      val scaledRadius = radius * ratio
      gc.strokeOval(
          xPod - scaledRadius,
          yPod - scaledRadius,
          2 * scaledRadius,
          2 * scaledRadius
      )
      val direction = IntVec(2 * radius, 0).rotate(angleProperty.get()) * ratio
      gc.strokeLine(
          xPod,
          yPod,
          xPod + direction.x.toDouble() * ratio,
          yPod + direction.y.toDouble() * ratio
      )
      gc.fill = Color.BLACK
      gc.fillText("${xPod.toInt()} ${yPod.toInt()}", xPod * ratio, yPod * ratio)
    }
  }
}
