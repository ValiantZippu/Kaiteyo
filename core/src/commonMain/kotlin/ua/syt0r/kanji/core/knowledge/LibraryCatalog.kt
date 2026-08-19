package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable

// ============================================================
// LIBRARY — learning material model (spec §29)
// ------------------------------------------------------------
// The Library is the user's learning material, not a generic
// folder: COURSES → LESSONS → COLLECTIONS → ENTRIES, plus
// SAVED/IMPORTED/user collections.
//
// Catalog entries are built from REAL dataset queries:
//   - "Jōyō Grade 1" is the real set of grade-1 kanji
//   - "JLPT N5" is the real set of N5 kanji
//   - a lesson is a curated subset with a real entry list
// No course is fabricated: every collection references actual
// kanji/words from the bundled database.
// ============================================================

@Serializable
enum class LibraryItemKind {
    Kanji,
    Word,
    Sentence,
    Grammar,
    Lesson,
    Course
}

@Serializable
data class LibraryItemRef(
    val kind: LibraryItemKind,
    /** Kanji character / word id / sentence text / grammar pattern id. */
    val ref: String
)

@Serializable
data class LibraryCollection(
    val id: String,
    val title: String,
    val description: String,
    val entries: List<LibraryItemRef>
) {
    val size: Int get() = entries.size
}

@Serializable
data class LibraryLesson(
    val id: String,
    val title: String,
    val description: String,
    val collectionIds: List<String>,
    val order: Int
)

@Serializable
data class LibraryCourse(
    val id: String,
    val title: String,
    val description: String,
    val lessonIds: List<String>,
    val levelId: String? = null
)

/**
 * Real, data-driven starter catalog. The collections reference actual
 * kanji via their classifications — every list is a real dataset query
 * result, never a made-up set.
 */
class LibraryCatalog(
    private val knowledge: KnowledgeRepository
) {

    /**
     * Builds the grade-based collections (Kyōiku 1–6, Jōyō remainder).
     * Each collection contains the real kanji of that grade.
     */
    suspend fun gradeCollections(): List<LibraryCollection> = buildList {
        for (grade in 1..6) {
            val kanji = kanjiForGrade(grade)
            add(
                LibraryCollection(
                    id = "grade-$grade",
                    title = "Kyōiku Grade $grade",
                    description = "The $kanji.size kanji taught in Japanese school grade $grade.",
                    entries = kanji.map { LibraryItemRef(LibraryItemKind.Kanji, it) }
                )
            )
        }
        val joyoRemainder = kanjiForGrade(8)
        add(
            LibraryCollection(
                id = "joyo-remainder",
                title = "Jōyō (remaining set)",
                description = "The ${joyoRemainder.size} non-kyōiku jōyō kanji (grade-8 tag in the dataset).",
                entries = joyoRemainder.map { LibraryItemRef(LibraryItemKind.Kanji, it) }
            )
        )
    }

    /**
     * JLPT collections (N5 → N1), from the real n5…n1 classification tags.
     */
    suspend fun jlptCollections(): List<LibraryCollection> = buildList {
        for (level in 5 downTo 1) {
            val kanji = knowledge.kanjiTags()
                .filterValues { tags -> tags.any { it is KanjiTag.Jlpt && it.level == level } }
                .keys
                .sorted()
            add(
                LibraryCollection(
                    id = "jlpt-n$level",
                    title = "JLPT N$level",
                    description = "${kanji.size} kanji tagged N$level in the bundled dataset.",
                    entries = kanji.map { LibraryItemRef(LibraryItemKind.Kanji, it) }
                )
            )
        }
    }

    /** All starter courses, built from the real collections above. */
    suspend fun courses(): List<LibraryCourse> {
        val gradeCollections = gradeCollections()
        val jlpt = jlptCollections()
        val gradeIds = gradeCollections.map { it.id }
        val jlptIds = jlpt.map { it.id }
        return listOf(
            LibraryCourse(
                id = "kyoiku-course",
                title = "Kyōiku Kanji (Grades 1–6)",
                description = "Work through the ${gradeCollections.sumOf { it.size }} kanji of the Japanese school curriculum, grade by grade.",
                lessonIds = gradeIds
            ),
            LibraryCourse(
                id = "jlpt-course",
                title = "JLPT Kanji (N5 → N1)",
                description = "The ${jlpt.sumOf { it.size }} kanji tagged for the JLPT, from N5 upward.",
                lessonIds = jlptIds
            )
        )
    }

    suspend fun allCollections(): List<LibraryCollection> =
        gradeCollections() + jlptCollections()

    /** Resolves a collection by id, or null when it doesn't exist. */
    suspend fun collection(id: String): LibraryCollection? =
        allCollections().firstOrNull { it.id == id }

    /** Kanji characters in a collection (kanji-kind entries only, in order). */
    suspend fun kanjiIn(collectionId: String): List<String> {
        val collection = collection(collectionId) ?: return emptyList()
        return collection.entries
            .filter { it.kind == LibraryItemKind.Kanji }
            .map { it.ref }
    }

    private suspend fun kanjiForGrade(grade: Int): List<String> =
        knowledge.kanjiTags()
            .filterValues { tags -> tags.any { it is KanjiTag.Grade && it.number == grade } }
            .keys
            .sorted()
}
