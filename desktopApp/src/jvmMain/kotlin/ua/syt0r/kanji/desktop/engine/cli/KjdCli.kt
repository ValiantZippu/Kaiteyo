package ua.syt0r.kanji.desktop.engine.cli

import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryEntryType
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryImporter
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseText
import ua.syt0r.kanji.desktop.engine.dictionary.KanjiSpelling
import ua.syt0r.kanji.desktop.engine.dictionary.ThirdPartyDataReport
import ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase
import ua.syt0r.kanji.desktop.engine.jdata.engine.KanjiVgGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.engine.NoStrokeGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.engine.WritingStrictness
import ua.syt0r.kanji.desktop.engine.jdata.integration.PlatformBuilder
import ua.syt0r.kanji.desktop.engine.jdata.io.DatabaseExporter
import ua.syt0r.kanji.desktop.engine.jdata.pipeline.ConsolePipelineLogger
import ua.syt0r.kanji.desktop.engine.jdata.pipeline.GenerationConfig
import ua.syt0r.kanji.desktop.engine.jdata.pipeline.GenerationPipeline
import ua.syt0r.kanji.desktop.engine.jdata.profiles.DatabaseProfile
import ua.syt0r.kanji.desktop.engine.jdata.writing.KanjiWritingSession
import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess

/**
 * `kjd` — Kaiteyo Japanese language data CLI.
 *
 * Operates directly on a dictionary data directory (default `~/.kaiteyo/dictionary`)
 * through [DictionaryRepository]. Every command reads real on-disk data:
 * nothing here is mocked. This is the same engine the desktop app uses.
 *
 * Usage:
 *   kjd [--data <dir>] info
 *   kjd [--data <dir>] sources [--enabled]
 *   kjd [--data <dir>] search <query> [--limit N]
 *   kjd [--data <dir>] lookup <query>
 *   kjd [--data <dir>] strokes <character>
 *   kjd [--data <dir>] radical <character>
 *   kjd [--data <dir>] validate
 *   kjd [--data <dir>] stats
 *   kjd [--data <dir>] attribution [--out <dir>]
 *   kjd [--data <dir>] import <file-or-dir>
 *   kjd [--data <dir>] build [--out <dir>] [--profile minimal|standard|full]
 *   kjd [--data <dir>] export <json|csv|md> [--out <dir>]
 *   kjd [--data <dir>] kanji <character>
 *   kjd [--data <dir>] vocab <word>
 *   kjd [--data <dir>] autocomplete <prefix> [--limit N]
 *   kjd [--data <dir>] writing-check <kanji> <svg-path>... [--strictness relaxed|normal|exam] [--vg <dir>] [--strokes <file>]
 */
fun main(args: Array<String>) {
    exitProcess(KjdCli(System.out, System.err).run(args.toList()))
}

class KjdCli(
    private val out: PrintStream,
    private val err: PrintStream
) {

    fun run(args: List<String>): Int {
        var dataDir: File? = null
        var remaining = args

        // Global flags.
        if (remaining.size >= 2 && remaining[0] == "--data") {
            dataDir = File(remaining[1])
            remaining = remaining.drop(2)
        }
        if (remaining.isEmpty()) {
            printUsage()
            return 2
        }

        val dir = dataDir ?: defaultDataDir()
        val repository = try {
            DictionaryRepository(dir)
        } catch (t: Throwable) {
            err.println("[kjd] Failed to open data directory $dir: ${t.message}")
            return 1
        }

        return try {
            when (val command = remaining.first()) {
                "info" -> info(repository, remaining.drop(1))
                "sources" -> sources(repository, remaining.drop(1))
                "search" -> search(repository, remaining.drop(1))
                "lookup" -> lookup(repository, remaining.drop(1))
                "strokes" -> strokes(repository, remaining.drop(1))
                "radical" -> radical(repository, remaining.drop(1))
                "validate" -> validate(repository)
                "stats" -> stats(repository)
                "attribution" -> attribution(repository, remaining.drop(1))
                "import" -> importDict(repository, remaining.drop(1))
                "build" -> build(repository, remaining.drop(1))
                "export" -> exportData(repository, remaining.drop(1))
                "kanji" -> kanjiDetail(repository, remaining.drop(1))
                "vocab" -> vocabDetail(repository, remaining.drop(1))
                "autocomplete" -> autocomplete(repository, remaining.drop(1))
                "writing-check" -> writingCheck(repository, remaining.drop(1))
                "help", "--help", "-h" -> { printUsage(); 0 }
                else -> {
                    err.println("[kjd] Unknown command: $command")
                    printUsage()
                    2
                }
            }
        } catch (t: Throwable) {
            err.println("[kjd] Command failed: ${t.message}")
            1
        }
    }

    // ------------------------------------------------------------
    // info
    // ------------------------------------------------------------

    private fun info(repository: DictionaryRepository, rest: List<String>): Int {
        if (rest.isNotEmpty() && rest.first() == "help") { printUsage(); return 0 }
        out.println("[kjd] Kaiteyo Japanese language data")
        out.println("  Data directory : ${repository.rootDirectory.absolutePath}")
        out.println("  Dictionaries   : ${repository.installedDictionaries().size} installed, ${repository.enabledDictionaries().size} enabled")
        out.println("  Total entries  : ${repository.totalEntries()}")
        val installed = repository.installedDictionaries()
        installed.forEach { dict ->
            out.println("    - ${dict.id}: ${dict.entryCount} entries (${dict.format})")
        }
        return 0
    }

    // ------------------------------------------------------------
    // sources
    // ------------------------------------------------------------

    private fun sources(repository: DictionaryRepository, rest: List<String>): Int {
        val onlyEnabled = rest.contains("--enabled")
        val list = if (onlyEnabled) repository.enabledDictionaries() else repository.installedDictionaries()
        if (list.isEmpty()) {
            out.println("[kjd] No dictionaries installed in ${repository.rootDirectory.absolutePath}")
            out.println("[kjd] Import one (Yomitan-compatible) or seed the Kaiteyo core kanji dictionary first.")
            return 0
        }
        out.println("[kjd] Sources (${list.size}):")
        list.forEach { dict ->
            out.println(
                "  ${dict.id.padEnd(24)} ${dict.name.padEnd(28)} v${dict.revision.padEnd(8)} " +
                    "${dict.format.name.padEnd(10)} ${dict.entryCount} entries" +
                    if (dict.enabled) "" else "  [disabled]"
            )
        }
        return 0
    }

    // ------------------------------------------------------------
    // search
    // ------------------------------------------------------------

    private fun search(repository: DictionaryRepository, rest: List<String>): Int {
        val query = rest.firstOrNull { !it.startsWith("--") }
        val limit = rest.indexOfFirst { it == "--limit" }
            .takeIf { it >= 0 && it + 1 < rest.size }
            ?.let { rest[it + 1].toIntOrNull() } ?: 20
        if (query.isNullOrBlank()) {
            err.println("[kjd] usage: kjd search <query> [--limit N]")
            return 2
        }
        val matches = repository.lookup(query).take(limit)
        if (matches.isEmpty()) {
            out.println("[kjd] No results for \"$query\"")
            return 0
        }
        out.println("[kjd] ${matches.size} result(s) for \"$query\":")
        matches.forEach { match ->
            val head = match.entry.headword
            val reading = match.entry.readings.firstOrNull()?.reading.orEmpty()
            val gloss = match.entry.senses.firstOrNull()?.glosses?.firstOrNull().orEmpty()
            out.println("  ${head.padEnd(20)} ${reading.padEnd(20)} ${gloss.take(60)}  [${match.dictionary.id}] (score ${match.score})")
        }
        return 0
    }

    // ------------------------------------------------------------
    // lookup
    // ------------------------------------------------------------

    private fun lookup(repository: DictionaryRepository, rest: List<String>): Int {
        val query = rest.firstOrNull()
        if (query.isNullOrBlank()) {
            err.println("[kjd] usage: kjd lookup <query>")
            return 2
        }
        val groups = repository.lookupGrouped(query)
        if (groups.isEmpty()) {
            out.println("[kjd] No results for \"$query\"")
            return 0
        }
        groups.forEach { group ->
            out.println("[${group.dictionary.id}] ${group.dictionary.name}")
            group.matches.forEach { match ->
                val entry = match.entry
                out.println("  ${entry.headword}${entry.readings.firstOrNull()?.let { "  [${it.reading}]" } ?: ""}")
                entry.senses.forEachIndexed { i, sense ->
                    val pos = if (sense.partOfSpeech.isEmpty()) "" else " (${sense.partOfSpeech.joinToString("/")})"
                    out.println("    ${i + 1}. ${sense.glosses.joinToString("; ").take(120)}$pos")
                }
            }
            out.println()
        }
        return 0
    }

    // ------------------------------------------------------------
    // strokes
    // ------------------------------------------------------------

    private fun strokes(repository: DictionaryRepository, rest: List<String>): Int {
        val character = rest.firstOrNull { !it.startsWith("--") }
        if (character.isNullOrBlank() || character.length != 1 || !JapaneseText.isKanji(character)) {
            err.println("[kjd] usage: kjd strokes <single kanji character> [--vg <kanjivg-dir>]")
            return 2
        }
        val vgIdx = rest.indexOfFirst { it == "--vg" }
        val geometry = if (vgIdx >= 0 && vgIdx + 1 < rest.size) KanjiVgGeometryProvider(File(rest[vgIdx + 1])) else null

        val kanji = findKanji(repository, character)
        if (kanji == null) {
            out.println("[kjd] No kanji data for \"$character\" in any installed dictionary.")
            out.println("[kjd] Install a kanji dictionary (e.g. the Kaiteyo core kanji seed) to get stroke data.")
            return 0
        }
        out.println("[kjd] $character — ${kanji.meanings.take(3).joinToString("; ")}")
        out.println("  Stroke count(s): ${kanji.strokeCounts.ifEmpty { listOf("—") }.joinToString(", ")}")
        out.println("  On readings    : ${kanji.onReadings.ifEmpty { listOf("—") }.joinToString("、")}")
        out.println("  Kun readings   : ${kanji.kunReadings.ifEmpty { listOf("—") }.joinToString("、")}")
        out.println("  Radicals       : ${kanji.radicals.ifEmpty { listOf("—") }.joinToString("、")}")
        kanji.jlpt?.let { out.println("  JLPT           : N$it") }
        kanji.grade?.let { out.println("  Grade          : $it") }
        kanji.frequency?.let { out.println("  Frequency rank : $it") }
        if (geometry != null) {
            val paths = geometry.strokesFor(character)
            if (paths.isEmpty()) {
                out.println("  KanjiVG        : no stroke geometry found for \"$character\" in $geometry")
            } else {
                out.println("  KanjiVG        : ${paths.size} stroke path(s)")
                paths.forEachIndexed { index, path ->
                    val bounds = ua.syt0r.kanji.desktop.engine.jdata.engine.SvgPathBounds.of(path)
                    val box = bounds?.let { " ${it.minX.toInt()},${it.minY.toInt()}..${it.maxX.toInt()},${it.maxY.toInt()}" } ?: ""
                    out.println("    ${index + 1}. ${path.take(60)}${if (path.length > 60) "…" else ""}$box")
                }
            }
        }
        return 0
    }

    // ------------------------------------------------------------
    // radical
    // ------------------------------------------------------------

    private fun radical(repository: DictionaryRepository, rest: List<String>): Int {
        val character = rest.firstOrNull()
        if (character.isNullOrBlank() || character.length != 1 || !JapaneseText.isKanji(character)) {
            err.println("[kjd] usage: kjd radical <single kanji character>")
            return 2
        }
        val kanji = findKanji(repository, character)
        if (kanji == null) {
            out.println("[kjd] No kanji data for \"$character\" in any installed dictionary.")
            return 0
        }
        if (kanji.radicals.isEmpty()) {
            out.println("[kjd] No radical data for \"$character\".")
            return 0
        }
        out.println("[kjd] Radicals of $character: ${kanji.radicals.joinToString("、")}")
        // Kanji sharing the same radical (cross-lookup), capped.
        val sharing = repository.allEntries()
            .filter { it.kanjiSpellings.any { s -> s.character != character && s.radicals.any { r -> r in kanji.radicals } } }
            .map { it.headword }
            .distinct()
            .take(12)
        if (sharing.isNotEmpty()) {
            out.println("  Also in: ${sharing.joinToString("、")}")
        }
        return 0
    }

    // ------------------------------------------------------------
    // validate
    // ------------------------------------------------------------

    private fun validate(repository: DictionaryRepository): Int {
        out.println("[kjd] Validating ${repository.installedDictionaries().size} dictionary(ies)...")
        val entries = repository.allEntries()
        var fatal = 0
        var warnings = 0

        val duplicateHeadwords = entries.groupBy { "${it.dictionaryId}|${it.headword}" }.filterValues { it.size > 1 }
        if (duplicateHeadwords.isNotEmpty()) {
            fatal += duplicateHeadwords.size
            out.println("[kjd] FATAL: ${duplicateHeadwords.size} duplicate headword(s):")
            duplicateHeadwords.keys.take(5).forEach { out.println("    - $it") }
        }

        val noGloss = entries.filter { entry -> entry.senses.none { it.glosses.isNotEmpty() } }
        if (noGloss.isNotEmpty()) {
            warnings += noGloss.size
            out.println("[kjd] WARN: ${noGloss.size} entr(ies) without any gloss")
        }

        val noReading = entries.filter { entry -> entry.readings.isEmpty() && entry.kanjiSpellings.none { it.onReadings.isNotEmpty() || it.kunReadings.isNotEmpty() } }
        if (noReading.isNotEmpty()) {
            warnings += noReading.size
            out.println("[kjd] WARN: ${noReading.size} entr(ies) without readings")
        }

        val malformedUnicode = entries.filter { entry -> entry.headword.any { it.isSurrogate() } }
        if (malformedUnicode.isNotEmpty()) {
            fatal += malformedUnicode.size
            out.println("[kjd] FATAL: ${malformedUnicode.size} entr(ies) with malformed (surrogate) headword characters")
        }

        val invalidStrokeCounts = entries.flatMap { it.kanjiSpellings }
            .filter { it.strokeCounts.any { n -> n <= 0 || n > 64 } }
        if (invalidStrokeCounts.isNotEmpty()) {
            warnings += invalidStrokeCounts.size
            out.println("[kjd] WARN: ${invalidStrokeCounts.size} kanji with implausible stroke counts")
        }

        out.println("[kjd] Validated ${entries.size} entries: $fatal fatal issue(s), $warnings warning(s)")
        return if (fatal == 0) 0 else 1
    }

    // ------------------------------------------------------------
    // stats
    // ------------------------------------------------------------

    private fun stats(repository: DictionaryRepository): Int {
        val entries = repository.allEntries()
        if (entries.isEmpty()) {
            out.println("[kjd] No data installed — nothing to summarize.")
            return 0
        }
        out.println("[kjd] Data coverage:")
        out.println("  Total entries              : ${entries.size}")
        out.println("  Kanji entries              : ${entries.count { it.source == DictionaryEntryType.Kanji }}")
        out.println("  Vocabulary entries         : ${entries.count { it.source == DictionaryEntryType.Vocabulary }}")

        val kanjiSpellings = entries.flatMap { it.kanjiSpellings }.distinctBy { it.character }
        out.println("  Distinct kanji            : ${kanjiSpellings.size}")
        out.println("  Kanji with stroke counts  : ${kanjiSpellings.count { it.strokeCounts.isNotEmpty() }}")
        out.println("  Kanji with radicals       : ${kanjiSpellings.count { it.radicals.isNotEmpty() }}")
        out.println("  Kanji with JLPT tag       : ${kanjiSpellings.count { it.jlpt != null }}")
        (5 downTo 1).forEach { level ->
            val count = kanjiSpellings.count { it.jlpt == level }
            if (count > 0) out.println("    N$level: $count")
        }

        val vocabWithReading = entries.filter { it.source == DictionaryEntryType.Vocabulary && it.readings.isNotEmpty() }
        out.println("  Vocab with readings       : ${vocabWithReading.size}")
        out.println("  Vocab with senses         : ${entries.count { it.senses.isNotEmpty() }}")
        out.println("  Vocab with pitch accents  : ${entries.count { e -> e.readings.any { it.pitchAccents.isNotEmpty() } }}")
        out.println("  Entries with frequency    : ${entries.count { it.frequency.rank != null || it.frequency.score != null }}")
        return 0
    }

    // ------------------------------------------------------------
    // attribution
    // ------------------------------------------------------------

    private fun attribution(repository: DictionaryRepository, rest: List<String>): Int {
        val outDirIdx = rest.indexOfFirst { it == "--out" }
        val outDir = if (outDirIdx >= 0 && outDirIdx + 1 < rest.size) File(rest[outDirIdx + 1]) else File(".")
        outDir.mkdirs()
        val manifest = ThirdPartyDataReport.generate(repository)
        val md = File(outDir, "THIRD_PARTY_DATA.md")
        val json = File(outDir, "THIRD_PARTY_DATA.json")
        md.writeText(ThirdPartyDataReport.toMarkdown(manifest))
        json.writeText(ThirdPartyDataReport.toJson(manifest))
        out.println("[kjd] Wrote ${md.absolutePath}")
        out.println("[kjd] Wrote ${json.absolutePath}")
        return 0
    }

    // ------------------------------------------------------------
    // import
    // ------------------------------------------------------------

    private fun importDict(repository: DictionaryRepository, rest: List<String>): Int {
        val path = rest.firstOrNull { !it.startsWith("--") }
        if (path.isNullOrBlank()) {
            err.println("[kjd] usage: kjd import <file-or-directory> (Yomitan-compatible ZIP/dir/JSON)")
            return 2
        }
        return try {
            val bundle = DictionaryImporter.import(File(path))
            if (bundle.entries.isEmpty()) {
                err.println("[kjd] No entries could be parsed from $path")
                return 1
            }
            val dict = repository.installImport(bundle.result, bundle.entries)
            out.println("[kjd] Installed \"${dict.name}\" (${dict.entryCount} entries, ${dict.format})")
            0
        } catch (t: Throwable) {
            err.println("[kjd] Import failed: ${t.message}")
            1
        }
    }

    // ------------------------------------------------------------
    // build — full platform generation
    // ------------------------------------------------------------

    private fun build(repository: DictionaryRepository, rest: List<String>): Int {
        val outDirIdx = rest.indexOfFirst { it == "--out" }
        val outDir = if (outDirIdx >= 0 && outDirIdx + 1 < rest.size) File(rest[outDirIdx + 1]) else File("build/kjd")
        val profileIdx = rest.indexOfFirst { it == "--profile" }
        val profile = if (profileIdx >= 0 && profileIdx + 1 < rest.size) {
            DatabaseProfile.parse(rest[profileIdx + 1]) ?: DatabaseProfile.Standard
        } else DatabaseProfile.Standard
        val vgIdx = rest.indexOfFirst { it == "--vg" }
        val geometry = if (vgIdx >= 0 && vgIdx + 1 < rest.size) {
            KanjiVgGeometryProvider(File(rest[vgIdx + 1]))
        } else null
        outDir.mkdirs()

        val logger = ConsolePipelineLogger { out.println(it) }
        if (geometry != null) logger.info("Using KanjiVG stroke geometry from $geometry")
        val result = GenerationPipeline(logger).run(
            repository,
            GenerationConfig(
                profile = profile,
                geometry = geometry ?: ua.syt0r.kanji.desktop.engine.jdata.engine.NoStrokeGeometryProvider
            )
        )

        DatabaseExporter.writeAll(result.platformData, outDir)
        File(outDir, "quality-report.md").writeText(result.quality.toMarkdown())
        File(outDir, "release-manifest.json").writeText(result.release.toJson())
        val manifest = ThirdPartyDataReport.generate(repository)
        File(outDir, "THIRD_PARTY_DATA.md").writeText(ThirdPartyDataReport.toMarkdown(manifest))
        File(outDir, "THIRD_PARTY_DATA.json").writeText(ThirdPartyDataReport.toJson(manifest))
        out.println("[kjd] Wrote release artifacts (profile=${profile.label}) to ${outDir.absolutePath}")
        return if (result.success) 0 else 1
    }

    // ------------------------------------------------------------
    // export
    // ------------------------------------------------------------

    private fun exportData(repository: DictionaryRepository, rest: List<String>): Int {
        val format = rest.firstOrNull { !it.startsWith("--") } ?: "json"
        val outDirIdx = rest.indexOfFirst { it == "--out" }
        val outDir = if (outDirIdx >= 0 && outDirIdx + 1 < rest.size) File(rest[outDirIdx + 1]) else File(".")
        outDir.mkdirs()
        val data = PlatformBuilder.fromRepository(repository)
        val written = when (format.lowercase()) {
            "json" -> listOf(File(outDir, "kaiteyo-data.json") to DatabaseExporter.toJson(data))
            "csv" -> listOf(
                File(outDir, "kanji.csv") to DatabaseExporter.toCsvKanji(data),
                File(outDir, "vocabulary.csv") to DatabaseExporter.toCsvVocab(data),
                File(outDir, "radicals.csv") to DatabaseExporter.toCsvRadicals(data),
                File(outDir, "relations.csv") to DatabaseExporter.toCsvRelations(data)
            )
            "md", "markdown" -> listOf(File(outDir, "data-summary.md") to DatabaseExporter.toMarkdown(data))
            else -> {
                err.println("[kjd] Unknown export format: $format (json|csv|md)")
                return 2
            }
        }
        written.forEach { (file, content) -> file.writeText(content); out.println("[kjd] Wrote ${file.absolutePath}") }
        return 0
    }

    // ------------------------------------------------------------
    // kanji — canonical platform detail
    // ------------------------------------------------------------

    private fun kanjiDetail(repository: DictionaryRepository, rest: List<String>): Int {
        val character = rest.firstOrNull()
        if (character.isNullOrBlank() || character.length != 1 || !JapaneseText.isKanji(character)) {
            err.println("[kjd] usage: kjd kanji <single kanji character>")
            return 2
        }
        val db = LanguageDatabase.open(PlatformBuilder.fromRepository(repository))
        val kanji = db.getKanji(character)
        if (kanji == null) {
            out.println("[kjd] No kanji data for \"$character\" in any installed dictionary.")
            return 0
        }
        out.println("[kjd] $character — ${kanji.meanings.joinToString("; ")}")
        out.println("  On readings  : ${kanji.onReadings.joinToString("、")}")
        out.println("  Kun readings : ${kanji.kunReadings.joinToString("、")}")
        out.println("  Strokes      : ${kanji.strokeCount ?: "—"}")
        out.println("  Radical      : ${kanji.radicalId?.let { db.getRadical(character)?.meaning ?: it } ?: "—"}")
        out.println("  Components   : ${db.getComponents(character).joinToString("、") { it.character }}")
        kanji.jlpt?.let { out.println("  JLPT         : N$it") }
        kanji.grade?.let { out.println("  Grade        : $it") }
        kanji.frequencyRank?.let { out.println("  Frequency    : #$it") }
        out.println("  Provenance   : ${kanji.sources.joinToString(", ") { it.sourceId }}")
        val inVocab = db.vocabForKanji(character)
        out.println("  Vocabulary   : ${inVocab.take(12).joinToString("、") { it.expression }}${if (inVocab.size > 12) " …" else ""} (${inVocab.size})")
        return 0
    }

    // ------------------------------------------------------------
    // vocab — canonical platform detail
    // ------------------------------------------------------------

    private fun vocabDetail(repository: DictionaryRepository, rest: List<String>): Int {
        val query = rest.firstOrNull()
        if (query.isNullOrBlank()) {
            err.println("[kjd] usage: kjd vocab <word>")
            return 2
        }
        val db = LanguageDatabase.open(PlatformBuilder.fromRepository(repository))
        val entries = db.getVocabulary(query)
        if (entries.isEmpty()) {
            out.println("[kjd] No vocabulary data for \"$query\"")
            return 0
        }
        entries.forEach { entry ->
            out.println("[kjd] ${entry.expression} [${entry.primaryReading ?: "—"}] (${entry.id})")
            if (entry.furigana.isNotEmpty()) {
                out.println("  Furigana : " + entry.furigana.joinToString("") {
                    if (it.reading == null) it.text else "${it.text}[${it.reading}]"
                })
            }
            entry.senses.forEachIndexed { i, sense ->
                val pos = if (sense.partOfSpeech.isEmpty()) "" else " (${sense.partOfSpeech.joinToString("/")})"
                out.println("  ${i + 1}. ${sense.glosses.joinToString("; ").take(140)}$pos")
            }
            if (entry.frequencies.isNotEmpty()) {
                out.println("  Frequency: ${entry.frequencies.joinToString(", ") { f -> "${f.source}=${f.rank ?: f.value}" }}")
            }
            entry.jlpt?.let { out.println("  JLPT     : N$it") }
            out.println("  Provenance: ${entry.sources.joinToString(", ") { it.sourceId }}")
            out.println()
        }
        return 0
    }

    // ------------------------------------------------------------
    // autocomplete
    // ------------------------------------------------------------

    private fun autocomplete(repository: DictionaryRepository, rest: List<String>): Int {
        val prefix = rest.firstOrNull { !it.startsWith("--") }
        val limit = rest.indexOfFirst { it == "--limit" }
            .takeIf { it >= 0 && it + 1 < rest.size }
            ?.let { rest[it + 1].toIntOrNull() } ?: 10
        if (prefix.isNullOrBlank()) {
            err.println("[kjd] usage: kjd autocomplete <prefix> [--limit N]")
            return 2
        }
        val db = LanguageDatabase.open(PlatformBuilder.fromRepository(repository))
        val suggestions = db.autocomplete(prefix, limit)
        if (suggestions.isEmpty()) {
            out.println("[kjd] No suggestions for \"$prefix\"")
            return 0
        }
        suggestions.forEach { s ->
            out.println("  ${s.text.padEnd(16)} ${s.reading.padEnd(12)} ${s.entityType.name.padEnd(10)} score=${(s.score * 10).toInt() / 10f}")
        }
        return 0
    }

    // ------------------------------------------------------------
    // writing-check — evaluate drawn strokes against reference data
    // ------------------------------------------------------------

    private fun writingCheck(repository: DictionaryRepository, rest: List<String>): Int {
        val character = rest.firstOrNull { !it.startsWith("--") }
        if (character.isNullOrBlank() || character.length != 1 || !JapaneseText.isKanji(character)) {
            err.println("[kjd] usage: kjd writing-check <kanji> <svg-path> [<svg-path> ...]")
            err.println("            [--strictness relaxed|normal|exam] [--vg <dir>] [--strokes <file>]")
            return 2
        }
        // Positional SVG paths must come before any --flag.
        val pathArgs = rest.drop(1).takeWhile { !it.startsWith("--") }
        val strictnessIdx = rest.indexOfFirst { it == "--strictness" }
        val strictness = when (strictnessIdx.takeIf { it >= 0 && it + 1 < rest.size }?.let { rest[it + 1].lowercase() }) {
            "relaxed" -> WritingStrictness.Relaxed
            "exam" -> WritingStrictness.Exam
            else -> WritingStrictness.Normal
        }
        val vgIdx = rest.indexOfFirst { it == "--vg" }
        val geometry = if (vgIdx >= 0 && vgIdx + 1 < rest.size) {
            KanjiVgGeometryProvider(File(rest[vgIdx + 1]))
        } else NoStrokeGeometryProvider
        val strokesIdx = rest.indexOfFirst { it == "--strokes" }
        val drawnPaths = if (strokesIdx >= 0 && strokesIdx + 1 < rest.size) {
            val file = File(rest[strokesIdx + 1])
            if (!file.isFile) {
                err.println("[kjd] Strokes file not found: ${file.absolutePath}")
                return 2
            }
            file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        } else pathArgs
        if (drawnPaths.isEmpty()) {
            err.println("[kjd] No drawn strokes given — pass SVG path data as arguments (before any --flags)")
            err.println("            or use --strokes <file> (one SVG path per line).")
            return 2
        }

        val db = LanguageDatabase.open(PlatformBuilder.fromRepository(repository, geometry = geometry))
        val writer = KanjiWritingSession(db)
        val session = writer.begin(character)
        if (session == null) {
            out.println("[kjd] No stroke data for \"$character\" in the platform (dictionary has no stroke counts).")
            return 1
        }
        if (!session.hasGeometry) {
            out.println("[kjd] No reference stroke geometry for \"$character\" — pass --vg <kanjivg-dir>")
            out.println("            for full geometric evaluation; running structural (count) checks only.")
        }
        val attempt = writer.submit(session, drawnPaths, strictness)
        attempt.summaryLines().forEach { out.println(it) }
        return if (attempt.accepted) 0 else 1
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun findKanji(repository: DictionaryRepository, character: String): KanjiSpelling? =
        repository.allEntries()
            .flatMap { it.kanjiSpellings }
            .firstOrNull { it.character == character }

    private fun defaultDataDir(): File =
        File(System.getProperty("user.home"), ".kaiteyo/dictionary")

    private fun printUsage() {
        out.println(
            """
            |kjd — Kaiteyo Japanese language data CLI
            |
            |Usage: kjd [--data <dir>] <command> [args]
            |
            |Commands:
            |  info                          Engine & data-directory summary
            |  sources [--enabled]           List installed dictionary sources
            |  search <query> [--limit N]    Fast search across enabled dictionaries
            |  lookup <query>                Detailed grouped lookup
            |  strokes <kanji> [--vg <dir>]  Stroke counts/readings; --vg adds real KanjiVG paths
            |  radical <kanji>               Radicals and related kanji
            |  validate                      Integrity validation (fatal issues fail)
            |  stats                         Data coverage summary
            |  attribution [--out <dir>]     Write THIRD_PARTY_DATA.md/.json
            |  import <file-or-dir>          Import a Yomitan-compatible dictionary
            |  build [--out <dir>] [--profile <minimal|standard|full>] [--vg <dir>]
            |                                Run the full platform generation pipeline
            |                                (--vg: attach real KanjiVG stroke geometry)
            |  export <json|csv|md> [--out <dir>]
            |                                Export canonical platform data
            |  kanji <character>             Canonical kanji detail (platform model)
            |  vocab <word>                  Canonical vocabulary detail (platform model)
            |  autocomplete <prefix> [--limit N]
            |                                Ranked suggestions from the search index
            |  writing-check <kanji> <svg-path>...  Evaluate drawn strokes against reference
            |                                data. Flags: --strictness relaxed|normal|exam,
            |                                --vg <dir> (KanjiVG geometry), --strokes <file>
            |  help                          This help
            |
            |Global:
            |  --data <dir>   Dictionary data directory (default: ~/.kaiteyo/dictionary)
            """.trimMargin()
        )
    }

    private fun Char.isSurrogate(): Boolean = this in '\uD800'..'\uDFFF'
}
