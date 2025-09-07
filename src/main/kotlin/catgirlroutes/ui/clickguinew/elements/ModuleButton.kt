package catgirlroutes.ui.clickguinew.elements

import catgirlroutes.CatgirlRoutes.Companion.RESOURCE_DOMAIN
import catgirlroutes.module.Module
import catgirlroutes.module.impl.render.ClickGui
import catgirlroutes.module.settings.impl.*
import catgirlroutes.ui.animations.impl.ColorAnimation
import catgirlroutes.ui.animations.impl.LinearAnimation
import catgirlroutes.ui.clickgui.elements.ModuleButton.Companion.haramIcon
import catgirlroutes.ui.clickgui.elements.ModuleButton.Companion.whipIcon
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.MouseUtils.mouseX
import catgirlroutes.ui.clickgui.util.MouseUtils.mouseY
import catgirlroutes.ui.clickguinew.Window
import catgirlroutes.ui.clickguinew.Window.Companion.SCROLL_DISTANCE
import catgirlroutes.ui.clickguinew.elements.menu.*
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawTexturedRect
import net.minecraft.client.gui.GuiScreen.isShiftKeyDown
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class ModuleButton(val module: Module, val window: Window) {
    val menuElements: ArrayList<Element<*>> = ArrayList()

    var x = 0.0
    var y = 0.0

    val width = this.window.width
    val height: Double = 20.0

    var extended = false

    val xAbsolute: Double
        get() = x + window.x
    val yAbsolute: Double
        get() = y + window.y

    private val elementsHeight get() = this.menuElements.sumOf { it.getElementHeight() + 5.0 }

    private val colourAnimation = ColorAnimation(100)
    private val scrollAnimation = LinearAnimation<Double>(150)

    private var scrollTarget = 0.0
    private var scrollOffset = 0.0

    private var listeningKey = false
    private lateinit var keySetting: KeyBindSetting

    init {
        updateElements()
    }

    fun updateElements() {
        var position = -1
        var showInListSetting: BooleanSetting? = null
        for (setting in module.settings) {
            if ((setting.visibility.visibleInClickGui || setting.visibility.visibleInAdvanced) && setting.shouldBeVisible) run addElement@{
                if (setting is BooleanSetting && setting.name.equals("Show in array list", true)) {
                    showInListSetting = setting
                    return@addElement
                }
                if (this.menuElements.any { it.setting === setting }) return@addElement
                position++
                val newElement = when (setting) {
                    is BooleanSetting ->    ElementBoolean(this, setting)
                    is NumberSetting ->     ElementSlider(this, setting)
                    is SelectorSetting ->   ElementSelector(this, setting)
                    is StringSetting ->     ElementTextField(this, setting)
                    is ColorSetting ->      ElementColor(this, setting)
                    is ActionSetting ->     ElementAction(this, setting)
                    is KeyBindSetting ->    ElementKeyBind(this, setting)
                    is DropdownSetting ->   ElementDropdown(this, setting)
                    is HudSetting ->        ElementHud(this, setting)
                    is OrderSetting ->      ElementOrder(this, setting)
                    else -> return@addElement
                }
                try { // for now ig
                    if (position >= 0 && position <= this.menuElements.size) {
                        this.menuElements.add(position, newElement)
                    } else {
                        this.menuElements.add(newElement)
                    }
                } catch (e: IndexOutOfBoundsException) {
                    this.menuElements.add(newElement)
                }
            } else {
                this.menuElements.removeIf { it.setting === setting }
            }
        }
        this.menuElements.last { it.setting is KeyBindSetting }
            .let { element ->
                this.keySetting = element.setting as KeyBindSetting
                this.menuElements.remove(element)
            }
        showInListSetting?.let { captured ->
            this.menuElements.removeIf { it.setting === captured || it.displayName.equals("Show in array list", true) }
            this.menuElements.add(ElementBoolean(this, captured))
        }
    }

    fun draw() : Double {
        GlStateManager.pushMatrix()

        if (!this.window.inModule) {
            GlStateManager.translate(x, y, 0.0)
            val colour = this.colourAnimation.get(ColorUtil.outlineColor, ColorUtil.bgColor, this.module.enabled)
            val isHovered = isButtonHovered()
            val renderColor = if (isHovered && !this.module.enabled) ColorUtil.clickGUIColor.withAlpha(50) else colour
            drawRoundedBorderedRect(0.0, 0.0, this.width, this.height, 3.0, 1.0, renderColor, ColorUtil.outlineColor)
            FontUtil.drawString(module.name, 9.5, 2 + fontHeight / 2.0)

            if (this.menuElements.isNotEmpty()) {
                drawTexturedRect(dotsIcon, this.width - 18.5, 2.5, 16.0, 16.0)
            }

            val keyName = if (this.listeningKey) "..."
            else if (this.keySetting.value.key > 0) Keyboard.getKeyName(this.keySetting.value.key) ?: "Err"
            else if (this.keySetting.value.key < 0) Mouse.getButtonName(this.keySetting.value.key + 100)
            else "NONE"
            FontUtil.drawString("[${keyName}]", this.width - 25.5 - FontUtil.getStringWidth("[${keyName}]"), 2 + fontHeight / 2.0)

            if (isHovered && this.module.description.isNotBlank()) {
                this.window.clickGui.run {
                    if (!selectedWindow.inModule) {
                        hoveredModuleDesc = module.description
                    }
                }
            }

            when (this.module.tag) {
                Module.TagType.HARAM -> drawTexturedRect(haramIcon, FontUtil.getStringWidth(this.module.name) + 15.0, 1.5, 17.0, 17.0)
                Module.TagType.WHIP -> drawTexturedRect(whipIcon, FontUtil.getStringWidth(this.module.name) + 15.0, 1.5, 18.0, 18.0)
                Module.TagType.NONE -> {}
            }
            GlStateManager.popMatrix()
            return this.height + 5.0
        }

        if (this.extended) {
            this.scrollOffset = this.scrollAnimation.get(this.scrollOffset, this.scrollTarget)

            val titleY = this.scrollOffset
            FontUtil.drawString(module.name, 6.0, titleY, ColorUtil.clickGUIColor.rgb)
            catgirlroutes.utils.render.HUDRenderUtils.renderRect(0.0, titleY + fontHeight + 2.0, this.width, 1.0, ColorUtil.outlineColor)

            var drawY = titleY + fontHeight + 8.0
            this.menuElements.forEach {
                it.y = drawY
                it.update()
                drawY += it.draw()
            }
        }
        GlStateManager.popMatrix()
        return 0.0
    }

    fun scroll(amount: Int): Boolean {
        if (!this.extended || !this.window.isHovered()) return false
        val chipH = 12.0
        val spacing = 3.0
        val headerH = fontHeight + 8.0
        val h = this.elementsHeight + 15.0 + headerH
        if (h < this.window.height) {
            if (this.scrollTarget != 0.0) {
                this.scrollTarget = 0.0
                this.scrollAnimation.start(true)
            }
            return false
        }
        val newTarget = (this.scrollTarget + amount * SCROLL_DISTANCE).coerceIn(-h + this.window.height, 0.0)
        if (newTarget != this.scrollTarget) {
            this.scrollTarget = newTarget
            this.scrollAnimation.start(true)
        }
        return true
    }

    fun mouseClicked(mouseButton: Int): Boolean {
        if (this.listeningKey) {
            this.keySetting.value.key = -100 + mouseButton
            this.listeningKey = false
            return true
        }
        if (this.extended) {
            val chipPadding = 5.0
            val chipTextW = FontUtil.getStringWidthDouble(module.name)
            val chipW = (chipTextW + chipPadding * 2).coerceAtMost(this.width * 0.8)
            val chipH = 12.0
            val chipXAbs = this.window.x + this.width - chipW - 6.0
            val chipYAbs = this.yAbsolute + this.scrollOffset
            val toggleXAbs = chipXAbs - TOGGLE_W - 6.0
            val toggleYAbs = chipYAbs + (chipH - TOGGLE_H) / 2.0
            val mx = mouseX.toDouble()
            val my = mouseY.toDouble()
            if (mx >= toggleXAbs && mx <= toggleXAbs + TOGGLE_W && my >= toggleYAbs && my <= toggleYAbs + TOGGLE_H) {
                this.module.showInArrayList = !this.module.showInArrayList
                return true
            }
        }

        return when {
            isButtonHovered() && (!this.window.inModule) -> when (mouseButton) {
                0 -> {
                    when {
                        isShiftKeyDown() -> {
                            this.listeningKey = true
                            true
                        }
                        !this.listeningKey -> {
                            this.module.toggle()
                            this.colourAnimation.start()
                            true
                        }
                        else -> false
                    }
                }
                1 -> {
                    if (this.listeningKey) return false
                    this.menuElements.takeIf { it.isNotEmpty() }?.let { elements ->
                        this.extended = true
                        elements.forEach { it.listening = false }
                        true
                    } ?: false
                }
                2 -> {
                    this.listeningKey = true
                    true
                }
                else -> false
            }
            this.isMouseUnderButton() -> this.menuElements.reversed().any {
                it.mouseClicked(mouseButton).also { clicked ->
                    if (clicked) {
                        if (it.parent.module.name == "ClickGUI" && it.displayName == "ClickGui") {
                            ClickGui.onEnable()
                        }
                        updateElements()
                    }
                }
            }
            else -> false
        }
    }

    fun mouseReleased(state: Int) {
        if (this.extended) this.menuElements.reversed().forEach { it.mouseReleased(state) }
    }

    fun mouseClickMove(mouseButton: Int, timeSinceLastClick: Long) {
        if (this.extended) this.menuElements.reversed().forEach { it.mouseClickMove(mouseButton, timeSinceLastClick) }
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (this.extended) {
            this.menuElements.reversed().forEach {
                if (it.keyTyped(typedChar, keyCode)) return true
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.extended = false
                return true
            }
            if (!this.menuElements.any { it.listening }) when (keyCode) {
                Keyboard.KEY_UP -> this.scroll(1)
                Keyboard.KEY_DOWN -> this.scroll(-1)
            }
        }

        if (!this.listeningKey) return false
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                this.keySetting.value.key = Keyboard.KEY_NONE
                this.listeningKey = false
            }
            Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> {
                this.listeningKey = false
            }
            else -> {
                this.keySetting.value.key = keyCode
                this.listeningKey = false
            }
        }
        return true
    }

    fun onGuiClosed() {
        this.menuElements.reversed().forEach { it.onGuiClosed() }
    }

    private fun isButtonHovered(): Boolean {
        return (!this.extended || !this.window.inModule) && mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + this.height
    }

    private fun isMouseUnderButton(): Boolean {
        return this.extended && mouseX >= this.window.x && mouseX <= this.window.x + this.width && mouseY > yAbsolute
    }

    companion object {
        val dotsIcon = ResourceLocation(RESOURCE_DOMAIN, "dots.png")
        private const val TOGGLE_W = 12.0
        private const val TOGGLE_H = 8.0
    }
}