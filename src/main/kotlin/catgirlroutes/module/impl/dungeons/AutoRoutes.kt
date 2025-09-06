package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.CatgirlRoutes.Companion.totalTicks
import catgirlroutes.commands.impl.Node
import catgirlroutes.commands.impl.NodeManager
import catgirlroutes.commands.impl.NodeManager.nodes
import catgirlroutes.commands.impl.nodeEditMode
import catgirlroutes.events.impl.*
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.impl.player.PearlClip
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.SelectorSetting
import catgirlroutes.utils.*
import catgirlroutes.utils.ChatUtils.commandAny
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.PlayerUtils.airClick
import catgirlroutes.utils.PlayerUtils.leftClick2
import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posY
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.PlayerUtils.recentlySwapped
import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.dungeon.DungeonUtils.getRealCoords
import catgirlroutes.utils.dungeon.DungeonUtils.getRealYaw
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.dungeon.ScanUtils.currentRoom
import catgirlroutes.utils.render.WorldRenderUtils.drawBlock
import catgirlroutes.utils.render.WorldRenderUtils.drawCylinder
import catgirlroutes.utils.render.WorldRenderUtils.drawP3boxWithLayers
import catgirlroutes.utils.render.WorldRenderUtils.renderGayFlag
import catgirlroutes.utils.render.WorldRenderUtils.renderLesbianFlag
import catgirlroutes.utils.render.WorldRenderUtils.renderTransFlag
import catgirlroutes.utils.rotation.RotationUtils.snapTo
import kotlinx.coroutines.*
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.passive.EntityBat
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import java.awt.Color.*
import kotlin.collections.set
import kotlin.math.floor

object AutoRoutes : Module( // todo recode this shit
    "Auto Routes",
    Category.DUNGEON,
    "A module that allows you to place down nodes that execute various actions."
) {
    private val editTitle by BooleanSetting("EditMode title", false)
    private val chatFeedback by BooleanSetting("Chat feedback", true, "Sends chat messages when the ring is activated.")
    private val boomType by SelectorSetting("Boom type","Regular", arrayListOf("Regular", "Infinity"), "Superboom TNT type to use for BOOM ring.")

    private val preset by SelectorSetting("Node style","Trans", arrayListOf("Trans", "Normal", "Ring", "LGBTQIA+", "Lesbian"), "Ring render style to be used.")
    private val layers by NumberSetting("Ring layers amount", 3.0, 1.0, 5.0, 1.0, "Amount of ring layers to render").withDependency { preset.selected.equalsOneOf("Normal", "Ring") }
    private val colour1 by ColorSetting("Ring colour (inactive)", black, true, "Colour of Normal ring style while inactive").withDependency { preset.selected.equalsOneOf("Normal", "Ring") }
    private val colour2 by ColorSetting("Ring colour (active)", WHITE, true, "Colour of Normal ring style while active").withDependency { preset.selected.equalsOneOf("Normal", "Ring") }

    private val cooldownMap = mutableMapOf<String, Boolean>()

    private var awaitSecret = AwaitThing()
    private var awaitBat = AwaitThing()

    @SubscribeEvent
    fun onSkyblockIsland(event: SkyblockJoinIslandEvent) = NodeManager.loadNodes()

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) = NodeManager.loadNodes()

    @SubscribeEvent
    fun onSecret(event: SecretPickupEvent) = this.awaitSecret.complete()

    @SubscribeEvent(receiveCanceled = true)
    fun onBatSpawn(event: PacketReceiveEvent) {
        if (event.packet !is S0FPacketSpawnMob) return
        if (event.packet.entityType != 65 || distanceToPlayer(event.packet.x / 32, event.packet.y / 32, event.packet.z / 32) > 8) return
        this.awaitBat.complete()
    }

    @SubscribeEvent
    fun onBat(event: TickEvent.ClientTickEvent) {
        val stupid = mc.theWorld?.loadedEntityList
            ?.filterIsInstance<EntityBat>()
            ?.any { distanceToPlayer(it.posX, it.posY, it.posZ) < 10 } == true

        if (stupid) {
            this.awaitBat.complete()
        }
    }

    @SubscribeEvent
    fun onMouse(event: MouseEvent) {
        if (!event.buttonstate || event.button != 0) return

        cooldownMap.forEach { (key, _) ->
            cooldownMap[key] = false
        }

        currentNodes.forEach { node ->
            node.arguments?.let { arguments ->
                if (arguments.contains("await")) {
                    this.awaitSecret.complete()
                    return@forEach
                }
                if (arguments.contains("awaitbat")) {
                    this.awaitBat.complete()
                    return@forEach
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (nodeEditMode || event.phase != TickEvent.Phase.START || mc.thePlayer == null) return
        nodes.forEach { node ->
            val key = "${node.location.xCoord},${node.location.yCoord},${node.location.zCoord},${node.type}"
            val cooldown: Boolean = cooldownMap[key] == true
            if (inNode(node)) {
                if (cooldown) return@forEach

                cooldownMap[key] = true
                GlobalScope.launch {
                    executeAction(node)
                }
            } else if (cooldown) {
                node.arguments?.let {
                    if ("once" in it) return
                }
                cooldownMap[key] = false
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        nodes.forEach { node ->
            val realLocation = currentRoom?.getRealCoords(node.location) ?: node.location

            val x: Double = realLocation.xCoord + 0.5
            val y: Double = realLocation.yCoord
            val z: Double = realLocation.zCoord + 0.5

            val cooldown: Boolean = cooldownMap["${node.location.xCoord},${node.location.yCoord},${node.location.zCoord},${node.type}"] == true
            val color = if (cooldown) colour2 else colour1

            when(preset.selected) {
                "Trans"     -> renderTransFlag(x, y, z, node.width, node.width, node.height)
                "Normal"    -> drawP3boxWithLayers(x, y, z, node.width, node.width, node.height, color, layers.toInt())
                "Ring"      -> drawCylinder(Vec3(x, y, z), node.width / 2, node.width / 2, .05f, 35, 1, 0f, 90f, 90f, color, true)
                "LGBTQIA+"  -> renderGayFlag(x, y, z, node.width, node.width, node.height)
                "Lesbian"   -> renderLesbianFlag(x, y, z, node.width, node.width, node.height)
            }

            if (inNode(node) && node.block != null) {
                val colour = if (node.block!!.first.blockState.toIdMetadataString() == node.block!!.second) GREEN else RED
                drawBlock(currentRoom?.getRealCoords(node.block!!.first.toBlockPos()) ?: node.block!!.first.toBlockPos(), colour)
            }
        }
    }

    @SubscribeEvent
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Post) {
        if (editTitle && nodeEditMode && inDungeons) {
            renderText("Edit Mode")
        }
    }

    private var shouldClick = false
    private var shouldLeftClick = false
    private var shouldClip = false

    private var lastC08 = 0L
    private var stupid = false
    private val canSendC08 get() = totalTicks - lastC08 > 2

    @SubscribeEvent
    fun onPacket(event: PacketSentEventReturn) {
        if (event.packet !is C03PacketPlayer) return
        if (recentlySwapped) {
            return
        }

        if (!canSendC08) return

        if (shouldClick) {
            shouldClick = false
            airClick()
        }
        if (shouldLeftClick) {
            shouldLeftClick = false
            leftClick2()
        }

        if (shouldClip) { // shout out schizo codebase
            shouldClip = false
            PearlClip.pearlClip((currentNode?.depth?.takeIf { it != 0F } ?: 0F).toDouble())
        }
    }

    @SubscribeEvent
    fun onSecretClick(event: PacketSentEvent) {
        if (event.packet !is C08PacketPlayerBlockPlacement) return

        if (event.packet.placedBlockDirection == 255) {
            this.stupid = true
            this.lastC08 = 0L
            return
        }

        this.lastC08 = totalTicks
        this.stupid = false
    }

    @SubscribeEvent
    fun onKeyInputEvent(event: InputEvent.KeyInputEvent) {
        if (Keyboard.getEventKey() == mc.gameSettings.keyBindSneak.keyCode && !nodeEditMode && currentNodes.any { it.type == "warp" }) {
            MovementUtils.setKey("shift", true)
        }
    }

    private suspend fun executeAction(node: Node) {
        var actionDelay: Int = if (node.delay == null) 0 else node.delay!!
        val yaw = currentRoom?.getRealYaw(node.yaw) ?: node.yaw

        if (node.type in setOf("warp", "aotv", "hype", "pearl")) snapTo(yaw, node.pitch)
        node.block?.let { block ->
            val key = "${node.location.xCoord},${node.location.yCoord},${node.location.zCoord},${node.type}"
            if (block.first.blockState.toIdMetadataString() != block.second) {
                cooldownMap[key] = false
                return
            } else if (!recentlySwapped) {
                actionDelay += 50
            }
        }

        if (node.arguments?.contains("await") == true) awaitSecret.await()
        if (node.arguments?.contains("awaitbat") == true) awaitBat.await()

        delay(actionDelay.toLong())
        node.arguments?.let {
            if ("stop" in it) MovementUtils.stopVelo()
            if ("walk" in it) MovementUtils.setKey("w", true)
            if ("look" in it) snapTo(yaw, node.pitch)
            if ("unshift" in it) MovementUtils.setKey("shift", false)
        }

        when(node.type) {
            "warp" -> {
                val state = swapFromName("aspect of the void")
                MovementUtils.setKey("shift", true)
                if (state == SwapState.SWAPPED) {
                    scheduleTask {
                        shouldClick = true
                    }
                } else if (state == SwapState.ALREADY_HELD) {
                    shouldClick = true
                }
                scheduleTask(1) {
                    if (nodes.none { inNode(it) && it.type == "warp" }) {
                        MovementUtils.setKey("shift", false)
                    }
                }
            }
            "aotv" -> {
                swapFromName("aspect of the void")
                MovementUtils.setKey("shift", false)
                scheduleTask { shouldClick = true }
            }
            "hype" -> {
                swapFromName("hyperion")
                scheduleTask(0) { shouldClick = true }
            }
            "walk" -> {
                feedbackMessage("Walking!")
                MovementUtils.setKey("shift", false)
                MovementUtils.setKey("w", true)
            }
            "jump" -> {
                feedbackMessage("Jumping!")
                MovementUtils.jump()
            }
            "stop" -> {
                feedbackMessage("Stopping!")
                MovementUtils.stopMovement()
                MovementUtils.stopVelo()
            }
            "boom" -> {
                feedbackMessage("Bomb denmark!")
                if (boomType.selected == "Regular") swapFromName("superboom tnt") else swapFromName("infinityboom tnt")
                scheduleTask { shouldLeftClick = true }
            }
            "pearl" -> {
                swapFromName("ender pearl")
                MovementUtils.setKey("shift", false)
                scheduleTask { shouldClick = true }
            }
            "pearlclip" -> {
                snapTo(mc.renderManager.playerViewY, 90f)
                shouldClip = true
            }
            "look" -> {
                feedbackMessage("Looking!")
                snapTo(yaw, node.pitch)
            }
            "align" -> {
                feedbackMessage("Aligning!")
                mc.thePlayer.setPosition(floor(mc.thePlayer.posX) + 0.5, mc.thePlayer.posY, floor(mc.thePlayer.posZ) + 0.5)
            }
            "command" -> {
                feedbackMessage("Sexecuting!")
                commandAny(node.command!!)
            }
        }
    }

    private fun inNode(node: Node): Boolean {
        if (mc.theWorld == null) return false
        val realLocation = currentRoom?.getRealCoords(node.location) ?: node.location

        val minX = realLocation.xCoord
        val minY = realLocation.yCoord
        val minZ = realLocation.zCoord
        val maxX = minX + node.width
        val maxY = minY + node.height
        val maxZ = minZ + node.width

        return posX in minX..maxX && posY in minY..maxY && posZ in minZ..maxZ
    }

    val currentNode: Node? get() = nodes.lastOrNull { inNode(it) }
    private val currentNodes: List<Node> get() = nodes.filter { inNode(it) }

    private val Vec3.blockState get() = mc.theWorld.getBlockState(currentRoom?.getRealCoords(BlockPos(this)) ?: BlockPos(this))

    fun IBlockState.toIdMetadataString(): String {
        val id = Block.getIdFromBlock(this.block)
        val metadata = this.block.getMetaFromState(this)
        return "$id:$metadata"
    }

    private class AwaitThing {
        var deferred: CompletableDeferred<Unit>? = null

        fun complete() {
            scheduleTask(1) {
                this.deferred?.complete(Unit)
                this.deferred = null
            }
        }

        suspend fun await() {
            val newDeferred = CompletableDeferred<Unit>()
            this.deferred = newDeferred
            newDeferred.await()
        }
    }

    private fun feedbackMessage(message: String) {
        if (chatFeedback) modMessage(message)
    }
}