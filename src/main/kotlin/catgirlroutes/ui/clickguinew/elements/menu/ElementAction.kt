package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil.capitalizeOnlyFirst
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.ui.misc.elements.impl.button
import catgirlroutes.ui.misc.elements.util.update

class ElementAction(parent: ModuleButton, setting: ActionSetting) :
    Element<ActionSetting>(parent, setting, ElementType.ACTION) {

    private val actionButton = button {
        text = displayName.capitalizeOnlyFirst()
        size(this@ElementAction.width, this@ElementAction.height)
        onClick { setting.doAction() }
    }

    override fun renderElement(): Double {
        run {
            val oc = ColorUtil.outlineColor
            val hc = ColorUtil.clickGUIColor
            if (this.actionButton.outlineColour != oc || this.actionButton.outlineHoverColour != hc) {
                this.actionButton.update {
                    outlineColour = oc
                    outlineHoverColour = hc
                }
            }
        }
        this.actionButton.draw(mouseXRel, mouseYRel)
        return super.renderElement()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        return this.actionButton.onMouseClick(mouseXRel, mouseYRel, mouseButton)
    }
}