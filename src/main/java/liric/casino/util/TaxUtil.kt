package liric.casino.util

import liric.casino.CasinoPlugin

object TaxUtil {


    fun applyTax(plugin: CasinoPlugin, winnings: Double, gameKey: String): Pair<Double, Double> {
        if (!plugin.config.getBoolean("taxes.enabled", false)) return Pair(winnings, 0.0)
        val rate = plugin.config.getDouble("taxes.$gameKey",
            plugin.config.getDouble("taxes.default-rate", 0.05))
        if (rate <= 0.0) return Pair(winnings, 0.0)
        val tax = winnings * rate
        val net = winnings - tax
        return Pair(net, tax)
    }


    fun taxMessage(plugin: CasinoPlugin, tax: Double): String {
        if (tax <= 0.0) return ""
        val format = plugin.messagesConfig.getString("tax.format", " <gray>(Impuesto: <red>-$${tax}</red>)</gray>")!!
        return format.replace("{tax}", String.format("%.0f", tax))
    }
}
