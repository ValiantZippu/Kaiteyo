package ua.syt0r.kanji.desktop.engine.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.logger.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.Base64

// ============================================
// TOKEN VAULT
// Where OAuth access/refresh tokens live. The
// interface is the seam for OS keychain backends
// (Windows Credential Manager, macOS Keychain,
// Linux Secret Service); FileTokenVault is the
// default JVM implementation.
//
// The file implementation keeps tokens out of
// plain sight: values are obfuscated with a
// machine-derived key (no hardcoded secrets) and
// the vault file is restricted to the current
// user. This is defense-in-depth, not a claim of
// cryptographic strength — production builds
// should bind an OS keychain implementation.
// Token material is never logged.
// ============================================

interface TokenVault {
    suspend fun save(kind: ProviderKind, token: AuthToken)
    suspend fun read(kind: ProviderKind): AuthToken?
    suspend fun delete(kind: ProviderKind)
    suspend fun clear()
    fun configuredKinds(): List<ProviderKind>
}

class FileTokenVault(
    private val directory: File
) : TokenVault {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val vaultFile: File get() = File(directory, "vault.json")
    private val key: ByteArray = deriveMachineKey()

    override suspend fun save(kind: ProviderKind, token: AuthToken) = withContext(Dispatchers.IO) {
        val stored = readAll().toMutableMap()
        stored[kind.id] = token
        writeAll(stored)
    }

    override suspend fun read(kind: ProviderKind): AuthToken? = withContext(Dispatchers.IO) {
        readAll()[kind.id]
    }

    override suspend fun delete(kind: ProviderKind) = withContext(Dispatchers.IO) {
        val stored = readAll().toMutableMap()
        if (stored.remove(kind.id) != null) writeAll(stored)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        writeAll(emptyMap())
    }

    override fun configuredKinds(): List<ProviderKind> {
        return readAll().keys.mapNotNull { kindId -> ProviderKind.fromId(kindId).takeIf { it != ProviderKind.Local } }
    }

    private fun readAll(): Map<String, AuthToken> {
        if (!vaultFile.exists()) return emptyMap()
        return try {
            val raw = vaultFile.readText()
            if (raw.isBlank()) return emptyMap()
            val tokens = mutableMapOf<String, AuthToken>()
            json.decodeFromString<Map<String, String>>(raw).forEach { (kindId, obfuscated) ->
                val decoded = deobfuscate(obfuscated) ?: return@forEach
                runCatching {
                    tokens[kindId] = json.decodeFromString<AuthToken>(decoded)
                }.onFailure { Logger.w("TokenVault: ignoring corrupt entry for $kindId") }
            }
            tokens
        } catch (e: Exception) {
            Logger.w("TokenVault: failed to read vault: ${e.message}")
            emptyMap()
        }
    }

    private fun writeAll(tokens: Map<String, AuthToken>) {
        val serialized = tokens.mapValues { (_, token) ->
            obfuscate(json.encodeToString(token))
        }
        runCatching {
            vaultFile.parentFile?.mkdirs()
            vaultFile.writeText(json.encodeToString(serialized))
            restrictPermissions(vaultFile)
        }.onFailure { Logger.w("TokenVault: failed to persist vault: ${it.message}") }
    }

    private fun restrictPermissions(file: File) {
        runCatching {
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setReadable(true, true)
            file.setWritable(true, true)
            file.setExecutable(false, false)
        }
        runCatching {
            Files.setPosixFilePermissions(
                file.toPath(),
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            )
        }
        // Non-POSIX filesystems throw UnsupportedOperationException — acceptable.
    }

    private fun obfuscate(plain: String): String =
        Base64.getEncoder().encodeToString(xor(plain.encodeToByteArray(), key))

    private fun deobfuscate(obfuscated: String): String? = runCatching {
        xor(Base64.getDecoder().decode(obfuscated), key).decodeToString()
    }.getOrNull()

    private fun xor(data: ByteArray, k: ByteArray): ByteArray =
        ByteArray(data.size) { i -> (data[i].toInt() xor k[i % k.size].toInt()).toByte() }

    /** FNV-1a hash over machine identity — stable per installation, no hardcoded secrets. */
    private fun deriveMachineKey(): ByteArray {
        val seed = listOf(
            System.getProperty("user.home"),
            System.getProperty("os.name"),
            System.getProperty("user.name"),
            System.getProperty("java.version")
        ).joinToString("|")
        var hash = 0x811C9DC5L
        seed.encodeToByteArray().forEach { b ->
            hash = (hash xor (b.toLong() and 0xFF)) * 0x01000193L and 0xFFFFFFFFL
        }
        return ByteArray(32) { i ->
            var h = hash
            repeat(i + 1) { h = h * 31L + 7L and 0xFFFFFFFFL }
            h.toByte()
        }
    }
}
