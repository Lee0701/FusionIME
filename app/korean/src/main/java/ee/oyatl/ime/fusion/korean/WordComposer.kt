package ee.oyatl.ime.fusion.korean

class WordComposer {
    private val history: MutableList<String> = mutableListOf()
    private var composing: String = ""
    val word: String get() = history.lastOrNull().orEmpty() + composing

    fun compose(text: String) {
        composing = text
    }

    fun commit(text: String) {
        if(text.isEmpty()) return
        history += history.lastOrNull().orEmpty() + text
        composing = ""
    }

    fun delete(length: Int): Boolean {
        (0 until length).forEach { _ ->
            history.removeLastOrNull() ?: return false
        }
        return true
    }

    fun consume(length: Int) {
        commit(composing)
        val newHistory = history.map { it.drop(length) }.filter { it.isNotEmpty() }
        history.clear()
        history += newHistory
    }

    fun reset() {
        history.clear()
        composing = ""
    }
}