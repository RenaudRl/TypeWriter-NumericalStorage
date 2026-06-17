package btcrenaud.numericalstorage

import btcrenaud.gui.services.MenuSessionService
import btcrenaud.numericalstorage.entries.action.NumericalStorageTransactionHandler
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import org.koin.java.KoinJavaComponent.get

@Singleton
class NumericalStorageExtension : Initializable {

    override suspend fun initialize() {
        // Register custom GUI command handler so button clicks are intercepted
        // before being dispatched as console commands (which would fail the
        // sender-as-Player check in @TypewriterCommand handlers).
        MenuSessionService.registerCustomCommandHandler("numstorage ") { player, _, cmd, _, _ ->
            val trimmed = cmd.removePrefix("numstorage ").trim()
            val parts = trimmed.split(" ")
            val handler = get<NumericalStorageTransactionHandler>(NumericalStorageTransactionHandler::class.java)

            when {
                parts.size >= 3 && parts[0] == "tx" -> {
                    val defId = parts[1]
                    val action = parts.drop(2).joinToString(" ")
                    handler.handleTransaction(player, action, defId)
                }
                parts.size >= 2 && parts[0] == "upgrade" -> {
                    val defId = parts[1]
                    handler.handleTransaction(player, "upgrade", defId)
                }
                parts.size >= 2 && parts[0] == "back_main" -> {
                    val defId = parts[1]
                    handler.handleTransaction(player, "back_main", defId)
                }
            }
        }
    }

    override suspend fun shutdown() {
        // Koin auto-discovery handles shutdown for @Singleton services.
    }
}
