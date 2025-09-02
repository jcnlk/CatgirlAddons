package catgirlroutes.utils.dungeon

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.utils.ChatUtils.devMessage
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.PlayerUtils.airClick
import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonTeammatesNoSelf
import catgirlroutes.utils.dungeon.DungeonUtils.getMageCooldownMultiplier
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LeapUtils {
    private var leapQueue = mutableListOf<String>()
    private var menuOpened = false
    private var inProgress = false
    private var clickedLeap = false
    private var cwid = -1

    private var lastLeap = 0L
    private val leapCD get() = 2400 * getMageCooldownMultiplier()

    private val currentLeap get() = leapQueue[0]
    private val inQueue get() = leapQueue.size > 0

    private fun reloadGui() {
        menuOpened = false
        leapQueue.removeFirst()
        inProgress = false
    }

    fun queueLeap(name: String) {
        leapQueue.add(name)
    }

    fun click(slot: Int) {
        if (cwid == -1) return
        mc.netHandler.addToSendQueue(C0EPacketClickWindow(cwid, slot, 0, 0, null, 0))
    }

    fun leap(target: Any) {
        if (!inDungeons || inProgress || target == DungeonClass.Unknown) return
        val elapsed = System.currentTimeMillis() - lastLeap
        if (elapsed < leapCD) {
            modMessage("§cFailed to leap! On cooldown: ${"%.1f".format((leapCD - elapsed) / 1000.0)}s")
            return
        }
        val teammate = when (target) {
            is String -> dungeonTeammatesNoSelf.firstOrNull { it.name == target }
            is DungeonClass -> dungeonTeammatesNoSelf.firstOrNull { it.clazz == target }
            else -> return
        }

        if (teammate != null) {
            inProgress = true
            swapFromName("Infinileap") {
                airClick()
                clickedLeap = true
                lastLeap = System.currentTimeMillis()
                modMessage("§аLeaping to $target")
            }
            leapQueue.add(teammate.name)
        } else {
            inProgress = false
            modMessage("§cFailed to leap! §r$target §cnot found")
        }
    }

    @SubscribeEvent
    fun onS2F(event: PacketReceiveEvent) {
        if (event.packet !is S2FPacketSetSlot) return
        if (!inQueue || !menuOpened) return
        devMessage("5")

        val slot = event.packet.func_149173_d()
        val itemStack = event.packet.func_149174_e() ?: return

        devMessage("6")
        if (slot > 35) {
            modMessage("§cFailed to leap! §r$currentLeap §cnot found!")
            reloadGui()
            return
        }
        devMessage("7")
        event.isCanceled = true

        devMessage(itemStack.displayName)
        if (itemStack.displayName.contains(currentLeap)) {
            click(slot)
            reloadGui()
            devMessage("8")
        }
    }

    @SubscribeEvent
    fun onS2D(event: PacketReceiveEvent) {
        if (event.packet !is S2DPacketOpenWindow) return
        cwid = event.packet.windowId
        devMessage("1")
        if (!inQueue) return
        devMessage("2")
        val title = event.packet.windowTitle.unformattedText
        devMessage(title)
        if (!title.contains("Leap")) {
            menuOpened = false
            return
        }
        devMessage("3")
        menuOpened = true
        clickedLeap = false
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onCloseServer(event: PacketReceiveEvent) {
        if (event.packet !is S2EPacketCloseWindow) return
        cwid = -1
        menuOpened = false
    }

    @SubscribeEvent
    fun onCloseClient(event: PacketSentEvent) {
        if (event.packet !is C0DPacketCloseWindow) return
        cwid = -1
        menuOpened = false
    }
}