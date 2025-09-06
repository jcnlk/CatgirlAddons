package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.KeyBindSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.PlayerUtils.leftClick
import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.SwapState
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.clock.Executor.Companion.register
import catgirlroutes.utils.dungeon.DungeonUtils.isSecret
import net.minecraft.util.BlockPos
import org.lwjgl.input.Keyboard

object GhostBlocks : Module(  // todo: add delay, range, option to gkey skulls maybeidk
    "Ghost Blocks",
    category = Category.DUNGEON,
    description = "Creates ghost blocks where you are looking when the key bind is pressed."
) {
    private val gKey by BooleanSetting("GKey")
    private val swingHand by BooleanSetting("Swing hand", true, "Swings the player's hand.").withDependency { gKey }
    private val gKeyDelay by NumberSetting("Delay", 50.0, 50.0, 1000.0, 50.0, "Delay for gkey").withDependency { gKey }
    private val gKeyKeyBind by KeyBindSetting("GKey Key", Keyboard.KEY_NONE, "Creates ghost blocks where you are looking.").withDependency { gKey }

    private val swapStonk by BooleanSetting("Swap Stonk", false);

    private var running = false
    private var prevSlot = -1
    private val swapKeyBind by KeyBindSetting("Swap Key", Keyboard.KEY_NONE, "Automatically makes a ghost block using a swap to pickaxe.").withDependency { swapStonk }
        .onPress {
            if (!this.swapStonk || !this.enabled) return@onPress
            if (this.running) return@onPress;

            this.running = true;
            this.prevSlot = mc.thePlayer.inventory.currentItem;

            val swapResult = swapFromName("pickaxe")
            if (swapResult == SwapState.UNKNOWN) {
                this.running = false
                return@onPress
            }

            scheduleTask(1) { leftClick() }
            scheduleTask(2) {
                mc.thePlayer.inventory.currentItem = this.prevSlot
                this.running = false
            }
        }

    init {
        gKeyLoop()
    }

    private fun gKeyLoop() {
        Executor(gKeyDelay.toLong()) {
            if (!gKeyKeyBind.isDown() || !gKey || !this.enabled || mc.currentScreen != null) return@Executor
            val blockPos = mc.objectMouseOver.blockPos

            if (isSecret(mc.theWorld.getBlockState(blockPos), blockPos)) return@Executor
            if (swingHand) mc.thePlayer.swingItem()
            toAir(blockPos)
        }.register()
    }

    private fun toAir(blockPos: BlockPos?): Boolean {
        if (blockPos != null) {
            mc.theWorld.setBlockToAir(mc.objectMouseOver.blockPos)
            return true
        }
        return false
    }
}
