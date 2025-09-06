package catgirlroutes.config.jsonutils

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.impl.*
import com.google.gson.*
import java.lang.reflect.Type

class SettingDeserializer: JsonDeserializer<Setting<*>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Setting<*> {
        if (!json.isJsonObject || json.asJsonObject.entrySet().isEmpty()) {
            return DummySetting("undefined")
        }

        val (name, value) = json.asJsonObject.entrySet().first()

        return when {
            value.isJsonPrimitive -> handlePrimitive(name, value.asJsonPrimitive)
            value.isJsonArray -> ListSetting(name, parseList(value.asJsonArray, context))
            value.isJsonObject -> MapSetting(name, parseMap(value.asJsonObject, context))
            else -> DummySetting("invalid json")
        }
    }

    private fun handlePrimitive(name: String, value: JsonPrimitive): Setting<*> = when {
        value.isBoolean -> BooleanSetting(name, value.asBoolean)
        value.isNumber -> NumberSetting(name, value.asDouble)
        value.isString -> StringSetting(name, value.asString)
        else -> DummySetting("invalid primitive")
    }

    private fun parseList(array: JsonArray, context: JsonDeserializationContext): MutableList<Any?> {
        return array.map { element ->
            parseValue(element, context)
        }.toMutableList()
    }

    private fun parseMap(obj: JsonObject, context: JsonDeserializationContext): MutableMap<String, Any?> {
        return obj.entrySet().associate { (key, value) ->
            key to parseValue(value, context)
        }.toMutableMap()
    }

    private fun parseValue(element: JsonElement, context: JsonDeserializationContext): Any? {
        return when {
            element.isJsonNull -> null
            element.isJsonPrimitive -> when {
                element.asJsonPrimitive.isBoolean -> element.asBoolean
                element.asJsonPrimitive.isNumber -> element.asNumber
                element.asJsonPrimitive.isString -> element.asString
                else -> null
            }
            element.isJsonArray -> parseList(element.asJsonArray, context)
            element.isJsonObject -> {
                val obj = element.asJsonObject
                if (obj.has("_dataClass")) {
                    try {
                        val clazz = Class.forName(obj.get("_dataClass").asString)
                        context.deserialize(obj, clazz)
                    } catch (e: Exception) {
                        parseMap(obj, context)
                    }
                } else {
                    parseMap(obj, context)
                }
            }
            else -> null
        }
    }
}