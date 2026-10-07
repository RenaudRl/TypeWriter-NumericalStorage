package btcrenaud.numericalstorage

import com.typewritermc.engine.paper.snippets.snippet

/*
 * Texts that belong to no definition, so they cannot be fields of one. They are Typewriter snippets: each is
 * written to `snippets.yml` with its default the first time it is read and can be edited there, then reloaded.
 * They are read on use, never cached here. Keep this file free of logic: it is not loadable without a server.
 */

internal val playerOnlyMessage: String by snippet(
    "numericalstorage.command.player_only",
    "<red>Only players can use this command.",
    "Sent to a non-player that runs a menu button command (numstorage tx / upgrade / back_main). MiniMessage.",
)

internal val interestDisabledText: String by snippet(
    "numericalstorage.placeholder.interest_disabled",
    "Disabled",
    "Value of the interest_cooldown placeholder when the definition pays no interest.",
)

internal val interestErrorText: String by snippet(
    "numericalstorage.placeholder.interest_error",
    "Error",
    "Value of the interest_cooldown placeholder when the next interest time cannot be computed.",
)

internal val unlimitedCapacityText: String by snippet(
    "numericalstorage.placeholder.capacity_unlimited",
    "∞",
    "Value of the capacity placeholder when the level has no limit.",
)

private val durationDaysFormat: String by snippet(
    "numericalstorage.duration.days",
    "{value}j",
    "Days in the interest_cooldown placeholder. {value} is the amount.",
)

private val durationHoursFormat: String by snippet(
    "numericalstorage.duration.hours",
    "{value}h",
    "Hours in the interest_cooldown placeholder. {value} is the amount.",
)

private val durationMinutesFormat: String by snippet(
    "numericalstorage.duration.minutes",
    "{value}m",
    "Minutes in the interest_cooldown placeholder. {value} is the amount.",
)

private val durationSecondsFormat: String by snippet(
    "numericalstorage.duration.seconds",
    "{value}s",
    "Seconds in the interest_cooldown placeholder. {value} is the amount.",
)

private val durationSeparator: String by snippet(
    "numericalstorage.duration.separator",
    " ",
    "Written between the units of the interest_cooldown placeholder.",
)

internal fun durationFormat() = DurationFormat(
    days = durationDaysFormat,
    hours = durationHoursFormat,
    minutes = durationMinutesFormat,
    seconds = durationSecondsFormat,
    separator = durationSeparator,
)
