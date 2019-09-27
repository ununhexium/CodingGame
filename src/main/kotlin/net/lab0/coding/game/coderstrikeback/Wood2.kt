package net.lab0.coding.game.coderstrikeback

import java.util.*
import kotlin.math.acos
import kotlin.math.sqrt

object Wood2 {

  var previousX: Int = 0
  var previousY: Int = 0

  var round = 0

  fun main(args: Array<String>) {
    val input = Scanner(System.`in`)

    // game loop
    while (true) {
      round++
      val currentX = input.nextInt() // x position of your pod
      val currentY = input.nextInt() // y position of your pod
      val nextCheckpointX = input.nextInt() // x position of the next check point
      val nextCheckpointY = input.nextInt() // y position of the next check point
      val nextCheckpointDist = input.nextInt() // distance to the next checkpoint
      val nextCheckpointAngle = input.nextInt() // angle to the next checkpoint

      val opponentX = input.nextInt()
      val opponentY = input.nextInt()

      if (round == 1) {
        // kick it in motion
        act(nextCheckpointX, nextCheckpointY, 100)
      } else {
        act(nextCheckpointX, nextCheckpointY, thrust(currentX, currentY, nextCheckpointAngle))
      }

      previousX = currentX
      previousY = currentY
    }
  }

  fun thrust(currentX: Int, currentY: Int, nextCheckpointAngle: Int): Int {
    return if (nextCheckpointAngle > 90f || nextCheckpointAngle < -90f) 0 else 100
  }

  fun getSpeed(currentX: Int, currentY: Int) =
      (currentX - previousX) to (currentY - previousY)


  /**
   * @returns the angle between 2 vectors `a[ax,ay]` and `b[bx,by]`
   */
  fun vectorAngle(ax: Int, ay: Int, bx: Int, by: Int): Double {
    val dotProduct = ax * bx + ay * by

    val normA = norm(ax.toDouble(), ay.toDouble())
    val normB = norm(bx.toDouble(), by.toDouble())

    return acos(dotProduct / (normA * normB))
  }

  /**
   * @return the norm of a vector
   */
  fun norm(x: Double, y: Double): Double =
      sqrt(x * x + y * y)

  fun act(targetX: Int, targetY: Int, thrust: Int) {
    println("$targetX $targetY $thrust")
  }

}