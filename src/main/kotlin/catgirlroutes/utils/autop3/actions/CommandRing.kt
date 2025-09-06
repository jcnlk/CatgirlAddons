package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import kotlinx.serialization.Transient

@TypeName("command")
class CommandRing(val command: String) : RingAction() {
//    @Transient
//    override val description: String = "executes a specified command"
//    @Transient
//    override val aliases: List<String> = listOf("cmd")

    override fun execute(ring: Ring) {
        ChatUtils.commandAny(command)
    }
}
