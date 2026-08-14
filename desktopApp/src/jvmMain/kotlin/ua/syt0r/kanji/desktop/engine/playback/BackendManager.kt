package ua.syt0r.kanji.desktop.engine.playback

import java.io.File
import java.util.concurrent.TimeUnit

// ============================================
// BACKEND MANAGER
// Probes the system for VLC / mpv installations,
// creates the right backend for a media kind,
// owns their lifecycle (single instance, cleanup
// on shutdown) and powers the Settings → Media
// "Test backend" actions.
// ============================================

data class BackendDetection(
    val executable: String? = null,
    val libraryDir: String? = null,
    val version: String = ""
) {
    val found: Boolean get() = executable != null
}

class BackendManager {

    // ------------------------------------------------------------
    // Detection
    // ------------------------------------------------------------

    fun probeVlc(): BackendProbe {
        val det = detectVlc()
        return if (det.found) {
            BackendProbe(BackendKind.Vlc, true, det.version, det.executable ?: "", "VLC found")
        } else {
            BackendProbe(BackendKind.Vlc, false, message = installHint("VLC", "https://www.videolan.org/vlc/"))
        }
    }

    fun probeMpv(): BackendProbe {
        val det = detectMpv()
        return if (det.found) {
            BackendProbe(BackendKind.Mpv, true, det.version, det.executable ?: "", "mpv found")
        } else {
            BackendProbe(BackendKind.Mpv, false, message = installHint("mpv", "https://mpv.io/installation/"))
        }
    }

    private fun installHint(name: String, url: String): String =
        "$name is not installed. Install it and restart Kaiteyo, or set the path in Settings → Media. ($url)"

    fun detectVlc(): BackendDetection {
        System.getenv("KAITEYO_VLC_PATH")?.takeIf { it.isNotBlank() }?.let { path ->
            val f = File(path)
            if (f.exists()) return BackendDetection(f.absolutePath, vlcLibDir(f.absolutePath), versionOf(f.absolutePath))
        }
        val candidates = mutableListOf<String>()
        val os = System.getProperty("os.name", "").lowercase()
        when {
            os.contains("win") -> {
                listOf(
                    "C:\\Program Files\\VideoLAN\\VLC\\vlc.exe",
                    "C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe"
                ).forEach { candidates.add(it) }
            }
            os.contains("mac") -> {
                candidates.add("/Applications/VLC.app/Contents/MacOS/VLC")
            }
            else -> {
                findInPath(listOf("vlc", "cvlc"))?.let { candidates.add(it) }
                candidates.add("/usr/bin/vlc")
                candidates.add("/usr/local/bin/vlc")
            }
        }
        val found = candidates.firstOrNull { File(it).canExecute() } ?: return BackendDetection()
        return BackendDetection(found, vlcLibDir(found), versionOf(found))
    }

    /** Best-guess libvlc directory so VLCJ can find the native libraries. */
    private fun vlcLibDir(executable: String): String? {
        val exe = File(executable)
        val os = System.getProperty("os.name", "").lowercase()
        return when {
            os.contains("mac") -> {
                // /Applications/VLC.app/Contents/MacOS/VLC -> .../Contents/MacOS/lib
                File(exe.parentFile, "lib").takeIf { it.isDirectory }?.absolutePath
            }
            os.contains("win") -> exe.parentFile?.absolutePath
            else -> {
                // Debian/Ubuntu: /usr/lib/<triplet>/vlc ; Fedora: /usr/lib64/vlc ; flatpak: /app/lib/vlc
                listOf(
                    File(exe.parentFile, "lib/vlc"),
                    File("/usr/lib/vlc"),
                    File("/usr/lib64/vlc"),
                    File("/usr/local/lib/vlc"),
                    File("/snap/vlc/current/usr/lib/vlc"),
                    File(exe.parentFile, "lib")
                ).firstOrNull { it.isDirectory }?.absolutePath
            }
        }
    }

    fun detectMpv(): BackendDetection {
        System.getenv("KAITEYO_MPV_PATH")?.takeIf { it.isNotBlank() }?.let { path ->
            val f = File(path)
            if (f.exists()) return BackendDetection(f.absolutePath, version = versionOf(f.absolutePath))
        }
        val candidates = mutableListOf<String>()
        val os = System.getProperty("os.name", "").lowercase()
        when {
            os.contains("win") -> {
                listOf(
                    "C:\\Program Files\\mpv\\mpv.exe",
                    "C:\\Program Files\\mpv-x86_64\\mpv.exe"
                ).forEach { candidates.add(it) }
            }
            else -> {
                findInPath(listOf("mpv"))?.let { candidates.add(it) }
                candidates.add("/usr/bin/mpv")
                candidates.add("/usr/local/bin/mpv")
                candidates.add("/opt/homebrew/bin/mpv")
            }
        }
        val found = candidates.firstOrNull { File(it).canExecute() } ?: return BackendDetection()
        return BackendDetection(found, version = versionOf(found))
    }

    private fun findInPath(names: List<String>): String? {
        val path = System.getenv("PATH") ?: return null
        return path.split(File.pathSeparator).mapNotNull { dir ->
            names.mapNotNull { name ->
                val f = File(dir, name)
                if (f.canExecute()) f.absolutePath else null
            }.firstOrNull()
        }.firstOrNull()
    }

    private fun versionOf(executable: String): String = runCatching {
        val pb = ProcessBuilder(executable, "--version").redirectErrorStream(true)
        val p = pb.start()
        val out = p.inputStream.bufferedReader().use { it.readLine() }
        p.waitFor(1500, TimeUnit.MILLISECONDS)
        p.destroy()
        out?.trim().orEmpty()
    }.getOrDefault("")

    // ------------------------------------------------------------
    // Backend creation + lifecycle
    // ------------------------------------------------------------

    fun createVlcBackend(): VlcBackend? {
        val det = detectVlc()
        if (!det.found) return null
        return VlcBackend(libDir = det.libraryDir, vlcPath = det.executable)
    }

    fun createMpvBackend(): MpvBackend? {
        val det = detectMpv()
        if (!det.found) return null
        return MpvBackend(mpvExecutable = det.executable!!)
    }

    val audioBackend = AudioBackend()

    /** Singleton live backends — one per kind, recreated when a probe changes. */
    @Volatile private var vlcBackend: VlcBackend? = null
    @Volatile private var mpvBackend: MpvBackend? = null

    /**
     * Best backend for the media kind, honouring user preference.
     * Returns null when no video backend is installed — the caller then
     * reports a clear, actionable error instead of silently degrading.
     */
    fun backendFor(
        kind: ua.syt0r.kanji.desktop.engine.media.MediaKind,
        preferVlc: Boolean = true
    ): PlaybackBackend? {
        if (kind == ua.syt0r.kanji.desktop.engine.media.MediaKind.Audio) return audioBackend
        if (preferVlc) {
            vlcBackend?.let { return it }
            vlcBackend = createVlcBackend()
            vlcBackend?.let { return it }
        }
        mpvBackend?.let { return it }
        mpvBackend = createMpvBackend()
        mpvBackend?.let { return it }
        return null
    }

    /** Re-probe and swap the VLC singleton (used after backend installation). */
    fun refreshVlc(): VlcBackend? {
        vlcBackend?.close()
        vlcBackend = createVlcBackend()
        return vlcBackend
    }

    fun refreshMpv(): MpvBackend? {
        mpvBackend?.close()
        mpvBackend = createMpvBackend()
        return mpvBackend
    }

    fun activeBackends(): List<PlaybackBackend> = listOfNotNull(vlcBackend, mpvBackend, audioBackend)

    /** Shut down every owned child process / native surface. */
    fun shutdownAll() {
        vlcBackend?.close()
        mpvBackend?.close()
        audioBackend.close()
        vlcBackend = null
        mpvBackend = null
    }

    companion object {
        fun isWindows(): Boolean = System.getProperty("os.name", "").lowercase().contains("win")
    }
}
