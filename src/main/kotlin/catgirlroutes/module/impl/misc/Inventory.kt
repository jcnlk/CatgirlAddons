package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.GuiContainerEvent
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.mixins.accessors.AccessorGuiEditSign
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.*
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.MouseUtils.mouseButton
import catgirlroutes.ui.clickgui.util.MouseUtils.mx
import catgirlroutes.ui.clickgui.util.MouseUtils.my
import catgirlroutes.ui.misc.elements.impl.textField
import catgirlroutes.ui.misc.elements.util.update
import catgirlroutes.ui.misc.searchoverlay.AuctionOverlay
import catgirlroutes.ui.misc.searchoverlay.BazaarOverlay
import catgirlroutes.ui.misc.searchoverlay.SearchType
import catgirlroutes.utils.LocationManager.inSkyblock
import catgirlroutes.utils.lore
import catgirlroutes.utils.noControlCodes
import catgirlroutes.utils.render.HUDRenderUtils.drawItemStackWithText
import catgirlroutes.utils.render.HUDRenderUtils.drawPlayerOnScreen
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.highlight
import catgirlroutes.utils.render.WorldRenderUtils.partialTicks
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color


object Inventory : Module(
    "Inventory",
    Category.MISC,
    "Various quality of life features for inventory GUIs",
    tag = TagType.WHIP
) { // todo: neu type shit search overlay (jei shit on the side)

    private val invDropdown by DropdownSetting("Inventory")
    private val invHUD by HudSetting("Inventory HUD") {
        size(2 + 22 * 9, 2 + 22 * 3)
        render {
            val bgColour = Color(139, 139, 139, 155)
            val borderColour = Color(250, 250, 250, 155)

            width = 2.0 + 22 * (if (playerModel) 11 else 9)
            drawRoundedBorderedRect(0.0, 0.0, width, height, 5.0, 2.0, bgColour, borderColour)

            if (playerModel) drawPlayerOnScreen(width - 22.5, 62.5, partialTicks, 30.0)

            val stacks = mc.thePlayer.inventory.mainInventory.drop(9)
            var y = -20.0
            stacks.forEachIndexed { i, stack ->
                if (i % 9 == 0) y += 22.0
                val x = 2 + 22.0 * (i % 9)
                drawRoundedBorderedRect(x, y, 20.0, 20.0, 5.0, 2.0, Color(139, 139, 139, 155), Color(250, 250, 250, 155))
                stack?.let { drawItemStackWithText(it, x + 2.5, y + 2.5) }
            }
        }
    }.withDependency(invDropdown)
    private val playerModel: Boolean by BooleanSetting("Player model").withDependency(invDropdown) { invHUD.enabled }

    private val sbDropdown by DropdownSetting("Search bar dropdown")
    private val searchBar by HudSetting("Search bar", "Use \",\" separator to search for things like attributes") { size(200.0, 25.0) }.withDependency(this.sbDropdown)
    private val bgColour_ by ColorSetting("Background colour", ColorUtil.buttonColor).withDependency(this.sbDropdown) { searchBar.enabled }
    private val outlineColour_ by ColorSetting("Outline colour", ColorUtil.clickGUIColor).withDependency(this.sbDropdown) { searchBar.enabled }
    private val itemList by BooleanSetting("Item list").withDependency(this.sbDropdown) { searchBar.enabled }

    private val ahDropdown by DropdownSetting("Auction house")
    private val auctionOverlay by BooleanSetting("Auction search").withDependency(ahDropdown)

    private val bzDropdown by DropdownSetting("Bazaar")
    private val bazaarOverlay by BooleanSetting("Bazaar search").withDependency(bzDropdown)

    private val ctrlF by BooleanSetting("Ctrl + F to search").withDependency { this.auctionOverlay || this.bazaarOverlay } // todo: Make it click on signs in other guis (price, quantity, etc)

    var ahHistory by ListSetting("AH_SEARCH", mutableListOf<String>())
    var bzHistory by ListSetting("BZ_SEARCH", mutableListOf<String>())

    private var overlay: SearchType = SearchType.NONE
    private var clickedSearch = false

    private var textField = textField {
        size(200.0, 20.0)
        thickness = 2.0
        radius = 5.0
        colour = bgColour_
        outlineColour = outlineColour_.darker()
    }

    private var highlightSlots = mutableMapOf<Int, HighlightSlot>()
    private val stupid get() = mc.theWorld == null || !inSkyblock || !this.searchBar.enabled || (mc.currentScreen !is GuiInventory && mc.currentScreen !is GuiChest)

    @SubscribeEvent
    fun onS2DPacketOpenWindow(event: PacketReceiveEvent) {
        if (!inSkyblock || event.packet !is S2DPacketOpenWindow) return
        val title = event.packet.windowTitle.unformattedText
        overlay = if ((title.contains("Auctions") || title.contains("Auction House")) && this.auctionOverlay) {
            SearchType.AUCTION
        } else if (title.contains("Bazaar") && this.bazaarOverlay) {
            SearchType.BAZAAR
        } else {
            SearchType.NONE
        }
    }

    @SubscribeEvent
    fun onC0EPacketClickWindow(event: PacketSentEvent) {
        if (!inSkyblock || overlay == SearchType.NONE || event.packet !is C0EPacketClickWindow) return
        val registry = event.packet.clickedItem?.item?.registryName
        val name = event.packet.clickedItem?.displayName
        val slot = event.packet.slotId
        clickedSearch = (registry == "minecraft:sign" && name == "§aSearch" && slot in listOf(48, 45))
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onGuiScreenKeyboard(event: GuiScreenEvent.KeyboardInputEvent.Pre) {
        if (event.gui !is GuiChest || overlay == SearchType.NONE || !this.ctrlF) return
        val openSlots = mc.thePlayer?.openContainer?.inventorySlots ?: return

        val slotId = if (overlay == SearchType.AUCTION) 48 else 45
        val gui = event.gui as GuiChest
        val signStack = openSlots.getOrNull(slotId)?.stack ?: return

        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_F)) {
            if (signStack.item == Items.sign && signStack.displayName == "§aSearch") {
                mc.playerController.windowClick(gui.inventorySlots.windowId, slotId, 0, 0, mc.thePlayer)
            } else {
//                display = AhBzSearch(overlay) // todo: fix
            }
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiEditSign || overlay == SearchType.NONE || !clickedSearch) return

        val sign = (event.gui as AccessorGuiEditSign).tileSign
        sign?.let {
            event.gui = when(overlay) {
                SearchType.AUCTION -> AuctionOverlay(it)
                SearchType.BAZAAR -> BazaarOverlay(it)
                SearchType.NONE -> return
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (mc.theWorld == null || !inSkyblock || !this.searchBar.enabled || mc.currentScreen == null || this.textField.text.isEmpty()) {
            this.highlightSlots.clear()
            return
        }

        this.highlightSlots.clear()

        mc.thePlayer?.openContainer?.inventorySlots?.forEachIndexed { i, slot ->
            slot.stack?.let {
                val name = slot.stack.displayName.noControlCodes.lowercase()
                val lore = slot.stack.lore.joinToString().lowercase()

                this.textField.text.split(",").map(String::trim).map(String::lowercase).forEach {
                    when (matchType(name, lore, it)) {
                        1 -> this.highlightSlots[i] = HighlightSlot(slot, this.textField.text, Color.WHITE)
                        2 -> this.highlightSlots[i] = HighlightSlot(slot, this.textField.text, Color.MAGENTA)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onDrawScreenPost(event: GuiScreenEvent.DrawScreenEvent.Post) {
        if (this.stupid) return
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()

        this.textField.update {
            x = searchBar.x
            y = searchBar.y
            colour = bgColour_
            outlineColour = outlineColour_.darker()
            outlineHoverColour = outlineColour_
        }.draw()

        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onDrawSlot(event: GuiContainerEvent.DrawSlotEvent) {
        if (this.stupid) return
        this.highlightSlots.values.forEach { it.slot.highlight(it.colour) }
    }

    @SubscribeEvent
    fun onGuiScreenKeyboard2(event: GuiScreenEvent.KeyboardInputEvent.Pre) {
        if (!inSkyblock || !this.searchBar.enabled || !this.textField.isFocused || !Keyboard.getEventKeyState()) return
        this.textField.onKey(Keyboard.getEventCharacter(), Keyboard.getEventKey())

        event.isCanceled = true
        if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            this.textField.isFocused = false
        }
    }

    @SubscribeEvent
    fun onGuiScreenMouse(event: GuiScreenEvent.MouseInputEvent.Pre) {
        if (this.stupid || !Mouse.getEventButtonState()) return
        this.textField.onMouseClick(mx, my, mouseButton)
    }

    private fun matchType(name: String, lore: String, string: String) = when {
        name.isEmpty() || lore.isEmpty() || string.isEmpty() -> 0
        name.contains(string.lowercase()) -> 1
        lore.contains(string.lowercase()) -> 2
        else -> 0
    }

    data class HighlightSlot(var slot: Slot, var string: String, val colour: Color)
}