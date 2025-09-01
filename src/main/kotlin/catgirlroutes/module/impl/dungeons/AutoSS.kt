package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.BlockChangeEvent
import catgirlroutes.events.impl.ChatPacket
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.Visibility
import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.StringSetting
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.LocationManager
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.clock.Executor.Companion.register
import catgirlroutes.utils.render.WorldRenderUtils.drawCustomSizedBoxAt
import catgirlroutes.utils.render.WorldRenderUtils.drawStringInWorld
import catgirlroutes.utils.rotation.RotationUtils.getYawAndPitch
import catgirlroutes.utils.rotation.RotationUtils.rotateSmoothly
import catgirlroutes.utils.rotation.RotationUtils.targets
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.random.Random

/** @Author Kaze.0707**/

//Sponsored by chinese spyware

object AutoSS : Module(
    name = "AutoSS",
    Category.DUNGEON
){
    private val autostartDelays by StringSetting("Autostart delays", "0,125,250", 50, "delay1,delay2,delay3", "Delays in ms for the three autostart clicks, separated by commas (e.g. 0,125,250).")
    val delay by NumberSetting("Delay", 200.0, 50.0, 500.0, 10.0, "AutoSS delay.", unit = "ms")
    private val randomnessAmount by NumberSetting("Click randomness", 0.0, 0.0, 100.0, 1.0, "Maximum extra random delay added to each click (0 = disabled).", unit = "ms")
    private val smoothRotate by BooleanSetting("Rotate", "Rotates smoothly.")
    private val time by NumberSetting("Rotation speed", 200.0, 0.0, 500.0, 10.0, unit = "ms").withDependency { this.smoothRotate }
    private val dontCheck by BooleanSetting("Faster SS", "Makes SS a bit faster by not checking if the button is spawned.")
    private val forceDevice by BooleanSetting("Force device", "Makes the mod think the player is in front of the device.", Visibility.ADVANCED_ONLY)
    private val resetSS by ActionSetting("Reset SS", "Resets the device") {reset(); doingSS = false; clicked = false}

    init {
        ssLoop()
    }

    private var lastClickAdded = System.currentTimeMillis()
    private var next = false
    private var progress = 0
    private var doneFirst = false
    private var doingSS = false
    private var clicked = false
    private var clicks = ArrayList<BlockPos>()
    private var wtflip = System.currentTimeMillis()
    private var clickedButton: Vec3? = null
    private var allButtons = ArrayList<Vec3>()
    private val startButton = BlockPos(110, 121, 91)
    private val defaultDelays = listOf(0L, 125L, 250L)

    fun reset() {
        allButtons.clear()
        clicks.clear()
        next = false
        progress = 0
        doneFirst = false
        doingSS = false
        clicked = false
        debugMessage("Reset!")
    }

    override fun onKeyBind() {
        start()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        reset()
    }

    private fun parseAutostartDelays(): List<Long> {
        return try {
            val delayStrings = autostartDelays.split(",")
            if (delayStrings.size != 3) {
                throw IllegalArgumentException("Expected exactly 3 delays separated by commas")
            }

            val delays = delayStrings.map { delayStr ->
                val delay = delayStr.trim().toLong()
                if (delay < 0 || delay > 5000) {
                    throw IllegalArgumentException("Delay must be between 0 and 5000ms")
                }
                delay
            }
            delays
        } catch (e: Exception) {
            modMessage("§cInvalid autostart delays format! Expected format: '0,125,250'. Using default delays (0,125,250).")
            debugMessage("Autostart delay parsing error: ${e.message}")
            defaultDelays
        }
    }

    fun start() {
        allButtons.clear()
        val (yaw, pitch) = getYawAndPitch(110.875, 121.5, 91.5)
        if (smoothRotate) {
            rotateSmoothly(yaw, pitch, time.toInt())
        }
        if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return
        if (!clicked) {
            debugMessage("Starting SS")
            debugMessage(System.currentTimeMillis())
            reset()
            clicked = true
            doingSS = true

            val delays = parseAutostartDelays()

            Thread {
                try {
                    reset()
                    clickButton(startButton.x, startButton.y, startButton.z)
                    val firstDelay = delays[0] + if (randomnessAmount > 0) Random.nextLong(randomnessAmount.toLong() + 1) else 0
                    if (firstDelay > 0) Thread.sleep(firstDelay)

                    reset()
                    clickButton(startButton.x, startButton.y, startButton.z)
                    val secondDelay = delays[1] + if (randomnessAmount > 0) Random.nextLong(randomnessAmount.toLong() + 1) else 0
                    if (secondDelay > 0) Thread.sleep(secondDelay)

                    doingSS = true
                    val thirdDelay = delays[2] + if (randomnessAmount > 0) Random.nextLong(randomnessAmount.toLong() + 1) else 0
                    if (thirdDelay > 0) Thread.sleep(thirdDelay)
                    clickButton(startButton.x, startButton.y, startButton.z)
                } catch (e: Exception) {
                    modMessage("§cError in autostart: ${e.message}")
                }
            }.start()
        }
    }

    @SubscribeEvent
    fun onChat(event: ChatPacket) {
        val msg = event.message
        if (mc.thePlayer == null || mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return
        if (msg.contains("Device")) {
            debugMessage(System.currentTimeMillis())
        }
        if (!msg.contains("Who dares trespass into my domain")) return
        debugMessage("Starting SS")
        start()
    }

    private fun ssLoop() {
        Executor(10) {
            if (System.currentTimeMillis() - lastClickAdded + 1 < delay) return@Executor
            if (mc.theWorld == null) return@Executor
            if (!this.enabled) return@Executor
            if (!LocationManager.inSkyblock && !forceDevice) return@Executor
            val detect: Block = mc.theWorld.getBlockState(BlockPos(110, 123, 92)).block

            if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return@Executor

            var device = false

            mc.theWorld.loadedEntityList
                .filterIsInstance<EntityArmorStand>()
                .filter { it.getDistanceToEntity(mc.thePlayer) < 6 && it.displayName.unformattedText.contains("Device") }
                .forEach { _ ->
                    device = true
                }

            if (forceDevice) device = true

            if (!device) {
                clicked = false
                return@Executor
            }

            if ((detect == Blocks.stone_button || (dontCheck && doneFirst)) && doingSS) {
                if (!doneFirst && clicks.size == 3) {
                    clicks.removeAt(0)
                    allButtons.removeAt(0)
                }
                doneFirst = true
                if (progress < clicks.size) {
                    val next: BlockPos = clicks[progress]
                    if (mc.theWorld.getBlockState(next).block == Blocks.stone_button) {
                        if (smoothRotate) {
                            val (yaw, pitch) = getYawAndPitch(next.x.toDouble() + 0.875, next.y.toDouble() + 0.5, next.z.toDouble() + 0.5)
                            targets.add(Vec3(yaw.toDouble(), pitch.toDouble(), time))
                        }
                        clickButton(next.x, next.y, next.z)
                        progress++
                    }
                }
            }
        }.register()
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!this.enabled) return
        if (!LocationManager.inSkyblock && !forceDevice) return
        if (mc.theWorld == null) return

        if (System.currentTimeMillis() - lastClickAdded > delay) clickedButton = null

        if (mc.thePlayer.getDistanceSqToCenter(startButton) < 1600) {
            if (clickedButton != null) {
                drawCustomSizedBoxAt(clickedButton!!.xCoord + 0.875, clickedButton!!.yCoord + 0.375, clickedButton!!.zCoord + 0.3125, 0.125, 0.25, 0.375, Color.PINK, filled = true)
            }
            allButtons.forEachIndexed{index, location ->
                drawStringInWorld((index + 1).toString(), Vec3(location.xCoord - 0.0625, location.yCoord + 0.5625, location.zCoord + 0.5), scale = 0.02f, shadow = true, depthTest = false)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val mop: MovingObjectPosition = mc.objectMouseOver ?: return
        if (System.currentTimeMillis() - wtflip < 1000) return
        wtflip = System.currentTimeMillis()
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && startButton == event.pos && startButton == mop.blockPos && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            clicked = false
            reset()
            start()
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (event.pos.x == 111 && event.pos.y >= 120 && event.pos.y <= 123 && event.pos.z >= 92 && event.pos.z <= 95) {
            val button = BlockPos(110, event.pos.y, event.pos.z)
            if (event.update.block == Blocks.sea_lantern) {
                if (clicks.size == 2) {
                    if (clicks[0] == button && !doneFirst) {
                        doneFirst = true
                        clicks.removeFirst()
                        allButtons.removeFirst()
                    }
                }
                if (!clicks.contains(button)) {
                    debugMessage("Added to clicks: x: ${event.pos.x}, y: ${event.pos.y}, z: ${event.pos.z}")
                    progress = 0
                    clicks.add(button)
                    allButtons.add(Vec3(event.pos.x.toDouble(), event.pos.y.toDouble(), event.pos.z.toDouble()))
                }
            }
        }
    }

    private fun clickButton(x: Int, y: Int, z: Int) {
        if (mc.thePlayer.getDistanceSqToCenter(BlockPos(x, y, z)) > 25) return
        debugMessage("Clicked at: x: ${x}, y: ${y}, z: ${z}. Time: ${System.currentTimeMillis()}")
        clickedButton = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
        lastClickAdded = System.currentTimeMillis()
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos(x, y, z), 4, mc.thePlayer.heldItem, 0.875f, 0.5f, 0.5f))
    }
}