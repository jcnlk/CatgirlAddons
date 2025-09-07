package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.KeyBindSetting
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.ui.misc.elements.impl.keyBind
import catgirlroutes.ui.misc.elements.util.update

class ElementKeyBind(parent: ModuleButton, setting: KeyBindSetting) :
    Element<KeyBindSetting>(parent, setting, ElementType.KEY_BIND) {

    private val stringWidth = displayName.getWidth()

    private val keyBind = keyBind {
        _x = stringWidth + 5.0
        height = this@ElementKeyBind.height
        keyCode = setting.value.key
        hoverColour = ColorUtil.buttonColor
    }

    override fun renderElement(): Double {
        FontUtil.drawString(displayName, 0.0, 1.0)
        run {
            val c = ColorUtil.clickGUIColor
            val oc = ColorUtil.outlineColor
            if (this.keyBind.colour != c || this.keyBind.outlineColour != oc) {
                this.keyBind.update {
                    colour = c
                    outlineColour = oc
                }
            }
        }
        this.keyBind.draw(mouseXRel, mouseYRel)
        return super.renderElement()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (keyBind.onMouseClick(mouseXRel, mouseYRel, mouseButton)) {
            this.setting.value.key = keyBind.keyCode
            return true
        }
        return false
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (keyBind.onKey(typedChar, keyCode)) {
            this.setting.value.key = keyBind.keyCode
            return true
        }
        return false
    }
}