window.WikiSections = window.WikiSections || {};

window.WikiSections['loteria'] = {
    es: `
        <h2>Lotería</h2>
        <div class="about-content">
            <p>El sistema de Lotería permite a los jugadores comprar boletos y esperar a un sorteo automático (diario, semanal, o cuando se cumpla una meta) donde un afortunado gana el gran bote.</p>
            
            <h3>Comandos y Setup</h3>
            <pre><code>/loteria info - Muestra información del bote actual y tus boletos.
/loteria comprar [cantidad] - Compra boletos.
/loteria forcestart - Fuerza el sorteo (Admin).
/loteria give &lt;jugador&gt; [cantidad] - Da boletos gratis (Admin).</code></pre>

            <h3>Cómo Funciona</h3>
            <p>Cada vez que un jugador compra un boleto, una parte del costo va al "Bote Acumulado". Al llegar el momento del sorteo, el sistema escoge un boleto ganador al azar y le transfiere el bote entero (menos impuestos si están configurados).</p>

            <h3>Límites y Permisos de Apuesta</h3>
            <p>En este juego no se apuesta dinero directamente, sino que se compran boletos (<code>lottery.bet</code> para el costo o uso, <code>lottery.uses</code> para máximo de boletos).</p>
            <ul>
                <li><strong>Máximo de Boletos:</strong> Puedes limitar cuántos boletos puede comprar un jugador en un mismo sorteo con <code>lottery.uses.ranks.&lt;rango&gt;.max-uses-per-day</code>, previniendo que jugadores ricos compren la lotería entera.</li>
            </ul>
        </div>
    `,
    en: `
        <h2>Lottery</h2>
        <div class="about-content">
            <p>The Lottery system allows players to buy tickets and wait for an automatic draw (daily, weekly, or goal-based) where a lucky winner takes the huge jackpot.</p>
            
            <h3>Commands and Setup</h3>
            <pre><code>/lottery info - Shows jackpot info and your tickets.
/lottery comprar [amount] - Buy tickets.
/lottery forcestart - Force the draw (Admin).
/lottery give &lt;player&gt; [amount] - Give free tickets (Admin).</code></pre>

            <h3>How it Works</h3>
            <p>Every time a ticket is bought, a portion of the cost goes into the "Accumulated Jackpot". When the draw time arrives, the system picks a random winning ticket and transfers the entire pot (minus configured taxes).</p>

            <h3>Limits and Betting Permissions</h3>
            <p>In this game you don't bet money directly, but buy tickets (<code>lottery.bet</code> for cost/use, <code>lottery.uses</code> for max tickets).</p>
            <ul>
                <li><strong>Max Tickets:</strong> You can limit how many tickets a player can buy in a single draw with <code>lottery.uses.ranks.&lt;rank&gt;.max-uses-per-day</code>, preventing rich players from buying out the entire lottery.</li>
            </ul>
        </div>
    `
};
