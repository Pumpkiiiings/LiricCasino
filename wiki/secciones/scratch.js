window.WikiSections = window.WikiSections || {};

window.WikiSections['scratch'] = {
    es: `
        <h2>Boletos Raspadita (Scratch)</h2>
        <div class="about-content">
            <p>Boletos que los jugadores pueden raspar en un menú interactivo para descubrir premios ocultos. Soportan múltiples categorías (Tiers) con diferentes premios.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/boleto comprar [tier] [cantidad] - Compra raspaditas.
/boleto get [tier] [cantidad] - Obtiene raspaditas gratis (Admin).
/boleto give &lt;jugador&gt; [tier] [cantidad] - Da raspaditas (Admin).</code></pre>

            <h3>Cómo Jugar</h3>
            <p>El jugador recibe un ítem físico (boleto). Al hacer clic derecho sobre él, se abre un menú lleno de casillas ocultas. El jugador debe hacer clic para "raspar" y si encuentra 3 figuras iguales, ¡Gana ese premio!</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>Configurado en <code>scratch.uses</code> dentro de <code>config.yml</code>.</p>
            <ul>
                <li><strong>Límite Diario:</strong> Define cuántos boletos puede raspar o comprar un jugador al día, útil para evitar abuso si los boletos son muy baratos. Configura permisos como <code>casino.limit.scratch.vip</code> en la sección de <code>ranks</code>.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Scratch Cards</h2>
        <div class="about-content">
            <p>Tickets that players can scratch in an interactive menu to reveal hidden prizes. Supports multiple tiers with different rewards.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/scratch comprar [tier] [amount] - Buy scratch cards.
/scratch get [tier] [amount] - Get free scratch cards (Admin).
/scratch give &lt;player&gt; [tier] [amount] - Give scratch cards (Admin).</code></pre>

            <h3>How to Play</h3>
            <p>The player receives a physical item (ticket). Right-clicking it opens a menu full of hidden slots. The player must click to "scratch", and finding 3 identical symbols grants that prize!</p>

            <h3>Limits and Betting Permissions</h3>
            <p>Configured in <code>scratch.uses</code> inside <code>config.yml</code>.</p>
            <ul>
                <li><strong>Daily Limit:</strong> Defines how many tickets a player can scratch or buy per day, useful for preventing abuse if tickets are cheap. Configure permissions like <code>casino.limit.scratch.vip</code> under the <code>ranks</code> section.</li>
            </ul>
        </div>
    `
};
