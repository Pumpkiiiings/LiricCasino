window.WikiSections = window.WikiSections || {};

window.WikiSections['tictactoe'] = {
    es: `
        <h2>Tic Tac Toe (Tres en Raya)</h2>
        <div class="about-content">
            <p>Juego PvP de estrategia pura. Los jugadores apuestan y compiten en una cuadrícula de 3x3 usando un menú de interfaz.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/ttt crear &lt;monto&gt; - Crea la sala.
/ttt unirse &lt;jugador&gt; - Únete a la partida.
/ttt cancelar - Abandona tu partida creada.</code></pre>

            <h3>Cómo Jugar</h3>
            <p>Al comenzar, se decide el turno. Los jugadores colocan alternativamente sus marcas (X u O, representadas por ítems). El primero en alinear tres marcas gana todo el dinero apostado.</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>Controlado mediante <code>tictactoe.bet</code> y <code>tictactoe.uses</code>.</p>
            <ul>
                <li><strong>Usos Diarios (Anti-Grind):</strong> Este juego puede tardar más. Configurar un límite diario (ej. 10 partidas/día) mediante permisos asegura que los usuarios prueben otros juegos del casino.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Tic Tac Toe</h2>
        <div class="about-content">
            <p>Pure strategy PvP game. Players bet and compete on a 3x3 grid using a GUI menu.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/ttt crear &lt;amount&gt; - Create the room.
/ttt unirse &lt;player&gt; - Join the game.
/ttt cancelar - Leave your created game.</code></pre>

            <h3>How to Play</h3>
            <p>Upon starting, turns are decided. Players alternately place their marks (X or O, represented by items). The first to align three marks wins all the bet money.</p>

            <h3>Limits and Betting Permissions</h3>
            <p>Controlled via <code>tictactoe.bet</code> and <code>tictactoe.uses</code>.</p>
            <ul>
                <li><strong>Daily Uses (Anti-Grind):</strong> This game takes longer. Configuring a daily limit (e.g., 10 games/day) via permissions ensures users try other casino games.</li>
            </ul>
        </div>
    `
};
