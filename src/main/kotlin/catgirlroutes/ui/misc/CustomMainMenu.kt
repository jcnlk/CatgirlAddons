package catgirlroutes.ui.misc

import catgirlroutes.CatgirlRoutes.Companion.RESOURCE_DOMAIN
import catgirlroutes.CatgirlRoutes.Companion.scope
import catgirlroutes.module.impl.misc.PhoenixAuth
import catgirlroutes.ui.Screen
import catgirlroutes.ui.clickgui.util.MouseUtils.mx
import catgirlroutes.ui.clickgui.util.MouseUtils.my
import catgirlroutes.ui.misc.elements.impl.MiscElementButton
import catgirlroutes.ui.misc.elements.impl.button
import catgirlroutes.utils.downloadImage
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawTexturedRect
import catgirlroutes.module.impl.render.ClickGui.customMenuPic
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import net.minecraft.world.storage.ISaveFormat
import net.minecraft.world.storage.SaveFormatComparator
import net.minecraftforge.fml.client.FMLClientHandler
import java.awt.Color
import java.awt.Desktop
import java.net.URI
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object CustomMainMenu: Screen(false) { // todo add more shit

    private var buttons = listOf<MiscElementButton>()
    private var customBackgroundTexture: ResourceLocation? = null
    private var catTexture: ResourceLocation? = null

    override fun onInit() {
        val list = mutableListOf<MiscElementButton>()
        var nextY = 10

        fun add(title: String, action: () -> Unit) {
            list.add(button(10, nextY, title, action))
            nextY += 25
        }

        add("Singleplayer") { mc.displayGuiScreen(GuiSelectWorld(mc.currentScreen)) }
        add("Multiplayer") { mc.displayGuiScreen(GuiMultiplayer(mc.currentScreen)) }
        add("Options") { mc.displayGuiScreen(GuiOptions(mc.currentScreen, mc.gameSettings)) }
        add("Last world") { joinLastSingleplayerWorld() }
        add("Hypixel") {
            try {
                FMLClientHandler.instance().setupServerList()
                FMLClientHandler.instance().connectToServer(this, ServerData("Hypixel", "mc.hypixel.net:25565", false))
            } catch (e: Exception) {
                println("Error connecting to Hypixel: ${e.message}")
                e.printStackTrace()
            }
        }

        if (PhoenixAuth.addToMainMenu) {
            add("Phoenix") {
                try {
                    FMLClientHandler.instance().setupServerList()
                    FMLClientHandler.instance().connectToServer(this, ServerData("Phoenix", PhoenixAuth.phoenixProxy, false))
                } catch (e: Exception) {
                    println("Error connecting to Phoenix: ${e.message}")
                    println("Phoenix proxy value: '${PhoenixAuth.phoenixProxy}'")
                    e.printStackTrace()
                }
            }
        }

        list.add(button(10, this@CustomMainMenu.height - 30, "Quit") { mc.shutdown() })

        list.add(button(this@CustomMainMenu.width - 210, this@CustomMainMenu.height - 30, "Open Github") {
            try {
                Desktop.getDesktop().browse(URI("https://github.com/jcnlk/CatgirlAddons"))
            } catch (e: Exception) {
                println("Failed to open GitHub: ${e.message}")
                e.printStackTrace()
            }
        })
        list.add(button(this@CustomMainMenu.width - 210, this@CustomMainMenu.height - 55, "Random cat picture") {
            downloadCatImage { catTexture = it }
        })

        buttons = list
    }

    override fun draw() {
        if (customMenuPic) {
            if (customBackgroundTexture == null) loadCustomBackground()

            val texture = customBackgroundTexture ?: ResourceLocation(RESOURCE_DOMAIN, "gui/custom_background.png")
            drawTexturedRect(texture, 0.0, 0.0, sr.scaledWidth_double, sr.scaledHeight_double)
        } else {
            drawTexturedRect(ResourceLocation(RESOURCE_DOMAIN, "gui/custom_background.png"), 0.0, 0.0, sr.scaledWidth_double, sr.scaledHeight_double)
        }
        catTexture?.let {
            drawRoundedRect(width - 210.0, 10.0, 200.0, 200.0, 3.0, Color(239, 137, 175))
            drawTexturedRect(it, width - 209.0, 11.0, 198.0, 198.0)
        }
        buttons.forEach { it.draw(mx, my) }
    }

    override fun onMouseClick(mouseButton: Int) {
        buttons.forEach { it.onMouseClick(mx, my, mouseButton) }
    }

    private fun downloadCatImage(callback: (ResourceLocation?) -> Unit) {
        scope.launch {
            val image = downloadImage("https://cataas.com/cat") ?: run {
                println("Failed to load image")
                callback(null)
                return@launch
            }

            mc.addScheduledTask {
                val texture = DynamicTexture(image)
                val location = mc.renderEngine.getDynamicTextureLocation("cat", texture)
                callback(location)
            }
            callback(null)
        }
    }

    private fun button(x: Int, y: Int, title: String, action: () -> Unit) = button {
        at(x, y)
        size(200, 20)
        text = title
        textShadow = true
        colour = ColorUtil.clickGUIColor.withAlpha(0)
        hoverColour = ColorUtil.clickGUIColor.withAlpha(50)
        outlineColour = ColorUtil.clickGUIColor
        outlineHoverColour = ColorUtil.clickGUIColor.brighter()
        onClick { action.invoke() }
    }

    private fun loadCustomBackground() {
        try {
            val imagePath = File(mc.mcDataDir, "config/catgirlroutes/images/custom_background.png")
            if (imagePath.exists()) {
                val image: BufferedImage = ImageIO.read(imagePath)
                val dynamicTexture = DynamicTexture(image)
                customBackgroundTexture = mc.textureManager.getDynamicTextureLocation("custom_menu_background", dynamicTexture)
            }
        } catch (e: Exception) {
            println("Failed to load custom menu background: ${e.message}")
            e.printStackTrace()
            customBackgroundTexture = null
        }
    }

    private fun joinLastSingleplayerWorld() {
        try {
            val saveLoader: ISaveFormat = mc.saveLoader
            val saves: List<SaveFormatComparator> = saveLoader.getSaveList()
            if (saves.isEmpty()) {
                println("No singleplayer worlds found")
                return
            }
            val last = saves.maxByOrNull { it.lastTimePlayed } ?: saves.first()
            mc.displayGuiScreen(null)
            mc.launchIntegratedServer(last.fileName, last.displayName, null)
        } catch (e: Exception) {
            println("Failed to join last singleplayer world: ${e.message}")
            e.printStackTrace()
        }
    }
}