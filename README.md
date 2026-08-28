# CasinoLiric 🎰

The ultimate casino plugin for Minecraft servers (Paper/Spigot). Designed to offer highly customizable minigames with a betting system integrated directly into your server's economy.

Perfect for keeping your players engaged, spending money, and having fun!

---

## ✨ Key Features

*   **10 Included Minigames**: Physical Roulette, Blackjack, Slots, Poker, Scratch Cards, Lottery, CoinFlip (PvP), Rock Paper Scissors (PvP), Tic Tac Toe (PvP), and Horse Racing.
*   **Enable/Disable Games**: You can easily enable or disable any game at any time from the `config.yml` using the `active: true/false` option.
*   **Limits via Permissions**: Control max bets, taxes (House Edge), and daily usage limits per game using a custom permission system. Perfect for VIP ranks!
*   **Scalable Roulette**: The Roulette is a physical object in your world. You can adjust its size (make it giant or tiny) live in-game using `/roulette scale <size>`.
*   **100% Configurable Menus**: Change the size, titles, colors (MiniMessage/Hex support), items, and decorations of every game menu from the `menus/` folder.
*   **Discord Webhooks**: Configure Discord alerts in a dedicated file (`webhooks.yml`) to automatically announce when a player wins a big jackpot.
*   **Leaderboards & Stats**: Built-in leaderboards to see who has won or wagered the most in each game.
*   **PlaceholderAPI Support**: Ready-to-use variables for your Scoreboards, menus, or chat.

---

## 🛠️ Main Commands

You can use the main command `/casino` or direct shortcuts for each game.

### Player Commands
*   `/casino <game>` or use direct aliases: `/roulette`, `/blackjack`, `/slots`, `/poker`, `/scratch`, `/lottery`, `/cf`, `/rps`, `/ttt`, `/racing`.
*   `/casino stats` - View personal statistics.
*   `/casino top [game]` - View the top players.

### Admin Commands (Permission: `casinoliric.admin`)
*   `/casino <game> setup` - Spawns a physical game (like the Roulette or Slot Machine) right where you are standing.
*   `/casino <game> delete` - Removes the closest physical game to you.
*   `/roulette scale <0.1 to 2.0> [radius]` - Change the roulette size live.
*   `/casino reload` - Reloads the config, menus, and messages.

---

## 💻 Requirements & Installation

**Required Dependencies:**
1.  [Vault](https://www.spigotmc.org/resources/vault.34315/)
2.  A Vault-compatible economy plugin (e.g., EssentialsX, CMI, etc).

**Quick Installation:**
1.  Download the `CasinoLiric.jar` file.
2.  Drop it into your server's `/plugins/` folder.
3.  Make sure **Vault** and your economy plugin are installed and running.
4.  Start or restart your server (versions 1.20.x - 1.21.x).
5.  Done! Use `/casino <game> setup` to place the physical games in your world and configure everything to your liking inside the `plugins/CasinoLiric/` folder.