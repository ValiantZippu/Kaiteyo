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

    // === FREQUENCY SOURCES ===
    private val netflixFrequency = SourceMetadata(
        id = SourceIds.NETFLIX_FREQUENCY,
        name = "Netflix Japanese Frequency List",
        homepage = "https://github.com/pciavolici/Netflix-Japanese-Subtitle-Frequency-List",
        license = License(
            id = "custom-free",
            name = "Free to use",
            url = "https://github.com/pciavolici/Netflix-Japanese-Subtitle-Frequency-List",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "netflix-2024",
        retrievedAt = "",
        attribution = "Netflix Japanese Frequency List © OhTalkWho オタク (Dave Doebrick)",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Word frequency extracted from Netflix Japanese subtitles.",
        sourceUrl = "https://github.com/pciavolici/Netflix-Japanese-Subtitle-Frequency-List"
    )

    private val kempsonFrequency = SourceMetadata(
        id = SourceIds.KEMPSON_FREQUENCY,
        name = "Chris Kempson Japanese Subtitles Frequency",
        homepage = "https://github.com/chriskempson/japanese-subtitles-word-frequency-list",
        license = License(
            id = "mit",
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "kempson-2024",
        retrievedAt = "",
        attribution = "Japanese Subtitles Word & Kanji Frequency Lists © Chris Kempson",
        redistributionNotes = "MIT License; attribution appreciated.",
        modificationNotes = "Word and kanji frequency from Japanese subtitles.",
        sourceUrl = "https://github.com/chriskempson/japanese-subtitles-word-frequency-list"
    )

    private val kandrac2242 = SourceMetadata(
        id = SourceIds.KANDRAC_2242,
        name = "Patrick Kandrac 2242 Kanji Frequency",
        homepage = "https://forum.koohii.com/viewtopic.php?id=16394",
        license = License(
            id = "public",
            name = "Public",
            url = "",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "kandrac-2024",
        retrievedAt = "",
        attribution = "2242 Kanji Frequency List © Patrick Kandrac, sources: Google Kanji Data, KUF, MCD, 文化庁",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Frequency ranking for 2242 kanji from multiple sources.",
        sourceUrl = "https://forum.koohii.com/viewtopic.php?id=16394"
    )

    private val nukemarineRtk = SourceMetadata(
        id = SourceIds.NUKEMARINE_RTK,
        name = "Nukemarine RTK Frequency Groups",
        homepage = "https://www.reddit.com/r/LearnJapanese/",
        license = License(
            id = "public",
            name = "Public",
            url = "",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "nukemarine-2024",
        retrievedAt = "",
        attribution = "Kanji Frequency Report and RTK Frequency Groups © Nukemarine",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Frequency groups organized by RTK order.",
        sourceUrl = "https://www.reddit.com/r/LearnJapanese/"
    )

    private val yatskovWikipedia = SourceMetadata(
        id = SourceIds.YATSKOV_WIKIPEDIA,
        name = "Alex Yatskov Wikipedia Kanji Frequency",
        homepage = "https://github.com/yatskov",
        license = License(
            id = "public",
            name = "Public",
            url = "",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "yatskov-2024",
        retrievedAt = "",
        attribution = "Wikipedia Kanji Frequency Report © Alex Yatskov",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Kanji frequency extracted from Japanese Wikipedia.",
        sourceUrl = "https://github.com/yatskov"
    )

    private val girardiWordFreq = SourceMetadata(
        id = SourceIds.GIRARDI_WORD_FREQ,
        name = "Alexandre Girardi Word Frequency",
        homepage = "http://ftp.monash.edu.au/pub/nihongo/",
        license = License(
            id = "public-domain",
            name = "Public Domain",
            url = "http://ftp.monash.edu.au/pub/nihongo/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "girardi-2024",
        retrievedAt = "",
        attribution = "Word frequency list © Alexandre Girardi (public domain, Monash FTP Archive)",
        redistributionNotes = "Public domain; attribution appreciated.",
        modificationNotes = "Word frequency from Japanese text corpus.",
        sourceUrl = "http://ftp.monash.edu.au/pub/nihongo/"
    )

    private val shpikaKanjiKeys = SourceMetadata(
        id = SourceIds.SHPIKA_KANJI_KEYS,
        name = "Kanji Keys",
        homepage = "https://github.com/dshpika",
        license = License(
            id = "cc-by-4.0",
            name = "Creative Commons Attribution 4.0",
            url = "https://creativecommons.org/licenses/by/4.0/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "shpika-2024",
        retrievedAt = "",
        attribution = "Kanji Keys © Dmitry Shpika, licensed under CC BY 4.0",
        redistributionNotes = "CC BY 4.0; attribution required.",
        modificationNotes = "Kanji metadata and frequency data.",
        sourceUrl = "https://github.com/dshpika"
    )

    private val topoKanji = SourceMetadata(
        id = SourceIds.TOPOKANJI,
        name = "TopoKanji",
        homepage = "https://github.com/dshpika/TopoKanji",
        license = License(
            id = "cc-by-4.0",
            name = "Creative Commons Attribution 4.0",
            url = "https://creativecommons.org/licenses/by/4.0/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "topokanji-2024",
        retrievedAt = "",
        attribution = "TopoKanji © Dmitry Shpika, uses CJK Decompositions Data",
        redistributionNotes = "CC BY 4.0; attribution required.",
        modificationNotes = "Kanji decomposition and frequency from multiple sources.",
        sourceUrl = "https://github.com/dshpika/TopoKanji"
    )

    // === STRUCTURAL DATA ===
    private val cjkDecompositions = SourceMetadata(
        id = SourceIds.CJK_DECOMPOSITIONS,
        name = "CJK Decompositions Data",
        homepage = "https://github.com/nieldlr/CJK-Decompositions",
        license = License(
            id = "public-domain",
            name = "Public Domain",
            url = "",
            allowsRedistribution = true,
            attributionRequired = false
        ),
        version = "cjk-decomp-2024",
        retrievedAt = "",
        attribution = "CJK Decompositions Data (public domain)",
        redistributionNotes = "Public domain.",
        modificationNotes = "Kanji component decomposition data.",
        sourceUrl = "https://github.com/nieldlr/CJK-Decompositions"
    )

    private val bunkacho = SourceMetadata(
        id = SourceIds.BUNKACHO,
        name = "文化庁 (Agency for Cultural Affairs)",
        homepage = "https://www.bunka.go.jp/",
        license = License(
            id = "government",
            name = "Japanese Government (public)",
            url = "https://www.bunka.go.jp/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "bunkacho-2024",
        retrievedAt = "",
        attribution = "Official kanji data © Japanese Agency for Cultural Affairs (文化庁)",
        redistributionNotes = "Government data; attribution required.",
        modificationNotes = "Jōyō kanji list, jinmeiyō kanji, educational kanji classifications.",
        sourceUrl = "https://www.bunka.go.jp/kokugo_nihongo/sisaku/joho/j家装字悉.csv/"
    )

    private val kanjidatabase = SourceMetadata(
        id = SourceIds.KANJIDATABASE,
        name = "kanjidatabase.com",
        homepage = "https://kanjidatabase.com/",
        license = License(
            id = "custom-free",
            name = "Free to use",
            url = "https://kanjidatabase.com/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "kanjidb-2024",
        retrievedAt = "",
        attribution = "kanjidatabase.com",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Supplementary kanji metadata.",
        sourceUrl = "https://kanjidatabase.com/"
    )

    private val davidGouveia = SourceMetadata(
        id = SourceIds.DAVID_GOUVEIA,
        name = "David Gouveia Kanji Data",
        homepage = "https://github.com/davidgouveia",
        license = License(
            id = "public",
            name = "Public",
            url = "",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "gouveia-2024",
        retrievedAt = "",
        attribution = "Kanji Data © David Gouveia",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Supplementary kanji metadata.",
        sourceUrl = "https://github.com/davidgouveia"
    )

    // === LEARNING METADATA ===
    private val usagiChanPhonetics = SourceMetadata(
        id = SourceIds.USAGI_CHAN_PHONETICS,
        name = "Usagi Chan Kanji Phonetics Deck",
        homepage = "https://ankiweb.net/shared/info/1218648935",
        license = License(
            id = "cc-by-sa-4.0",
            name = "Creative Commons Attribution-ShareAlike 4.0",
            url = "https://creativecommons.org/licenses/by-sa/4.0/",
            allowsRedistribution = true,
            attributionRequired = true,
            shareAlike = true
        ),
        version = "usagi-chan-2024",
        retrievedAt = "",
        attribution = "Kanji Phonetics Deck © shoui520",
        redistributionNotes = "CC BY-SA 4.0; attribution required.",
        modificationNotes = "Phonetic component groups for kanji.",
        sourceUrl = "https://ankiweb.net/shared/info/1218648935"
    )

    private val shirabeJlpt = SourceMetadata(
        id = SourceIds.SHIRABE_JLPT,
        name = "Shirabe Jisho JLPT Lists",
        homepage = "https://www.shirabejisho.com/",
        license = License(
            id = "public",
            name = "Public",
            url = "https://www.shirabejisho.com/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "shirabe-jlpt-2024",
        retrievedAt = "",
        attribution = "JLPT lists from Shirabe Jisho",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "JLPT kanji classification.",
        sourceUrl = "https://www.shirabejisho.com/"
    )

    private val shirabeCommon = SourceMetadata(
        id = SourceIds.SHIRABE_COMMON,
        name = "Shirabe Jisho Common Words",
        homepage = "https://www.shirabejisho.com/",
        license = License(
            id = "public",
            name = "Public",
            url = "https://www.shirabejisho.com/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "shirabe-common-2024",
        retrievedAt = "",
        attribution = "Common Words list from Shirabe Jisho",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Common Japanese words list.",
        sourceUrl = "https://www.shirabejisho.com/"
    )

    private val kanjiApi = SourceMetadata(
        id = SourceIds.KANJI_API,
        name = "kanjiapi.dev",
        homepage = "https://kanjiapi.dev/",
        license = License(
            id = "public",
            name = "Public",
            url = "https://kanjiapi.dev/",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "kanjiapi-2024",
        retrievedAt = "",
        attribution = "kanjiapi.dev (uses EDICT, KANJIDIC)",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "REST API data for kanji readings, meanings, examples.",
        sourceUrl = "https://kanjiapi.dev/"
    )

    private val kanjiSchool = SourceMetadata(
        id = SourceIds.KANJI_SCHOOL,
        name = "Kanji School",
        homepage = "https://github.com/drewdrawsws/kanji-school",
        license = License(
            id = "public",
            name = "Public",
            url = "",
            allowsRedistribution = true,
            attributionRequired = true
        ),
        version = "kanji-school-2024",
        retrievedAt = "",
        attribution = "Kanji School © Drew Edwards, data from Jisho.org/KANJIDIC",
        redistributionNotes = "Free to use with attribution.",
        modificationNotes = "Kanji data from Jisho.org (KANJIDIC-derived).",
        sourceUrl = "https://github.com/drewdrawsws/kanji-school"
    )

    val all: List<SourceMetadata> = listOf(
        kanjiVg, kanjidic, jmdict, jmdictFurigana, tanosJlpt, leedsFrequency, yomichanJlptVocab,
        // Frequency sources
        netflixFrequency, kempsonFrequency, kandrac2242, nukemarineRtk, yatskovWikipedia,
        girardiWordFreq, shpikaKanjiKeys, topoKanji,
        // Structural data
        cjkDecompositions, bunkacho, kanjidatabase, davidGouveia,
        // Learning metadata
        usagiChanPhonetics, shirabeJlpt, shirabeCommon, kanjiApi, kanjiSchool
    )

    fun byId(id: String): SourceMetadata =
        all.firstOrNull { it.id == id } ?: error("Unknown source id: $id")
}
