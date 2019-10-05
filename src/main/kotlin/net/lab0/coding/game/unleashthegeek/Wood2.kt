import java.util.*

fun main(vararg args: String) {
  Wood2.solve()
}

object Wood2 {
  private val scanner = Scanner(System.`in`)
  /**
   * Deliver more ore to hq (left side of the map) than your opponent. Use radars to find ore but beware of traps!
   **/
  fun solve() {
    val input = scanner
    val (width, height) = initParser(input)
    terrain = Terrain(width, height)

    // game loop
    while (true) {
      loopParser(input)
      assignMinion(getOrders())
      act()
    }
  }

  // structures

  class Position(val x: Int, val y: Int) {
    override fun toString() = "$x $y"
    fun offset(x: Int, y: Int): Position {
      return Position(this.x + x, this.y + y)
    }
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
      var orders: LinkedList<Action> = LinkedList()
  ) :
      Robot

  data class ItsRobot(override val id: Int, override val pos: Position, val load: Type? = null) : Robot
  data class Radar(override val id: Int, override val pos: Position) : Entity
  class Trap(override val id: Int, override val pos: Position) : Entity

  class Scores(var mine: Int, var its: Int)

  class PowerUps(var radarCoolDown: Int, var trapCooldown: Int)

  abstract class Action {
    abstract val printed: String
    override fun toString() = printed
  }

  class Wait : Action() {
    override val printed = "WAIT"
  }

  class Move(private val pos: Position) : Action() {
    override val printed = "MOVE $pos"
  }

  class Dig(private val pos: Position) : Action() {
    override val printed = "DIG $pos"
  }

  class RequestRadar : Action() {
    override val printed = "REQUEST RADAR"
  }

  class RequestTrap : Action() {
    override val printed = "REQUEST TRAP"
  }

  data class Terrain(val width: Int, val height: Int) {
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
      debug("Update $entity")
      when (entity) {
        is MyRobot -> {
          entity as MyRobot
          val myRobots = myRobots.getOrPut(entity.id) { mutableListOf() }
          val existingRobot = myRobots.lastOrNull()
          if (existingRobot != null) {
            debug("Update $entity")
            myRobots.add(existingRobot.copy(pos = entity.pos, load = entity.load, orders = existingRobot.orders))
          } else {
            debug("Add new $entity")
            myRobots.add(entity)
          }

        }
        is ItsRobot -> {
          entity as ItsRobot
          val existingRobots = itsRobots.getOrPut(entity.id) { mutableListOf() }
          val existingRobot = existingRobots.lastOrNull()
          // make a copy to preserve the existing attributes
          if (existingRobot != null) {
            debug("Update $entity")
            existingRobot.copy(pos = entity.pos)
          } else {
            debug("Add new $entity")
            existingRobots.add(entity)
          }
        }
        else -> radarsAndTraps.getOrPut(entity.id) { mutableSetOf() }.add(entity)
      }
    }

    fun getKnownOres() = cells.flatten().filter { it.ore != null }.filter { it.ore!! > 0 }

    fun getLazyRobots() = myRobots
        .map { it.value.last() }
        .filter { it.orders.isEmpty() || it.orders.all { it is Wait } }
  }

  // game data
  lateinit var terrain: Terrain
  val scores = Scores(0, 0)
  val myPowerUps = PowerUps(0, 0)

  private fun initParser(input: Scanner): Pair<Int, Int> {
    val width = input.nextInt()
    val height = input.nextInt() // size of the map
    return Pair(width, height)
  }

  private fun loopParser(input: Scanner) {
    // Amount of ore delivered
    scores.mine = input.nextInt()
    scores.its = input.nextInt()

    (0 until terrain.height).map { y ->
      (0 until terrain.width).map { x ->
        val ore = input.next() // amount of ore or "?" if unknown
        val hole = input.nextInt() // 1 if cell has a hole

        val cell = terrain[x, y]
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

    debug("Receiving $entityCount entities")

    (0 until entityCount).forEach { _ ->
      val id = input.nextInt() // unique id of the entity
      val typeId = input.nextInt() // 0 for your robot, 1 for other robot, 2 for radar, 3 for trap
      val x = input.nextInt()
      val y = input.nextInt() // position of the entity
      val pos = Position(x, y)

      // if this entity is a robot, the item it is carrying (-1 for NONE, 2 for RADAR, 3 for TRAP, 4 for ORE)
      val item = input.nextInt().toType()

      terrain.updateEntity(
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

  private fun assignMinion(orders: MutableList<LinkedList<Action>>) {
    val lazyRobots = terrain.getLazyRobots()
    debug("Got ${orders.size} orders and ${lazyRobots.size} available robots")
    lazyRobots.zip(orders).forEach {
      it.first.orders = it.second
    }
  }

  private fun getOrders(): MutableList<LinkedList<Action>> {
    val actions = mutableListOf<LinkedList<Action>>()

    // find something to mine

    val knowOres = terrain.getKnownOres()
    when (knowOres.size) {
      0 -> {
        debug("No ore found, dowsing and random pick")
        val prospectingTargets = dowser()
        val target = prospectingTargets.first()
        // TODO test if radar is available
        actions.add(
            actionList() + RequestRadar() + Dig(target)
        )
        val targets = listOf(
            target.offset(-2, -2),
            target.offset(2, -2),
            target.offset(-2, 2),
            target.offset(2, 2)
        )
        actions.addAll(
            targets.map {
              actionList() + Dig(it)
            }
        )
      }
      in 1..4 -> {
        debug("${knowOres.size} ore found, dowsing ahead")
        val prospectingTargets = dowser()
        val target = prospectingTargets.first()
        // TODO test if radar is available
        actions.add(
            actionList() + RequestRadar() + Move(target) + Dig(target)
        )
      }
      else ->
        // get the ore
        actions.addAll(
            knowOres.take(5).map {
              actionList() + Dig(it.pos)
            }
        )
    }

    return actions
  }

  /**
   * @return the best places to put a radar
   */
  private fun dowser(): List<Position> =
      // wood: constants
      (4 until terrain.width step 4).flatMap { x ->
        (4 until terrain.height step 4).map { y ->
          Position(x, y)
        }
      }.filter {
        // TODO: cache radars positions instead of recomputing
        it !in terrain.radars.map { it.pos }
      }

  fun act() {
    val act = terrain.myRobots.map {
      it.value.last().orders.poll() ?: Wait()
    }.joinToString("\n") {
      it.printed
    }

    debug(act)
    println(act)
  }

  fun actionList() = LinkedList<Action>()
  operator fun LinkedList<Action>.plus(action: Action): LinkedList<Action> = this.also { add(action) }
  fun debug(vararg message: String) = System.err.println("DBG " + message.joinToString())
}