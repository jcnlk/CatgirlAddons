package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.CatgirlRoutes.Companion.scope
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.Setting.Companion.withInputTransform
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ListSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.PlayerUtils.leftClick2
import catgirlroutes.utils.PlayerUtils.rightClick2
import catgirlroutes.utils.skyblockUUID
import kotlinx.coroutines.*
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.util.MathHelper
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.random.Random


object AutoClicker: Module(
    "Auto Clicker",
    Category.MISC,
    "A simple auto clicker for both left and right click. Activates when the corresponding key is being held down. "
) {
    private val breakBlocks by BooleanSetting("Break blocks", "Allows the player to break blocks.")
    private val clickInGui by BooleanSetting("Click while in inventory", "Continues to auto click while the player is in inventory.")
    private val favouriteItems by BooleanSetting("Favourite items only")
    var favItemsList by ListSetting("FAVOURITE_ITEMS", mutableListOf<String>())
    private val leftClick by BooleanSetting("Left Click", true, "Toggles the auto clicker for left click.")
    private val leftMaxCps: Double by NumberSetting("Left max CPS", 12.0, 1.0, 20.0, 1.0, "Maximum cps for left click.")
        .withInputTransform { input, setting ->
            MathHelper.clamp_double(input, leftMinCps, setting.max)
        }.withDependency { leftClick }

    private val leftMinCps: Double by NumberSetting("Left min CPS", 10.0, 1.0, 20.0, 1.0, "Minimum CPS for left click.")
        .withInputTransform { input, setting ->
            MathHelper.clamp_double(input, setting.min, leftMaxCps)
        }.withDependency { leftClick }

    private val rightClick by BooleanSetting("Right Click", "Toggles the auto clicker for right click.")
    private val rightMaxCps: Double by NumberSetting("Right max CPS", 12.0, 1.0, 20.0, 1.0, "Maximum cps for left click.")
        .withInputTransform { input, setting ->
            MathHelper.clamp_double(input, rightMinCps, setting.max)
        }.withDependency { rightClick }

    private val rightMinCps: Double by NumberSetting("Right min CPS", 10.0, 1.0, 20.0, 1.0, "Minimum CPS for left click.")
        .withInputTransform { input, setting ->
            MathHelper.clamp_double(input, setting.min, rightMaxCps)
        }.withDependency { rightClick }

    private var leftClicking = false
    private var rightClicking = false
    private var leftClickJob: Job? = null
    private var rightClickJob: Job? = null
    private var lastHeldSlot = -1

    private val shouldClick get() = (clickInGui || mc.currentScreen == null) && (!favouriteItems || mc.thePlayer?.heldItem?.skyblockUUID in favItemsList)

    @OptIn(DelicateCoroutinesApi::class)
    @SubscribeEvent
    fun onMouse(event: MouseEvent) {
        if (event.button != 0 && event.button != 1) return

        if (breakingBlocks()) return

        if (event.buttonstate && shouldClick) {
            if (event.button == 0 && leftClick) {
                event.isCanceled = true
                leftClicking = true
                leftClickJob?.cancel()
                leftClickJob = scope.launch { leftClicker() }
            } else if (event.button == 1 && rightClick) {
                event.isCanceled = true
                rightClicking = true
                rightClickJob?.cancel()
                rightClickJob = scope.launch { rightClicker() }
            }
        } else {
            if (event.button == 0) {
                leftClicking = false
                leftClickJob?.cancel()
                leftClickJob = null
            } else {
                rightClicking = false
                rightClickJob?.cancel()
                rightClickJob = null
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        val current = mc.thePlayer?.inventory?.currentItem ?: return

        if (lastHeldSlot == -1) {
            lastHeldSlot = current
            return
        }

        if (current != lastHeldSlot) {
            lastHeldSlot = current

            rightClicking = false
            rightClickJob?.cancel()
            rightClickJob = null

            leftClicking = false
            leftClickJob?.cancel()
            leftClickJob = null
        }
    }

    private suspend fun leftClicker() {
        while (leftClicking && shouldClick) {
            if (!breakingBlocks()) leftClick2()
            delay(Random.nextDouble(1000.0 / leftMaxCps, 1000.0 / leftMinCps).toLong())
        }
    }

    private suspend fun rightClicker() {
        while (rightClicking && shouldClick) {
            rightClick2()
            delay(Random.nextDouble(1000.0 / rightMaxCps, 1000.0 / rightMinCps).toLong())
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        leftClicking = false
        leftClickJob?.cancel()
        leftClickJob = null

        rightClicking = false
        rightClickJob?.cancel()
        rightClickJob = null

        lastHeldSlot = -1
    }

    private var breakHeld: Boolean = false

    private fun breakingBlocks(): Boolean {
        if (!breakBlocks) return false
        if (mc.objectMouseOver != null) {
            if (mc.objectMouseOver.blockPos != null) {
                val block: Block = mc.theWorld.getBlockState(mc.objectMouseOver.blockPos).block
                if (block !== Blocks.air && block !is BlockLiquid) {
                    if (!breakHeld) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.keyCode, true)
                        KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
                        breakHeld = true
                    }
                    return true
                }
                if (breakHeld) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.keyCode, false)
                    breakHeld = false
                }
            }
        }
        return false
    }
}