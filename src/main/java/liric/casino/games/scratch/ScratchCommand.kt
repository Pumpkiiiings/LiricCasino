package liric.casino.games.scratch

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ScratchCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(msg("scratch.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "comprar" -> handleComprar(sender, args)
            "get"     -> handleGet(sender, args)
            "give"    -> handleGive(sender, args)
            else      -> sender.sendMessage(msg("scratch.usage"))
        }
        return true
    }

    private fun parseTierAndAmount(args: Array<out String>, startIndex: Int): Pair<TicketTier?, Int> {
        var tierStr = "basico"
        var amount = 1
        if (args.size == startIndex + 1) {
            val possibleAmount = args[startIndex].toIntOrNull()
            if (possibleAmount != null) amount = possibleAmount else tierStr = args[startIndex]
        } else if (args.size >= startIndex + 2) {
            tierStr = args[startIndex]
            amount = args[startIndex + 1].toIntOrNull() ?: 1
        }
        val tier = TicketTier.values().find { it.id.equals(tierStr, ignoreCase = true) }
        return Pair(tier, amount)
    }

    private fun handleComprar(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        val (tier, amount) = parseTierAndAmount(args, 1)
        if (tier == null) { sender.sendMessage(msg("scratch.invalid-tier")); return }
        if (amount <= 0) { sender.sendMessage(msg("scratch.invalid-amount")); return }

        val totalCost = tier.price * amount
        if (plugin.economyManager.withdrawPlayer(sender, totalCost).transactionSuccess()) {
            val ticket = ScratchTicket.create(plugin, tier, amount)
            sender.inventory.addItem(ticket)
            sender.sendMessage(msg("scratch.bought", "amount" to amount.toString(), "tier" to tier.displayName, "cost" to totalCost.toString()))
            sender.playSound(sender.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        } else {
            sender.sendMessage(msg("scratch.no-funds", "cost" to totalCost.toString()))
            sender.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }
    }

    private fun handleGet(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("casino.admin")) { sender.sendMessage(msg("general.no-permission")); return }
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        val (tier, amount) = parseTierAndAmount(args, 1)
        if (tier == null) { sender.sendMessage(msg("scratch.invalid-tier")); return }
        if (amount <= 0) { sender.sendMessage(msg("scratch.invalid-amount")); return }

        val ticket = ScratchTicket.create(plugin, tier, amount)
        sender.inventory.addItem(ticket)
        sender.sendMessage(msg("scratch.got-free", "amount" to amount.toString(), "tier" to tier.displayName))
        sender.playSound(sender.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("casino.admin")) { sender.sendMessage(msg("general.no-permission")); return }
        if (args.size < 2) { sender.sendMessage(msg("scratch.usage")); return }

        val target = Bukkit.getPlayer(args[1])
        if (target == null) { sender.sendMessage(msg("scratch.player-offline")); return }

        val (tier, amount) = parseTierAndAmount(args, 2)
        if (tier == null) { sender.sendMessage(msg("scratch.invalid-tier")); return }
        if (amount <= 0) { sender.sendMessage(msg("scratch.invalid-amount")); return }

        val ticket = ScratchTicket.create(plugin, tier, amount)
        target.inventory.addItem(ticket)
        sender.sendMessage(msg("scratch.gave-sender", "amount" to amount.toString(), "tier" to tier.displayName, "player" to target.name))
        target.sendMessage(msg("scratch.gave-target", "amount" to amount.toString(), "tier" to tier.displayName))
        target.playSound(target.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        val isAdmin = sender.hasPermission("casino.admin")
        if (args.size == 1) {
            val options = mutableListOf("comprar")
            if (isAdmin) { options.add("get"); options.add("give") }
            return options.filter { it.startsWith(args[0], true) }
        }
        val action = args[0].lowercase()
        if (action == "comprar" || (action == "get" && isAdmin)) {
            if (args.size == 2) return TicketTier.values().map { it.id }.plus(listOf("1","5","10")).filter { it.startsWith(args[1], true) }
            if (args.size == 3) return listOf("1","5","10","64").filter { it.startsWith(args[2], true) }
        }
        if (action == "give" && isAdmin) {
            if (args.size == 2) return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], true) }
            if (args.size == 3) return TicketTier.values().map { it.id }.plus(listOf("1","5","10")).filter { it.startsWith(args[2], true) }
            if (args.size == 4) return listOf("1","5","10","64").filter { it.startsWith(args[3], true) }
        }
        return emptyList()
    }
}
