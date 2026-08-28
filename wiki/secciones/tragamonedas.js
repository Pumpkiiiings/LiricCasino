window.WikiSections = window.WikiSections || {};

window.WikiSections['tragamonedas'] = {
    es: `
        <h2>Tragamonedas (Slots 777)</h2>
        <div class="about-content">
            <p>Una vibrante máquina tragamonedas (Slots) con múltiples líneas de pago y emocionantes animaciones de rodillos.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/tragamonedas setup - Coloca decoraciones y el holograma de la máquina frente a ti.
/tragamonedas delete - Elimina el holograma o máquina que miras.
/tragamonedas purge - Limpia todas las instancias activas.</code></pre>

            <h3>Cómo Jugar</h3>
            <p>El jugador interactúa con la máquina (usualmente interactuando con el holograma). Luego selecciona su apuesta en el menú y tira de la palanca. Los rodillos girarán y se pagará según las líneas ganadoras configuradas.</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>Controla las apuestas en <code>config.yml</code> (<code>slots.bet</code> y <code>slots.uses</code>).</p>
            <ul>
                <li><strong>Límite de Apuesta:</strong> Se define un límite máximo (ej: <code>slots.bet.ranks.vip</code>) por el cual el jugador no puede apostar más de ese monto por tiro.</li>
                <li><strong>Usos Diarios:</strong> Si se define <code>slots.uses.ranks.default.max-uses-per-day: 50</code>, el jugador solo podrá tirar 50 veces por día.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Slots (777)</h2>
        <div class="about-content">
            <p>A vibrant slot machine with multiple paylines and exciting reel animations.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/slots setup - Places decorations and a machine hologram in front of you.
/slots delete - Removes the targeted hologram or machine.
/slots purge - Clears all active instances.</code></pre>

            <h3>How to Play</h3>
            <p>The player interacts with the machine (usually by interacting with the hologram). Then, they select their bet in the menu and pull the lever. The reels will spin, and payouts are made based on configured winning lines.</p>

            <h3>Limits and Betting Permissions</h3>
            <p>Control bets in <code>config.yml</code> (<code>slots.bet</code> and <code>slots.uses</code>).</p>
            <ul>
                <li><strong>Bet Limits:</strong> A maximum limit is set (e.g., <code>slots.bet.ranks.vip</code>) beyond which the player cannot bet per spin.</li>
                <li><strong>Daily Uses:</strong> If <code>slots.uses.ranks.default.max-uses-per-day: 50</code> is defined, the player can only spin 50 times a day.</li>
            </ul>
        </div>
    `
};
