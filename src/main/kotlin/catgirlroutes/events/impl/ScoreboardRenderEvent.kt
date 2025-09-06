package catgirlroutes.events.impl

import net.minecraft.client.gui.ScaledResolution
import net.minecraft.scoreboard.ScoreObjective
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

@Cancelable
class ScoreboardRenderEvent(var objective: ScoreObjective, var resolution: ScaledResolution) : Event()