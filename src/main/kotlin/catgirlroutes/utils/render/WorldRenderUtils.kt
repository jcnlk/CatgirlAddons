package catgirlroutes.utils.render

import catgirlroutes.CatgirlRoutes.Companion.mc

import catgirlroutes.ui.clickgui.util.ColorUtil.toInt
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.utils.renderText
import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posY
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.addVec
import catgirlroutes.utils.fastEyeHeight
import catgirlroutes.utils.WorldToScreen
import catgirlroutes.utils.render.HUDRenderUtils.sr
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.glu.Cylinder
import org.lwjgl.util.vector.Vector3f
import java.awt.Color
import java.awt.Color.*
import kotlin.math.cos
import kotlin.math.sin

// TODO CLEAN UP
/**
 * ## A Collection of Methods for Rendering within the 3D World.
 *
 * This class provides methods for rendering shapes in the 3D in-game world.
 *
 *
 *  ### The **phase** Parameter
 * To control whether objects should be visible through walls you can use the **phase** parameter.
 * This will disable the depth test.
 *
 *
 * ### The *relocate* Parameter
 * Depending on when methods in here are called in the rendering process, coordinates may or may not be already translated by the camera position.
 * To account for this most methods have a **relocate** parameter.
 * In general, like when using the [RenderWorldLastEvent][net.minecraftforge.client.event.RenderWorldLastEvent],
 * this should be set to true for the expected behaviour.
 *
 *
 * @author Aton
 * @author Stivais
 */
object WorldRenderUtils {

    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer
    val renderManager = mc.renderManager

    /**
     * Draws a line connecting the points [start] and [finish].
     *
     * @param phase Determines whether the box should be visible through walls (disables the depth test).
     */
    fun drawLine(start: Vec3, finish: Vec3, color: Color, thickness: Float = 3f, phase: Boolean = true) {
        drawLine(start.xCoord, start.yCoord, start.zCoord, finish.xCoord, finish.yCoord, finish.zCoord, color, thickness, phase)
    }

    fun drawTracer(goal: Vec3, color: Color, thickness: Float = 3f, phase: Boolean = true) {
        drawLine(mc.thePlayer.renderVec.addVec(y = fastEyeHeight()), goal, color, thickness, phase)
    }

    /**
     * Gets the rendered x-coordinate of an entity based on its last tick and current tick positions.
     *
     * @receiver The entity for which to retrieve the rendered x-coordinate.
     * @return The rendered x-coordinate.
     */
    inline val Entity.renderX: Double
        get() = prevPosX + (posX - prevPosX ) * partialTicks

    /**
     * Gets the rendered y-coordinate of an entity based on its last tick and current tick positions.
     *
     * @receiver The entity for which to retrieve the rendered y-coordinate.
     * @return The rendered y-coordinate.
     */
    inline val Entity.renderY: Double
        get() = prevPosY + (posY - prevPosY) * partialTicks

    /**
     * Gets the rendered z-coordinate of an entity based on its last tick and current tick positions.
     *
     * @receiver The entity for which to retrieve the rendered z-coordinate.
     * @return The rendered z-coordinate.
     */
    inline val Entity.renderZ: Double
        get() = prevPosZ + (posZ - prevPosZ) * partialTicks

    /**
     * Gets the rendered position of an entity as a `Vec3`.
     *
     * @receiver The entity for which to retrieve the rendered position.
     * @return The rendered position as a `Vec3`.
     */
    inline val Entity.renderVec: Vec3
        get() = Vec3(renderX, renderY, renderZ)

    var partialTicks = 0f

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderWorld(event: RenderWorldLastEvent) {
        this.partialTicks = event.partialTicks
    }

    /**
     * Draws a line connecting the points ([x], [y], [z]) and ([x2], [y2], [z2]).
     *
     * @param phase Determines whether the box should be visible through walls (disables the depth test).
     */
    fun drawLine (x: Double, y: Double, z: Double, x2: Double, y2: Double, z2:Double, color: Color, thickness: Float = 3f, phase: Boolean = true) {
        GlStateManager.disableLighting()
        GL11.glBlendFunc(770, 771)
        GlStateManager.enableBlend()
        GL11.glLineWidth(thickness)
        if (phase) GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()
        GlStateManager.pushMatrix()

        GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        GlStateManager.color(color.red / 255f, color.green / 255f,
            color.blue / 255f, color.alpha / 255f)

        worldRenderer.pos(x, y, z).endVertex()
        worldRenderer.pos(x2, y2, z2).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    /**
     * Draws a cube outline for the block at the given [blockPos].
     *
     * This outline will be visible through walls. The depth test is disabled.
     *
     * @param relocate Translates the coordinates to account for the camera position. See [WorldRenderUtils] for more information.
     */
    fun drawBoxAtBlock (blockPos: BlockPos, color: Color, thickness: Float = 3f, relocate: Boolean = true, filled: Boolean = false) {
        drawBoxAtBlock(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), color, thickness, relocate, filled)
    }

    /**
     * Converts a BlockPos to an AxisAlignedBB with specified dimensions
     *
     * @param pos The starting BlockPos
     * @param width The width of the box (in blocks)
     * @param height The height of the box (in blocks)
     * @param depth The depth of the box (in blocks)
     * @return An AxisAlignedBB representing the specified dimensions
     */
    fun blockPosToAABB(pos: BlockPos, width: Int, height: Int, depth: Int): AxisAlignedBB {
        // Create the box starting at the BlockPos coordinates
        return AxisAlignedBB(
            pos.x.toDouble(),                 // minX
            pos.y.toDouble(),                 // minY
            pos.z.toDouble(),                 // minZ
            pos.x.toDouble() + width,         // maxX
            pos.y.toDouble() + height,        // maxY
            pos.z.toDouble() + depth          // maxZ
        )
    }

    fun vec3ToAABB(pos: Vec3, width: Int, height: Int, depth: Int): AxisAlignedBB {
        val halfWidth = width / 2.0
        val halfHeight = height / 2.0
        val halfDepth = depth / 2.0

        return AxisAlignedBB(
            pos.xCoord - halfWidth,   // minX
            pos.yCoord - halfHeight,  // minY
            pos.zCoord - halfDepth,   // minZ
            pos.xCoord + halfWidth,   // maxX
            pos.yCoord + halfHeight,  // maxY
            pos.zCoord + halfDepth    // maxZ
        )
    }


    /**
     * Draws a cube outline of size 1 starting at [x], [y], [z] which extends by 1 along the axes in positive direction.
     *
     * This outline will be visible through walls. The depth test is disabled.
     *
     * @param relocate Translates the coordinates to account for the camera position. See [WorldRenderUtils] for more information.
     */
    fun drawBoxAtBlock (x: Double, y: Double, z: Double, color: Color, thickness: Float = 3f, relocate: Boolean = true, filled: Boolean = false) {
        drawCustomSizedBoxAt(x, y, z, 1.0, 1.0, 1.0, color, thickness, true, relocate, filled)
    }

    /**
     * Draws a rectangular cuboid outline (box) around the [entity].
     *
     * This box is centered horizontally around the entity with the given [width].
     * Vertically the box is aligned with the bottom of the entities hit-box and extends upwards by [height].
     * The box can be offset from this default alignment through the use of [xOffset], [yOffset], [zOffset].
     *
     * @param phase Determines whether the box should be visible through walls (disables the depth test).
     * @param partialTicks Used for predicting the [entity]'s position so that the box smoothly moves with the entity.
     */
    fun drawBoxByEntity (entity: Entity, color: Color, width: Double, height: Double, partialTicks: Float = 0f,
                         lineWidth: Double = 2.0, phase: Boolean = false,
                         xOffset: Double = 0.0, yOffset: Double = 0.0, zOffset: Double = 0.0
    ) {
        drawBoxByEntity(entity, color, width.toFloat(), height.toFloat(), partialTicks, lineWidth.toFloat(),phase,xOffset, yOffset, zOffset)
    }

    fun drawEntityBox(entity: Entity, color: Color, fillcolor: Color, outline: Boolean, fill: Boolean, partialTicks: Float, lineWidth: Float, offset: Float = 0.0f) {
        if (!outline && !fill) return
        val renderManager = mc.renderManager
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ

        var axisAlignedBB: AxisAlignedBB
        entity.entityBoundingBox.run {
            axisAlignedBB = AxisAlignedBB(
                minX - entity.posX - offset,
                minY - entity.posY - offset,
                minZ - entity.posZ - offset,
                maxX - entity.posX + offset,
                maxY - entity.posY + offset,
                maxZ - entity.posZ + offset
            ).offset(x, y, z)
        }

        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_LIGHTING)
        glDepthMask(false)

        if (outline) {
            glLineWidth(lineWidth)
            drawOutlinedAABB(axisAlignedBB, color)
        }
        if (fill) {
            drawFilledAABB(axisAlignedBB, fillcolor)
        }

        glDepthMask(true)
        glPopAttrib()
        glPopMatrix()
    }

    fun drawOutlinedAABB(aabb: AxisAlignedBB, colour: Color, thickness: Float = 3.0f, phase: Boolean = false) {
        GlStateManager.pushMatrix()
        GlStateManager.disableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        if (phase) GlStateManager.disableDepth()
        glLineWidth(thickness)
        drawOutlinedAABB(aabb, colour)

        GlStateManager.enableDepth()
        GlStateManager.enableBlend()
        GlStateManager.enableTexture2D()
        GlStateManager.popMatrix()
    }

    private fun drawOutlinedAABB(aabb: AxisAlignedBB, color: Color) {
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()

        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()

        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()

        tessellator.draw()
    }

    fun drawFilledAABB(aabb: AxisAlignedBB, color: Color) {
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)

        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()

        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()

        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()

        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()

        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()

        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()
    }


    /**
     * Draws a rectangular cuboid outline (box) around the [entity].
     *
     * This box is centered horizontally around the entity with the given [width].
     * Vertically the box is aligned with the bottom of the entities hit-box and extends upwards by [height].
     * The box can be offset from this default alignment through the use of [xOffset], [yOffset], [zOffset].
     *
     * @param phase Determines whether the box should be visible through walls (disables the depth test).
     * @param partialTicks Used for predicting the [entity]'s position so that the box smoothly moves with the entity.
     */
    fun drawBoxByEntity (entity: Entity, color: Color, width: Float, height: Float, partialTicks: Float = 0f,
                         lineWidth: Float = 2f, phase: Boolean = false,
                         xOffset: Double = 0.0, yOffset: Double = 0.0, zOffset: Double = 0.0
    ){
        val x = entity.posX + ((entity.posX-entity.lastTickPosX)*partialTicks) + xOffset - width / 2.0
        val y = entity.posY + ((entity.posY-entity.lastTickPosY)*partialTicks) + yOffset
        val z = entity.posZ + ((entity.posZ-entity.lastTickPosZ)*partialTicks) + zOffset - width / 2.0

        drawCustomSizedBoxAt(x, y, z, width.toDouble(), height.toDouble(), width.toDouble(), color, lineWidth, phase)
    }

    fun draw2DBoxByEntity(entity: Entity, color: Color, width: Double, height: Double, partialTicks: Float = 0f, lineWidth: Double = 2.0, phase: Boolean = false, xOffset: Double = 0.0, yOffset: Double = 0.0, zOffset: Double = 0.0) { // todo remove it I think
            val x = entity.posX + ((entity.posX-entity.lastTickPosX)*partialTicks) + xOffset - width / 2.0
            val y = entity.posY + ((entity.posY-entity.lastTickPosY)*partialTicks) + yOffset
            val z = entity.posZ + ((entity.posZ-entity.lastTickPosZ)*partialTicks) + zOffset - width / 2.0


            GlStateManager.pushMatrix()

            GlStateManager.color(color.red.toFloat() / 255f, color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f, 1f)

        GL11.glTranslated(x, y - 0.2, z)
        GL11.glRotated((-mc.renderManager.playerViewY).toDouble(), 0.0, 1.0, 0.0)

        if (phase) GlStateManager.disableDepth()
        val outline = Color.black.rgb
        Gui.drawRect(-20, -1, -26, 75, outline)
        Gui.drawRect(20, -1, 26, 75, outline)
        Gui.drawRect(-20, -1, 21, 5, outline)
        Gui.drawRect(-20, 70, 21, 75, outline)
        Gui.drawRect(-21, 0, -25, 74, 1)
        Gui.drawRect(21, 0, 25, 74, 1)
        Gui.drawRect(-21, 0, 24, 4, 1)
        Gui.drawRect(-21, 71, 25, 74, 1)

        GlStateManager.enableDepth()

        GlStateManager.popMatrix()
    }

    fun draw2DBoxByEntity(entity: Entity, color: Color, partialTicks: Float = 0f, lineWidth: Float = 2.0f, phase: Boolean = false) {
        val mvMatrix = WorldToScreen.getMatrix(GL11.GL_MODELVIEW_MATRIX)
        val projectionMatrix = WorldToScreen.getMatrix(GL11.GL_PROJECTION_MATRIX)

        GlStateManager.pushAttrib()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        if (phase) GlStateManager.disableDepth()

        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glOrtho(0.0, mc.displayWidth.toDouble(), mc.displayHeight.toDouble(), 0.0, -1.0, 1.0)

        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(true)
        GL11.glLineWidth(lineWidth)

        val bb = entity.entityBoundingBox
            .offset(-entity.posX, -entity.posY, -entity.posZ)
            .offset(
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
            )
            .offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)

        GlStateManager.color(
            color.red.toFloat() / 255f,
            color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f,
            color.alpha.toFloat() / 255f
        )

        val boxVertices = arrayOf(
            doubleArrayOf(bb.minX, bb.minY, bb.minZ),
            doubleArrayOf(bb.minX, bb.maxY, bb.minZ),
            doubleArrayOf(bb.maxX, bb.maxY, bb.minZ),
            doubleArrayOf(bb.maxX, bb.minY, bb.minZ),
            doubleArrayOf(bb.minX, bb.minY, bb.maxZ),
            doubleArrayOf(bb.minX, bb.maxY, bb.maxZ),
            doubleArrayOf(bb.maxX, bb.maxY, bb.maxZ),
            doubleArrayOf(bb.maxX, bb.minY, bb.maxZ)
        )

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -1.0f
        var maxY = -1.0f

        for (boxVertex in boxVertices) {
            val screenPos = WorldToScreen.worldToScreen(
                Vector3f(boxVertex[0].toFloat(), boxVertex[1].toFloat(), boxVertex[2].toFloat()),
                mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight
            )
            if (screenPos != null) {
                minX = minOf(screenPos.x, minX)
                minY = minOf(screenPos.y, minY)
                maxX = maxOf(screenPos.x, maxX)
                maxY = maxOf(screenPos.y, maxY)
            }
        }

        if (minX > 0.0f || minY > 0.0f || maxX <= mc.displayWidth || maxY <= mc.displayWidth) {
            GL11.glBegin(GL11.GL_LINE_LOOP)
            GL11.glVertex2f(minX, minY)
            GL11.glVertex2f(minX, maxY)
            GL11.glVertex2f(maxX, maxY)
            GL11.glVertex2f(maxX, minY)
            GL11.glEnd()
        }

        GlStateManager.enableDepth()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPopMatrix()
        GlStateManager.popAttrib()
    }

    private fun WorldRenderer.addVertex(x: Double, y: Double, z: Double, nx: Float, ny: Float, nz: Float) {
        pos(x, y, z).normal(nx, ny, nz).endVertex()
    }

    fun drawBlock(blockPos: BlockPos, colour: Color, thickness: Float = 3f, phase: Boolean = true, filled: Boolean = false) { // alpha no workie for some reason
        val viewerPosX = renderManager.viewerPosX
        val viewerPosY = renderManager.viewerPosY
        val viewerPosZ = renderManager.viewerPosZ

        val block = mc.theWorld.getBlockState(blockPos)
        val blockAABB = block.block.getSelectedBoundingBox(mc.theWorld, blockPos)

        val aabb = AxisAlignedBB(
            blockAABB.minX - viewerPosX,
            blockAABB.minY - viewerPosY,
            blockAABB.minZ - viewerPosZ,
            blockAABB.maxX - viewerPosX,
            blockAABB.maxY - viewerPosY,
            blockAABB.maxZ - viewerPosZ
        )
        GlStateManager.pushMatrix()

        GlStateManager.disableBlend()
        GlStateManager.disableTexture2D()
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        if (phase) GlStateManager.disableDepth()
        if (filled) {
            drawFilledAABB(aabb, colour)
        } else {
            glLineWidth(thickness)
            drawOutlinedAABB(aabb, colour)
        }
        if (phase) GlStateManager.enableDepth()

        GlStateManager.enableBlend()
        GlStateManager.enableTexture2D()
        GlStateManager.popMatrix()
    }

    fun drawCustomSizedBoxAt(x: Double, y: Double, z: Double, xWidth: Double, yHeight: Double, zWidth: Double, color: Color, thickness: Float = 3f, phase: Boolean = true, relocate: Boolean = true, filled: Boolean = false) {
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(thickness)
        if (phase) GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()

        GlStateManager.pushMatrix()

        if (relocate) GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)

        GlStateManager.color(color.red.toFloat() / 255f, color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f, color.alpha.toFloat() / 255f)

        if (filled) {
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_NORMAL)
            val minX = x
            val minY = y
            val minZ = z
            val maxX = x + xWidth
            val maxY = y + yHeight
            val maxZ = z + zWidth
            // Front face
            worldRenderer.addVertex(minX, maxY, minZ, 0f, 0f, -1f)
            worldRenderer.addVertex(maxX, maxY, minZ, 0f, 0f, -1f)
            worldRenderer.addVertex(maxX, minY, minZ, 0f, 0f, -1f)
            worldRenderer.addVertex(minX, minY, minZ, 0f, 0f, -1f)

            // Back face
            worldRenderer.addVertex(minX, minY, maxZ, 0f, 0f, 1f)
            worldRenderer.addVertex(maxX, minY, maxZ, 0f, 0f, 1f)
            worldRenderer.addVertex(maxX, maxY, maxZ, 0f, 0f, 1f)
            worldRenderer.addVertex(minX, maxY, maxZ, 0f, 0f, 1f)

            // Bottom face
            worldRenderer.addVertex(minX, minY, minZ, 0f, -1f, 0f)
            worldRenderer.addVertex(maxX, minY, minZ, 0f, -1f, 0f)
            worldRenderer.addVertex(maxX, minY, maxZ, 0f, -1f, 0f)
            worldRenderer.addVertex(minX, minY, maxZ, 0f, -1f, 0f)

            // Top face
            worldRenderer.addVertex(minX, maxY, maxZ, 0f, 1f, 0f)
            worldRenderer.addVertex(maxX, maxY, maxZ, 0f, 1f, 0f)
            worldRenderer.addVertex(maxX, maxY, minZ, 0f, 1f, 0f)
            worldRenderer.addVertex(minX, maxY, minZ, 0f, 1f, 0f)

            // Left face
            worldRenderer.addVertex(minX, minY, maxZ, -1f, 0f, 0f)
            worldRenderer.addVertex(minX, maxY, maxZ, -1f, 0f, 0f)
            worldRenderer.addVertex(minX, maxY, minZ, -1f, 0f, 0f)
            worldRenderer.addVertex(minX, minY, minZ, -1f, 0f, 0f)

            // Right face
            worldRenderer.addVertex(maxX, minY, minZ, 1f, 0f, 0f)
            worldRenderer.addVertex(maxX, maxY, minZ, 1f, 0f, 0f)
            worldRenderer.addVertex(maxX, maxY, maxZ, 1f, 0f, 0f)
            worldRenderer.addVertex(maxX, minY, maxZ, 1f, 0f, 0f)
        } else {
            worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)

            worldRenderer.pos(x+xWidth,y+yHeight,z+zWidth).endVertex()
            worldRenderer.pos(x+xWidth,y+yHeight,z).endVertex()
            worldRenderer.pos(x,y+yHeight,z).endVertex()
            worldRenderer.pos(x,y+yHeight,z+zWidth).endVertex()
            worldRenderer.pos(x+xWidth,y+yHeight,z+zWidth).endVertex()
            worldRenderer.pos(x+xWidth,y,z+zWidth).endVertex()
            worldRenderer.pos(x+xWidth,y,z).endVertex()
            worldRenderer.pos(x,y,z).endVertex()
            worldRenderer.pos(x,y,z+zWidth).endVertex()
            worldRenderer.pos(x,y,z).endVertex()
            worldRenderer.pos(x,y+yHeight,z).endVertex()
            worldRenderer.pos(x,y,z).endVertex()
            worldRenderer.pos(x+xWidth,y,z).endVertex()
            worldRenderer.pos(x+xWidth,y+yHeight,z).endVertex()
            worldRenderer.pos(x+xWidth,y,z).endVertex()
            worldRenderer.pos(x+xWidth,y,z+zWidth).endVertex()
            worldRenderer.pos(x,y,z+zWidth).endVertex()
            worldRenderer.pos(x,y+yHeight,z+zWidth).endVertex()
            worldRenderer.pos(x+xWidth,y+yHeight,z+zWidth).endVertex()
        }

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }
    fun drawP3box(x: Double, y: Double, z: Double, xWidth: Double, yHeight: Double, zWidth: Double, color: Color, thickness: Float = 3f, phase: Boolean = true, relocate: Boolean = true) {

        val yMiddle = (y + yHeight / 2)
        drawSquare(x, y + yHeight, z, xWidth, zWidth, color, thickness, phase, relocate)
        drawSquare(x, yMiddle, z, xWidth, zWidth, color, thickness, phase, relocate)
        drawSquare(x, y + 0.02, z, xWidth, zWidth, color, thickness, phase, relocate)
    }

    /**
     * Modified https://github.com/q12323/Meow-Client/blob/main/utils/RenderUtils.js#L46
     */
    fun drawEllipse(
        x: Double,
        y: Double,
        z: Double,
        radiusX: Float,  // width
        radiusZ: Float,  // length
        color: Color,
        slices: Int = 30,
        lineWidth: Float = 2f,
        phase: Boolean = false
    ) {
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(lineWidth)

        if (phase) GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()

        GlStateManager.pushMatrix()
        GlStateManager.translate(x - renderManager.viewerPosX, y - renderManager.viewerPosY + 0.1, z - renderManager.viewerPosZ)

        val deltaTheta = (Math.PI * 2) / slices
        worldRenderer.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION)
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        for (i in 0..slices) {
            val theta = deltaTheta * i
            worldRenderer.pos(
                radiusX * cos(theta),
                0.0,
                radiusZ * sin(theta)
            ).endVertex()
        }

        tessellator.draw()
        GlStateManager.popMatrix()

        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    fun drawSquare(
        x: Double,
        y: Double,
        z: Double,
        xWidth: Double,
        zWidth: Double,
        color: Color,
        thickness: Float = 3f,
        phase: Boolean = true,
        relocate: Boolean = true
    ) {
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(thickness)
        if (phase) GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()

        GlStateManager.pushMatrix()

        if (relocate) GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        GlStateManager.color(
            color.red.toFloat() / 255f,
            color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f,
            1f
        )

        val halfXWidth = xWidth / 2
        val halfZWidth = zWidth / 2

        worldRenderer.pos(x - halfXWidth, y, z - halfZWidth).endVertex()
        worldRenderer.pos(x + halfXWidth, y, z - halfZWidth).endVertex()
        worldRenderer.pos(x + halfXWidth, y, z + halfZWidth).endVertex()
        worldRenderer.pos(x - halfXWidth, y, z + halfZWidth).endVertex()
        worldRenderer.pos(x - halfXWidth, y, z - halfZWidth).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    fun drawP3boxWithLayers(
        x: Double,
        y: Double,
        z: Double,
        length: Float,
        width: Float,
        height: Float,
        colour: Color,
        layers: Int = 3
    ) {

        val gap = height / (layers - 1)
        for (i in 1 until layers - 1) {
            drawSquareTwo(x, y + (gap * i), z, length, width, colour, 4f, false)
        }

        drawSquareTwo(x, y + 0.01, z, length, width, colour, 4f, false)
        if (layers == 1) return
        drawSquareTwo(x, y + height, z, length, width, colour, 4f, false)
    }


    fun renderTransFlag(
        x: Double,
        y: Double,
        z: Double,
        length: Float,
        width: Float,
        height: Float,
    ){
        drawSquareTwo(x, y + 0.01, z, length, width, cyan, 4f, false)
        drawSquareTwo(x, y + height * 0.25, z, length, width, pink, 4f, false)
        drawSquareTwo(x, y + height * 0.5, z, length, width, white, 4f, false)
        drawSquareTwo(x, y + height * 0.75, z, length, width, pink, 4f, false)
        drawSquareTwo(x, y + height, z, length, width, cyan, 4f, false)
    }

    fun renderGayFlag(
        x: Double,
        y: Double,
        z: Double,
        length: Float,
        width: Float,
        height: Float,
    ){
        drawSquareTwo(x, y + 0.01, z, length, width, red, 4f, false)
        drawSquareTwo(x, y + height * 0.2, z, length, width, orange, 4f, false)
        drawSquareTwo(x, y + height * 0.4, z, length, width, yellow, 4f, false)
        drawSquareTwo(x, y + height * 0.6, z, length, width, green, 4f, false)
        drawSquareTwo(x, y + height * 0.8, z, length, width, blue, 4f, false)
        drawSquareTwo(x, y + height, z, length, width, pink, 4f, false)
    }

    fun renderLesbianFlag(
        x: Double,
        y: Double,
        z: Double,
        length: Float,
        width: Float,
        height: Float,
    ){
        drawSquareTwo(x, y + 0.01, z, length, width, Color(213, 45, 0), 4f, false)
        drawSquareTwo(x, y + height * 0.165, z, length, width, Color(239, 118, 39), 4f, false)
        drawSquareTwo(x, y + height * 0.33, z, length, width, Color(255, 154, 86), 4f, false)
        drawSquareTwo(x, y + height * 0.495, z, length, width, Color(255, 255, 255), 4f, false)
        drawSquareTwo(x, y + height * 0.66, z, length, width, Color(209, 98, 164), 4f, false)
        drawSquareTwo(x, y + height * 0.825, z, length, width, Color(181, 86, 144), 4f, false)
        drawSquareTwo(x, y + height, z, length, width, Color(163, 2, 98), 4f, false)
    }


    fun drawSquareTwo(
        x: Double,
        y: Double,
        z: Double,
        length: Float,
        width: Float,
        color: Color,
        thickness: Float = 3f,
        phase: Boolean = true,
        relocate: Boolean = true
    ) {
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(thickness)

        if (phase) GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()

        GlStateManager.pushMatrix()

        if (relocate) {
            GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        }

        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        GlStateManager.color(color.red / 255f, color.green / 255f,
            color.blue / 255f, color.alpha / 255f)

        val halfLength = length / 2
        val halfWidth = width / 2

        worldRenderer.pos(x + halfWidth, y, z + halfLength).endVertex()
        worldRenderer.pos(x + halfWidth, y, z - halfLength).endVertex()
        worldRenderer.pos(x - halfWidth, y, z - halfLength).endVertex()
        worldRenderer.pos(x - halfWidth, y, z + halfLength).endVertex()
        worldRenderer.pos(x + halfWidth, y, z + halfLength).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()

        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    fun drawStringInWorld(
        text: String,
        vec3: Vec3,
        color: Color = WHITE,
        depthTest: Boolean = true,
        scale: Float = 0.3f,
        shadow: Boolean = false
    ) {
        if (text.isBlank()) return
        GlStateManager.pushMatrix()

        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)

        GlStateManager.translate(vec3.xCoord, vec3.yCoord, vec3.zCoord)
        GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewX * if (mc.gameSettings.thirdPersonView == 2) -1 else 1, 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(-scale, -scale, scale)

        if (depthTest) GlStateManager.enableDepth() else GlStateManager.disableDepth()
        GlStateManager.depthMask(depthTest)

        mc.fontRendererObj.drawString("${text}\u00A7r", -mc.fontRendererObj.getStringWidth(text) / 2f, 0f, color.rgb, shadow)

        if (!depthTest) {
            GlStateManager.enableDepth()
            GlStateManager.depthMask(true)
        }
        GlStateManager.disableBlend()
        GlStateManager.enableTexture2D()
        GlStateManager.resetColor()
        GlStateManager.popMatrix()
    }

    fun drawCylinder(
        pos: Vec3, baseRadius: Number, topRadius: Number, height: Number,
        slices: Number, stacks: Number, rot1: Number, rot2: Number, rot3: Number,
        color: Color, depth: Boolean = false
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        GL11.glLineWidth(2.0f)

        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)

        if (depth) GlStateManager.enableDepth() else GlStateManager.disableDepth()
        GlStateManager.depthMask(depth)

        GlStateManager.resetColor()
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        GlStateManager.translate(pos.xCoord, pos.yCoord, pos.zCoord)
        GlStateManager.rotate(rot1.toFloat(), 1f, 0f, 0f)
        GlStateManager.rotate(rot2.toFloat(), 0f, 0f, 1f)
        GlStateManager.rotate(rot3.toFloat(), 0f, 1f, 0f)

        Cylinder().draw(baseRadius.toFloat(), topRadius.toFloat(), height.toFloat(), slices.toInt(), stacks.toInt())

        GlStateManager.disableBlend()
        GlStateManager.enableTexture2D()
        GlStateManager.resetColor()

        GL11.glLineWidth(1f)
        GlStateManager.enableCull()
        if (!depth) {
            GlStateManager.enableDepth()
            GlStateManager.depthMask(true)
        }
        GlStateManager.popMatrix()
    }

    fun renderPlayer(partialTicks: Float = 0f) {
        val renderPlayer = RenderPlayer(renderManager, mc.thePlayer.skinType == "slim")
        val x = mc.thePlayer.lastTickPosX + (posX - mc.thePlayer.lastTickPosX) * partialTicks - renderManager.viewerPosX
        val y = mc.thePlayer.lastTickPosY + (posY - mc.thePlayer.lastTickPosY) * partialTicks - renderManager.viewerPosY
        val z = mc.thePlayer.lastTickPosZ + (posZ - mc.thePlayer.lastTickPosZ) * partialTicks - renderManager.viewerPosZ

        GlStateManager.pushMatrix()
        GlStateManager.translate(x + 2, y, z)
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()

        renderPlayer.doRender(mc.thePlayer, x + 2, y, z, mc.thePlayer.rotationYaw, partialTicks)

        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.popMatrix()
    }

    private var displayTitle = ""
    private var titleTicks = 0
    private var titleColor = Color.PINK

    fun displayTitle(title: String, ticks: Int, color: Color = PINK) {
        displayTitle = title
        titleTicks = ticks
        titleColor = color
    }

    fun clearTitle() {
        displayTitle = ""
        titleTicks = 0
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || titleTicks <= 0) return
        mc.entityRenderer.setupOverlayRendering()
        renderText(
            displayTitle,
            sr.scaledWidth_double / 2 - displayTitle.getWidth() / 2 + 1,
            sr.scaledHeight_double / 2 + fontHeight, color = titleColor.toInt,
        )
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        titleTicks--
    }
}