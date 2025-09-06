package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes
import catgirlroutes.events.impl.ChatPacket
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.events.impl.RoomEnterEvent
import catgirlroutes.events.impl.ServerTickEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.utils.ChatUtils.command
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.noControlCodes
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.Floor
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoPot: Module(
    "Auto Pot",
    Category.DUNGEON,
    "Automatically gets a potion from your potion bag."
){
    private val autoGrabFromBag by BooleanSetting("Auto grab from Potion Bag", false, "When getting a potion, grab it from your Potion Bag.")
    private val autoDrinkAfterStart by BooleanSetting("Auto drink after start", false, "Automatically move potion to hotbar and drink ~3s after dungeon start.")
    private val m7Only by BooleanSetting("M7 only", false, "Gets a pot only when the player is in M7.").withDependency { autoDrinkAfterStart || autoGrabFromBag }

    private var awaitPot = false
    private var triggeredThisDungeon = false
    private var holdUseActive = false
    private var holdUseTicks = 0
    private var holdUseSlot = -1
    private var serverDrinkDelay = -1
    private val DUNGEON_POTION_REGEX = Regex("""Dungeon\s+(?:[1-7]|I{1,3}|IV|V|VI{0,2})\s+Potion""", RegexOption.IGNORE_CASE)

    @SubscribeEvent
    fun onS2D(event: PacketReceiveEvent) {
        if (event.packet !is S2DPacketOpenWindow || !awaitPot) return
        if (event.packet.windowTitle?.unformattedText != "Potion Bag") return
        if (!autoGrabFromBag) { awaitPot = false; return }
        awaitPot = false
        val cwid = event.packet.windowId
        event.isCanceled = true
        scheduleTask(0) {
            if (cwid == -1) return@scheduleTask
            CatgirlRoutes.mc.netHandler.addToSendQueue(C0EPacketClickWindow(cwid, 0, 0, 0, null, 0))
            CatgirlRoutes.mc.netHandler.addToSendQueue(C0DPacketCloseWindow())
        }
    }

    @SubscribeEvent
    fun onChat(event: ChatPacket) {
        if (!autoGrabFromBag || (m7Only && DungeonUtils.floor != Floor.M7)) return
        if (event.message.matches(Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\."))) {
            modMessage("Getting pot!")
            awaitPot = true
            command("potionbag", false)
        }
    }

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) {
        if (triggeredThisDungeon) return
        if (m7Only && DungeonUtils.floor != Floor.M7) return
        val roomName = event.room?.data?.name ?: return
        if (roomName.equals("Entrance", ignoreCase = true)) {
            triggeredThisDungeon = true
            if (autoDrinkAfterStart) serverDrinkDelay = 60
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        triggeredThisDungeon = false
        awaitPot = false
        releaseHold()
        serverDrinkDelay = -1
    }

    private fun movePotionToHotbarAndDrink() {
        val mc = CatgirlRoutes.mc
        val player = mc.thePlayer ?: return

        val inv = player.inventory?.mainInventory ?: return
        val invIndex = inv.indexOfFirst { stack ->
            val name = stack?.displayName?.noControlCodes ?: return@indexOfFirst false
            DUNGEON_POTION_REGEX.containsMatchIn(name)
        }.takeIf { it != -1 } ?: return

        val hotbarIndex = player.inventory.currentItem.coerceIn(0, 8)

        if (invIndex in 0..8) {
            player.inventory.currentItem = invIndex
        } else {
            val slotId = mapMainInvIndexToContainerSlot(invIndex)
            mc.playerController.windowClick(player.openContainer.windowId, slotId, hotbarIndex, 2, player)
        }

        holdUseSlot = player.inventory.currentItem
        holdUseActive = true
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
    }

    private fun mapMainInvIndexToContainerSlot(mainIndex: Int): Int {
        val container = CatgirlRoutes.mc.thePlayer?.openContainer ?: return mainIndex
        val base = container.inventorySlots.size - 36
        return if (mainIndex < 9) {
            base + 27 + mainIndex
        } else {
            base + (mainIndex - 9)
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        if (serverDrinkDelay > 0) serverDrinkDelay--
        if (serverDrinkDelay == 0) {
            serverDrinkDelay = -1
            movePotionToHotbarAndDrink()
        }

        val mc = CatgirlRoutes.mc
        val player = mc.thePlayer

        if (holdUseActive) {
            if (holdUseSlot in 0..8 && player != null) player.inventory.currentItem = holdUseSlot

            val name = player?.heldItem?.displayName?.noControlCodes ?: ""
            val consumed = name.isEmpty() || !DUNGEON_POTION_REGEX.containsMatchIn(name)
            holdUseTicks--
            if (consumed || holdUseTicks <= 0) {
                releaseHold()
            }
        }
    }

    private fun releaseHold() {
        val mc = CatgirlRoutes.mc
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
        holdUseActive = false
        holdUseTicks = 0
        holdUseSlot = -1
    }
}