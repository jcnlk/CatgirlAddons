package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.OrderSetting
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.drawString
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.ui.misc.elements.impl.MiscElementButton
import catgirlroutes.ui.misc.elements.impl.button
import catgirlroutes.ui.misc.elements.util.update
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedOutline
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import catgirlroutes.utils.render.HUDRenderUtils.renderRect
import java.awt.Color
import java.util.*
import kotlin.math.ceil
import kotlin.math.hypot

class ElementOrder(parent: ModuleButton, setting: OrderSetting) :
    Element<OrderSetting>(parent, setting, ElementType.ORDER) {

    private val maxWidth get() = if (setting.options.isEmpty()) this.width else this.width * 0.9
    private val minWidth = 10.0

    private lateinit var placeholders: List<String>
    private lateinit var values: MutableList<String>
    private lateinit var options: List<String>

    private val elementsPerRow = setting.valuesPerRow
    private val buttons = mutableListOf<MiscElementButton>()
    private var draggedButton: MiscElementButton? = null
    private var hoveredButton: MiscElementButton? = null
    private var offsetX = 0.0
    private var offsetY = 0.0

    private val optionButtons = mutableListOf<MiscElementButton>()
    private var draggedOption: MiscElementButton? = null
    private val optionsListWidth get() = if (options.isEmpty()) 0.0 else this.parent.width - maxWidth
    private val optionElementHeight get() = DEFAULT_HEIGHT

    private val elementWidth: Double get() {
        val availableWidth = (maxWidth - (PADDING * (elementsPerRow - 1)) - 10.0) / elementsPerRow
        return availableWidth.coerceIn(minWidth, maxWidth)
    }

    private val elementsHeight: Double get() {
        val numRows = ceil(setting.value.size.toDouble() / elementsPerRow).toInt()
        return (numRows * ELEMENT_HEIGHT) + ((numRows - 1) * 5.0)
    }

    private val extraHeight get() = setting.updateAction?.let { DEFAULT_HEIGHT + 5.0 } ?: 0.0

    private val totalHeight get() = fontHeight + 3.0 + elementsHeight + 10.0 + extraHeight

    private val updateButton = button {
        text = "Update"
        size(maxWidth - 10.5, DEFAULT_HEIGHT)
        onClick {
            setting.update()
            updateElements()
            updateOptionElements()
        }
    }

    init {
        updateElements()
        updateOptionElements()
    }

    private fun updateElements() {
        placeholders = setting.placeholders.toList()
        values = setting.values.toMutableList()
        buttons.clear()
        values.forEachIndexed { i, value ->
            val pos = getPosIndex(i)
            buttons.add(button {
                text = value
                at(pos.first, pos.second)
                size(elementWidth, ELEMENT_HEIGHT)
                colour = ColorUtil.bgColor.darker()
                outlineColour = Color(208, 208, 208, 136)
                outlineHoverColour = Color(136, 255, 136)
            })
        }
    }

    private fun updateOptionElements() {
        options = setting.options
        optionButtons.clear()
        options.forEachIndexed { index, option ->
            optionButtons.add(button {
                text = option
                at(maxWidth + 5.0, fontHeight + 3.0 + PADDING + index * (optionElementHeight + PADDING))
                size(optionsListWidth - 10.5, optionElementHeight)
                colour = ColorUtil.bgColor.darker()
                outlineColour = Color(208, 208, 208, 136)
                outlineHoverColour = Color(136, 255, 136)
            })
        }
    }


    override fun renderElement(): Double {
        drawString(displayName, 0.0, 0.0)
        drawRoundedOutline(0.0, fontHeight + 3.0, maxWidth + optionsListWidth, elementsHeight + extraHeight + 10.0, 3.0, 1.0, ColorUtil.clickGUIColor)

        setting.updateAction?.let {
            run {
                val oc = ColorUtil.outlineColor
                val hc = ColorUtil.clickGUIColor
                if (updateButton.outlineColour != oc || updateButton.outlineHoverColour != hc) {
                    updateButton.update {
                        outlineColour = oc
                        outlineHoverColour = hc
                    }
                }
            }
            this.updateButton.update {
                x = 5.0
                y = elementsHeight + DEFAULT_HEIGHT + 10.0
            }.draw(mouseXRel, mouseYRel)
        }

        buttons.forEach { button ->
            if (button != draggedButton && button != hoveredButton) {
                val (origX, origY) = getPos(button)
                button.update {
                    x = origX
                    y = origY
                }.draw(mouseXRel, mouseYRel)
                drawPlaceholder(button)
            }
        }

        if (options.isNotEmpty()) {
            renderRect(maxWidth - 0.5, fontHeight + 3.0 + 5.0, 1.0, elementsHeight + extraHeight, ColorUtil.outlineColor)

            optionButtons.forEachIndexed { index, button ->
                button.update {
                    x = this@ElementOrder.maxWidth + 5.0
                    y = fontHeight + 3.0 + PADDING + index * (optionElementHeight + PADDING)
                }.draw(mouseXRel, mouseYRel)
            }
        }

        val dragged = draggedButton ?: draggedOption

        if (dragged != null) {
            hoveredButton?.let { hovered ->
                val hoveredPos = getPos(hovered)
                drawRoundedOutline(hoveredPos.first, hoveredPos.second, hovered.width, hovered.height, 3.0, 1.0, Color(255, 136, 136))

                if (draggedButton != null) {
                    val (xPos, yPos) = getPos(dragged)

                    hovered.update {
                        x = xPos
                        y = yPos
                    }.draw(mouseXRel, mouseYRel)
                }

                drawPlaceholder(hovered)
            }

            if (draggedButton != null) {
                val pos = getPos(dragged)
                drawRoundedRect(pos.first, pos.second, elementWidth, ELEMENT_HEIGHT, 3.0, Color(136, 136, 136, 128))
                drawPlaceholder(dragged)
            }

            dragged.update {
                x = mouseXRel - offsetX
                y = mouseYRel - offsetY
                colour = Color(136, 255, 136, 128)
            }.draw(mouseXRel, mouseYRel)
        }

        return totalHeight
    }

    override fun mouseClicked(mouseButton: Int): Boolean {

        if (options.isNotEmpty()) {
            optionButtons.forEach { button ->
                if (button.onMouseClick(mouseXRel, mouseYRel, mouseButton)) {
                    draggedOption = button
                    offsetX = mouseXRel - button.x
                    offsetY = mouseYRel - button.y
                    return true
                }
            }
        }

        buttons.forEach { button ->
            if (button.onMouseClick(mouseXRel, mouseYRel, mouseButton)) {
                draggedButton = button
                offsetX = mouseXRel - button.x
                offsetY = mouseYRel - button.y
                return true
            }
        }
        setting.updateAction?.let { return updateButton.onMouseClick(mouseXRel, mouseYRel, mouseButton) }
        return false
    }

    override fun mouseClickMove(mouseButton: Int, timeSinceLastClick: Long) {
        if (draggedButton != null || draggedOption != null) {
            hoveredButton = getHovered()
        }
    }

    override fun mouseReleased(state: Int) {

        draggedOption?.let { option ->
            hoveredButton?.let { hovered ->
                val hoveredIndex = buttons.indexOf(hovered)
                    val optionIndex = optionButtons.indexOf(option)
                    values[hoveredIndex] = setting.options[optionIndex]
                    hovered.value = values[hoveredIndex]
                    debugMessage(setting.value)
                    setting.value = placeholders.zip(values).toMap()
                    debugMessage("§b" + setting.value)
            }

            val optionIndex = optionButtons.indexOf(option)
            option.update {
                x = maxWidth + 5.0
                y = fontHeight + 3.0 + PADDING + optionIndex * (optionElementHeight + PADDING)
                width = optionsListWidth - 10.0
                height = optionElementHeight
                colour = ColorUtil.bgColor.darker()
            }

            draggedOption = null
        }

        draggedButton?.let { dragged ->
            hoveredButton?.let { hovered ->
                val draggedIndex = buttons.indexOf(dragged)
                val hoveredIndex = buttons.indexOf(hovered)
                Collections.swap(values, draggedIndex, hoveredIndex)
                Collections.swap(buttons, draggedIndex, hoveredIndex)
                debugMessage(setting.value)
                setting.value = placeholders.zip(values).toMap()
                debugMessage("§6" + setting.value)
            }

            buttons.forEach { button ->
                getPos(button).let { (x, y) ->
                    button.x = x
                    button.y = y
                }
            }

            dragged.colour = ColorUtil.bgColor.darker()
        }

        draggedButton = null
        hoveredButton = null
    }

    override fun onGuiClosed() {
        setting.onGuiClosed()
    }

    override fun getElementHeight(): Double {
        return totalHeight
    }

    private fun drawPlaceholder(button: MiscElementButton, x: Double? = null, y: Double? = null) {
        val i = buttons.indexOf(button)
        val (xPos, yPos) = if (x == null || y == null) getPos(button) else x to y
        val placeholder = placeholders[i]

        FontUtil.drawStringWithShadow(
            placeholder,
            xPos + (elementWidth - placeholder.getWidth()) / 2,
            yPos + ELEMENT_HEIGHT - 10,
            Color(170, 170, 170).rgb
        )
    }

    private fun getPos(button: MiscElementButton): Pair<Double, Double> {
        return getPosIndex(buttons.indexOf(button))
    }

    private fun getPosIndex(i: Int): Pair<Double, Double> {
        val row = i / elementsPerRow
        val col = i % elementsPerRow
        return 5.0 + col * (elementWidth + PADDING) to fontHeight + 3.0 + 5.0 + row * (ELEMENT_HEIGHT + PADDING)
    }

    private fun getHovered(): MiscElementButton? {
        var closestButton: MiscElementButton? = null
        var minDistance = if (draggedButton != null) SNAP_THRESHOLD else SNAP_THRESHOLD * 1.5

        val dragged = draggedButton ?: draggedOption
        if (dragged == null) return null

        val (dragX, dragY) = dragged.x + dragged.width / 2 to dragged.y + dragged.height / 2

        buttons.forEach { button ->
            if (button != draggedButton) {
                val (xPos, yPos) = getPos(button)
                val centerX = xPos + elementWidth / 2
                val centerY = yPos + ELEMENT_HEIGHT / 2
                val distance = hypot(dragX - centerX, dragY - centerY)

                if (distance < minDistance) {
                    minDistance = distance
                    closestButton = button
                }
            }
        }

        return closestButton
    }

    companion object {
        private const val ELEMENT_HEIGHT = 30.0
        private const val PADDING = 5.0
        private const val SNAP_THRESHOLD = 25.0
    }
}