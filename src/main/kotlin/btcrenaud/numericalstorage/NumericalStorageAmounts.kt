package btcrenaud.numericalstorage

import java.math.BigDecimal

/** Single validation boundary for values received from commands, dialogs and GUI actions. */
fun String.toFinitePositiveAmountOrNull(): BigDecimal? =
    toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }

fun String.toFinitePositiveDoubleOrNull(): Double? =
    toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }

fun Double.toFinitePositiveAmountOrNull(): BigDecimal? =
    takeIf { isFinite() && it > 0.0 }?.let(BigDecimal::valueOf)
