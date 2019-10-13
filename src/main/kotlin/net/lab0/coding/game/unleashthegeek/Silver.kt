import java.util.*
import Silver.Action.*
import Silver.Type.*
import Silver.Objective.*
import java.lang.IllegalArgumentException
import kotlin.math.abs
import kotlin.system.measureNanoTime

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

  val xAxisNoBase = (1 until WIDTH)

  // game data
  val arena = Arena()
  val clock = Clock(0)
  val holeManager = HoleManager(clock)
  val trapManager = TrapManager(clock)
  val oreManager = OreManager(clock, trapManager)
  val radarManager = RadarManager(clock, trapManager)
  val scores = Scores(0, 0)

  /**
   * Deliver more ore to hq (left side of the map) than your opponent. Use radars to find ore but beware of traps!
   */
  fun solve() {
    val input = scanner
    initParser(input)

    // game loop
    while (true) {
      time("Parse") { loopParser(input) }
      time("Assign actions") { assignActions() }
      time("Act") { act() }
    }
  }

  // ENTITIES

  data class Clock(var step: Int = 0) {
    fun tick() = step++
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
      var strategy: Strategy = EagerDigger,
      var internalLastOrder: Action = Wait()
  ) : Robot<MyRobot> {
    /**
     * Pops the previous order if it was reached
     */

    val atBase
      get() = pos.x == 0

    val toBase
      get() = pos.copy(x = 0)

    val action
      get() = internalLastOrder

    override fun copyAndUpdate(update: MyRobot) = this.copy(pos = update.pos, load = update.load)

    fun computeAction(): Action {
      internalLastOrder = strategy.nextOrder(this)
      return internalLastOrder
    }

    private fun getClosestCells() = arena.cells.flatten().sortedBy { it.pos.stepDistance(this.pos) }

    fun Sequence<Position>.notTargeted(): Sequence<Position> {
      val targetedPositions = arena.myRobots.current<MyRobot>().map { robot ->
        when (val lo = robot.action) {
          is Move -> lo.pos
          is Dig -> lo.pos
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
          pos == update.pos && pos.x == 0 -> DANGER.also { debug("Suspect load from $id at $pos") }
          /**
           * Hoping for the best and assuming that it mine and didn't trick me into thinking I mined
           */
          pos == update.pos && pos.x != 0 && load == DANGER -> {
            debug("Suspect danger from $id at $pos")
            trapManager.updateDanger(pos)
            UNKNOWN
          }
          else -> load
        }
    )
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

  // STRATEGIES

  enum class Objective {
    EMERGENCY_SCOUT,
    SCOUT,
    ARBITRARY_COLLECT,
    COLLECT,
    STAY_AWAY
  }

  interface Strategy {
    fun nextOrder(robot: MyRobot): Action
    fun selection(index: Int, robots: List<MyRobot>): MyRobot?
    val name: String
    override fun toString(): String
  }

  object Scout : Strategy {
    override fun nextOrder(robot: MyRobot): Action = with(robot) {
      val nextRadarPosition = radarManager.getNextRadarPosition()
      when (load) {
        RADAR -> {
          nextRadarPosition?.let { Dig(it) }
              ?: internalLastOrder.also { debug("No radar spot available: continue to $it") }
        }
        ORE -> if (nextRadarPosition != null) Move(toBase.copy(y = nextRadarPosition.y)) else Move(toBase)
        else -> {
          // look for something on the way back
          // TODO: this should actually be computed in steps cost instead of rough distance
          val oreNearby = oreManager.getSafeOres().filter { it.x < pos.x }.sortedByDescending { it.y }
              .firstOrNull { it.stepDistance(robot.pos) < 1 }
          when {
            atBase && !radarManager.radarAvailable && nextRadarPosition != null -> Move(nextRadarPosition.copy(x = 0))
            oreNearby != null -> Dig(oreNearby)
            else -> RequestRadar()
          }
        }
      }
    }

    // TODO: allow for selection by index: so it's possible to select more than 1 robot for a role
    override fun selection(index: Int, robots: List<MyRobot>): MyRobot? {
      return when (index) {
        0 -> {
          robots.firstOrNull { it.strategy == Scout } // re-select the scout
              ?: robots.firstOrNull { it.load == RADAR }
              // closest to next radar location
              ?: robots.filter { (it.atBase) }
                  .minBy { it.pos.stepDistance(radarManager.getNextRadarPosition() ?: Position(0, 0)) }
              ?: robots.firstOrNull { it.atBase } // otherwise select a robot at base
              ?: robots.minBy { it.pos.x } // closest to base
        }
        // otherwise select one at base
        else -> robots.firstOrNull { radarManager.radarAvailable && it.atBase }
      }
    }

    override fun toString() = "S"
    override val name = "spotter"
  }

  object EagerDigger : Strategy {
    override fun nextOrder(robot: MyRobot) = with(robot) {
      trapManager.kaboomPotential(robot)?.let { return Dig(it) }
      if (load == ORE) {
        Move(toBase)
      } else {
        if (atBase && trapManager.shouldTakeTrap()) {
          RequestTrap()
        } else when (val lo = internalLastOrder) {
          is Dig -> when {
            trapManager[lo.pos] > 0 -> oreManager.getNextTarget(robot)
            oreManager[lo.pos] == 0 -> oreManager.getNextTarget(robot)
            oreManager[lo.pos] == -1 && holeManager[lo.pos] -> oreManager.getNextTarget(robot)
            else -> lo
          }
          else -> oreManager.getNextTarget(robot)
        }
      }
    }

    override fun selection(index: Int, robots: List<MyRobot>): MyRobot? {
      val ores = oreManager.getSafeOres()
      var minDistance = Int.MAX_VALUE
      var bestRobot = robots.first()
      robots.forEach { robot ->
        ores.forEach { ore ->
          val thisDistance = robot.pos.stepDistance(ore)
          if (thisDistance < minDistance) {
            minDistance = thisDistance
            bestRobot = robot
          }
        }
      }
      return bestRobot
    }

    override fun toString() = "E"
    override val name = "eagerDigger"
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
        this.robots.entries.filter { it.value.size == clock.step }.map { it.value.last() as T }
  }

  // MANAGEMENT

  class OreManager(private val clock: Clock, private val trapManager: TrapManager) {
    // -1 is unknown
    val history: Array<Array<IntArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { IntArray(WIDTH) { -1 } } }

    val digTargets: Array<MutableList<Position>> =
        Array(MAX_TURNS) { mutableListOf<Position>() }

    fun update(x: Int, y: Int, amount: Int) {
      history[clock.step][y][x] = amount
    }

    // TODO: reduce the value when there was a dig in the area
    fun stepUpdate() {}
    //yAxis.forEach { y -> xAxis.forEach { x -> history[clock.step][y][x] = history[clock.step - 1][y][x] } }

    fun lastKnownValue(x: Int, y: Int): Int =
        history.lastOrNull { it[y][x] != -1 }?.get(y)?.get(x) ?: -1

    val current
      get() = history[clock.step]

    operator fun get(pos: Position) = current[pos.y][pos.x]

    // OPTIM: could be cached
    fun getKnownOres(): Sequence<Position> = sequence {
      val currentOre = current
      yAxis.flatMap { y ->
        xAxis.flatMap { x ->
          (1..currentOre[y][x]).map {
            yield(Position(x, y))
          }
        }
      }
    }

    fun countOres() = current.sumBy { it.sumBy { if (it <= 0) 0 else it } }

    fun getSafeOres() = getKnownOres().filter { trapManager[it] == 0 }

    fun getUnsafeOres() = getKnownOres().sortedBy { trapManager[it] }

    // TODO: in late game, could commit suicide to free an ore to others
//          val unsafeOre = oreManager.getUnsafeOres()
    // don't sacrifice the last robot
//          if (unsafeOre.isNotEmpty() && clock.step > 150 && arena.myRobots.current<MyRobot>().size > 1) {
//            val unsafeDig = unsafeOre.filter { trapManager[it] < 5 }.minBy { it.distance(pos) }
//            debug("Suicide digging to $unsafeDig")
//            if (unsafeDig != null) {
//              Dig(unsafeDig)
//            } else {
//              Wait()
//            }

    fun shouldNotDig(x: Int, y: Int): Boolean {
      val oreValue = oreManager.lastKnownValue(x, y)

      return oreValue == 0 ||
          (holeManager.history[clock.step][y][x] && oreValue <= 0) ||
          trapManager.history[clock.step][y][x] > 0
      // TODO: should not dig if other robot are aiming at it
    }

    fun getNextTarget(robot: MyRobot): Action {
      if (clock.step == 1) {
        val order = arena.myRobots.current<MyRobot>().sortedBy { it.pos.y }.indexOfFirst { it.id == robot.id }
        return if (order % 2 == 0) Move(robot.pos.copy(x = 3)) else Move(robot.pos.copy(x = 7))
      }

      val currentDigTargets = arena.myRobots.current<MyRobot>()
          .mapNotNull { it.action as? Dig }
          .map { it.pos }
          .toMutableList()

      val directOre = getSafeOres()
          .filterNot { currentDigTargets.remove(it) }
          // sorted by round trip (1 step)
          .sortedBy { it.stepDistance(robot.pos) + it.stepDistance(it.copy(x = 0)) + 1 /*dig*/ }
          .firstOrNull()

      // aim for radar when it will be planted
      val nextRadarLocation = if (clock.step > 10) {
        arena.myRobots.current<MyRobot>()
            .filter { it.strategy == Scout }
            .mapNotNull { (it.action as? Dig)?.pos }
            .firstOrNull()
            ?: radarManager.getNextRadarPosition()
      } else null

      val searchingArea =
          if (nextRadarLocation != null && robot.pos in nextRadarLocation.inRadius(4)) robot.pos else nextRadarLocation
              ?: robot.pos

      // if nothing around current position, look for large mineable space
      val bestTarget =
          directOre
              ?: searchingArea.inRadius(1)
                  .filter { trapManager[it] == 0 && !holeManager[it] && it !in currentDigTargets }
                  .firstOrNull()

              ?: searchingArea
                  .inRadius(1.steps)
                  .toList()
                  .shuffled()
                  .filter { holeManager.holeDensityAt(it) < 50 }
                  .firstOrNull { holeManager.uncoveredPositionsAround(it) > 4 }

              ?: searchingArea
                  .inRadius(2.steps)
                  .toList()
                  .shuffled()
                  .firstOrNull { holeManager.uncoveredPositionsAround(it) > 4 }

      return if (bestTarget != null) {
        digTargets[clock.step].add(bestTarget)
        Dig(bestTarget)
      } else {
        // last resort option, go to the right
        Move(robot.pos.copy(x = robot.pos.x + 8))
      }
    }
  }

  class HoleManager(private val clock: Clock) {
    val history: Array<Array<BooleanArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { BooleanArray(WIDTH) { false } } }

    val densityHistory: Array<Array<IntArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { IntArray(WIDTH) { 0 } } }

    private val computedDensity = Array(MAX_TURNS) { false }

    fun update(x: Int, y: Int, amount: Boolean) {
      history[clock.step][y][x] = amount
    }

    operator fun get(pos: Position) = history[clock.step][pos.y][pos.x]

    fun lastKnownValue(x: Int, y: Int): Boolean = history[clock.step][y][x]

    fun uncoveredPositionsAround(pos: Position) = pos.inRadius(1).sumBy { if (this@HoleManager[it]) 0 else 1 }

    fun holeDensityAt(it: Position, radius: Int = 2): Int {
      if (!computedDensity[clock.step]) {
        val current = densityHistory[clock.step]
        yAxis.forEach { y ->
          xAxisNoBase.forEach { x ->
            val area = Position(x, y).inRadius(radius).toList()
            current[y][x] = area.count { this@HoleManager[it] } * 100 / area.count()
          }
        }
        computedDensity[clock.step] = true
      }
      return densityHistory[clock.step][it.y][it.x]
    }
  }

  class TrapManager(private val clock: Clock) {
    val history: Array<Array<IntArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { IntArray(WIDTH) { 0 } } }

    val current
      get() = history[clock.step]

    operator fun get(pos: Position) = history[clock.step][pos.y][pos.x]

    private var internalCooldown: Int = 0

    fun updateCooldown(cooldown: Int) {
      internalCooldown = cooldown
    }

    fun request() {
      internalCooldown = 99
    }

    val available = internalCooldown == 0

    fun updateDanger(pos: Position) {
      pos.inRadius(1).forEach {
        history[clock.step][it.y][it.x] += 1
      }
    }

    fun updateTrap(pos: Position) {
      history[clock.step][pos.y][pos.x] = 5
    }

    fun stepUpdate() {
      yAxis.forEach { y ->
        xAxis.forEach { x ->
          history[clock.step][y][x] = history[clock.step - 1][y][x]
        }
      }
    }

    var lastTrapTime = 0
    fun shouldTakeTrap(): Boolean {
      val should = radarManager.radarAvailable && (clock.step - lastTrapTime) > 15
      if (should) lastTrapTime = clock.step
      return should
    }

    // TODO check for chain reactions
    fun kaboomPotential(robot: MyRobot): Position? {
      // hardcore kamikaze
      return robot.pos.inRadius(1).filter { this@TrapManager[it] > 4 }.filter { trap ->
        val myRobots = arena.current<MyRobot>().count { it.pos == trap }
        val itsRobots = arena.current<ItsRobot>().count { it.pos == trap }
        itsRobots - myRobots > 0
      }.firstOrNull()    // TODO check for chain reactions

      // TODO: suicidal tendencies: if has potential to trigger explosion favourably AND there is ore at that place
    }
  }

  class RadarManager(private val clock: Clock, private val trapManager: TrapManager) {
    val history: Array<Array<BooleanArray>> =
        Array(MAX_TURNS) { Array(HEIGHT) { BooleanArray(WIDTH) { false } } }

    val current
      get() = history[clock.step]

    operator fun get(pos: Position) = history[clock.step][pos.y][pos.x]

    fun update(x: Int, y: Int) {
      history[clock.step][y][x] = true
    }

    private var internalCooldown: Int = 0

    fun updateCooldown(cooldown: Int) {
      internalCooldown = cooldown
    }

    fun request() {
      internalCooldown = 99
    }

    val radarAvailable
      get() = internalCooldown == 0

    // TODO side radar scanning when there is a lot of ore. Shows greater mining potential
    private val radars = listOf(
        Position(6, 3),
        Position(6, 11),
        Position(11, 7),
        Position(16, 3),
        Position(16, 11),
        Position(21, 7),
        Position(26, 3),
        Position(26, 11),
        Position(29, 7)
    )

    // TODO: avoid gaps in radar coverage
    fun getNextRadarPosition(): Position? =
        radars
            .firstOrNull { it.getAlternativePositions().none { it.alreadyHasRadar() } }
            ?.getAlternativePositions()
            ?.firstOrNull { it.canPlaceRadar() }

    fun canAddRadar() = getNextRadarPosition() != null

    fun Position.canPlaceRadar(): Boolean {
      // TODO: avoid putting radar on ore? does digging my radar remove it?
      return trapManager[this] == 0
    }

    fun Position.alreadyHasRadar() = this@RadarManager[this]

    // TODO: LOW may need to adjust this range
    fun Position.getAlternativePositions() = this.inRadius(2)

    var lastFakeTrapTime = 0
    fun shouldTakeFakeTrap(): Boolean {
      val should = radarAvailable && (clock.step - lastFakeTrapTime) > 15
      if (should) lastFakeTrapTime = clock.step
      return should
    }
  }

  class Arena {

    val cellsHistory: MutableList<List<List<Cell>>> = mutableListOf(
        yAxis.map { y -> xAxis.map { x -> Cell(Position(x, y)) } }
    )

    val cells
      get() = cellsHistory.last()

    // moving entities as map<id,history>
    val myRobots = RobotTracker<MyRobot>()
    val itsRobots = RobotTracker<ItsRobot>()


    inline fun <reified T> current() where T : Robot<T> =
        when (T::class) {
          MyRobot::class -> myRobots.current<MyRobot>()
          ItsRobot::class -> itsRobots.current<ItsRobot>()
          else -> throw IllegalStateException("Wrong robot type")
        }

    operator fun get(x: Int, y: Int) = cells[y][x]
    operator fun get(pos: Position) = cells[pos.y][pos.x]

    // TODO: remove non existing entities
    fun updateEntity(entity: Entity) {
      when (entity) {
        is MyRobot -> myRobots.update(entity)
        is ItsRobot -> itsRobots.update(entity)
        else -> TODO("Nope")
      }
    }
  }

  data class Scores(var mine: Int, var its: Int)

  @Suppress("UNUSED_VARIABLE")
  private fun initParser(input: Scanner) {
    val width = input.nextInt()
    val height = input.nextInt() // size of the map
  }

  private fun loopParser(input: Scanner) {
    clock.tick()
    trapManager.stepUpdate()
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
    radarManager.updateCooldown(input.nextInt())  // turns left until a new radar can be requested
    trapManager.updateCooldown(input.nextInt()) // turns left until a new trap can be requested

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
        TRAP -> trapManager.updateTrap(pos)
        else -> TODO("How to scan type id $typeId")
      }
    }
  }

  private fun assignActions() {
    val robots = arena.myRobots.current<MyRobot>().toMutableList()

    if (robots.size == 1) {
      robots.first().internalLastOrder = Move(Position(0, 0))
    }

    // scout
    val knownSafeOres = oreManager.getSafeOres().count()
    debug("Known safe ores: $knownSafeOres")
    when {
      radarManager.canAddRadar() && knownSafeOres < 10 -> {
        val robot = Scout.selection(0, robots)
        if (robot != null) {
          robot.internalLastOrder = Scout.nextOrder(robot)
          robots.remove(robot)
        }
      }
      radarManager.canAddRadar() && knownSafeOres < 30 -> {
        val robot = Scout.selection(1, robots)
        if (robot != null) {
          robot.internalLastOrder = Scout.nextOrder(robot)
          robots.remove(robot)
        }
      }
    }

    // dig
    val safeOres = oreManager.getSafeOres().toList()
    robots.permutations().minBy { it.zip(safeOres).sumBy { it.first.pos.stepDistance(it.second) } }!!.forEach {
      it.internalLastOrder = EagerDigger.nextOrder(it)
    }
  }

  fun act() {
    val act = arena.myRobots.current<MyRobot>().joinToString("\n") {
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
        radarManager.request()
      }

      override val order = "REQUEST RADAR"
      override fun toString() = "R R"
    }

    class RequestTrap : Action() {
      init {
        trapManager.request()
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

    fun inRadius(radius: Int, xBounds: IntRange = xAxisNoBase, yBounds: IntRange = yAxis) =
        sequence {
          (0..radius).forEach { radius ->
            atRadius(radius, xBounds, yBounds).forEach {
              yield(it)
            }
          }
        }

    fun atRadius(i: Int, xRange: IntRange, yRange: IntRange): Sequence<Position> =
        when {
          i == 0 -> if (this.x in xRange && this.y in yRange) sequenceOf(this) else sequenceOf()
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

  // META

  fun Int.toType() = when (this) {
    -1 -> NONE
    in Type.values().indices -> Type.values()[this]
    else -> throw IllegalStateException("How to scan item type id $this")
  }

  operator fun LinkedList<Action>.plus(action: Action) = this.also { add(action) }
  operator fun <E> List<List<E>>.get(pos: Position): E = this[pos.y][pos.x]
  fun debug(vararg message: Any) = System.err.println("DBG " + message.joinToString(" ") { it.toString() })

  fun MutableList<MyRobot>.assign(
      selector: (List<MyRobot>) -> MyRobot?,
      change: (MyRobot) -> Unit,
      notAvailable: () -> Unit = { debug("Didn't find any robot") }
  ) {
    val robot = selector(this)
    if (robot != null) {
      change(robot)
      robot.computeAction()
      this.remove(robot)
    } else {
      notAvailable()
    }
  }

  private var timingInfo = true

  fun <R> Any.timeNullable(timerName: String, action: () -> R): R? where R : Any? {
    var t: R? = null
    val duration = measureNanoTime { t = action() }
    if (timingInfo) debug("$timerName took ${duration / 1_000_000} ms")
    return t
  }

  fun <R> Any.time(timerName: String, action: () -> R): R where R : Any {
    var t: R? = null
    val duration = measureNanoTime { t = action() }
    if (timingInfo) debug("$timerName took ${duration / 1_000_000} ms")
    return t!!
  }

  val Int.steps
    get() = this * MAX_DISTANCE_PER_STEP

  val Int.asSteps
    get() = this / MAX_DISTANCE_PER_STEP

  /**
   * Generates all the permutations for the given list
   *
   * From `https://en.wikipedia.org/wiki/Heap%27s_algorithm`
   */
  fun <T> List<T>.permutations(): Sequence<List<T>> = sequence {
    val copy = this@permutations.toMutableList()
    val fakeStack = IntArray(copy.size) { 0 }

    /**
     * Swaps a and b and returns the list itself
     */
    fun <E> MutableList<E>.swap(a: Int, b: Int): MutableList<E> =
        this.also { this[a] = this[b].also { this[b] = this[a] } }

    yield(copy)

    var stackPointer = 0
    while (stackPointer < copy.size) {
      if (fakeStack[stackPointer] < stackPointer) {
        yield(
            if (stackPointer % 2 == 0) copy.swap(0, stackPointer)
            else copy.swap(fakeStack[stackPointer], stackPointer)
        )
        fakeStack[stackPointer] += 1
        stackPointer = 0
      } else {
        fakeStack[stackPointer] = 0
        stackPointer++
      }
    }
  }
}


// HIGH PRIO

// TODO: coordinate mining operations

// TODO: scout areas close to position
// TODO: when getting back to the base to get a new radar, try to get closer to the next place where it has to be put


// TODO: don't put a mine and collect on the same tile :/ silly me ...
// TODO: when found ore, add to ore potential heat map

// TODO: when dug and found o ore -> try again at the same place

// TODO: when radar gets destroyed, remember the ore's position and decrease their value based on surrounding digging


// LOW OCCURENCE

// TODO: prefer put mines on high yield ores
// TODO: when opponent mines my radar and the same cell contains ore, then I still think tha there is ore in there -> find if it's possible to deduce which ore was mined (radar disapeared) and mark as empty

// LATE GAME
// TODO: when doing suicide missions, keep other collectors outside of trap and trap chain explosion area
// TODO: when mining, if it's possible to mine at a position that will indicate a danger on more ore fields (to the opponent), prefer that over the shortest distance mining, assuming it's the same number of steps
// TODO: when there is no more ore, try to kill the opponent's robots
// TODO: if dug once on ore and didn't come back: smells like a trap
// TODO: when doing random digging, stay ~2 steps from the trap
// TODO: when

// 88 71 100 94 93
// 85 72  92 87 90