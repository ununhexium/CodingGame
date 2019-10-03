import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Silver {

  fun solve() {
    val input = Scanner(System.`in`)

    // game loop
    while (true) {
      parseInput(input)

      preCompute()

      act(
          computeTarget(),
          thrust(),
          shouldBoost(),
          shouldShield()
      )
    }
  }

  // constants
  val maxLaps = 3 // guessing that

  val checkpointRadius = 600
  val podRadius = 400

  // concepts
  data class Vec(val x: Int, val y: Int) {
    // outside of the grid
    companion object {
      val NONE = Vec(Int.MIN_VALUE, Int.MIN_VALUE)
    }

    operator fun plus(vec: Vec) = Vec(this.x + vec.x, this.y + vec.y)

    operator fun minus(vec: Vec) = Vec(this.x - vec.x, this.y - vec.y)

    operator fun times(scalar: Int) = Vec(scalar * this.x, scalar * this.y)

    operator fun times(scalar: Double) = Vec((scalar * this.x).toInt(), (scalar * this.y).toInt())

    operator fun div(scalar: Double) = Vec((this.x / scalar).toInt(), (this.y / scalar).toInt())

    infix fun dot(vec: Vec) = this.x * vec.x + this.y * vec.y

    fun norm() = sqrt(x.toDouble() * x + y * y)

    /**
     * @returns the angle between 2 vectors in radians
     */
    infix fun angleTo(vec: Vec) = atan2(
        this.x.toDouble() * vec.y - this.y * vec.x,
        this.x.toDouble() * vec.x + this.y * vec.y
    )

    fun arg() = atan2(y.toDouble(), x.toDouble())

    fun rotate(radian: Double) =
        Vec(
            (x * cos(radian) - y * sin(radian)).toInt(),
            (x * sin(radian) + y * cos(radian)).toInt()
        )
  }

  data class Player(
      private var internalCurrent: Vec = Vec.NONE,
      private val internalHistory: MutableList<Vec> = mutableListOf()
  ) {
    fun pushPosition(position: Vec) {
      if (current == Vec.NONE) {
        internalHistory.add(position)
      } else {
        internalHistory.add(current)
      }

      internalCurrent = position
    }

    val computedTargetPosition get() = computedTargetVector + me.current

    val computedTargetVector: Vec
      get() {
        // compensate for the momentum
        val approachAngle = me.speed.angleTo(me.rawTargetVector)
        val effectiveApproachAngle = when {
          abs(approachAngle) >= PI / 2 -> PI - approachAngle
          else -> approachAngle
        }

        return me.rawTargetVector.rotate(effectiveApproachAngle)
      }
    /**
     * Aiming straight to the the center of the checkpoint
     */
    val rawTargetVector: Vec get() = nextCheckpoint - current

    val rawTargetPosition: Vec get() = me.current + rawTargetVector

    /**
     * The current position of the player
     */
    val current get() = internalCurrent

    /**
     * The previous position of the player
     */
    val previous get() = internalHistory.last()

    /**
     * The history of the player's positions
     */
    val history: List<Vec> get() = internalHistory

    val speed get() = internalCurrent - previous

    fun estimatedNextPosition(ticks: Int = 1) = internalCurrent + speed * ticks

    fun canReachTheNextCheckpoint(ticks: Int = 1): Boolean =
        (me.estimatedNextPosition(ticks) - nextCheckpoint).norm() < checkpointRadius

  }

  // raw data
  val me = Player()
  val other = Player()
  lateinit var nextCheckpoint: Vec
  var nextCheckpointDistance = 0
  var nextCheckpointAngle = 0.0

  // memory
  var boosted = false

  // states
  var tick = 0

  val checkpoints = mutableListOf<Vec>()

  private fun computeTarget(): Vec {
    return if (me.speed.norm() < 10) {
      nextCheckpoint
    } else {

      // the closer to a target, the more compensation to use
      fun blend(distance: Double) = if (distance < 600) 1.0 else 600 / distance

      val rawPosition = me.rawTargetVector + me.current

      val distanceToNextCheckpoint = (me.current - nextCheckpoint).norm() - 600

      val blendCoefficient = blend(distanceToNextCheckpoint)
      debug("Blend coef: $blendCoefficient")
      return (me.computedTargetPosition * blendCoefficient) + (rawPosition * (1 - blendCoefficient))
    }
  }

  private fun preCompute() {
    tick++
  }

  private fun thrust(): Int {
    val coef =  if (abs(nextCheckpointAngle) >= PI / 2) {
      0.0
    } else {
      when {
        abs(nextCheckpointAngle) >= PI / 4 -> abs(nextCheckpointAngle) * -4 / PI + 2
        else -> 1.0
      }
    }

    return (100 * coef).toInt()
  }

  /**
   * Guesses the best moment to boost
   */
  private fun shouldBoost(): Boolean {
    return nextCheckpointDistance > 5000 && abs(me.speed.angleTo(nextCheckpoint)) < PI / 16
  }

  private fun shouldShield(): Boolean {
    val nextMe = me.estimatedNextPosition()
    val nextOther = other.estimatedNextPosition()
    val willTouch = (nextMe - nextOther).norm() <= 2 * podRadius * 1.02
    val conflictsWithDirection = (me.rawTargetVector dot other.speed) < 0
    val goingInTheRightDirection = me.speed dot me.computedTargetVector > 0

    debug("Will touch: $willTouch")
    debug("Direction conflict: $conflictsWithDirection")

    val shouldShield = willTouch && conflictsWithDirection && goingInTheRightDirection
    debug("Should shield: $shouldShield")

    return shouldShield
  }

  private fun parseInput(input: Scanner) {
    me.pushPosition(
        Vec(
            input.nextInt(), // x position of your pod
            -input.nextInt() // y position of your pod
        )
    )
    nextCheckpoint = Vec(
        input.nextInt(), // x position of the next check point
        -input.nextInt() // y position of the next check point
    )
    nextCheckpointDistance = input.nextInt() // distance to the next checkpoint
    nextCheckpointAngle = input.nextInt() / 180.0 * PI // angle to the next checkpoint

    other.pushPosition(
        Vec(
            input.nextInt(), // x position of opponent pod
            -input.nextInt() // y position of opponent pod
        )
    )
  }

  private fun act(
      target: Vec,
      thrust: Int,
      boost: Boolean,
      shouldShield: Boolean
  ) {

    val thrustAction = if (shouldShield) {
      "SHIELD"
    } else if (boost) {
      if (boosted) "100" else "BOOST"
    } else thrust.toString()

    println("${target.x} ${-target.y} $thrustAction")
  }

  private fun debug(s: String) = System.err.println(s)
}
