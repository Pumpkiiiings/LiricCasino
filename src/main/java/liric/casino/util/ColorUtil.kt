package liric.casino.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Utilidad de colores unificada.
 * Acepta tres formatos de color en el mismo string:
 *  - Legacy  : &a &b &c ... &l &o &r
 *  - Hex     : &#RRGGBB  (estilo BungeeCord / CMI)
 *  - MiniMsg : <red> <#FF0000> <gradient:...> etc.
 */
object ColorUtil {

    private val mm = MiniMessage.miniMessage()

    // Mapa de códigos legacy a tags MiniMessage
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

    /**
     * Parsea un string con cualquier mezcla de formatos y devuelve un Component.
     */
    fun parse(text: String): Component {
        var s = text

        // 1. &#RRGGBB  →  <#RRGGBB>
        s = s.replace(Regex("&#([0-9A-Fa-f]{6})")) { "<#${it.groupValues[1]}>" }

        // 2. &x  y  §x  →  <tag_mm>
        s = translateLegacy(s)

        // 3. Deserializar con MiniMessage (añadiendo <!italic> para evitar cursiva por defecto en items)
        return mm.deserialize("<!italic>$s")
    }

    /**
     * Parsea el string y devuelve el texto plano sin colores (útil para logs).
     */
    fun stripColor(text: String): String {
        return mm.stripTags(translate(text))
    }

    /**
     * Convierte el string al formato MiniMessage sin deserializar.
     * Útil si necesitas el string intermedio para concatenar.
     */
    fun translate(text: String): String {
        var s = text
        s = s.replace(Regex("&#([0-9A-Fa-f]{6})")) { "<#${it.groupValues[1]}>" }
        s = translateLegacy(s)
        return s
    }

    // ─── Interno ─────────────────────────────────────────────────────────────

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
