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
    val availableInCurrentVersion: Boolean get() =
        if(paidVersion) availableInPaidVersion
        else availableInFreeVersion

    companion object {
        @Suppress("SENSELESS_COMPARISON")
        val paidVersion: Boolean = BuildConfig.IS_PAID
    }
}