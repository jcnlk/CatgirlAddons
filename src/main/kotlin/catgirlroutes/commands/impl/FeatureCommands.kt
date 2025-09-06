package catgirlroutes.commands.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.impl.dungeons.LavaClip
import catgirlroutes.module.impl.dungeons.SecretAura
import catgirlroutes.module.impl.misc.AutoClicker.favItemsList
import catgirlroutes.module.impl.misc.InventoryButtons
import catgirlroutes.module.impl.player.BlockClip
import catgirlroutes.module.impl.player.PearlClip
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.skyblockUUID
import com.github.stivais.commodore.Commodore

val pearlClip = Commodore("pearlclip") {
    runs { depth: Double? ->
        PearlClip.pearlClip(depth ?: 0.0)
    }
}

val lavaClip = Commodore("lavaclip") {
    runs { depth: Double? ->
        LavaClip.lavaClipToggle(depth ?: 0.0)
    }
}

val blockClip = Commodore("blockclip") {
    runs {  distance: Double? ->
        BlockClip.blockClip(distance ?: 1.0)
    }
}

val aura = Commodore("cgaaura") {
    literal("enable").runs {
        if (SecretAura.enabled) return@runs
        SecretAura.onKeyBind()
    }

    literal("disable").runs {
        if (!SecretAura.enabled) return@runs
        SecretAura.onKeyBind()
    }

    literal("clear").runs {
        SecretAura.clearBlocks()
        modMessage("Blocks cleared!")
    }
}

val inventoryButtons = Commodore("cgabuttons") {
    runs {
        InventoryButtons.editMode.invoke()
    }
}

val autoClicker = Commodore("cgaac") {
    literal("add").runs {
        val held = mc.thePlayer?.heldItem?.takeIf { it.skyblockUUID.isNotEmpty() } ?: return@runs modMessage("Not holding skyblock item")

        favItemsList.add(held.skyblockUUID)
        modMessage("Added ${held.displayName}!")
    }

    literal("remove").runs {
        val held = mc.thePlayer?.heldItem?.takeIf { it.skyblockUUID.isNotEmpty() } ?: return@runs modMessage("Not holding skyblock item")

        favItemsList.remove(held.skyblockUUID)
        modMessage("Removed ${held.displayName}!")
    }

    literal("clear").runs {
        favItemsList = mutableListOf()
        modMessage("Cleared!")
    }
}