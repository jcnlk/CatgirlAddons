package catgirlroutes.ui.misc.elements.impl

import catgirlroutes.ui.animations.impl.ColorAnimation
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.misc.elements.ElementDSL
import catgirlroutes.ui.misc.elements.MiscElement
import catgirlroutes.ui.misc.elements.MiscElementStyle
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedOutline

class MiscElementBoolean(
    style: MiscElementStyle = MiscElementStyle(),
    var enabled: Boolean = false,
    var gap: Double = 1.0
) : MiscElement(style) {

    private val colourAnimation = ColorAnimation(250)

    override fun render(mouseX: Int, mouseY: Int) {
        val colour = colourAnimation.get(hoverColour, colour, this.enabled) // enabledColour, disabledColour
        drawRoundedBorderedRect(x + this.gap, y + this.gap, width - this.gap * 2, height - this.gap * 2, radii, thickness, colour, colour)
        drawRoundedOutline(
            x, y, width, height, radii, thickness,
            if (isHovered(mouseX, mouseY)) outlineHoverColour else outlineColour
        )

        FontUtil.drawString(this.value, x + width + 5.0, y + height / 2 - FontUtil.fontHeight / 2, style.textColour.rgb)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            this.enabled = !this.enabled
            colourAnimation.start(true)
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }
}

class BooleanBuilder : ElementDSL<MiscElementBoolean>() {
    var text by _style::value

    var enabledColour by _style::hoverColour
    var disabledColour by _style::colour

    var gap: Double = 1.0
    var enabled: Boolean = false

    override fun buildElement(): MiscElementBoolean {
        return MiscElementBoolean(createStyle(), enabled, gap)
    }
}

fun boolean(block: BooleanBuilder.() -> Unit): MiscElementBoolean {
    return BooleanBuilder().apply(block).build()
}
