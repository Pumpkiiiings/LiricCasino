window.WikiSections = window.WikiSections || {};

window.WikiSections['configuracion'] = {
    es: `
        <h2>Configuración y Límites</h2>
        <div class="about-content">
            <p>CasinoLiric integra un sistema avanzado de gestión de permisos que te permite tener un control total sobre <strong>cuánto juegan</strong> y <strong>cuánto apuestan</strong> los usuarios en cada módulo.</p>
            
            <h3>La estructura: <code>bet</code> y <code>uses</code></h3>
            <p>Cada juego (Blackjack, Ruleta, etc.) tiene en el archivo <code>config.yml</code> sus propias secciones de <code>bet</code> (Apuesta) y <code>uses</code> (Usos diarios). Esta estructura permite configurar un valor por defecto (default) y valores excepcionales mediante rangos y permisos.</p>

            <pre><code>  bet:
    default:
      min: 100.0
      max: 100000.0
    ranks:
      vip:
        permission: "casinoliric.bet.vip"
        max: 500000.0
      admin:
        permission: "casinoliric.bet.admin"
        max: 10000000.0

  uses:
    default:
      max-uses-per-day: 5
    ranks:
      vip:
        permission: "casinoliric.uses.vip"
        max-uses-per-day: 10
      admin:
        permission: "casinoliric.uses.admin"
        max-uses-per-day: -1</code></pre>

            <h3>Entendiendo la Configuración</h3>
            
            <h4>1. Límites de Apuesta (<code>bet</code>)</h4>
            <ul>
                <li><strong><code>default</code></strong>: Es el límite estándar. Cualquier jugador normal podrá apostar entre el <code>min</code> (100) y el <code>max</code> (100,000).</li>
                <li><strong><code>ranks</code></strong>: Si un jugador posee el permiso <code>casinoliric.bet.vip</code>, el sistema ignorará el máximo default y le permitirá apostar hasta 500,000. ¡Ideal para incentivar compras en tu tienda del servidor!</li>
            </ul>

            <h4>2. Usos Diarios (<code>uses</code>)</h4>
            <p>Este sistema previene el <em>"grind"</em> (jugadores abusando de los juegos todo el día).</p>
            <ul>
                <li>Un usuario sin permisos solo podrá jugar o utilizar el módulo <strong>5 veces al día</strong>.</li>
                <li>Los rangos VIP podrán usarlo hasta 10 veces.</li>
                <li>El uso del valor <strong><code>-1</code></strong> significa <strong>Usos Infinitos</strong>. En el ejemplo, los admins (<code>casinoliric.uses.admin</code>) no tienen límite.</li>
            </ul>

            <h3>Integración Automática</h3>
            <p>No necesitas usar comandos adicionales; CasinoLiric verifica automáticamente los permisos usando <strong>Vault</strong> cada vez que un jugador hace clic en un menú de apuesta o intenta jugar.</p>
        </div>
    `,
    en: `
        <h2>Configuration & Limits</h2>
        <div class="about-content">
            <p>CasinoLiric integrates an advanced permission management system that gives you total control over <strong>how much</strong> and <strong>how often</strong> users play in each module.</p>
            
            <h3>The Structure: <code>bet</code> and <code>uses</code></h3>
            <p>Every game (Blackjack, Roulette, etc.) has its own <code>bet</code> and <code>uses</code> (daily limits) sections in the <code>config.yml</code> file. This structure allows you to configure default values and exceptional values using ranks and permissions.</p>

            <pre><code>  bet:
    default:
      min: 100.0
      max: 100000.0
    ranks:
      vip:
        permission: "casinoliric.bet.vip"
        max: 500000.0
      admin:
        permission: "casinoliric.bet.admin"
        max: 10000000.0

  uses:
    default:
      max-uses-per-day: 5
    ranks:
      vip:
        permission: "casinoliric.uses.vip"
        max-uses-per-day: 10
      admin:
        permission: "casinoliric.uses.admin"
        max-uses-per-day: -1</code></pre>

            <h3>Understanding the Configuration</h3>
            
            <h4>1. Bet Limits (<code>bet</code>)</h4>
            <ul>
                <li><strong><code>default</code></strong>: This is the standard limit. Any normal player can bet between <code>min</code> (100) and <code>max</code> (100,000).</li>
                <li><strong><code>ranks</code></strong>: If a player has the <code>casinoliric.bet.vip</code> permission, the system overrides the default maximum and allows them to bet up to 500,000. Perfect for incentivizing server store purchases!</li>
            </ul>

            <h4>2. Daily Uses (<code>uses</code>)</h4>
            <p>This system prevents <em>"grinding"</em> (players abusing the games all day).</p>
            <ul>
                <li>A user without permissions can only play or use the module <strong>5 times a day</strong>.</li>
                <li>VIP ranks can use it up to 10 times.</li>
                <li>Using the value <strong><code>-1</code></strong> means <strong>Infinite Uses</strong>. In the example, admins (<code>casinoliric.uses.admin</code>) have no limit.</li>
            </ul>

            <h3>Automatic Integration</h3>
            <p>You don't need any additional commands; CasinoLiric automatically checks permissions using <strong>Vault</strong> every time a player clicks a bet menu or attempts to play.</p>
        </div>
    `
};
