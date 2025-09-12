package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.display
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.SettingsCategory
import catgirlroutes.ui.hud.EditHudGUI

@SettingsCategory
object EditHud : Module(
    "Edit Hud",
    Category.SETTINGS,
    "Opens the edit hud gui."
) {
    /**
     * Overridden to prevent the chat message from being sent.
     */
    override fun onKeyBind() {
        this.toggle()
    }

    /**
     * Automatically disable it again and open the gui
     */
    override fun onEnable() {
        display = EditHudGUI
        toggle()
        super.onEnable()
    }
}