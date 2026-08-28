package liric.casino.games.rps

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractMatchmakingCommand
import org.bukkit.entity.Player

class RPSCommand(plugin: CasinoPlugin) : AbstractMatchmakingCommand(plugin, "rps", "ROCK PAPER SCISSORS") {

    override fun onCreate(player: Player, amount: Double) {
        plugin.rpsManager.createGame(player, amount)
    }

    override fun onJoin(player: Player, targetName: String) {
        plugin.rpsManager.joinGame(player, targetName)
    }

    override fun onCancel(player: Player) {
        plugin.rpsManager.cancelGame(player)
    }

    override fun getOpenGameCreators(): List<String> {
        return plugin.rpsManager.getOpenGames().map { it.creatorName }
    }

    override fun getOpenGameLines(): List<String> {
        val format = plugin.messagesConfig.getString("rps-extra.list-format", "  <white>{creator} <dark_gray>• <#FFD700>${'$'}{amount}")!!
        return plugin.rpsManager.getOpenGames().map { s ->
            format.replace("{creator}", s.creatorName).replace("{amount}", s.betAmount.toLong().toString())
        }
    }
}
