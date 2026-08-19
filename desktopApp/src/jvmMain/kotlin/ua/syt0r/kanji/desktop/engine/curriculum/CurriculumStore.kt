package ua.syt0r.kanji.desktop.engine.curriculum

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ============================================
// KAITEYO CURRICULUM — STORE
// Persists the learner's progress (active
// course/lesson + completed objectives) to
// ~/.kaiteyo/curriculum.json. Corruption-safe:
// a broken payload resets to a fresh progress.
// ============================================

class CurriculumStore(
    private val progressFile: File = File(
        System.getProperty("user.home"),
        ".kaiteyo/curriculum.json"
    )
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): CurriculumProgress {
        if (!progressFile.exists()) return CurriculumProgress()
        return runCatching {
            json.decodeFromString<CurriculumProgress>(progressFile.readText())
        }.getOrDefault(CurriculumProgress())
    }

    fun save(progress: CurriculumProgress) {
        runCatching {
            progressFile.parentFile?.mkdirs()
            progressFile.writeText(json.encodeToString(progress))
        }
    }
}
