package catgirlroutes.module.impl.dungeons.puzzlesolvers

import catgirlroutes.events.impl.ChatPacket
import catgirlroutes.events.impl.MotionUpdateEvent
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.events.impl.RoomEnterEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.impl.dungeons.puzzlesolvers.WaterSolver.waterInteract
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.DropdownSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.clock.Executor.Companion.register
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

object Puzzles : Module (
    "Puzzles",
    Category.DUNGEON,
    "Puzzle solvers"
) {
    private val fillDropdown by DropdownSetting("Ice fill")
    var fillSolver by BooleanSetting("Ice fill solver", "Shows you the solution to the ice fill puzzle.").withDependency(fillDropdown)
    var fillAuto by BooleanSetting("Auto ice fill", "Automatically completes the ice fill puzzle").withDependency(fillDropdown) { fillSolver }
    var fillDelay by NumberSetting("Auto ice fill delay", 2.0, 1.0, 10.0, 1.0, unit = "t").withDependency(fillDropdown) { fillSolver && fillAuto }

    private val tttDropdown by DropdownSetting("Tic tac toe")
    private var tttSolver by BooleanSetting("Tic tac toe solver", "Shows you the solution to the tic tac toe puzzle.").withDependency(tttDropdown)
    private val renderNext by BooleanSetting("Show next move", true, "Shows which move is next.").withDependency(tttDropdown) { tttSolver }
    var tttAuto by BooleanSetting("Auto TTT", "Automatically completes the tic tac toe puzzle").withDependency(tttDropdown) { tttSolver }
    var tttReach by NumberSetting("Auto TTT reach", 4.5, 1.0, 6.0, 0.1).withDependency(tttDropdown) { tttSolver && tttAuto }

    private val wbDropdown by DropdownSetting("Water board")
    val wbSolver by BooleanSetting("Water board solver", "Shows you the solution to the water puzzle.").withDependency(wbDropdown)
    private val wbTracer by BooleanSetting("Show Tracer", true, "Shows a tracer to the next lever.").withDependency(wbDropdown) { wbSolver }

    private val weirdosDropdown by DropdownSetting("Three weirdos")
    val weirdosSolver by BooleanSetting("Weirdo solver", "Shows you the solution to the three weirdos puzzle.").withDependency(weirdosDropdown)
    val weirdosAuto by BooleanSetting("Auto weirdos", "Automatically completes the three weirdos puzzle").withDependency(weirdosDropdown) { weirdosSolver }

    init {
        Executor(500) {
            if (!inDungeons || inBoss) return@Executor
            if (wbSolver) WaterSolver.scan()
        }.register()
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        IceFillSolver.reset()
        TicTacToeSolver.onWorldLoad()
        WaterSolver.reset()
        AutoWeirdos.onWorldChange()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onMotionUpdate(event: MotionUpdateEvent.Post) {
        TicTacToeSolver.onMotion()
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (event.packet is C08PacketPlayerBlockPlacement) {
            if (wbSolver) waterInteract(event.packet)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: ChatPacket) {
        AutoWeirdos.onChat(event.message)
    }

    @SubscribeEvent
    fun onWorldLast(event: RenderWorldLastEvent) {
        if (fillSolver) IceFillSolver.onRenderWorld(Color.GREEN)
        if (tttSolver) TicTacToeSolver.onRenderWorld()
        if (wbSolver) WaterSolver.onRenderWorld(wbTracer)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        TicTacToeSolver.onTick()
        WaterSolver.onTick()
        AutoWeirdos.onTick()
    }

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) {
        if (fillSolver) IceFillSolver.onRoomEnter(event)
    }
}