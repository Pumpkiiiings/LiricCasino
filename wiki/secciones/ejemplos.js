window.WikiSections = window.WikiSections || {};

window.WikiSections['ejemplos'] = {
    es: `
        <h2>Ejemplos de Archivos</h2>
        <div class="about-content">
            <p>CasinoLiric genera automáticamente todos sus archivos de configuración y menús en tu servidor. Aquí tienes un ejemplo interactivo de cómo se ve el directorio <code>plugins/CasinoLiric/</code> una vez instalado.</p>
            <p style="font-size: 0.9rem; color: var(--text-muted); margin-bottom: 20px;"><em>Tip: Haz clic en las carpetas para desplegarlas y ver su contenido.</em></p>
            
            <div class="interactive-file-tree">
                <details open>
                    <summary>CasinoLiric/</summary>
                    <div class="tree-content">
                        <div class="tree-file clickable-file" data-file="config.yml">config.yml <span style="color: var(--emerald); font-size: 0.8em; margin-left: 10px;">(Configuración global, permisos y límites)</span></div>
                        <div class="tree-file clickable-file" data-file="messages.yml">messages.yml <span style="color: var(--text-muted); font-size: 0.8em; margin-left: 10px;">(Traducciones y mensajes)</span></div>
                        <div class="tree-file clickable-file" data-file="webhooks.yml">webhooks.yml <span style="color: var(--text-muted); font-size: 0.8em; margin-left: 10px;">(Integración con Discord)</span></div>
                        <div class="tree-file clickable-file" data-file="plugin.yml">plugin.yml</div>
                        
                        <details>
                            <summary>menus/</summary>
                            <div class="tree-content">
                                <div class="tree-file clickable-file" data-file="menus/blackjack_bet.yml">blackjack_bet.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/blackjack_choice.yml">blackjack_choice.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/ruleta.yml">ruleta.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/slots.yml">slots.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/coinflip.yml">coinflip.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/rps.yml">rps.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/scratch.yml">scratch.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/lottery.yml">lottery.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/ttt.yml">ttt.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/racing.yml">racing.yml</div>
                            </div>
                        </details>
                        
                        <details>
                            <summary>data/</summary>
                            <div class="tree-content">
                                <div class="tree-file clickable-file" data-file="data.yml">players.db <span style="color: var(--text-muted); font-size: 0.8em; margin-left: 10px;">(Base de datos local SQlite)</span></div>
                            </div>
                        </details>
                    </div>
                </details>
            </div>

            <h3>Personalización de Menús</h3>
            <p>Puedes editar cualquier archivo dentro de la carpeta <code>menus/</code> para cambiar los ítems, nombres, y posiciones (slots) del inventario GUI. ¡Si usas <strong>PlaceholderAPI</strong>, los placeholders funcionarán perfectamente en la descripción (lore) y nombre de los ítems!</p>
        </div>
    `,
    en: `
        <h2>File Examples</h2>
        <div class="about-content">
            <p>CasinoLiric automatically generates all its configuration and menu files on your server. Here is an interactive example of how the <code>plugins/CasinoLiric/</code> directory looks once installed.</p>
            <p style="font-size: 0.9rem; color: var(--text-muted); margin-bottom: 20px;"><em>Tip: Click on the folders to expand them and see their content.</em></p>
            
            <div class="interactive-file-tree">
                <details open>
                    <summary>CasinoLiric/</summary>
                    <div class="tree-content">
                        <div class="tree-file clickable-file" data-file="config.yml">config.yml <span style="color: var(--emerald); font-size: 0.8em; margin-left: 10px;">(Configuración global, permisos y límites)</span></div>
                        <div class="tree-file clickable-file" data-file="messages.yml">messages.yml <span style="color: var(--text-muted); font-size: 0.8em; margin-left: 10px;">(Traducciones y mensajes)</span></div>
                        <div class="tree-file clickable-file" data-file="webhooks.yml">webhooks.yml <span style="color: var(--text-muted); font-size: 0.8em; margin-left: 10px;">(Integración con Discord)</span></div>
                        <div class="tree-file clickable-file" data-file="plugin.yml">plugin.yml</div>
                        
                        <details>
                            <summary>menus/</summary>
                            <div class="tree-content">
                                <div class="tree-file clickable-file" data-file="menus/blackjack_bet.yml">blackjack_bet.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/blackjack_choice.yml">blackjack_choice.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/ruleta.yml">ruleta.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/slots.yml">slots.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/coinflip.yml">coinflip.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/rps.yml">rps.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/scratch.yml">scratch.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/lottery.yml">lottery.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/ttt.yml">ttt.yml</div>
                                <div class="tree-file clickable-file" data-file="menus/racing.yml">racing.yml</div>
                            </div>
                        </details>
                        
                        <details>
                            <summary>data/</summary>
                            <div class="tree-content">
                                <div class="tree-file clickable-file" data-file="data.yml">players.db <span style="color: var(--text-muted); font-size: 0.8em; margin-left: 10px;">(Base de datos local SQlite)</span></div>
                            </div>
                        </details>
                    </div>
                </details>
            </div>

            <h3>Menu Customization</h3>
            <p>You can edit any file inside the <code>menus/</code> folder to change the items, names, and slots of the GUI inventory. If you use <strong>PlaceholderAPI</strong>, placeholders will work perfectly in the item's lore and name!</p>
        </div>
    `
};
