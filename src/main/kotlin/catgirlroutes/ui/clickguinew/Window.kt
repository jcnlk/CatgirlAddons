package catgirlroutes.ui.clickguinew

import catgirlroutes.module.Category
import catgirlroutes.module.ModuleManager
import catgirlroutes.module.settings.SettingsCategory
import catgirlroutes.ui.animations.impl.LinearAnimation
import catgirlroutes.ui.clickgui.util.MouseUtils.mouseX
import catgirlroutes.ui.clickgui.util.MouseUtils.mouseY
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import kotlin.reflect.full.hasAnnotation

class Window(
    val category: Category,
    val clickGui: ClickGUI
) {
    val moduleButtons: ArrayList<ModuleButton> = ArrayList()

    var x = this.clickGui.x + this.clickGui.categoryWidth + 10.0
    var y = this.clickGui.y + 25.0 + 5.0

    val width = this.clickGui.guiWidth - this.clickGui.categoryWidth - 15.0
    val height = this.clickGui.guiHeight - 25.0

    private val selected: Boolean
        get() = this.clickGui.selectedWindow == this
    val inModule: Boolean get() = this.moduleButtons.any { it.extended }

    private var scrollTarget = 0.0
    private var scrollOffset = 0.0

    private val scrollAnimation = LinearAnimation<Double>(150)

    init {
        ModuleManager.modules
            .filter { (this.category == Category.SETTINGS && it::class.hasAnnotation<SettingsCategory>()) || it.category == this.category }
            .sortedBy { catgirlroutes.ui.clickgui.util.FontUtil.getStringWidth(it.name) }
            .forEach { this.moduleButtons.add(ModuleButton(it, this)) }
    }

    fun draw() {
        if (!this.selected) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0.0)

        this.scrollOffset = this.scrollAnimation.get(this.scrollOffset, this.scrollTarget)

        var drawY = this.scrollOffset
        this.moduleButtons.filtered().forEach {
            it.x = 0.0
            it.y = drawY
            drawY += it.draw()
        }

        GlStateManager.popMatrix()
    }

    fun mouseClicked(mouseButton: Int): Boolean {
        if (this.isHovered()) {
            this.moduleButtons.filtered().forEach {
                if (it.mouseClicked(mouseButton)) return true
            }
        }
        return false
    }

    fun mouseReleased(state: Int) {
        if (this.selected) this.moduleButtons.filtered().forEach { it.mouseReleased(state) }
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (this.selected) {
            this.moduleButtons.filtered().forEach {
                if (it.keyTyped(typedChar, keyCode)) return true
            }
        }
        if (!this.inModule) when (keyCode) {
            Keyboard.KEY_UP -> this.scroll(1)
            Keyboard.KEY_DOWN -> this.scroll(-1)
        }
        return false
    }

    fun mouseClickMove(clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (this.selected) this.moduleButtons.filtered().forEach { it.mouseClickMove(clickedMouseButton, timeSinceLastClick) }
    }

    fun onGuiClosed() {
        this.moduleButtons.filtered().forEach { it.onGuiClosed() }
    }

    fun scroll(amount: Int): Boolean {
        if (inModule || !isHovered()) return false
        val h = moduleButtons.filtered().size * 25.0 + 5.0
        if (h < this.height) {
            if (scrollTarget != 0.0) {
                scrollTarget = 0.0
                scrollAnimation.start(true)
            }
            return false
        }
        val newTarget = (scrollTarget + amount * SCROLL_DISTANCE).coerceIn(-h + this.height, 0.0)
        if (newTarget != scrollTarget) {
            scrollTarget = newTarget
            scrollAnimation.start(true)
        }
        return true
    }

    fun isHovered(): Boolean {
        if (!this.selected) return false
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height
    }

    private fun List<ModuleButton>.filtered() = filter { 
        clickGui.searchBar.text.isEmpty() || it.module.name.contains(clickGui.searchBar.text, true) 
    }.reversed()

    companion object {
        const val SCROLL_DISTANCE = 25
    }
}