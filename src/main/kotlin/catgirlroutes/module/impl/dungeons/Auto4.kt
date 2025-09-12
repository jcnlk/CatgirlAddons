package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.ChatPacket
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.*
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.MovementUtils.clearBlocks
import catgirlroutes.utils.MovementUtils.moveToBlock
import catgirlroutes.utils.MovementUtils.movementKeysDown
import catgirlroutes.utils.MovementUtils.targetBlocks
import catgirlroutes.utils.PacketUtils.sendPacket
import catgirlroutes.utils.PlayerUtils.airClick
import catgirlroutes.utils.PlayerUtils.isHolding
import catgirlroutes.utils.*
import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posY
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.dungeon.DungeonClass
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonTeammatesNoSelf
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.dungeon.LeapUtils.leap
import catgirlroutes.utils.render.WorldRenderUtils.drawCustomSizedBoxAt
import catgirlroutes.utils.render.WorldRenderUtils.drawSquare
import catgirlroutes.utils.rotation.RotationUtils.getYawAndPitch
import catgirlroutes.utils.rotation.RotationUtils.snapTo
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraft.util.StringUtils
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

object Auto4: Module(
    "Auto 4",
    Category.DUNGEON,
    "Automatically does fourth dev.",
    TagType.WHIP
){

    private val autoPlate by BooleanSetting("Auto plate", "Automatically puts the player on device start.")

    private val aimSnap by BooleanSetting("Aim snapping", "Instantly snaps instead of smooth rotations.")

    private val statusMessages by BooleanSetting("Status messages", "Sends status messages in chat.")

    private val deviceTextOn by StringSetting("On text", "Display text when on device.")
    private val deviceTextOff by StringSetting("Off text", "Display text when off device.")

    private var autoLeap by BooleanSetting("Auto leap", "Automatically leaps when the device is finished.")
    private var leapMode by SelectorSetting("Leap mode", "Name", arrayListOf("Name", "Class"), "Leap mode for Auto leap.").withDependency { autoLeap }

    private var teamList by ListSetting("Teammates", mutableListOf("None"))
    private var devLeap by SelectorSetting("Leap name", "None", ArrayList(teamList), "Player name to leap to after the device is finished.").withDependency { leapMode.selected == "Name" && autoLeap }
    private var update by ActionSetting("Update.", "Updates teammates names (currently works in dungeon only)") { updateTeammates() }.withDependency { leapMode.selected == "Name" && autoLeap }

    private var devLeapClass by SelectorSetting("Leap class", "Mage", arrayListOf("Mage", "Bers", "Arch", "Tank", "Heal"), "Class name to leap to after the device is finished.").withDependency { leapMode.selected == "Class" && autoLeap }

    private val forceTerm by BooleanSetting("Force term", "Makes the mod think the player is using a terminator.")

    init {
        devLeap.options = teamList
    }

    private fun updateTeammates() {
        if (dungeonTeammatesNoSelf.isEmpty()) return
        val teammates = dungeonTeammatesNoSelf.map { it.name } + "None"
        teamList = teammates.toMutableList()
        devLeap.options = teammates
    }

    @SubscribeEvent
    fun onChat(event: ChatPacket) {
        if (!inDungeons || !inBoss || !onDev()) return
        val message = StringUtils.stripControlCodes(event.message)
        if (message.matches(Regex("(\\w+) completed a device! \\((.*?)\\)"))) {
            if (leapMode.selected == "Name" && autoLeap) leap(devLeap.selected)
            else if (leapMode.selected == "Class" && autoLeap) leap(classEnumMapping[devLeapClass.index])
            if (statusMessages) ChatUtils.commandAny("/pc [CGA] I4 completed")
        } else if (message.matches(Regex("☠ (\\w{1,16}) .* and became a ghost\\."))) {
            ChatUtils.commandAny("/pc [CGA] I4 not completed")
        }
    }

    data class BowStats (
        var isTerm: Boolean,
        var cooldown: Int
    )

    private val devBlocks = listOf(
        BlockPos(64, 126, 50),
        BlockPos(66, 126, 50),
        BlockPos(68, 126, 50),
        BlockPos(64, 128, 50),
        BlockPos(66, 128, 50),
        BlockPos(68, 128, 50),
        BlockPos(64, 130, 50),
        BlockPos(66, 130, 50),
        BlockPos(68, 130, 50)
    )

    private val classEnumMapping = arrayListOf(DungeonClass.Mage, DungeonClass.Berserk, DungeonClass.Archer, DungeonClass.Tank, DungeonClass.Healer, DungeonClass.Unknown)

    private var doneBlocks = mutableListOf<Vec3>()
    private var cancelNext = false
    private var currentBlock: BlockPos? = null

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!inDungeons) return
        if (platePressed()) {
            drawSquare(63.5, 127.01, 35.5, 3.0, 3.0, Color.GREEN, phase = false)
            if (currentBlock != null && !doneBlocks.contains(currentBlock!!.toVec3())) doneBlocks.add(currentBlock!!.toVec3())
            doneBlocks.forEach {block ->
                drawCustomSizedBoxAt(block.xCoord, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.RED, filled = true)
            }
            if (currentBlock == null) return
            val block = currentBlock!!.toVec3()
            drawCustomSizedBoxAt(block.xCoord, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.GREEN, filled = true)
            if (forceTerm) {
                if (block.xCoord == 64.0 && !doneBlocks.contains(Vec3(block.xCoord + 2, block.yCoord, block.zCoord))) drawCustomSizedBoxAt(block.xCoord + 2, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.ORANGE, filled = true)
                else if (block.xCoord != 64.0 && !doneBlocks.contains(Vec3(block.xCoord - 2, block.yCoord, block.zCoord))) drawCustomSizedBoxAt(block.xCoord - 2, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.ORANGE, filled = true)
            }
        }
        else if (onDev())  {
            drawSquare(63.5, 127.01, 35.5, 3.0, 3.0, Color.ORANGE, phase = false)
        }
        else {
            doneBlocks = mutableListOf()
            currentBlock = null
            drawSquare(63.5, 127.01, 35.5, 3.0, 3.0, Color.RED, phase = false)
        }
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !onDev() || mc.ingameGUI == null || !inDungeons) return

        var text = "$deviceTextOff ${doneBlocks.size}/9"
        var color = 0xFA5F55
        if (platePressed()) {
            text = "$deviceTextOn ${doneBlocks.size}/9"
            color = 0x00FF7F
        }
        val scale = 1.5
        renderText(text, scale, color)
    }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Ava is such a good girl
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || !inDungeons) return
        if (!onDev() || !autoPlate) return
        if (mc.thePlayer.posX == 63.5 && mc.thePlayer.posZ == 35.5) return
        if (movementKeysDown()) {
            clearBlocks()
            return
        }
        moveToBlock(63.5, 35.5)
    }

    private fun shootBlock() {
        if (currentBlock == null || !onDev() || !inDungeons) return
        val coords = aimCoords(currentBlock!!)
        val rotation = getYawAndPitch(coords.xCoord, coords.yCoord, coords.zCoord)
        if (aimSnap) snapTo(rotation.first, rotation.second)
        sendPacket(C05PacketPlayerLook(rotation.first, rotation.second, mc.thePlayer.onGround))
        cancelNext = true
        airClick()
    }

    private fun aimCoords(coords: BlockPos): Vec3 {
        val offsetMap = mapOf(
            68 to -0.7,
            66 to -0.5,
            64 to 1.5
        )

        var xOffset = offsetMap[coords.x]
        if (!forceTerm) xOffset = 0.5

        return Vec3(coords.x + xOffset!!, coords.y + 1.1, coords.z.toDouble())
    }

    private fun getBow(): BowStats { // ???
        val isTerm = isHolding("TERMINATOR")
        var shotSpeed = 300
        val lore = mc.thePlayer.heldItem.lore
        lore.forEach{line ->
            val regex = Regex("Shot Cooldown: (\\d\\.?\\d+)s")
            val regexMatch = regex.find(removeFormatting(line))
            if (regexMatch != null) shotSpeed = regexMatch.value.toInt() * 1000
        }
        return BowStats(isTerm, shotSpeed)
    }

    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) {
        if (event.packet is S23PacketBlockChange) {
            if (!devBlocks.contains(event.packet.blockPosition) || event.packet.blockState.block != Blocks.emerald_block) return
            currentBlock = event.packet.blockPosition
            shootBlock()
            return
        } else if (event.packet is S22PacketMultiBlockChange) {
            event.packet.changedBlocks.forEach {block ->
                if (!devBlocks.contains(block.pos) || block.blockState.block != Blocks.emerald_block) return@forEach
                currentBlock = block.pos
                shootBlock()
                return
            }
        } else if (event.packet is S08PacketPlayerPosLook && autoPlate && targetBlocks.isNotEmpty() && targetBlocks.first() == Vec3(63.5, 0.0, 35.5)) {
            clearBlocks()
        } else if (event.packet is C03PacketPlayer && cancelNext) {
            cancelNext = false
            event.isCanceled = true
        }
    }

    private fun onDev(): Boolean {
        return posX in 62.0..65.0 && posY == 127.0 && posZ in 34.0..37.0
    }

    private fun platePressed(): Boolean {
        val plate = mc.theWorld.getBlockState(BlockPos(63, 127, 35))
        return plate.block === Blocks.light_weighted_pressure_plate && plate.block.getMetaFromState(plate) > 0
    }
}