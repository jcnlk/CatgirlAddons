package catgirlroutes.commands.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.impl.dungeons.AutoP3
import catgirlroutes.module.impl.dungeons.AutoP3.inBossOnly
import catgirlroutes.module.impl.dungeons.AutoP3.selectedRoute
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.ChatUtils.getPrefix
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posY
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.autop3.RingsManager
import catgirlroutes.utils.autop3.RingsManager.allRoutes
import catgirlroutes.utils.autop3.RingsManager.currentRoute
import catgirlroutes.utils.autop3.RingsManager.format
import catgirlroutes.utils.autop3.RingsManager.ringEditMode
import catgirlroutes.utils.autop3.actions.*
import catgirlroutes.utils.autop3.arguments.*
import catgirlroutes.utils.dungeon.DungeonUtils.floorNumber
import catgirlroutes.utils.toVec3
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3

val ringTypes: List<String> = listOf("velo", "walk", "look", "stop", "bonzo", "boom", "hclip", "block", "edge", "lavaclip", "jump", "align", "command")
val removedRings: MutableList<MutableList<Ring>> = mutableListOf()

val autoP3Commands = Commodore("p3") {
    literal("help").runs { // todo addarg rmarg
        modMessage("""
            List of AutoP3 commands:
              §7/p3 create §7<§bnamee§7> §8: §rcreates a route
              §7/p3 add §7<§btype§7> [§bdepth§7] [§bargs..§7] §8: §radds a ring (§7/p3 add §rfor info)
              §7/p3 em §8: §rmakes rings inactive
              
              §7/p3 toggle §8: §rtoggles the module
              §7/p3 remove §7<§brange§7>§r §8: §rremoves rings in range (default value - 2)
              §7/p3 undo §8: §rremoves last placed ring
              §7/p3 redo §8: §radds back removed rings
              §7/p3 clearroute §8: §rclears current route
              §7/p3 clear §8: §rclears ALL routes
              §7/p3 load §7[§broute§7]§r §8: §rloads selected route/routes
              §7/p3 save §8: §rsaves current route
              §7/p3 help §8: §rshows this message
        """.trimIndent())
    }

    literal("create").runs { name: String ->
        RingsManager.createRoute(name)
    }

    literal("em").runs {
        ringEditMode = !ringEditMode
        modMessage("EditMode ${if (ringEditMode) "§aenabled" else "§cdisabled"}")
    }

    literal("toggle").runs {
        AutoP3.onKeyBind()
    }

    literal("remove").runs { range: Double? ->
        if (!check) return@runs
        val originalRings = currentRoute.first().rings.toList()

        val r = range ?: 2.0

        val playerBox = AxisAlignedBB(posX - r, posY - r, posZ - r, posX + r, posY + r, posZ + r)

        val allRings = originalRings.filter { ring ->
            !playerBox.intersectsWith(ring.boundingBox())
        }

        currentRoute.first().rings = allRings.toMutableList()

        val rmRings = originalRings - allRings.toSet()
        if (rmRings.isEmpty()) return@runs modMessage("Nothing to remove")

        removedRings.add(rmRings.toMutableList())
        modMessage("Removed ${rmRings.joinToString(", ") { it.format() }}")
        RingsManager.loadSaveRoute()
    }

    literal("undo").runs {
        if (!check) return@runs
        if (currentRoute.isEmpty()) return@runs modMessage("Nothing to undo")

        val lastRing = currentRoute.first().rings.removeLast()
        removedRings.add(mutableListOf(lastRing))
        modMessage("Undone ${lastRing.format()}")
        RingsManager.loadSaveRoute()
    }

    literal("redo").runs {
        if (!check) return@runs
        if (removedRings.isEmpty()) return@runs modMessage("Nothing to redo")

        val lastRemoved = removedRings.removeLast()
        currentRoute.first().rings.addAll(lastRemoved)
        modMessage("Redone ${lastRemoved.joinToString(", ") { it.format() }}")
        RingsManager.loadSaveRoute()
    }

    literal("clearroute").runs {
        if (!check) return@runs
        ChatUtils.createClickableText(
            text = "${getPrefix()} §8»§r Are you sure you want to clear §nCURRENT§r route?",
            hoverText = "Click to clear §nCURRENT§r route!",
            action = "/p3 clearrouteconfirm"
        )
    }

    literal("clearrouteconfirm").runs {
        if (!check) return@runs
        val originalRings = currentRoute.first().rings
        currentRoute.first().rings.clear()
        removedRings.add(originalRings)
        modMessage("Removed ${originalRings.size} rings. Do §7/p3 redo§r to revert")
        RingsManager.loadSaveRoute()
    }

    literal("clear").runs {
        ChatUtils.createClickableText(
            text = "${getPrefix()} §8»§r Are you sure you want to clear §nALL§r routes? It's irreversible!",
            hoverText = "Click to clear §nCURRENT§r route!",
            action = "/p3 clearconfirm"
        )
    }

    literal("clearconfirm").runs {
        modMessage("Cleared all routes")
        RingsManager.clearAll()
        RingsManager.loadRoute()
    }

    literal("save").runs {
        if (!check) return@runs
        modMessage("Saved $selectedRoute")
        RingsManager.saveRoute()
    }

    literal("list").runs {
        modMessage(allRoutes.joinToString("\n") { "${it.name} (${it.rings.size} rings)" })
    }

    literal("load").runs { routes: GreedyString? ->
        val route = (routes ?: selectedRoute).toString()
        RingsManager.loadRoute(route, true)
    }

    literal("add") {
        runs { // todo make it auto build this
            modMessage("""
            Usage: §7/p3 add §7<§btype§7> [§bargs..§7]
                List of types: §7${ringTypes.joinToString()}
                  §7- walk §8: §rmakes the player walk
                  §7- look §8: §rturns player's head
                  §7- stop §8: §rsets player's velocity to 0
                  §7- fullstop §8: §rfully stops the player 
                  §7- bonzo §8: §ruses bonzo staff
                  §7- boom §8: §ruses boom tnt
                  §7- edge §8: §rjumps from block's edge
                  §7- lavaclip §8: §rlava clips with a specified depth
                  §7- jump §8: §rmakes the player jump
                  §7- align §8: §raligns the player with the centre of the ring
                  §7- command §8: §rexecutes a specified command
                  §7- swap §8: §rswaps to a specified item
                  
                List of args: §bl_, w_, r_, h_, delay_, look, walk, term, stop, fullstop, exact, block, centre, ground, leap_, distance_
                  §7- §blook §8: §rturns player's head
                  §7- §bwalk §8: §rmakes the player walk
                  §7- §bterm §8: §ractivates the node when terminal opens
                  §7- §bstop §8: §rsets player's velocity to 0
                  §7- §bfullstop §8: §rfully stops the player 
                  §7- §bblock §8: §rlooks at a block instead of yaw and pitch
                  §7- §bcentre §8: §rexecutes the ring when the player is in the centre
                  §7- §bground §8: §rexecutes the ring when the player is on the ground
                  §7- §bleap_ §8: §rexecutes the ring when N people leapt to the player
                  §7- §bdistance_ §8: §rsets the distance for lavaclip ring
        """.trimIndent())
        }

        runs { type: String, arguments: GreedyString? -> // schizophrenia starts here
            if (!check) return@runs
            val args = arguments?.toString()?.split(" ") ?: emptyList()

            var x = Math.round(mc.renderManager.viewerPosX * 2) / 2.0
            var y = Math.round(mc.renderManager.viewerPosY * 2) / 2.0
            var z = Math.round(mc.renderManager.viewerPosZ * 2) / 2.0

            val yaw = (mc.thePlayer.rotationYaw % 360 + 360) % 360
            val pitch = mc.renderManager.playerViewX

            var length = 1.0f // front to back
            var width = 1.0f // side to side
            var height = 1.0f
            var radius: Float? = null
            var delay: Int? = null
            var distance: Double? = null

            var stringHolder: String? = null

            val ringArgs = mutableListOf<RingArgument>().apply { // todo recode
                args.forEach { arg ->
                    Regex("^(\\w+?)(\\d*\\.?\\d*)\$").find(arg)?.destructured?.let { (flag, value) ->
                        when(flag.lowercase()) {
                            "l", "length"   -> length = value.toFloatOrNull() ?: return@runs invalidUsage("length", "l")
                            "w", "width"    -> width = value.toFloatOrNull() ?: return@runs invalidUsage("width", "w")
                            "h", "height"   -> height = value.toFloatOrNull() ?: return@runs invalidUsage("height", "h")
                            "r", "radius"   -> radius = value.toFloatOrNull() ?: return@runs invalidUsage("radius", "r")
                            "delay"         -> delay = value.toIntOrNull() ?: return@runs invalidUsage("delay")
                            "distance"      -> distance = value.toDoubleOrNull() ?: return@runs invalidUsage("distance")

                            "leap"          -> add(LeapArgument(value.toIntOrNull() ?: return@runs invalidUsage("leap") ))
                            "block"         -> add(BlockArgument(mc.thePlayer.rayTrace(40.0, 1f).hitVec))
                            "stop"          -> add(StopArgument())
                            "fullstop"      -> add(StopArgument(true))
                            "term"          -> add(TermArgument)
                            "centre", "center" -> add(CentreArgument)
                            "ground", "onground" -> add(GroundArgument)
                            "look", "rotate" -> add(LookArgument)

                            "exact" -> {
                                x = mc.renderManager.viewerPosX
                                y = mc.renderManager.viewerPosY
                                z = mc.renderManager.viewerPosZ
                            }

                            else -> {}
                        }
                    }

                    stringHolder = when (type.lowercase()) {
                        "command", "cmd" -> Regex(""""([^"]*)"""").find(arguments.toString())?.destructured?.component1()
                        "swap" -> Regex(""""([^"]*)"""").find(arguments.toString())?.destructured?.component1()
                        else -> null
                    }
                }
            }.takeIf { it.isNotEmpty() }

            val action = when(type.lowercase()) {
                "align"     -> AlignRing
                "edge"      -> EdgeRing
                "hclip"     -> HClipRing
                "jump"      -> JumpRing
                "swap"      -> SwapRing(stringHolder ?: return@runs invalidUsageString("swap"))
                "command", "cmd" -> CommandRing(stringHolder ?: return@runs invalidUsageString("command", "cmd"))
                "lavaclip", "lc", "clip", "vclip" -> LavaClipRing(distance ?: 0.0)
                "look", "rotate" -> LookRing
                "motion", "velo" -> MotionRing
                "stop"      -> StopRing()
                "fullstop"  -> StopRing(true)
                "bonzo"     -> UseItemRing("bonzo's staff")
                "walk"      -> WalkRing
                "boom"      -> mc.objectMouseOver
                    ?.takeIf { it.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK }
                    ?.blockPos
                    ?.toVec3()
                    ?.let { BoomRing(it) }
                    ?: return@runs modMessage("Look at block")
                else -> return@runs modMessage("Unknown ring type")
            }

            val isFacingX = (yaw in 45.0..135.0) || (yaw in 225.0..315.0)

            val (finalLength, finalWidth) = radius?.let { it to it } ?: run {
                if (isFacingX) width to length
                else length to width
            }

            if ((action is BoomRing) && (finalLength > 1.0f || finalWidth > 1.0f)) return@runs modMessage("Ring is too big")

            val ring = Ring(action, Vec3(x, y, z), yaw, pitch, ringArgs, finalLength, finalWidth, height, delay)
            if (!RingsManager.addRing(ring)) return@runs modMessage("Route §7$selectedRoute§r doesn't exist. Do §7/p3 create §7<§bname§7>")
            modMessage("Added ${ring.format()}")
            RingsManager.loadSaveRoute()
        } // schizophrenia ends here
    }
}

fun invalidUsage(name: String, vararg aliases: String) {
    modMessage("Usage: §7/p3 add §7<§btype§7> ${name}<§bnum§7> [§bother args..§7]" + if (aliases.isNotEmpty()) " §rAliases: ${aliases.joinToString(", ") { it }}" else "")
}

fun invalidUsageString(name: String, vararg  aliases: String) {
    modMessage("Usage: §7/p3 add §7$name \"§bvalue§7\" [§bargs..§7]" + if (aliases.isNotEmpty()) " §rAliases: ${aliases.joinToString(", ") { it }}" else "")
}

val check get(): Boolean {
    if (currentRoute.size > 1) {
        modMessage("Can't edit when multiple routes loaded. Loaded routes: §7${currentRoute.joinToString(", ") { it.name }}")
        return false
    }
    if (inBossOnly && floorNumber != 7) {
        return false
    }
    return true
}