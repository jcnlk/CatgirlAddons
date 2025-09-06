package catgirlroutes.ui.hud

import catgirlroutes.module.Module
import catgirlroutes.module.settings.AlwaysActive
import catgirlroutes.module.settings.Visibility
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.ui.clickgui.util.FontUtil.drawStringWithShadow
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.ui.clickgui.util.MouseUtils.mx
import catgirlroutes.ui.clickgui.util.MouseUtils.my
import catgirlroutes.utils.render.HUDRenderUtils.drawHoveringText
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedOutline
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import catgirlroutes.utils.render.HUDRenderUtils.sr
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.floor
import kotlin.reflect.full.hasAnnotation

/**
 * Sets up a hud Element.
 * This constructor takes care of creating the [NumberSetting]s required to save the position and scale of the hud
 * element to the config.
 * Provides functionality for game overlay elements.
 * @author Aton
 */
abstract class HudElement(
    val name: String,
    private val xDefault: Double,
    private val yDefault: Double,
    var width: Double,
    var height: Double,
    private val defaultScale: Double,
) {

    lateinit var parentModule: Module

    val toggled: Boolean
        get() = parentModule.enabled && enabled || parentModule::class.hasAnnotation<AlwaysActive>()

    val enabled get() = enabledSett.enabled

    open val visible: Boolean
        get() = true

    private lateinit var xSett: NumberSetting
    private lateinit var ySett: NumberSetting
    lateinit var scaleSett: NumberSetting
    lateinit var enabledSett: BooleanSetting

    private val zoomIncrement = 0.05

    /**
     * Use these instead of a direct reference to the NumberSetting
     */
    var x: Double
     get() = xSett.value
     set(value) {
         xSett.value = value
     }

    var y: Double
        get() = ySett.value
        set(value) {
            ySett.value = value
        }

    val scale: Double
        get() = scaleSett.value

    fun init(module: Module) {
        this.parentModule = module
        val id = parentModule.settings.count { it.name.startsWith("xHud") }
        this.xSett = NumberSetting("xHud_$id", xDefault, visibility = Visibility.HIDDEN)
        this.ySett = NumberSetting("yHud_$id", yDefault, visibility = Visibility.HIDDEN)
        this.scaleSett = NumberSetting("scaleHud_$id", defaultScale, 0.1, 4.0, 0.01, visibility = Visibility.HIDDEN)
        this.enabledSett = BooleanSetting("enabledHud_$id", name.isEmpty(), visibility = Visibility.HIDDEN)

        this.parentModule.addSettings(this.xSett, this.ySett, this.scaleSett, this.enabledSett)
    }

    /**
     * Resets the position of this hud element by setting the value of xSett and ySett to their default.
     *
     * Can be overridden in the implementation.
     */
    open fun resetElement() {
        xSett.value = xSett.default
        ySett.value = ySett.default
        scaleSett.value = scaleSett.default
    }

    /**
     * Handles scroll wheel action for this element.
     * Can be overridden in implementation.
     */
    open fun scroll(amount: Int) {
        this.scaleSett.value += amount * zoomIncrement
    }

    /**
     * This will initiate the hud render and translate to the correct position and scale.
     */
    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !this.toggled || !this.visible) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scaleSett.value, scaleSett.value, 1.0)

        renderHud()

        GlStateManager.popMatrix()
    }

    /**
     * Override this method in your implementations.
     *
     * This method is responsible for rendering the HUD element.
     * Within this method coordinates are already transformed regarding the HUD position [x],[x] and [scaleSett].
     */
    open fun renderHud() = Unit

    open fun preview() {
    }

    /**
     * Use this method to dynamically update [HudElement] dimensions
     * (I couldn't come up with anything else without putting a lot of effort)
     * @see [TestHud]
     */
    open fun updateSize() = Unit

    fun getBounds(x: Double = this.x, y: Double = this.y, scale: Double = this.scale, padding: Double = 2.5 * scale): ElementBounds {
        val renderWidth = width * scale
        val renderHeight = height * scale
        return ElementBounds(
            x - padding,
            x + renderWidth + padding,
            y - padding,
            y + renderHeight + padding,
            x + renderWidth / 2,
            y + renderHeight / 2
        )
    }

    fun isHovered(mouseX: Int = mx, mouseY: Int = my): Boolean {
        if (!this.toggled) return false
        return mouseX > (x - 2.5 * scale) && mouseX < (x + width * scale + 2.5 * scale)
                && mouseY > (y - 2.5 * scale) && mouseY < (y + height * scale + 2.5 * scale)
    }
    /**
     * Used for moving the hud element.
     * Draws a rectangle in place of the actual element
     */
    fun renderPreview(isDragging: Boolean) {
        if (!this.toggled) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0.0)
        GlStateManager.scale(scale, scale, 1.0)

        GlStateManager.pushMatrix()
        GlStateManager.scale(1.0 / scale, 1.0 / scale, 1.0)
        GlStateManager.translate(-x, -y, 0.0)

        if (isDragging) {
            val text = "x: ${x.toInt() * 2} y: ${y.toInt() * 2} ${if (scale != 1.0) "${floor(scale * 100) / 100} " else ""}"
            val bounds = this.getBounds()
            val textWidth = text.getWidth()

            val xPos = -2.0 + if (sr.scaledWidth < bounds.right + textWidth + 5.0) bounds.left - textWidth + 1.0 else bounds.right + 2.0
            val yPos = -2.0 + if (bounds.top - 12 < 5) bounds.bottom + 1.0 else bounds.top - 10.0

            drawRoundedRect(xPos, yPos, textWidth, 12.0, 3.0, Color(21, 21, 21, 200))
            drawStringWithShadow(text, xPos, yPos)
        } else if (isHovered()) {
            val text = listOfNotNull(
                parentModule.name,
                name.takeIf { it.isNotEmpty() }?.let { if (!it.contains("hud", true)) "$it hud" else it },
                "   x: ${x.toInt() * 2}, y: ${y.toInt() * 2}, scale: ${floor(scale * 100) / 100}",
                "",
                "§7Right click to reset position",
                "§7Middle click to open module",
                "§7Scroll wheel to resize",
                "§7Shift to disable snapping"
            )
            drawHoveringText(text, mx, my)
        }

        GlStateManager.popMatrix()

        if (!isDragging) {
            drawRoundedRect(-2.0, -2.0, width + 4, height + 4, 3.0, Color(21, 21, 21, 200)) // bg
            this.preview()
        }

        drawRoundedOutline(-2.0, -2.0, width + 4, height + 4, 3.0, 0.5, Color(208, 208, 208))

        GlStateManager.popMatrix()
    }
}

class HudElementDSL(val name: String) {
    var x: Double = 0.0
    var y: Double = 0.0
    var width: Double = 10.0
    var height: Double = 10.0
    var scale: Double = 1.0

    private var renderHud: (HudElement.() -> Unit)? = null
    private var preview: (HudElement.() -> Unit)? = null
    private var visibleIf: (HudElement.() -> Boolean)? = null

    private var dimensions: (HudElement.() -> Unit)? = null
    private var setWidth: (HudElement.() -> Number)? = null
    private var setHeight: (HudElement.() -> Number)? = null

    fun at(x: Number, y: Number) {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    fun size(width: Number, height: Number, scale: Number = 1.0) {
        this.width = width.toDouble()
        this.height = height.toDouble()
        this.scale = scale.toDouble()
    }

    fun size(text: String, height: Number = fontHeight - 2, scale: Number = 1.0) {
        size(text.getWidth(), height, scale)
    }

    fun render(block: HudElement.() -> Unit) {
        this.renderHud = block
    }

    fun preview(block: HudElement.() -> Unit) {
        this.preview = block
    }

    fun visibleIf(block: HudElement.() -> Boolean) = apply {
        this.visibleIf = block
    }

    fun updateSize(block: HudElement.() -> Unit) {
        this.dimensions = block
    }

    fun width(block: HudElement.() -> Number) {
        this.setWidth = block
    }

    fun height(block: HudElement.() -> Number) {
        this.setHeight = block
    }

    fun build(): HudElement {
        return object : HudElement(name, x, y, width, height, scale) {
            override fun renderHud() { renderHud?.invoke(this) }
            override fun preview() { preview?.invoke(this) ?: super.preview() }
            override val visible: Boolean get() = visibleIf?.invoke(this) ?: super.visible
            override fun updateSize() {
                dimensions?.invoke(this)
                setWidth?.let { width = it(this).toDouble() }
                setHeight?.let { height = it(this).toDouble() }
            }
        }
    }
}

data class ElementBounds(
    val left: Double,
    val right: Double,
    val top: Double,
    val bottom: Double,
    val centreX: Double,
    val centreY: Double
)