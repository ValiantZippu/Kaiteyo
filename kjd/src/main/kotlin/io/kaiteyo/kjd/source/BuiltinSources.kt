package io.kaiteyo.kjd.source

/**
 * Registry of the built-in data sources. License details reflect the current
 * published terms of each project (verify before distributing a bundled
 * release — see the redistribution notes).
 */
object BuiltinSources {

    private val kanjiVg = SourceMetadata(
        id = SourceIds.KANJIVG,
        name = "KanjiVG",
        homepage = "https://kanjivg.tagaini.net/",
        license = License(
            id = "cc-by-sa-3.0",
            name = "Creative Commons Attribution-ShareAlike 3.0",
            url = "https://creativecommons.org/licenses/by-sa/3.0/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = true
        ),
        version = "kanjivg-2022-01-21",
        retrievedAt = "",
        attribution = "KanjiVG © Ulrich Apel, licensed under CC BY-SA 3.0",
        redistributionNotes = "Derived works must be shared under the same license and credit KanjiVG.",
        modificationNotes = "Stroke paths extracted and normalized into canonical stroke records.",
        sourceUrl = "https://github.com/KanjiVG/kanjivg/releases"
    )

    private val kanjidic = SourceMetadata(
        id = SourceIds.KANJIDIC,
        name = "KANJIDIC",
        homepage = "https://www.edrdg.org/kanjidic/",
        license = License(
            id = "cc-by-sa-3.0",
            name = "Creative Commons Attribution-ShareAlike 3.0",
            url = "https://creativecommons.org/licenses/by-sa/3.0/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = true
        ),
        version = "kanjidic2-2024-01",
        retrievedAt = "",
        attribution = "KANJIDIC © The Electronic Dictionary Research and Development Group (EDRDG)",
        redistributionNotes = "CC BY-SA 3.0; attribution to EDRDG required.",
        modificationNotes = "Readings, meanings and classifications extracted and normalized.",
        sourceUrl = "https://www.edrdg.org/kanjidic/kanjidic2.xml.gz"
    )

    private val jmdict = SourceMetadata(
        id = SourceIds.JMDICT,
        name = "JMdict",
        homepage = "https://www.edrdg.org/jmdict/",
        license = License(
            id = "cc-by-sa-4.0",
            name = "Creative Commons Attribution-ShareAlike 4.0",
            url = "https://creativecommons.org/licenses/by-sa/4.0/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = true
        ),
        version = "jmdict-2024-01",
        retrievedAt = "",
        attribution = "JMdict © Electronic Dictionary Research and Development Group (EDRDG)",
        redistributionNotes = "CC BY-SA 4.0; attribution to EDRDG required.",
        modificationNotes = "Entries, senses and glosses extracted and normalized into the canonical vocabulary model.",
        sourceUrl = "https://www.edrdg.org/jmdict/jmdict.xml.gz"
    )

    private val jmdictFurigana = SourceMetadata(
        id = SourceIds.JMDICT_FURIGANA,
        name = "JmdictFurigana",
        homepage = "https://github.com/Doublevil/JmdictFurigana",
        license = License(
            id = "cc-by-sa-4.0",
            name = "Creative Commons Attribution-ShareAlike 4.0",
            url = "https://creativecommons.org/licenses/by-sa/4.0/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = true
        ),
        version = "jmdict_furigana-2024",
        retrievedAt = "",
        attribution = "JmdictFurigana © Doublevil, based on JMdict",
        redistributionNotes = "CC BY-SA 4.0; derived from JMdict.",
        modificationNotes = "Furigana segmentation normalized into structured segments.",
        sourceUrl = "https://github.com/Doublevil/JmdictFurigana/releases"
    )

    private val tanosJlpt = SourceMetadata(
        id = SourceIds.TANOS_JLPT,
        name = "Tanos JLPT lists",
        homepage = "https://www.tanos.co.uk/jlpt/",
        license = License(
            id = "custom-free",
            name = "Free to use with attribution",
            url = "https://www.tanos.co.uk/jlpt/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = false
        ),
        version = "tanos-jlpt-2024",
        retrievedAt = "",
        attribution = "JLPT lists © Jonathan Waller (tanos.co.uk)",
        redistributionNotes = "Free to use with attribution; see the source site for current terms.",
        modificationNotes = "Level classifications extracted; not assumed complete or authoritative beyond the source scope.",
        sourceUrl = "https://www.tanos.co.uk/jlpt/"
    )

    private val leedsFrequency = SourceMetadata(
        id = SourceIds.LEEDS_FREQUENCY,
        name = "Leeds Internet Corpus frequency data",
        homepage = "http://corpus.leeds.ac.uk/list/plain/",
        license = License(
            id = "research-free",
            name = "Free for research/education with attribution",
            url = "http://corpus.leeds.ac.uk/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = false
        ),
        version = "leeds-japanese-2024",
        retrievedAt = "",
        attribution = "Frequency data © Centre for Translation Studies, University of Leeds",
        redistributionNotes = "Verify current terms before bundling; primarily a ranking methodology reference.",
        modificationNotes = "Rank positions preserved; methodology retained in frequency records.",
        sourceUrl = "http://corpus.leeds.ac.uk/list/plain/japanese.txt"
    )

    private val yomichanJlptVocab = SourceMetadata(
        id = SourceIds.YOMICHAN_JLPT_VOCAB,
        name = "yomichan-jlpt-vocab",
        homepage = "https://github.com/stephenmk/yomichan-jlpt-vocab",
        license = License(
            id = "cc-by-sa-4.0",
            name = "Creative Commons Attribution-ShareAlike 4.0",
            url = "https://creativecommons.org/licenses/by-sa/4.0/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = true
        ),
        version = "yomichan-jlpt-vocab-2024",
        retrievedAt = "",
        attribution = "yomichan-jlpt-vocab © Stephen M. Kellett (data compiled from public lists)",
        redistributionNotes = "CC BY-SA 4.0; verify provenance of the compiled lists.",
        modificationNotes = "JLPT tags extracted as a secondary classification source (Tanos remains canonical).",
        sourceUrl = "https://github.com/stephenmk/yomichan-jlpt-vocab"
    )

    val all: List<SourceMetadata> = listOf(
        kanjiVg, kanjidic, jmdict, jmdictFurigana, tanosJlpt, leedsFrequency, yomichanJlptVocab
    )

    fun byId(id: String): SourceMetadata =
        all.firstOrNull { it.id == id } ?: error("Unknown source id: $id")
}
