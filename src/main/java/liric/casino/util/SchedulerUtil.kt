package liric.casino.util

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

/**
 * Folia-compatible scheduling utilities.
 *
 * On Paper  → delegates to the classic BukkitScheduler.
 * On Folia  → uses GlobalRegionScheduler (for sync tasks) and
 *             AsyncScheduler (for async tasks), as recommended by
 *             https://docs.papermc.io/paper/dev/folia-support
 *
 * Use these instead of `Bukkit.getScheduler()` / `BukkitRunnable` everywhere.
 */
object SchedulerUtil {

    // ──────────────────────────────────────────────────────────────
    // ASYNC helpers  (DB access, HTTP calls, heavy computation …)
    // ──────────────────────────────────────────────────────────────

    /** Run [task] once on an async thread. */
    fun runAsync(plugin: Plugin, task: Runnable) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin) { task.run() }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        }
    }

    /**
     * Run [task] repeatedly on an async thread.
     * [initialDelayTicks] / [periodTicks] are in server ticks (Paper) or
     * converted to milliseconds on Folia.
     */
    fun runAsyncTimer(plugin: Plugin, initialDelayTicks: Long, periodTicks: Long, task: Runnable) {
        if (isFolia()) {
            val initMs  = initialDelayTicks * 50L
            val periodMs = periodTicks * 50L
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { task.run() }, initMs, periodMs, TimeUnit.MILLISECONDS)
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // SYNC / GLOBAL helpers  (main thread / global region on Folia)
    // ──────────────────────────────────────────────────────────────

    /** Run [task] once on the main thread (or global region thread on Folia). */
    fun runGlobal(plugin: Plugin, task: Runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin) { task.run() }
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    /**
     * Run [task] once after [delayTicks] ticks on the main thread
     * (or global region on Folia).
     */
    fun runGlobalLater(plugin: Plugin, delayTicks: Long, task: Runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { task.run() }, delayTicks)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks)
        }
    }

    /**
     * Run [task] repeatedly on the main thread (or global region on Folia).
     * Returns a canceller lambda – call it to stop the task.
     */
    fun runGlobalTimer(plugin: Plugin, initialDelayTicks: Long, periodTicks: Long, task: Runnable): Runnable {
        return if (isFolia()) {
            val handle = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, { task.run() }, initialDelayTicks.coerceAtLeast(1L), periodTicks)
            Runnable { handle?.cancel() }
        } else {
            val handle = Bukkit.getScheduler()
                .runTaskTimer(plugin, task, initialDelayTicks, periodTicks)
            Runnable { handle.cancel() }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Internal
    // ──────────────────────────────────────────────────────────────

    private fun isFolia(): Boolean = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    } catch (_: ClassNotFoundException) { false }
}
