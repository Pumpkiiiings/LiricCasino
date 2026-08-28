window.WikiSections = window.WikiSections || {};

window.WikiSections['ruleta'] = {
    es: `
        <h2>Ruleta (Roulette)</h2>
        <div class="about-content">
            <p>La ruleta clásica de casino. Los jugadores pueden apostar a números, colores o pares/impares y observar el giro en tiempo real mediante un menú interactivo.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/ruleta jugar - Abre el menú de apuestas.
/ruleta setup - Crea decoraciones y un holograma donde estás mirando.
/ruleta forcestart - Inicia la ruleta inmediatamente (Admin).
/ruleta purge - Fuerza la limpieza de la mesa.</code></pre>

            <h3>Cómo Jugar</h3>
            <p>Al abrir el menú, los jugadores seleccionan el monto de la ficha y luego hacen clic en los números de la ruleta (1-36), Colores (Rojo/Negro) o Pares/Impares. Cuando el contador termina, la ruleta gira visualmente.</p>

            <h3>Control de Economía</h3>
            <p>Al igual que otros juegos, cuenta con el sistema dinámico de límites en <code>config.yml</code> (<code>roulette.bet</code> y <code>roulette.uses</code>).</p>
            <ul>
                <li><strong>Min/Max Bet:</strong> Cada clic suma al total apostado. El sistema valida automáticamente que no excedas tu límite configurado según tus permisos (ej. límite VIP vs Default).</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Roulette</h2>
        <div class="about-content">
            <p>The classic casino roulette. Players can bet on numbers, colors, or evens/odds and watch the spin in real-time through an interactive menu.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/ruleta jugar - Opens the betting menu.
/ruleta setup - Creates decorations and a hologram where you are looking.
/ruleta forcestart - Forces the roulette to spin immediately (Admin).
/ruleta purge - Forces a table cleanup.</code></pre>

            <h3>How to Play</h3>
            <p>Upon opening the menu, players select their chip amount and click on numbers (1-36), Colors (Red/Black), or Evens/Odds. When the timer ends, the roulette visually spins.</p>

            <h3>Economy Control</h3>
            <p>Like other games, it features the dynamic limits system in <code>config.yml</code> (<code>roulette.bet</code> and <code>roulette.uses</code>).</p>
            <ul>
                <li><strong>Min/Max Bet:</strong> Every click adds to the total bet. The system automatically validates that you don't exceed your configured limit based on permissions (e.g. VIP limit vs Default).</li>
            </ul>
        </div>
    `
};
