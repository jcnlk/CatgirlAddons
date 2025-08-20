package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.SelectorSetting
import catgirlroutes.utils.BlockUtils.collisionRayTrace
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.PacketUtils
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoom
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoomName
import catgirlroutes.utils.dungeon.DungeonUtils.getRealCoords
import catgirlroutes.utils.dungeon.DungeonUtils.getRelativeCoords
import catgirlroutes.utils.dungeon.PuzzleStatus
import catgirlroutes.utils.equalsOneOf
import catgirlroutes.utils.isAir
import net.minecraft.block.BlockLever
import net.minecraft.block.BlockLever.EnumOrientation
import net.minecraft.block.BlockSkull
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.*
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.*
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.util.*

//Ty soshimishimi for inspiration https://github.com/soshimee/secretguide
object SecretAura : Module( // TODO: RECODE
    "Secret Aura",
    Category.DUNGEON,
    "Automatically clicks secrets and levers."
){
    private val auraRange by NumberSetting("Range", 6.2, 2.1, 6.5, 0.1, "Maximum range for secret aura.")
    private val auraSkullRange by NumberSetting("Skull range", 4.7, 2.1, 4.7, 0.1, "Maximum range for secret aura when clicking skulls.")

    private val swapOn by SelectorSetting("Swap on", "Skulls", arrayListOf("None", "Skulls", "All"), "Makes secret aura swap")
    private val swapBack by BooleanSetting("Swap back", true, "Makes secret aura swap back to previous item after swapping.").withDependency { swapOn.index >= 1 }
    private val swapSlot by NumberSetting("Swap item slot", 1.0, 1.0, 9.0, 1.0, "Slot for secret aura to swap to.").withDependency { swapOn.index >= 1 }

    private val swing by BooleanSetting("Swing hand", "Makes secret aura swing hand on click.")
    private val auraClose by BooleanSetting("Auto close", "Makes secret aura auto close chests.")
    private val onlyDungeons by BooleanSetting("Only in dungeons", true, "Makes secret aura only work in dungeons.")

    private val clearButton by ActionSetting("Clear blocks", "Manually clears all tracked blocks and cooldowns.") { 
        clearBlocks()
        modMessage("Secret Aura: Blocks cleared manually!")
    }

    private val blocksDone: MutableList<BlockPos> = LinkedList()
    private val blocksCooldown: MutableMap<BlockPos, Long> = HashMap()
    private var redstoneKey = false
    private var prevSlot = -1

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!DungeonUtils.inDungeons && onlyDungeons) return
        if (mc.thePlayer == null || mc.theWorld == null) return
        val eyePos = mc.thePlayer.getPositionEyes(0f)
        val blockPos1 = BlockPos(eyePos.xCoord - auraRange, eyePos.yCoord - auraRange, eyePos.zCoord - auraRange)
        val blockPos2 = BlockPos(eyePos.xCoord + auraRange, eyePos.yCoord + auraRange, eyePos.zCoord + auraRange)
        val blocks = BlockPos.getAllInBox(blockPos1, blockPos2)
        val time: Long = Date().time
        for (block in blocks) {
            if (blocksDone.contains(block)) continue
            if (blocksCooldown.containsKey(block) && blocksCooldown[block]!! + 500 > time) continue

            val blockState = mc.theWorld.getBlockState(block)
            if (blockState.block === Blocks.chest || blockState.block === Blocks.trapped_chest) {
                if (currentRoomName == "Three Weirdos") continue
                if (currentRoomName == "Ice Path" && !isAir(currentRoom!!.getRealCoords(BlockPos(15, 68, 25)))) continue
                if (currentRoomName == "Water Board" && (15..19).any { !isAir(currentRoom!!.getRealCoords(BlockPos(15, 57, it))) }) continue
                if (currentRoomName.equalsOneOf("Higher Blaze", "Lower Blaze") &&
                    DungeonUtils.puzzles.find { it.name == "Higher Or Lower" }?.status != PuzzleStatus.Completed &&
                    currentRoom!!.getRelativeCoords(block).x != 2) continue



                val centerPos = Vec3(block.x + 0.5, block.y + 0.4375, block.z + 0.5)

                if (eyePos.distanceTo(Vec3(block)) <= auraRange) {
                    val movingObjectPosition: MovingObjectPosition = collisionRayTrace(
                        block,
                        AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375),
                        eyePos,
                        centerPos
                    ) ?: continue

                    if (swapOn.index == 2 && mc.thePlayer.inventory.currentItem != swapSlot.toInt() - 1) {
                        prevSlot = mc.thePlayer.inventory.currentItem
                        mc.thePlayer.inventory.currentItem = swapSlot.toInt() - 1
                        return
                    }

                    PacketUtils.sendPacket(
                        C08PacketPlayerBlockPlacement(
                            block,
                            movingObjectPosition.sideHit.index,
                            mc.thePlayer.heldItem,
                            movingObjectPosition.hitVec.xCoord.toFloat(),
                            movingObjectPosition.hitVec.yCoord.toFloat(),
                            movingObjectPosition.hitVec.zCoord.toFloat()
                        )
                    )

                    if (!mc.thePlayer.isSneaking && swing) mc.thePlayer.swingItem()
                    blocksCooldown[block] = Date().time // System.currentTimeMillis()??
                    return
                }
            } else if (blockState.block === Blocks.lever) {
                if (currentRoomName == "Water Board") continue
                if (currentRoomName == "Tic Tac Toe") continue

                val orientation = blockState.properties[BlockLever.FACING] as EnumOrientation
                val aabb = when(orientation) {
                    EnumOrientation.EAST -> AxisAlignedBB(0.0, 0.2, 0.315, 0.375, 0.8, 0.6875)
                    EnumOrientation.WEST -> AxisAlignedBB(0.625, 0.2, 0.315, 1.0, 0.8, 0.6875)
                    EnumOrientation.SOUTH -> AxisAlignedBB(0.3125, 0.2, 0.0, 0.6875, 0.8, 0.375)
                    EnumOrientation.NORTH -> AxisAlignedBB(0.3125, 0.2, 0.625, 0.6875, 0.8, 1.0)
                    EnumOrientation.UP_Z, EnumOrientation.UP_X -> AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.6, 0.75)
                    EnumOrientation.DOWN_X, EnumOrientation.DOWN_Z -> AxisAlignedBB(0.25, 0.4, 0.25, 0.75, 1.0, 0.75)
                    else -> continue
                }

                val centerPos = Vec3(block).addVector(
                    (aabb.minX + aabb.maxX) / 2,
                    (aabb.minY + aabb.maxY) / 2,
                    (aabb.minZ + aabb.maxZ) / 2
                )

                if (eyePos.distanceTo(Vec3(block)) <= auraRange) {

                    val movingObjectPosition: MovingObjectPosition = collisionRayTrace(block, aabb, eyePos, centerPos) ?: continue

                    if (swapOn.index == 2 && mc.thePlayer.inventory.currentItem != swapSlot.toInt() - 1) {
                        prevSlot = mc.thePlayer.inventory.currentItem
                        mc.thePlayer.inventory.currentItem = swapSlot.toInt() - 1
                        return
                    }

                    PacketUtils.sendPacket(
                        C08PacketPlayerBlockPlacement(
                            block,
                            movingObjectPosition.sideHit.index,
                            mc.thePlayer.heldItem,
                            movingObjectPosition.hitVec.xCoord.toFloat(),
                            movingObjectPosition.hitVec.yCoord.toFloat(),
                            movingObjectPosition.hitVec.zCoord.toFloat()
                        )
                    )

                    if (!mc.thePlayer.isSneaking && swing) mc.thePlayer.swingItem()

                    blocksCooldown[block] = Date().time // System.currentTimeMillis()??
                    return
                }
            } else if (blockState.block === Blocks.skull) {

                val tileEntity: TileEntity = mc.theWorld.getTileEntity(block) as? TileEntitySkull ?: continue // I think we have a util for this, but I cba
                val profile = (tileEntity as TileEntitySkull).playerProfile ?: continue
                val profileId = profile.id.toString()

                if (!Objects.equals(profileId, "e0f3e929-869e-3dca-9504-54c666ee6f23")) {
                    if (!Objects.equals(profileId, "fed95410-aba1-39df-9b95-1d4f361eb66e")) continue
                    else if (
                        mc.theWorld.getBlockState(block.down()).block === Blocks.redstone_block ||
                        mc.theWorld.getBlockState(block.north()).block === Blocks.redstone_block ||
                        mc.theWorld.getBlockState(block.south()).block === Blocks.redstone_block ||
                        mc.theWorld.getBlockState(block.west()).block === Blocks.redstone_block ||
                        mc.theWorld.getBlockState(block.east()).block === Blocks.redstone_block
                    ) {
                        redstoneKey = false
                        blocksDone.add(block)
                        continue
                    }
                }

                val aabb = when (blockState.properties[BlockSkull.FACING] as EnumFacing) {
                    EnumFacing.NORTH -> AxisAlignedBB(0.25, 0.25, 0.5, 0.75, 0.75, 1.0)
                    EnumFacing.SOUTH -> AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 0.5)
                    EnumFacing.WEST -> AxisAlignedBB(0.5, 0.25, 0.25, 1.0, 0.75, 0.75)
                    EnumFacing.EAST -> AxisAlignedBB(0.0, 0.25, 0.25, 0.5, 0.75, 0.75)
                    else -> AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.5, 0.75)
                }

                val centerPos = Vec3(block).addVector(
                    (aabb.minX + aabb.maxX) / 2,
                    (aabb.minY + aabb.maxY) / 2,
                    (aabb.minZ + aabb.maxZ) / 2
                )

                if (eyePos.distanceTo(centerPos) <= auraSkullRange) {
                    val movingObjectPosition: MovingObjectPosition = collisionRayTrace(block, aabb, eyePos, centerPos) ?: continue

                    if (swapOn.index >= 1 && mc.thePlayer.inventory.currentItem != swapSlot.toInt() - 1) {
                        prevSlot = mc.thePlayer.inventory.currentItem
                        mc.thePlayer.inventory.currentItem = swapSlot.toInt() - 1
                        return
                    }

                    PacketUtils.sendPacket(
                        C08PacketPlayerBlockPlacement(
                            block,
                            movingObjectPosition.sideHit.index,
                            mc.thePlayer.heldItem,
                            movingObjectPosition.hitVec.xCoord.toFloat(),
                            movingObjectPosition.hitVec.yCoord.toFloat(),
                            movingObjectPosition.hitVec.zCoord.toFloat()
                        )
                    )

                    blocksCooldown[block] = Date().time // // System.currentTimeMillis()??
                    return
                }
            } else if (blockState.block === Blocks.redstone_block) {
                if (!redstoneKey) continue
                if (mc.thePlayer.posX < -200 || mc.thePlayer.posZ < -200 || mc.thePlayer.posX > 0 || mc.thePlayer.posZ > 0) continue

                if (mc.theWorld.getBlockState(block.up()).block === Blocks.skull ||
                    mc.theWorld.getBlockState(block.north()).block === Blocks.skull ||
                    mc.theWorld.getBlockState(block.south()).block === Blocks.skull ||
                    mc.theWorld.getBlockState(block.west()).block === Blocks.skull ||
                    mc.theWorld.getBlockState(block.east()).block === Blocks.skull
                ) {
                    redstoneKey = false
                    blocksDone.add(block)
                    continue
                }

                val centerPos = Vec3(block).addVector(0.5, 0.5, 0.5)
                if (eyePos.distanceTo(Vec3(block)) <= auraRange) {
                    val movingObjectPosition: MovingObjectPosition = collisionRayTrace(
                        block,
                        AxisAlignedBB(0.0,0.0,0.0, 1.0,1.0,1.0),
                        eyePos,
                        centerPos
                    ) ?: continue
                    if (movingObjectPosition.sideHit == EnumFacing.DOWN) continue

                    if (swapOn.index >= 1 && mc.thePlayer.inventory.currentItem != swapSlot.toInt() - 1) {
                        prevSlot = mc.thePlayer.inventory.currentItem
                        mc.thePlayer.inventory.currentItem = swapSlot.toInt() - 1
                        return
                    }

                    PacketUtils.sendPacket(
                        C08PacketPlayerBlockPlacement(
                            block,
                            movingObjectPosition.sideHit.index,
                            mc.thePlayer.heldItem,
                            movingObjectPosition.hitVec.xCoord.toFloat(),
                            movingObjectPosition.hitVec.yCoord.toFloat(),
                            movingObjectPosition.hitVec.zCoord.toFloat()
                        )
                    )

                    blocksCooldown[block] = Date().time // // System.currentTimeMillis()??
                    return
                }
            }
        }
        if (swapBack && prevSlot >= 0) {
            mc.thePlayer.inventory.currentItem = prevSlot
        }
        prevSlot = -1
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load?) {
        clearBlocks()
        redstoneKey = false
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packet is S24PacketBlockAction) {
            val packet = event.packet
            if (packet.blockType === Blocks.chest) {
                blocksDone.add(packet.blockPosition)
            }
        } else if (event.packet is S23PacketBlockChange) {
            val packet = event.packet
            val mc = Minecraft.getMinecraft()
            val world = mc.theWorld
            val blockPos = packet.blockPosition
            val blockState = world.getBlockState(blockPos)
            if (blockState.block === Blocks.lever) {
                blocksDone.add(blockPos)
            } else if (blockState.block === Blocks.skull) {
                if (packet.getBlockState().block !== Blocks.air) return
                val tileEntity = world.getTileEntity(blockPos) as? TileEntitySkull ?: return
                val profile = tileEntity.playerProfile ?: return
                val profileId = profile.id.toString()
                if (profileId == "fed95410-aba1-39df-9b95-1d4f361eb66e") {
                    redstoneKey = true
                }
            } else if (blockState.block === Blocks.redstone_block) {
                blocksDone.add(blockPos)
            }
        } else if (event.packet is S22PacketMultiBlockChange) {
            val mc = Minecraft.getMinecraft()
            val world = mc.theWorld
            for (changedBlock in event.packet.changedBlocks) {
                val blockPos = changedBlock.pos
                val blockState = world.getBlockState(blockPos)
                if (blockState.block === Blocks.lever) {
                    blocksDone.add(blockPos)
                } else if (blockState.block === Blocks.skull) {
                    if (changedBlock.blockState.block !== Blocks.air) return
                    val tileEntity = world.getTileEntity(blockPos) as? TileEntitySkull ?: return
                    val profile = tileEntity.playerProfile ?: return
                    val profileId = profile.id.toString()
                    if (profileId == "fed95410-aba1-39df-9b95-1d4f361eb66e") {
                        redstoneKey = true
                    }
                } else if (blockState.block === Blocks.redstone_block) {
                    blocksDone.add(blockPos)
                }
            }
        } else if (event.packet is S04PacketEntityEquipment) {
            val packet = event.packet
            val mc = Minecraft.getMinecraft()
            val world = mc.theWorld
            val entity: Entity = world.getEntityByID(packet.entityID) as? EntityArmorStand ?: return
            if (packet.equipmentSlot != 4) return
            val itemStack = packet.itemStack ?: return
            if (itemStack.item !== Items.skull) return
            if (!itemStack.hasTagCompound()) return
            val profileId = itemStack.tagCompound.getCompoundTag("SkullOwner").getString("Id")
            if (profileId != "e0f3e929-869e-3dca-9504-54c666ee6f23") return
            blocksDone.add(BlockPos(entity.posX, entity.posY + 2, entity.posZ))
        } else if (event.packet is S02PacketChat) {
            val packet = event.packet
            if (packet.type.toInt() == 2) return
            val message = packet.chatComponent.unformattedText.replace("§[0-9a-fk-or]".toRegex(), "")
            if (message == "[BOSS] Goldor: Who dares trespass into my domain?") {
                clearBlocks()
                modMessage("Blocks cleared!")
            }
        }
    }

    private var closeId: Int? = null
    @SubscribeEvent
    fun onTick2(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || closeId == null) return
        mc.netHandler.networkManager.sendPacket(C0DPacketCloseWindow(closeId!!))
        closeId = null
    }

    @SubscribeEvent
    fun onPacketReceive2(event: PacketReceiveEvent) {
        if (!auraClose || event.packet !is S2DPacketOpenWindow) return
        if (!DungeonUtils.inDungeons && onlyDungeons) return
        val packet = event.packet
        if (packet.guiId != "minecraft:chest") return
        if ((packet.windowTitle.formattedText == "Chest§r" && packet.slotCount == 27) || (packet.windowTitle.formattedText == "Large Chest§r" && packet.slotCount == 54)) {
            closeId = packet.windowId
            event.isCanceled = true
        }
    }

    fun clearBlocks() {
        blocksDone.clear()
        blocksCooldown.clear()
    }
}
