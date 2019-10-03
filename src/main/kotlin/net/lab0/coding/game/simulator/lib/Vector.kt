package net.lab0.coding.game.simulator.lib

interface Vector<T> where T:Number {
  val x: T
  val y: T

  /**
   * @return a new vector which value is the sum of this vector and the other vector
   */
  operator fun plus(other: Vector<T>): Vector<T>

  /**
   * @return a new vector which value is the difference of this vector and the other vector
   */
  operator fun minus(other: Vector<T>): Vector<T>

  /**
   * @return a new vector which value is this vector multiplied by a scalar
   */
  operator fun times(scalar: T): Vector<T>

  /**
   * @return a new vector which value is this vector multiplied by a scalar
   */
  operator fun times(scalar: Double): Vector<T>

  /**
   * @return a new vector which value is this vector divided by a scalar (vector * (1/scalar)
   */
  operator fun div(scalar: Double) = this * (1 / scalar)

  /**
   * @return a new Vector which value is the dot product of this vector and the other vector
   */
  infix fun dot(other: Vector<T>): T

  /**
   * @return the norm 2 of this vector
   */
  fun norm(): Double

  /**
   * @returns the oriented angle from this vector to the other vector
   */
  infix fun angleTo(other: Vector<T>): Double

  /**
   * @return the argument of this vector
   */
  fun arg(): Double

  /**
   * @return a new vector which value is this vector rotated by the given angle
   */
  fun rotate(radian: Double): Vector<T>
}
