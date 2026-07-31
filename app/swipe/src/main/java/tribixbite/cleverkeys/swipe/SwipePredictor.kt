package tribixbite.cleverkeys.swipe

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.android.inputmethod.latin.Dictionary
import com.android.inputmethod.latin.DictionaryFactory
import tribixbite.cleverkeys.SwipeInput
import tribixbite.cleverkeys.SwipeTokenizer
import tribixbite.cleverkeys.SwipeTrajectoryProcessor
import tribixbite.cleverkeys.onnx.EncoderWrapper
import tribixbite.cleverkeys.onnx.GreedySearchEngine
import tribixbite.cleverkeys.onnx.ModelLoader
import tribixbite.cleverkeys.onnx.TensorFactory
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SwipePredictor(
    context: Context
) {
    val enableHardwareAcceleration = true
    val xnnPackThreads = 2
    val maxSequenceLength = 250
    val trajectoryFeatures = 6
    val maxLength = 20
    val encoderPath = "models/swipe_encoder_android.onnx"
    val decoderPath = "models/swipe_decoder_android.onnx"

    private val sessionLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val tokenizer: SwipeTokenizer = SwipeTokenizer()
    private val modelLoader: ModelLoader = ModelLoader(context, ortEnvironment)
    private val trajectoryProcessor: SwipeTrajectoryProcessor = SwipeTrajectoryProcessor()
    private val dictionary: Dictionary = DictionaryFactory.createMainDictionaryFromManager(context, Locale.ENGLISH)

    private var tensorFactory: TensorFactory? = null
    private var greedySearchEngine: GreedySearchEngine? = null

    private var isLoaded: Boolean = false

    private var encoderWrapper: EncoderWrapper? = null
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

            this.greedySearchEngine = GreedySearchEngine(decoder.session, ortEnvironment, tokenizer, maxLength)

            this.isLoaded = true
        }
    }

    fun predict(input: SwipeInput): List<Result> {
        return sessionLock.read {
            if(!isLoaded) return emptyList()

            val encoder = encoderWrapper ?: return emptyList()
            val decoder = decoderSession ?: return emptyList()
            val greedySearchEngine = greedySearchEngine ?: return emptyList()

            val features = trajectoryProcessor.extractFeatures(input, maxSequenceLength)
            val encoderResult = encoder.encode(features)

            val firstDetectedKey = features.nearestKeys.firstOrNull { it in 4 .. 29 }?.let { 'a' + (it - 4) }

            val candidates = encoderResult.use { encoderResult ->
                greedySearchEngine.search(encoderResult.memory, features.actualLength)
            }

            candidates
                .filter { dictionary.isInDictionary(it.word) }
                .map { Result(it.word, dictionary.getFrequency(it.word)) }
        }
    }

    data class Result(
        val word: String,
        val score: Int
    )
}