package catgirlroutes.utils.render

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.ui.Screen.Companion.CLICK_GUI_SCALE
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.config.GuiUtils.drawGradientRect
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * ## A Collection of methods for rendering 2D Objects in orthographic projection for the HUD or for a gui.
 *
 * ### Coordinate space
 * The coordinate space used by the methods here sees the top left corner of your window as the origin 0,0.
 * The x-axis is pointing towards the right of the screen. and the y-axis is pointing **downwards**.
 *
 *
 * Heavily based on the rendering for [Funny Map by Harry282](https://github.com/Harry282/FunnyMap/blob/master/src/main/kotlin/funnymap/utils/RenderUtils.kt).
 *
 * @author Aton
 */
object HUDRenderUtils {
    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer

    val sr get() = ScaledResolution(mc)
    val scale get() = CLICK_GUI_SCALE / sr.scaleFactor
    val displayWidth get() = Display.getDesktopDisplayMode().width
    val displayHeight get() = Display.getDesktopDisplayMode().height

    fun renderRect(x: Double, y: Double, w: Double, h: Double, color: Color) {
        if (color.alpha == 0) return
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.enableAlpha()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        addQuadVertices(x, y, w, h)
        tessellator.draw()

        GlStateManager.disableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    fun renderRectBorder(x: Double, y: Double, w: Double, h: Double, thickness: Double, color: Color) {
        if (color.alpha == 0) return
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.translate(0f, 0f, 0f)
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        GlStateManager.shadeModel(GL11.GL_FLAT)

        addQuadVertices(x - thickness, y, thickness, h)
        addQuadVertices(x - thickness, y - thickness, w + thickness * 2, thickness)
        addQuadVertices(x + w, y, thickness, h)
        addQuadVertices(x - thickness, y + h, w + thickness * 2, thickness)

        tessellator.draw()

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
    }

    private fun addQuadVertices(x: Double, y: Double, w: Double, h: Double) {
        worldRenderer.pos(x, y + h, 0.0).endVertex()
        worldRenderer.pos(x + w, y + h, 0.0).endVertex()
        worldRenderer.pos(x + w, y, 0.0).endVertex()
        worldRenderer.pos(x, y, 0.0).endVertex()
    }

    fun drawTexturedModalRect(x: Int, y: Int, width: Int, height: Int) {
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(0.0, 1.0).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(1.0, 1.0).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0).tex(1.0, 0.0).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex(0.0, 0.0).endVertex()
        tessellator.draw()
    }

    data class Scissor(val x: Float, val y: Float, val width: Float, val height: Float, val context: Int)

    private val scissorList = mutableListOf(Scissor(0.0f, 0.0f, 16000.0f, 16000.0f, 0))

    private fun intersectScissors(a: Scissor, b: Scissor): Scissor? {
        val nx = maxOf(a.x, b.x)
        val ny = maxOf(a.y, b.y)
        val nr = minOf(a.x + a.width, b.x + b.width)
        val nb = minOf(a.y + a.height, b.y + b.height)
        val nw = nr - nx
        val nh = nb - ny
        return if (nw > 0 && nh > 0) Scissor(nx, ny, nw, nh, 0) else null
    }

    fun scissor(x: Number, y: Number, width: Number, height: Number): Scissor {
        val scale = sr.scaleFactor

        val parentScissor = scissorList.last()

        val newScissor = intersectScissors(parentScissor, Scissor(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), 0))
            ?: return Scissor(0.0f, 0.0f, 0.0f, 0.0f, scissorList.size)

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(
            (newScissor.x * scale).toInt(),
            (mc.displayHeight - (newScissor.height + newScissor.y) * scale).toInt(),
            (newScissor.width * scale).toInt(),
            (newScissor.height * scale).toInt()
        )

        val scissor = newScissor.copy(context = scissorList.size)
        scissorList.add(scissor)
        return scissor
    }

    fun resetScissor() {
        scissorList.removeLast()
        val scale = sr.scaleFactor
        if (scissorList.isNotEmpty()) {
            val nextScissor = scissorList.last()
            GL11.glScissor(
                (nextScissor.x * scale).toInt(),
                (mc.displayHeight - (nextScissor.height + nextScissor.y) * scale).toInt(),
                (nextScissor.width * scale).toInt(),
                (nextScissor.height * scale).toInt()
            )
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }
    }

    fun disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    fun enableScissor() {
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
    }

    /**
     * Sets up a GL scissor test for the specified region of the screen.
     *
     * Uses the same coordinate system as all the rendering methods.
     * The native OpenGL method [GL11.glScissor] uses a different coordinate system.
     * This method takes care of the coordinate transform for you.
     * @see setUpScissor
     * @see endScissor
     */
    fun setUpScissorAbsolute(left: Int, top: Int, right: Int, bottom: Int) {
        setUpScissor(left, top, (right - left).coerceAtLeast(0), (bottom - top).coerceAtLeast(0))
    }

    /**
     * Sets up a GL scissor test for the specified region of the screen.
     *
     * Uses the same coordinate system as all the rendering methods.
     * The native OpenGL method [GL11.glScissor] uses a different coordinate system.
     * This method takes care of the coordinate transform for you.
     * @see setUpScissorAbsolute
     * @see endScissor
     */
    fun setUpScissor(x: Int, y: Int, width: Int, height: Int) {
        /*
        glScissor uses different coordinates than all the rendering methods.
        It uses absolute window coordinates starting with 0,0 in the bottom left corner of the window.
        The coordinates directly relate to pixels.
        It is not affected by things such as glTanslate and glScale.

        In contrast, all other hud rendering methods use the top left corner as 0,0
         */
        val scale = mc.displayHeight / ScaledResolution(mc).scaledHeight.toDouble()
        GL11.glScissor(
            (x * scale).toInt(),
            (mc.displayHeight - (height + y) *scale).toInt() ,
            (width*scale).toInt(),
            (height * scale).toInt()
        )
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
    }

    fun setUpScissor(x: Double, y: Double, width: Double, height: Double) {
        this.setUpScissor(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }

    /**
     * Disables the GL scissor test.
     * @see setUpScissor
     * @see setUpScissorAbsolute
     */
    fun endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    private fun setColor(color: Int) {
        val a = (color shr 24 and 0xFF) / 255.0f
        val r = (color shr 16 and 0xFF) / 255.0f
        val g = (color shr 8 and 0xFF) / 255.0f
        val b = (color and 0xFF) / 255.0f
        GL11.glColor4f(r, g, b, a)
    }

    fun drawBorderedRect(x: Double, y: Double, width: Double, height: Double, thickness: Double, colour1: Color, colour2: Color) {
        renderRect(x, y, width, height, colour1)
        renderRectBorder(x, y, width, height, thickness, colour2)
    }

    fun drawRoundedBorderedRect(x: Double, y: Double, width: Double, height: Double, radius: Double, thickness: Double, colour1: Color, colour2: Color) {
        drawRoundedRect(x, y, width, height, radius, colour1)
        drawRoundedOutline(x, y, width, height, radius, thickness, colour2)
    }

    fun drawRoundedBorderedRect(x: Double, y: Double, width: Double, height: Double, radii: Radii, thickness: Double, colour1: Color, colour2: Color) {
        drawRoundedRect(x, y, width, height, radii, colour1)
        drawRoundedOutline(x, y, width, height, radii, thickness, colour2)
    }

    fun drawRoundedRect(x: Double, y: Double, width: Double, height: Double, radius: Double, colour: Color) {
        drawRoundedRect(x, y, width, height, Radii(radius, radius, radius, radius), colour)
    }

    fun drawRoundedRect(x: Double, y: Double, width: Double, height: Double, radii: Radii, colour: Color) {
        if (colour.alpha == 0) return
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        val color = colour.rgb
        var x1 = x
        var y1 = y
        val x2 = x1 + width
        val y2 = y1 + height
        val f = ((color shr 24) and 0xFF) / 255.0f
        val f2 = ((color shr 16) and 0xFF) / 255.0f
        val f3 = ((color shr 8) and 0xFF) / 255.0f
        val f4 = (color and 0xFF) / 255.0f

        GL11.glPushAttrib(0);
        GL11.glScaled(0.5, 0.5, 0.5);

        x1 *= 2.0
        y1 *= 2.0
        val x2Scaled = x2 * 2.0
        val y2Scaled = y2 * 2.0

        GL11.glDisable(3553);
        GL11.glColor4f(f2, f3, f4, f);
        GL11.glEnable(2848);
        GL11.glBegin(9);

        drawArc(x1 + radii.topLeft, y1 + radii.topLeft, -radii.topLeft, -radii.topLeft, 0, 90, 3)
        drawArc(x1 + radii.bottomLeft, y2Scaled - radii.bottomLeft, -radii.bottomLeft, -radii.bottomLeft, 90, 180, 3)
        drawArc(x2Scaled - radii.bottomRight, y2Scaled - radii.bottomRight, radii.bottomRight, radii.bottomRight, 0, 90, 3)
        drawArc(x2Scaled - radii.topRight, y1 + radii.topRight, radii.topRight, radii.topRight, 90, 180, 3)

        GL11.glEnd();
        GL11.glEnable(3553)
        GL11.glDisable(2848)
        GL11.glEnable(3553)
        GL11.glScaled(2.0, 2.0, 2.0)
        GL11.glPopAttrib()
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    fun drawRoundedOutline(x: Double, y: Double, width: Double, height: Double, radius: Double, thickness: Double, colour: Color) {
        drawRoundedOutline(x, y, width, height, Radii(radius, radius, radius, radius), thickness, colour)
    }

    fun drawRoundedOutline(x: Double, y: Double, width: Double, height: Double, radii: Radii, thickness: Double, colour: Color) {
        if (colour.alpha == 0) return
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        var x2 = x + width
        var y2 = y + height
        val f: Float = (colour.rgb shr 24 and 0xFF) / 255.0f
        val f2: Float = (colour.rgb shr 16 and 0xFF) / 255.0f
        val f3: Float = (colour.rgb shr 8 and 0xFF) / 255.0f
        val f4: Float = (colour.rgb and 0xFF) / 255.0f
        GL11.glPushAttrib(0)
        GL11.glScaled(0.5, 0.5, 0.5)
        val x1 = x * 2.0f
        val y1 = y * 2.0f
        x2 *= 2.0
        y2 *= 2.0
        GL11.glLineWidth(thickness.toFloat())
        GL11.glDisable(3553)
        GL11.glColor4f(f2, f3, f4, f)
        GL11.glEnable(2848)
        GL11.glBegin(2)

        drawArc(x1 + radii.topLeft, y1 + radii.topLeft, -radii.topLeft, -radii.topLeft, 0, 90, 3)
        drawArc(x1 + radii.bottomLeft, y2 - radii.bottomLeft, -radii.bottomLeft, -radii.bottomLeft, 90, 180, 3)
        drawArc(x2 - radii.bottomRight, y2 - radii.bottomRight, radii.bottomRight, radii.bottomRight, 0, 90, 3)
        drawArc(x2 - radii.topRight, y1 + radii.topRight, radii.topRight, radii.topRight, 90, 180, 3)

        GL11.glEnd();
        GL11.glEnable(3553)
        GL11.glDisable(2848)
        GL11.glEnable(3553)
        GL11.glScaled(2.0, 2.0, 2.0)
        GL11.glPopAttrib()
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun drawArc(centerX: Double, centerY: Double, radiusX: Double, radiusY: Double, startAngle: Int, endAngle: Int, step: Int) {
        for (i in startAngle..endAngle step step) {
            val angle = Math.toRadians(i.toDouble())
            GL11.glVertex2d(centerX + sin(angle) * radiusX, centerY + cos(angle) * radiusY)
        }
    }

    fun drawTexturedRect(
        resource: ResourceLocation,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        uMin: Double = 0.0,
        uMax: Double = 1.0,
        vMin: Double = 0.0,
        vMax: Double = 1.0,
        filter: Int = GL11.GL_NEAREST
    ) {
       mc.textureManager.bindTexture(resource)
       drawTexturedRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), uMin.toFloat(), uMax.toFloat(), vMin.toFloat(), vMax.toFloat(), filter)
    }

    fun drawTexturedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        uMin: Float,
        uMax: Float,
        vMin: Float,
        vMax: Float,
        filter: Int
    ) {
        GlStateManager.enableBlend()
        setColor(Color.WHITE.rgb)

        GL14.glBlendFuncSeparate(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA
        )

        drawTexturedRectNoBlend(x, y, width, height, uMin, uMax, vMin, vMax, filter)

        GlStateManager.disableBlend()
    }

    private fun drawTexturedRectNoBlend(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        uMin: Float,
        uMax: Float,
        vMin: Float,
        vMax: Float,
        filter: Int
    ) {
        GlStateManager.enableTexture2D()

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter)

        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldRenderer
            .pos(x.toDouble(), (y + height).toDouble(), 0.0)
            .tex(uMin.toDouble(), vMax.toDouble()).endVertex()
        worldRenderer
            .pos((x + width).toDouble(), (y + height).toDouble(), 0.0)
            .tex(uMax.toDouble(), vMax.toDouble()).endVertex()
        worldRenderer
            .pos((x + width).toDouble(), y.toDouble(), 0.0)
            .tex(uMax.toDouble(), vMin.toDouble()).endVertex()
        worldRenderer
            .pos(x.toDouble(), y.toDouble(), 0.0)
            .tex(uMin.toDouble(), vMin.toDouble()).endVertex()
        tessellator.draw()

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    }

    fun drawSBBox(
        x: Double, y: Double, width: Double, height: Double,
        topRight: Int, topLeft: Int = Color.WHITE.rgb,
        botRight: Int = Color.black.rgb, botLeft: Int = Color.black.rgb
    ) {
        val x2: Double = x + width
        val y2: Double = y + height

        val a1 = (topRight shr 24 and 0xFF) / 255.0f
        val r1 = (topRight shr 16 and 0xFF) / 255.0f
        val g1 = (topRight shr 8 and 0xFF) / 255.0f
        val b1 = (topRight and 0xFF) / 255.0f

        // WHITE by default
        val a2 = (topLeft shr 24 and 0xFF) / 255.0f
        val r2 = (topLeft shr 16 and 0xFF) / 255.0f
        val g2 = (topLeft shr 8 and 0xFF) / 255.0f
        val b2 = (topLeft and 0xFF) / 255.0f

        // black by default
        val a3 = (botRight shr 24 and 0xFF) / 255.0f
        val r3 = (botRight shr 16 and 0xFF) / 255.0f
        val g3 = (botRight shr 8 and 0xFF) / 255.0f
        val b3 = (botRight and 0xFF) / 255.0f

        // black by default
        val a4 = (botLeft shr 24 and 0xFF) / 255.0f
        val r4 = (botLeft shr 16 and 0xFF) / 255.0f
        val g4 = (botLeft shr 8 and 0xFF) / 255.0f
        val b4 = (botLeft and 0xFF) / 255.0f

        GlStateManager.pushMatrix()
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.disableDepth()
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)

        worldRenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR)

        // 1st
        worldRenderer.pos(x, y2, 0.0).color(r4, g4, b4, a4).endVertex() // bot left
        worldRenderer.pos(x2, y2, 0.0).color(r3, g3, b3, a3).endVertex() // bot right
        worldRenderer.pos(x2, y, 0.0).color(r1, g1, b1, a1).endVertex() // top right

        // 2nd
        worldRenderer.pos(x, y, 0.0).color(r2, g2, b2, a2).endVertex() // top left
        worldRenderer.pos(x, y2, 0.0).color(r4, g4, b4, a4).endVertex() // bot left
        worldRenderer.pos(x2, y, 0.0).color(r1, g1, b1, a1).endVertex() // top right

        tessellator.draw()

        GL11.glShadeModel(GL11.GL_FLAT)
        GlStateManager.disableBlend()
        GlStateManager.enableDepth()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.popMatrix()
    }

    fun drawRoundedSBBox( // todo: fix
        x: Double, y: Double, width: Double, height: Double,
        radius: Double,
        topRight: Int, topLeft: Int = Color.WHITE.rgb,
        botRight: Int = Color.BLACK.rgb, botLeft: Int = Color.BLACK.rgb
    ) {
        val x2: Double = x + width
        val y2: Double = y + height

        // Extract color components
        val a1 = (topRight shr 24 and 0xFF) / 255.0f
        val r1 = (topRight shr 16 and 0xFF) / 255.0f
        val g1 = (topRight shr 8 and 0xFF) / 255.0f
        val b1 = (topRight and 0xFF) / 255.0f

        val a2 = (topLeft shr 24 and 0xFF) / 255.0f
        val r2 = (topLeft shr 16 and 0xFF) / 255.0f
        val g2 = (topLeft shr 8 and 0xFF) / 255.0f
        val b2 = (topLeft and 0xFF) / 255.0f

        val a3 = (botRight shr 24 and 0xFF) / 255.0f
        val r3 = (botRight shr 16 and 0xFF) / 255.0f
        val g3 = (botRight shr 8 and 0xFF) / 255.0f
        val b3 = (botRight and 0xFF) / 255.0f

        val a4 = (botLeft shr 24 and 0xFF) / 255.0f
        val r4 = (botLeft shr 16 and 0xFF) / 255.0f
        val g4 = (botLeft shr 8 and 0xFF) / 255.0f
        val b4 = (botLeft and 0xFF) / 255.0f

        GlStateManager.pushMatrix()
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GL11.glShadeModel(GL11.GL_SMOOTH)

        drawRoundedRect(x, y, width, height, radius, Color(r1, g1, b1, a1))
        drawRoundedRect(x, y, width, height, radius, Color(r2, g2, b2, a2))
        drawRoundedRect(x, y, width, height, radius, Color(r3, g3, b3, a3))
        drawRoundedRect(x, y, width, height, radius, Color(r4, g4, b4, a4))

        GL11.glShadeModel(GL11.GL_FLAT)
        GlStateManager.disableBlend()
        GlStateManager.enableDepth()
        GlStateManager.enableTexture2D()
        GlStateManager.popMatrix()
    }

    fun drawHueBox(x: Int, y: Int, width: Int, height: Int) {
        for (i in 0 until width) {
            val ratio = i.toFloat() / width.toFloat()
            val color = Color.HSBtoRGB(ratio, 1.0f, 1.0f)
            Gui.drawRect(x + i, y, x + i + 1, y + height, color)
        }
    }

    fun drawRoundedHueBox(x: Double, y: Double, width: Double, height: Double, radius: Double, vertical: Boolean = false) { // todo fix

        if (vertical) {
            for (i in 0 until height.toInt()) {
                val ratio = i.toFloat() / height.toFloat()
                val color = Color.HSBtoRGB(ratio, 1.0f, 1.0f)
                renderRect(x, y + i, width, 1.0, Color(color))
            }
        } else {
            for (i in 0 until width.toInt()) {
                val ratio = i.toFloat() / width.toFloat()
                val color = Color.HSBtoRGB(ratio, 1.0f, 1.0f)
                renderRect(x + i, y, 1.0, height, Color(color))
            }
        }
    }

    fun drawItemStackWithText(stack: ItemStack?, x: Double, y: Double, text: String? = null) {
        if (stack == null) return
        val itemRender = mc.renderItem

        RenderHelper.enableGUIStandardItemLighting()
        itemRender.zLevel = -145f //Negates the z-offset of the below method.
        itemRender.renderItemAndEffectIntoGUI(stack, x.toInt(), y.toInt())
        itemRender.renderItemOverlayIntoGUI(mc.fontRendererObj, stack, x.toInt(), y.toInt(), text)
        itemRender.zLevel = 0f
        RenderHelper.disableStandardItemLighting()
    }

    fun Slot.highlight(color: Color) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(0.0, 0.0, -10.0)
        GlStateManager.disableBlend()
        GlStateManager.disableLighting()
        Gui.drawRect(
            xDisplayPosition,
            yDisplayPosition,
            xDisplayPosition + 16,
            yDisplayPosition + 16,
            color.rgb
        )
        GlStateManager.enableBlend()
        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }

    fun drawPlayerOnScreen(x: Double, y: Double, partialTicks: Float, scale: Double = 1.0) {

        val ent = mc.thePlayer

        GlStateManager.enableColorMaterial()
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 50.0)
        GlStateManager.scale(-scale, scale, scale)
        GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f)
        RenderHelper.enableStandardItemLighting()

        val renderManager = mc.renderManager
        renderManager.isRenderShadow = false

        renderManager.renderEntityWithPosYaw(ent, 0.0, 0.0, 0.0, ent.rotationYaw, partialTicks)
        renderManager.isRenderShadow = true

        GlStateManager.popMatrix()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit)
        GlStateManager.disableTexture2D()
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
    }

    fun drawHoveringText(
        textLines: List<String>,
        x: Int,
        y: Int,
        scrollOffset: Int
    ) {
        drawHoveringText(
            textLines,
            x,
            y - scrollOffset,
            screenHeight = sr.scaledHeight - scrollOffset
        )
    }

    fun drawHoveringText(
        textLines: List<String>,
        x: Int,
        y: Int,
        screenWidth: Int = sr.scaledWidth,
        screenHeight: Int = sr.scaledHeight,
        maxTextWidth: Int = -1,
        themed: Boolean = true,
        yOffset: Double = 0.0
    ) {
        if (textLines.isEmpty()) return
        GlStateManager.pushMatrix()
        GlStateManager.disableRescaleNormal()
        GlStateManager.translate(0.0, 0.0, 300.0)

        var tooltipTextWidth = textLines.maxOfOrNull { FontUtil.getStringWidth(it) } ?: 0

        var needsWrap = false
        var titleLinesCount = 1
        var tooltipX = x + 12

        if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
            tooltipX = x - 16 - tooltipTextWidth
            if (tooltipX < 4) { // if the tooltip doesn't fit on the screen
                tooltipTextWidth = if (x > screenWidth / 2) {
                    x - 12 - 8
                } else {
                    screenWidth - 16 - x
                }
                needsWrap = true
            }
        }

        if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth) {
            tooltipTextWidth = maxTextWidth
            needsWrap = true
        }

        val finalTextLines = if (needsWrap) {
            var wrappedTooltipWidth = 0
            val wrappedTextLines = mutableListOf<String>()

            textLines.forEachIndexed { i, textLine ->
                val wrappedLine = mc.fontRendererObj.listFormattedStringToWidth(textLine, tooltipTextWidth)
                if (i == 0) {
                    titleLinesCount = wrappedLine.size
                }

                wrappedLine.forEach { line ->
                    val lineWidth = FontUtil.getStringWidth(line)
                    if (lineWidth > wrappedTooltipWidth) {
                        wrappedTooltipWidth = lineWidth
                    }
                    wrappedTextLines.add(line)
                }
            }

            tooltipTextWidth = wrappedTooltipWidth

            tooltipX = if (x > screenWidth / 2) {
                x - 16 - tooltipTextWidth
            } else {
                x + 12
            }

            wrappedTextLines
        } else {
            textLines
        }

        var tooltipY = y - 12
        var tooltipHeight = 8

        if (finalTextLines.size > 1) {
            tooltipHeight += (finalTextLines.size - 1) * 10
            if (finalTextLines.size > titleLinesCount) {
                tooltipHeight += 2 // gap between title lines and next lines
            }
        }

        if (tooltipY + tooltipHeight + 6 > screenHeight) {
            tooltipY = screenHeight - tooltipHeight - 6
        }

        if (!themed) {
            val backgroundColor = 0xF0100010.toInt()
            drawGradientRect(0, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor)
            drawGradientRect(0, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor)
            drawGradientRect(0, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor)
            drawGradientRect(0, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor)
            drawGradientRect(0, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor)

            val borderColorStart = 0x505000FF
            val borderColorEnd = (borderColorStart and 0xFEFEFE) shr 1 or (borderColorStart and 0xFF000000.toInt())
            drawGradientRect(0, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd)
            drawGradientRect(0, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd)
            drawGradientRect(0, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart)
            drawGradientRect(0, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd)
        } else {
            drawRoundedBorderedRect(tooltipX - 3.0, tooltipY - 3.0, tooltipTextWidth + 6.0, tooltipHeight + 6.0, 3.0, 1.0, ColorUtil.buttonColor.withAlpha(0.94f), ColorUtil.clickGUIColor)
        }

        finalTextLines.forEachIndexed { lineNumber, line ->
            FontUtil.drawStringWithShadow(line, tooltipX.toDouble(), tooltipY.toDouble(), -1)

            if (lineNumber + 1 == titleLinesCount) {
                tooltipY += 2
            }

            tooltipY += 10
        }

        GlStateManager.enableRescaleNormal()
        GlStateManager.popMatrix()
    }
}

data class Radii(
    val topLeft: Double,
    val topRight: Double = topLeft,
    val bottomRight: Double = topLeft,
    val bottomLeft: Double = topLeft
)