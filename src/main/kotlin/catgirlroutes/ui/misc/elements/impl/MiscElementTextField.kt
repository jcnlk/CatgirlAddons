package catgirlroutes.ui.misc.elements.impl

import catgirlroutes.ui.clickgui.util.FontUtil.drawString
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.ui.clickgui.util.FontUtil.trimToWidth
import catgirlroutes.ui.misc.elements.ElementDSL
import catgirlroutes.ui.misc.elements.MiscElement
import catgirlroutes.ui.misc.elements.MiscElementStyle
import catgirlroutes.utils.dropAt
import catgirlroutes.utils.noControlCodes
import catgirlroutes.utils.substringSafe
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Color.LIGHT_GRAY
import java.awt.Color.WHITE
import kotlin.math.max
import kotlin.math.min

class MiscElementTextField(
    style: MiscElementStyle = MiscElementStyle(),
    var maxLength: Int = 0,
    var placeholder: String = "",
    var options: Int = 0,
    var prependText: String = "",
) : MiscElement(style) {
    private var cursorPos = 0
    private var selectionPos = 0
    private var scrollOffset = 0
    var isFocused = false

    var text by ::value
    val textNoCodes get() = text.noControlCodes
    private var _text: String
        get() = prependText + text
        set(value) {
            text = value
        }

    private val lengthText get() = if (maxLength != 0) "${text.length}/${maxLength}" else ""

    private val availableWidth get() = width - lengthText.getWidth() - 8.0

    private val selectionLeft get() = min(selectionPos, cursorPos).coerceIn(0, text.length)
    private val selectionRight get() = max(selectionPos, cursorPos).coerceIn(0, text.length)
    private val selectedText get() = text.substring(selectionLeft, selectionRight)

    private fun deleteSelection() {
        val from = selectionLeft
        val to = selectionRight

        if (from == to) return

        text = text.removeRange(from, to)
        cursorPos = from
        selectionPos = from
    }

    private val previousWord: Int
        get() {
            var pos = cursorPos
            if (pos == 0) return pos

            if (text[pos - 1].isWhitespace()) {
                while (pos > 0 && text[pos - 1].isWhitespace()) {
                    pos--
                }
            }
            if (pos > 0) {
                val ch = text[pos - 1]
                if (ch.isLetterOrDigit()) {
                    while (pos > 0 && text[pos - 1].isLetterOrDigit()) {
                        pos--
                    }
                } else {
                    while (pos > 0 && text[pos - 1] == ch) {
                        pos--
                    }
                }
            }
            return pos
        }

    private val nextWord: Int
        get() {
            var pos = cursorPos
            if (pos >= text.length) return pos

            if (text[pos].isWhitespace()) {
                while (pos < text.length && text[pos].isWhitespace()) {
                    pos++
                }
            }
            if (pos < text.length) {
                val ch = text[pos]
                if (ch.isLetterOrDigit()) {
                    while (pos < text.length && text[pos].isLetterOrDigit()) {
                        pos++
                    }
                } else {
                    while (pos < text.length && text[pos] == ch) {
                        pos++
                    }
                }
            }
            return pos
        }

    private fun updateScrollOffset() {
        val textWidth = availableWidth - prependText.getWidth()

        if (cursorPos < scrollOffset) {
            scrollOffset = cursorPos
        }

        while (scrollOffset < cursorPos) {
            val currentWidth = text.substring(scrollOffset, cursorPos).getWidth()
            if (currentWidth <= textWidth) break
            scrollOffset++
        }

        while (scrollOffset > 0) {
            val newWidth = text.substring(scrollOffset - 1).getWidth()
            if (newWidth >= textWidth) break
            scrollOffset--
        }

        scrollOffset = scrollOffset.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    }

    private fun getCursorPos(mouseX: Int): Int {
        val relX = (mouseX - x - 5 - prependText.getWidth()).coerceAtLeast(0.0)
        var width = 0.0
        var pos = scrollOffset

        if (text.isEmpty()) return 0

        for (i in scrollOffset until min(scrollOffset + availableWidth.toInt(), text.length)) {
            val charWidth = text[i].getWidth()
            if (width + charWidth / 2 > relX) return pos
            width += charWidth
            pos++
        }
        return pos.coerceAtMost(text.length)
    }

    private fun selectWord() {
        var start = cursorPos
        var end = cursorPos
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++

        selectionPos = start
        cursorPos = end
        updateScrollOffset()
    }

    override fun render(mouseX: Int, mouseY: Int) {
        drawRoundedBorderedRect(x, y, width, height, radii, thickness, colour, if (this.isFocused) outlineHoverColour else outlineColour)

        val yPos = y + (height - 8) / 2
        val visibleText = _text.substring(scrollOffset).trimToWidth(availableWidth)

        if (this.isFocused && System.currentTimeMillis() % 1000 > 500) {
            val cursorOffset = (prependText.length + cursorPos - scrollOffset).coerceAtLeast(0)
            val cursorX = x + 5 + visibleText.substringSafe(0, cursorOffset).getWidth()
            drawRoundedRect(cursorX, yPos - 1, 1.0, 10.0, 1.0, WHITE)
        }

        if (selectedText.isNotEmpty()) {
            val start = max(selectionLeft + prependText.length, scrollOffset)
            val end = min(selectionRight + prependText.length, scrollOffset + availableWidth.toInt())

            if (start < end) {
                val unselectedWidth = _text.substring(scrollOffset, start).getWidth()
                val selectionWidth = _text.substring(start, end).getWidth()

                drawRoundedRect(x + 5 + unselectedWidth, yPos - 1, selectionWidth, 10.0, 0.1, Color(110, 180, 255, 150))
            }
        }

        if (visibleText.isNotEmpty()) drawString(visibleText, x + 5, yPos, style.textColour.rgb)
        else if (text.isEmpty()) drawString(this.placeholder, x + 5 + this.prependText.getWidth(), yPos, LIGHT_GRAY.rgb)

        drawString(lengthText, x + availableWidth + 5, yPos, if (text.length == maxLength) Color.RED.rgb else LIGHT_GRAY.rgb)
    }

    private var lastClickTime = 0L
    private var clicks = 0
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) {
            isFocused = false
            selectionPos = cursorPos
            return false
        }

        when(mouseButton) {
            0 -> {
                val now = System.currentTimeMillis()
                clicks = if (now - lastClickTime < 250) clicks + 1 else 1
                lastClickTime = now

                when (clicks) {
                    1 -> {
                        cursorPos = getCursorPos(mouseX)
                        selectionPos = cursorPos
                    }
                    2 -> {
                        selectWord()
                    }
                    3 -> {
                        cursorPos = text.length
                        selectionPos = 0
                    }
                    else -> clicks = 0
                }
            }
            1 -> {
                text = ""
                cursorPos = 0
            }
            else -> {
                cursorPos = getCursorPos(mouseX)
                selectionPos = cursorPos
            }
        }

        isFocused = true
        updateScrollOffset()
        return true
    }


    override fun mouseClickMove(mouseX: Int, mouseY: Int, mouseButton: Int, timeSinceLastClick: Long) {
        if (!isFocused || !isHovered(mouseX, mouseY)) return
        selectionPos = getCursorPos(mouseX)
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!isFocused) return false
        val isShift = GuiScreen.isShiftKeyDown()
        val isCtrl = GuiScreen.isCtrlKeyDown()

        if (isCtrl) {
            when(keyCode) {
                Keyboard.KEY_A -> {
                    cursorPos = text.length
                    selectionPos = 0
                }
                Keyboard.KEY_C -> {
                    GuiScreen.setClipboardString(selectedText)
                }
                Keyboard.KEY_V -> {
                    writeText(GuiScreen.getClipboardString())
                }
                Keyboard.KEY_X -> {
                    GuiScreen.setClipboardString(selectedText)
                    deleteSelection()
                }
                Keyboard.KEY_Z -> {
                    if (isShift) {} // todo redo
                    // todo undo
                }
                Keyboard.KEY_Y -> {
                    // todo redo
                }
            }
        }

        when(keyCode) {
            Keyboard.KEY_ESCAPE -> isFocused = false
            Keyboard.KEY_BACK -> {
                when {
                    selectionPos != cursorPos -> deleteSelection()
                    isCtrl -> previousWord.let { start ->
                        if (start < cursorPos) {
                            text = text.dropAt(start, cursorPos - start)
                            cursorPos = start
                            selectionPos = cursorPos
                        }
                    }
                    cursorPos > 0 -> {
                        text = text.dropAt(cursorPos, -1)
                        cursorPos--
                        selectionPos = cursorPos
                    }
                }
            }
            Keyboard.KEY_DELETE -> {
                when {
                    selectionPos != cursorPos -> deleteSelection()
                    isCtrl -> nextWord.let { end ->
                        if (end > cursorPos) {
                            text = text.dropAt(cursorPos, end - cursorPos)
                        }
                    }
                    cursorPos < text.length -> {
                        text = text.dropAt(cursorPos, 1)
                    }
                }
            }
            Keyboard.KEY_HOME -> cursorPos = 0
            Keyboard.KEY_END -> cursorPos = text.length
            Keyboard.KEY_RIGHT -> {
                val newPos = if (isCtrl) nextWord else cursorPos + 1
                cursorPos = newPos.coerceAtMost(text.length)
                if (!isShift) selectionPos = cursorPos
            }
            Keyboard.KEY_LEFT -> {
                val newPos = if (isCtrl) previousWord else cursorPos - 1
                cursorPos = newPos.coerceAtLeast(0)
                if (!isShift) selectionPos = cursorPos
            }
            else -> {
                // todo options
                writeText(typedChar)
            }
        }

        updateScrollOffset()
        return true
    }

    private fun isAllowedCharacter(char: Char): Boolean = char >= ' ' && char.code != 127

    private fun writeText(string: String) {
        string.toCharArray().forEach { writeText(it) }
    }

    private fun writeText(c: Char) {
        if (!isAllowedCharacter(c)) return
        if (selectionPos != cursorPos) deleteSelection()
        if (maxLength != 0 && text.length >= maxLength) return

        var char = c
        if (char == '\r') char = '\n'
        text = text.substringSafe(0, cursorPos) + char + text.substringSafe(cursorPos, text.length)
        cursorPos++
        selectionPos = cursorPos
    }
}

class TextFieldBuilder : ElementDSL<MiscElementTextField>() {
    var text by _style::value
    var maxLength: Int = 0
    var placeholder: String = ""
    var options: Int = 0
    var prependText: String = ""
    var isFocused: Boolean = false

    override fun buildElement(): MiscElementTextField {
        val t = MiscElementTextField(createStyle(), maxLength, placeholder, options, prependText)
        t.isFocused = isFocused
        return t
    }
}

fun textField(block: TextFieldBuilder.() -> Unit): MiscElementTextField {
    return TextFieldBuilder().apply(block).build()
}