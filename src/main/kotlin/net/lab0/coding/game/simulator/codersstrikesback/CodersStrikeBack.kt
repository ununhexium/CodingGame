package net.lab0.coding.game.simulator.codersstrikesback

import javafx.animation.AnimationTimer
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.application.Application
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Duration
import net.lab0.coding.game.simulator.api.Game
import net.lab0.coding.game.simulator.codersstrikesback.draw.DrawableCheckpoint
import net.lab0.coding.game.simulator.codersstrikesback.draw.DrawablePod
import net.lab0.coding.game.simulator.ui.ResizableCanvas
import kotlin.math.PI
import kotlin.math.min
import kotlin.random.Random


class CodersStrikeBack : Game<RawInitialisationOutput, RawInputBlock, RawOutputBlock> {

  private val random = Random
  var initialised = false

  val laps = random.nextInt(1, 5)
  // TODO: checkpoints may not touch each other?
  internal val checkpoints = (1..random.nextInt(2, 9)).map {
    Checkpoint(
        random.nextInt(0, 16000),
        random.nextInt(0, 9000),
        it
    )
  }

  internal val history = mutableListOf<Frame>()

  override fun initialize(): Pair<RawInitialisationOutput, RawOutputBlock> {
    check(!initialised) { "Only 1 init per game allowed" }

    history.add(
        Frame(
            listOf(
                PodSnapshot(
                    checkpoints.first(),
                    Speed(0, 0),
                    (checkpoints[0].angleTo(checkpoints[1]) * 180 / PI).toInt(),
                    checkpoints[1].id
                )
            ),
            listOf()
        )
    )

    initialised = true

    return RawInitialisationOutput(laps, checkpoints) to RawOutputBlock(history.last().myPods, history.last().itsPods)
  }

  override fun computeTick(input: RawInputBlock): RawOutputBlock {
    TODO("not implemented")
  }
}
