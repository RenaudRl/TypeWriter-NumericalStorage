package btcrenaud.numericalstorage.entries.action

import btcrenaud.gui.api.GenericButtonResolverLayout
import btcrenaud.gui.api.GuiSlot
import btcrenaud.gui.api.GuiSlotInteraction
import btcrenaud.gui.api.InteractionType
import btcrenaud.gui.api.MenuLayout
import btcrenaud.gui.api.Viewport
import btcrenaud.gui.services.MenuSessionService
import btcrenaud.numericalstorage.AmountType
import btcrenaud.numericalstorage.NumericalStorageDefinitionEntry
import btcrenaud.numericalstorage.PlayerNumericalStorageArtifactEntry
import btcrenaud.numericalstorage.TransactionType
import btcrenaud.numericalstorage.buildItem
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.utils.asMini
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import java.math.BigDecimal

/**
 * Resolves [NumericalStorageButtonType] tagged placeholders into dynamic GUI slots.
 * Prefix: "numericalstorage_button:"
 *
 * Tag format: "numericalstorage_button:TYPE[:param]"
 *   - TYPE = enum name (DEPOSIT_FIXED, DEPOSIT_PERCENT, ...)
 *   - param (optional) = amount for disambiguation (e.g. "DEPOSIT_PERCENT:50")
 *     When omitted, the first matching transaction button is used.
 */
class NumericalStorageButtonResolverLayout(
    private val player: Player,
    private val definition: NumericalStorageDefinitionEntry,
    inner: MenuLayout,
    override val id: String? = null,
) : MenuLayout {

    private val artifact = definition.artifact.get()

    private val delegate = GenericButtonResolverLayout(
        inner = inner,
        prefix = "numericalstorage_button:",
        resolver = { type, p, slot -> resolveButtonType(type, p, slot) },
        id = id,
    )

    override val innerLayout: MenuLayout?
        get() = delegate.innerLayout

    override fun getSlots(
        session: MenuSessionService.ActiveSession,
        viewport: Viewport,
    ): List<GuiSlot> =
        delegate.getSlots(session, viewport)

    override val virtualWidth: Int
        get() = delegate.virtualWidth

    override val virtualHeight: Int
        get() = delegate.virtualHeight

    /**
     * Parse TYPE[:param] and dispatch.
     * The param is the amount (as double) used to match the right transaction button
     * when multiple buttons share the same type.
     */
    private fun resolveButtonType(type: String, p: Player, slot: GuiSlot): GuiSlot? {
        val colonIdx = type.indexOf(':')
        val enumTypeName = if (colonIdx >= 0) type.substring(0, colonIdx) else type
        val param = if (colonIdx >= 0) type.substring(colonIdx + 1) else null

        val enumType = try {
            NumericalStorageButtonType.valueOf(enumTypeName)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return when (enumType) {
            NumericalStorageButtonType.DEPOSIT_FIXED -> resolveDepositFixedButton(p, slot, param)
            NumericalStorageButtonType.DEPOSIT_PERCENT -> resolveDepositPercentButton(p, slot, param)
            NumericalStorageButtonType.DEPOSIT_CUSTOM -> resolveDepositCustomButton(p, slot)
            NumericalStorageButtonType.DEPOSIT_ALL -> resolveDepositAllButton(p, slot)
            NumericalStorageButtonType.DEPOSIT_SUBMENU -> resolveDepositSubmenuButton(p, slot, param)
            NumericalStorageButtonType.WITHDRAW_FIXED -> resolveWithdrawFixedButton(p, slot, param)
            NumericalStorageButtonType.WITHDRAW_PERCENT -> resolveWithdrawPercentButton(p, slot, param)
            NumericalStorageButtonType.WITHDRAW_CUSTOM -> resolveWithdrawCustomButton(p, slot)
            NumericalStorageButtonType.WITHDRAW_ALL -> resolveWithdrawAllButton(p, slot)
            NumericalStorageButtonType.WITHDRAW_SUBMENU -> resolveWithdrawSubmenuButton(p, slot, param)
            NumericalStorageButtonType.TRANSFER_FIXED -> resolveTransferFixedButton(p, slot, param)
            NumericalStorageButtonType.TRANSFER_PERCENT -> resolveTransferPercentButton(p, slot, param)
            NumericalStorageButtonType.TRANSFER_ALL -> resolveTransferAllButton(p, slot)
            NumericalStorageButtonType.UPGRADE_LEVEL -> resolveUpgradeLevelButton(p, slot)
            NumericalStorageButtonType.NAV_CLOSE -> resolveCloseButton(p, slot)
            NumericalStorageButtonType.SUB_MENU -> resolveGenericSubmenuButton(p, slot, param)
        }
    }

    private fun resolveDepositFixedButton(p: Player, slot: GuiSlot, amountStr: String?): GuiSlot {
        val targetAmount = amountStr?.toDoubleOrNull()
        val btn = if (targetAmount != null) {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.DEPOSIT && it.amountType == AmountType.FIXED && it.amount == targetAmount
            }
        } else {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.DEPOSIT && it.amountType == AmountType.FIXED
            }
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.depositLabel.parsePlaceholders(p)
        val effectiveAmount = btn?.amount ?: targetAmount ?: 0.0
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.LIME_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} deposit_fixed:${java.math.BigDecimal.valueOf(effectiveAmount).toPlainString()}")
                )
            )
        )
    }

    private fun resolveDepositPercentButton(p: Player, slot: GuiSlot, amountStr: String?): GuiSlot {
        val targetAmount = amountStr?.toDoubleOrNull()
        val btn = if (targetAmount != null) {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.DEPOSIT && it.amountType == AmountType.PERCENTAGE && it.amount == targetAmount
            }
        } else {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.DEPOSIT && it.amountType == AmountType.PERCENTAGE
            }
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.depositPercentLabel.parsePlaceholders(p)
        val effectivePercent = btn?.amount ?: targetAmount ?: 0.0
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.GREEN_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} deposit_percent:$effectivePercent")
                )
            )
        )
    }

    private fun resolveDepositCustomButton(p: Player, slot: GuiSlot): GuiSlot {
        val btn = definition.menu.transactionButtons.firstOrNull {
            it.type == TransactionType.DEPOSIT && it.amountType == AmountType.CUSTOM
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.depositCustomLabel.parsePlaceholders(p)
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.YELLOW_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} deposit_custom")
                )
            )
        )
    }

    private fun resolveWithdrawFixedButton(p: Player, slot: GuiSlot, amountStr: String?): GuiSlot {
        val targetAmount = amountStr?.toDoubleOrNull()
        val btn = if (targetAmount != null) {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.WITHDRAW && it.amountType == AmountType.FIXED && it.amount == targetAmount
            }
        } else {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.WITHDRAW && it.amountType == AmountType.FIXED
            }
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.withdrawLabel.parsePlaceholders(p)
        val effectiveAmount = btn?.amount ?: targetAmount ?: 0.0
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.RED_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} withdraw_fixed:${java.math.BigDecimal.valueOf(effectiveAmount).toPlainString()}")
                )
            )
        )
    }

    private fun resolveWithdrawPercentButton(p: Player, slot: GuiSlot, amountStr: String?): GuiSlot {
        val targetAmount = amountStr?.toDoubleOrNull()
        val btn = if (targetAmount != null) {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.WITHDRAW && it.amountType == AmountType.PERCENTAGE && it.amount == targetAmount
            }
        } else {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.WITHDRAW && it.amountType == AmountType.PERCENTAGE
            }
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.withdrawPercentLabel.parsePlaceholders(p)
        val effectivePercent = btn?.amount ?: targetAmount ?: 0.0
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.ORANGE_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} withdraw_percent:$effectivePercent")
                )
            )
        )
    }

    private fun resolveWithdrawCustomButton(p: Player, slot: GuiSlot): GuiSlot {
        val btn = definition.menu.transactionButtons.firstOrNull {
            it.type == TransactionType.WITHDRAW && it.amountType == AmountType.CUSTOM
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.withdrawCustomLabel.parsePlaceholders(p)
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.PINK_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} withdraw_custom")
                )
            )
        )
    }

    private fun resolveDepositAllButton(p: Player, slot: GuiSlot): GuiSlot {
        val label = definition.menu.depositAllLabel.parsePlaceholders(p)
        return slot.copy(
            item = buildFallbackItem(label, Material.LIME_DYE),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} deposit_all")
                )
            )
        )
    }

    private fun resolveWithdrawAllButton(p: Player, slot: GuiSlot): GuiSlot {
        val label = definition.menu.withdrawAllLabel.parsePlaceholders(p)
        return slot.copy(
            item = buildFallbackItem(label, Material.RED_DYE),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} withdraw_all")
                )
            )
        )
    }

    /**
     * Opens a deposit sub-menu identified by [subMenuId].
     * The param is the sub-menu ID (e.g. "deposit_options").
     */
    private fun resolveDepositSubmenuButton(p: Player, slot: GuiSlot, subMenuId: String?): GuiSlot {
        val id = subMenuId ?: return slot
        val label = definition.menu.depositSubmenuLabel.parsePlaceholders(p)
        return slot.copy(
            item = buildFallbackItem(label, Material.GREEN_DYE),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} submenu_deposit:$id")
                )
            )
        )
    }

    /**
     * Opens a withdraw sub-menu identified by [subMenuId].
     */
    private fun resolveWithdrawSubmenuButton(p: Player, slot: GuiSlot, subMenuId: String?): GuiSlot {
        val id = subMenuId ?: return slot
        val label = definition.menu.withdrawSubmenuLabel.parsePlaceholders(p)
        return slot.copy(
            item = buildFallbackItem(label, Material.ORANGE_DYE),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} submenu_withdraw:$id")
                )
            )
        )
    }

    /**
     * Generic sub-menu button: opens any sub-menu by its ID.
     * Format: SUB_MENU:<subMenuId>
     */
    private fun resolveGenericSubmenuButton(p: Player, slot: GuiSlot, subMenuId: String?): GuiSlot {
        val id = subMenuId ?: return slot
        val subMenuConfig = definition.menu.subMenus[id] ?: return slot
        val label = subMenuConfig.title.parsePlaceholders(p)
        return slot.copy(
            item = buildFallbackItem(label, Material.BOOK),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} submenu_deposit:$id")
                )
            )
        )
    }

    private fun resolveTransferFixedButton(p: Player, slot: GuiSlot, amountStr: String?): GuiSlot {
        val targetAmount = amountStr?.toDoubleOrNull()
        val btn = if (targetAmount != null) {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.TRANSFER && it.amountType == AmountType.FIXED && it.amount == targetAmount
            }
        } else {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.TRANSFER && it.amountType == AmountType.FIXED
            }
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.transferLabel.parsePlaceholders(p)
        val effectiveAmount = btn?.amount ?: targetAmount ?: 0.0
        val targetId = btn?.transferTarget ?: ""
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.CYAN_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} transfer_fixed:$targetId:${java.math.BigDecimal.valueOf(effectiveAmount).toPlainString()}")
                )
            )
        )
    }

    private fun resolveTransferPercentButton(p: Player, slot: GuiSlot, amountStr: String?): GuiSlot {
        val targetPercent = amountStr?.toDoubleOrNull()
        val btn = if (targetPercent != null) {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.TRANSFER && it.amountType == AmountType.PERCENTAGE && it.amount == targetPercent
            }
        } else {
            definition.menu.transactionButtons.firstOrNull {
                it.type == TransactionType.TRANSFER && it.amountType == AmountType.PERCENTAGE
            }
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.transferPercentLabel.parsePlaceholders(p)
        val effectivePercent = btn?.amount ?: targetPercent ?: 0.0
        val targetId = btn?.transferTarget ?: ""
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.CYAN_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} transfer_percent:$targetId:$effectivePercent")
                )
            )
        )
    }

    private fun resolveTransferAllButton(p: Player, slot: GuiSlot): GuiSlot {
        val btn = definition.menu.transactionButtons.firstOrNull {
            it.type == TransactionType.TRANSFER && it.amountType == AmountType.ALL
        }
        val label = btn?.label?.parsePlaceholders(p)
            ?: definition.menu.transferAllLabel.parsePlaceholders(p)
        val targetId = btn?.transferTarget ?: ""
        return slot.copy(
            item = (btn?.template?.buildItem(p) ?: buildFallbackItem(label, Material.CYAN_DYE)),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage tx ${definition.id} transfer_all:$targetId")
                )
            )
        )
    }

    private fun resolveUpgradeLevelButton(p: Player, slot: GuiSlot): GuiSlot {
        if (artifact == null) return slot

        val level = artifact.getLevel(p.uniqueId)
        val maxLevel = definition.levels.size
        val btnConfig = definition.menu.levelButton

        if (level >= maxLevel) {
            return slot.copy(
                item = btnConfig.maxLevelTemplate.buildItem(p),
                interactions = emptyMap(),
            )
        }

        val cost = definition.levels[level - 1].limit
        val costString = BigDecimal.valueOf(cost).toPlainString()
        val nextLevelString = (level + 1).toString()
        val name = btnConfig.availableTemplate.name
            .replace("{cost}", costString)
            .replace("{level}", nextLevelString)
            .parsePlaceholders(p)
            .asMini()
            .decoration(TextDecoration.ITALIC, false)
        val lore = btnConfig.availableTemplate.lore.map {
            it.replace("{cost}", costString)
                .replace("{level}", nextLevelString)
                .parsePlaceholders(p)
                .asMini()
                .decoration(TextDecoration.ITALIC, false)
        }
        val item = btnConfig.availableTemplate.item.build(p).clone()
        val meta = item.itemMeta
        if (meta != null) {
            meta.displayName(name)
            meta.lore(lore)
            item.itemMeta = meta
        }
        return slot.copy(
            item = item,
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("numstorage upgrade ${definition.id}")
                )
            )
        )
    }

    private fun resolveCloseButton(p: Player, slot: GuiSlot): GuiSlot {
        val btn = definition.menu.closeButton
        return slot.copy(
            item = btn.template.buildItem(p, Material.BARRIER),
            interactions = mapOf(
                InteractionType.LEFT_CLICK to GuiSlotInteraction(
                    commands = listOf("gui:close")
                )
            )
        )
    }

    private fun buildFallbackItem(
        name: String,
        material: Material,
    ): org.bukkit.inventory.ItemStack {
        val stack = org.bukkit.inventory.ItemStack(material)
        val meta = stack.itemMeta
        if (meta != null) {
            meta.displayName(name.asMini())
            stack.itemMeta = meta
        }
        return stack
    }
}
