import java.awt.geom.Point2D.distance
import java.util.*
import kotlin.math.abs
import kotlin.math.min

fun main(args: Array<String>) {
  Bronze1.solve()
}

object Bronze1 {
  var boosted = false

  var tick = 0

  data class Checkpoint(val x: Int, val y: Int)

  val checkpoints = mutableListOf<Checkpoint>()
  val positions = mutableListOf<Pair<Int, Int>>()

  val maxLaps = 3

  val checkpointRadius = 600

  fun solve() {
    val input = Scanner(System.`in`)

    // game loop
    while (true) {
      tick++
      val currentX = input.nextInt() // x position of your pod
      val currentY = input.nextInt() // y position of your pod
      val nextCheckpointX = input.nextInt() // x position of the next check point
      val nextCheckpointY = input.nextInt() // y position of the next check point
      val nextCheckpointDist = input.nextInt() // distance to the next checkpoint
      val nextCheckpointAngle = input.nextInt() // angle to the next checkpoint

      val opponentX = input.nextInt()
      val opponentY = input.nextInt()

      val targetX = nextCheckpointX
      val targetY = nextCheckpointY

      act(
          targetX,
          targetY,
          thrust(currentX, currentY, nextCheckpointX, nextCheckpointY, nextCheckpointAngle),
          shouldBoost(nextCheckpointDist, nextCheckpointAngle)
      )
    }
  }

  fun thrust(currentX: Int, currentY: Int, nextCheckpointX: Int, nextCheckpointY: Int, nextCheckpointAngle: Int): Int {
    return when {
      nextCheckpointAngle > 90f || nextCheckpointAngle < -90f -> 0
      else -> min(
          100.0,
          distance(currentX.toDouble(), currentY.toDouble(), nextCheckpointX.toDouble(), nextCheckpointY.toDouble())
      ).toInt()
    }

  }

  /**
   * Guesses the best moment to boost
   */
  fun shouldBoost(nextCheckpointDist: Int, nextCheckpointAngle: Int): Boolean {
    return nextCheckpointDist > 5000 && abs(nextCheckpointAngle) < 5
  }

  fun countLaps(): Int {
    return checkpoints.count { it == checkpoints.first() }
  }

  fun act(targetX: Int, targetY: Int, thrust: Int, boost: Boolean = false) {
    val thrustAction = if (boost) {
      if (boosted) 100 else "BOOST"
    } else thrust.toString()

    println("$targetX $targetY $thrustAction")
  }


}
