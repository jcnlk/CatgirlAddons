package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.SkyblockJoinIslandEvent
import catgirlroutes.module.impl.render.ClickGui
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.clock.Executor.Companion.register
import catgirlroutes.utils.dungeon.Dungeon
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.Floor
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

object LocationManager {
    var onHypixel: Boolean = false
    var inSkyblock: Boolean = false

    var currentDungeon: Dungeon? = null
    var currentArea: Island = Island.Unknown
    var kuudraTier: Int = 0

    init {
        Executor(500) {
            if (!inSkyblock)
                inSkyblock = onHypixel && mc.theWorld?.scoreboard?.getObjectiveInDisplaySlot(1)
                    ?.let { cleanSB(it.displayName).contains("SKYBLOCK") } == true || ClickGui.forceSkyblock

            if (currentArea.isArea(Island.Kuudra) && kuudraTier == 0)
                sidebarLines.find { cleanLine(it).contains("Kuudra's Hollow (") }?.let {
                    kuudraTier = it.substringBefore(")").lastOrNull()?.digitToIntOrNull() ?: 0
                }

            if (currentArea.isArea(Island.Unknown)) {
                val previousArea = currentArea
                currentArea = getArea()
                if (!currentArea.isArea(Island.Unknown) && previousArea != currentArea) SkyblockJoinIslandEvent(
                    currentArea
                ).postAndCatch()
            }

            if ((DungeonUtils.inDungeons || currentArea.isArea(Island.SinglePlayer)) && currentDungeon == null) currentDungeon =
                Dungeon(getFloor() ?: return@Executor)

        }.register()
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        onHypixel = false
        inSkyblock = false
        currentArea = Island.Unknown
        SkyblockJoinIslandEvent(currentArea).postAndCatch()
        currentDungeon = null
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Unload) {
        currentDungeon = null
        inSkyblock = false
        currentArea = Island.Unknown
    }

    /**
     * Taken from [SBC](https://github.com/Harry282/Skyblock-Client/blob/main/src/main/kotlin/skyblockclient/utils/LocationUtils.kt)
     *
     * @author Harry282
     */
    @SubscribeEvent
    fun onConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        onHypixel = mc.runCatching {
            !event.isLocal && ((thePlayer?.clientBrand?.lowercase()?.contains("hypixel")
                ?: currentServerData?.serverIP?.lowercase()?.contains("hypixel")) == true) || ClickGui.forceHypixel
        }.getOrDefault(false)
    }

    /**
     * Returns the current area from the tab list info.
     * If no info can be found, return Island.Unknown.
     *
     * @author Aton
     */
    fun getArea(): Island {
        if (mc.isSingleplayer) return Island.SinglePlayer
        if (!inSkyblock) return Island.Unknown
        val netHandlerPlayClient: NetHandlerPlayClient = mc.thePlayer?.sendQueue ?: return Island.Unknown
        val list = netHandlerPlayClient.playerInfoMap ?: return Island.Unknown

        val area = list.find {
            it?.displayName?.unformattedText?.startsWith("Area: ") == true ||
                    it?.displayName?.unformattedText?.startsWith("Dungeon: ") == true
        }?.displayName?.formattedText

        return Island.entries.firstOrNull { area?.contains(it.displayName, true) == true } ?: Island.Unknown
    }

    fun getFloor(): Floor? {
        if (currentArea.isArea(Island.SinglePlayer)) return Floor.E
        for (i in sidebarLines) {
            val floor = Regex("The Catacombs \\((\\w+)\\)\$").find(cleanSB(i))?.groupValues?.get(1) ?: continue
            return Floor.valueOf(floor)
        }
        return null
    }
}