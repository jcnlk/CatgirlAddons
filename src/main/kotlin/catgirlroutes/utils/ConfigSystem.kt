package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.configPath
import catgirlroutes.utils.autop3.actions.RingAction
import catgirlroutes.utils.autop3.arguments.RingArgument
import catgirlroutes.utils.TypeName
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Type
import kotlin.properties.ReadOnlyProperty

object ConfigSystem {

    /**
        How to use:
        private val exampleFile = File("config/catgirlroutes/example.json")

        private fun loadWaypoints(): MutableList<[DataClass]> {
        return ConfigSystem.loadConfig(exampleFile, object : TypeToken<MutableList<[DataClass]>>() {}.type) ?: mutableListOf()
        }

        private fun saveWaypoints() {
        ConfigSystem.saveConfig(exampleFile, [Data])

        Alternative usage:
        private val someList by configList<DataClass>("file.json")
        semeList.add(somedata) // should auto save with most methods
        someList.save() // manual saving
        }
    **/
    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(typeAdapter<RingAction>())
        .registerTypeAdapterFactory(typeAdapter<RingArgument>())
        .setPrettyPrinting().create()

    fun saveConfig(file: File, configData: Any) {
        try {
            val writer = FileWriter(file)
            gson.toJson(configData, writer)
            writer.close()
        } catch (e: IOException) {
            //
        }
    }

    fun <T> loadConfig(file: File, type: Type): T? {
        if (!file.exists()) {
            file.createNewFile()
            return null
        }

        try {
            val reader = FileReader(file)
            val configData = gson.fromJson<T>(reader, type)
            reader.close()
            return configData
        } catch (e: Exception) {
            return null
        }
    }

    fun saveToFile(file: File, key: String, value: Any) {
        val configData: MutableMap<String, Any> = loadConfig(file, object : TypeToken<MutableMap<String, Any>>() {}.type) ?: mutableMapOf()
        configData[key] = value
        saveConfig(file, configData)
    }

    inline fun <reified T> loadFromFile(file: File, key: String): T? {
        val configData: Map<String, Any> = loadConfig(file, object : TypeToken<Map<String, Any>>() {}.type) ?: return null
        return configData[key] as? T
    }
}

inline fun <reified T : Any> typeAdapter(
    discriminator: String = "type"
): TypeAdapterFactory {
    if (!T::class.isSealed) {
        return object : TypeAdapterFactory {
            override fun <R> create(gson: Gson, type: TypeToken<R>): TypeAdapter<R>? = null
        }
    }

    val subtypesByName = T::class.sealedSubclasses.associate { subclass ->
        val typeName = subclass.java.getAnnotation(TypeName::class.java)?.value
            ?: error("seally subclass ${subclass.simpleName} gotta be annotated with @TypeName")
        typeName to subclass.java
    }

    return object : TypeAdapterFactory {
        override fun <R> create(gson: Gson, type: TypeToken<R>): TypeAdapter<R>? {
            if (type.rawType != T::class.java) return null

            return object : TypeAdapter<R>() {
                override fun write(out: JsonWriter, value: R) {
                    val json = gson.toJsonTree(value).asJsonObject
                    json.addProperty(discriminator, (value as Any).typeName)
                    Streams.write(json, out)
                }

                override fun read(reader: JsonReader): R {
                    val json = Streams.parse(reader).asJsonObject
                    val label = json.get(discriminator)?.asString
                        ?: error("no discriminator '$discriminator'")
                    val javaType = subtypesByName[label]
                        ?: error("unknow type label: $label")
                    @Suppress("UNCHECKED_CAST")
                    return gson.fromJson(json, javaType) as R
                }
            }.nullSafe()
        }
    }
}

val <T : Any> T.typeName: String // todo make it work with specific types only // todo remove this shit
    get() = this::class.java.getAnnotation(TypeName::class.java)?.value
        ?: error("class ${this::class.simpleName} must be annotated with @TypeName")

// schizo shit to make other shit cleaner
class ConfigList<T>(
    private val file: File,
    private val list: MutableList<T>,
    private val type: Type
) : MutableList<T> by list {

    fun save() = ConfigSystem.saveConfig(file, list)
    fun load() {
        val loaded = ConfigSystem.loadConfig<MutableList<T>>(file, type) ?: return
        list.clear()
        list.addAll(loaded)
    }

    private inline fun <R> modify(block: () -> R): R {
        val result = block()
        save()
        return result
    }

    override fun add(element: T): Boolean = modify { list.add(element) }
    override fun add(index: Int, element: T): Unit = modify { list.add(index, element) }
    override fun addAll(elements: Collection<T>): Boolean = modify { list.addAll(elements) }
    override fun addAll(index: Int, elements: Collection<T>): Boolean = modify { list.addAll(index, elements) }
    override fun remove(element: T): Boolean = modify { list.remove(element) }
    override fun removeAt(index: Int): T = modify { list.removeAt(index) }
    override fun removeAll(elements: Collection<T>): Boolean = modify { list.removeAll(elements) }
    override fun retainAll(elements: Collection<T>): Boolean = modify { list.retainAll(elements) }
    override fun clear(): Unit = modify { list.clear() }
    override fun set(index: Int, element: T): T = modify { list.set(index, element) }
}

inline fun <reified T> configList(name: String): ReadOnlyProperty<Any?, ConfigList<T>> {
    val file = File(configPath, name)
    val type = object : TypeToken<MutableList<T>>() {}.type
    val loadedList: MutableList<T> = ConfigSystem.loadConfig(file, type) ?: mutableListOf()

    return ReadOnlyProperty { _, _ -> ConfigList(file, loadedList, type) }
}
