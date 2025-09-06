package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("command")
class CommandRing(val command: String) : RingAction() {

    override fun execute(ring: Ring) {
        ChatUtils.commandAny(command)
    }
}
