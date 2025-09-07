package catgirlroutes.ui.misc.elements

import catgirlroutes.utils.render.Radii
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

abstract class MiscElement(open val style: MiscElementStyle) { // TODO fucking recode this shit it's disgusting
    var value: String // todo make better idk
        get() = style.value
        set(v) { updates.value = v }

    var x: Double
        get() = style.x
        set(v) { updates.x = v }

    var y: Double
        get() = style.y
        set(v) { updates.y = v }

    var width: Double
        get() = style.width
        set(v) { updates.width = v }

    var height: Double
        get() = style.height
        set(v) { updates.height = v }

    var thickness: Double
        get() = style.thickness
        set(v) { updates.thickness = v }

    var radii: Radii
        get() = style.radii
        set(v) { updates.radii = v }

    var colour: Color
        get() = style.colour
        set(v) { updates.colour = v }

    var hoverColour: Color
        get() = style.hoverColour
        set(v) { updates.hoverColour = v }

    var outlineColour: Color
        get() = style.outlineColour
        set(v) { updates.outlineColour = v }

    var outlineHoverColour: Color
        get() = style.outlineHoverColour
        set(v) { updates.outlineHoverColour = v }

    val updates: MiscElementStyle get() = style

    var onClickAction: (Int) -> Unit = { _ -> }
    var onRenderAction: () -> Unit = {  }
    var onHoverAction: () -> Unit = {  }
    var onKeyAction: (Char, Int) -> Unit = { _, _ -> }

    protected open fun render(mouseX: Int, mouseY: Int) {  }
    protected open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean = false
    open fun mouseClickMove(mouseX: Int, mouseY: Int, mouseButton: Int, timeSinceLastClick: Long) {  }
    protected open fun keyTyped(typedChar: Char, keyCode: Int): Boolean = false
    open fun onScroll(mouseX: Int, mouseY: Int, amount: Int): Boolean = false // todo add scrollable shit

    fun draw(mouseX: Int = 0, mouseY: Int = 0) {
        GlStateManager.pushMatrix()
        this.render(mouseX, mouseY)
        this.onRenderAction.invoke()
        if (this.isHovered(mouseX, mouseY)) this.onHoverAction.invoke()
        GlStateManager.popMatrix()
    }

    fun onMouseClick(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        val isClicked = mouseClicked(mouseX, mouseY, mouseButton)
        if (isClicked) this.onClickAction.invoke(mouseButton)
        return isClicked
    }

    fun onKey(typedChar: Char, keyCode: Int): Boolean {
        val isTyped = keyTyped(typedChar, keyCode)
        if (isTyped) this.onKeyAction.invoke(typedChar, keyCode)
        return isTyped
    }

    open fun isHovered(mouseX: Int, mouseY: Int, xOff: Double = 0.0, yOff: Double = 0.0): Boolean {
        return mouseX >= x + xOff && mouseX <= x + width + xOff &&
                mouseY >= y + yOff && mouseY <= y + height + yOff
    }

    companion object {
        var currentlyFocused: MiscElement? = null
    }
}

abstract class ElementDSL<T : MiscElement> {
    val _style = MiscElementStyle()

    var _x by _style::x
    var _y by _style::y
    var width by _style::width
    var height by _style::height
    var thickness by _style::thickness
    var radius by _style::radius
    var radii by _style::radii
    var textColour by _style::textColour
    var textShadow by _style::textShadow
    var colour by _style::colour
    var hoverColour by _style::hoverColour
    var outlineColour by _style::outlineColour
    var outlineHoverColour by _style::outlineHoverColour
    var alignment by _style::alignment
    var vAlignment by _style::vAlignment
    var textPadding by _style::textPadding

    lateinit var element: MiscElement

    private var onClickAction: (Int) -> Unit = { _ -> }
    private var onRenderAction: () -> Unit = {  }
    private var onHoverAction: () -> Unit = {  }
    private var onKeyAction: (Char, Int) -> Unit = { _, _ -> }

    fun at(x: Number, y: Number) {
        this._x = x.toDouble()
        this._y = y.toDouble()
    }

    fun size(width: Number, height: Number) {
        this.width = width.toDouble()
        this.height = height.toDouble()
    }

    fun onRender(action: () -> Unit) {
        this.onRenderAction = action
    }

    fun onHover(action: () -> Unit) {
        this.onHoverAction = action
    }

    fun onClick(action: (Int) -> Unit) {
        this.onClickAction = action
    }

    fun onKey(action: (Char, Int) -> Unit) {
        this.onKeyAction = action
    }

    protected open fun createStyle(): MiscElementStyle = _style
    protected abstract fun buildElement(): T
    fun build(): T {
        val built = buildElement()
        built.onClickAction = onClickAction
        built.onRenderAction = onRenderAction
        built.onHoverAction = onHoverAction
        built.onKeyAction = onKeyAction
        element = built
        return built
    }
}
