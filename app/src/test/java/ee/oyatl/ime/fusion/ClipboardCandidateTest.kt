package ee.oyatl.ime.fusion

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardCandidateTest {
    @Test
    fun passwordInputTypesAreSensitive() {
        assertTrue(sensitiveText(InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertTrue(sensitiveText(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
        assertTrue(sensitiveText(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
        assertTrue(
            isSensitiveInputType(
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            )
        )
    }

    @Test
    fun ordinaryTextInputTypesAreNotSensitive() {
        assertFalse(isSensitiveInputType(InputType.TYPE_CLASS_TEXT))
        assertFalse(sensitiveText(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertFalse(isSensitiveInputType(InputType.TYPE_CLASS_NUMBER))
    }

    private fun sensitiveText(variation: Int): Boolean {
        return isSensitiveInputType(InputType.TYPE_CLASS_TEXT or variation)
    }
}
