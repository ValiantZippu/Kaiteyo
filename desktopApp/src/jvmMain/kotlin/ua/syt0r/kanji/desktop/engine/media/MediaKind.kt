package ua.syt0r.kanji.desktop.engine.media

import java.io.File

// ============================================
// KAITEYO MEDIA KIND
// Coarse classification of a media source. The
// BackendManager routes Audio to the platform
// audio backend and Video to VLC/MPV; the library
// and UI use it for icons and grouping.
// ============================================

enum class MediaKind {
    Video,
    Audio;

    companion object {
        private val videoExtensions = setOf(
            "mp4", "mkv", "webm", "avi", "mov", "wmv", "flv", "m4v", "mpg", "mpeg",
            "ts", "m2ts", "3gp", "ogv", "vob", "divx", "rmvb"
        )
        private val audioExtensions = setOf(
            "mp3", "wav", "flac", "aac", "ogg", "oga", "opus", "m4a", "wma",
            "aiff", "aif", "amr", "ac3", "mka", "mid", "midi"
        )

        /** Media kind for a file, or null when the extension is not recognized media. */
        fun of(file: File): MediaKind? {
            val ext = file.extension.lowercase()
            return when {
                ext in videoExtensions -> Video
                ext in audioExtensions -> Audio
                else -> null
            }
        }

        /** Media kind from a URL's path extension (query strings ignored). */
        fun fromUrl(url: String): MediaKind? {
            val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            return when {
                ext in videoExtensions -> Video
                ext in audioExtensions -> Audio
                else -> null
            }
        }
    }
}