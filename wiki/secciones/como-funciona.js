window.WikiSections = window.WikiSections || {};

window.WikiSections['como-funciona'] = {
    es: `
        <h2>Cómo Funciona</h2>
        <div class="about-content">
            <p>CasinoLiric utiliza un sistema modular para gestionar múltiples juegos de manera eficiente sin afectar el rendimiento de tu servidor.</p>
            
            <h3>El Sistema de Menús</h3>
            <p>Cada juego interactúa con el usuario a través de interfaces (Inventarios GUI). Estos menús son completamente configurables desde los archivos <code>.yml</code> ubicados en la carpeta <code>menus/</code>.</p>

            <h3>Gestión de Apuestas</h3>
            <p>Las apuestas se procesan de forma segura utilizando <strong>Vault</strong>. Antes de iniciar cualquier partida, el plugin verifica el balance del jugador y retiene el dinero temporalmente.</p>
            
            <pre><code>/casino - Abre el menú principal
/casino reload - Recarga la configuración (Solo Admins)</code></pre>
        </div>
    `,
    en: `
        <h2>How it Works</h2>
        <div class="about-content">
            <p>CasinoLiric uses a modular system to efficiently manage multiple games without affecting your server's performance.</p>
            
            <h3>The Menu System</h3>
            <p>Each game interacts with the user through interfaces (GUI Inventories). These menus are fully configurable from the <code>.yml</code> files located in the <code>menus/</code> folder.</p>

            <h3>Bet Management</h3>
            <p>Bets are processed securely using <strong>Vault</strong>. Before starting any game, the plugin checks the player's balance and temporarily holds the money.</p>
            
            <pre><code>/casino - Opens the main menu
/casino reload - Reloads configuration (Admins only)</code></pre>
        </div>
    `
};
