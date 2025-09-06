package catgirlroutes.ui.clickgui.elements.menu

import catgirlroutes.module.settings.impl.HudSetting
import catgirlroutes.ui.clickgui.elements.Element
import catgirlroutes.ui.clickgui.elements.ElementType
import catgirlroutes.ui.clickgui.elements.ModuleButton
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui

/**
 * Provides a checkbox element.
 *
 * @author  Aton
 */
class ElementHud(parent: ModuleButton, setting: HudSetting) :
    Element<HudSetting>(parent, setting, ElementType.HUD) {

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        if (height == 0) return super.renderElement(mouseX, mouseY, partialTicks)
        val buttonColor = if (setting.enabled)
            ColorUtil.clickGUIColor.rgb
        else ColorUtil.buttonColor.rgb

        /** Rendering the name and the checkbox */
        FontUtil.drawString(displayName, 1, 3)
        Gui.drawRect(width -13, 2, width - 1, 13, buttonColor)
        if (isCheckHovered(mouseX, mouseY))
            Gui.drawRect(width -13, 2, width - 1, 13, ColorUtil.boxHoverColor)

        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (height == 0) return false
        if (mouseButton == 0 && isCheckHovered(mouseX, mouseY)) {
            setting.enabled = !setting.enabled
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Checks whether this element is hovered
     */
    private fun isCheckHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute + width - 13 && mouseX <= xAbsolute + width - 1 && mouseY >= yAbsolute + 2 && mouseY <= yAbsolute + height - 2
    }
}