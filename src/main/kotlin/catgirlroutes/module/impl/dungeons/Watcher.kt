package catgirlroutes.module.impl.dungeons

import catgirlroutes.events.impl.EntityRemovedEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.HudSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.ui.clickgui.util.FontUtil.drawStringWithShadow
import catgirlroutes.utils.ClientListener.scheduleTask
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object Watcher : Module(
    name = "Watcher",
    category = Category.DUNGEON
) {
    val killTitle = BooleanSetting("Display title", false)
    val displayTime = NumberSetting("Display time", 40.0, 20.0, 100.0)
    val hud by HudSetting{
        size("KILL NOW!")
        visibleIf { title }
        render { drawStringWithShadow("KILL NOW!", 0.0, 0.0, Color.PINK.rgb) }
    }

    private var title = false

    init {
        this.addSettings(killTitle, displayTime)
    }

    @SubscribeEvent
    fun onEntityLeave(event: EntityRemovedEvent) {
        if (event.entity.name.contains("Let's see how you can handle this.") && killTitle.value) {
            title = true
            scheduleTask(displayTime.value.toInt()) { title = false }
        }
    }
}