package liric.casino.webhook

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale


class WebhookManager(private val plugin: CasinoPlugin) {

    private val configFile = File(plugin.dataFolder, "webhooks.yml")
    private var config: FileConfiguration

    init {
        if (!configFile.exists()) {
            plugin.saveResource("webhooks.yml", false)
        }
        config = YamlConfiguration.loadConfiguration(configFile)
    }

    private fun cfg() = config
    private fun enabled() = cfg().getBoolean("webhooks.enabled", false)
    private fun fmt(n: Double) = "$" + NumberFormat.getNumberInstance(Locale.US).format(n)


    fun sendJackpot(game: String, playerName: String, amount: Double) {
        if (!enabled()) return
        val url = cfg().getString("webhooks.jackpot.url") ?: return
        if (url.isBlank() || url == "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN") return

        val color   = cfg().getInt("webhooks.jackpot.color", 0xFF00FF)
        val title   = cfg().getString("webhooks.jackpot.title", "🎰 ¡JACKPOT!") ?: "🎰 ¡JACKPOT!"
        val desc    = (cfg().getString("webhooks.jackpot.description")
            ?: "**{player}** ganó el JACKPOT en **{game}** y se llevó **{amount}**!")
            .replace("{player}", playerName)
            .replace("{game}", game)
            .replace("{amount}", fmt(amount))
        val footer  = cfg().getString("webhooks.jackpot.footer", "Liric Casino") ?: "Liric Casino"
        val mention = cfg().getString("webhooks.jackpot.mention-role", "") ?: ""

        val content = if (mention.isNotBlank()) "$mention" else ""

        sendEmbed(url, content, title, desc, color, footer)
    }


    fun sendBigWin(game: String, playerName: String, amount: Double) {
        if (!enabled()) return
        val url = cfg().getString("webhooks.big-win.url") ?: return
        if (url.isBlank() || url == "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN") return

        val threshold = cfg().getDouble("webhooks.big-win.min-amount", 50000.0)
        if (amount < threshold) return

        val color   = cfg().getInt("webhooks.big-win.color", 0xFFD700)
        val title   = cfg().getString("webhooks.big-win.title", "💰 ¡Gran Victoria!") ?: "💰 ¡Gran Victoria!"
        val desc    = (cfg().getString("webhooks.big-win.description")
            ?: "**{player}** ganó **{amount}** en **{game}**!")
            .replace("{player}", playerName)
            .replace("{game}", game)
            .replace("{amount}", fmt(amount))
        val footer  = cfg().getString("webhooks.big-win.footer", "Liric Casino") ?: "Liric Casino"
        val mention = cfg().getString("webhooks.big-win.mention-role", "") ?: ""

        val content = if (mention.isNotBlank()) "$mention" else ""

        sendEmbed(url, content, title, desc, color, footer)
    }


    fun sendLotteryWinner(playerName: String, amount: Double, number: Int) {
        if (!enabled()) return
        val url = cfg().getString("webhooks.lottery.url") ?: return
        if (url.isBlank() || url == "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN") return

        val color   = cfg().getInt("webhooks.lottery.color", 0xFFD700)
        val title   = cfg().getString("webhooks.lottery.title", "🎟 ¡Ganador de Lotería!") ?: "🎟 ¡Ganador de Lotería!"
        val desc    = (cfg().getString("webhooks.lottery.description")
            ?: "**{player}** ganó la Lotería con el número **{number}** y se llevó **{amount}**!")
            .replace("{player}", playerName)
            .replace("{number}", number.toString())
            .replace("{amount}", fmt(amount))
        val footer  = cfg().getString("webhooks.lottery.footer", "Liric Casino") ?: "Liric Casino"
        val mention = cfg().getString("webhooks.lottery.mention-role", "") ?: ""

        val content = if (mention.isNotBlank()) "$mention" else ""

        sendEmbed(url, content, title, desc, color, footer)
    }


    private fun sendEmbed(
        webhookUrl: String,
        content: String,
        title: String,
        description: String,
        color: Int,
        footer: String
    ) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val json = buildJson(content, title, description, color, footer)
                val conn = URL(webhookUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("User-Agent", "CasinoLiric-Plugin")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout    = 5000

                conn.outputStream.use { os: OutputStream -> os.write(json.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                if (code !in 200..299) {
                    plugin.logger.warning("[Webhook] Código HTTP inesperado: $code para $webhookUrl")
                }
                conn.disconnect()
            } catch (e: Exception) {
                plugin.logger.warning("[Webhook] Error enviando embed: ${e.message}")
            }
        })
    }

    private fun buildJson(content: String, title: String, description: String, color: Int, footer: String): String {
        val escapedTitle       = title.replace("\"", "\\\"").replace("\n", "\\n")
        val escapedDescription = description.replace("\"", "\\\"").replace("\n", "\\n")
        val escapedFooter      = footer.replace("\"", "\\\"")
        val escapedContent     = content.replace("\"", "\\\"")

        return """{"content":"$escapedContent","embeds":[{"title":"$escapedTitle","description":"$escapedDescription","color":$color,"footer":{"text":"$escapedFooter"}}]}"""
    }
}
