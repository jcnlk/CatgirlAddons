package catgirlroutes.ui

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import catgirlroutes.CatgirlRoutes.Companion.mc as mainMc

abstract class Screen(private val forceScale: Boolean = true) : GuiScreen() {

    var sr = ScaledResolution(mainMc)
    var scale = CLICK_GUI_SCALE / sr.scaleFactor

    final override fun initGui() {
        sr = ScaledResolution(mc)
        scale = CLICK_GUI_SCALE / sr.scaleFactor
        onInit()
    }

    open fun onInit() {  }

    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (forceScale) {
            GlStateManager.pushMatrix()
            val prevScale = mc.gameSettings.guiScale
            mc.gameSettings.guiScale = 2
            GlStateManager.scale(scale, scale, scale)

            draw()

            GlStateManager.popMatrix()
            mc.gameSettings.guiScale = prevScale
        } else {
            draw()
        }
    }

    open fun draw() {  }

    final override fun handleMouseInput() {
        super.handleMouseInput()
        val scrollEvent = Mouse.getEventDWheel()
        if (scrollEvent != 0) {
            var amount = scrollEvent.coerceIn(-1, 1)
            if (isShiftKeyDown()) amount *= 7
            onScroll(amount)
        }
    }

    open fun onScroll(amount: Int) {  }

    final override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        onMouseClick(mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    open fun onMouseClick(mouseButton: Int) {  }

    final override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        onMouseClickMove(clickedMouseButton, timeSinceLastClick)
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    open fun onMouseClickMove(mouseButton: Int, timeSinceLastClick: Long) {  }

    final override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        onMouseRelease(state)
        super.mouseReleased(mouseX, mouseY, state)
    }

    open fun onMouseRelease(state: Int) {  }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        handleKeyScroll(keyCode)
        super.keyTyped(typedChar, keyCode)
    }

    final override fun doesGuiPauseGame(): Boolean = false

    private fun handleKeyScroll(keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_UP -> onScroll(1)
            Keyboard.KEY_DOWN -> onScroll(-1)
        }
    }

    fun getX(guiWidth: Double): Double = sr.scaledWidth / (2.0 * scale) - guiWidth / 2.0
    fun getY(guiHeight: Double): Double = sr.scaledHeight / (2.0 * scale) - guiHeight / 2.0

    companion object {
        const val CLICK_GUI_SCALE = 2.0
    }
}