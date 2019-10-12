import java.util.*
import Silver.Action.*
import Silver.Type.*
import java.lang.IllegalArgumentException
import kotlin.math.abs

fun main(vararg args: String) {
  Silver.solve()
}

object Silver {
  private val scanner = Scanner(System.`in`)
  private val MAX_DISTANCE_PER_STEP = 4
  private val MAX_TURNS = 202 // 200 + init (step 0) + buffer (last step)
  private val HEIGHT = 15
  private val WIDTH = 30

  val xAxis = (0 until WIDTH)
  val yAxis = (0 until HEIGHT)

  // game data
  val arena = Arena()
  val holeManager = HoleManager()
  val oreManager = OreManager()
  val dangerManager = DangerManager()
  val radarManager = RadarManager(dangerManager)
  val scores = Scores(0, 0)
  val myPowerUps = PowerUps()

  var step = 0
    private set

  /**
   * Deliver more ore to hq (left side of the map) than your opponent. Use radars to find ore but beware of traps!
   */
  fun solve() {
    val input = scanner
    initParser(input)

    // game loop
    while (true) {
      loopParser(input)
      val orders = getObjectives()
      assignMinion(orders)
      act()
    }
  }

  // ENTITIES

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

    fun getBestDiggingSpot(): Action {
      val safeOres = arena.getSafeOres()
      val closestCells = getClosestCells()
      return if (safeOres.isEmpty()) {
        if (step == 1) {
          Dig(pos.copy(x = 4))
        } else {
          // TODO: in late game, could commit suicide to free an ore to others
          val unsafeOre = arena.getUnsafeOres()

          // don't sacrifice the last robot
          if (unsafeOre.isNotEmpty() && step > 150 && arena.myRobots.current<MyRobot>().size > 1) {
            val unsafeDig = unsafeOre.filter { dangerManager[it] < 5 }.minBy { it.distance(pos) }
            debug("Suicide digging to $unsafeDig")
            if (unsafeDig != null) {
              Dig(unsafeDig)
            } else {
              Wait()
            }

          } else {
            val randomDig = pos
                .inRadius(4)
                .sortedByDescending { it.x }
                .notTargeted()
                .firstOrNull() ?: closestCells
                .filter { dangerManager[pos] == 0 }
                .map { it.pos }
                .notTargeted()
                .first()
            debug("Random digging to $randomDig")
            Dig(
                randomDig
            )
          }
        }
      } else {
        val closest = safeOres
            .sortedBy { it.distance(pos) }
            .sortedBy { it.x } // favour things that are on the way back home
            .map { it }
        Action.Dig(
            closest.getOrElse(0) {
              closestCells
                  .filterNot { holeManager.lastKnownValue(it.pos.x, it.pos.y) }
                  .map { it.pos }
                  .notTargeted()
                  .first()
            }
        )
      }
    }

    private fun getClosestCells() = arena.cells.flatten().sortedBy { it.pos.distance(this.pos) }

    fun Sequence<Position>.notTargeted(): Sequence<Position> {
      val targetedPositions = arena.myRobots.current<MyRobot>().map { robot ->
        when (val lo = robot.action) {
          is Move -> lo.pos
          is Action.Dig -> lo.pos
          else -> Position(0, 0)
        }
      }

      return this.filter { it !in targetedPositions }
    }

    fun List<Position>.notTargeted() = this.asSequence().notTargeted()
  }

  data class ItsRobot(
      override val id: Int,
      override val pos: Position,
      override val load: Type = UNKNOWN
  ) : Robot<ItsRobot> {
    override fun copyAndUpdate(update: ItsRobot) = this.copy(
        pos = update.pos,
        load = when {
          pos == update.pos && pos.x == 0 -> Type.DANGER.also { debug("Suspect load from $id at $pos") }
          /**
           * Hoping for the best and assuming that it mine and didn't trick me into thinking I mined
           */
          pos == update.pos && pos.x != 0 && load == DANGER -> {
            debug("Suspect danger from $id at $pos")
            dangerManager.updateDanger(pos)
            UNKNOWN
          }
          else -> load
        }
    )
  }

  data class Radar(override val id: Int, override val pos: Position) : Entity
  data class Trap(override val id: Int, override val pos: Position) : Entity


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

  // STRATEGIES

  interface Strategy {
    fun nextOrder(robot: MyRobot): Action
    val name: String
    override fun toString(): String
  }

  class Spotter : Strategy {
    override fun nextOrder(robot: MyRobot): Action = with(robot) {
      val bestRadarSpot = radarManager.getNextRadarPosition()
      when {
        step == 1 -> RequestRadar()
        load == ORE -> Move(toBase)
        bestRadarSpot == null -> {
          this.strategy = PassiveDigger()
          return strategy.nextOrder(this)
        }
        else -> when (val lo = internalLastOrder) {
          is Dig -> {
            if (load == RADAR) {
              Dig(bestRadarSpot)
            } else {
              val directlyAccessibleOres = arena.getSafeOres().filter { it.distance(this.pos) <= 4 }
              if (directlyAccessibleOres.isEmpty()) {
                Move(toBase)
              } else {
                Dig(directlyAccessibleOres.sortedBy { it.x }.first())
              }
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
          is RequestRadar -> Dig(bestRadarSpot)
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
        is Dig -> if (shouldNotDig(lo.pos.x, lo.pos.y)) {
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
        is Dig -> if (shouldNotDig(lo.pos.x, lo.pos.y)) {
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
    override val name = "aggressiveDigger"
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
        this.robots.entries.filter { it.value.size == step }.map { it.value.last() as T }
  }

  // MANAGEMENT

  class OreManager {
    // -1 is unknown
    val history: Array<Array<IntArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { IntArray(WIDTH) { -1 } } }

    fun update(x: Int, y: Int, amount: Int) {
      history[step][y][x] = amount
    }

    // TODO: reduce the value when there was a dig in the area
    fun stepUpdate(){}
    //yAxis.forEach { y -> xAxis.forEach { x -> history[step][y][x] = history[step - 1][y][x] } }

    fun lastKnownValue(x: Int, y: Int): Int =
        history.lastOrNull { it[y][x] != -1 }?.get(y)?.get(x) ?: -1

    val current get() = history[step]
  }

  class HoleManager {
    val history: Array<Array<BooleanArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { BooleanArray(WIDTH) { false } } }

    fun update(x: Int, y: Int, amount: Boolean) {
      history[step][y][x] = amount
    }

    operator fun get(pos: Position) = history[step][pos.y][pos.x]

    fun lastKnownValue(x: Int, y: Int): Boolean = history[step][y][x]
  }

  class DangerManager {
    val history: Array<Array<IntArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { IntArray(WIDTH) { 0 } } }

    operator fun get(pos: Position) = history[step][pos.y][pos.x]

    fun updateDanger(pos: Position) {
      pos.inRadius(1).forEach {
        history[step][it.y][it.x] += 1
      }
    }

    fun updateTrap(pos: Position) {
      history[step][pos.y][pos.x] = 5
    }

    fun stepUpdate() {
      yAxis.forEach { y ->
        xAxis.forEach { x ->
          history[step][y][x] = history[step - 1][y][x]
        }
      }
    }
  }

  class RadarManager(private val dangerManager: DangerManager) {
    val history: Array<Array<BooleanArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { BooleanArray(WIDTH) { false } } }

    val current get() = history[step]

    operator fun get(pos: Position) = history[step][pos.y][pos.x]

    fun update(x: Int, y: Int) {
      history[step][y][x] = true
    }

    private val radars = listOf(
        Position(4, 3),
        Position(9, 7),
        Position(4, 11),
        Position(14, 3),
        Position(14, 11),
        Position(19, 7),
        Position(24, 3),
        Position(24, 11),
        Position(28, 7)
    )

    // TODO: avoid gaps in radar coverage
    fun getNextRadarPosition(): Position? =
        radars
            .firstOrNull { it.getAlternativePositions().none { it.alreadyHasRadar() } }
            ?.getAlternativePositions()
            ?.firstOrNull { it.canPlaceRadar() }

    fun Position.canPlaceRadar(): Boolean {
      // TODO: avoid putting radar on ore? does digging my radar remove it?
      return dangerManager[this] == 0
    }

    fun Position.alreadyHasRadar() = this@RadarManager[this]

    // TODO: LOW may need to adjust this range
    fun Position.getAlternativePositions() = this.inRadius(2)
  }

  class Arena {

    val cellsHistory: MutableList<List<List<Cell>>> = mutableListOf(
        yAxis.map { y -> xAxis.map { x -> Cell(Position(x, y)) } }
    )

    fun updateCells(cells: List<List<Cell>>) = cellsHistory.add(cells)

    val cells get() = cellsHistory.last()

    // moving entities as map<id,history>
    val myRobots = RobotTracker<MyRobot>()
    val itsRobots = RobotTracker<ItsRobot>()

    // my static entities
    val radars = mutableListOf<Radar>()

    operator fun get(x: Int, y: Int) = cells[y][x]
    operator fun get(pos: Position) = cells[pos.y][pos.x]

    // TODO: remove non existing entities
    fun updateEntity(entity: Entity) {
      when (entity) {
        is MyRobot -> myRobots.update(entity)
        is ItsRobot -> itsRobots.update(entity)
        is Radar -> radars.add(entity)
        else -> TODO("Nope")
      }
    }

    fun getKnownOres(): List<Position> {
      val currentOre = oreManager.current
      return yAxis.flatMap { y ->
        xAxis.mapNotNull { x ->
          if (currentOre[y][x] > 0) {
            Position(x, y)
          } else null
        }
      }
    }

    fun getSafeOres() = arena.getKnownOres().filter { dangerManager[it] == 0 }

    fun getUnsafeOres() = arena.getKnownOres().sortedBy { dangerManager[it] }
  }

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

  @Suppress("UNUSED_VARIABLE")
  private fun initParser(input: Scanner) {
    val width = input.nextInt()
    val height = input.nextInt() // size of the map
  }

  private fun loopParser(input: Scanner) {
    step++
    dangerManager.stepUpdate()
    oreManager.stepUpdate()

    // Amount of ore delivered
    scores.mine = input.nextInt()
    scores.its = input.nextInt()

    yAxis.map { y ->
      xAxis.map { x ->
        val ore = input.next() // amount of ore or "?" if unknown
        val hole = input.nextInt() // 1 if cell has a hole

        when (ore) {
          "?" -> Unit
          else -> oreManager.update(
              x, y, ore.toInt()
          )
        }

        holeManager.update(x, y, hole == 1)
      }
    }

    val entityCount = input.nextInt() // number of entities visible to you
    myPowerUps.radar.updateCooldown(input.nextInt())  // turns left until a new radar can be requested
    myPowerUps.trap.updateCooldown(input.nextInt()) // turns left until a new trap can be requested

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

      when (typeId.toType()) {
        MINE -> arena.updateEntity(MyRobot(id, pos, item))
        ITS -> arena.updateEntity(ItsRobot(id, pos))
        RADAR -> radarManager.update(x, y)
        TRAP -> dangerManager.updateTrap(pos)
        else -> TODO("How to scan type id $typeId")
      }
    }
  }

  private fun assignMinion(orders: List<Strategy>) {
    val robots = arena.myRobots.current<MyRobot>().sortedBy { it.pos.x }

    // if same composition -> do nothing
    val namedOrders = orders.map { it.name }.toSet()
    val namedCurrent = robots.map { it.strategy.name }.toSet()

    if (namedOrders != namedCurrent) {
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
      size < 10 && radarManager.getNextRadarPosition() != null -> listOf(
          Spotter(),
          PassiveDigger(),
          PassiveDigger(),
          PassiveDigger(),
          AggressiveDigger()
      )
      else -> listOf()
    } // no changes
  }


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
    fun stepDistance(pos: Position) = (this.distance(pos) + MAX_DISTANCE_PER_STEP - 1) / MAX_DISTANCE_PER_STEP

    /**
     * Yields positions around this point, starting from this point's position
     */
    fun around(xBounds: IntRange = xAxis, yBounds: IntRange = yAxis): Sequence<Position> {
      return sequence {
        (0..Int.MAX_VALUE).asSequence().forEach { radius ->
          this@Position.atRadius(radius, xBounds, yBounds).forEach {
            yield(it)
          }
        }
      }
    }

    fun inRadius(radius: Int, xBounds: IntRange = xAxis, yBounds: IntRange = yAxis) =
        sequence {
          (0..radius).forEach { radius ->
            atRadius(radius, xBounds, yBounds).forEach {
              yield(it)
            }
          }
        }

    fun atRadius(i: Int, xRange: IntRange, yRange: IntRange): Sequence<Position> =
        when {
          i == 0 -> sequenceOf(this)
          i > 0 -> sequence {
            // return 4 edges in trigonometric order

            val rangeCheck = { p: Pair<Int, Int> ->
              (this@Position.x + p.first) in xRange && (this@Position.y + p.second) in yRange
            }

            // top right
            (i downTo 1).toList()
                .zip((0..i - 1).toList())
                .filter(rangeCheck)
                .forEach { (x, y) -> yield(Position(this@Position.x + x, this@Position.y + y)) }

            // top left
            (0 downTo -i + 1).toList()
                .zip((i downTo 1).toList())
                .filter(rangeCheck)
                .forEach { (x, y) -> yield(Position(this@Position.x + x, this@Position.y + y)) }

            // bottom left
            (-i..-1).toList()
                .zip((0 downTo -i + 1).toList())
                .filter(rangeCheck)
                .forEach { (x, y) -> yield(Position(this@Position.x + x, this@Position.y + y)) }

            // bottom right
            (0..i - 1).toList().zip((-i..-1).toList())
                .filter(rangeCheck)
                .forEach { (x, y) -> yield(Position(this@Position.x + x, this@Position.y + y)) }
          }
          else -> throw IllegalArgumentException("Expected i to be positive, was $i")
        }
  }

  data class Cell(
      val pos: Position
  )

  fun shouldNotDig(x: Int, y: Int): Boolean {
    val oreValue = oreManager.lastKnownValue(x, y)

    return oreValue == 0 ||
        (holeManager.history[step][y][x] && oreValue == -1) ||
        dangerManager.history[step][y][x] > 0
  }

  // META

  interface Initializable<T> where T : Initializable<T> {
    fun initialize()
  }

  inline fun <reified T> Initializable<T>.init(): T where T : Initializable<T> = this.apply { initialize() } as T


  fun Int.toType() = when (this) {
    -1 -> NONE
    in Type.values().indices -> Type.values()[this]
    else -> throw IllegalStateException("How to scan item type id $this")
  }

  operator fun LinkedList<Action>.plus(action: Action) = this.also { add(action) }
  operator fun <E> List<List<E>>.get(pos: Position): E = this[pos.y][pos.x]
  fun debug(vararg message: Any) = System.err.println("DBG " + message.joinToString(" ") { it.toString() })
}


// HIGH PRIO

// TODO: coordinate mining operations

// TODO: don't put a mine and collect on the same tile :/ silly me ...
// TODO: when found ore, add to ore potential heat map

// TODO: when dug and found o ore -> try again at the same place

// TODO: when radar gets destroyed, remember the ore's position and decrease their value based on surrounding digging

// TODO: prefer put mines on high yield ores

// TODO: when getting back to the base to get a new radar, try to get closer to the next place where it has to be put

// LOW OCCURENCE

// TODO: when opponent mines my radar and the same cell contains ore, then I still think tha there is ore in there -> find if it's possible to deduce which ore was mined (radar disapeared) and mark as empty

// LATE GAME
// TODO: when doing suicide missions, keep other collectors outside of trap and trap chain explosion area
// TODO: when mining, if it's possible to mine at a position that will indicate a danger on more ore fields (to the opponent), prefer that over the shortest distance mining, assuming it's the same number of steps
// TODO: when there is no more ore, try to kill the opponent's robots
// TODO: if dug once on ore and didn't come back: smells like a trap
// TODO: when doing random digging, stay ~2 steps from the trap
