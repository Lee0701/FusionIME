package ee.oyatl.ime.fusion

import java.time.LocalDate

enum class Feature(
    val availableFrom: LocalDate
) {
    BigramHanjaConverter(LocalDate.of(2025, 9, 11)),
    MozcCandidateHeight(LocalDate.of(2025, 9, 25)),
    NumberRow(LocalDate.of(2026, 3, 15)),
    SplitKeyboard(LocalDate.of(2026, 3, 15)),
    TouchMode(LocalDate.of(2026, 4, 16)),
    CursorKeys(LocalDate.of(2026, 8, 3)),
    ;

    val availableInPaidVersion: Boolean get() =
        LocalDate.now().isAfter(availableFrom)
    val availableInFreeVersion: Boolean get() =
        LocalDate.now().isAfter(availableFrom.plusMonths(autoUnlockMonths))
    val availableInCurrentVersion: Boolean get() =
        if(paidVersion) availableInPaidVersion
        else availableInFreeVersion

    companion object {
        val paidVersion: Boolean = true
        val autoUnlockMonths: Long = 6
    }
}