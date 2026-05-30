package liric.casino.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage


object ColorUtil {

    private val mm = MiniMessage.miniMessage()


    private val legacyMap = mapOf(
        '0' to "<black>",        '1' to "<dark_blue>",
        '2' to "<dark_green>",   '3' to "<dark_aqua>",
        '4' to "<dark_red>",     '5' to "<dark_purple>",
        '6' to "<gold>",         '7' to "<gray>",
        '8' to "<dark_gray>",    '9' to "<blue>",
        'a' to "<green>",        'b' to "<aqua>",
        'c' to "<red>",          'd' to "<light_purple>",
        'e' to "<yellow>",       'f' to "<white>",
        'r' to "<reset>",
        'l' to "<bold>",         'o' to "<italic>",
        'n' to "<underlined>",   'm' to "<strikethrough>",
        'k' to "<obfuscated>"
    )


    fun parse(text: String): Component {
        var s = text


        s = s.replace(Regex("&#([0-9A-Fa-f]{6})")) { "<#${it.groupValues[1]}>" }


        s = translateLegacy(s)


        return mm.deserialize("<!italic>$s")
    }


    fun stripColor(text: String): String {
        return mm.stripTags(translate(text))
    }


    fun translate(text: String): String {
        var s = text
        s = s.replace(Regex("&#([0-9A-Fa-f]{6})")) { "<#${it.groupValues[1]}>" }
        s = translateLegacy(s)
        return s
    }



    private fun translateLegacy(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if ((c == '&' || c == '§') && i + 1 < text.length) {
                val next = text[i + 1].lowercaseChar()
                val tag = legacyMap[next]
                if (tag != null) {
                    sb.append(tag)
                    i += 2
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
