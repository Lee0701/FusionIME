package tribixbite.cleverkeys.onnx

interface SearchEngine {
    data class Result(
        val word: String,
        val confidence: Float
    )
}