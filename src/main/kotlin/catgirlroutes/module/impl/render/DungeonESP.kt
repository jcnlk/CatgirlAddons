package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.RenderEntityModelEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.*
import catgirlroutes.utils.isOtherPlayer
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.clock.Executor.Companion.register
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.render.OutlineUtils.outlineESP
import catgirlroutes.utils.render.WorldRenderUtils.draw2DBoxByEntity
import catgirlroutes.utils.render.WorldRenderUtils.drawEntityBox
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.passive.EntityBat
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object DungeonESP: Module(
    "Dungeon ESP",
    Category.RENDER
) {

    private val espStyle by SelectorSetting("Esp style","3D", arrayListOf("3D", "2D", "Outline"), "Esp render style to be used.")
    private val espFill by BooleanSetting("Esp fill", false).withDependency { espStyle.selected == "3D"}
    private val boxOffset by NumberSetting("Box size offset", 0.0, -1.0, 1.0, 0.05, "Change box size offset").withDependency { this.espStyle.selected == "3D" }
    private val lineWidth by NumberSetting("Line width", 4.0, 0.0, 8.0, 1.0)

    private val colorDropdown by DropdownSetting("Colors")
    private val colorStar by ColorSetting("Star Color", Color(255, 0, 0), true, "ESP color for star mobs.").withDependency(colorDropdown)
    private val colorSA by ColorSetting("Shadow Color", Color(255, 0, 0), true, "ESP color for shadow assassins.").withDependency(colorDropdown)
    private val colorBat by ColorSetting("Bat Color", Color(255, 0, 0), true, "ESP color for bats.").withDependency(colorDropdown)

    private val fillDropdown by DropdownSetting("Fill").withDependency { espFill }
    private val colorStarFill by ColorSetting("Star Fill", Color(255, 0, 0), true, "ESP color for star mobs.").withDependency(fillDropdown) { espFill }
    private val colorSAFill by ColorSetting("Shadow Fill", Color(255, 0, 0), true, "ESP color for shadow assassins.").withDependency(fillDropdown) { espFill }
    private val colorBatFill by ColorSetting("Bat Fill", Color(255, 0, 0), true, "ESP color for bats.").withDependency(fillDropdown) { espFill }

    private var currentEntities = mutableSetOf<ESPEntity>()

    init {
        Executor(500) {
            if (!inDungeons || !this.enabled) return@Executor
            getEntities()
        }.register()
    }

    data class ESPEntity (
        val entity: Entity,
        val color: Color,
        val fillColor: Color
    )

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        currentEntities = mutableSetOf()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!inDungeons) return
        val entitiesToRemove = mutableListOf<ESPEntity>()
        currentEntities.forEach{espEntity ->
            if (espEntity.entity.isDead) {
                entitiesToRemove.add(espEntity)
                return@forEach
            }
            if (espStyle.selected == "Outline") return@forEach
            when (espStyle.selected) {
                "2D" -> draw2DBoxByEntity(espEntity.entity, espEntity.color, event.partialTicks, lineWidth.toFloat(), true)
                "3D" -> drawEntityBox(espEntity.entity, espEntity.color, espEntity.fillColor, true, espFill, event.partialTicks, lineWidth.toFloat(), this.boxOffset.toFloat())
            }
        }
        currentEntities.removeAll(entitiesToRemove.toSet())
    }

    @SubscribeEvent
    fun onRenderModel(event: RenderEntityModelEvent) {
        if (!inDungeons || espStyle.selected != "Outline") return
        currentEntities.forEach{espEntity ->
            if (espEntity.entity.isDead) {
                currentEntities.remove(espEntity)
                return@forEach
            }
            if (event.entity != espEntity.entity) return@forEach
            outlineESP(event, lineWidth.toFloat(), espEntity.color, true)
        }
    }

    private fun getEntities() {
        mc.theWorld.loadedEntityList.stream().forEach {entity ->
            if (currentEntities.any { it.entity == entity }) return@forEach
            when (entity) {
                is EntityArmorStand -> handleStands(entity)
                is EntityOtherPlayerMP -> handlePlayer(entity)
                is EntityBat -> handleBat(entity)
            }
        }
    }

    private fun handleStands(entity: Entity) {
        val entityName = entity.customNameTag?.let { StringUtils.stripControlCodes(it) } ?: return
        if (entity.name.matches(Regex("^(?:.* )?§6✯ .+ .*§c❤$"))) {
            val correspondingEntity = getMobEntity(entity) ?: return
            currentEntities.add(ESPEntity(correspondingEntity, colorStar, colorStarFill))
        }
    }

    private fun handlePlayer(entity: Entity) {
        if (entity.name.contains("Shadow Assassin")) {
            currentEntities.add(ESPEntity(entity, colorSA, colorSAFill))
        } else if (entity.name == "Diamond Guy" || entity.name == "Lost Adventurer") {
            currentEntities.add(ESPEntity(entity, colorStar, colorStarFill))
        }
    }

    private fun handleBat(entity: EntityBat) {
        if (!listOf(100F, 200F, 400F, 800F).contains(entity.maxHealth)) return
        currentEntities.add(ESPEntity(entity, colorBat, colorBatFill))
    }

    private fun getMobEntity(entity: Entity): Entity? {
        return mc.theWorld?.getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox.offset(0.0, -1.0, 0.0))
            ?.filter { it !is EntityArmorStand && mc.thePlayer != it && !(it is EntityWither && it.isInvisible) && !(it is EntityOtherPlayerMP && it.isOtherPlayer()) }
            ?.minByOrNull { entity.getDistanceToEntity(it) }
    }
}