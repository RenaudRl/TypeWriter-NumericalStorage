package btcrenaud.numericalstorage.entries.action

import btcrenaud.gui.api.GuiText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

/** How the storage menus turn the title an author wrote into the title a player sees. */
internal object NumericalStorageMenuTitle {

    /**
     * The title TEMPLATE of a menu, placeholders unresolved: OmniGUI resolves them for the viewer, as components, so that
     * a value a player controls (a name holding `<shift:-400>`) cannot shift the title.
     *
     * [editorTitle] is the field the web editor writes the composed title into; when it is not empty it wins. Otherwise
     * [definitionTitle] (the title of the storage definition) is used, so pages saved before the field existed show
     * what they always showed.
     */
    fun template(editorTitle: String, definitionTitle: String): String = editorTitle.ifEmpty { definitionTitle }

    /** [template] as the component of the menu; a template MiniMessage cannot read is shown as white text with `&` read as `§`. */
    fun component(template: String, viewer: Player): Component =
        try {
            GuiText.parse(template, viewer)
        } catch (_: Exception) {
            MiniMessage.miniMessage().deserialize("<white>${template.replace("&", "§")}")
        }
}
