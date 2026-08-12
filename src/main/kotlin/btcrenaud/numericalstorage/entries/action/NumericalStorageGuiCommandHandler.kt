package btcrenaud.numericalstorage.entries.action

import com.typewritermc.core.extension.annotations.TypewriterCommand
import com.typewritermc.engine.paper.command.dsl.*
import org.koin.java.KoinJavaComponent.get
import btcrenaud.gui.services.MenuSessionService
import org.bukkit.entity.Player

/**
 * GUI button command handler.
 * Buttons dispatch commands like:
 *   numstorage tx <definitionId> <action>
 *   numstorage upgrade <definitionId>
 */
@TypewriterCommand
fun CommandTree.numericalStorageGuiCommands() = literal("numstorage") {
    literal("tx") {
        string("definitionId") { defIdArg ->
            string("action") { actionArg ->
                executes {
                    val player = sender as? org.bukkit.entity.Player ?: run {
                        sender.sendMessage("<red>Only players can use this command.")
                        return@executes
                    }
                    if (!MenuSessionService.hasActiveSession(player)) return@executes
                    val defId = defIdArg()
                    val action = actionArg()
                    val handler = get<NumericalStorageTransactionHandler>(NumericalStorageTransactionHandler::class.java)
                    handler.handleTransaction(player, action, defId)
                }
            }
        }
    }

    literal("upgrade") {
        string("definitionId") { defIdArg ->
            executes {
                val player = sender as? org.bukkit.entity.Player ?: run {
                    sender.sendMessage("<red>Only players can use this command.")
                    return@executes
                }
                if (!MenuSessionService.hasActiveSession(player)) return@executes
                val defId = defIdArg()
                val handler = get<NumericalStorageTransactionHandler>(NumericalStorageTransactionHandler::class.java)
                handler.handleTransaction(player, "upgrade", defId)
            }
        }
    }

    literal("back_main") {
        string("definitionId") { defIdArg ->
            executes {
                val player = sender as? org.bukkit.entity.Player ?: run {
                    sender.sendMessage("<red>Only players can use this command.")
                    return@executes
                }
                if (!MenuSessionService.hasActiveSession(player)) return@executes
                val defId = defIdArg()
                val handler = get<NumericalStorageTransactionHandler>(NumericalStorageTransactionHandler::class.java)
                handler.handleTransaction(player, "back_main", defId)
            }
        }
    }
}

/**
 * Handles commands originating from a GUI slot.
 *
 * MenuSessionService invokes this hook before falling back to Bukkit command
 * dispatch. Keeping the parsing local avoids losing the Player sender context
 * on Paper/Folia and makes repeated whitespace/case variations harmless.
 */
internal fun handleNumericalStorageGuiCommand(player: Player, command: String) {
    val args = command.trim().split(Regex("\\s+"), limit = 4)
    if (args.size < 3 || !args[0].equals("numstorage", ignoreCase = true)) return
    if (!MenuSessionService.hasActiveSession(player)) return

    val handler = get<NumericalStorageTransactionHandler>(NumericalStorageTransactionHandler::class.java)
    when (args[1].lowercase()) {
        "tx" -> if (args.size == 4) {
            handler.handleTransaction(player, args[3], args[2])
        }
        "upgrade", "back_main" -> {
            handler.handleTransaction(player, args[1].lowercase(), args[2])
        }
    }
}
