package btcrenaud.numericalstorage.entries.action

import btcrenaud.gui.GuiType
import btcrenaud.gui.InventorySize
import btcrenaud.gui.api.GuiSlot
import btcrenaud.gui.api.GuiSlotInteraction
import btcrenaud.gui.api.InteractionType
import btcrenaud.gui.api.MenuDefinition
import btcrenaud.gui.api.SimpleLayout
import btcrenaud.gui.services.MenuSessionService
import btcrenaud.numericalstorage.AmountType
import btcrenaud.numericalstorage.CriteriaMode
import btcrenaud.numericalstorage.NumericalStorageDefinitionEntry
import btcrenaud.numericalstorage.NumericalStorageDialogService
import btcrenaud.numericalstorage.PlayerNumericalStorageArtifactEntry
import btcrenaud.numericalstorage.TransactionButtonConfig
import btcrenaud.numericalstorage.TransactionMode
import btcrenaud.numericalstorage.TransactionType
import btcrenaud.numericalstorage.VaultEconomyProvider
import btcrenaud.numericalstorage.NumericalStorageCoroutines
import btcrenaud.numericalstorage.toFinitePositiveDoubleOrNull
import btcrenaud.numericalstorage.buildItem
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.ref
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.utils.asMini
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers as sendMiniWithResolversUnsafe
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.util.logging.Level
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * Handles all transaction logic dispatched from GUI button interactions.
 * Called by the command handler (numstorage tx ...) which is triggered by GUI slot clicks.
 */
@Singleton
class NumericalStorageTransactionHandler {

    private suspend fun Player.sendMiniWithResolvers(message: String, vararg resolvers: TagResolver) {
        NumericalStorageCoroutines.onPlayerThread(this) {
            sendMiniWithResolversUnsafe(message, *resolvers)
        }
    }

    /**
     * Handle a transaction request from a GUI button click.
     * Called via: "numstorage tx <action> <definitionId>"
     */
    fun handleTransaction(player: Player, action: String, definitionId: String) {
        NumericalStorageCoroutines.launch { handleTransactionAsync(player, action, definitionId) }
    }

    private suspend fun handleTransactionAsync(player: Player, action: String, definitionId: String) {
        val definition = findDefinitionById(definitionId) ?: return
        val artifact = definition.artifact.get() ?: return

        when {
            // ---- Deposit ----
            action == "deposit_all" -> handleDeposit(player, definition, artifact, AmountType.ALL)
            action == "deposit_custom" -> {
                NumericalStorageCoroutines.onPlayerThread(player) {
                    player.closeInventory()
                    NumericalStorageDialogService.openAmountDialog(player, definition.menu.customAmountDialog) { amount ->
                        NumericalStorageCoroutines.launch {
                            executeTransaction(player, definition, artifact, TransactionType.DEPOSIT, amount)
                            reopenMenu(player, definition)
                        }
                    }
                }
                return
            }
            action.startsWith("deposit_fixed:") -> {
                val amount = action.removePrefix("deposit_fixed:").toFinitePositiveDoubleOrNull() ?: return
                executeTransaction(player, definition, artifact, TransactionType.DEPOSIT, amount)
            }
            action.startsWith("deposit_percent:") -> {
                val percent = action.removePrefix("deposit_percent:").toFinitePositiveDoubleOrNull() ?: return
                val sourceAmount = getSourceAmount(player, definition)
                val hasSource = definition.transaction.transactionMode == TransactionMode.VAULT
                    || definition.transaction.amountPlaceholder.isNotBlank()
                if (hasSource && sourceAmount <= 0) {
                    player.sendMiniWithResolvers(
                        definition.transaction.noFundsMessage,
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
                val amount = sourceAmount * (percent / 100.0)
                executeTransaction(player, definition, artifact, TransactionType.DEPOSIT, amount)
            }

            // ---- Withdraw ----
            action == "withdraw_all" -> handleWithdraw(player, definition, artifact, AmountType.ALL)
            action == "withdraw_custom" -> {
                NumericalStorageCoroutines.onPlayerThread(player) {
                    player.closeInventory()
                    NumericalStorageDialogService.openAmountDialog(player, definition.menu.customAmountDialog) { amount ->
                        NumericalStorageCoroutines.launch {
                            executeTransaction(player, definition, artifact, TransactionType.WITHDRAW, amount)
                            reopenMenu(player, definition)
                        }
                    }
                }
                return
            }
            action.startsWith("withdraw_fixed:") -> {
                val amount = action.removePrefix("withdraw_fixed:").toFinitePositiveDoubleOrNull() ?: return
                executeTransaction(player, definition, artifact, TransactionType.WITHDRAW, amount)
            }
            action.startsWith("withdraw_percent:") -> {
                val percent = action.removePrefix("withdraw_percent:").toFinitePositiveDoubleOrNull() ?: return
                val storageBalance = artifact.getBalanceAsync(player.uniqueId, definition.profileMode).toDouble()
                if (storageBalance <= 0) {
                    player.sendMiniWithResolvers(
                        definition.transaction.noFundsMessage,
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
                val amount = storageBalance * (percent / 100.0)
                executeTransaction(player, definition, artifact, TransactionType.WITHDRAW, amount)
            }

            // ---- Transfer ----
            action.startsWith("transfer_fixed:") -> {
                val rest = action.removePrefix("transfer_fixed:")
                val colonIdx = rest.indexOf(':')
                if (colonIdx == -1) return
                val targetId = rest.substring(0, colonIdx)
                val amount = rest.substring(colonIdx + 1).toFinitePositiveDoubleOrNull() ?: return
                handleTransfer(player, definition, artifact, targetId, amount)
            }
            action.startsWith("transfer_percent:") -> {
                val rest = action.removePrefix("transfer_percent:")
                val colonIdx = rest.indexOf(':')
                if (colonIdx == -1) return
                val targetId = rest.substring(0, colonIdx)
                val percent = rest.substring(colonIdx + 1).toFinitePositiveDoubleOrNull() ?: return
                val sourceBalance = artifact.getBalanceAsync(player.uniqueId, definition.profileMode).toDouble()
                if (sourceBalance <= 0) {
                    player.sendMiniWithResolvers(
                        definition.menu.transferInsufficientMessage,
                        Placeholder.parsed("balance", artifact.getBalanceAsync(player.uniqueId, definition.profileMode).toPlainString()),
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
                val amount = sourceBalance * (percent / 100.0)
                handleTransfer(player, definition, artifact, targetId, amount)
            }
            action.startsWith("transfer_all:") -> {
                val targetId = action.removePrefix("transfer_all:")
                val sourceBalance = artifact.getBalanceAsync(player.uniqueId, definition.profileMode).toDouble()
                handleTransfer(player, definition, artifact, targetId, sourceBalance)
            }

            // ---- Other ----
            action == "upgrade" -> handleLevelUp(player, definition, artifact)
            action == "back_main" -> reopenMenu(player, definition)
            else -> {
                if (action.startsWith("submenu_deposit:")) {
                    val subMenuId = action.removePrefix("submenu_deposit:")
                    handleSubMenuAction(player, definition, artifact, TransactionType.DEPOSIT, subMenuId)
                    return
                }
                if (action.startsWith("submenu_withdraw:")) {
                    val subMenuId = action.removePrefix("submenu_withdraw:")
                    handleSubMenuAction(player, definition, artifact, TransactionType.WITHDRAW, subMenuId)
                    return
                }
                return
            }
        }

        // Reopen menu after transaction (submenu/custom handle it internally)
        reopenMenu(player, definition)
    }

    private suspend fun handleDeposit(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
        amountType: AmountType,
    ) {
        when (amountType) {
            AmountType.ALL -> {
                val sourceAmount = getSourceAmount(player, definition)
                val balance = artifact.getBalanceAsync(player.uniqueId, definition.profileMode)
                val level = artifact.getLevelAsync(player.uniqueId, definition.profileMode)
                val levelConfig = definition.levels.getOrNull(level - 1)
                val limit = levelConfig?.let { BigDecimal.valueOf(it.limit) } ?: BigDecimal.valueOf(Double.MAX_VALUE)
                val remainingCapacity = limit.subtract(balance)
                val maxDeposit = if (definition.transaction.amountPlaceholder.isNotBlank()) {
                    minOf(BigDecimal.valueOf(sourceAmount), remainingCapacity)
                } else {
                    remainingCapacity
                }
                executeTransaction(player, definition, artifact, TransactionType.DEPOSIT, maxDeposit.toDouble())
            }
            else -> {} // FIXED/PERCENTAGE/CUSTOM handled in handleTransaction directly
        }
    }

    private suspend fun handleWithdraw(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
        amountType: AmountType,
    ) {
        when (amountType) {
            AmountType.ALL -> {
                val balance = artifact.getBalanceAsync(player.uniqueId, definition.profileMode)
                executeTransaction(player, definition, artifact, TransactionType.WITHDRAW, balance.toDouble())
            }
            else -> {} // FIXED/PERCENTAGE/CUSTOM handled in handleTransaction directly
        }
    }

    private suspend fun handleLevelUp(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
    ) {
        val level = artifact.getLevelAsync(player.uniqueId, definition.profileMode)
        val maxLevel = definition.levels.size

        if (level >= maxLevel) {
            player.sendMiniWithResolvers(
                definition.menu.maxLevelMessage,
                Placeholder.parsed("prefix", definition.prefix)
            )
            return
        }

        val currentLevelConfig = definition.levels[level - 1]
        val nextLevelConfig = definition.levels[level]
        val balance = artifact.getBalanceAsync(player.uniqueId, definition.profileMode)

        val criteriaNotMet = nextLevelConfig.criteria.filter { criteria ->
            val currentValue = when (criteria.mode) {
                CriteriaMode.PLACEHOLDER -> NumericalStorageCoroutines.onPlayerThread(player) {
                    criteria.placeholder.parsePlaceholders(player).toDoubleOrNull()
                } ?: 0.0
                CriteriaMode.FACT -> NumericalStorageCoroutines.onPlayerThread(player) {
                    criteria.fact.get()?.readForPlayersGroup(player)?.value?.toDouble()
                } ?: 0.0
            }
            !criteria.isMet(currentValue)
        }

        if (criteriaNotMet.isNotEmpty()) {
            player.sendMiniWithResolvers(
                definition.menu.criteriaNotMetMessage,
                Placeholder.parsed("prefix", definition.prefix)
            )
            return
        }

        var internalDeduction = BigDecimal.ZERO
        if (nextLevelConfig.criteria.isEmpty()) {
            val legacyCost = BigDecimal.valueOf(currentLevelConfig.limit)
            if (balance < legacyCost) {
                player.sendMiniWithResolvers(
                    definition.menu.notEnoughFundsMessage,
                    Placeholder.parsed("cost", legacyCost.toPlainString()),
                    Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }
            internalDeduction = legacyCost
        } else {
            if (nextLevelConfig.criteria.any { !it.deduct(player) }) {
                player.sendMiniWithResolvers(
                    definition.menu.notEnoughFundsMessage,
                    Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }
            nextLevelConfig.criteria.forEach { criteria ->
                if (criteria.deductOnMet && criteria.mode == CriteriaMode.PLACEHOLDER) {
                    val internalPrefixes = listOf(
                        "%typewriter_ns_balance_${definition.id}%",
                        "<ns_balance:${definition.id}>"
                    )
                    if (internalPrefixes.any(criteria.placeholder::startsWith)) {
                        internalDeduction += BigDecimal.valueOf(criteria.requiredValue)
                    }
                }
            }
        }

        val nextLevelNumber = level + 1
        artifact.update { balances, levels, _ ->
            val key = artifact.storageKey(player.uniqueId, definition.profileMode)
            if (internalDeduction > BigDecimal.ZERO) balances[key] = (balances[key] ?: BigDecimal.ZERO).subtract(internalDeduction).max(BigDecimal.ZERO)
            levels[key] = nextLevelNumber
        }

        player.sendMiniWithResolvers(
            nextLevelConfig.levelUpMessage,
            Placeholder.parsed("limit", BigDecimal.valueOf(nextLevelConfig.limit).toPlainString()),
            Placeholder.parsed("prefix", definition.prefix),
            Placeholder.parsed("level", nextLevelNumber.toString())
        )

        nextLevelConfig.levelUpTriggers.forEach { triggerRef ->
            NumericalStorageCoroutines.onPlayerThread(player) {
                triggerRef.triggerFor(player, context())
            }
        }
    }

    private suspend fun executeTransaction(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
        type: TransactionType,
        amount: Double,
    ) {
        if (!amount.isFinite() || amount <= 0) {
            player.sendMiniWithResolvers(
                definition.transaction.invalidAmountMessage,
                Placeholder.parsed("prefix", definition.prefix)
            )
            return
        }

        val decimalAmount = BigDecimal.valueOf(amount)
        val balance = artifact.getBalanceAsync(player.uniqueId, definition.profileMode)
        val level = artifact.getLevelAsync(player.uniqueId, definition.profileMode)
        val levelConfig = definition.levels.getOrNull(level - 1)
        val limit = levelConfig?.let { BigDecimal.valueOf(it.limit) } ?: BigDecimal.valueOf(Double.MAX_VALUE)
        val txConfig = definition.transaction
        val isVault = txConfig.transactionMode == TransactionMode.VAULT
        val hasExternalSource = !isVault && txConfig.amountPlaceholder.isNotBlank()

        if (type == TransactionType.DEPOSIT) {
            // --- Source funds check ---
            if (isVault) {
                if (NumericalStorageCoroutines.onPlayerThread(player) { VaultEconomyProvider.isAvailable } != true) {
                    player.sendMiniWithResolvers(
                        txConfig.vaultUnavailableMessage,
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
                val sourceBalance = NumericalStorageCoroutines.onPlayerThread(player) {
                    VaultEconomyProvider.getBalance(player)
                } ?: 0.0
                if (sourceBalance < amount) {
                    player.sendMiniWithResolvers(
                        txConfig.vaultInsufficientMessage,
                        Placeholder.parsed("amount", decimalAmount.toPlainString()),
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
            } else if (hasExternalSource) {
                val sourceAmount = getSourceAmount(player, definition)
                if (sourceAmount < amount) {
                    player.sendMiniWithResolvers(
                        txConfig.noFundsMessage,
                        Placeholder.parsed("amount", decimalAmount.toPlainString()),
                        Placeholder.parsed("balance", balance.toPlainString()),
                        Placeholder.parsed("limit", limit.toPlainString()),
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
            }

            // --- Capacity check ---
            val newBalance = balance.add(decimalAmount)
            if (newBalance > limit) {
                player.sendMiniWithResolvers(
                    txConfig.addErrorMessage,
                    Placeholder.parsed("amount", decimalAmount.toPlainString()),
                    Placeholder.parsed("balance", balance.toPlainString()),
                    Placeholder.parsed("limit", limit.toPlainString()),
                    Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }

            // --- Deduct from source ---
            if (isVault) {
                val withdrawn = NumericalStorageCoroutines.onPlayerThread(player) {
                    VaultEconomyProvider.isAvailable && VaultEconomyProvider.withdrawPlayer(player, amount)
                } == true
                if (!withdrawn) {
                    player.sendMiniWithResolvers(
                        txConfig.vaultInsufficientMessage,
                        Placeholder.parsed("amount", decimalAmount.toPlainString()),
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
            } else if (txConfig.addCommand.isNotBlank()) {
                val cmd = txConfig.addCommand
                    .replace("{amount}", decimalAmount.toPlainString())
                    .replace("{player}", player.name)
                val commandSucceeded = NumericalStorageCoroutines.onGlobalThread {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                } == true
                if (!commandSucceeded) {
                    player.sendMiniWithResolvers(
                        txConfig.addErrorMessage,
                        Placeholder.parsed("amount", decimalAmount.toPlainString()),
                        Placeholder.parsed("balance", balance.toPlainString()),
                        Placeholder.parsed("limit", limit.toPlainString()),
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
            }

            // --- Add to storage ---
            val newSnapshot = try {
                artifact.addBalance(player.uniqueId, decimalAmount, definition.profileMode)
            } catch (error: Throwable) {
                // Vault has already been debited; compensate immediately if the
                // persistent artifact cannot be written.
                if (isVault) {
                    NumericalStorageCoroutines.onPlayerThread(player) {
                        VaultEconomyProvider.depositPlayer(player, amount)
                    }
                }
                Bukkit.getLogger().log(Level.SEVERE, "NumericalStorage deposit failed for ${player.name}", error)
                player.sendMiniWithResolvers(
                    txConfig.addErrorMessage,
                    Placeholder.parsed("amount", decimalAmount.toPlainString()),
                    Placeholder.parsed("balance", balance.toPlainString()),
                    Placeholder.parsed("limit", limit.toPlainString()),
                    Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }

            player.sendMiniWithResolvers(
                txConfig.addSuccessMessage,
                Placeholder.parsed("amount", decimalAmount.toPlainString()),
                Placeholder.parsed("new_balance", newSnapshot.balance(artifact.storageKey(player.uniqueId, definition.profileMode)).toPlainString()),
                Placeholder.parsed("limit", limit.toPlainString()),
                Placeholder.parsed("prefix", definition.prefix)
            )
        } else {
            // --- Storage balance check ---
            if (balance < decimalAmount) {
                player.sendMiniWithResolvers(
                    txConfig.removeErrorMessage,
                    Placeholder.parsed("amount", decimalAmount.toPlainString()),
                    Placeholder.parsed("balance", balance.toPlainString()),
                    Placeholder.parsed("limit", limit.toPlainString()),
                    Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }

            // --- Add to source / execute external command first ---
            if (isVault) {
                val deposited = NumericalStorageCoroutines.onPlayerThread(player) {
                    VaultEconomyProvider.isAvailable && VaultEconomyProvider.depositPlayer(player, amount)
                } == true
                if (!deposited) {
                    player.sendMiniWithResolvers(
                        txConfig.vaultUnavailableMessage,
                        Placeholder.parsed("amount", decimalAmount.toPlainString()),
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
            } else if (txConfig.removeCommand.isNotBlank()) {
                val cmd = txConfig.removeCommand
                    .replace("{amount}", decimalAmount.toPlainString())
                    .replace("{player}", player.name)
                val commandSucceeded = NumericalStorageCoroutines.onGlobalThread {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                } == true
                if (!commandSucceeded) {
                    player.sendMiniWithResolvers(
                        txConfig.removeErrorMessage,
                        Placeholder.parsed("amount", decimalAmount.toPlainString()),
                        Placeholder.parsed("balance", balance.toPlainString()),
                        Placeholder.parsed("limit", limit.toPlainString()),
                        Placeholder.parsed("prefix", definition.prefix)
                    )
                    return
                }
            }

            // --- Remove from storage ---
            val newSnapshot = try {
                artifact.removeBalance(player.uniqueId, decimalAmount, definition.profileMode)
            } catch (error: Throwable) {
                // Vault credit succeeded; roll it back if the storage write
                // fails. Arbitrary commands cannot be generically reversed,
                // so they are executed only after a successful preflight.
                if (isVault) {
                    NumericalStorageCoroutines.onPlayerThread(player) {
                        VaultEconomyProvider.withdrawPlayer(player, amount)
                    }
                }
                Bukkit.getLogger().log(Level.SEVERE, "NumericalStorage withdrawal failed for ${player.name}", error)
                player.sendMiniWithResolvers(
                    txConfig.removeErrorMessage,
                    Placeholder.parsed("amount", decimalAmount.toPlainString()),
                    Placeholder.parsed("balance", balance.toPlainString()),
                    Placeholder.parsed("limit", limit.toPlainString()),
                    Placeholder.parsed("prefix", definition.prefix)
                )
                return
            }

            player.sendMiniWithResolvers(
                txConfig.removeSuccessMessage,
                Placeholder.parsed("amount", decimalAmount.toPlainString()),
                Placeholder.parsed("new_balance", newSnapshot.balance(artifact.storageKey(player.uniqueId, definition.profileMode)).toPlainString()),
                Placeholder.parsed("limit", limit.toPlainString()),
                Placeholder.parsed("prefix", definition.prefix)
            )
        }
    }

    private suspend fun getSourceAmount(player: Player, definition: NumericalStorageDefinitionEntry): Double {
        val txConfig = definition.transaction
        return when (txConfig.transactionMode) {
            TransactionMode.VAULT -> {
                NumericalStorageCoroutines.onPlayerThread(player) { VaultEconomyProvider.getBalance(player) } ?: 0.0
            }
            TransactionMode.INTERNAL -> {
                val placeholder = txConfig.amountPlaceholder
                if (placeholder.isBlank()) return 0.0
                NumericalStorageCoroutines.onPlayerThread(player) {
                    placeholder.parsePlaceholders(player).toDoubleOrNull() ?: 0.0
                } ?: 0.0
            }
        }
    }

    private fun findDefinitionById(id: String): NumericalStorageDefinitionEntry? {
        val all = Query.find<NumericalStorageDefinitionEntry>()
        for (def in all) {
            if (def.id == id) return def
        }
        return null
    }

    /**
     * Atomically transfers [amount] from the source [artifact] to the target definition's artifact.
     * Validates source balance and target capacity before executing either side.
     */
    private suspend fun handleTransfer(
        player: Player,
        sourceDef: NumericalStorageDefinitionEntry,
        sourceArtifact: PlayerNumericalStorageArtifactEntry,
        targetId: String,
        amount: Double,
    ) {
        if (!amount.isFinite() || amount <= 0) {
            player.sendMiniWithResolvers(
                sourceDef.transaction.invalidAmountMessage,
                Placeholder.parsed("prefix", sourceDef.prefix)
            )
            return
        }

        val targetDef = findDefinitionById(targetId)
        if (targetDef == null) {
            player.sendMiniWithResolvers(
                "<red>Target storage not found.",
                Placeholder.parsed("prefix", sourceDef.prefix)
            )
            return
        }
        val targetArtifact = targetDef.artifact.get()
        if (targetArtifact == null) {
            player.sendMiniWithResolvers(
                "<red>Target storage is not available.",
                Placeholder.parsed("prefix", sourceDef.prefix)
            )
            return
        }

        val decimalAmount = java.math.BigDecimal.valueOf(amount)
        val sourceBalance = sourceArtifact.getBalanceAsync(player.uniqueId, sourceDef.profileMode)

        // Validate source has enough
        if (sourceBalance < decimalAmount) {
            player.sendMiniWithResolvers(
                sourceDef.menu.transferInsufficientMessage,
                Placeholder.parsed("amount", decimalAmount.toPlainString()),
                Placeholder.parsed("balance", sourceBalance.toPlainString()),
                Placeholder.parsed("prefix", sourceDef.prefix)
            )
            return
        }

        // Validate target has capacity
        val targetLevel = targetArtifact.getLevelAsync(player.uniqueId, targetDef.profileMode)
        val targetLevelConfig = targetDef.levels.getOrNull(targetLevel - 1)
        val targetLimit = targetLevelConfig?.let { java.math.BigDecimal.valueOf(it.limit) }
            ?: java.math.BigDecimal.valueOf(Double.MAX_VALUE)
        val targetBalance = targetArtifact.getBalanceAsync(player.uniqueId, targetDef.profileMode)
        val targetNewBalance = targetBalance.add(decimalAmount)
        if (targetNewBalance > targetLimit) {
            player.sendMiniWithResolvers(
                sourceDef.menu.transferCapacityMessage,
                Placeholder.parsed("amount", decimalAmount.toPlainString()),
                Placeholder.parsed("limit", targetLimit.toPlainString()),
                Placeholder.parsed("prefix", sourceDef.prefix)
            )
            return
        }

        // Atomic: both operations
        val transfer = sourceArtifact.transferTo(
            targetArtifact,
            player.uniqueId,
            decimalAmount,
            sourceDef.profileMode,
            targetDef.profileMode,
        )
        if (transfer.isFailure) return
        val balances = transfer.getOrThrow()

        player.sendMiniWithResolvers(
            sourceDef.menu.transferSuccessMessage,
            Placeholder.parsed("amount", decimalAmount.toPlainString()),
            Placeholder.parsed("new_balance", balances.first.toPlainString()),
            Placeholder.parsed("target_balance", balances.second.toPlainString()),
            Placeholder.parsed("prefix", sourceDef.prefix)
        )
    }

    /**
     * Opens a sub-menu with the given [subMenuId] and [transactionType] (DEPOSIT or WITHDRAW).
     * The sub-menu provides a list of transaction buttons configured in the definition.
     */
    private suspend fun handleSubMenuAction(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
        transactionType: TransactionType,
        subMenuId: String,
    ) {
        if (definition.menu.subMenus[subMenuId] == null) return
        NumericalStorageCoroutines.onPlayerThread(player) {
            handleSubMenuActionOnPlayerThread(player, definition, artifact, transactionType, subMenuId)
        }
    }

    private fun handleSubMenuActionOnPlayerThread(
        player: Player,
        definition: NumericalStorageDefinitionEntry,
        artifact: PlayerNumericalStorageArtifactEntry,
        transactionType: TransactionType,
        subMenuId: String,
    ) {
        val subMenu = definition.menu.subMenus[subMenuId] ?: return
        val mm = MiniMessage.miniMessage()

        val rawTitle = subMenu.title.parsePlaceholders(player)
        val title = try {
            mm.deserialize(rawTitle)
        } catch (_: Exception) {
            mm.deserialize("<white>${rawTitle.replace("&", "§")}")
        }
        val rows = subMenu.rows.coerceIn(1, 6)
        val size = InventorySize.entries.getOrNull(rows - 1) ?: InventorySize.SIZE_54

        val slots = mutableListOf<GuiSlot>()

        // Transaction buttons from the sub-menu config
        for ((idx, btn) in subMenu.transactionButtons.withIndex()) {
            if (btn.slots.isEmpty() && idx >= size.slots) continue
            val slotIndex = btn.slots.firstOrNull() ?: idx
            val actionKey = when (btn.type) {
                TransactionType.DEPOSIT -> when (btn.amountType) {
                    AmountType.FIXED -> "deposit_fixed:${java.math.BigDecimal.valueOf(btn.amount).toPlainString()}"
                    AmountType.PERCENTAGE -> "deposit_percent:${btn.amount}"
                    AmountType.ALL -> "deposit_all"
                    AmountType.CUSTOM -> "deposit_custom"
                }
                TransactionType.WITHDRAW -> when (btn.amountType) {
                    AmountType.FIXED -> "withdraw_fixed:${java.math.BigDecimal.valueOf(btn.amount).toPlainString()}"
                    AmountType.PERCENTAGE -> "withdraw_percent:${btn.amount}"
                    AmountType.ALL -> "withdraw_all"
                    AmountType.CUSTOM -> "withdraw_custom"
                }
                TransactionType.TRANSFER -> when (btn.amountType) {
                    AmountType.FIXED -> "transfer_fixed:${btn.transferTarget}:${java.math.BigDecimal.valueOf(btn.amount).toPlainString()}"
                    AmountType.PERCENTAGE -> "transfer_percent:${btn.transferTarget}:${btn.amount}"
                    AmountType.ALL -> "transfer_all:${btn.transferTarget}"
                    AmountType.CUSTOM -> "transfer_all:${btn.transferTarget}"
                }
            }

            val label = btn.label.ifBlank { null }
                ?: when (btn.type) {
                    TransactionType.DEPOSIT -> when (btn.amountType) {
                        AmountType.FIXED -> "${definition.menu.depositLabel} ${btn.amount}"
                        AmountType.PERCENTAGE -> "${definition.menu.depositPercentLabel} ${btn.amount}%"
                        AmountType.ALL -> definition.menu.depositAllLabel
                        AmountType.CUSTOM -> definition.menu.depositCustomLabel
                    }
                    TransactionType.WITHDRAW -> when (btn.amountType) {
                        AmountType.FIXED -> "${definition.menu.withdrawLabel} ${btn.amount}"
                        AmountType.PERCENTAGE -> "${definition.menu.withdrawPercentLabel} ${btn.amount}%"
                        AmountType.ALL -> definition.menu.withdrawAllLabel
                        AmountType.CUSTOM -> definition.menu.withdrawCustomLabel
                    }
                    TransactionType.TRANSFER -> when (btn.amountType) {
                        AmountType.FIXED -> "${definition.menu.transferLabel} ${btn.amount}"
                        AmountType.PERCENTAGE -> "${definition.menu.transferPercentLabel} ${btn.amount}%"
                        AmountType.ALL -> definition.menu.transferAllLabel
                        AmountType.CUSTOM -> definition.menu.transferAllLabel
                    }
                }

            val item = btn.template.buildItem(player)
            val meta = item.itemMeta
            if (meta != null) {
                meta.displayName(label.parsePlaceholders(player).asMini().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                item.itemMeta = meta
            }

            slots.add(
                GuiSlot(
                    x = slotIndex % 9,
                    y = slotIndex / 9,
                    item = item,
                    interactions = mapOf(
                        InteractionType.LEFT_CLICK to GuiSlotInteraction(
                            commands = listOf("numstorage tx ${definition.id} $actionKey")
                        )
                    )
                )
            )
        }

        // Back button
        val backBtn = subMenu.backButton
        val backSlots = subMenu.backButtonSlots
        if (backSlots.isNotEmpty()) {
            val backSlotIndex = backSlots.first()
            val backItem = backBtn.buildItem(player, org.bukkit.Material.BARRIER)
            val backMeta = backItem.itemMeta
            if (backMeta != null) {
                backMeta.displayName(backBtn.name.parsePlaceholders(player).asMini().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                backItem.itemMeta = backMeta
            }
            slots.add(
                GuiSlot(
                    x = backSlotIndex % 9,
                    y = backSlotIndex / 9,
                    item = backItem,
                    interactions = mapOf(
                        InteractionType.LEFT_CLICK to GuiSlotInteraction(
                            commands = listOf("numstorage back_main ${definition.id}")
                        )
                    )
                )
            )
        }

        // Close button
        val closeBtn = subMenu.closeButton
        for (closeSlot in closeBtn.slots) {
            val closeItem = closeBtn.template.buildItem(player, org.bukkit.Material.BARRIER)
            slots.add(
                GuiSlot(
                    x = closeSlot % 9,
                    y = closeSlot / 9,
                    item = closeItem,
                    interactions = mapOf(
                        InteractionType.LEFT_CLICK to GuiSlotInteraction(
                            commands = listOf("gui:close")
                        )
                    )
                )
            )
        }

        // Fill background
        if (definition.menu.fillEnabled) {
            val fillItem = subMenu.fill.buildItem(player)
            for (s in 0 until size.slots) {
                if (slots.none { it.x + it.y * 9 == s }) {
                    slots.add(GuiSlot(x = s % 9, y = s / 9, item = fillItem.clone(), interactions = emptyMap()))
                }
            }
        }

        val layout = SimpleLayout(slots)
        val menuDef = MenuDefinition(
            id = "numstorage_sub_${definition.id}_${subMenuId}_${player.uniqueId}",
            type = GuiType.CUSTOM,
            title = title,
            rawTitle = rawTitle,
            size = size,
            layout = layout,
        )
        MenuSessionService.register(player, menuDef)
    }

    private suspend fun reopenMenu(player: Player, definition: NumericalStorageDefinitionEntry) {
        // Re-trigger the open menu entry to refresh the GUI
        val menuEntries = Query.find<NumericalStorageOpenMenuEntry>()
        for (me in menuEntries) {
            val meDef = me.definition.get()
            if (meDef != null && meDef.id == definition.id) {
                NumericalStorageCoroutines.onPlayerThread(player) {
                    me.ref().triggerFor(player, context())
                }
                return
            }
        }
        player.closeInventory()
    }
}
