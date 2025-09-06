package catgirlroutes.module.settings.impl

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.Visibility

/**
 * A setting that shows or hides settings in the ClickGui only
 */
class DropdownSetting(
    name: String,
    enabled: Boolean = false,
    override val default: Dropdown = Dropdown(enabled),
) : Setting<Dropdown>(name, null, Visibility.CLICK_GUI_ONLY) {

    override var value: Dropdown = default
    val dependentModules = value.dependentModules
    var enabled: Boolean
        get() = value.enabled
        set(value) { this.value.enabled = value }

}

class Dropdown(
    var enabled: Boolean = false
) {
    val dependentModules = mutableListOf<Setting<*>>()
}