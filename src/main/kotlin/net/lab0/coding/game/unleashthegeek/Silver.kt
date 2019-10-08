import java.util.*
import Silver.Action.*
import Silver.Type.*
import kotlin.math.abs

fun main(vararg args: String) {
  Silver.solve()
}

object Silver {
  private val scanner = Scanner(System.`in`)
  private lateinit var internalArena: Arena
  val arena get() = internalArena

  /**
   * Deliver more ore to hq (left side of the map) than your opponent. Use radars to find ore but beware of traps!
   */
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

  interface Entity {
    val id: Int
    val pos: Position
  }

  interface Robot<R> : Entity where R : Robot<R> {
    val load: Type?
    fun copyAndUpdate(update: R): R
  }

  data class MyRobot(
      override val id: Int,
      override val pos: Position,
      override val load: Type,
      var strategy: Strategy = PassiveDigger(),
      internal var internalLastOrder: Action = Wait()
  ) : Robot<MyRobot> {
    /**
     * Pops the previous order if it was reached
     */

    val atBase get() = pos.x == 0

    val toBase get() = pos.copy(x = 0)

    val action get() = internalLastOrder

    override fun copyAndUpdate(update: MyRobot) = this.copy(pos = update.pos, load = update.load)

    fun computeAction(): Action {
      internalLastOrder = strategy.nextOrder(this)
      return internalLastOrder
    }

    fun getBestRadarSpot() = Dig(
        idealRadarLocations().firstOrNull { pos -> pos !in arena.radars } ?: pos.copy(x = 1)
    )

    fun getBestDiggingSpot(): Dig {
      val usableOre = arena.getSafeOres()
      return if (usableOre.isEmpty()) {
        if (arena.tick == 1) {
          Dig(pos.copy(x = 8))
        } else {
          Dig(
              getClosestCells().filter { it.hole == false && it.pos.isNotBase() }.map { it.pos }.notTargeted().first()
          )
        }
      } else {
        val closest = usableOre.sortedBy { it.pos.distance(pos) }.map { it.pos }
        Dig(
            closest.getOrElse(0) { getClosestCells().filter { it.hole == false }.map { it.pos }.notTargeted().first() }
        )
      }
    }

    private fun getClosestCells() = arena.cells.flatten().sortedBy { it.pos.distance(this.pos) }

    fun List<Position>.notTargeted(): List<Position> {
      val targetedPositions = arena.myRobots.current<MyRobot>().map { robot ->
        when (val lo = robot.action) {
          is Move -> lo.pos
          is Dig -> lo.pos
          else -> Position(0, 0)
        }
      }

      return this.filter { it !in targetedPositions }
    }
  }

  data class ItsRobot(
      override val id: Int,
      override val pos: Position,
      override val load: Type? = null
  ) : Robot<ItsRobot> {
    override fun copyAndUpdate(update: ItsRobot) = this.copy(pos = update.pos)
  }

  data class Radar(override val id: Int, override val pos: Position) : Entity
  data class Trap(override val id: Int, override val pos: Position) : Entity

  data class Scores(var mine: Int, var its: Int)

  class Cooldown {
    private var internalCooldown: Int = 0

    fun updateCooldown(cooldown: Int) {
      internalCooldown = cooldown
    }

    fun request() {
      internalCooldown = 99
    }

    val available = internalCooldown == 0
  }

  class PowerUps {
    val radar = Cooldown()
    val trap = Cooldown()
  }

  interface Strategy {
    fun nextOrder(robot: MyRobot): Action
    override fun toString(): String
  }

  class Spotter : Strategy {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(toBase)
      } else when (val lo = internalLastOrder) {
        is Dig -> {
          val cell = arena[lo.pos]
          if (load == RADAR) {
            if (cell.trap || cell.radar) getBestRadarSpot() else lo
          } else Move(pos.toBase())
        }
        is Move -> if (atBase) {
          if (myPowerUps.radar.available) {
            debug("${robot.id} request radar")
            RequestRadar()
          } else {
            Wait()
          }
        } else lo
        is RequestRadar -> getBestRadarSpot()
        else -> Move(pos.toBase())
      }
    }

    override fun toString() = "Spot"
  }

  class PassiveDigger : Strategy {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(toBase)
      } else when (val lo = internalLastOrder) {
        is Dig -> if (arena[lo.pos].shouldNotDig) {
          getBestDiggingSpot()
        } else lo
        is Move -> if (atBase) {
          getBestDiggingSpot()
        } else lo
        else -> getBestDiggingSpot()
      }
    }

    override fun toString() = "PDig"
  }

  class AggressiveDigger : Strategy {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(pos.toBase())
      } else when (val lo = internalLastOrder) {
        is Dig -> if (arena[lo.pos].shouldNotDig.also { debug("Should not dig ${lo.pos} $it -> ${arena[lo.pos]} : ${arena[lo.pos].shouldNotDig}") }) {
          getBestDiggingSpot()
        } else lo
        is Move -> if (atBase) {
          if (myPowerUps.trap.available) {
            RequestTrap()
          } else {
            getBestDiggingSpot()
          }
        } else lo
        else -> getBestDiggingSpot()
      }
    }

    override fun toString() = "ADig"
  }

  class RobotTracker<R> where R : Robot<R> {
    val robots = mutableMapOf<Int, MutableList<Robot<R>>>()

    fun update(update: R) {
      debug("Update $update")
      val robotHistory = robots.getOrPut(update.id) { mutableListOf() }
      val existingRobot = robotHistory.lastOrNull()
      if (existingRobot != null) {
        robotHistory.add(existingRobot.copyAndUpdate(update))
      } else {
        robotHistory.add(update)
      }
    }

    inline fun <reified T> current() where T : Robot<T> =
        this.robots.entries.filter { it.value.size == arena.tick }.map { it.value.last() as T }
  }

  data class Arena(val width: Int, val height: Int) {
    var tick = 0
      private set

    fun tick() = tick++

    val cells = (0 until height).map { y -> (0 until width).map { x -> Cell(Position(x, y), null, null) } }

    // moving entities as map<id,history>
    val myRobots = RobotTracker<MyRobot>()
    val itsRobots = RobotTracker<ItsRobot>()

    // my static entities
    val radars get() = cells.flatten().filter { it.radar }.map { it.pos }
    val traps get() = cells.flatten().filter { it.trap }

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
        is MyRobot -> myRobots.update(entity)
        is ItsRobot -> itsRobots.update(entity)
        is Radar -> this[entity.pos].radar = true
        is Trap -> this[entity.pos].trap = true
      }
    }

    fun getKnownOres() = cells.flatten().filter { it.ore != null }.filter { it.ore!! > 0 }
    
    fun getKnownTraps() = traps

    fun getSafeOres(): List<Cell> {
      val knownOres = arena.getKnownOres()
      val knownTraps = arena.getKnownTraps()
      return knownOres.filter { it !in knownTraps }
    }
  }

  // game data
  val scores = Scores(0, 0)
  val myPowerUps = PowerUps()

  private fun initParser(input: Scanner): Arena {
    val width = input.nextInt()
    val height = input.nextInt() // size of the map
    return Arena(width, height)
  }

  private fun loopParser(input: Scanner) {
    arena.tick()
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
    myPowerUps.radar.updateCooldown(input.nextInt())  // turns left until a new radar can be requested
    myPowerUps.trap.updateCooldown(input.nextInt()) // turns left until a new trap can be requested

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

  private fun assignMinion(orders: List<Strategy>) {
    val robots = arena.myRobots.current<MyRobot>()
    robots.zip(orders).forEach {
      it.first.strategy = it.second
    }
  }

  private fun getObjectives(): List<Strategy> {
    val size = arena.getSafeOres().size
    debug("Known ores: $size")
    return when {
      size > 20 -> listOf(
          AggressiveDigger(),
          AggressiveDigger(),
          AggressiveDigger(),
          AggressiveDigger(),
          AggressiveDigger()
      )
      size < 10 -> listOf(
          Spotter(),
          AggressiveDigger(),
          AggressiveDigger(),
          AggressiveDigger(),
          AggressiveDigger()
      )
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
    val act = arena.myRobots.current<MyRobot>().onEach { it.computeAction() }.joinToString("\n") {
      "${it.action} ${it.strategy}"
    }
    println(act)
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
      init {
        myPowerUps.radar.request()
      }

      override val printed = "REQUEST RADAR"
    }

    class RequestTrap : Action() {
      init {
        myPowerUps.trap.request()
      }

      override val printed = "REQUEST TRAP"
    }
  }

  data class Position(val x: Int, val y: Int) {
    override fun toString() = "$x $y"
    fun distance(pos: Position) = abs(this.x - pos.x) + abs(this.y - pos.y)
    fun isNotBase() = x > 0
    fun toBase() = this.copy(x = 0)
  }

  data class Cell(
      val pos: Position,
      var ore: Int? = null,
      var hole: Boolean? = null,
      var radar: Boolean = false,
      var trap: Boolean = false
  ) {
    val shouldNotDig = trap || !(hole == true && ore ?: 0 > 0)
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


  operator fun LinkedList<Action>.plus(action: Action) = this.also { add(action) }
  fun debug(vararg message: String) = System.err.println("DBG " + message.joinToString())
}

