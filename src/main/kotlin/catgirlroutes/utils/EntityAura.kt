package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.utils.PacketUtils.sendPacket
import catgirlroutes.utils.render.WorldRenderUtils
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

object EntityAura {

    val entityArray = mutableListOf<EntityAuraAction>()
    private val recentClicks = mutableListOf<MovingObjectPosition>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || entityArray.isEmpty()) return
        entityArray.forEach{auraAction ->
            if (distanceToPlayer(auraAction.entity.posX, auraAction.entity.posY, auraAction.entity.posZ) < 5) {
                if (auraAction.action == C02PacketUseEntity.Action.INTERACT) {
                    sendPacket(C02PacketUseEntity(auraAction.entity, C02PacketUseEntity.Action.INTERACT))
                } else if (auraAction.action == C02PacketUseEntity.Action.INTERACT_AT) {
                    val expandValue: Double = auraAction.entity.collisionBorderSize.toDouble()
                    val eyePos = mc.thePlayer.getPositionEyes(0f)
                    val movingObjectPosition = auraAction.entity.entityBoundingBox.expand(expandValue, expandValue, expandValue).calculateIntercept(eyePos, getEntityCenter(auraAction.entity)) ?: return@forEach
                    sendPacket(C02PacketUseEntity(auraAction.entity, Vec3(movingObjectPosition.hitVec.xCoord, movingObjectPosition.hitVec.yCoord, movingObjectPosition.hitVec.zCoord)))
                    recentClicks.add(movingObjectPosition)
                    ClientListener.scheduleTask(10) { recentClicks.remove(movingObjectPosition) }
                }
                entityArray.remove(auraAction)
                return
            }
        }
    }

    private fun getEntityCenter(entity: Entity): Vec3 {
        val boundingBox = entity.entityBoundingBox
        val centerX = (boundingBox.minX + boundingBox.maxX) / 2
        val centerY = (boundingBox.minY + boundingBox.maxY) / 2
        val centerZ = (boundingBox.minZ + boundingBox.maxZ) / 2
        return Vec3(centerX, centerY, centerZ)
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        recentClicks.forEach{objectPos ->
            WorldRenderUtils.drawCustomSizedBoxAt(
                objectPos.blockPos.x + objectPos.hitVec.xCoord,
                objectPos.blockPos.y + objectPos.hitVec.yCoord,
                objectPos.blockPos.z + objectPos.hitVec.zCoord,
                0.1,
                0.1,
                0.1,
                Color.BLUE
            )
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        entityArray.clear()
    }

    data class EntityAuraAction (
        val entity: Entity,
        val action: C02PacketUseEntity.Action,
    )
}