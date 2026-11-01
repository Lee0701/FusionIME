package ee.oyatl.ime.fusion.keyboard.core

sealed interface FusionKeyEvent {
    val keyCode: Int
    val shiftState: KeyboardState.Shift
    val altGrState: Boolean
    val source: KeyEventSource
    val time: Long

    fun with(
        keyCode: Int = this.keyCode,
        shiftState: KeyboardState.Shift = this.shiftState,
        altGrState: Boolean = this.altGrState,
        source: KeyEventSource = this.source,
        time: Long = this.time
    ): FusionKeyEvent

    data class Press(
        override val keyCode: Int,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent {
        override fun with(
            keyCode: Int,
            shiftState: KeyboardState.Shift,
            altGrState: Boolean,
            source: KeyEventSource,
            time: Long
        ): FusionKeyEvent {
            return this.copy(
                keyCode = keyCode,
                shiftState = shiftState,
                altGrState = altGrState,
                source = source,
                time = time
            )
        }
    }

    data class Release(
        override val keyCode: Int,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent {
        override fun with(
            keyCode: Int,
            shiftState: KeyboardState.Shift,
            altGrState: Boolean,
            source: KeyEventSource,
            time: Long
        ): FusionKeyEvent {
            return this.copy(
                keyCode = keyCode,
                shiftState = shiftState,
                altGrState = altGrState,
                source = source,
                time = time
            )
        }
    }

    data class LongPress(
        override val keyCode: Int,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent {
        override fun with(
            keyCode: Int,
            shiftState: KeyboardState.Shift,
            altGrState: Boolean,
            source: KeyEventSource,
            time: Long
        ): FusionKeyEvent {
            return this.copy(
                keyCode = keyCode,
                shiftState = shiftState,
                altGrState = altGrState,
                source = source,
                time = time
            )
        }
    }

    data class Flick(
        override val keyCode: Int,
        val direction: FlickDirection,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent {
        override fun with(
            keyCode: Int,
            shiftState: KeyboardState.Shift,
            altGrState: Boolean,
            source: KeyEventSource,
            time: Long
        ): FusionKeyEvent {
            return this.copy(
                keyCode = keyCode,
                direction = this.direction,
                shiftState = shiftState,
                altGrState = altGrState,
                source = source,
                time = time
            )
        }
    }

    data class Cancel(
        override val keyCode: Int,
        override val shiftState: KeyboardState.Shift,
        override val altGrState: Boolean,
        override val source: KeyEventSource,
        override val time: Long = System.currentTimeMillis()
    ): FusionKeyEvent {
        override fun with(
            keyCode: Int,
            shiftState: KeyboardState.Shift,
            altGrState: Boolean,
            source: KeyEventSource,
            time: Long
        ): FusionKeyEvent {
            return this.copy(
                keyCode = keyCode,
                shiftState = shiftState,
                altGrState = altGrState,
                source = source,
                time = time
            )
        }
    }
}