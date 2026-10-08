package btcrenaud.numericalstorage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The admin replies are fields of the definition: their defaults are what a fresh entry shows, so they are a contract. */
class AdminCommandMessagesTest {

    private val defaults = AdminCommandMessages()

    private val everyReply = mapOf(
        "resetMessage" to defaults.resetMessage,
        "levelSetMessage" to defaults.levelSetMessage,
        "addMessage" to defaults.addMessage,
        "removeMessage" to defaults.removeMessage,
        "noMenuMessage" to defaults.noMenuMessage,
        "invalidAmountMessage" to defaults.invalidAmountMessage,
        "noArtifactMessage" to defaults.noArtifactMessage,
        "noActiveProfileMessage" to defaults.noActiveProfileMessage,
    )

    @Test
    fun `every default reply starts with the engine header`() {
        everyReply.forEach { (field, message) ->
            assertTrue(message.startsWith("<red><bold>Typewriter »<reset>"), "$field does not start with the header: $message")
        }
    }

    @Test
    fun `the invalid amount reply shows the value that was typed`() {
        assertTrue("<amount>" in defaults.invalidAmountMessage)
    }

    @Test
    fun `the missing artifact reply names the storage`() {
        assertTrue("<storage>" in defaults.noArtifactMessage)
    }

    @Test
    fun `every reply is a separate field with its own text`() {
        assertEquals(everyReply.size, everyReply.values.toSet().size)
    }
}
