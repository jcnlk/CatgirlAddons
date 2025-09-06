package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.*

/**
 * Retrieves the block ID at the specified `BlockPos` in the Minecraft world.
 *
 * @param blockPos The position in the world to query for the block ID.
 * @return The block ID as an `Int`, or `null` if the block at the given position is not present.
 */
fun getBlockIdAt(blockPos: BlockPos): Int? {
    return Block.getIdFromBlock(getBlockStateAt(blockPos).block ?: return null)
}

/**
 * Checks if the block at the specified `BlockPos` is considered "air" in the Minecraft world.
 *
 * @param blockPos The position in the world to query.
 * @return `true` if the block at the given position is air, `false` otherwise.
 */
fun isAir(blockPos: BlockPos): Boolean =
    getBlockAt(blockPos) == Blocks.air

/**
 * Checks if the block at the specified `BlockPos` is a gold block in the Minecraft world.
 *
 * @param blockPos The position in the world to query.
 * @return `true` if the block at the given position is a gold block, `false` otherwise.
 */
fun isGold(blockPos: BlockPos): Boolean =
    getBlockAt(blockPos) == Blocks.gold_block

/**
 * Retrieves the block at the specified `BlockPos` in the Minecraft world.
 *
 * @param pos The position in the world to query for the block.
 * @return The block at the given position, or `Blocks.air` if the block is not present.
 */
fun getBlockAt(pos: BlockPos): Block =
    mc.theWorld?.chunkProvider?.provideChunk(pos.x shr 4, pos.z shr 4)?.getBlock(pos) ?: Blocks.air

/**
 * Retrieves the block state at the specified `BlockPos` in the Minecraft world.
 *
 * @param pos The position in the world to query for the block state.
 * @return The block state at the given position, or the default state of `Blocks.air` if the block is not present.
 */
fun getBlockStateAt(pos: BlockPos): IBlockState =
    mc.theWorld?.chunkProvider?.provideChunk(pos.x shr 4, pos.z shr 4)?.getBlockState(pos) ?: Blocks.air.defaultState

object BlockUtils {
    fun collisionRayTrace(pos: BlockPos?, aabb: AxisAlignedBB, start: Vec3, end: Vec3): MovingObjectPosition? {
        var start = start
        var end = end
        start = start.subtract(Vec3(pos))
        end = end.subtract(Vec3(pos))
        var vec3 = start.getIntermediateWithXValue(end, aabb.minX)
        var vec31 = start.getIntermediateWithXValue(end, aabb.maxX)
        var vec32 = start.getIntermediateWithYValue(end, aabb.minY)
        var vec33 = start.getIntermediateWithYValue(end, aabb.maxY)
        var vec34 = start.getIntermediateWithZValue(end, aabb.minZ)
        var vec35 = start.getIntermediateWithZValue(end, aabb.maxZ)

        if (isVecOutsideYZBounds(vec3, aabb.minY, aabb.minZ, aabb.maxY, aabb.maxZ)) vec3 = null
        if (isVecOutsideYZBounds(vec31, aabb.minY, aabb.minZ, aabb.maxY, aabb.maxZ)) vec31 = null
        if (isVecOutsideXZBounds(vec32, aabb.minX, aabb.minZ, aabb.maxX, aabb.maxZ)) vec32 = null
        if (isVecOutsideXZBounds(vec33, aabb.minX, aabb.minZ, aabb.maxX, aabb.maxZ)) vec33 = null
        if (isVecOutsideXYBounds(vec34, aabb.minX, aabb.minY, aabb.maxX, aabb.maxY)) vec34 = null
        if (isVecOutsideXYBounds(vec35, aabb.minX, aabb.minY, aabb.maxX, aabb.maxY)) vec35 = null

        var vec36: Vec3? = null
        if (vec3 != null) vec36 = vec3
        if (vec31 != null && (vec36 == null || start.squareDistanceTo(vec31) < start.squareDistanceTo(vec36))) vec36 =
            vec31
        if (vec32 != null && (vec36 == null || start.squareDistanceTo(vec32) < start.squareDistanceTo(vec36))) vec36 =
            vec32
        if (vec33 != null && (vec36 == null || start.squareDistanceTo(vec33) < start.squareDistanceTo(vec36))) vec36 =
            vec33
        if (vec34 != null && (vec36 == null || start.squareDistanceTo(vec34) < start.squareDistanceTo(vec36))) vec36 =
            vec34
        if (vec35 != null && (vec36 == null || start.squareDistanceTo(vec35) < start.squareDistanceTo(vec36))) vec36 =
            vec35

        if (vec36 == null) return null
        else {
            var enumfacing: EnumFacing? = null
            if (vec36 === vec3) enumfacing = EnumFacing.WEST
            if (vec36 === vec31) enumfacing = EnumFacing.EAST
            if (vec36 === vec32) enumfacing = EnumFacing.DOWN
            if (vec36 === vec33) enumfacing = EnumFacing.UP
            if (vec36 === vec34) enumfacing = EnumFacing.NORTH
            if (vec36 === vec35) enumfacing = EnumFacing.SOUTH
            return MovingObjectPosition(vec36, enumfacing, pos)
        }
    }

    private fun isVecOutsideYZBounds(point: Vec3?, minY: Double, minZ: Double, maxY: Double, maxZ: Double): Boolean {
        return point == null || !(point.yCoord >= minY) || !(point.yCoord <= maxY) || !(point.zCoord >= minZ) || !(point.zCoord <= maxZ)
    }

    private fun isVecOutsideXZBounds(point: Vec3?, minX: Double, minZ: Double, maxX: Double, maxZ: Double): Boolean {
        return point == null || !(point.xCoord >= minX) || !(point.xCoord <= maxX) || !(point.zCoord >= minZ) || !(point.zCoord <= maxZ)
    }

    private fun isVecOutsideXYBounds(point: Vec3?, minX: Double, minY: Double, maxX: Double, maxY: Double): Boolean {
        return point == null || !(point.xCoord >= minX) || !(point.xCoord <= maxX) || !(point.yCoord >= minY) || !(point.yCoord <= maxY)
    }
}