package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.clickGUI
import catgirlroutes.CatgirlRoutes.Companion.clickGUINew
import catgirlroutes.CatgirlRoutes.Companion.display
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.AlwaysActive
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.Setting.Companion.withInputTransform
import catgirlroutes.module.settings.SettingsCategory
import catgirlroutes.module.settings.Visibility
import catgirlroutes.module.settings.impl.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.Display
import java.awt.Color
import net.minecraft.client.Minecraft
import catgirlroutes.CatgirlRoutes
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Settings for the CLick Gui
 * @author Aton
 */
@AlwaysActive
@SettingsCategory
object ClickGui: Module(
    "ClickGUI",
    Keyboard.KEY_RSHIFT,
    category = Category.SETTINGS,
    description = "Appearance settings for the click gui. \n" +
            "You can set a custom chat prefix with formatting here. For formatting use & or the paragrph symbol followed by a modifier. " +
            "A benefit of using the paragraph symbol is, that you can directly see how it will look in the text field, but you wont be able to see the formatting. \n" +
            "§00...§ff§r are colors, l is §lBold§r, n is §nUnderlined§r, o is §oItalic§r, m is §mStrikethrough§r, k is §kObfuscated§r, r is Reset."
) {

    val clickGui by SelectorSetting("ClickGui", "Cga", arrayListOf("Cga", "Flopper"))
    val design by SelectorSetting("Design", "New", arrayListOf("JellyLike", "New"), "Design theme of the gui.")
    val customFont by BooleanSetting("Custom font (WIP; scaling is schizo)")
    val notifications by BooleanSetting("Notifications", true, "Send notifications instead of chat messages")
    val blur by BooleanSetting("Blur", false,  "Toggles the background blur for the gui.")
    val color by ColorSetting("Color", Color(255, 137, 213), false, "Color theme in the gui.", false)

    val customMenu by BooleanSetting("Custom main menu")
    val customMenuPic by BooleanSetting("Custom main menu picture", false, "Use a custom picture instead of the default one.").withDependency { customMenu }
    val windowName by StringSetting("Window name", "", description = "Sets window name to a custom one if not empty. Supports {player}, {server}, {mod}, {version}, {time}, {fps}")
        .withInputTransform { input, _ ->
            pendingTitleUpdate = true
            input
        }
    val guiName by StringSetting("Gui name", "CatgirlAddons", 15, description = "Name that will be rendered in the gui.")
    val prefixStyle by SelectorSetting("Prefix style", "Long", arrayListOf("Long", "Short", "Custom"), "Chat prefix selection for mod messages.")
    val customPrefix by StringSetting("Custom prefix", "§0§l[§4§lCatgirlAddons§0§l]§r", 40,  description = "You can set a custom chat prefix that will be used when Custom is selected in the Prefix Style dropdown.").withDependency { this.prefixStyle.index == 2 }

    private val devSettings by DropdownSetting("Dev settings", false)
    val devMode by BooleanSetting("Dev mode", false, "Toggles developer mode").withDependency(devSettings)
    val debugMode by BooleanSetting("Debug mode", false, "Toggles debug mode").withDependency(devSettings)
    val forceHypixel by BooleanSetting("Force hypixel", false, "Makes the mod think that you're on Hypixel").withDependency(devSettings)
    val forceSkyblock by BooleanSetting("Force skyblock", false, "Makes the mod think that you're in Skyblock").withDependency(devSettings)
    val forceDungeon by BooleanSetting("Force dungeon", false, "Makes the mod think that you're in Dungeon").withDependency(devSettings)
    val forceBoss by BooleanSetting("Force boss", false, "Makes the mod think that you're in a boss fight").withDependency(devSettings)

    val showUsageInfo by BooleanSetting("Usage information", true, "Show info on how to use the GUI.", Visibility.ADVANCED_ONLY)

    val panelX: MutableMap<Category, NumberSetting> = mutableMapOf()
    val panelY: MutableMap<Category, NumberSetting> = mutableMapOf()
    val panelExtended: MutableMap<Category, BooleanSetting> = mutableMapOf()

    private const val pwidth = 120.0
    private const val pheight = 15.0

    val panelWidth = NumberSetting("Panel width", pwidth, visibility = Visibility.HIDDEN)
    val panelHeight = NumberSetting("Panel height", pheight, visibility = Visibility.HIDDEN)

    const val advancedRelWidth = 0.5
    const val advancedRelHeight = 0.5

    val advancedRelX = NumberSetting("Advanced_RelX",(1 - advancedRelWidth)/2.0,0.0, (1- advancedRelWidth), 0.0001, visibility = Visibility.HIDDEN)
    val advancedRelY = NumberSetting("Advanced_RelY",(1 - advancedRelHeight)/2.0,0.0, (1- advancedRelHeight), 0.0001, visibility = Visibility.HIDDEN)

    init {
        addSettings(
            advancedRelX,
            advancedRelY
        )

        // The Panels

        // this will set the default click gui panel settings. These will be overwritten by the config once it is loaded
        resetPositions()

        addSettings(
            panelWidth,
            panelHeight
        )

        for(category in Category.entries) {
            addSettings(
                panelX[category]!!,
                panelY[category]!!,
                panelExtended[category]!!
            )
        }
    }

    /**
     * Adds if missing and sets the default click gui positions for the category panels.
     */
    private fun resetPositions() {
        panelWidth.value = pwidth
        panelHeight.value = pheight

        var px = 10.0
        val py = 10.0
        val pxplus = panelWidth.value + 10
        for(category in Category.entries) {
            panelX.getOrPut(category) { NumberSetting(category.name + ",x", px, visibility = Visibility.HIDDEN) }.value = px
            panelY.getOrPut(category) { NumberSetting(category.name + ",y", py, visibility = Visibility.HIDDEN) }.value = py
            panelExtended.getOrPut(category) { BooleanSetting(category.name + ",extended", true, visibility = Visibility.HIDDEN) }.enabled = true
            px += pxplus
        }

        advancedRelX.reset()
        advancedRelY.reset()
    }

    private var originalWindowTitle: String? = null
    @Volatile var pendingTitleUpdate: Boolean = false
    private var lastAppliedTitle: String? = null

    /**
     * Overridden to prevent the chat message from being sent.
     */
    override fun onKeyBind() {
        if (this.clickGui.index == 0) design.selected = "New"
        display = if (this.clickGui.index == 0) clickGUINew else clickGUI
    }

    /**
     * Automatically disable it again and open the gui
     */
    override fun onEnable() {
        if (this.clickGui.index == 0) design.selected = "New"
        display = if (this.clickGui.index == 0) clickGUINew else clickGUI
        super.onEnable()
        toggle()
    }

    override fun onInitialize() {
        pendingTitleUpdate = true
    }

    fun buildWindowTitle(): String {
        val tpl = windowName.trim()
        return if (tpl.isBlank()) originalWindowTitle ?: runCatching { Display.getTitle() }.getOrNull() ?: CatgirlRoutes.MOD_NAME
        else resolveWindowVariables(tpl)
    }

    private fun resolveWindowVariables(s: String): String {
        val mc = CatgirlRoutes.mc
        val player = mc.thePlayer?.displayNameString ?: mc.thePlayer?.name ?: "Player"
        val server = when {
            mc.isSingleplayer -> "Singleplayer"
            mc.currentServerData != null -> mc.currentServerData.serverIP
            else -> "Multiplayer"
        }
        val time = runCatching { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrElse { "" }
        val fps = runCatching { Minecraft.getDebugFPS() }.getOrElse { 0 }
        return s
            .replace("{player}", player, ignoreCase = true)
            .replace("{server}", server ?: "", ignoreCase = true)
            .replace("{mod}", CatgirlRoutes.MOD_NAME, ignoreCase = true)
            .replace("{version}", CatgirlRoutes.MOD_VERSION, ignoreCase = true)
            .replace("{time}", time, ignoreCase = true)
            .replace("{fps}", fps.toString(), ignoreCase = true)
    }

    fun applyWindowTitleIfNeeded(force: Boolean = false) {
        try {
            if (originalWindowTitle == null) {
                originalWindowTitle = runCatching { Display.getTitle() }.getOrNull()
            }
            val target = if (windowName.isNotBlank()) buildWindowTitle() else originalWindowTitle ?: runCatching { Display.getTitle() }.getOrNull() ?: CatgirlRoutes.MOD_NAME
            if (force || lastAppliedTitle != target) {
                Display.setTitle(target)
                lastAppliedTitle = target
            }
        } catch (_: Throwable) {
            // ignore
        } finally {
            pendingTitleUpdate = false
        }
    }
}