package btc.renaud.numericalstorage

import com.typewritermc.engine.paper.utils.asMini
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import java.math.BigDecimal

object NumericalStorageDialogService {

    fun openAmountDialog(
        player: Player,
        config: DialogConfig,
        onAmount: (Double) -> Unit
    ) {
        val inputKey = "amount_input"
        val inputs = listOf(
            DialogInput.text(
                inputKey,
                config.inputWidth,
                Component.text(config.inputLabel),
                true,
                config.inputPlaceholder,
                config.maxLength,
                null
            )
        )


        val submitAction = ActionButton.builder(Component.text(config.submitButton))
            .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick({ result, _ ->
                val text = result.getText(inputKey) ?: ""
                val amount = text.toDoubleOrNull()
                
                if (amount != null && amount > 0) {
                    onAmount(amount)
                    // Note: Success message is now handled by the transaction logic
                } else {
                    player.sendMessage(config.invalidInputMessage.asMini())
                }
            }, ClickCallback.Options.builder().build()))
            .build()

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(DialogBase.builder(Component.text(config.title))
                    .inputs(inputs)
                    .build())
                .type(DialogType.multiAction(listOf(submitAction), null, 1))
        }

        player.showDialog(dialog)
    }
}
