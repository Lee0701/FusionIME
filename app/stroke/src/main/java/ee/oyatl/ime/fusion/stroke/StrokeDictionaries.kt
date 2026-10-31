package ee.oyatl.ime.fusion.stroke

import android.content.Context

object StrokeDictionaries {
    @Volatile
    private var dictionary: StrokeDictionary? = null

    fun load(context: Context): StrokeDictionary {
        dictionary?.let { return it }
        synchronized(this) {
            dictionary?.let { return it }
            val resources = context.resources
            val ranking = resources.openRawResource(R.raw.stroke_ranking).use(StrokeRanking::read)
            val loaded = resources.openRawResource(R.raw.stroke_sequences).use { StrokeDictionary(it, ranking) }
            dictionary = loaded
            return loaded
        }
    }
}
