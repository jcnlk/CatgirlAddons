package catgirlroutes.utils.autop3.actions

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import net.minecraftforge.client.ClientCommandHandler

@TypeName("command")
class CommandRing(val command: String, val clientOnly: Boolean = false) : RingAction() {

    override fun execute(ring: Ring) {
        if (clientOnly) {
            mc.thePlayer?.let {
                val commandText = command.removePrefix("/")
                ClientCommandHandler.instance.executeCommand(it, "/$commandText")
            }
        } else {
            ChatUtils.commandAny(command)
        }
    }
}