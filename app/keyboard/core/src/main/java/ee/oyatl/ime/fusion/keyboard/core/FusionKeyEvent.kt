package ee.oyatl.ime.fusion.keyboard.core

sealed interface FusionKeyEvent {
    val keyCode: Int
    val shiftState: KeyboardState.Shift
    val altGrState: Boolean
    val source: KeyEventSource
    val time: Long

    data class Press(
        override val keyCode: Int,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent

    data class Release(
        override val keyCode: Int,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent

    data class LongPress(
        override val keyCode: Int,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent

    data class Flick(
        override val keyCode: Int,
        val direction: FlickDirection,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent
}