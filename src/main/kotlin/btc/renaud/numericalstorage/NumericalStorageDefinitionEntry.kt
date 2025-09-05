package btc.renaud.numericalstorage

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.engine.paper.entry.ManifestEntry

@Entry(
    "numericalstorage_definition",
    "Defines a configurable numerical storage",
    Colors.BLUE,
    "fa6-solid:piggy-bank"
)
@Tags("numericalstorage", "definition")
class NumericalStorageDefinitionEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Display name shown to players.")
    val displayName: String = "",
    @Help("Prefix used in messages.")
    val prefix: String = "",
    @Help("Artifact storing player data")
    val artifact: Ref<PlayerNumericalStorageArtifactEntry> = emptyRef(),
    @Help("Use classic leveling progression starting at level 1.(Experience system)")
    val classicLeveling: Boolean = false,
    @Help("Configured levels. Messages support placeholders: {limit}, {amount}, {new_balance}, {prefix}, {level}")
    val levels: List<BankLevel> = emptyList()
) : ManifestEntry
