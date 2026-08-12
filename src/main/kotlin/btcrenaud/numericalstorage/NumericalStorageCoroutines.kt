package btcrenaud.numericalstorage

import com.typewritermc.core.utils.UntickedAsync
import com.typewritermc.engine.paper.plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bukkit.entity.Player
import org.bukkit.Bukkit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Lifecycle-bound async work and Folia-safe player affinity boundary. */
object NumericalStorageCoroutines {
    private val scopeRef = AtomicReference<CoroutineScope?>(null)

    fun initialize() {
        scopeRef.getAndSet(CoroutineScope(SupervisorJob() + Dispatchers.UntickedAsync))
            ?.cancel("numerical-storage-reinitialised")
    }

    fun shutdown() {
        scopeRef.getAndSet(null)?.cancel("numerical-storage-shutdown")
    }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = ensureScope().launch(block = block)

    suspend fun <T> onPlayerThread(player: Player, block: () -> T): T? =
        suspendCancellableCoroutine { continuation ->
            if (!plugin.isEnabled) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            player.scheduler.run(
                plugin,
                { _ ->
                    if (!continuation.isActive || !player.isOnline) return@run
                    runCatching(block).fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(it) },
                    )
                },
                { if (continuation.isActive) continuation.resume(null) },
            )
        }

    suspend fun <T> onGlobalThread(block: () -> T): T? =
        suspendCancellableCoroutine { continuation ->
            if (!plugin.isEnabled) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            Bukkit.getGlobalRegionScheduler().run(plugin, { _ ->
                if (!continuation.isActive) return@run
                runCatching(block).fold(
                    onSuccess = { continuation.resume(it) },
                    onFailure = { continuation.resumeWithException(it) },
                )
            })
        }

    private fun ensureScope(): CoroutineScope {
        scopeRef.get()?.let { return it }
        val fallback = CoroutineScope(SupervisorJob() + Dispatchers.UntickedAsync)
        if (scopeRef.compareAndSet(null, fallback)) return fallback
        fallback.cancel("scope-race")
        return scopeRef.get() ?: fallback
    }
}
