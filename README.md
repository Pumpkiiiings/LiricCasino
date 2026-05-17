# CasinoLiric 2.0.0 🎰

Un plugin de casino integral, moderno y altamente personalizable para servidores de Minecraft (Paper/Spigot). Diseñado para proporcionar múltiples minijuegos con apuestas usando el sistema de economía de tu servidor. 

![Version](https://img.shields.io/badge/Versión-2.0.0-gold.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blueviolet.svg)
![API](https://img.shields.io/badge/API-1.20+-lightgrey.svg)

## ✨ Características Principales

- **10 Minijuegos Únicos**: Una suite completa de juegos clásicos de casino y apuestas entre jugadores (PvP y PvE).
- **GUIs 100% Personalizables**: Todos los menús (tamaño, botones, slots, decoraciones, materiales, texto) se cargan dinámicamente desde archivos `.yml` en la carpeta `menus/`. Utiliza el potente framework `Triumph-GUI`.
- **Economía Integrada**: Soporte total para **Vault**. Todas las apuestas y premios se gestionan de forma segura.
- **Sistema de Impuestos (House Edge)**: Configura un porcentaje de retención global (ej. 5%) en cada ganancia, que el casino se quedará para mantener la economía balanceada.
- **Base de Datos Robusta**: Almacenamiento estadístico de victorias, derrotas y cantidades apostadas mediante **SQLite** (`casino.db`).
- **Tablas de Clasificación (Top)**: Descubre a los jugadores más ricos o más apostadores por cada minijuego.
- **PlaceholderAPI**: Múltiples variables para usar en tus scoreboards y menús (`%casinoliric_ruleta_wins%`, etc.).
- **Anuncios por Webhooks**: Conexión a Discord para anunciar automáticamente cuando un jugador gana un premio masivo (Grandes Victorias).
- **Entidades Animadas (BlockDisplay)**: Innovador sistema visual para la ruleta, utilizando físicas de bloques sin necesidad de texturas externas.

---

## 🎮 Minijuegos Incluidos

1. **🎡 Ruleta (Roulette)**: Una auténtica ruleta visual generada con *BlockDisplays*. Soporta múltiples tipos de apuestas simultáneas (Rojo/Negro, Par/Impar, Números específicos).
2. **🃏 Blackjack (21)**: El clásico juego de cartas donde debes acercarte a 21 sin pasarte.
3. **🎰 Tragamonedas (Slots 777)**: Máquinas con animaciones de giro de ítems.
4. **♠️ Poker**: Mesa clásica para competir en estilo Texas Hold'em.
5. **🎫 Rasca y Gana (Boleto/Scratch)**: Interfaz animada donde "rascas" casillas virtuales para encontrar premios escondidos.
6. **🎟️ Lotería**: Sistema global con pozo acumulable. Los jugadores compran tickets y los sorteos se realizan automáticamente por programación (ej. cada hora o día).
7. **🪙 CoinFlip (Cara o Cruz PvP)**: Los jugadores crean un lobby y se enfrentan al azar contra otro oponente en un espectacular menú animado.
8. **✂️ Piedra, Papel o Tijera (PvP)**: Elige tu jugada, reta a un oponente por una suma de dinero y mira quién gana.
9. **❌ Tic Tac Toe (PvP)**: Juego del Gato / Tres en raya con un tablero 3x3 interactivo. El ganador se lleva el pozo de las apuestas.
10. **🏇 Carreras de Caballos (PvE)**: Simulación de apuestas. Cada caballo tiene una probabilidad única de victoria y un multiplicador específico. ¡Apuesta al caballo correcto y observa la cuenta regresiva!

---

## 🛠️ Comandos

Todos los comandos están centralizados en `/casino`, pero disponen de sus alias directos.

### 👤 Comandos de Jugador
- `/casino <juego> jugar` - Abre la interfaz principal del juego seleccionado.
- `/casino stats [juego]` - Muestra tus estadísticas globales o por juego.
- `/casino top [juego]` - Despliega el ranking de los mejores jugadores.
- **Alias directos**: `/ruleta`, `/blackjack`, `/poker`, `/tragamonedas`, `/boleto`, `/loteria`, `/coinflip`, `/rps`, `/ttt`, `/carreras`.

### 🛡️ Comandos de Administrador (Permiso: `casinoliric.admin`)
- `/casino <juego> setup` - Establece la ubicación actual (o el NPC clickeado) como el punto de inicio para ese juego.
- `/casino <juego> delete` - Elimina la ubicación configurada.
- `/casino ruleta escala <tamaño> [radio]` - Permite ajustar el tamaño visual de la estructura BlockDisplay de la ruleta en tiempo real.
- `/casino reload` - Recarga `config.yml`, `messages.yml` y todas las configuraciones visuales dentro de `menus/`.

---

## 📁 Estructura de Configuración

Al iniciar el plugin por primera vez en la versión 2.0.0, se generará la carpeta `plugins/CasinoLiric/` con la siguiente estructura:

```text
plugins/CasinoLiric/
├── config.yml        # Economía, impuestos, webhooks, bases de datos.
├── messages.yml      # Todos los textos, prefijos y mensajes del plugin.
├── data.yml          # Almacena las ubicaciones de los setup de cada minijuego.
├── casino.db         # (Generado automáticamente) Base de datos SQLite.
└── menus/            # Configuraciones completas de Triumph-GUI
    ├── coinflip.yml
    ├── rps.yml
    ├── ttt.yml
    ├── racing.yml
    ├── lottery.yml
    └── ...
```

---

## 🎨 Menús Dinámicos (Triumph-GUI)
Absolutamente todas las interfaces gráficas son editables. Podrás configurar:
- `rows`: Cantidad de filas del inventario (1 a 6).
- `title`: Título de la interfaz con soporte MiniMessage (`<rainbow>`, `<bold>`, hex colors).
- `decorations`: Colocar paneles de cristal u otros ítems decorativos definiendo los *slots*.
- `items`: Modificar los botones principales, sus nombres, descripciones y materiales.

---

## 💻 Requisitos
- Servidor de Minecraft ejecutando Paper, Purpur o Spigot (Se recomienda altamente **Paper** para un mejor rendimiento en la Ruleta de bloques).
- **Versión**: 1.20.x - 1.21.x.
- **Vault**: Requerido para la integración de economía.
- Plugin base de economía compatible con Vault (EssentialsX, CMI, etc).

## 🚀 Instalación
1. Descarga el archivo `CasinoLiric-2.0.0.jar`.
2. Colócalo en la carpeta `/plugins/` de tu servidor.
3. Asegúrate de tener instalado `Vault`.
4. Reinicia tu servidor.
5. Usa los comandos de administración para establecer las ubicaciones de juego y edita a tu gusto en `plugins/CasinoLiric/`.
