package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.StringSetting
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.ui.misc.elements.impl.textField
import catgirlroutes.ui.misc.elements.util.update

class ElementTextField(parent: ModuleButton, setting: StringSetting) :
    Element<StringSetting>(parent, setting, ElementType.TEXT_FIELD) {

    private val textField = textField {
        at(0.0, fontHeight + 3.0)
        size(this@ElementTextField.width, 13.0)
        text = setting.text
        maxLength = setting.length
        placeholder = setting.placeholder
    }

    private var lastKnownText = setting.text

    override fun renderElement(): Double {
        if (setting.text != this.lastKnownText) {
            this.textField.text = setting.text
            this.lastKnownText = setting.text
        }

        FontUtil.drawString(displayName, 0.0, 0.0)

        if (this.textField.text != setting.text) {
            setting.text = this.textField.text
            this.lastKnownText = this.textField.text
        }
        run {
            val oc = ColorUtil.outlineColor
            val hc = ColorUtil.clickGUIColor
            if (this.textField.outlineColour != oc || this.textField.outlineHoverColour != hc) {
                this.textField.update {
                    outlineColour = oc
                    outlineHoverColour = hc
                }
            }
        }
        this.textField.draw(mouseXRel, mouseYRel)
        return super.renderElement()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        return this.textField.onMouseClick(mouseXRel, mouseYRel, mouseButton)
    }

    override fun mouseClickMove(mouseButton: Int, timeSinceLastClick: Long) {
        this.textField.mouseClickMove(mouseXRel, mouseYRel, mouseButton, timeSinceLastClick)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return this.textField.onKey(typedChar, keyCode)
    }

}