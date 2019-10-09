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

    fun getBestRadarSpot(): Dig {
      val target = idealRadarLocations().firstOrNull { pos -> pos !in arena.radars.map { it.pos } }

      return if (target == null) {
        getBestDiggingSpot()
      } else {
        if (arena.getHeatMap()[target] > 0) {
          val alternative = target.inRadius(2, arena)
              .sortedByDescending { it.x }
              .firstOrNull { arena.getHeatMap()[it] == 0 }

          if (alternative == null) {
            getBestDiggingSpot()
          } else {
            Dig(alternative)
          }
        } else {
          Dig(target)
        }
      }
    }

    fun getBestDiggingSpot(): Dig {
      val usableOre = arena.getSafeOres()
      val closestCells = getClosestCells()
      return if (usableOre.isEmpty()) {
        if (arena.step == 1) {
          Dig(pos.copy(x = 4))
        } else {
          Dig(
              pos
                  .inRadius(4, arena)
                  .sortedByDescending { it.x }
                  .notTargeted()
                  .firstOrNull() ?: closestCells
                  .filter { !it.shouldNotDig }
                  .map { it.pos }
                  .notTargeted()
                  .first()
          )
        }
      } else {
        val closest = usableOre
            .sortedBy { it.pos.distance(pos) }
            .sortedBy { it.pos.x } // favour things that are on the way back home
            .map { it.pos }
        Dig(
            closest.getOrElse(0) { closestCells.filter { it.hole == false }.map { it.pos }.notTargeted().first() }
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
      override val load: Type = UNKNOWN
  ) : Robot<ItsRobot> {
    override fun copyAndUpdate(update: ItsRobot) = this.copy(
        pos = update.pos,
        load = when {
          pos == update.pos && pos.x == 0 -> DANGER.also { debug("Suspect load from $id at $pos") }
          /**
           * Hoping for the best and assuming that it mine and didn't trick me into thinking I mined
           */
          pos == update.pos && pos.x != 0 && load == DANGER -> {
            debug("Suspect danger from $id at $pos")
            arena[pos].danger = true
            UNKNOWN
          }
          else -> load
        }
    )
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
    val name:String
    override fun toString(): String
  }

  class Spotter : Strategy {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      when {
        arena.step == 1 -> RequestRadar()
        load == ORE -> Move(toBase)
        else -> when (val lo = internalLastOrder) {
          is Dig -> {
            val cell = arena[lo.pos]
            if (load == RADAR) {
              if (arena.getHeatMap()[cell.pos] > 0 || cell.radar) getBestRadarSpot() else lo
            } else {
              val bestDig = getBestDiggingSpot()
              if (bestDig.pos.distance(pos) <= 4) bestDig else Move(toBase)
            }
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
          else -> Move(toBase)
        }
      }
    }

    override fun toString() = "S"
    override val name = "spotter"
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

    override fun toString() = "P"
    override val name = "passiveDigger"
  }

  class AggressiveDigger : Strategy {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      if (load == ORE) {
        Move(toBase)
      } else when (val lo = internalLastOrder) {
        is Dig -> if (arena[lo.pos].shouldNotDig) {
          getBestDiggingSpot()
        } else lo
        is Move -> when {
          atBase && myPowerUps.trap.available -> RequestTrap()
          else -> lo // keep rolling baby B)
        }
        else -> getBestDiggingSpot()
      }
    }

    override fun toString() = "A"
    override val name= "aggressiveDigger"
  }

  class RobotTracker<R> where R : Robot<R> {
    val robots = mutableMapOf<Int, MutableList<Robot<R>>>()

    fun update(update: R) {
      val robotHistory = robots.getOrPut(update.id) { mutableListOf() }
      val existingRobot = robotHistory.lastOrNull()
      if (existingRobot != null) {
        robotHistory.add(existingRobot.copyAndUpdate(update))
      } else {
        robotHistory.add(update)
      }
    }

    inline fun <reified T> current() where T : Robot<T> =
        this.robots.entries.filter { it.value.size == arena.step }.map { it.value.last() as T }
  }

  data class Arena(val width: Int, val height: Int) {
    val xAxis = (0 until width)
    val yAxis = (0 until height)

    var step = 0
      private set

    fun goToNextStep() = step++

    val cells = yAxis.map { y -> xAxis.map { x -> Cell(Position(x, y), null, null) } }

    // moving entities as map<id,history>
    val myRobots = RobotTracker<MyRobot>()
    val itsRobots = RobotTracker<ItsRobot>()

    // my static entities
    val radars = mutableListOf<Radar>()
    val traps = mutableListOf<Trap>()

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
        is Radar -> radars.add(entity)
        is Trap -> this[entity.pos].trap = true
      }
    }

    fun getKnownOres() = cells.flatten().filter { it.ore != null }.filter { it.ore!! > 0 }

    fun getKnownTraps() = traps

    fun getSafeOres(): List<Cell> {
      val knownOres = arena.getKnownOres()
      val knownTraps = arena.getKnownTraps().map { it.pos }
      val suspectedTraps = arena.getHeatMap()
      return knownOres.filter { it.pos !in knownTraps }.filter { suspectedTraps[it.pos] == 0 }
    }

    /**
     * @return The weather forecast for traps explosions
     */
    private val cache: MutableMap<Int, List<List<Int>>> = mutableMapOf(0 to listOf(listOf()))

    fun getHeatMap(): List<List<Int>> =
        cache.getOrPut(step) {
          yAxis.map { y ->
            xAxis.map { x ->
              val here = Position(x, y)
              here.inRadius(1, this).map { pos ->
                if (cells[pos].danger) 1 else 0
              }.sum() + if (cells[here].trap) 5 else 0
            }
          }.also {
            debug(
                "Suspected problems for" +
                    it.mapIndexed { y, row ->
                      row.mapIndexed { x, count -> if (count > 0) "$x $y ($count)" else "" }
                    }.flatten().filter { it.isNotEmpty() }.joinToString(", ")
            )
          }
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
    arena.goToNextStep()
    // Amount of ore delivered
    scores.mine = input.nextInt()
    scores.its = input.nextInt()

    arena.yAxis.map { y ->
      arena.xAxis.map { x ->
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

      // if this entity is a robot, the item it is carrying (-1 for NONE, 2 for RADAR, 3 for TRAP, 4 for ORE)
      val item = input.nextInt().toType()

      val pos = if (x < 0 || y < 0) {
        // item was destroyed
        Position(0, 0)
      } else {
        Position(x, y)
      }

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
    val robots = arena.myRobots.current<MyRobot>().sortedBy { it.pos.x }

    // if same composition -> do nothing
    val namedOrders = orders.map { it.name }.toSet()
    val namedCurrent = robots.map { it.strategy.name }.toSet()

    if(namedOrders != namedCurrent) {
      debug("Updating order")
      robots.zip(orders).forEach {
        it.first.strategy = it.second
      }
    }
  }

  private fun getObjectives(): List<Strategy> {
    val size = arena.getSafeOres().size
    debug("Known ores: $size")
    return when {
      size > 20 -> listOf(
          PassiveDigger(),
          PassiveDigger(),
          PassiveDigger(),
          PassiveDigger(),
          AggressiveDigger()
      )
      size < 10 && idealRadarLocations().isNotEmpty() -> listOf(
          Spotter(),
          PassiveDigger(),
          PassiveDigger(),
          PassiveDigger(),
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
          Position(10, 7),
          Position(5, 3),
          Position(5, 11),
          Position(15, 3),
          Position(15, 11),
          Position(20, 7),
          Position(25, 3),
          Position(10, 0),
          Position(10, 14),
          Position(25, 11),
          Position(29, 7),
          Position(20, 0),
          Position(20, 14)
      )

  fun act() {
    val act = arena.myRobots.current<MyRobot>().onEach { it.computeAction() }.joinToString("\n") {
      "${it.action.order}  ${it.strategy}: ${it.action}"
    }
    println(act)
  }


  sealed class Action {
    abstract val order: String
    override fun toString() = order

    class Wait : Action() {
      override val order = "WAIT"
      override fun toString() = "W"
    }

    class Move(val pos: Position) : Action() {
      override val order = "MOVE $pos"
      override fun toString() = "M $pos"
    }

    class Dig(val pos: Position) : Action() {
      override val order = "DIG $pos"
      override fun toString() = "D $pos"
    }

    class RequestRadar : Action() {
      init {
        myPowerUps.radar.request()
      }

      override val order = "REQUEST RADAR"
      override fun toString() = "R R"
    }

    class RequestTrap : Action() {
      init {
        myPowerUps.trap.request()
      }

      override val order = "REQUEST TRAP"
      override fun toString() = "R T"
    }
  }

  data class Position(val x: Int, val y: Int) {
    override fun toString() = "$x $y"
    fun distance(pos: Position) = abs(this.x - pos.x) + abs(this.y - pos.y)
    fun inRadius(radius: Int, arena: Arena) =
        (-radius..radius).flatMap { y ->
          (-radius..radius).map { x ->
            Position(this.x + x, this.y + y)
          }
        }.filter { it.x in arena.xAxis && it.y in arena.yAxis && it.distance(this) <= radius }
  }

  data class Cell(
      val pos: Position,
      var ore: Int? = null,
      var hole: Boolean? = null,
      var radar: Boolean = false,
      var trap: Boolean = false,
      var danger: Boolean = false
  ) {
    val shouldNotDig get() = arena.getHeatMap()[pos] > 0 || ore == 0 || (hole == true && ore == null)
  }

  enum class Type {
    MINE,
    ITS,
    RADAR,
    TRAP,
    ORE,
    NONE,
    DANGER,
    UNKNOWN
  }

  fun Int.toType() = when (this) {
    -1 -> NONE
    in Type.values().indices -> Type.values()[this]
    else -> TODO("How to scan item type id $this")
  }


  operator fun LinkedList<Action>.plus(action: Action) = this.also { add(action) }
  operator fun <E> List<List<E>>.get(pos: Position): E = this[pos.y][pos.x]
  fun debug(vararg message: Any) = System.err.println("DBG " + message.joinToString(" ") { it.toString() })
}

