package catgirlroutes.ui.clickgui.elements.menu

import catgirlroutes.module.settings.impl.KeyBindSetting
import catgirlroutes.ui.clickgui.elements.Element
import catgirlroutes.ui.clickgui.elements.ElementType
import catgirlroutes.ui.clickgui.elements.ModuleButton
import catgirlroutes.ui.clickgui.util.FontUtil
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

/**
 * Provides a key bind element.
 *
 * @author Aton
 */
class ElementKeyBind(parent: ModuleButton, setting: KeyBindSetting) :
    Element<KeyBindSetting>(parent, setting, ElementType.KEY_BIND) {

    private val keyBlackList = intArrayOf()

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        val keyName = if (setting.value.key > 0)
            Keyboard.getKeyName(setting.value.key) ?: "Err"
        else if (setting.value.key < 0)
            Mouse.getButtonName(setting.value.key + 100)
        else
            ".."
        val displayValue = "[$keyName]"

        FontUtil.drawString(displayName, 1, 2)
        FontUtil.drawString(displayValue, width - FontUtil.getStringWidth(displayValue), 2)

        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed.
     * Used to interact with the element and to register mouse binds.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isCheckHovered(mouseX, mouseY) && !listening) {
            listening = true
            return true
        } else if (listening) {
            setting.value.key = -100 + mouseButton
            listening = false
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Register key strokes. Used to set the key bind.
     */
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                setting.value.key = Keyboard.KEY_NONE
                listening = false
            } else if (keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                listening = false
            } else if (!keyBlackList.contains(keyCode)) {
                setting.value.key = keyCode
                listening = false
            }
            return true
        }
        return super.keyTyped(typedChar, keyCode)
    }

    /**
     * Checks whether this element is hovered
     */
    private fun isCheckHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + height
    }
}