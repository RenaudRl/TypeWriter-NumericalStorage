package btcrenaud.numericalstorage

import com.typewritermc.core.extension.annotations.TypewriterCommand
import com.typewritermc.core.entries.Query
import com.typewritermc.engine.paper.command.dsl.CommandTree
import com.typewritermc.engine.paper.command.dsl.double
import com.typewritermc.engine.paper.command.dsl.int
import com.typewritermc.engine.paper.command.dsl.entry
import com.typewritermc.engine.paper.command.dsl.executePlayerOrTarget
import com.typewritermc.engine.paper.command.dsl.sender
import com.typewritermc.engine.paper.command.dsl.withPermission
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.core.interaction.context
import btcrenaud.numericalstorage.entries.action.NumericalStorageOpenMenuEntry
import com.typewritermc.core.entries.ref
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.entity.Player
import java.math.BigDecimal

@TypewriterCommand
fun CommandTree.numericalStorageCommands() = literal("ns") {
    literal("reset") {
        withPermission("typewriter.ns.reset")
        entry<NumericalStorageDefinitionEntry>("definition") { def ->
            executePlayerOrTarget { target ->
                val definition = def()
                val art = definition.artifact.get() ?: return@executePlayerOrTarget
                art.resetLevel(target.uniqueId)
                art.setBalance(target.uniqueId, BigDecimal.ZERO)
                sender.msg("Numerical storage reset for ${target.name}.")
            }
        }
    }

    literal("level") {
        withPermission("typewriter.ns.level")
        entry<NumericalStorageDefinitionEntry>("definition") { def ->
            int("level", min = 1) { lvl ->
                executePlayerOrTarget { target ->
                    val definition = def()
                    val art = definition.artifact.get() ?: return@executePlayerOrTarget
                    val levelVal = lvl()
                    art.setLevel(target.uniqueId, levelVal)
                    sender.msg("Set numerical storage level to $levelVal for ${target.name}.")
                    definition.levels.getOrNull(levelVal - 1)?.let { level ->
                        target.sendLevelUp(definition, level, levelVal)
                    }
                }
            }
        }
    }

    literal("add") {
        withPermission("typewriter.ns.add")
        entry<NumericalStorageDefinitionEntry>("definition") { def ->
            double("amount", min = 0.0) { amt ->
                executePlayerOrTarget { target ->
                    val definition = def()
                    val art = definition.artifact.get() ?: return@executePlayerOrTarget
                    val addAmount = BigDecimal.valueOf(amt())
                    art.addBalance(target.uniqueId, addAmount)
                    sender.msg("Added ${amt()} to ${target.name}.")
                }
            }
        }
    }

    literal("remove") {
        withPermission("typewriter.ns.remove")
        entry<NumericalStorageDefinitionEntry>("definition") { def ->
            double("amount", min = 0.0) { amt ->
                executePlayerOrTarget { target ->
                    val definition = def()
                    val art = definition.artifact.get() ?: return@executePlayerOrTarget
                    art.removeBalance(target.uniqueId, BigDecimal.valueOf(amt()))
                    sender.msg("Removed ${amt()} from ${target.name}.")
                }
            }
        }
    }

    literal("open") {
        withPermission("typewriter.ns.open")
        entry<NumericalStorageDefinitionEntry>("definition") { def ->
            executePlayerOrTarget { target ->
                val definition = def()
                // Find the corresponding open menu entry for this definition
                val menuEntries = Query.find<NumericalStorageOpenMenuEntry>()
                var menuEntry: NumericalStorageOpenMenuEntry? = null
                for (me in menuEntries) {
                    val meDef = me.definition.get()
                    if (meDef != null && meDef.id == definition.id) {
                        menuEntry = me
                        break
                    }
                }
                if (menuEntry != null) {
                    menuEntry.ref().triggerFor(target, context())
                } else {
                    sender.msg("<red>No menu entry found for storage '${definition.id}'. Create a 'numericalstorage_open_menu' entry in a Typewriter page.")
                }
            }
        }
    }
}

private fun Player.sendLevelUp(
    definition: NumericalStorageDefinitionEntry,
    level: BankLevel,
    levelNumber: Int,
) {
    sendMiniWithResolvers(
        level.levelUpMessage,
        parsed("limit", BigDecimal.valueOf(level.limit).toPlainString()),
        parsed("prefix", definition.prefix),
        parsed("level", levelNumber.toString())
    )
    level.levelUpTriggers.forEach { triggerRef ->
        triggerRef.triggerFor(this, context())
    }
}
