package ee.oyatl.ime.fusion.settings

import androidx.preference.PreferenceDataStore

class StringMapPreferenceDataStore(
    val map: MutableMap<String, String>
): PreferenceDataStore() {

    override fun putString(key: String?, value: String?) {
        map[key ?: return] = value ?: return
    }

    override fun putInt(key: String?, value: Int) {
        map[key ?: return] = value.toString()
    }

    override fun putLong(key: String?, value: Long) {
        map[key ?: return] = value.toString()
    }

    override fun putFloat(key: String?, value: Float) {
        map[key ?: return] = value.toString()
    }

    override fun putBoolean(key: String?, value: Boolean) {
        map[key ?: return] = value.toString()
    }

    override fun getString(key: String?, defValue: String?): String? {
        return map[key]
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return map[key]?.toInt() ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return map[key]?.toLong() ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return map[key]?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return map[key]?.toBoolean() ?: defValue
    }
}