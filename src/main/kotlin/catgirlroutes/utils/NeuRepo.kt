package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.scope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.util.ResourceLocation

object NeuRepo {
    var repoItems = mutableListOf<RepoItem>()
    var mobs = mutableListOf<JsonObject>()
    var constants = mutableListOf<JsonObject>()

    var reforges = listOf<ReforgeData>()
    var essence = listOf<EssenceData>()
    var gemstones = listOf<GemstoneData>()

    private val gson = Gson()
    private const val UPDATE_INTERVAL = 60 * 60 * 1000L

    init {
        updateRepo()
        autoUpdate()
    }

    private fun autoUpdate() {
        scope.launch {
            while (true) {
                delay(UPDATE_INTERVAL)
                updateRepo(false)
            }
        }
    }

    private fun updateRepo(firstLoad: Boolean = true) {
        scope.launch {
            val startTime = System.currentTimeMillis()

            val result = downloadRepo()
            result?.let { (items, mobsList, constantsList) ->
                processRepoItems(items)
                mobs = mobsList.toMutableList()
                constants = constantsList.toMutableList()

                if (firstLoad) {
                    println("Constants size: ${constants.size}")

                    try {
                        val reforgeData = constants.find { constant ->
                            constant.has("reforgeName") ||
                                    constant.entrySet().any { it.value.isJsonObject && it.value.asJsonObject.has("reforgeName") }
                        }
                        reforges = processReforges(reforgeData)
                        println("Loaded ${reforges.size} reforges")
                    } catch (e: Exception) {
                        println("Error processing reforges: ${e.message}")
                        reforges = emptyList()
                    }

                    try {
                        val essenceData = constants.find { constant ->
                            constant.entrySet().any { entry ->
                                entry.value.isJsonObject &&
                                        entry.value.asJsonObject.has("type")
                            }
                        }
                        essence = processEssence(essenceData)
                        println("Loaded ${essence.size} essence types")
                    } catch (e: Exception) {
                        println("Error processing essence: ${e.message}")
                        essence = emptyList()
                    }

                    //gemstones = processGemstones(constants.getOrNull(13))
                }

                val auctionJob = launch { updateItemsWithAuctionData() }
                val bazaarJob = launch { updateItemsWithBazaarData() }
                val npcJob = if (firstLoad) launch { updateItemsWithNpcData() } else null

                auctionJob.join()
                bazaarJob.join()
                npcJob?.join()

                val totalTime = System.currentTimeMillis() - startTime
                println("Loaded NeuRepo in ${totalTime}ms (${repoItems.size} items)")
            }
        }
    }

    private suspend fun processRepoItems(items: List<JsonObject>) = withContext(Dispatchers.IO) {
        repoItems = items.mapNotNull { itemJson ->
            runCatching {
                gson.fromJson(itemJson, RepoItem::class.java).apply {
                    if (skyblockID.contains(";") && id == "minecraft:enchanted_book") {
                        skyblockID.split(";").takeIf { it.size == 2 }?.let { (enchantment, tier) ->
                            skyblockID = "ENCHANTMENT_${enchantment}_$tier"
                            name = enchantment.formatEnchantment(tier.toInt())
                        }
                    }
                }
            }.getOrNull()
        }.toMutableList()
    }

    private suspend fun processReforges(reforgeJson: JsonObject?): List<ReforgeData> = withContext(Dispatchers.IO) {
        reforgeJson?.entrySet()?.mapNotNull { (key, value) ->
            val data = value.asJsonObject
            ReforgeData(
                key,
                data["reforgeName"].asString,
                data["reforgeCosts"].asJsonObject.entrySet()
                    .associate { (rarity, cost) -> rarity to cost.asDouble }
            )
        } ?: emptyList()
    }

    private suspend fun processEssence(essenceJson: JsonObject?): List<EssenceData> =
        withContext(Dispatchers.IO) {
            essenceJson?.entrySet()?.mapNotNull { (key, value) ->
                val data = value.asJsonObject ?: return@mapNotNull null
                val essenceType = data["type"]?.asString?.uppercase() ?: return@mapNotNull null
                val itemsData = data["items"]?.asJsonObject

                val upgrades = (1..15).mapNotNull { star ->
                    val cost = data[star.toString()]?.asInt ?: return@mapNotNull null

                    val items = itemsData?.get(star.toString())
                        ?.asJsonArray
                        ?.mapNotNull { element ->
                            val parts = element.asString.split(":")
                            if (parts.size == 2) {
                                val id = parts[0]
                                val amount = parts[1].toIntOrNull()
                                if (amount != null) UpgradeItem(id, amount) else null
                            } else null
                        } ?: emptyList()

                    EssenceUpgrade(star, cost, items)
                }

                EssenceData(key, "ESSENCE_$essenceType", upgrades)
            } ?: emptyList()
        }

    private suspend fun updateItemsWithAuctionData() {
        val auctionDataJson = getDataFromServer("https://moulberry.codes/lowestbin.json")
        if (auctionDataJson.isNotEmpty()) {
            val auctionMap = withContext(Dispatchers.IO) {
                gson.fromJson(auctionDataJson, JsonObject::class.java)
                    .entrySet().associate { it.key to it.value.asDouble }
            }

            val itemMap = repoItems.associateBy { it.skyblockID }

            auctionMap.forEach { (id, price) ->
                itemMap[id]?.let { item ->
                    item.auction = price > 0.0
                    item.price = price
                }
            }
        }
    }

    private suspend fun updateItemsWithBazaarData() {
        val bazaarDataJson = getDataFromServer("https://api.hypixel.net/v2/skyblock/bazaar")
        if (bazaarDataJson.isEmpty()) return

        val jsonObject = withContext(Dispatchers.IO) {
            gson.fromJson(bazaarDataJson, JsonObject::class.java)
        }

        val products = jsonObject.getAsJsonObject("products") ?: return

        val itemMap = repoItems.associateBy { it.skyblockID }

        products.entrySet().forEach { (id, product) ->
            product.asJsonObject?.getAsJsonObject("quick_status")?.get("buyPrice")?.asDouble?.let { price ->
                itemMap[id]?.let { item ->
                    item.bazaar = true
                    item.price = price
                }
            }
        }
    }

    private suspend fun updateItemsWithNpcData() {
        val itemDataJson = getDataFromServer("https://api.hypixel.net/resources/skyblock/items")
        if (itemDataJson.isEmpty()) return

        val jsonObject = withContext(Dispatchers.IO) {
            gson.fromJson(itemDataJson, JsonObject::class.java)
        }

        val items = jsonObject.getAsJsonArray("items") ?: return

        val itemMap = repoItems.associateBy { it.skyblockID }

        val itemMapById = items.associate {
            it.asJsonObject.get("id").asString to it.asJsonObject
        }

        itemMapById.forEach { (id, itemObj) ->
            itemObj.get("npc_sell_price")?.asDouble?.let { price ->
                itemMap[id]?.npcPrice = price
            }
        }
    }

    fun getItemFromID(skyblockID: String): RepoItem? = repoItems.find { it.skyblockID == skyblockID }

    fun getItemFromName(name: String, contains: Boolean = true): RepoItem? {
        return if (contains) {
            repoItems.find { it.name.contains(name) || it.name.noControlCodes.contains(name) }
        } else {
            repoItems.find { it.name == name || it.name.noControlCodes == name || it.name.noControlCodes.removePrefix("[Lvl {LVL}] ") == name }
        }
    }

    fun getReforgeFromID(skyblockID: String): ReforgeData? {
        return reforges.find { it.skyblockID == skyblockID }
    }

    fun getReforgeFromName(name: String): ReforgeData? {
        return reforges.find { it.name.equals(name, true) || it.name.noControlCodes.equals(name, true) }
    }

    fun getEssenceDataFromID(skyblockID: String): EssenceData? {
        return essence.find { it.skyblockID == skyblockID }
    }

    fun getGemstoneData(skyblockID: String): GemstoneData? {
        return gemstones.find { it.skyblockID == skyblockID }
    }

    // modified schizo shit from neu
    private val itemStackCache: MutableMap<String, ItemStack> = HashMap()
    fun RepoItem.toStack(
        useCache: Boolean = true,
        copyStack: Boolean = false
    ): ItemStack {
        var cacheEnabled = useCache

        if (this.skyblockID == "_") cacheEnabled = false

        if (cacheEnabled) {
            itemStackCache[this.skyblockID]?.let { stack ->
                return if (copyStack) stack.copy() else stack
            }
        }

        val stack = ItemStack(net.minecraft.item.Item.itemRegistry.getObject(ResourceLocation(this.id)), 1, this.damage)

        if (stack.item == null) {
            return ItemStack(net.minecraft.item.Item.getItemFromBlock(Blocks.stone), 0, 255) // Purple broken texture item
        } else {
            try {
                val tag: NBTTagCompound = JsonToNBT.getTagFromJson(this.nbt)
                stack.tagCompound = tag
            } catch (ignored: NBTException) { }

            val display = NBTTagCompound().apply {
                if (stack.tagCompound != null && stack.tagCompound!!.hasKey("display")) {
                    this.merge(stack.tagCompound!!.getCompoundTag("display"))
                }
                this.setTag("Lore", this@toStack.lore.processLore())
            }
            val tag = stack.tagCompound ?: NBTTagCompound()
            tag.setTag("display", display)
            stack.tagCompound = tag
        }

        if (cacheEnabled) itemStackCache[this.id] = stack
        return if (copyStack) stack.copy() else stack
    }

    private fun List<String>.processLore(): NBTTagList {
        val nbtLore = NBTTagList()
        for (line in this) {
            if (!line.contains("Click to view recipes!") &&
                !line.contains("Click to view recipe!")) {
                nbtLore.appendTag(NBTTagString(line))
            }
        }
        return nbtLore
    }
}

data class RepoItem(
    @SerializedName("itemid") val id: String,
    @SerializedName("displayname") var name: String,
    @SerializedName("nbttag") val nbt: String,
    @SerializedName("lore") val lore: List<String>,
    @SerializedName("internalname") var skyblockID: String,
    @SerializedName("damage") val damage: Int,
    var auction: Boolean = false,
    var bazaar: Boolean = false,
    var price: Double = 0.0,
    var npcPrice: Double = 0.0
)

data class ReforgeData(
    @SerializedName("internalname") val skyblockID: String,
    val name: String,
    val costs: Map<String, Double>
)

data class EssenceUpgrade(
    val star: Int,
    val cost: Int,
    val items: List<UpgradeItem>
)

data class UpgradeItem(
    val skyblockID: String,
    val amount: Int
)

data class EssenceData(
    val skyblockID: String,
    val essenceID: String,
    val upgrades: List<EssenceUpgrade>
)

data class GemstoneCost( // idk
    val skyblockID: String,
    val amount: Int
)

data class GemstoneSlot(
    val gemstoneType: String,
    val slotIndex: Int
)

data class GemstoneData(
    @SerializedName("internalname") val skyblockID: String,
    val slots: Map<GemstoneSlot, List<GemstoneCost>>
)