package btcrenaud.numericalstorage.entries.action

import btcrenaud.gui.GuiType
import btcrenaud.gui.InventorySize
import btcrenaud.gui.LayoutData
import btcrenaud.gui.api.LayoutParser
import btcrenaud.gui.api.MenuDefinition
import btcrenaud.gui.api.MenuLayout
import btcrenaud.gui.services.MenuSessionService
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import btcrenaud.numericalstorage.NumericalStorageDefinitionEntry
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Default
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import btcrenaud.numericalstorage.NumericalStorageCoroutines

@Entry(
    "numericalstorage_open_menu",
    "Opens a Numerical Storage menu (triggerable)",
    Colors.BLUE,
    "fa6-solid:piggy-bank"
)
@Tags("numericalstorage", "menu", "open", "gui_menu")
class NumericalStorageOpenMenuEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("The storage definition to open.")
    val definition: Ref<NumericalStorageDefinitionEntry> = emptyRef(),
    @Help("Layout pool for the menu.")
    val layoutPool: List<LayoutData> = emptyList(),
    @Help("The ID of the main layout to display from the pool.")
    val mainLayoutId: String = "",
    @Help("Full title of the menu. The web editor writes the title it composes (theme layers, glyphs) here. When it is not empty it is used instead of the title of the storage definition; leave it empty to keep that one.")
    @Default("\"\"") @Placeholder @Colored
    val title: String = "",
    @Help("Triggerable entries after this entry.")
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Criteria to check before triggering.")
    override val criteria: List<Criteria> = emptyList(),
    @Help("Modifiers to apply when triggered.")
    override val modifiers: List<Modifier> = emptyList(),
) : ActionEntry {

    override fun ActionTrigger.execute() {
        val def = definition.get() ?: return

        NumericalStorageCoroutines.launch {
            def.artifact.get()?.preload()
            NumericalStorageCoroutines.onPlayerThread(player) {
            val ctx = context {}
            val rawTitle = NumericalStorageMenuTitle.template(title, def.menu.title)
            val componentTitle = NumericalStorageMenuTitle.component(rawTitle, player)
            val rows = def.menu.rows.coerceIn(1, 6)
            val size = InventorySize.entries.getOrNull(rows - 1) ?: InventorySize.SIZE_54

            val pool = layoutPool.filterNotNull().associateBy { it.id }
            val baseLayout: MenuLayout = if (mainLayoutId.isNotBlank() && pool.containsKey(mainLayoutId)) {
                LayoutParser.parse(player, ctx, GuiType.CUSTOM, size.slots, pool, pool[mainLayoutId]!!)
            } else {
                btcrenaud.gui.api.EmptyLayout
            }

            val resolvedLayout = NumericalStorageButtonResolverLayout(
                player = player,
                definition = def,
                inner = baseLayout,
            )

            val menuDef = MenuDefinition(
                id = "numstorage_${def.id}_${player.uniqueId}",
                type = GuiType.CUSTOM,
                title = componentTitle,
                rawTitle = rawTitle,
                size = size,
                layout = resolvedLayout,
            )

            MenuSessionService.register(player, menuDef)
            }
        }
    }
}
