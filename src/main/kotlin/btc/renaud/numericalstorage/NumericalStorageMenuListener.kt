package btc.renaud.numericalstorage

import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.core.interaction.context
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import com.typewritermc.engine.paper.entry.entries.get

@Singleton
class NumericalStorageMenuListener(private val plugin: Plugin) : Listener {

    fun register() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holderRaw = try {
            val top = event.view.topInventory
            val h = top.holder ?: return
            // Use class name comparison to avoid ClassNotFoundException after hot reload
            if (h::class.java.name != NumericalStorageMenu::class.java.name) return
            h as NumericalStorageMenu
        } catch (e: Throwable) {
            return
        }
        val holder = holderRaw
        if (event.clickedInventory != event.view.topInventory) return // Allow bottom inventory interaction? Maybe not for safety.
        // Actually, usually we cancel top inventory clicks.
        
        event.isCancelled = true
        
        val player = event.whoClicked as? Player ?: return
        val config = holder.definition.menu
        val artifact = holder.definition.artifact.get() ?: return

        // Check Transaction Buttons
        config.transactionButtons.find { it.slot == event.slot }?.let { btn ->
            handleTransaction(player, holder.definition, artifact, btn)
            return
        }

        // Check Level Button - always active now (no LevelingMode check)
        if (config.levelButton.slot == event.slot) {
            handleLevelButton(player, holder.definition, artifact)
            return
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is NumericalStorageMenu) {
            event.isCancelled = true
        }
    }

    private fun handleLevelButton(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry
    ) {
        val level = artifact.getLevel(player.uniqueId)
        val maxLevel = definition.levels.size
        
        if (level >= maxLevel) {
            player.sendMiniWithResolvers(
                definition.menu.maxLevelMessage,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
            )
            return
        }
        
        val currentLevelConfig = definition.levels[level - 1]
        val nextLevelConfig = definition.levels[level]
        val cost = BigDecimal.valueOf(currentLevelConfig.limit)
        val balance = artifact.getBalance(player.uniqueId)
        
        // Check criteria for next level with support for FACT mode
        val criteriaNotMet = nextLevelConfig.criteria.filter { criteria ->
            val currentValue = when (criteria.mode) {
                CriteriaMode.PLACEHOLDER -> criteria.placeholder.parsePlaceholders(player).toDoubleOrNull() ?: 0.0
                CriteriaMode.FACT -> criteria.fact.get()?.readForPlayersGroup(player)?.value?.toDouble() ?: 0.0
            }
            !criteria.isMet(currentValue)
        }
        
        if (criteriaNotMet.isNotEmpty()) {
            player.sendMiniWithResolvers(
                definition.menu.criteriaNotMetMessage,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
            )
            return
        }
        
        if (balance >= cost) {
            val newBalance = balance.subtract(cost)
            artifact.setBalance(player.uniqueId, newBalance)
            
            val nextLevelNumber = level + 1
            artifact.setLevel(player.uniqueId, nextLevelNumber)
            
            // Send feedback
            player.sendMiniWithResolvers(
                nextLevelConfig.levelUpMessage,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("limit", BigDecimal.valueOf(nextLevelConfig.limit).toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("level", nextLevelNumber.toString())
            )
            
            // Execute all triggers
            nextLevelConfig.levelUpTriggers.forEach { triggerRef ->
                triggerRef.triggerFor(player, context())
            }
            
            // Refresh menu
            if (player.openInventory.topInventory.holder is NumericalStorageMenu) {
                (player.openInventory.topInventory.holder as NumericalStorageMenu).render()
            }
        } else {
            player.sendMiniWithResolvers(
                definition.menu.notEnoughFundsMessage,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("cost", cost.toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
            )
        }
    }

    private fun handleTransaction(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
        btn: TransactionButtonConfig
    ) {
        when (btn.amountType) {
            AmountType.FIXED -> {
                executeValidatedTransaction(player, definition, artifact, btn.type, btn.amount)
            }
            AmountType.PERCENTAGE -> {
                val sourceAmount = getSourceAmount(player, definition)
                if (sourceAmount <= 0) {
                    player.sendMiniWithResolvers(
                        definition.transaction.noFundsMessage,
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
                val amount = sourceAmount * (btn.amount / 100.0)
                executeValidatedTransaction(player, definition, artifact, btn.type, amount)
            }
            AmountType.CUSTOM -> {
                player.closeInventory()
                NumericalStorageDialogService.openAmountDialog(player, definition.menu.customAmountDialog) { amount ->
                    executeValidatedTransaction(player, definition, artifact, btn.type, amount)
                    player.openInventory(NumericalStorageMenu(player, definition).inventory)
                }
            }
        }
    }

    /**
     * Get the source amount from TransactionConfig.amountPlaceholder
     */
    private fun getSourceAmount(player: Player, definition: NumericalStorageDefinitionEntry): Double {
        val placeholder = definition.transaction.amountPlaceholder
        if (placeholder.isBlank()) return 0.0
        return placeholder.parsePlaceholders(player).toDoubleOrNull() ?: 0.0
    }

    /**
     * Execute transaction with full validation
     */
    private fun executeValidatedTransaction(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
        type: TransactionType,
        amount: Double
    ) {
        if (amount <= 0) {
            player.sendMiniWithResolvers(
                definition.transaction.invalidAmountMessage,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
            )
            return
        }

        val decimalAmount = BigDecimal.valueOf(amount)
        val balance = artifact.getBalance(player.uniqueId)
        val level = artifact.getLevel(player.uniqueId)
        val levelConfig = definition.levels.getOrNull(level - 1)
        val limit = levelConfig?.let { BigDecimal.valueOf(it.limit) } ?: BigDecimal.valueOf(Double.MAX_VALUE)
        val txConfig = definition.transaction

        if (type == TransactionType.DEPOSIT) {
            // Check if player has enough source funds
            val sourceAmount = getSourceAmount(player, definition)
            if (sourceAmount < amount) {
                player.sendMiniWithResolvers(
                    txConfig.addErrorMessage,
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", decimalAmount.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("balance", balance.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("limit", limit.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }
            // Check if deposit would exceed limit
            val newBalance = balance.add(decimalAmount)
            if (newBalance > limit) {
                player.sendMiniWithResolvers(
                    txConfig.addErrorMessage,
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", decimalAmount.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("balance", balance.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("limit", limit.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }
            // Execute deposit
            artifact.addBalance(player.uniqueId, decimalAmount)
            // Execute command if configured
            if (txConfig.addCommand.isNotBlank()) {
                val cmd = txConfig.addCommand
                    .replace("{amount}", amount.toString())
                    .replace("{player}", player.name)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            }
            player.sendMiniWithResolvers(
                txConfig.addSuccessMessage,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", decimalAmount.toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("new_balance", artifact.getBalance(player.uniqueId).toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("limit", limit.toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
            )
        } else {
            // WITHDRAW
            if (balance < decimalAmount) {
                player.sendMiniWithResolvers(
                    txConfig.removeErrorMessage,
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", decimalAmount.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("balance", balance.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("limit", limit.toPlainString()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }
            artifact.removeBalance(player.uniqueId, decimalAmount)
            // Execute command if configured
            if (txConfig.removeCommand.isNotBlank()) {
                val cmd = txConfig.removeCommand
                    .replace("{amount}", amount.toString())
                    .replace("{player}", player.name)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            }
            player.sendMiniWithResolvers(
                txConfig.removeSuccessMessage,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", decimalAmount.toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("new_balance", artifact.getBalance(player.uniqueId).toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("limit", limit.toPlainString()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("prefix", definition.prefix)
            )
        }
        // Refresh menu if open
        if (player.openInventory.topInventory.holder is NumericalStorageMenu) {
            (player.openInventory.topInventory.holder as NumericalStorageMenu).render()
        }
    }
}
