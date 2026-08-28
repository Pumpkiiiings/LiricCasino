package liric.casino.games.tictactoe

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractMatchmakingCommand
import org.bukkit.entity.Player

class TTTCommand(plugin: CasinoPlugin) : AbstractMatchmakingCommand(plugin, "ttt", "TIC TAC TOE") {

    override fun onCreate(player: Player, amount: Double) {
        plugin.tttManager.createGame(player, amount)
    }

    override fun onJoin(player: Player, targetName: String) {
        plugin.tttManager.joinGame(player, targetName)
    }

    override fun onCancel(player: Player) {
        plugin.tttManager.cancelGame(player)
    }

    override fun getOpenGameCreators(): List<String> {
        return plugin.tttManager.getOpenGames().map { it.creatorName }
    }

    override fun getOpenGameLines(): List<String> {
        val format = plugin.messagesConfig.getString("ttt-extra.list-format", "  <white>{creator} <dark_gray>• <#FFD700>${amount}")!!
        return plugin.tttManager.getOpenGames().map { s ->
            format.replace("{creator}", s.creatorName).replace("{amount}", s.betAmount.toLong().toString())
        }
    }
}
