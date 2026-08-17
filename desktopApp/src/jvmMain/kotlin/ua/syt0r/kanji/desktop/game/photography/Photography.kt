package ua.syt0r.kanji.desktop.game.photography

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

// ============================================================
// PHOTOGRAPHY (spec §43-45, inspired by the Shashingo direction)
// Enter camera mode → frame → capture → the scene's objects are
// tagged → "Found 4 vocabulary items". Album is persisted.
// ============================================================

@Serializable
enum class PhotoCategory(val label: String) {
    Animals("Animals"), Food("Food"), Places("Places"), Transportation("Transportation"),
    Nature("Nature"), Objects("Objects"), People("People"), Signs("Signs")
}

/** A vocabulary tag found inside a photo. */
@Serializable
data class PhotoTag(
    val knowledgeId: String,
    val headword: String,
    val reading: String,
    val meaning: String,
    /** Where the object sat in world space (framing helper). */
    val worldPosition: Vec2? = null
)

@Serializable
data class Photo(
    val id: String,
    val title: String = "",
    val category: PhotoCategory = PhotoCategory.Places,
    val locationId: String? = null,
    val tags: List<PhotoTag> = emptyList(),
    val takenAt: String = "",
    val regionId: String = "hamanaka"
)

/** Persisted album, grouped by category. */
@Serializable
data class AlbumData(
    val photos: List<Photo> = emptyList(),
    val categories: List<PhotoCategory> = PhotoCategory.entries
)

/**
 * The photo camera. In the slice it's a viewfinder overlay that scans world
 * objects inside the frame rectangle; in the 3D integration it drives the
 * first-person camera (spec §12-13). Both go through [PhotoCamera.capture].
 */
class PhotoCamera {

    var active by mutableStateOf(false)

    /** Viewfinder centre in world space. */
    var focus: Vec2 = Vec2.Zero

    /** Half-extent of the frame in world units. */
    var frameHalfWidth: Float = 140f
    var frameHalfHeight: Float = 100f

    var zoomLevel: Float = 1.2f

    fun enter() {
        active = true
    }

    fun exit() {
        active = false
    }

    fun frameRect(): ua.syt0r.kanji.desktop.game.engine.geom.Rect =
        ua.syt0r.kanji.desktop.game.engine.geom.Rect(
            focus.x - frameHalfWidth,
            focus.y - frameHalfHeight,
            frameHalfWidth * 2f,
            frameHalfHeight * 2f
        )

    /** Objects inside the frame get tagged; returns the discovered tags. */
    fun capture(
        frameObjects: List<PhotoSubject>,
        locationId: String?,
        nowIso: String,
        regionId: String
    ): Photo {
        val tags = frameObjects.mapNotNull { subject ->
            subject.knowledgeNode?.let { node ->
                PhotoTag(
                    knowledgeId = node.id,
                    headword = node.headword,
                    reading = node.reading,
                    meaning = node.meaning,
                    worldPosition = subject.position
                )
            }
        }.distinctBy { it.knowledgeId }
        val categories = tags.mapNotNull { tag -> categoryFor(tag.headword) }.distinct()
        return Photo(
            id = "photo-${System.currentTimeMillis()}",
            title = tags.firstOrNull()?.headword ?: "Hamanaka",
            category = categories.firstOrNull() ?: PhotoCategory.Places,
            locationId = locationId,
            tags = tags,
            takenAt = nowIso,
            regionId = regionId
        )
    }

    private fun categoryFor(headword: String): PhotoCategory? = when (headword) {
        "猫", "犬", "鳥" -> PhotoCategory.Animals
        "海", "空", "船", "砂", "浜", "灯台" -> PhotoCategory.Nature
        "電車", "駅", "自転車", "バス" -> PhotoCategory.Transportation
        "店", "コンビニ", "看板", "メニュー" -> PhotoCategory.Signs
        "水", "お茶", "ジュース" -> PhotoCategory.Food
        "人", "店員", "おじいさん" -> PhotoCategory.People
        else -> null
    }
}

/** A photoable subject in the world (object/NPC with optional knowledge). */
data class PhotoSubject(
    val id: String,
    val position: Vec2,
    val label: String,
    val knowledgeNode: ua.syt0r.kanji.desktop.game.learning.KnowledgeNode? = null
)

/** Album store (persisted through the save file). */
class PhotoAlbum {
    var photos by mutableStateOf<List<Photo>>(emptyList())
        private set

    fun add(photo: Photo) {
        photos = (photos + photo).sortedByDescending { it.takenAt }
    }

    fun byCategory(category: PhotoCategory): List<Photo> = photos.filter { it.category == category }

    fun allTags(): List<PhotoTag> = photos.flatMap { it.tags }

    fun snapshot(): AlbumData = AlbumData(photos = photos)

    fun restore(data: AlbumData) {
        photos = data.photos
    }
}
