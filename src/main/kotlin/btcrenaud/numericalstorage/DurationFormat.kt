package btcrenaud.numericalstorage

import java.time.Duration

/**
 * How a duration is written to a player. Each unit is a template where [VALUE_TAG] stands for the amount;
 * [separator] goes between the units that are shown.
 */
internal data class DurationFormat(
    val days: String,
    val hours: String,
    val minutes: String,
    val seconds: String,
    val separator: String,
) {
    companion object {
        const val VALUE_TAG = "{value}"
    }
}

/**
 * Writes the duration with its non-zero units from days to seconds. Seconds are the last resort, so a duration
 * shorter than one second reads as zero seconds rather than as an empty string.
 */
internal fun Duration.asReadable(format: DurationFormat): String {
    val shown = listOf(
        format.days to toDays(),
        format.hours to toHoursPart().toLong(),
        format.minutes to toMinutesPart().toLong(),
        format.seconds to toSecondsPart().toLong(),
    ).filter { (_, amount) -> amount > 0 }
        .ifEmpty { listOf(format.seconds to 0L) }
    return shown.joinToString(format.separator) { (template, amount) ->
        template.replace(DurationFormat.VALUE_TAG, amount.toString())
    }
}
