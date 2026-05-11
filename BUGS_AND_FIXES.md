# 🐛 Reporte de Vulnerabilidades y Soluciones (CasinoLiric)

Durante la auditoría del código se detectaron y parchearon los siguientes problemas críticos de seguridad y rendimiento.

---

## 1. Duplicación Masiva de Entidades (LAG EXTREMO)
**Estado:** `[SOLUCIONADO]` | **Severidad:** `CRÍTICA`

### El Problema
El monitor de entidades de la `RouletteMix` corría cada 5 segundos (100 ticks). Cuando se reiniciaba el servidor, las entidades base se borraban (o perdían su link), pero el Holograma de Estado (`statusText`) moría. Esto activaba la función `spawnRoulette()`.
Si el servidor sufría un pico de lag, el monitor podía ejecutarse dos veces seguidas mientras la ruleta apenas estaba repareciendo, causando que se instanciaran **75 entidades duplicadas (Displays) por cada ciclo**.

### La Solución
1. **Guardia de concurrencia (`respawningKeys`)**: Se agregó un HashSet que registra qué ruletas están en proceso de regenerarse, impidiendo ejecuciones superpuestas.
2. **Intervalo extendido**: El chequeo pasó de 5 segundos a 30 segundos (600 ticks).
3. **Verificación de Chunks**: Ahora el plugin ignora el chequeo si el chunk donde está la ruleta no se encuentra cargado, evitando carga innecesaria en memoria.

---

## 2. Double-Submit en Apuestas (Race Condition)
**Estado:** `[SOLUCIONADO]` | **Severidad:** `ALTA`

### El Problema
En `BetAmountMixMenu`, `BlackjackBetMenu` y `SlotMachineMenu`, los jugadores confirmaban sus apuestas haciendo click en un botón. Debido a la naturaleza asíncrona de los menús (Triumph-GUI cierra los menús en el siguiente tick), un jugador podía spamear click derecho/izquierdo rápidamente (o usar un auto-clicker) y enviar la señal de "Confirmar" múltiples veces en el mismo milisegundo. Esto generaba cobros dobles si tenían saldo o incluso forzaba el inicio de jugadas duplicadas.

### La Solución
Se implementó `AtomicBoolean` de `java.util.concurrent.atomic`.
Antes de realizar cualquier cobro a la economía, se evalúa `if (!processing.compareAndSet(false, true)) return`. Esto bloquea el hilo de manera atómica, garantizando matemáticamente que solo el primer click se ejecute.

---

## 3. Dupe (Vulnerabilidad) de Double Down en Blackjack
**Estado:** `[SOLUCIONADO]` | **Severidad:** `ALTA`

### El Problema
En el menú en juego de Blackjack, la acción de "Doblar (Double Down)" permitía al jugador retirar el equivalente a su apuesta (`withdrawPlayer`). El bloqueo que impedía usar el botón nuevamente (`isFirstAction = false`) se asignaba **después** de comprobar la economía.
Un jugador con lag y autoclicker podía darle click 3 veces antes de que el servidor respondiera, retirando el triple de la apuesta y ganando exponencialmente si derrotaba a la casa.

### La Solución
Se movió el flag `isFirstAction = false` al principio de la ejecución del botón (bloqueo optimista). Si la economía rechaza la transacción (falta de fondos), se hace rollback del flag con `isFirstAction = true`.

---

## 4. Pago Doble en Rasca y Gana (Click Spam)
**Estado:** `[SOLUCIONADO]` | **Severidad:** `MEDIA`

### El Problema
El sistema de revelado de slots en `ScratchMenu` usaba un simple `mutableSetOf<Int>()` para registrar qué casillas ya habían sido rascadas. Las colecciones estándar no son "Thread-Safe". Un click múltiple sobre la misma casilla podía evadir la comprobación de `scratchedSlots.contains(slot)` y sumar 2 veces a la cuenta del premio (`revealedCounts`). Con 2 clicks rápidos en el premio gordo, completaba el requerimiento de "match = 3" instantáneamente.

### La Solución
1. Las variables se cambiaron a versiones atómicas: `ConcurrentHashMap.newKeySet()` y `@Volatile var locked = false`.
2. La validación ahora delega en el Set Concurrente: `if (!scratchedSlots.add(slot)) return`, lo que es completamente seguro contra condiciones de carrera.

---

## Recomendaciones de Mantenimiento

> [!NOTE]
> Las estadísticas ahora se guardan en base de datos. Para asegurar escalabilidad, mantén la variable `database.type: sqlite` para servidores medianos, o migra a `mariadb` si montas un entorno Multi-Server (BungeeCord/Velocity).
