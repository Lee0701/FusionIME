package ee.oyatl.ime.keyboard.touchhandler

import android.view.KeyEvent
import com.truetap.keyboard.engine.KeyGeom
import com.truetap.keyboard.engine.SpatialModel
import com.truetap.keyboard.engine.Tap
import com.truetap.keyboard.engine.TouchModel
import ee.oyatl.ime.keyboard.KeyboardView
import kotlin.collections.toList
import kotlin.math.hypot

class TouchCorrector(
    val keyboardView: KeyboardView
) {
    val keyCodes = (KeyEvent.KEYCODE_A .. KeyEvent.KEYCODE_Z).toList() +
            (KeyEvent.KEYCODE_0 .. KeyEvent.KEYCODE_9).toList() +
            listOf(
                KeyEvent.KEYCODE_SEMICOLON, KeyEvent.KEYCODE_APOSTROPHE,
                KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_COMMA, KeyEvent.KEYCODE_SLASH
            )
    var keyGeom: List<KeyGeom> = listOf()
    var touchModel: TouchModel = TouchModel()
    var spatialModel: SpatialModel = SpatialModel(listOf(), touchModel)

    fun reset() {
        keyGeom = keyCodes
            .flatMap { keyboardView.findKeys(it) }
            .map {
                KeyGeom(
                    it.keyCode.toChar(),
                    it.rect.centerX().toFloat(),
                    it.rect.centerY().toFloat(),
                    it.rect.width().toFloat(),
                    it.rect.height().toFloat()
                )
            }
        spatialModel = SpatialModel(keyGeom, touchModel)
    }

    fun correct(x: Int, y: Int, learn: Boolean): KeyboardView.Key? {
        val key = keyboardView.findKey(x, y) ?: return null
        val keyCode = key.keyCode

        if(keyCode !in keyCodes) {
            return key
        }

        val tap = Tap(x.toFloat(), y.toFloat())
        val scores = spatialModel.scores(tap)
        val map = keyGeom.zip(scores.toList()).associate { (geom, score) -> geom.char to score }
        val correctedCode = map.entries.maxByOrNull { it.value }?.key?.code ?: keyCode
        val correction = keyCode != correctedCode

        if(learn && !correction && hypot(
                (x - key.rect.centerX()) / key.rect.width().toFloat(),
                (y - key.rect.centerY()) / key.rect.height().toFloat()
            ) <= 1.3f) {
            spatialModel.learn(keyCode.toChar(), tap)
        }

        return keyboardView.findKeys(correctedCode).firstOrNull()
    }
}