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
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.core.interaction.context
import btcrenaud.numericalstorage.entries.action.NumericalStorageOpenMenuEntry
import com.typewritermc.core.entries.ref
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.math.BigDecimal

@TypewriterCommand
fun CommandTree.numericalStorageCommands() = literal("ns") {
    literal("reset") {
        withPermission("typewriter.ns.reset")
        entry<NumericalStorageDefinitionEntry>("definition") { def ->
            executePlayerOrTarget { target ->
                val definition = def()
                val art = definition.artifact.get()
                    ?: return@executePlayerOrTarget sender.replyNoArtifact(definition, target)
                NumericalStorageCoroutines.launch {
                    art.update { balances, levels, _ ->
                        val key = art.storageKey(target.uniqueId, definition.profileMode)
                        levels[key] = 1
                        balances[key] = BigDecimal.ZERO
                    }
                    NumericalStorageCoroutines.onPlayerThread(target) {
                        sender.reply(definition, definition.adminMessages.resetMessage, unparsed("player", target.name))
                    }
                }
            }
        }
    }

    literal("level") {
        withPermission("typewriter.ns.level")
        entry<NumericalStorageDefinitionEntry>("definition") { def ->
            int("level", min = 1) { lvl ->
                executePlayerOrTarget { target ->
                    val definition = def()
                    val art = definition.artifact.get()
                        ?: return@executePlayerOrTarget sender.replyNoArtifact(definition, target)
                    val levelVal = lvl()
                    NumericalStorageCoroutines.launch {
                        art.update { balances, levels, _ ->
                            val key = art.storageKey(target.uniqueId, definition.profileMode)
                            levels[key] = levelVal.coerceAtLeast(1)
                            balances[key] = BigDecimal.ZERO
                        }
                        NumericalStorageCoroutines.onPlayerThread(target) {
                            sender.reply(
                                definition,
                                definition.adminMessages.levelSetMessage,
                                unparsed("player", target.name),
                                unparsed("level", levelVal.toString()),
                            )
                            definition.levels.getOrNull(levelVal - 1)?.let { level ->
                                target.sendLevelUp(definition, level, levelVal)
                            }
                        }
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
                    val art = definition.artifact.get()
                        ?: return@executePlayerOrTarget sender.replyNoArtifact(definition, target)
                    val typed = amt()
                    val amount = typed.toFinitePositiveAmountOrNull()
                        ?: return@executePlayerOrTarget sender.replyInvalidAmount(definition, target, typed)
                    NumericalStorageCoroutines.launch {
                        art.addBalance(target.uniqueId, amount, definition.profileMode)
                        NumericalStorageCoroutines.onPlayerThread(target) {
                            sender.reply(
                                definition,
                                definition.adminMessages.addMessage,
                                unparsed("player", target.name),
                                unparsed("amount", amount.toPlainString()),
                            )
                        }
                    }
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
                    val art = definition.artifact.get()
                        ?: return@executePlayerOrTarget sender.replyNoArtifact(definition, target)
                    val typed = amt()
                    val amount = typed.toFinitePositiveAmountOrNull()
                        ?: return@executePlayerOrTarget sender.replyInvalidAmount(definition, target, typed)
                    NumericalStorageCoroutines.launch {
                        art.removeBalance(target.uniqueId, amount, definition.profileMode)
                        NumericalStorageCoroutines.onPlayerThread(target) {
                            sender.reply(
                                definition,
                                definition.adminMessages.removeMessage,
                                unparsed("player", target.name),
                                unparsed("amount", amount.toPlainString()),
                            )
                        }
                    }
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
                    sender.reply(definition, definition.adminMessages.noMenuMessage)
                }
            }
        }
    }
}

private fun CommandSender.reply(
    definition: NumericalStorageDefinitionEntry,
    message: String,
    vararg resolvers: TagResolver,
) = sendMiniWithResolvers(
    message,
    parsed("prefix", definition.prefix),
    unparsed("storage", definition.id),
    *resolvers,
)

private fun CommandSender.replyNoArtifact(definition: NumericalStorageDefinitionEntry, target: Player) =
    reply(definition, definition.adminMessages.noArtifactMessage, unparsed("player", target.name))

private fun CommandSender.replyInvalidAmount(definition: NumericalStorageDefinitionEntry, target: Player, typed: Double) =
    reply(
        definition,
        definition.adminMessages.invalidAmountMessage,
        unparsed("player", target.name),
        unparsed("amount", typed.toString()),
    )

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
