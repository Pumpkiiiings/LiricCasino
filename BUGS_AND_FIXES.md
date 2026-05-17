# 🐛 Reporte de Vulnerabilidades y Soluciones (CasinoLiric)

Durante las auditorías de código se detectaron y parchearon los siguientes problemas críticos de seguridad y rendimiento.

---

## 🛑 AUDITORÍA v2.0.0 (Nuevos Sistemas PvP y Lotería)

### 5. Fuga de Economía en Reinicios (Wipe en `cleanupAll`)
**Estado:** `[PENDIENTE]` | **Severidad:** `CRÍTICA`
- **El Problema:** Al crear partidas de CoinFlip, RPS o Tic Tac Toe, Vault retira el dinero inmediatamente y la partida se almacena en memoria (`WAITING`). Si el servidor se apaga o reinicia (ej. `/stop` o `/casino reload`), el método `cleanupAll()` limpia los diccionarios (`sessions.clear()`) pero no devuelve el dinero.
- **Consecuencia:** **Pérdida masiva de fondos**. Todo el dinero de las partidas en espera se pierde para siempre.
- **La Solución (Planeada):** Iterar sobre las sesiones activas dentro de `cleanupAll()` y ejecutar `economyManager.depositPlayer()` a todos los participantes antes de limpiar la memoria RAM.

### 6. Bloqueo de Capital por Desconexión (Limbo)
**Estado:** `[PENDIENTE]` | **Severidad:** `ALTA`
- **El Problema:** En RPS y TTT, si el oponente abandona el servidor durante su turno o fase de selección, el juego se queda esperando indefinidamente.
- **Consecuencia:** El dinero de ambos jugadores queda congelado para siempre. Además, genera una pequeña fuga de memoria.
- **La Solución (Planeada):** Crear un `PlayerQuitListener`. Si un jugador huye de una partida activa, la partida finaliza dándole victoria automática al oponente que no se desconectó. Si la partida estaba en espera, se cancela y se devuelve el dinero.

---

## ✅ AUDITORÍA v1.5.0 a v1.7.0 (Sistemas Anteriores)

### 1. Duplicación Masiva de Entidades (LAG EXTREMO)
**Estado:** `[SOLUCIONADO]` | **Severidad:** `CRÍTICA`
- **El Problema:** El monitor de la `RouletteMix` reparecía 75 entidades por cada ciclo si había lag.
- **La Solución:** `respawningKeys` concurrente y extensión de intervalo de 5s a 30s.

### 2. Double-Submit en Apuestas (Race Condition)
**Estado:** `[SOLUCIONADO]` | **Severidad:** `ALTA`
- **El Problema:** Spam de clicks rápidos en GUIs causaba doble cobro o juegos paralelos (Slots, Blackjack).
- **La Solución:** Candados atómicos (`AtomicBoolean`) antes de las transacciones de Vault.

### 3. Dupe (Vulnerabilidad) de Double Down en Blackjack
**Estado:** `[SOLUCIONADO]` | **Severidad:** `ALTA`
- **El Problema:** Spam de click retiraba el triple de fondos en el Double Down.
- **La Solución:** Bloqueo optimista y rollback de estado.

### 4. Pago Doble en Rasca y Gana (Click Spam)
**Estado:** `[SOLUCIONADO]` | **Severidad:** `MEDIA`
- **El Problema:** Sets no concurrentes permitían marcar la misma casilla múltiples veces.
- **La Solución:** Uso de `ConcurrentHashMap.newKeySet()` en la clase `ScratchSession`.

---

## Recomendaciones de Mantenimiento

> [!NOTE]
> Las estadísticas ahora se guardan en base de datos. Para asegurar escalabilidad, mantén la variable `database.type: sqlite` para servidores medianos, o migra a `mariadb` si montas un entorno Multi-Server (BungeeCord/Velocity).
