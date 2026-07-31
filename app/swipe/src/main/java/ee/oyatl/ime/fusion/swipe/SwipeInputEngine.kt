package ee.oyatl.ime.fusion.swipe

import android.content.Context
import android.graphics.PointF
import ee.oyatl.ime.keyboard.listener.SwipeListener
import tribixbite.cleverkeys.SwipeInput
import tribixbite.cleverkeys.swipe.SwipePredictor

class SwipeInputEngine(
    context: Context,
    val listener: Listener
): SwipeListener {
    val swipePredictor: SwipePredictor = SwipePredictor(context, SwipePredictor.SearchEngineType.Beam)

    fun init() {
        swipePredictor.init()
    }

    override fun onKeyDown(keyCode: Int, metaState: Int) = Unit

    override fun onKeyUp(keyCode: Int, metaState: Int) = Unit

    override fun onReset() = Unit

    override fun onSwipeStart() {
    }

    override fun onSwipeEnd(pointers: List<SwipeListener.Pointer>) {
        val input = SwipeInput(
            coordinates = pointers.map { PointF(it.x, it.y) },
            timestamps = pointers.map { it.time },
            touchedKeys = pointers.map { it.touchedKey }
        )
        val result = swipePredictor.predict(input)
        listener.onSwipeResult(result)
        val preview = result.firstOrNull()
        if(preview != null) listener.onSwipePreview(preview.word)
    }

    override fun onSwipeMove(pointers: List<SwipeListener.Pointer>) {
    }

    interface Listener {
        fun onSwipePreview(previewString: String)
        fun onSwipeResult(result: List<SwipePredictor.Result>)
    }
}