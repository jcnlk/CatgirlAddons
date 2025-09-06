package catgirlroutes.events

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.*
import catgirlroutes.utils.*
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonItemDrops
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.dungeon.DungeonUtils.isSecret
import catgirlroutes.utils.dungeon.DungeonUtils.termGuiTitles
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import net.minecraft.entity.item.EntityItem
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

object EventDispatcher { // I didn't come up with anything better so I'm just skibidiing odon clint :(

    @SubscribeEvent
    fun onRemoveEntity(event: EntityRemovedEvent) = with(event.entity) { // secret item
        if (inDungeons && this is EntityItem && this.entityItem?.unformattedName?.containsOneOf(dungeonItemDrops, true) != false && mc.thePlayer.getDistanceToEntity(this) <= 6)
            SecretPickupEvent.Item(this).postAndCatch()
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) = with(event.packet) { // secret interact
        if (inDungeons && this is C08PacketPlayerBlockPlacement && position != null)
            SecretPickupEvent.Interact(position, mc.theWorld?.getBlockState(position)?.takeIf { isSecret(it, position) } ?: return).postAndCatch()
    }

    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) {
        if (event.packet is S29PacketSoundEffect && inDungeons && !inBoss && (event.packet.soundName.equalsOneOf("mob.bat.hurt", "mob.bat.death") && event.packet.volume == 0.1f)) SecretPickupEvent.Bat(event.packet).postAndCatch()
        if (event.packet is S32PacketConfirmTransaction) ServerTickEvent().postAndCatch()
        if (event.packet is S02PacketChat) ChatPacket(event.packet.chatComponent.unformattedText.noControlCodes, event.packet.chatComponent.formattedText).postAndCatch()
    }

    @SubscribeEvent
    fun onNetworkEvent(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        event.manager.channel().pipeline().addAfter("fml:packet_handler", "CGA_packet_handler", object : ChannelDuplexHandler() {
            override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {

                if (msg is Packet<*>) {
                    val packetEvent = RawPacketReceiveEvent(msg)
                    MinecraftForge.EVENT_BUS.post(packetEvent)

                    if (msg is S2DPacketOpenWindow) {
                        val title = msg.windowTitle.formattedText.noControlCodes
                        if (title.containsOneOf(termGuiTitles)) TermOpenEvent(msg).postAndCatch()
                    }
                }
                ctx?.fireChannelRead(msg)
            }
        })
    }
}