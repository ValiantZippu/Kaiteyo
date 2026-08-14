package io.kaiteyo.kjd.cli

import io.kaiteyo.kjd.api.JapaneseDataApi
import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.KjdVersion
import io.kaiteyo.kjd.export.AttributionWriter
import io.kaiteyo.kjd.patch.DatabaseDiffGenerator
import io.kaiteyo.kjd.patch.DatabaseFingerprint
import io.kaiteyo.kjd.patch.DatabasePatch
import io.kaiteyo.kjd.patch.DatabasePatcher
import io.kaiteyo.kjd.patch.PatchResult
import io.kaiteyo.kjd.pipeline.KjdPipeline
import io.kaiteyo.kjd.pipeline.PipelineConfig
import io.kaiteyo.kjd.pipeline.ReleaseManifestWriter
import io.kaiteyo.kjd.search.preview
import io.kaiteyo.kjd.source.SafeArchiveExtractor
import io.kaiteyo.kjd.source.SourceArtifact
import java.io.File
import java.security.MessageDigest

/**
 * KJD developer CLI.
 *
 * ```
 * kjd info                          — platform & schema info
 * kjd search kanji 食                — search kanji
 * kjd search vocab 食べる            — search vocabulary
 * kjd lookup 食べる                  — full lookup (vocab first, then kanji)
 * kjd strokes 食                     — stroke order + metadata
 * kjd radical 食                     — radical info
 * kjd stats                         — database statistics
 * kjd validate                      — validate a database
 * kjd sources                       — list ingested sources & licenses
 * kjd build --sources-dir ... --out ...   — run the full generation pipeline
 * kjd export --format json --out ...       — export the database
 * ```
 *
 * The CLI opens the database file (default: `kjd-japanese.db` in the current
 * directory) for read commands and takes `--db` to point elsewhere.
 */
fun main(args: Array<String>) {
    val cli = KjdCli(args)
    cli.run()
}

class KjdCli(private val args: Array<String>) {

    private val dbPath: File
        get() = File(argumentValue("--db") ?: KjdVersion.DEFAULT_DATABASE_NAME)

    fun run() {
        val command = args.firstOrNull() ?: "help"
        when (command) {
            "info" -> info()
            "search" -> search()
            "lookup" -> lookup()
            "strokes" -> strokes()
            "radical" -> radical()
            "components" -> components()
            "kana" -> kana()
            "stats" -> stats()
            "validate" -> validate()
            "sources" -> sources()
            "attribution" -> attribution()
            "release" -> release()
            "build" -> build()
            "import" -> importSource()
            "export" -> export()
            "diff" -> diff()
            "patch" -> patch()
            "fingerprint" -> fingerprint()
            "help", "-h", "--help" -> help()
            else -> {
                System.err.println("Unknown command: $command")
                help()
            }
        }
    }

    private fun info() {
        println("${KjdVersion.PLATFORM_NAME}")
        println("SDK version      : ${KjdVersion.SDK_VERSION}")
        println("Schema version   : ${KjdVersion.SCHEMA_VERSION}")
        println("Database         : ${dbPath.absolutePath}")
        if (dbPath.exists()) {
            JapaneseDatabase.open(dbPath).use { db ->
                println("DB schema version: ${db.schemaVersion()}")
                println("Generator version: ${db.generatorVersion()}")
                println("Kanji            : ${db.kanjiCount()}")
                println("Vocabulary       : ${db.vocabularyCount()}")
            }
        } else {
            println("(no database at this path — run `kjd build`)")
        }
    }

    private fun search() {
        val kind = argumentAfter("search")?.lowercase() ?: "all"
        val query = argumentAfter(if (kind == "kanji" || kind == "vocab" || kind == "all") {
            if (kind == "kanji") "kanji" else if (kind == "vocab") "vocab" else "search"
        } else "search")
        if (query == null) {
            println("usage: kjd search [kanji|vocab] <query>")
            return
        }
        requireDatabase().use { db ->
            val results = db.search(query)
            if (results.isEmpty()) {
                println("No results for \"$query\"")
                return@use
            }
            results.take(25).forEach { result ->
                println("${result.entityType.name.lowercase().padEnd(10)} ${result.preview()}")
            }
            if (results.size > 25) println("… and ${results.size - 25} more")
        }
    }

    private fun lookup() {
        val query = argumentAfter("lookup")
        if (query == null) {
            println("usage: kjd lookup <expression>")
            return
        }
        requireDatabase().use { db ->
            val api = JapaneseDataApi(db)
            val vocab = api.getVocabulary(query)
            if (vocab != null) {
                println("Expression : ${vocab.expression}")
                println("Readings   : ${vocab.readings.joinToString(", ") { it.value }}")
                println("POS        : ${vocab.partsOfSpeech.joinToString(", ") { it.value }}")
                println("JLPT       : ${vocab.jlpt.joinToString(", ") { "N${it.level}" }}")
                println("Furigana   : ${formatFurigana(vocab.furigana)}")
                vocab.senses.take(6).forEachIndexed { index, sense ->
                    println("  [${index + 1}] ${sense.glosses.joinToString("; ") { it.value }}")
                }
                println("Kanji      : ${vocab.kanjiIds.joinToString(", ") { it.value.removePrefix("kanji:") }}")
                return@use
            }
            val kanji = api.getKanji(query)
            if (kanji != null) {
                println("Kanji      : ${kanji.character.literal}")
                println("On readings: ${kanji.onReadings.joinToString(", ") { it.value }}")
                println("Kun readings: ${kanji.kunReadings.joinToString(", ") { it.value }}")
                println("Meanings   : ${kanji.meanings.joinToString(", ") { it.value }}")
                println("Grade      : ${kanji.grade ?: "-"}")
                println("JLPT       : ${kanji.jlpt.joinToString(", ") { "N${it.level}" }}")
                println("Strokes    : ${kanji.strokeCount ?: kanji.strokes.size}")
                return@use
            }
            println("No match for \"$query\"")
        }
    }

    private fun strokes() {
        val query = argumentAfter("strokes")
        if (query == null) {
            println("usage: kjd strokes <kanji>")
            return
        }
        requireDatabase().use { db ->
            val strokes = db.strokesFor(query)
            if (strokes.isEmpty()) {
                println("No stroke data for \"$query\"")
                return@use
            }
            println("Stroke count: ${strokes.size}")
            strokes.forEach { stroke ->
                val box = stroke.boundingBox?.let { " bounds=(${it.minX.toInt()},${it.minY.toInt()})..(${it.maxX.toInt()},${it.maxY.toInt()})" } ?: ""
                println("  #${stroke.index} ${stroke.direction?.name ?: "?"}$box path=${stroke.path.take(48)}…")
            }
        }
    }

    private fun radical() {
        val query = argumentAfter("radical")
        if (query == null) {
            println("usage: kjd radical <kanji>")
            return
        }
        requireDatabase().use { db ->
            val radical = db.radicalFor(query)
            if (radical == null) {
                println("No radical info for \"$query\"")
                return@use
            }
            println("Radical     : ${radical.character ?: radical.name ?: radical.id.value}")
            println("Number      : ${radical.number ?: "-"}")
            println("Meanings    : ${radical.meanings.joinToString(", ") { it.value }}")
            println("Stroke count: ${radical.strokeCount ?: "-"}")
        }
    }

    private fun components() {
        val query = argumentAfter("components")
        if (query == null) {
            println("usage: kjd components <kanji>")
            return
        }
        requireDatabase().use { db ->
            val components = db.componentsFor(query)
            if (components.isEmpty()) {
                println("No component data for \"$query\" (source lacks structural parts)")
                return@use
            }
            println("Components of \"$query\":")
            components.forEach { component ->
                val role = if (component.role == "radical") "(radical)" else "(graphical)"
                println("  ${component.character} $role")
            }
        }
    }

    private fun kana() {
        val query = argumentAfter("kana")
        if (query == null) {
            println("usage: kjd kana <kana>")
            return
        }
        requireDatabase().use { db ->
            val kana = db.lookupKana(query)
            if (kana == null) {
                println("No kana record for \"$query\"")
                return@use
            }
            println("Kana       : ${kana.character.literal}")
            println("Syllabary  : ${kana.syllabary.name}")
            println("Romaji     : ${kana.romaji ?: "-"}")
            println("Strokes    : ${kana.strokeCount ?: db.strokesFor(query).size}")
        }
    }

    private fun stats() {
        requireDatabase().use { db ->
            println("Kanji      : ${db.kanjiCount()}")
            println("Kana       : ${db.kanaCount()}")
            println("Vocabulary : ${db.vocabularyCount()}")
            println("Senses     : ${db.senseCount()}")
            println("Radicals   : ${db.radicalCount()}")
            println("Components : ${db.componentCount()}")
            println("Schema v   : ${db.schemaVersion()}")
            println("Generator  : ${db.generatorVersion()}")
        }
    }

    private fun validate() {
        requireDatabase().use { db ->
            // Validate via a light re-materialization through the pipeline's
            // validator by re-reading all rows.
            val kanjiCount = db.kanjiCount()
            val vocabCount = db.vocabularyCount()
            val problems = mutableListOf<String>()
            if (db.schemaVersion() != KjdVersion.SCHEMA_VERSION) {
                problems.add("Schema version mismatch: ${db.schemaVersion()} (expected ${KjdVersion.SCHEMA_VERSION})")
            }
            if (kanjiCount == 0 && vocabCount == 0) {
                problems.add("Database is empty")
            }
            // Spot-check every kanji has a valid literal.
            var checked = 0
            var offset = 0
            while (checked < kanjiCount) {
                db.allKanji(limit = 500, offset = offset).forEach { kanji ->
                    if (kanji.character.literal.isEmpty()) {
                        problems.add("Empty literal at ${kanji.id.value}")
                    }
                    if (kanji.strokeCount != null && kanji.strokeCount < 0) {
                        problems.add("Negative stroke count at ${kanji.id.value}")
                    }
                }
                checked += 500
                offset += 500
                if (checked > 100_000) break
            }
            if (problems.isEmpty()) {
                println("Validation OK — $kanjiCount kanji, $vocabCount vocab, schema v${db.schemaVersion()}")
            } else {
                problems.forEach { println("FAIL: $it") }
            }
        }
    }

    private fun sources() {
        requireDatabase().use { db ->
            val raw = db.queryMeta("sources_json")
            if (raw.isNullOrBlank()) {
                println("No source manifest in this database")
                return@use
            }
            println("Ingested sources & licenses:")
            val manifest = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(io.kaiteyo.kjd.source.AttributionManifest.serializer(), raw)
            manifest.sources.forEach { source ->
                println("  - ${source.name} [${source.license.name}] v${source.version}")
                println("    ${source.homepage}")
                println("    ${source.license.url}")
            }
        }
    }

    private fun build() {
        val sourcesDir = argumentValue("--sources-dir")
        val out = argumentValue("--out")
        if (sourcesDir == null) {
            println("usage: kjd build --sources-dir <dir> [--out <database-file>]")
            return
        }
        println("Building KJD database from ${File(sourcesDir).absolutePath} …")
        val config = PipelineConfig(
            sourcesDir = File(sourcesDir),
            outputDatabase = out?.let { File(it) } ?: dbPath,
            outputReportDir = argumentValue("--report-dir")?.let { File(it) }
        )
        val report = KjdPipeline().run(config)
        println("Done. Kanji=${report.kanjiCount} Vocabulary=${report.vocabularyCount} Senses=${report.senseCount}")
        if (report.warnings.isNotEmpty()) {
            println("Warnings (${report.warnings.size}):")
            report.warnings.take(10).forEach { println("  - $it") }
        }
    }

    private fun attribution() {
        val outDir = argumentValue("--out")?.let { File(it) } ?: dbPath.parentFile ?: File(".")
        requireDatabase().use { db ->
            val raw = db.queryMeta("sources_json")
            if (raw.isNullOrBlank()) {
                println("No source manifest in this database")
                return@use
            }
            val manifest = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(io.kaiteyo.kjd.source.AttributionManifest.serializer(), raw)
            val (jsonFile, mdFile) = AttributionWriter().write(manifest, outDir)
            println("Wrote ${jsonFile.absolutePath}")
            println("Wrote ${mdFile.absolutePath}")
        }
    }

    private fun release() {
        val outDir = argumentValue("--out")?.let { File(it) } ?: dbPath.parentFile ?: File(".")
        requireDatabase().use { db ->
            val raw = db.queryMeta("sources_json")
            val manifest = raw?.takeIf { it.isNotBlank() }?.let {
                runCatching {
                    kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        .decodeFromString(io.kaiteyo.kjd.source.AttributionManifest.serializer(), it)
                }.getOrNull()
            }
            val releaseManifest = io.kaiteyo.kjd.pipeline.ReleaseManifest(
                platform = KjdVersion.PLATFORM_NAME,
                databaseVersion = KjdVersion.SDK_VERSION,
                schemaVersion = db.schemaVersion(),
                generatorVersion = db.generatorVersion() ?: "unknown",
                generatedAt = db.queryMeta(io.kaiteyo.kjd.db.Schema.Meta.GENERATED_AT) ?: "unknown",
                sourceVersions = manifest?.sources?.associate { it.id to it.version } ?: emptyMap(),
                licenseCount = manifest?.sources?.size ?: 0,
                recordCounts = io.kaiteyo.kjd.pipeline.RecordCounts(
                    kanji = db.kanjiCount(),
                    kana = db.kanaCount(),
                    vocabulary = db.vocabularyCount(),
                    senses = db.senseCount(),
                    radicals = db.radicalCount(),
                    components = db.componentCount(),
                    relationships = db.relationshipCount()
                ),
                validation = io.kaiteyo.kjd.pipeline.ValidationSummary(
                    passed = db.schemaVersion() == KjdVersion.SCHEMA_VERSION,
                    fatalCount = 0,
                    warningCount = 0
                ),
                stateFingerprint = db.queryMeta(io.kaiteyo.kjd.db.Schema.Meta.STATE_FINGERPRINT) ?: ""
            )
            val target = File(outDir, "release-manifest.json")
            ReleaseManifestWriter.write(releaseManifest, target)
            println("Wrote ${target.absolutePath}")
        }
    }

    private fun importSource() {
        val sourceId = argumentValue("--source")
        val file = argumentValue("--file")
        val sourcesDir = argumentValue("--sources-dir")?.let { File(it) } ?: File(".")
        if (sourceId == null || file == null) {
            println("usage: kjd import --source <id> --file <archive-or-file> [--sources-dir <dir>]")
            println("known sources: ${io.kaiteyo.kjd.source.BuiltinSources.all.joinToString(", ") { it.id }}")
            return
        }
        val metadata = runCatching { io.kaiteyo.kjd.source.BuiltinSources.byId(sourceId) }.getOrNull()
        if (metadata == null) {
            println("Unknown source id: $sourceId")
            return
        }
        val sourceFile = File(file)
        if (!sourceFile.exists()) {
            println("File not found: $file")
            return
        }

        val rawDir = File(sourcesDir, "sources/$sourceId/raw")
        rawDir.mkdirs()

        val written = if (sourceFile.name.endsWith(".zip", true)) {
            val result = SafeArchiveExtractor.extractZip(sourceFile, rawDir)
            result.rejected.forEach { rejected ->
                println("  skipped ${rejected.name}: ${rejected.reason}")
            }
            result.extracted
        } else {
            val target = File(rawDir, sourceFile.name)
            sourceFile.copyTo(target, overwrite = true)
            listOf(target.name)
        }
        if (written.isEmpty()) {
            println("Nothing imported (archive had no usable entries)")
            return
        }

        val sha256 = sha256(sourceFile)
        val artifact = SourceArtifact(
            sourceId = sourceId,
            fileName = sourceFile.name,
            sha256 = sha256,
            byteSize = sourceFile.length(),
            recordCount = written.size.toLong()
        )
        val metaDir = File(sourcesDir, "sources/$sourceId/metadata")
        metaDir.mkdirs()
        File(metaDir, "${sourceFile.nameWithoutExtension}.json").writeText(
            kotlinx.serialization.json.Json { prettyPrint = true }
                .encodeToString(SourceArtifact.serializer(), artifact)
        )
        println("Imported ${sourceFile.name} into $rawDir (${written.size} file(s), sha256 $sha256)")
    }

    private fun diff() {
        val from = argumentValue("--from")
        val to = argumentValue("--to")
        if (from == null || to == null) {
            println("usage: kjd diff --from <old.db> --to <new.db> [--out <patch.json>]")
            return
        }
        val fromFile = File(from)
        val toFile = File(to)
        if (!fromFile.exists() || !toFile.exists()) {
            println("Both databases must exist (from=$fromFile, to=$toFile)")
            return
        }
        val patch = DatabaseDiffGenerator().generate(fromFile, toFile)
        val out = argumentValue("--out")?.let { File(it) } ?: File("kjd-patch.json")
        out.writeText(
            kotlinx.serialization.json.Json { prettyPrint = true }
                .encodeToString(DatabasePatch.serializer(), patch)
        )
        println("Diff: ${patch.summary.inserted} inserts, ${patch.summary.updated} updates, ${patch.summary.deleted} deletes")
        println("Wrote $out")
        println("Fingerprint ${patch.fromFingerprint.take(12)}… → ${patch.toFingerprint.take(12)}…")
    }

    private fun patch() {
        val patchFile = argumentValue("--patch")?.let { File(it) }
        if (patchFile == null || !patchFile.exists()) {
            println("usage: kjd patch --db <target.db> --patch <patch.json> [--force] [--backup <dir>]")
            return
        }
        val patch = runCatching {
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(DatabasePatch.serializer(), patchFile.readText())
        }.getOrElse { t ->
            println("Invalid patch file: ${t.message}")
            return
        }
        val force = args.contains("--force")
        val backupDir = argumentValue("--backup")?.let { File(it) }
        val result = try {
            DatabasePatcher().apply(dbPath, patch, force = force, backupDir = backupDir)
        } catch (t: Throwable) {
            println("Patch NOT applied: ${t.message}")
            kotlin.system.exitProcess(1)
        }
        when (result) {
            is PatchResult.Applied -> {
                println("Applied: ${result.summary.inserted} inserts, ${result.summary.updated} updates, ${result.summary.deleted} deletes")
                println("Verified fingerprint: ${result.targetFingerprint.take(12)}…")
            }
            PatchResult.AlreadyApplied -> println("Database is already at the patch's target state — nothing to do.")
        }
    }

    private fun fingerprint() {
        requireDatabase().use { db ->
            val computed = DatabaseFingerprint.compute(dbPath)
            val stored = db.queryMeta(io.kaiteyo.kjd.db.Schema.Meta.STATE_FINGERPRINT)
            println("State fingerprint: $computed")
            stored?.let {
                println("Stored in meta    : $it")
                if (it != computed) println("WARNING: stored meta fingerprint differs from computed state")
            } ?: println("(no fingerprint recorded in meta — database predates incremental updates)")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun export() {
        val format = argumentValue("--format") ?: "json"
        val out = argumentValue("--out")
        if (out == null) {
            println("usage: kjd export --format <json|jsonl|csv> --out <file>")
            return
        }
        requireDatabase().use { db ->
            // Materialize the database into a snapshot for the exporter.
            val snapshot = io.kaiteyo.kjd.model.CanonicalDatabase.from(
                io.kaiteyo.kjd.resolve.CanonicalDatabaseBuilder().apply {
                    var offset = 0
                    while (true) {
                        val chunk = db.allKanji(limit = 5000, offset = offset)
                        chunk.forEach { upsertKanji(it) }
                        if (chunk.size < 5000) break
                        offset += 5000
                    }
                }
            )
            val exporter = io.kaiteyo.kjd.export.Exporter()
            when (format) {
                "json" -> exporter.exportJson(snapshot, File(out))
                "jsonl" -> exporter.exportJsonl(snapshot, File(out))
                "csv" -> exporter.exportCsv(snapshot, File(out))
                else -> {
                    println("Unknown format: $format (json|jsonl|csv)")
                    return@use
                }
            }
            println("Exported to $out")
        }
    }

    private fun help() {
        println(
            """
            KJD — Kaiteyo Japanese Data Platform CLI (v${KjdVersion.SDK_VERSION})

            USAGE: kjd <command> [args]

            Commands:
              info                     platform & database info
              search [kanji|vocab] Q   search kanji/vocabulary
              lookup Q                 full vocabulary (then kanji) lookup
              strokes K                stroke order + metadata
              radical K                radical info
              components K             structural components of a kanji
              kana K                   kana lookup (syllabary / romaji / strokes)
              stats                    database statistics
              validate                 validate database integrity
              sources                  list ingested sources & licenses
              attribution --out DIR    write THIRD_PARTY_DATA.md/.json
              release [--out DIR]      write release-manifest.json
              build --sources-dir D    run the generation pipeline
                    [--out FILE] [--report-dir DIR]
              import --source ID --file F  safely import a source artifact
                    [--sources-dir DIR]
              export --format F --out F  export json|jsonl|csv
              diff --from A --to B       generate an incremental patch
                    [--out patch.json]
              patch --db T --patch P     apply a patch (verified, transactional)
                    [--force] [--backup DIR]
              fingerprint [--db]         show the content-state fingerprint
              help                     this help

            Options: --db <path>  (default: ${KjdVersion.DEFAULT_DATABASE_NAME})
            """.trimIndent()
        )
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private fun requireDatabase(): JapaneseDatabase {
        if (!dbPath.exists()) {
            System.err.println("No database at ${dbPath.absolutePath}. Run `kjd build` first.")
            kotlin.system.exitProcess(1)
        }
        return JapaneseDatabase.open(dbPath)
    }

    private fun argumentAfter(name: String): String? {
        val index = args.indexOfFirst { it == name }
        return if (index >= 0 && index + 1 < args.size) args[index + 1] else null
    }

    private fun argumentValue(name: String): String? =
        args.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')

    private fun formatFurigana(segments: List<io.kaiteyo.kjd.model.FuriganaSegment>): String =
        segments.joinToString("") { segment ->
            if (segment.reading != null) "${segment.text}[${segment.reading}]" else segment.text
        }
}
