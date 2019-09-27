import java.util.*
import kotlin.math.abs

var boosted = false

var tick = 0

data class Checkpoint(val x: Int, val y: Int)

val checkpoints = mutableListOf<Checkpoint>()
val positions = mutableListOf<Pair<Int, Int>>()

val maxLaps = 3

fun main(args: Array<String>) {
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

    act(
        nextCheckpointX,
        nextCheckpointY,
        thrust(currentX, currentY, nextCheckpointAngle),
        shouldBoost(nextCheckpointDist, nextCheckpointAngle)
    )
  }
}

fun thrust(currentX: Int, currentY: Int, nextCheckpointAngle: Int): Int {
  return if (nextCheckpointAngle > 90f || nextCheckpointAngle < -90f) 0 else 100
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
