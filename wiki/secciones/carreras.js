window.WikiSections = window.WikiSections || {};

window.WikiSections['carreras'] = {
    es: `
        <h2>Carreras de Caballos (Racing)</h2>
        <div class="about-content">
            <p>Un sistema de apuestas virtual donde los jugadores apuestan por caballos con diferentes multiplicadores y probabilidades de ganar.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/carreras jugar - Abre el menú de apuestas de caballos.
/carreras ayuda - Muestra información de carreras.</code></pre>

            <h3>Cómo Jugar</h3>
            <p>En el menú verás a los diferentes caballos competidores, cada uno con un multiplicador (ej. 2x, 5x, 10x) basado en su probabilidad de ganar. Apuestas tu dinero a uno de ellos. Al terminar el tiempo, la carrera virtual simula el resultado y paga a los ganadores.</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>Gestionado por <code>racing.bet</code> y <code>racing.uses</code> en la configuración.</p>
            <ul>
                <li><strong>Apuesta Máxima:</strong> Evita que jugadores arriesguen todo su balance. Otorga un permiso VIP configurado en <code>config.yml</code> para permitir apuestas mayores en los caballos.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Horse Racing</h2>
        <div class="about-content">
            <p>A virtual betting system where players bet on horses with different multipliers and win probabilities.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/racing jugar - Opens the horse betting menu.
/racing ayuda - Shows racing information.</code></pre>

            <h3>How to Play</h3>
            <p>In the menu, you will see the competing horses, each with a multiplier (e.g., 2x, 5x, 10x) based on their win chance. You bet your money on one of them. When the timer ends, the virtual race simulates the outcome and pays out the winners.</p>

            <h3>Limits and Betting Permissions</h3>
            <p>Managed by <code>racing.bet</code> and <code>racing.uses</code> in the config.</p>
            <ul>
                <li><strong>Max Bet:</strong> Prevents players from risking their entire balance. Grant a VIP permission configured in <code>config.yml</code> to allow larger bets on the horses.</li>
            </ul>
        </div>
    `
};
