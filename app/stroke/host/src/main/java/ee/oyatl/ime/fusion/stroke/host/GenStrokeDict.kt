package ee.oyatl.ime.fusion.stroke.host

import java.io.File

/**
 * Usage: GenStrokeDict <stroke data dir> <output dir>
 *
 * Writes stroke_sequences.bin and stroke_ranking.bin, see [StrokeData].
 */
fun main(args: Array<String>) {
    val (dataDir, outDir) = args
    val data = StrokeData.load(File(dataDir))
    data.writeSequences(File(outDir, "stroke_sequences.bin"))
    data.writeRanking(File(outDir, "stroke_ranking.bin"))
    println("${data.sequences.size} sequences, ${data.sequences.values.sumOf { it.size }} entries")
}
