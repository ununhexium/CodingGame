import java.util.*
import Bronze2.Action.*
import Bronze2.Type.*
import kotlin.math.abs

fun main(vararg args: String) {
  Bronze2.solve()
}

object Bronze2 {
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
      fun randomNotInBase() = Position(1 + random.nextInt(arena.width - 1), random.nextInt(arena.height))
    }

    override fun toString() = "$x $y"
    fun distance(pos: Position) = abs(this.x - pos.x) + abs(this.y - pos.y)
    fun isNotBase() = x > 0
    fun toBase() = this.copy(x = 0)
  }

  // TODO: immutable
  data class Cell(
      val pos: Position,
      var ore: Int? = null,
      var hole: Boolean? = null,
      var radar: Boolean = false,
      var trap: Boolean = false
  ) {
    fun shouldNotDig() = hole == true && ore == 0 || hole == true && ore == null || trap

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
    -1 -> NONE
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
      internal var internalLastOrder: Action = Wait()
  ) : Robot {
    /**
     * Pops the previous order if it was reached
     */

    val atBase get() = pos.x == 0

    val lastOrder get() = internalLastOrder

    fun getAction(): Action {
      internalLastOrder = objective?.nextOrder(this) ?: Wait()
      return internalLastOrder
    }

    fun getBestRadarSpot() = Dig(
        idealRadarLocations().firstOrNull { pos -> pos !in arena.radars } ?: this.pos.copy(x = 1)
    )

    fun getBestDiggingSpot(): Dig {
      val usableOre = arena.getSafeOres()
      return if (usableOre.isEmpty()) {
        debug("tick = ${arena.tick}")
        if(arena.tick == 1) {
          Dig(pos.copy(x=10))
        }else{
          Dig(
              getClosestCells().filter { it.hole == false && it.pos.isNotBase() }.map { it.pos }.notTargeted().first()
          )
        }
      } else {
        val closest = usableOre.sortedBy { it.distance(pos) }
        Dig(
            closest.getOrElse(0) { getClosestCells().filter { it.hole == false }.map { it.pos }.notTargeted().first() }
        )
      }
    }

    private fun getClosestCells() = arena.cells.flatten().sortedBy { it.pos.distance(this.pos) }

    fun List<Position>.notTargeted(): List<Position> {
      val targetedPositions = arena.myRobots.current().map { robot ->
        when (val lo = robot.lastOrder) {
          is Move -> lo.pos
          is Dig -> lo.pos
          else -> Position(0, 0)
        }
      }

      return this.filter { it !in targetedPositions }
    }
  }

  data class ItsRobot(override val id: Int, override val pos: Position, val load: Type? = null) : Robot
  data class Radar(override val id: Int, override val pos: Position) : Entity
  class Trap(override val id: Int, override val pos: Position) : Entity

  class Scores(var mine: Int, var its: Int)

  class PowerUps(var radarCooldown: Int, var trapCooldown: Int) {
    val trapAvailable = trapCooldown == 0
    val radarAvailable = radarCooldown == 0
  }

  interface Objective {
    fun nextOrder(robot: MyRobot): Action
    override fun toString(): String
  }

  class StochasticDig : Objective {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(pos.copy(x = 0))
      } else {
        val lo = internalLastOrder
        when (lo) {
          is Dig -> if (arena[lo.pos].hole == true) Dig(Position.randomNotInBase()) else lo
          is Move -> if (atBase) Dig(Position.randomNotInBase()) else lo
          else -> Dig(Position.randomNotInBase())
        }
      }
    }

    override fun toString() = "RDig"
  }

  class Spotter : Objective {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(pos.toBase())
      } else {
        when (val lo = internalLastOrder) {
          is Dig -> {
            val cell = arena[lo.pos]
            if (load == RADAR) {
              if (cell.trap || cell.radar) getBestRadarSpot() else lo
            } else Move(pos.toBase())
          }
          is Move -> if (atBase) {
            if (myPowerUps.radarAvailable) {
              RequestRadar()
            } else {
              Wait()
            }
          } else lastOrder
          is RequestRadar -> getBestRadarSpot()
          else -> Move(pos.toBase())
        }
      }
    }
    override fun toString() = "Spot"
  }

  class PassiveDigger : Objective {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(pos.toBase())
      } else {
        when (val lo = internalLastOrder) {
          is Dig -> if (arena[lo.pos].shouldNotDig()) {
            getBestDiggingSpot()
          } else lo
          is Move -> if (atBase) {
            getBestDiggingSpot()
          } else lo
          else -> getBestDiggingSpot()
        }
      }
    }
    override fun toString() = "PDig"
  }

  class AggressiveDigger : Objective {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(pos.toBase())
      } else {
        when (val lo = internalLastOrder) {
          is Dig -> if (arena[lo.pos].shouldNotDig()) {
            getBestDiggingSpot()
          } else lo
          is Move -> if (atBase) {
            if (myPowerUps.trapAvailable) {
              // TODO: check that no other robot requested a trap
              RequestTrap()
            } else {
              getBestDiggingSpot()
            }
          } else lo
          else -> getBestDiggingSpot()
        }
      }
    }

    override fun toString() = "ADig"
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
    val tick get() = myRobots[0]?.size ?: TODO("Should not happen")
    val cells = (0 until height).map { y -> (0 until width).map { x -> Cell(Position(x, y), null, null) } }

    // moving entities as map<id,history>
    val myRobots = mutableMapOf<Int, MutableList<MyRobot>>()
    val itsRobots = mutableMapOf<Int, MutableList<ItsRobot>>()

    // static entities
    val radars get() = cells.flatten().filter { it.radar }.map { it.pos }
    val traps get() = cells.flatten().filter { it.trap }.map { it.pos }

    operator fun get(x: Int, y: Int) = cells[y][x]
    operator fun get(pos: Position) = cells[pos.y][pos.x]

    fun clearCells() {
      cells.flatten().forEach {
        it.radar = false
        it.trap = false
      }
    }

    // TODO: remove non existing entities
    fun updateEntity(entity: Entity) {
      when (entity) {
        is MyRobot -> {
          debug("entity.id ${entity.id}")
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
        is Radar -> this[entity.pos].radar = true
        is Trap -> this[entity.pos].trap = true
      }
    }

    fun getKnownOres() = cells.flatten().filter { it.ore != null }.filter { it.ore!! > 0 }.map { it.pos }

    fun getLazyRobots() = myRobots.map { it.value.last() }.filter { it.objective == null }

    fun getKnownTraps() = cells.flatten().filter { it.trap }.map { it.pos }

    fun getSafeOres(): List<Position> {
      val knownOres = arena.getKnownOres()
      val knownTraps = arena.getKnownTraps()
      return knownOres.filter { it !in knownTraps }
    }
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
    myPowerUps.radarCooldown = input.nextInt() // turns left until a new radar can be requested
    myPowerUps.trapCooldown = input.nextInt() // turns left until a new trap can be requested

    arena.clearCells() // TODO: find a way to avoid clearing everything (History of cells?)
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
            MINE -> MyRobot(id, pos, item)
            ITS -> ItsRobot(id, pos)
            RADAR -> Radar(id, pos)
            TRAP -> Trap(id, pos)
            else -> TODO("How to scan type id $typeId")
          }
      )
    }
  }

  private fun assignMinion(orders: List<Objective>) {
    val robots = arena.myRobots.current()
    robots.zip(orders).forEach {
      it.first.objective = it.second
    }
  }

  private fun getObjectives(): List<Objective> {
    return when {
      arena.getSafeOres().size > 20 -> listOf(AggressiveDigger(), AggressiveDigger(), AggressiveDigger(), AggressiveDigger(), AggressiveDigger())
      arena.getSafeOres().size < 10 -> listOf(Spotter(), AggressiveDigger(), AggressiveDigger(), AggressiveDigger(), AggressiveDigger())
      else -> listOf()
    } // no changes
  }

  /**
   * @return the best places to put a radar
   */
  private fun idealRadarLocations(): List<Position> =
      listOf(
          Position(5, 3),
          Position(5, 11),
          Position(10, 7),
          Position(15, 3),
          Position(15, 11),
          Position(20, 7),
          Position(25, 3),
          Position(25, 11),
          Position(29, 7)
      )

  fun act() {
    val act = arena.myRobots.map {
      it.value.last()
    }.joinToString("\n") {
      it.getAction().printed + " " + it.objective
    }
    println(act)
  }

  operator fun LinkedList<Action>.plus(action: Action) = this.also { add(action) }
  fun debug(vararg message: String) = System.err.println("DBG " + message.joinToString())
  private fun MutableMap<Int, MutableList<MyRobot>>.current() = this.entries.map { it.value.last() }
}

