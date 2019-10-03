package net.lab0.coding.game.simulator.codersstrikesback

import net.lab0.coding.game.simulator.api.Output


/**
 * Each pod is represented by: 6 integers,
 * @param x for the position.
 * @param y for the position.
 * @param vx for the speed vector.
 * @param vy for the speed vector.
 * @param angle for the rotation angle in degrees.
 * @param nextCheckPointId for the number of the next checkpoint the pod must go through.
 */
data class PodSnapshot(
    val x: Int,
    val y: Int,
    val vx: Int,
    val vy: Int,
    val angle: Int,
    val nextCheckPointId: Int,
    val radius: Int = 400
) : Output {
  constructor(position: Position, speed: Speed, angle: Int, nextCheckPointId: Int) :
      this(position.x, position.y, speed.x, speed.y, angle, nextCheckPointId)

  override fun getStdout() = "$x $y $vx $vy $angle $nextCheckPointId"
}
