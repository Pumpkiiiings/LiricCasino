# CasinoLiric 2.0.0 🎰

A comprehensive, modern, and highly customizable casino plugin for Minecraft servers (Paper/Spigot). Designed to provide multiple minigames with betting functionality integrated directly into your server's economy.

![Version](https://img.shields.io/badge/Version-2.0.0-gold.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blueviolet.svg)
![API](https://img.shields.io/badge/API-1.20+-lightgrey.svg)

---

## ✨ Key Features

- **10 Unique Minigames**: A complete suite of classic casino games and player-versus-player (PvP & PvE) challenges.
- **Highly Customizable GUIs**: All menus (size, buttons, slots, decorations, materials, and text) load dynamically from `.yml` files in the `menus/` directory.
- **Vault Economy Integration**: Full support for Vault to manage bets and payouts securely.
- **House Edge System**: Configurable tax percentage on winnings to help balance your server's economy.
- **Database Support**: SQLite (`casino.db`) storage for player statistics, including wins, losses, and total amounts wagered.
- **Leaderboards**: Displays top players based on earnings or total bets for each minigame.
- **PlaceholderAPI Integration**: Custom placeholders available for scoreboards, chat, and menus (e.g., `%casinoliric_ruleta_wins%`).
- **Discord Webhooks**: Announce high-value wins automatically to your Discord server.
- **In-Game Visuals**: Smooth animations for games like roulette and slot machines without requiring custom texture packs.

---

## 🎮 Included Minigames

1. **🎡 Roulette**: A visual roulette game built directly into the world. Supports multiple simultaneous bets (Red/Black, Even/Odd, or specific numbers).
2. **🃏 Blackjack (21)**: The classic card game where players try to get closer to 21 than the dealer without going over.
3. **🎰 Slots**: Slot machines featuring custom item-spinning animations in an inventory menu.
4. **♠️ Poker**: A standard Texas Hold'em style table for player-versus-player competition.
5. **🎫 Scratch Cards**: An interactive menu where players scratch off slots to reveal hidden rewards.
6. **🎟️ Lottery**: A server-wide drawing system. Players purchase tickets, and winners are selected automatically on a timer.
7. **🪙 CoinFlip (PvP)**: Two players wager money on a coin toss with real-time menu animations.
8. **✂️ Rock-Paper-Scissors (PvP)**: Quick wagers where players challenge others to a classic hand game.
9. **❌ Tic-Tac-Toe (PvP)**: A competitive 3x3 grid game where the winner takes the pot.
10. **🏇 Horse Racing (PvE)**: A virtual horse race simulation. Players bet on horses with different odds and payout multipliers.

---

## 🛠️ Commands

All commands are accessible via the main `/casino` command, alongside direct aliases.

### 👤 Player Commands
- `/casino <game> play` - Opens the main menu for the selected game.
- `/casino stats [game]` - Displays personal statistics.
- `/casino top [game]` - Displays the leaderboards.
- **Direct Aliases**: `/roulette`, `/blackjack`, `/poker`, `/slots`, `/scratch`, `/lottery`, `/coinflip`, `/rps`, `/ttt`, `/racing`.

### 🛡️ Admin Commands (Permission: `casinoliric.admin`)
- `/casino <game> setup` - Sets the current location or clicked NPC as the interaction point for the game.
- `/casino <game> delete` - Removes the configured location for a game.
- `/casino roulette scale <size> [radius]` - Adjusts the physical size of the roulette wheel structure in real time.
- `/casino reload` - Recargas `config.yml`, `messages.yml`, and all menu configurations.

---

## 📁 Configuration Structure

Upon the first startup, the plugin generates the following directory structure:

```text
plugins/CasinoLiric/
├── config.yml        # Economy settings, tax rates, webhooks, and database options.
├── messages.yml      # Custom messages, prefixes, and translations.
├── data.yml          # Stores configured locations for each minigame.
├── casino.db         # Auto-generated SQLite database file.
└── menus/            # GUI configuration files
    ├── coinflip.yml
    ├── rps.yml
    ├── ttt.yml
    ├── racing.yml
    ├── lottery.yml
    └── ...
```

---

## 🎨 Dynamic Menus

The user interfaces can be modified by editing the files within the `menus/` folder. Configurable options include:
- `rows`: The inventory size (1 to 6 rows).
- `title`: Menu title supporting MiniMessage format (`<rainbow>`, `<bold>`, hex colors).
- `decorations`: Background items, glass panes, or fillers assigned to specific slot ranges.
- `items`: Custom button configurations, including display names, lore, and materials.

---

## 💻 Requirements

- **Server Software**: Paper, Purpur, or Spigot.
- **Version Compatibility**: 1.20.x - 1.21.x.
- **Dependencies**:
    - Vault
    - A Vault-compatible economy plugin (e.g., EssentialsX, CMI).

## 🚀 Installation

1. Download the `CasinoLiric-x.x.x.jar` file.
2. Place the file into your server's `/plugins/` directory.
3. Ensure `Vault` and an economy plugin are installed and running.
4. Restart your server.
5. Use the admin setup commands to establish in-game access points and configure the files in `plugins/CasinoLiric/` to your preference.