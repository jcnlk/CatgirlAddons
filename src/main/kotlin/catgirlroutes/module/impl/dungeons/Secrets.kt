package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.ChatPacket
import catgirlroutes.events.impl.RenderEntityEvent
import catgirlroutes.events.impl.SecretPickupEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.*
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.PlayerUtils.playLoudSound
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonItemDrops
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.render.WorldRenderUtils.drawBlock
import catgirlroutes.utils.render.WorldRenderUtils.drawEntityBox
import catgirlroutes.utils.unformattedName
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object Secrets : Module(
    "Secrets",
     Category.DUNGEON,
) {
    private val soundOptions = arrayListOf(
        "note.pling",
        "mob.blaze.hit",
        "fire.ignite",
        "random.orb",
        "random.break",
        "mob.guardian.land.hit",
        "Custom"
    )

    private val chimeDropdown by DropdownSetting("Chime dropdown")
    private val secretChime by BooleanSetting("Secret chime", "Plays a sound on secret click.").withDependency(chimeDropdown)
    private val chimeSound by SelectorSetting("Chime sound", "note.pling", soundOptions, "Sound selection.").withDependency(chimeDropdown) { secretChime }
    private val chimeCustom by StringSetting("Custom chime sound", "note.pling", description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting.").withDependency(chimeDropdown) { chimeSound.selected == "Custom" && secretChime }

    private val dropSound by SelectorSetting("Drop sound", "note.pling", soundOptions, "Sound selection for item pickups.").withDependency(chimeDropdown) { secretChime  }
    private val dropCustom by StringSetting("Custom drop sound", "note.pling", description = "Name of a custom sound to play for item pickups.").withDependency(chimeDropdown) { dropSound.selected == "Custom" && secretChime }

    private val highlightDropdown by DropdownSetting("Highlight dropdown")
    private val secretClicks by BooleanSetting("Secret clicks", "Highlights the secret on click.").withDependency(highlightDropdown)
    private val outline by BooleanSetting("Outline", "Draws the outline.").withDependency(highlightDropdown) { secretClicks }
    private val clickColour by ColorSetting("Click colour", Color.GREEN).withDependency(highlightDropdown) { secretClicks }
    private val lockedColour by ColorSetting("Locked colour", Color.RED, description = "Locked secret colour.").withDependency(highlightDropdown){ secretClicks }

    private val itemDropdown by DropdownSetting("Item dropdown")
    private val itemHighlight by BooleanSetting("Item highlight", "Highlights secret items.").withDependency(itemDropdown)
    private val closeColour by ColorSetting("Close colour", Color.GREEN, description = "Highlight colour when the player is near the item.").withDependency(itemDropdown) { itemHighlight }
    private val farColour by ColorSetting("Far colour", Color.RED, description = "Highlight colour when the player is far from the item.").withDependency(itemDropdown) { itemHighlight }
    private val playSound by BooleanSetting("Play sound", "Plays a sound when the player is near the item.").withDependency(itemDropdown) { itemHighlight }
    private val itemSound by SelectorSetting("Sound", "note.pling", soundOptions, "Sound selection.").withDependency(itemDropdown) { itemHighlight && playSound }
    private val itemCustom by StringSetting("Custom sound", "note.pling").withDependency(itemDropdown) { itemHighlight && playSound && itemSound.selected == "Custom" }

    private val volume by NumberSetting("Volume", 1.0, 0.0, 1.0, 0.01, "Volume of the sound.").withDependency { secretChime || playSound }
    private val pitch by NumberSetting("Pitch", 2.0, 0.0, 2.0, 0.01, "Pitch of the sound.").withDependency { secretChime || playSound}

    private data class Secret(val blockPos: BlockPos, var isLocked: Boolean = false)
    private val clickedSecrets = CopyOnWriteArrayList<Secret>()
    private var lastPlayed = System.currentTimeMillis()

    @SubscribeEvent
    fun onSecret(event: SecretPickupEvent) {
        if (!inDungeons) return
        if (event is SecretPickupEvent.Interact) secretHighlight(event.blockPos)
        playSecretSound(getSound(event is SecretPickupEvent.Item))
    }

    @SubscribeEvent
    fun onChat(event: ChatPacket) {
        if (secretClicks && event.message == "That chest is locked!") {
            clickedSecrets.lastOrNull()?.isLocked = true
        }
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (!inDungeons) return

        if (this.clickedSecrets.isNotEmpty()) {
            this.clickedSecrets.forEach {
                val colour = if (it.isLocked) lockedColour else clickColour

                drawBlock(it.blockPos, colour, filled = true)
                if (this.outline) drawBlock(it.blockPos, colour.withAlpha(255))
            }
        }

        if (this.itemHighlight) {
            mc.theWorld.loadedEntityList.filter { it is EntityItem && dungeonItemDrops.contains(it.entityItem.unformattedName) }
                .forEach {
                    var colour = farColour

                    if (mc.thePlayer.getDistanceToEntity(it) < 3.5) {
                        playLoudSound(getSound(isHighlight = true), this.volume.toFloat() * 0.2f, this.pitch.toFloat())
                        colour = closeColour
                    }
                    drawEntityBox(it, colour, colour, false, true, event.partialTicks, 0.0f, 0.1f)
                }
        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (!inDungeons || inBoss || !this.itemHighlight) return
        if (event.entity !is EntityItem) return
        if (!dungeonItemDrops.contains((event.entity as EntityItem).entityItem.unformattedName)) return
        event.isCanceled = true
    }

    /**
     * Returns the sound from the selector setting, or the custom sound when the last element is selected
     */
    private fun getSound(isItemDrop: Boolean = false, isHighlight: Boolean = false): String {
        return if (isItemDrop && !isHighlight)
            if (dropSound.index < chimeSound.options.size - 1)
                dropSound.selected
            else
                dropCustom
        else if (!isHighlight)
            if (chimeSound.index < chimeSound.options.size - 1)
                chimeSound.selected
            else
                chimeCustom
        else if (itemSound.index < itemSound.options.size - 1)
            itemSound.selected
        else
            itemCustom
    }

    private fun playSecretSound(sound: String) {
        if (System.currentTimeMillis() - lastPlayed > 10 && this.secretChime) {
            playLoudSound(sound, volume.toFloat(), pitch.toFloat())
            lastPlayed = System.currentTimeMillis()
        }
    }

    private fun secretHighlight(blockPos: BlockPos) {
        if (!this.secretClicks || this.clickedSecrets.any { it.blockPos == blockPos }) return
        this.clickedSecrets.add(Secret(blockPos))
        scheduleTask(20) { this.clickedSecrets.removeFirstOrNull() }
    }
}