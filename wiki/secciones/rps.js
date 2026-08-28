window.WikiSections = window.WikiSections || {};

window.WikiSections['rps'] = {
    es: `
        <h2>Piedra, Papel o Tijera (RPS)</h2>
        <div class="about-content">
            <p>Otro juego PvP. Similar a CoinFlip, pero ambos jugadores deben seleccionar secretamente su opción en un menú interactivo.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/rps crear &lt;monto&gt; - Crea un reto por dinero.
/rps unirse &lt;jugador&gt; - Únete a un reto existente.
/rps cancelar - Cancela tu reto.</code></pre>

            <h3>Cómo Jugar</h3>
            <p>Tras aceptar el reto, ambos jugadores ven un menú donde deben seleccionar: Piedra, Papel o Tijera. Si empatan, se les devuelve el dinero. Si hay un ganador, se lleva el bote. ¡El plugin maneja las desconexiones para que nadie pierda dinero injustamente!</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>Gestionado por <code>rps.bet</code> y <code>rps.uses</code>.</p>
            <ul>
                <li><strong>Apuesta Máxima:</strong> Como es un juego 100% de jugador contra jugador, puedes configurar los rangos VIP para que tengan permisos especiales y puedan apostar fortunas.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Rock, Paper, Scissors (RPS)</h2>
        <div class="about-content">
            <p>Another PvP game. Similar to CoinFlip, but both players must secretly select their option in an interactive menu.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/rps crear &lt;amount&gt; - Create a money challenge.
/rps unirse &lt;player&gt; - Join an existing challenge.
/rps cancelar - Cancel your challenge.</code></pre>

            <h3>How to Play</h3>
            <p>After accepting the challenge, both players see a menu where they must select: Rock, Paper, or Scissors. If they tie, money is refunded. If there's a winner, they take the pot. The plugin handles disconnects so no one loses money unfairly!</p>

            <h3>Limits and Betting Permissions</h3>
            <p>Managed by <code>rps.bet</code> and <code>rps.uses</code>.</p>
            <ul>
                <li><strong>Max Bet:</strong> Since it's a 100% player vs player game, you can configure VIP ranks to have special permissions to bet fortunes.</li>
            </ul>
        </div>
    `
};
