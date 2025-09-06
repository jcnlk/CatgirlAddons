package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.display
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.config.InventoryButtonsConfig
import catgirlroutes.config.InventoryButtonsConfig.allButtons
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.mixins.accessors.AccessorGuiContainer
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.misc.inventorybuttons.InventoryButtonEditor
import catgirlroutes.utils.*
import catgirlroutes.utils.LocationManager.inSkyblock
import catgirlroutes.utils.render.HUDRenderUtils.drawHoveringText
import com.google.gson.JsonPrimitive
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Mouse

object InventoryButtons : Module(
    "Inventory Buttons",
    Category.MISC,
) {

    val equipmentOverlay by BooleanSetting("Equipment Overlay") // todo: make it work when you right click eq in hand

    val editMode by ActionSetting("Edit", "Opens edit menu for inventory buttons") { display = InventoryButtonEditor() }

    private var shouldScanEq = false
    private val equipmentSlots = listOf(10, 19, 28, 37)

    @SubscribeEvent
    fun onDrawScreenPost(event: GuiScreenEvent.DrawScreenEvent.Post) {
        if (!inSkyblock || mc.currentScreen !is GuiInventory) return
        val accessor = event.gui as AccessorGuiContainer
        GlStateManager.pushMatrix()
        allButtons.filter { it.isActive }.forEach { button ->
            button.render(accessor.guiLeft.toDouble(), accessor.guiTop.toDouble())

            if (button.isHovered(event.mouseX - accessor.guiLeft, event.mouseY - accessor.guiTop)) {
                val textLines = when {
                    button.isEquipment -> {
                        if (button.icon == "barrier") {
                            listOf("Empty")
                        } else {
                            button.icon.toJsonObject().toItemStack().let { itemStack ->
                                listOf(itemStack.displayName) + itemStack.lore
                            }
                        }
                    }
                    else -> listOf("§7/${button.command.replace("/", "")}")
                }

                val xOff = if (button.isEquipment) calculateTooltipXOffset(textLines) else 0

                drawHoveringText(
                    textLines,
                    event.mouseX - xOff, event.mouseY,
                    event.gui.width, event.gui.height, themed = false
                )
            }
        }
        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onMouseInputPre(event: GuiScreenEvent.MouseInputEvent.Pre) {
        if (!inSkyblock || mc.currentScreen !is GuiInventory) return

        val sr = ScaledResolution(mc)

        val mouseX: Int = Mouse.getX() * sr.scaledWidth / mc.displayWidth
        val mouseY: Int = sr.scaledHeight - (Mouse.getY() * sr.scaledHeight / mc.displayHeight)

        val accessor = event.gui as AccessorGuiContainer

        allButtons.filter { it.isActive && it.isHovered(mouseX - accessor.guiLeft, mouseY - accessor.guiTop) && Mouse.getEventButtonState() }.forEach { it.action }
    }

    @SubscribeEvent
    fun onS2DPacketOpenWindow(event: PacketReceiveEvent) {
        if (!inSkyblock || !this.equipmentOverlay || event.packet !is S2DPacketOpenWindow) return
        shouldScanEq = event.packet.windowTitle.unformattedText == "Your Equipment and Stats"
    }

    @SubscribeEvent
    fun onS30PacketWindowItems(event: PacketReceiveEvent) {
        if (!shouldScanEq || event.packet !is S30PacketWindowItems) return
        val buttons = allButtons.filter { it.isEquipment }
        this.equipmentSlots.forEachIndexed { i, slot ->
            val itemStack = event.packet.itemStacks?.getOrNull(slot) ?: return@forEachIndexed
            buttons[i].icon = when {
                itemStack.displayName.noControlCodes == "Empty Equipment Slot" -> "barrier"
                itemStack.item == Items.skull -> {
                    val tagString = itemStack.toJson()
                    if (!tagString.has("internalname")) {
                        tagString.add("internalname", JsonPrimitive("_"))
                    }
                    tagString.toString()
                }
                else -> return
            }
            InventoryButtonsConfig.save()
        }
        this.shouldScanEq = false
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        this.shouldScanEq = false
    }

    private fun calculateTooltipXOffset(tooltipToDisplay: List<String>?): Int {
        var offset = 0
        tooltipToDisplay?.forEach { line ->
            val lineWidth = FontUtil.getStringWidth(line)
            if (lineWidth > offset) {
                offset = lineWidth
            }
        }
        return offset + 20
    }
}