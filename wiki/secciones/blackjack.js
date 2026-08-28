window.WikiSections = window.WikiSections || {};

window.WikiSections['blackjack'] = {
    es: `
        <h2>Blackjack (21)</h2>
        <div class="about-content">
            <p>El clásico juego de casino de cartas donde el objetivo es sumar 21 o acercarse más que el crupier sin pasarse.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/blackjack jugar - Entra a una partida.
/blackjack setup - Coloca decoraciones y un holograma de Blackjack donde estés mirando.
/blackjack delete - Elimina el holograma o mesa que mires.</code></pre>

            <h3>Cómo Jugar</h3>
            <p>Al iniciar, el jugador recibe 2 cartas y el crupier 2 (una boca abajo). Puedes pedir carta (<em>Hit</em>) o plantarte (<em>Stand</em>). Si superas 21, pierdes automáticamente.</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>CasinoLiric incluye un poderoso sistema dinámico para controlar cuánto juegan los usuarios, configurable en <code>config.yml</code> (en la sección <code>blackjack.bet</code> y <code>blackjack.uses</code>).</p>
            <ul>
                <li><strong>Límite de Apuesta:</strong> Puedes establecer montos máximos por rango. Ejemplo: <code>blackjack.bet.ranks.vip</code>. Al jugador se le validará que posea el permiso definido para tener el límite incrementado.</li>
                <li><strong>Máximos Usos por Día:</strong> Limita cuántas partidas diarias puede jugar un usuario mediante permisos, útil para balancear tu economía.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Blackjack (21)</h2>
        <div class="about-content">
            <p>The classic casino card game where the goal is to get 21 or closer than the dealer without going over.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/blackjack jugar - Joins a game.
/blackjack setup - Places decorations and a Blackjack hologram where you are looking.
/blackjack delete - Removes the targeted hologram or table.</code></pre>

            <h3>How to Play</h3>
            <p>Upon starting, the player receives 2 cards and the dealer 2 (one face down). You can request a card (<em>Hit</em>) or hold (<em>Stand</em>). If you go over 21, you lose automatically.</p>

            <h3>Limits and Betting Permissions</h3>
            <p>CasinoLiric includes a powerful dynamic system to control how much users play, configurable in <code>config.yml</code> (under the <code>blackjack.bet</code> and <code>blackjack.uses</code> section).</p>
            <ul>
                <li><strong>Bet Limits:</strong> You can set maximum amounts per rank. Example: <code>blackjack.bet.ranks.vip</code>. The player must have the specified permission to receive the increased limit.</li>
                <li><strong>Max Uses Per Day:</strong> Limits how many daily games a user can play via permissions, useful for balancing your economy.</li>
            </ul>
        </div>
    `
};
