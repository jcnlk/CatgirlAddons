package catgirlroutes.utils.autop3.arguments

import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonTeammatesNoSelf
import catgirlroutes.utils.dungeon.DungeonUtils.getP3Section
import catgirlroutes.utils.dungeon.P3Sections
import net.minecraft.entity.player.EntityPlayer

@TypeName("leap")
class LeapArgument(val amount: Int) : RingArgument() {

    override fun check(ring: Ring): Boolean { // todo test and prob make a module with the same func
        val section = getP3Section()
        if (section == P3Sections.Unknown) return false

        val sameSection = mutableListOf<EntityPlayer>()

        dungeonTeammatesNoSelf.filter { it.entity != null && !it.entity!!.isInvisible }
            .forEach { player ->
                val playerSection = getP3Section(player.entity!!)
                if (playerSection == P3Sections.Unknown) return@forEach
                if (playerSection == section) sameSection.add(player.entity!!)
            }

        return sameSection.size >= amount
    }
}
