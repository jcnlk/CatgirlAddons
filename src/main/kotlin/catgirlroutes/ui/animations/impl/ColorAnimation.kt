package catgirlroutes.ui.animations.impl

import catgirlroutes.utils.render.Color
import kotlin.math.roundToInt
import java.awt.Color as JavaColor

class ColorAnimation(duration: Long) {

    private val anim = LinearAnimation<Int>(duration)

    fun start(bypass: Boolean = false): Boolean {
        return anim.start(bypass)
    }

    fun isAnimating(): Boolean {
        return anim.isAnimating()
    }

    fun percent(): Int {
        return anim.getPercent()
    }

    fun get(start: JavaColor, end: JavaColor, reverse: Boolean): JavaColor {
        val p = (if (anim.isAnimating()) anim.getPercent() else 100) / 100f
        val t = if (reverse) 1f - p else p

        val r = (start.red + (end.red - start.red) * t).roundToInt().coerceIn(0, 255)
        val g = (start.green + (end.green - start.green) * t).roundToInt().coerceIn(0, 255)
        val b = (start.blue + (end.blue - start.blue) * t).roundToInt().coerceIn(0, 255)
        val a = (start.alpha + (end.alpha - start.alpha) * t).roundToInt().coerceIn(0, 255)

        return Color(r, g, b, a / 255f).javaColor
    }
}