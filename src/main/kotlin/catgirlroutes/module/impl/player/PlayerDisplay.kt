package catgirlroutes.module.impl.player

import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.DropdownSetting
import catgirlroutes.module.settings.impl.HudSetting
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil.drawStringWithShadow
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.utils.LocationManager.inSkyblock
import catgirlroutes.utils.SkyblockPlayer
import catgirlroutes.utils.SkyblockPlayer.DEF_REGEX
import catgirlroutes.utils.SkyblockPlayer.HP_REGEX
import catgirlroutes.utils.SkyblockPlayer.MANA_REGEX
import catgirlroutes.utils.SkyblockPlayer.MANA_USAGE_REGEX
import catgirlroutes.utils.SkyblockPlayer.OVERFLOW_REGEX
import catgirlroutes.utils.SkyblockPlayer.SALVATION_REGEX
import catgirlroutes.utils.SkyblockPlayer.SECRETS_REGEX
import catgirlroutes.utils.SkyblockPlayer.STACKS_REGEX
import catgirlroutes.utils.SkyblockPlayer.currentSecrets
import catgirlroutes.utils.SkyblockPlayer.maxSecrets
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.render.HUDRenderUtils.drawItemStackWithText
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedOutline
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.text.NumberFormat
import java.util.*

object PlayerDisplay: Module(
    "Player Display",
    Category.PLAYER,
    tag = TagType.WHIP
) {
    private const val BAR_WIDTH = 75.0
    private const val BAR_HEIGHT = 7.0

    private val hideHealth by BooleanSetting("Hide health")
    private val hideArmour by BooleanSetting("Hide armour")
    private val hideHunger by BooleanSetting("Hide hunger")

    private val healthDropdown by DropdownSetting("Health dropdown")
    private val health by HudSetting("Health") {
        size("1,200/10,900")
        visibleIf { inSkyblock }
        render { drawStringWithShadow("${(SkyblockPlayer.health + SkyblockPlayer.absorption).commas()}/${SkyblockPlayer.maxHealth.commas()}", 0.0, 0.0, healthColour.rgb) }
        preview { drawStringWithShadow("1,200/10,900", 0.0, 0.0, healthColour.rgb) }
    }.withDependency(healthDropdown)

    private val healthColour: Color by ColorSetting("Health colour", Color(255,85,85), false).withDependency(healthDropdown) { health.enabled }

    private val healthBar by HudSetting("Health bar") {
        size(BAR_WIDTH, BAR_HEIGHT)
        visibleIf { inSkyblock }
        render {
            val fillWidth = SkyblockPlayer.health.toFloat() / SkyblockPlayer.maxHealth.toFloat() * BAR_WIDTH
            drawRoundedRect(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, ColorUtil.bgColor)
            drawRoundedRect(0.0, 0.0, fillWidth, BAR_HEIGHT, 5.0, healthBarColour)
            drawRoundedOutline(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, 1.0, Color(208, 208, 208))
        }
        preview {
            drawRoundedRect(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, ColorUtil.bgColor)
            drawRoundedRect(0.0, 0.0, 50.0, BAR_HEIGHT, 5.0, healthBarColour)
            drawRoundedOutline(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, 1.0, Color(208, 208, 208))
        }
    }.withDependency(healthDropdown)

    private val healthBarColour: Color by ColorSetting("Health bar colour", Color(255,85,85)).withDependency(healthDropdown) { healthBar.enabled }

    private val effectiveHealth by HudSetting("Effective health") {
        size("54,879")
        visibleIf { inSkyblock }
        render { drawStringWithShadow(SkyblockPlayer.effectiveHealth.commas(), 0.0, 0.0, effectiveHealthColour.rgb) }
        preview { drawStringWithShadow("54,879", 0.0, 0.0, effectiveHealthColour.rgb) }
    }.withDependency(healthDropdown)

    private val effectiveHealthColour: Color by ColorSetting("Effective health colour", Color(0, 170, 0)).withDependency(healthDropdown) { effectiveHealth.enabled }

    private val manaDropdown by DropdownSetting("Mana dropdown")
    private val mana by HudSetting("Mana") {
        size("1,300/10,900")
        visibleIf { inSkyblock }
        render { drawStringWithShadow("${SkyblockPlayer.mana.commas()}/${SkyblockPlayer.maxMana.commas()}", 0.0, 0.0, manaColour.rgb) }
        preview { drawStringWithShadow("1,300/10,900", 0.0, 0.0, manaColour.rgb) }
    }.withDependency(manaDropdown)

    private val manaColour: Color by ColorSetting("Mana colour", Color(85, 85, 255), false).withDependency(manaDropdown) { mana.enabled }

    private val manaBar by HudSetting("Mana bar") {
        size(BAR_WIDTH, BAR_HEIGHT)
        visibleIf { inSkyblock }
        render {
            val fillWidth = SkyblockPlayer.mana.toFloat() / SkyblockPlayer.maxMana.toFloat() * BAR_WIDTH
            drawRoundedRect(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, ColorUtil.bgColor)
            drawRoundedRect(0.0, 0.0, fillWidth, BAR_HEIGHT, 5.0, manaBarColour)
            drawRoundedOutline(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, 1.0, Color(208, 208, 208))
        }
        preview {
            drawRoundedRect(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, ColorUtil.bgColor)
            drawRoundedRect(0.0, 0.0, 50.0, BAR_HEIGHT, 5.0, manaBarColour)
            drawRoundedOutline(0.0, 0.0, BAR_WIDTH, BAR_HEIGHT, 5.0, 1.0, Color(208, 208, 208))
        }
    }.withDependency(manaDropdown)

    private val manaBarColour: Color by ColorSetting("Mana bar colour", Color(85, 85, 255)).withDependency(manaDropdown) { manaBar.enabled }

    private val manaUsage by HudSetting("Mana usage") {
        size("§b-50 Mana (§6Speed Boost§b)")
        visibleIf { inSkyblock && SkyblockPlayer.manaUsage.isNotEmpty() }
        render { drawStringWithShadow(SkyblockPlayer.manaUsage, 0.0, 0.0) }
        preview { drawStringWithShadow("§b-50 Mana (§6Speed Boost§b)", 0.0, 0.0) }
    }.withDependency(manaDropdown)

    private val overflowMana by HudSetting("Overflow mana") {
        size("600ʬ")
        visibleIf { inSkyblock && SkyblockPlayer.overflowMana != 0 }
        render { drawStringWithShadow("${SkyblockPlayer.overflowMana.commas()}ʬ", 0.0, 0.0, overflowManaColour.rgb) }
        preview { drawStringWithShadow("600ʬ", 0.0, 0.0, overflowManaColour.rgb) }
    }.withDependency(manaDropdown)

    private val overflowManaColour: Color by ColorSetting("Overflow mana colour", Color(0, 170, 170), false).withDependency(manaDropdown) { overflowMana.enabled }

    private val otherDropdown by DropdownSetting("Other")

    private val defence by HudSetting("Defence") {
        size("10,000")
        visibleIf { inSkyblock }
        render { drawStringWithShadow(SkyblockPlayer.defence.commas(), 0.0, 0.0, defenceColour.rgb) }
        preview { drawStringWithShadow("10,000", 0.0, 0.0, defenceColour.rgb) }
    }.withDependency(otherDropdown)

    private val defenceColour: Color by ColorSetting("Defence colour", Color( 85, 255, 85), false).withDependency(otherDropdown) { defence.enabled }

    private val speed by HudSetting("Speed") {
        size("✦500")
        visibleIf { inSkyblock }
        render { drawStringWithShadow("✦${SkyblockPlayer.speed}", 0.0, 0.0, speedColour.rgb) }
        preview { drawStringWithShadow("✦500", 0.0, 0.0, speedColour.rgb) }
    }.withDependency(otherDropdown)

    private val speedColour: Color by ColorSetting("Speed colour", Color.WHITE, false).withDependency(otherDropdown) { speed.enabled }

    private val stacks by HudSetting("Crimson stacks") {
        size("10ᝐ")
        visibleIf { inSkyblock && SkyblockPlayer.stacks.isNotEmpty() }
        render { drawStringWithShadow(SkyblockPlayer.stacks, 0.0, 0.0) }
        preview { drawStringWithShadow("10ᝐ", 0.0, 0.0) }
    }.withDependency(otherDropdown)

    private val salvation by HudSetting("Salvation") {
        size("T3!")
        visibleIf { inSkyblock && SkyblockPlayer.salvation != 0 }
        render { drawStringWithShadow("T${SkyblockPlayer.salvation}!", 0.0, 0.0) }
        preview { drawStringWithShadow("T3!", 0.0, 0.0) }
    }.withDependency(otherDropdown)

    private val secrets by HudSetting("Secret display") {
        width { if (sbaStyle) 18 + "Secrets".getWidth() else "5/10 Secrets".getWidth() }
        height { if (sbaStyle) fontHeight * 2 + 2.0 else fontHeight + 2.0 }
        visibleIf { inDungeons && !inBoss }
        render {
            val colour = when (currentSecrets / maxSecrets.toDouble()) {
                in 0.0..0.5 -> "§c"
                in 0.5..0.75 -> "§e"
                else -> "§a"
            }
            if (sbaStyle) {
                drawItemStackWithText(ItemStack(Blocks.chest), 0.0, 0.0)
                drawStringWithShadow("§7Secrets", 16.0, 0.0)
                val secretsText = if (currentSecrets > -1) "$colour$currentSecrets§7/$colour$maxSecrets" else "§7Unknown"
                drawStringWithShadow(secretsText, 18 + "Secrets".getWidth() / 2 - secretsText.getWidth() / 2, 11.0)
            } else if (currentSecrets > -1) {
                drawStringWithShadow("$colour$currentSecrets/$maxSecrets Secrets", 0.0, 0.0)
            }
        }
        preview {
            if (sbaStyle) {
                drawItemStackWithText(ItemStack(Blocks.chest), 0.0, 0.0)
                drawStringWithShadow("§7Secrets", 16.0, 0.0)
                drawStringWithShadow("§e5§7/§e10", 16.0 + 2.0 + "Secrets".getWidth() / 2.0 - "5/10".getWidth() / 2.0, 11.0)
            } else {
                drawStringWithShadow("§e5/10 Secrets", 0.0, 0.0)
            }
        }
    }.withDependency(otherDropdown)

    private val sbaStyle: Boolean by BooleanSetting("SBA secrets style").withDependency(otherDropdown) { secrets.enabled }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
        if (event.isCanceled) return
        if (!inSkyblock) return

        event.isCanceled = when (event.type) {
            RenderGameOverlayEvent.ElementType.HEALTH -> hideHealth
            RenderGameOverlayEvent.ElementType.ARMOR -> hideArmour
            RenderGameOverlayEvent.ElementType.FOOD -> hideHunger
            else -> return
        }
    }

    fun modifyActionBar(text: String): String {
        if (!this.enabled) return text
        var toReturn = text

        if (health.enabled) {
            toReturn = toReturn.replace(HP_REGEX, "")
        }
        if (mana.enabled) {
            toReturn = toReturn.replace(MANA_REGEX, "")
        }
        if (overflowMana.enabled) {
            toReturn = toReturn.replace(OVERFLOW_REGEX, "")
        }
        if (defence.enabled) {
            toReturn = toReturn.replace(DEF_REGEX, "")
        }
        if (stacks.enabled) {
            toReturn = toReturn.replace(STACKS_REGEX, "")
        }
        if (salvation.enabled) {
            toReturn = toReturn.replace(SALVATION_REGEX, "")
        }
        if (manaUsage.enabled) {
            toReturn = toReturn.replace(MANA_USAGE_REGEX, "")
        }
        if (secrets.enabled) {
            toReturn = toReturn.replace(SECRETS_REGEX, "")
        }

        return toReturn
    }

    private fun Number.commas(): String = NumberFormat.getInstance(Locale.US).format(this)
}