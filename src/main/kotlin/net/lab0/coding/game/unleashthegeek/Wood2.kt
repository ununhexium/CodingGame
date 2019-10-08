package net.lab0.coding.game.unleashthegeek

import java.util.*
import net.lab0.coding.game.unleashthegeek.Wood2.Action.*
import net.lab0.coding.game.unleashthegeek.Wood2.Objective.*
import kotlin.math.abs

object Wood2 {
  private val scanner = Scanner(System.`in`)
  private val random = Random()
  private lateinit var internalArena: Arena
  private val arena get() = internalArena
  /**
   * Deliver more ore to hq (left side of the map) than your opponent. Use radars to find ore but beware of traps!
   **/
  fun solve() {
    val input = scanner
    internalArena = initParser(input)

    // game loop
    while (true) {
      loopParser(input)
      val orders = getObjectives()
      assignMinion(orders)
      act()
    }
  }

  // structures

  data class Position(val x: Int, val y: Int) {
    companion object {
      fun random() = Position(random.nextInt(arena.width), random.nextInt(arena.height))
      fun randomNotInBase() = Position(1 + random.nextInt(arena.width - 1), random.nextInt(arena.height))
    }

    override fun toString() = "$x $y"
    fun offset(x: Int, y: Int): Position {
      // TODO: keep it insie the arena?
      return Position(this.x + x, this.y + y)
    }

    fun distance(pos: Position) = abs(this.x - pos.x) + abs(this.y - pos.y)
  }

  data class Cell(
      val pos: Position,
      var ore: Int? = null,
      var hole: Boolean? = null
  ) {
    companion object {
      val EMPTY = Cell(Position(-1, -1), null, false)
    }
  }

  enum class Type {
    MINE,
    ITS,
    RADAR,
    TRAP,
    ORE,
    NONE
  }

  fun Int.toType() = when (this) {
    -1 -> Type.NONE
    in Type.values().indices -> Type.values()[this]
    else -> TODO("How to scan item type id $this")
  }

  interface Entity {
    val id: Int
    val pos: Position
  }

  interface Robot : Entity // Marker interface

  data class MyRobot(
      override val id: Int,
      override val pos: Position,
      val load: Type,
      var objective: Objective? = null,
      private var lastOrder: Action = Wait()
  ) :
      Robot {
    /**
     * Pops the previous order if it was reached
     */
//    fun clearedOrders(newState: MyRobot): MyRobot {
//      val firstOrder = todo.peek()
//      debug("First order is $firstOrder")
//      return this.copy(
//          todo = when (firstOrder) {
//            is Action.Wait -> todo.popSelf()
//            is Move -> if (newState.pos == firstOrder.pos) todo.popSelf() else todo
//            // TODO: do we see the holes out of the radar coverage? If yes, hole must not be nullable
//            // TODO: check that we are near the position, not exactly on top
//            is Action.Dig -> if (newState.pos.distance(firstOrder.pos) <= 1 && arena[newState.pos].hole == true) todo.popSelf() else todo
//            is RequestRadar -> if (newState.load == Type.RADAR) todo.popSelf() else todo
//            is RequestTrap -> if (newState.load == Type.TRAP) todo.popSelf() else todo
//          }
//      )
//    }

    fun getAction(): Action {
      lastOrder = when (objective) {
        Objective.STOCHASTIC_DIG ->
          if (load == Type.ORE) {
            Move(pos.copy(x = 0))
          } else {
            val lo = lastOrder
            when (lo) {
              is Dig -> if (arena[lo.pos].hole == true) Dig(Position.random()) else lastOrder
              is Move -> if (pos.x == 0) Dig(Position.random()) else lastOrder
              else -> Dig(Position.random())
            }
          }
        else -> Wait()
      }

      return lastOrder
    }
  }

  data class ItsRobot(override val id: Int, override val pos: Position, val load: Type? = null) : Robot
  data class Radar(override val id: Int, override val pos: Position) : Entity
  class Trap(override val id: Int, override val pos: Position) : Entity

  class Scores(var mine: Int, var its: Int)

  class PowerUps(var radarCoolDown: Int, var trapCooldown: Int)

  enum class Objective {
    STOCHASTIC_DIG
  }

  sealed class Action {
    abstract val printed: String
    override fun toString() = printed

    class Wait : Action() {
      override val printed = "WAIT"
    }

    class Move(val pos: Position) : Action() {
      override val printed = "MOVE $pos"
    }

    class Dig(val pos: Position) : Action() {
      override val printed = "DIG $pos"
    }

    class RequestRadar : Action() {
      override val printed = "REQUEST RADAR"
    }

    class RequestTrap : Action() {
      override val printed = "REQUEST TRAP"
    }
  }


  data class Arena(val width: Int, val height: Int) {
    val cells = (0 until height).map { y -> (0 until width).map { x -> Cell(Position(x, y), null, null) } }

    // moving entities as map<id,history>
    val myRobots = mutableMapOf<Int, MutableList<MyRobot>>()
    val itsRobots = mutableMapOf<Int, MutableList<ItsRobot>>()

    // static entities
    val radarsAndTraps = mutableMapOf<Int, MutableSet<Entity>>()

    val radars get() = radarsAndTraps.flatMap { entry -> entry.value.filterIsInstance<Radar>() }

    operator fun get(x: Int, y: Int) = cells[y][x]
    operator fun get(pos: Position) = cells[pos.y][pos.x]

    // TODO: remove non existing entities
    fun updateEntity(entity: Entity) {
      when (entity) {
        is MyRobot -> {
          val myRobots = myRobots.getOrPut(entity.id) { mutableListOf() }
          val existingRobot = myRobots.lastOrNull()
          if (existingRobot != null) {
            myRobots
                .add(
                    existingRobot.copy(
                        pos = entity.pos,
                        load = entity.load
                    )
                )
          } else {
            myRobots.add(entity)
          }
        }
        is ItsRobot -> {
          val existingRobots = itsRobots.getOrPut(entity.id) { mutableListOf() }
          val existingRobot = existingRobots.lastOrNull()
          // make a copy to preserve the existing attributes
          if (existingRobot != null) {
            existingRobot.copy(pos = entity.pos)
          } else {
            existingRobots.add(entity)
          }
        }
        else -> radarsAndTraps.getOrPut(entity.id) { mutableSetOf() }.add(entity)
      }
    }

    fun getKnownOres() = cells.flatten().filter { it.ore != null }.filter { it.ore!! > 0 }

    fun getLazyRobots() = myRobots
        .map { it.value.last() }
        .filter { it.objective == null }
  }

  // game data
  val scores = Scores(0, 0)
  val myPowerUps = PowerUps(0, 0)

  private fun initParser(input: Scanner): Arena {
    val width = input.nextInt()
    val height = input.nextInt() // size of the map
    return Arena(width, height)
  }

  private fun loopParser(input: Scanner) {
    // Amount of ore delivered
    scores.mine = input.nextInt()
    scores.its = input.nextInt()

    (0 until arena.height).map { y ->
      (0 until arena.width).map { x ->
        val ore = input.next() // amount of ore or "?" if unknown
        val hole = input.nextInt() // 1 if cell has a hole

        val cell = arena[x, y]
        cell.ore = when (ore) {
          "?" -> null
          else -> ore.toInt()
        }
        cell.hole = hole == 1
      }
    }

    val entityCount = input.nextInt() // number of entities visible to you
    myPowerUps.radarCoolDown = input.nextInt() // turns left until a new radar can be requested
    myPowerUps.trapCooldown = input.nextInt() // turns left until a new trap can be requested

    (0 until entityCount).forEach { _ ->
      val id = input.nextInt() // unique id of the entity
      val typeId = input.nextInt() // 0 for your robot, 1 for other robot, 2 for radar, 3 for trap
      val x = input.nextInt()
      val y = input.nextInt() // position of the entity
      val pos = Position(x, y)

      // if this entity is a robot, the item it is carrying (-1 for NONE, 2 for RADAR, 3 for TRAP, 4 for ORE)
      val item = input.nextInt().toType()

      arena.updateEntity(
          when (typeId.toType()) {
            Type.MINE -> MyRobot(id, pos, item)
            Type.ITS -> ItsRobot(id, pos)
            Type.RADAR -> Radar(id, pos)
            Type.TRAP -> Trap(id, pos)
            else -> TODO("How to scan type id $typeId")
          }
      )
    }
  }

  private fun assignMinion(orders: List<Objective>) {
    val lazyRobots = arena.getLazyRobots()
    lazyRobots.zip(orders).forEach {
      it.first.objective = it.second
    }
  }

  private fun getObjectives(): List<Objective> {

    // find something to mine

//    val knowOres = arena.getKnownOres()
//    when (knowOres.size) {
//      0 -> {
//        debug("No ore found, dowsing and random pick")
//        val prospectingTargets = dowser(arena)
//        val target = prospectingTargets.first()
//        // TODO test if radar is available
//        actions.add(
//            actionList() + RequestRadar() + Dig(target)
//        )
//        val targets = listOf(
//            target.offset(-2, -2),
//            target.offset(2, -2),
//            target.offset(-2, 2),
//            target.offset(2, 2)
//        )
//        actions.addAll(
//            targets.map {
//              actionList() + Dig(it)
//            }
//        )
//      }
//      in 1..4 -> {
//        debug("${knowOres.size} ore found, dowsing ahead")
//        val prospectingTargets = dowser(arena)
//        val target = prospectingTargets.first()
//        // TODO test if radar is available
//        actions.add(
//            actionList() + RequestRadar() + Move(target) + Dig(target)
//        )
//      }
//      else ->
//        // get the ore
//        actions.addAll(
//            knowOres.take(5).map {
//              actionList() + Dig(it.pos)
//            }
//        )
//    }

    return Array(5) { STOCHASTIC_DIG }.toList()
  }

  /**
   * @return the best places to put a radar
   */
  private fun dowser(): List<Position> =
      // wood: constants
      (4 until arena.width step 4).flatMap { x ->
        (4 until arena.height step 4).map { y ->
          Position(x, y)
        }
      }.filter {
        // TODO: cache radars positions instead of recomputing
        it !in arena.radars.map { it.pos }
      }

  fun act() {
    val act = arena.myRobots.map {
      it.value.last().getAction()
    }.joinToString("\n") {
      it.printed
    }
    println(act)
  }

  fun actionList() = LinkedList<Action>()
  operator fun LinkedList<Action>.plus(action: Action) = this.also { add(action) }
  fun LinkedList<Action>.popSelf() = LinkedList(this).also { it.removeFirst() }
  fun debug(vararg message: String) = System.err.println("DBG " + message.joinToString())
}