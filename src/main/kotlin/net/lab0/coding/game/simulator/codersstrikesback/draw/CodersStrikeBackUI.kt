package net.lab0.coding.game.simulator.codersstrikesback.draw

import javafx.animation.AnimationTimer
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import net.lab0.coding.game.simulator.codersstrikesback.CodersStrikeBack
import net.lab0.coding.game.simulator.codersstrikesback.RacerDrawContext
import net.lab0.coding.game.simulator.ui.ResizableCanvas
import kotlin.math.min

class CodersStrikeBackUI : Application() {
  val game = CodersStrikeBack()


  override fun start(stage: Stage) {
    game.initialize()

    val drawableCheckpoints = game.checkpoints.map {
      DrawableCheckpoint(it)
    }

    val drawablePod =
        DrawablePod(
            when (game.history.size) {
              1 -> {
                val first = game.history.map {
                  it.myPods.first()
                }.first()

                listOf(first, first)
              }
              else -> game.history.map {
                it.myPods.first()
              }
            }
        )

    val timeline = Timeline(
        *drawableCheckpoints
            .map { it.keyFrames }
            .toTypedArray()
            .flatten()
            .toTypedArray(),
        *drawablePod.keyFrames
    )

    timeline.isAutoReverse = true
    timeline.cycleCount = Timeline.INDEFINITE

    val canvas = ResizableCanvas()

    val timer = object : AnimationTimer() {
      override fun handle(now: Long) {
        val gc = canvas.graphicsContext2D
        gc.fill = Color.DIMGRAY
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)
        val context = RacerDrawContext(
            min(canvas.width / 16000.0, canvas.height / 9000.0),
            canvas,
            gc
        )

        drawableCheckpoints.forEach {
          it.drawWith(context)
        }

        drawablePod.drawWith(context)
      }
    }

    val scene = Scene(Group(canvas))
    scene.widthProperty().addListener { _, _, _ ->
      canvas.resize(scene.width, scene.height)
    }
    scene.heightProperty().addListener { _, _, _ ->
      canvas.resize(scene.width, scene.height)
    }

    stage.scene = scene
    stage.show()

    timer.start()
    timeline.play()
  }
}