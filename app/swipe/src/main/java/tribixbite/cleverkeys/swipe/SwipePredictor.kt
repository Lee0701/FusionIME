package tribixbite.cleverkeys.swipe

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.android.inputmethod.latin.Dictionary
import com.android.inputmethod.latin.DictionaryFactory
import com.android.inputmethod.latin.common.StringUtils
import tribixbite.cleverkeys.SwipeInput
import tribixbite.cleverkeys.SwipeTokenizer
import tribixbite.cleverkeys.SwipeTrajectoryProcessor
import tribixbite.cleverkeys.onnx.BeamSearchEngine
import tribixbite.cleverkeys.onnx.EncoderWrapper
import tribixbite.cleverkeys.onnx.GreedySearchEngine
import tribixbite.cleverkeys.onnx.ModelLoader
import tribixbite.cleverkeys.onnx.OrtDecoderSession
import tribixbite.cleverkeys.onnx.TensorFactory
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SwipePredictor(
    context: Context,
    private val locale: Locale,
    private val searchEngineType: SearchEngineType
) {
    val enableHardwareAcceleration = true
    val xnnPackThreads = 2
    val maxSequenceLength = 250
    val trajectoryFeatures = 6
    val beamWidth = 6
    val maxLength = 20
    val encoderPath = "models/swipe_encoder_android.onnx"
    val decoderPath = "models/swipe_decoder_android.onnx"

    private val sessionLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val tokenizer: SwipeTokenizer = SwipeTokenizer()
    private val modelLoader: ModelLoader = ModelLoader(context, ortEnvironment)
    private val trajectoryProcessor: SwipeTrajectoryProcessor = SwipeTrajectoryProcessor()
    private val dictionary: Dictionary = DictionaryFactory.createMainDictionaryFromManager(context, locale)

    private var tensorFactory: TensorFactory? = null
    private var greedySearchEngine: GreedySearchEngine? = null
    private var beamSearchEngine: BeamSearchEngine? = null

    private var isLoaded: Boolean = false

    private var encoderWrapper: EncoderWrapper? = null
    private var ortDecoderSession: OrtDecoderSession? = null
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null

    init {
        tokenizer.loadFromAssets(context)
    }

    fun init() {
        val encoder = modelLoader.loadModel(encoderPath, "Encoder", enableHardwareAcceleration, xnnPackThreads)
        val decoder = modelLoader.loadModel(decoderPath, "Decoder", enableHardwareAcceleration, xnnPackThreads)

        sessionLock.write {
            encoderSession = encoder.session
            decoderSession = decoder.session

            val tensorFactory = TensorFactory(ortEnvironment, maxSequenceLength, trajectoryFeatures)
            this.tensorFactory = tensorFactory
            val encoderWrapper = EncoderWrapper(encoder.session, tensorFactory, ortEnvironment)
            this.encoderWrapper = encoderWrapper

            when (searchEngineType) {
                SearchEngineType.Greedy -> {
                    this.greedySearchEngine =
                        GreedySearchEngine(decoder.session, ortEnvironment, tokenizer, maxLength)
                }
                SearchEngineType.Beam -> {
                    val ortDecoderSession = OrtDecoderSession(decoder.session, ortEnvironment)
                    this.ortDecoderSession = ortDecoderSession
                    this.beamSearchEngine =
                        BeamSearchEngine(ortDecoderSession, tokenizer, dictionary, beamWidth, maxLength)
                }
            }

            this.isLoaded = true
        }
    }

    fun predict(input: SwipeInput): List<Result> {
        return sessionLock.read {
            if(!isLoaded) return emptyList()

            val encoder = encoderWrapper ?: return emptyList()

            val features = trajectoryProcessor.extractFeatures(input, maxSequenceLength)
            val encoderResult = encoder.encode(features)

            val candidates = encoderResult.use { encoderResult ->
                when (searchEngineType) {
                    SearchEngineType.Greedy -> {
                        val greedySearchEngine = greedySearchEngine ?: return emptyList()
                        val results = greedySearchEngine.search(encoderResult.memory, features.actualLength)
                        results.map { SearchResult(it.word, it.confidence) }
                    }
                    SearchEngineType.Beam -> {
                        val beamSearchEngine = beamSearchEngine ?: return emptyList()
                        val decoderSession = ortDecoderSession ?: return  emptyList()
                        decoderSession.setMemory(encoderResult.memory)
                        val results = beamSearchEngine.search(features.actualLength, false)
                        results.map { SearchResult(it.word, it.confidence) }
                    }
                }
            }

            candidates
                .map { it.word }
                .flatMap { listOf(it, it.uppercase(), StringUtils.capitalizeFirstCodePoint(it, locale)) }
                .flatMap { listOf(it, "'$it", "$it'", it.dropLast(1) + "'" + it.takeLast(1), it.dropLast(2) + "'" + it.takeLast(2)) }
                .mapNotNull { getResult(it) }
        }
    }

    private fun getResult(text: String): Result? {
        val freq = dictionary.getFrequency(text)
        if(freq == Dictionary.NOT_A_PROBABILITY) return null
        return Result(text, freq)
    }

    data class SearchResult(
        val word: String,
        val confidence: Float
    )

    data class Result(
        val word: String,
        val score: Int
    )

    enum class SearchEngineType {
        Greedy, Beam
    }
}