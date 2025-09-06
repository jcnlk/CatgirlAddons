package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.Island
import catgirlroutes.utils.LocationManager
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.floor

object F7sim : Module(
    "F7 Sim",
    Category.MISC,
    "Simulates 500 speed and lava bounce on single player."
){
    private val toggleLava by BooleanSetting("Toggle lava", false)
    private val toggleSpeed by BooleanSetting("Toggle speed", false)
    private val playerSpeed by NumberSetting("Player speed", 500.0, 100.0, 500.0, 1.0).withDependency { toggleSpeed }
    
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LocationManager.currentArea.isArea(Island.SinglePlayer)) return
        if (toggleSpeed) {
            mc.thePlayer.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).baseValue = playerSpeed / 1000
            mc.thePlayer.capabilities?.setPlayerWalkSpeed((playerSpeed / 1000).toFloat())
        }
        if (toggleLava && (mc.thePlayer.isInLava || mc.theWorld.getBlockState(BlockPos(floor(mc.thePlayer.posX), floor(mc.thePlayer.posY), floor(mc.thePlayer.posZ))).block == Blocks.rail) && mc.thePlayer.posY - floor(mc.thePlayer.posY) < 0.1) {
            mc.thePlayer.setVelocity(mc.thePlayer.motionX, 3.5, mc.thePlayer.motionZ)
        }
    }
}