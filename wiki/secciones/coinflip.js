window.WikiSections = window.WikiSections || {};

window.WikiSections['coinflip'] = {
    es: `
        <h2>CoinFlip (Cara o Cruz)</h2>
        <div class="about-content">
            <p>Un sistema de duelo (PvP) directo donde dos jugadores apuestan la misma cantidad de dinero. El ganador se lleva todo (menos un impuesto si lo configuras).</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/coinflip menu - Abre el buscador de duelos.
/coinflip crear &lt;monto&gt; - Crea un reto de moneda.
/coinflip unirse &lt;jugador&gt; - Acepta el reto de alguien.
/coinflip cancelar - Cancela tu reto activo.</code></pre>

            <h3>Cómo Jugar</h3>
            <p>Un jugador crea una apuesta. El reto aparece en el menú global o se transmite por chat. Otro jugador acepta el reto y el sistema lanza una moneda (Cara o Cruz). El que gana se lleva el bote.</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>Gestionado por <code>coinflip.bet</code> y <code>coinflip.uses</code> en la configuración.</p>
            <ul>
                <li><strong>Límite de Duelos:</strong> Define el límite diario de creaciones de duelos mediante rangos para fomentar que VIPs jueguen más (<code>coinflip.uses.ranks.vip</code>).</li>
                <li><strong>Máxima Apuesta:</strong> Previene la creación de retos que excedan el límite permitido para el rango del usuario.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>CoinFlip</h2>
        <div class="about-content">
            <p>A direct PvP duel system where two players bet the exact same amount. The winner takes all (minus a configured tax).</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/coinflip menu - Opens the duel browser.
/coinflip crear &lt;amount&gt; - Creates a coinflip challenge.
/coinflip unirse &lt;player&gt; - Accepts a challenge.
/coinflip cancelar - Cancels your active challenge.</code></pre>

            <h3>How to Play</h3>
            <p>A player creates a bet. The challenge appears in the global menu or chat. Another player accepts it, and the system flips a coin (Heads or Tails). The winner takes the pot.</p>

            <h3>Limits and Betting Permissions</h3>
            <p>Managed by <code>coinflip.bet</code> and <code>coinflip.uses</code> in the config.</p>
            <ul>
                <li><strong>Duel Limit:</strong> Define the daily limit of duel creations via ranks to encourage VIPs to play more (<code>coinflip.uses.ranks.vip</code>).</li>
                <li><strong>Max Bet:</strong> Prevents the creation of challenges that exceed the allowed limit for the user's rank.</li>
            </ul>
        </div>
    `
};
