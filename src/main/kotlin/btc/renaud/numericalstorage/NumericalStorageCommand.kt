package btc.renaud.numericalstorage

import com.typewritermc.core.extension.annotations.TypewriterCommand
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
import com.typewritermc.core.interaction.context
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.entity.Player
import java.math.BigDecimal

@TypewriterCommand
fun CommandTree.numericalStorageCommands() = literal("ns") {
    literal("reset") {
        withPermission("typewriter.ns.reset")
        entry<PlayerNumericalStorageArtifactEntry>("artifact") { artifact ->
            executePlayerOrTarget { target ->
                val art = artifact()
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
                    art.setLevel(target.uniqueId, lvl())
                    art.setBalance(target.uniqueId, BigDecimal.ZERO)
                    sender.msg("Set numerical storage level to ${lvl()} for ${target.name}.")
                    definition.levels.getOrNull(lvl() - 1)?.let { level ->
                        target.sendLevelUp(definition, level, lvl())
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
                    if (definition.classicLeveling) {
                        var levelNumber = art.getLevel(target.uniqueId)
                        var level = definition.levels.getOrNull(levelNumber - 1) ?: return@executePlayerOrTarget
                        var remaining = art.getBalance(target.uniqueId) + addAmount
                        while (levelNumber < definition.levels.size && remaining >= BigDecimal.valueOf(level.limit)) {
                            remaining -= BigDecimal.valueOf(level.limit)
                            levelNumber++
                            val nextLevel = definition.levels.getOrNull(levelNumber - 1) ?: break
                            target.sendLevelUp(definition, nextLevel, levelNumber)
                            level = nextLevel
                        }
                        art.setBalance(target.uniqueId, remaining)
                        art.setLevel(target.uniqueId, levelNumber)
                    } else {
                        art.addBalance(target.uniqueId, addAmount)
                    }
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
    level.levelUpTrigger.triggerFor(this, context())
}
