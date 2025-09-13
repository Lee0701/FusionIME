package ee.oyatl.ime.fusion

import java.time.LocalDate

enum class Feature(
    val availableFrom: LocalDate
) {
    BigramHanjaConverter(LocalDate.of(2025, 9, 11)),
    ;

    val availableInPaidVersion: Boolean get() =
        LocalDate.now().isAfter(availableFrom)
    val availableInFreeVersion: Boolean get() =
        LocalDate.now().isAfter(availableFrom.plusMonths(1))
}