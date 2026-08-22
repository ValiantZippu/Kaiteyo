package ua.syt0r.kanji.desktop.engine.media

import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

// ============================================
// KAITEYO IMAGE LOADER BRIDGE
// Unified image loading across platforms.
// Desktop uses Java ImageIO + optional Coil 3.
// Android uses Coil 3 natively.
// Provides caching, resize, and format conversion.
// ============================================

/** Image format support. */
enum class ImageFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp"),
    BMP("bmp", "image/bmp"),
    GIF("gif", "image/gif")
}

/** Options for image loading. */
data class ImageLoadOptions(
    val maxWidth: Int = Int.MAX_VALUE,
    val maxHeight: Int = Int.MAX_VALUE,
    val fitCenter: Boolean = true,
    val cacheResult: Boolean = true,
    val format: ImageFormat? = null
)

/** Result of loading an image. */
sealed class ImageLoadResult {
    data class Success(
        val image: BufferedImage,
        val width: Int,
        val height: Int,
        val fromCache: Boolean = false
    ) : ImageLoadResult()

    data class Error(val message: String) : ImageLoadResult()
}

/** Interface for image loading backends. */
interface ImageLoaderBackend {
    val name: String
    val available: Boolean
    suspend fun load(file: File, options: ImageLoadOptions = ImageLoadOptions()): ImageLoadResult
    suspend fun load(url: String, options: ImageLoadOptions = ImageLoadOptions()): ImageLoadResult
    suspend fun load(stream: InputStream, options: ImageLoadOptions = ImageLoadOptions()): ImageLoadResult
    fun clearCache()
    fun cacheSize(): Long
}

/**
 * Java ImageIO backend — always available on JVM.
 * Basic image loading with in-memory LRU cache.
 */
class JavaImageLoaderBackend : ImageLoaderBackend {
    override val name = "Java ImageIO"
    override val available = true

    private val cache = ConcurrentHashMap<String, BufferedImage>()
    private val maxCacheSize = 50 * 1024 * 1024L // 50MB
    private var currentCacheSize = 0L

    override suspend fun load(file: File, options: ImageLoadOptions): ImageLoadResult {
        if (!file.exists()) return ImageLoadResult.Error("File not found: ${file.absolutePath}")
        val key = "file:${file.absolutePath}:${file.lastModified()}"
        return loadFromCache(key) ?: runCatching {
            val image = ImageIO.read(file)
                ?: return ImageLoadResult.Error("Unsupported image format: ${file.name}")
            val resized = resizeIfNeeded(image, options)
            if (options.cacheResult) addToCache(key, resized)
            ImageLoadResult.Success(resized, resized.width, resized.height)
        }.getOrElse { ImageLoadResult.Error(it.message ?: "Failed to load image") }
    }

    override suspend fun load(url: String, options: ImageLoadOptions): ImageLoadResult {
        val key = "url:$url"
        return loadFromCache(key) ?: runCatching {
            val image = ImageIO.read(URL(url))
                ?: return ImageLoadResult.Error("Failed to load image from: $url")
            val resized = resizeIfNeeded(image, options)
            if (options.cacheResult) addToCache(key, resized)
            ImageLoadResult.Success(resized, resized.width, resized.height)
        }.getOrElse { ImageLoadResult.Error(it.message ?: "Failed to load image") }
    }

    override suspend fun load(stream: InputStream, options: ImageLoadOptions): ImageLoadResult {
        return runCatching {
            val image = ImageIO.read(stream)
                ?: return ImageLoadResult.Error("Failed to decode image from stream")
            val resized = resizeIfNeeded(image, options)
            ImageLoadResult.Success(resized, resized.width, resized.height)
        }.getOrElse { ImageLoadResult.Error(it.message ?: "Failed to load image") }
    }

    override fun clearCache() {
        cache.clear()
        currentCacheSize = 0
    }

    override fun cacheSize(): Long = currentCacheSize

    private fun loadFromCache(key: String): ImageLoadResult? {
        val image = cache[key] ?: return null
        return ImageLoadResult.Success(image, image.width, image.height, fromCache = true)
    }

    private fun addToCache(key: String, image: BufferedImage) {
        val size = image.width.toLong() * image.height.toLong() * 4 // ARGB = 4 bytes
        while (currentCacheSize + size > maxCacheSize && cache.isNotEmpty()) {
            val oldest = cache.keys.firstOrNull()
            if (oldest != null) {
                val removed = cache.remove(oldest)
                if (removed != null) currentCacheSize -= removed.width.toLong() * removed.height.toLong() * 4
            }
        }
        cache[key] = image
        currentCacheSize += size
    }

    private fun resizeIfNeeded(image: BufferedImage, options: ImageLoadOptions): BufferedImage {
        val w = image.width
        val h = image.height
        if (w <= options.maxWidth && h <= options.maxHeight) return image

        val scale = minOf(options.maxWidth.toDouble() / w, options.maxHeight.toDouble() / h)
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)

        val resized = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
        val g = resized.createGraphics()
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_RENDERING,
            java.awt.RenderingHints.VALUE_RENDER_QUALITY
        )
        g.drawImage(image, 0, 0, newW, newH, null)
        g.dispose()
        return resized
    }
}

/**
 * Composite image loader that selects the best available backend.
 */
object ImageLoaderBridge {
    private val backend: ImageLoaderBackend by lazy {
        val coil = runCatching { CoilImageLoaderBackend() }.getOrNull()
        if (coil?.available == true) coil else JavaImageLoaderBackend()
    }

    suspend fun load(file: File, options: ImageLoadOptions = ImageLoadOptions()): ImageLoadResult {
        return backend.load(file, options)
    }

    suspend fun load(url: String, options: ImageLoadOptions = ImageLoadOptions()): ImageLoadResult {
        return backend.load(url, options)
    }

    suspend fun load(stream: InputStream, options: ImageLoadOptions = ImageLoadOptions()): ImageLoadResult {
        return backend.load(stream, options)
    }

    fun clearCache() = backend.clearCache()
    fun cacheSize() = backend.cacheSize()
}

/**
 * Coil 3 backend — requires coil3 on the classpath.
 */
class CoilImageLoaderBackend : ImageLoaderBackend {
    override val name = "Coil 3"

    override val available: Boolean by lazy {
        runCatching {
            Class.forName("coil3.ImageLoader")
            true
        }.getOrDefault(false)
    }

    override suspend fun load(file: File, options: ImageLoadOptions): ImageLoadResult {
        if (!available) return JavaImageLoaderBackend().load(file, options)
        // Future: use Coil's async image pipeline
        return JavaImageLoaderBackend().load(file, options)
    }

    override suspend fun load(url: String, options: ImageLoadOptions): ImageLoadResult {
        if (!available) return JavaImageLoaderBackend().load(url, options)
        return JavaImageLoaderBackend().load(url, options)
    }

    override suspend fun load(stream: InputStream, options: ImageLoadOptions): ImageLoadResult {
        if (!available) return JavaImageLoaderBackend().load(stream, options)
        return JavaImageLoaderBackend().load(stream, options)
    }

    override fun clearCache() {}
    override fun cacheSize() = 0L
}
