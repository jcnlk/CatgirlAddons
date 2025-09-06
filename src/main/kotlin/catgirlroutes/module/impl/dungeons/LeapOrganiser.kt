package catgirlroutes.module.impl.dungeons

import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.OrderSetting
import catgirlroutes.module.settings.impl.SelectorSetting
import catgirlroutes.utils.ChatUtils.commandAny
import catgirlroutes.utils.Party

object LeapOrganiser : Module(
    "Leap Organiser",
    Category.DUNGEON,
) {
    val leapOrder by OrderSetting(
        "Leap order",
        mapOf("1" to "None", "2" to "None", "3" to "None", "4" to "None"),
        onGuiClosed = {
            if (!LeapOrganiser.enabled || currentOrder == value) return@OrderSetting
            currentOrder = value
            val order = values.joinToString(" ") { it.ifBlank { "_" } }
            when (leapMenu.selected) {
                "SA" -> commandAny("/sa leap $order")
                "Odin" -> commandAny("/od leaporder $order")
            }
        },
        updateAction = {
            val result = value.toMutableMap()
            val used = mutableSetOf<String>()
            val members = Party.members.take(4)

            value.forEach { (key, value) ->
                if (value in members && value !in used) {
                    result[key] = value
                    used.add(value)
                } else {
                    result[key] = ""
                }
            }

            val remaining = members.filterNot { it in used }.iterator()

            result.forEach { (key, value) ->
                if (value.isEmpty() && remaining.hasNext()) {
                    result[key] = remaining.next()
                }
            }

            value = result
        }
    )

    private var currentOrder: Map<String, String> = leapOrder

    private val leapMenu by SelectorSetting("Leap menu", "SA", arrayListOf("SA", "Odin"))
}