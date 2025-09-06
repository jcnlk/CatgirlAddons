package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.ServerTickEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.utils.*
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.MovementUtils.setKey
import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posY
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.PlayerUtils.swapToSlot
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.render.WorldRenderUtils.drawSquare
import catgirlroutes.utils.rotation.RotationUtils.snapTo
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object Relics: Module(
    "Relics",
    Category.DUNGEON
) {
    private val aura by BooleanSetting("Aura", "Automatically grabs and places the relic.")
    private val look by BooleanSetting("Look", "Looks and walks in the right direction after picking up the relic.").withDependency { aura }

    private val onOrangeSpot get() = posX in 90.0..90.7 && posY == 6.0 && posZ in 55.0..55.7
    private val onRedSpot get() = posX in 22.3..23.0 && posY == 6.0 && posZ in 58.0..58.7

    private var currentRelic = Relic.None
    private var auraCooldown = false

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        drawRings()
        if (auraCooldown || !aura) return
        val armourStands = mc.theWorld?.loadedEntityList?.firstOrNull {
            it is EntityArmorStand &&
            it.inventory?.get(4)?.displayName?.contains("Relic") == true &&
            mc.thePlayer.getDistanceToEntity(it) < 4.5
        } ?: return

        if (currentRelic == Relic.None) return

        auraCooldown = true
        grabRelic(armourStands)

        if (aura && currentRelic != Relic.None) {
            BlockAura.addBlockNoDupe(currentRelic.blockPos)
        }

        if (look) doLook()

        scheduleTask(20) {
            auraCooldown = false
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        currentRelic = Relic.entries.find {
            mc.thePlayer?.inventory?.mainInventory?.any { item ->
                item.skyblockID == it.skyblockID
            } == true
        } ?: Relic.None
    }

    private fun drawRings() {
        if (!look || !inBoss) return
        drawSquare(90.35, 6.0, 55.35, 0.7, 0.7, if (onOrangeSpot) Color.GREEN else Color.RED, phase = false)
        drawSquare(22.65, 6.0, 58.35, 0.7, 0.7, if (onRedSpot) Color.GREEN else Color.RED, phase = false)
    }

    private fun grabRelic(entity: Entity) {
        val objectMouseOver = mc.objectMouseOver?.hitVec ?: return
        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, Vec3(objectMouseOver.xCoord - entity.posX, objectMouseOver.yCoord - entity.posY, objectMouseOver.zCoord - entity.posZ)))
    }

    private fun doLook() {
        swapToSlot(8)
        when {
            onOrangeSpot -> {
                snapTo(111f, 0f)
                setKey("w", true)
            }
            onRedSpot -> {
                snapTo(-120f, 0f)
                setKey("w", true)
            }
        }
    }

    enum class Relic(val skyblockID: String, val blockPos: BlockPos) {
        Red("RED_KING_RELIC", BlockPos(51.0, 7.0, 42.0)),
        Green("GREEN_KING_RELIC", BlockPos(49.0, 7.0, 44.0)),
        Purple("PURPLE_KING_RELIC", BlockPos(54.0, 7.0, 41.0)),
        Blue("BLUE_KING_RELIC", BlockPos(59.0, 7.0, 44.0)),
        Orange("ORANGE_KING_RELIC", BlockPos(57.0, 7.0, 42.0)),
        None("", BlockPos(0.0, 0.0, 0.0))
    }
}