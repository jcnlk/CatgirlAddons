package catgirlroutes.ui.clickgui

import catgirlroutes.CatgirlRoutes
import catgirlroutes.CatgirlRoutes.Companion.moduleConfig
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.impl.render.ClickGui
import catgirlroutes.ui.clickgui.advanced.AdvancedMenu
import catgirlroutes.ui.clickgui.elements.menu.ElementColor
import catgirlroutes.ui.clickgui.elements.menu.ElementSlider
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.io.IOException

/**
 * ## Main class of the Click GUI.
 *
 * Provides the gui which can be viewed in game.
 *
 * This class dispatches all rendering and input actions to the components of the GUI.
 *
 * Structure of the GUI is:
 * [ClickGUI] -> [Panel]s -> [ModuleButton][catgirlroutes.ui.clickgui.elements.ModuleButton]s
 * -> [Element][catgirlroutes.ui.clickgui.elements.Element]s.
 * Each component of the gui handles it own actions and dispatches them to its subcomponents.
 *
 * Partially based on HeroCode's gui.
 * The only reference to it, and my source for it is [this YouTube video](https://www.youtube.com/watch?v=JPb5rBzUVKE).
 *
 * @author Aton
 */
class ClickGUI : GuiScreen() {
    var scale = 2.0
    /**
     * Used to add a delay for closing the gui, so that it does not instantly get closed
     */
    private var openedTime = System.currentTimeMillis()
    /**
     * Used to create the advanced menu for modules
     */
    var advancedMenu: AdvancedMenu? = null
    private var searchTextField: GuiTextField? = null
    var searchText = ""

    init {
        FontUtil.setupFontUtils()
        setUpPanels()
    }

    fun setUpPanels() {
        /** Create a panel for each module category */
        panels = ArrayList()
        for (category in Category.values()) {
            panels.add(Panel(category, this))
        }
    }

    /**
     * Dispatches all rendering for the GUI.
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {

        // Scale the gui and the mouse coordinates
        // the handling of the mouse coordinates is not nice, since it has to be done in multiple places
        val scaledresolution = ScaledResolution(mc)
        val prevScale = mc.gameSettings.guiScale
        scale = CLICK_GUI_SCALE / scaledresolution.scaleFactor
        mc.gameSettings.guiScale = 2
        GL11.glScaled(scale, scale, scale)

        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        renderLogo()
        renderSearchBar(scaledMouseX, scaledMouseY)

        /* Calls all panels to render themselves and their module buttons and elements.
		  * Important to keep in mind: the panel rendered last will be on top.
          * For intuitive behaviour the panels have to be checked in reversed order for clicks.
          * This ensures that interactions will happen with the top panel. */
        for (p in panels) {
            p.drawScreen(scaledMouseX, scaledMouseY, partialTicks)
        }

        if(ClickGui.showUsageInfo) {
            renderUsageInfo()
        }

        if (advancedMenu != null) {
            advancedMenu?.drawScreen(scaledMouseX, scaledMouseY, partialTicks)
        }

        /** Might be needed to use gui buttons */
        super.drawScreen(scaledMouseX, scaledMouseY, partialTicks)

        mc.gameSettings.guiScale = prevScale
    }

    /**
     * Draws the Logo and the title.
     */
    private fun renderLogo() {
        val scaledResolution = ScaledResolution(mc)
        val logoSize = 25

        GL11.glPushMatrix()
        GL11.glTranslated(
            scaledResolution.scaledWidth.toDouble(),
            scaledResolution.scaledHeight.toDouble(),
            0.0
        )

        GL11.glScaled(2.0, 2.0, 2.0)
        val titleWidth = FontUtil.getStringWidth(ClickGui.guiName)

        GlStateManager.color(255f, 255f, 255f, 255f)
        CatgirlRoutes.mc.textureManager.bindTexture(LOGO)
        Gui.drawModalRectWithCustomSizedTexture(
            - 5 - logoSize,
            -5 - logoSize,
            0f, 0f, logoSize, logoSize, logoSize.toFloat(), logoSize.toFloat()
        )

        FontUtil.drawString(
            ClickGui.guiName,
            -titleWidth.toDouble() - 10.0 - logoSize,
            -FontUtil.fontHeight.toDouble() / 2.0 - 5.0 - logoSize / 2.0,
            ColorUtil.clickGUIColor.rgb
        )
        GL11.glPopMatrix()
    }
    
    private fun renderSearchBar(mouseX: Int, mouseY: Int) {
        val scaledResolution = ScaledResolution(mc)
        val searchWidth = 180
        val searchHeight = 20

        val searchX = (scaledResolution.scaledWidth - searchWidth) / 2
        val searchY = scaledResolution.scaledHeight - searchHeight - 40
        
        if (searchTextField == null) {
            searchTextField = GuiTextField(0, mc.fontRendererObj, searchX, searchY, searchWidth, searchHeight)
            searchTextField?.maxStringLength = 50
            searchTextField?.text = searchText
            searchTextField?.setEnableBackgroundDrawing(false)
            searchTextField?.setTextColor(0xFFFFFFFF.toInt())
            searchTextField?.setDisabledTextColour(0xFFAAAAAA.toInt())
        }
        
        searchTextField?.xPosition = searchX
        searchTextField?.yPosition = searchY

        GlStateManager.pushMatrix()
        GlStateManager.scale(1.0/scale, 1.0/scale, 1.0)

        val unscaledX = (searchX * scale).toInt()
        val unscaledY = (searchY * scale).toInt()
        val unscaledWidth = (searchWidth * scale).toInt()
        val unscaledHeight = (searchHeight * scale).toInt()

        val focused = searchTextField?.isFocused == true
        val borderColor = if (focused) ColorUtil.clickGUIColor.rgb else ColorUtil.outlineColor.rgb
        val (bgColor, textColor, placeholderColor) = when {
            ClickGui.design.isSelected("JellyLike") -> {
                val bg = java.awt.Color(255, 255, 255, 100).rgb
                Triple(bg, 0xFF202020.toInt(), 0xFF7A7A7A.toInt())
            }
            ClickGui.design.isSelected("Glass") -> {
                val bg = java.awt.Color(0, 0, 0, 110).rgb
                Triple(bg, 0xFFFFFFFF.toInt(), 0xFFB0B0B0.toInt())
            }
            ClickGui.design.isSelected("Minimal") -> {
                val bg = ColorUtil.bgColor.rgb
                Triple(bg, 0xFFEFEFEF.toInt(), 0xFFB0B0B0.toInt())
            }
            ClickGui.design.isSelected("Outline") -> {
                val bg = java.awt.Color(0, 0, 0, 0).rgb
                Triple(bg, 0xFFFFFFFF.toInt(), 0xFFAAAAAA.toInt())
            }
            else -> { // New / default
                val bg = ColorUtil.bgColor.brighter().rgb
                Triple(bg, 0xFFFFFFFF.toInt(), 0xFFAAAAAA.toInt())
            }
        }

        Gui.drawRect(unscaledX - 2, unscaledY - 2, unscaledX + unscaledWidth + 2, unscaledY + unscaledHeight + 2, borderColor)
        Gui.drawRect(unscaledX, unscaledY, unscaledX + unscaledWidth, unscaledY + unscaledHeight, bgColor)

        val leftPadding = 8
        val rightPadding = 8
        searchTextField?.xPosition = unscaledX + leftPadding
        searchTextField?.yPosition = unscaledY + 2
        searchTextField?.width = unscaledWidth - (leftPadding + rightPadding)
        searchTextField?.height = unscaledHeight - 4
        searchTextField?.setTextColor(textColor)
        searchTextField?.drawTextBox()

        val font = mc.fontRendererObj
        if (searchTextField?.text?.isEmpty() == true && !searchTextField!!.isFocused) {
            val baseline = unscaledY + (unscaledHeight - font.FONT_HEIGHT) / 2
            font.drawString("Search...", unscaledX + leftPadding, baseline, placeholderColor)
        }

        GlStateManager.popMatrix()

        searchText = searchTextField?.text ?: ""
    }

    private fun renderUsageInfo() {
        val scaledResolution = ScaledResolution(mc)

        val lines = listOf("GUI Usage:",
            "Left click Module Buttons to toggle the Module.",
            "Right click Module Buttons to extend the Settings dropdown.",
            "Middle click Module Buttons to open the Advanced Gui.",
            "Press Ctrl+F to focus the search bar.",
            "Disable this Overlay in the Advanced Settings of the Click Gui Module in the Render Category."
        )

        GL11.glPushMatrix()
        GL11.glTranslated(
            scaledResolution.scaledWidth.toDouble()*0.1,
            scaledResolution.scaledHeight.toDouble()*0.7,
            0.0
        )

        GL11.glScaled(1.5, 1.5, 1.5)
        for ((ii, line) in lines.withIndex()) {
            FontUtil.drawString(
                line,
                0.0,
                FontUtil.fontHeight.toDouble() * ii,
                ColorUtil.clickGUIColor.rgb
            )
        }
        GL11.glPopMatrix()
    }

    /**
     * Handles scrolling.
     */
    @Throws(IOException::class)
    override fun handleMouseInput() {
        super.handleMouseInput()
        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        var i = Mouse.getEventDWheel()
        if (i != 0) {
            if (i > 1) {
                i = 1
            }
            if (i < -1) {
                i = -1
            }
            if (isShiftKeyDown()) {
                i *= 7
            }
            // Scroll the advanced gui
            if (advancedMenu?.scroll(i, scaledMouseX, scaledMouseY) == true) return

            /** Checking all panels for scroll action.
             * Reversed order is used to guarantee that the panel rendered on top will be handled first. */
            for (panel in panels.reversed()) {
                if (panel.scroll(i, scaledMouseX, scaledMouseY)) return
            }
        }
    }

    /**
     * Dispatches mouse clicks to the [panels] and [advancedMenu].
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        // handle the advanced gui first
        searchTextField?.mouseClicked(mouseX, mouseY, mouseButton)
        
        if (advancedMenu?.mouseClicked(scaledMouseX, scaledMouseY, mouseButton) == true){
            // Update the elements of the corresponding module button
            val module = advancedMenu?.module ?: return
            panels.find { it.category == module.category }?.moduleButtons?.find { it.module == module }?.updateElements()
            return
        }

        /** Checking all panels for click action.
          * Reversed order is used to guarantee that the panel rendered on top will be handled first. */
        for (panel in panels.reversed()) {
            if (panel.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)) return
        }

        try {
            super.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        // handle mouse release for advanced menu first
        advancedMenu?.mouseReleased(scaledMouseX, scaledMouseY, state)

        /** Checking all panels for mouse release action.
         * Reversed order is used to guarantee that the panel rendered on top will be handled first. */
        for (panel in panels.reversed()) {
            panel.mouseReleased(scaledMouseX, scaledMouseY, state)
        }

        super.mouseReleased(scaledMouseX, scaledMouseY, state)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (searchTextField?.textboxKeyTyped(typedChar, keyCode) == true) return

        /** If in an advanced menu only hande that */
        if (advancedMenu != null) {
            if (keyCode == Keyboard.KEY_ESCAPE && !advancedMenu!!.isListening()) {
                advancedMenu = null
            }
            advancedMenu?.keyTyped(typedChar,keyCode)
            return
        }

        /** For key registration in the menu elements. Required for text fields.
         * Reversed order to check the panel on top first! */
        for (panel in panels.reversed()) {
            if (panel.keyTyped(typedChar, keyCode)) return
        }

        /** Handle Ctrl+F for search focus (keep text) */
        if (keyCode == Keyboard.KEY_F && isCtrlKeyDown()) {
            searchTextField?.isFocused = true
            return
        }

        /** Handle Escape: just unfocus (keep text) */
        if (keyCode == Keyboard.KEY_ESCAPE && searchTextField?.isFocused == true) {
            searchTextField?.isFocused = false
            return
        }

        /** Exits the menu when the toggle key is pressed */
        if (keyCode == ClickGui.settings.last().value && System.currentTimeMillis() - openedTime > 200) {
            mc.displayGuiScreen(null as GuiScreen?)
            if (mc.currentScreen == null) {
                mc.setIngameFocus()
            }
            return
        }

        /** keyTyped in GuiScreen gets used to exit the gui on escape */
        try {
            super.keyTyped(typedChar, keyCode)
        } catch (e2: IOException) {
            e2.printStackTrace()
        }
    }

    override fun initGui() {
        openedTime = System.currentTimeMillis()
        /** Start blur */
        if (OpenGlHelper.shadersSupported && mc.renderViewEntity is EntityPlayer && ClickGui.blur) {
            mc.entityRenderer.stopUseShader()
            mc.entityRenderer.loadShader(ResourceLocation("shaders/post/blur.json"))
        }

        /** update panel positions to make it possible to update the positions
         * this is required for loading the panel positions from the config and for resetting the gui */
        for (panel in panels) {
            panel.x = ClickGui.panelX[panel.category]!!.value.toInt()
            panel.y = ClickGui.panelY[panel.category]!!.value.toInt()
            panel.extended = ClickGui.panelExtended[panel.category]!!.enabled
        }
    }

    override fun onGuiClosed() {
        /** End blur */
        mc.entityRenderer.stopUseShader()

        /** stop sliders from being active */
        for (panel in panels.reversed()) {
            if (panel.extended && panel.visible) {
                for (moduleButton in panel.moduleButtons) {
                    if (moduleButton.extended) {
                        for (menuElement in moduleButton.menuElements) {
                            if (menuElement is ElementSlider) {
                                menuElement.dragging = false
                            }
                            if (menuElement is ElementColor) {
                                menuElement.dragging = null
                            }
                            if (menuElement is ElementColor) {
                                menuElement.dragging = null
                            }
                        }
                    }
                }
            }
        }

        /** Save the changes to the config file */
        moduleConfig.saveConfig()
    }

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    fun closeAllSettings() {
        for (panel in panels) {
            if (panel.visible && panel.extended && panel.moduleButtons.size > 0) {
                for (moduleButton in panel.moduleButtons) {
                    moduleButton.extended = false
                }
            }
        }
    }

    fun getScaledMouseX(): Int {
        return MathHelper.ceiling_double_int(Mouse.getX() / CLICK_GUI_SCALE)
    }
    fun getScaledMouseY(): Int {
        // maybe -1 or floor required here because of the inversion.
        return MathHelper.ceiling_double_int( (mc.displayHeight - Mouse.getY()) / CLICK_GUI_SCALE)
    }

    fun openModule(module: Module) {
        val panel = panels.first { it.category == module.category }
        panel.moduleButtons.first { it.module.name == module.name }.extended = true
    }

    companion object {
        const val CLICK_GUI_SCALE = 2.0
        var panels: ArrayList<Panel> = arrayListOf()

        private val LOGO = ResourceLocation(CatgirlRoutes.RESOURCE_DOMAIN, "Icon.png")
    }
}