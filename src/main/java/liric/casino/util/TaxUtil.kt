package liric.casino.util

import liric.casino.CasinoPlugin

object TaxUtil {

    /**
     * Aplica el impuesto configurado sobre unas ganancias.
     * @param plugin instancia del plugin para leer config
     * @param winnings monto bruto ganado
     * @param gameKey clave del juego en config.yml (roulette, slots, blackjack, scratch, lottery, coinflip)
     * @return Par (netoRecibido, impuestoCobrado)
     */
    fun applyTax(plugin: CasinoPlugin, winnings: Double, gameKey: String): Pair<Double, Double> {
        if (!plugin.config.getBoolean("taxes.enabled", false)) return Pair(winnings, 0.0)
        val rate = plugin.config.getDouble("taxes.$gameKey",
            plugin.config.getDouble("taxes.default-rate", 0.05))
        if (rate <= 0.0) return Pair(winnings, 0.0)
        val tax = winnings * rate
        val net = winnings - tax
        return Pair(net, tax)
    }

    /**
     * Formatea el mensaje de impuesto para mostrar al jugador.
     */
    fun taxMessage(plugin: CasinoPlugin, tax: Double): String {
        if (tax <= 0.0) return ""
        return " <gray>(Impuesto: <red>-$${String.format("%.0f", tax)}</red>)</gray>"
    }
}
