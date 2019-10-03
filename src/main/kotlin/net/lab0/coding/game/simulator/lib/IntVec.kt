package net.lab0.coding.game.simulator.lib

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class IntVec(override val x: Int, override val y: Int): Vector<Int> {
    companion object {
      val MIN = IntVec(Int.MIN_VALUE, Int.MIN_VALUE)
    }

    override operator fun plus(other: Vector<Int>) = IntVec(
        this.x + other.x,
        this.y + other.y
    )

    override operator fun minus(other: Vector<Int>) = IntVec(
        this.x - other.x,
        this.y - other.y
    )

    override operator fun times(scalar: Int) = IntVec(scalar * this.x, scalar * this.y)

    override operator fun times(scalar: Double) = IntVec(
        (scalar * this.x).toInt(),
        (scalar * this.y).toInt()
    )

    override infix fun dot(other: Vector<Int>) = this.x * other.x + this.y * other.y

    override fun norm() = sqrt(x.toDouble() * x + y * y)

    /**
     * @returns the angle between 2 vectors in radians
     */
    override infix fun angleTo(other: Vector<Int>) = atan2(
        this.x.toDouble() * other.y - this.y * other.x,
        this.x.toDouble() * other.x + this.y * other.y
    )

    override fun arg() = atan2(y.toDouble(), x.toDouble())

    override fun rotate(radian: Double) =
        IntVec(
            (x * cos(radian) - y * sin(radian)).toInt(),
            (x * sin(radian) + y * cos(radian)).toInt()
        )
  }