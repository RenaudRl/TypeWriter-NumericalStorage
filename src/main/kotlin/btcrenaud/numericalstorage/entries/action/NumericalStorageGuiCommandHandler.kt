package btcrenaud.numericalstorage.entries.action

import com.typewritermc.core.extension.annotations.TypewriterCommand
import com.typewritermc.engine.paper.command.dsl.*
import org.koin.java.KoinJavaComponent.get

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
                val defId = defIdArg()
                val handler = get<NumericalStorageTransactionHandler>(NumericalStorageTransactionHandler::class.java)
                handler.handleTransaction(player, "back_main", defId)
            }
        }
    }
}
