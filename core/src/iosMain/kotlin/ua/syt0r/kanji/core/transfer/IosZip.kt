package ua.syt0r.kanji.core.transfer

import kotlin.math.max

// ============================================================
// Minimal ZIP archive support for Kotlin/Native (no java.util.zip).
//
// buildZip writes STORED entries — a fully valid ZIP readable by Anki,
// Python's zipfile and 7-Zip. readZip reads STORED and DEFLATE entries
// (the latter via IosInflate). ZIP64 archives are rejected with a clear
// error; Anki collections are far below the 4 GiB limit.
// ============================================================

internal class ZipDataEntry(
    val name: String,
    val content: ByteArray
)

private const val ZIP_LOCAL_HEADER_SIG = 0x04034b50
private const val ZIP_CENTRAL_HEADER_SIG = 0x02014b50
private const val ZIP_END_OF_CENTRAL_SIG = 0x06054b50

private const val METHOD_STORED = 0
private const val METHOD_DEFLATED = 8

private const val UTF8_FLAG = 0x0800

internal fun buildZip(entries: List<ZipDataEntry>): ByteArray {
    val writer = ByteWriter()
    val centralDirectory = ByteWriter()
    var localOffset = 0

    entries.forEach { entry ->
        val nameBytes = entry.name.toByteArray(Charsets.UTF_8)
        val crc = crc32(entry.content)
        val size = entry.content.size

        // Local file header
        writer.writeIntLE(ZIP_LOCAL_HEADER_SIG)
        writer.writeShortLE(20)          // version needed to extract
        writer.writeShortLE(UTF8_FLAG)
        writer.writeShortLE(METHOD_STORED)
        writer.writeShortLE(0)           // mod time
        writer.writeShortLE(0x21)        // mod date (1980-01-01)
        writer.writeIntLE(crc.toInt())
        writer.writeIntLE(size)
        writer.writeIntLE(size)
        writer.writeShortLE(nameBytes.size)
        writer.writeShortLE(0)           // extra length
        writer.writeBytes(nameBytes)
        writer.writeBytes(entry.content)

        // Central directory entry
        centralDirectory.writeIntLE(ZIP_CENTRAL_HEADER_SIG)
        centralDirectory.writeShortLE(20)   // version made by
        centralDirectory.writeShortLE(20)   // version needed
        centralDirectory.writeShortLE(UTF8_FLAG)
        centralDirectory.writeShortLE(METHOD_STORED)
        centralDirectory.writeShortLE(0)    // mod time
        centralDirectory.writeShortLE(0x21) // mod date
        centralDirectory.writeIntLE(crc.toInt())
        centralDirectory.writeIntLE(size)
        centralDirectory.writeIntLE(size)
        centralDirectory.writeShortLE(nameBytes.size)
        centralDirectory.writeShortLE(0)    // extra
        centralDirectory.writeShortLE(0)    // comment
        centralDirectory.writeShortLE(0)    // disk number
        centralDirectory.writeShortLE(0)    // internal attrs
        centralDirectory.writeIntLE(0)      // external attrs
        centralDirectory.writeIntLE(localOffset)
        centralDirectory.writeBytes(nameBytes)

        localOffset += 30 + nameBytes.size + size
    }

    val centralBytes = centralDirectory.toByteArray()
    writer.writeBytes(centralBytes)

    // End of central directory
    writer.writeIntLE(ZIP_END_OF_CENTRAL_SIG)
    writer.writeShortLE(0)                  // disk number
    writer.writeShortLE(0)                  // cd start disk
    writer.writeShortLE(entries.size)
    writer.writeShortLE(entries.size)
    writer.writeIntLE(centralBytes.size)
    writer.writeIntLE(localOffset)
    writer.writeShortLE(0)                  // comment length

    return writer.toByteArray()
}

internal fun readZip(bytes: ByteArray): List<ZipDataEntry> {
    val eocdPos = findEndOfCentralDirectory(bytes)
    val totalEntries = readShortLE(bytes, eocdPos + 10)
    val cdSize = readIntLE(bytes, eocdPos + 12)
    val cdOffset = readIntLE(bytes, eocdPos + 16)
    if (cdOffset == -1 || cdSize == -1) error("ZIP64 archives are not supported")

    val result = mutableListOf<ZipDataEntry>()
    var pos = cdOffset
    repeat(totalEntries) {
        if (pos < 0 || pos + 46 > bytes.size) {
            error("Corrupt ZIP archive — central directory entry out of bounds")
        }
        if (readIntLE(bytes, pos) != ZIP_CENTRAL_HEADER_SIG) {
            error("Corrupt ZIP archive — central directory signature missing")
        }
        val method = readShortLE(bytes, pos + 10)
        val compressedSize = readIntLE(bytes, pos + 20)
        val uncompressedSize = readIntLE(bytes, pos + 24)
        val nameLength = readShortLE(bytes, pos + 28)
        val extraLength = readShortLE(bytes, pos + 30)
        val commentLength = readShortLE(bytes, pos + 32)
        val localOffset = readIntLE(bytes, pos + 42)
        if (pos + 46 + nameLength + extraLength + commentLength > bytes.size) {
            error("Corrupt ZIP archive — central directory entry out of bounds")
        }
        val name = String(bytes, pos + 46, nameLength, Charsets.UTF_8)

        pos += 46 + nameLength + extraLength + commentLength

        if (compressedSize == -1 || uncompressedSize == -1 || localOffset == -1) {
            error("ZIP64 entry not supported: $name")
        }
        if (localOffset < 0 || localOffset + 30 > bytes.size) {
            error("Corrupt ZIP archive — local header out of bounds for $name")
        }
        if (readIntLE(bytes, localOffset) != ZIP_LOCAL_HEADER_SIG) {
            error("Corrupt ZIP archive — local header missing for $name")
        }
        val localNameLength = readShortLE(bytes, localOffset + 26)
        val localExtraLength = readShortLE(bytes, localOffset + 28)
        val dataStart = localOffset + 30 + localNameLength + localExtraLength
        if (dataStart < 0 || dataStart + compressedSize > bytes.size) {
            error("Corrupt ZIP archive — entry data out of bounds for $name")
        }
        val compressed = bytes.copyOfRange(dataStart, dataStart + compressedSize)

        val content = when (method) {
            METHOD_STORED -> compressed
            METHOD_DEFLATED -> inflate(compressed, uncompressedSize)
            else -> error("Unsupported ZIP compression method $method for $name")
        }
        result.add(ZipDataEntry(name, content))
    }
    return result
}

private fun findEndOfCentralDirectory(bytes: ByteArray): Int {
    val minPos = max(0, bytes.size - 65557)
    var pos = bytes.size - 22
    while (pos >= minPos) {
        if (readIntLE(bytes, pos) == ZIP_END_OF_CENTRAL_SIG) return pos
        pos--
    }
    error("Not a ZIP archive — end of central directory record not found")
}

// ------------------------------------------------------------
// CRC-32 (IEEE, reflected, poly 0xEDB88320) — Anki's zipfile checks
// CRCs on extract, so this must be exact.
// ------------------------------------------------------------

internal fun crc32(data: ByteArray): Long {
    var crc = 0xFFFFFFFFL
    data.forEach { byte ->
        val index = ((crc xor (byte.toLong() and 0xff)) and 0xff).toInt()
        crc = (crc ushr 8) xor CRC_TABLE[index]
    }
    return crc xor 0xFFFFFFFFL
}

private val CRC_TABLE: LongArray = LongArray(256) { n ->
    var c = n.toLong()
    repeat(8) {
        c = if (c and 1L != 0L) (c ushr 1) xor 0xEDB88320L else c ushr 1
    }
    c
}

// ------------------------------------------------------------
// Little-endian readers / writer
// ------------------------------------------------------------

private fun readShortLE(data: ByteArray, offset: Int): Int {
    val lo = data[offset].toInt() and 0xff
    val hi = data[offset + 1].toInt() and 0xff
    return lo or (hi shl 8)
}

private fun readIntLE(data: ByteArray, offset: Int): Int {
    var result = 0
    for (i in 0..3) {
        result = result or ((data[offset + i].toInt() and 0xff) shl (8 * i))
    }
    return result
}

private class ByteWriter {
    private var buffer = ByteArray(4096)
    private var size = 0

    fun writeByte(value: Int) {
        ensureCapacity(1)
        buffer[size++] = value.toByte()
    }

    fun writeShortLE(value: Int) {
        writeByte(value and 0xff)
        writeByte((value ushr 8) and 0xff)
    }

    fun writeIntLE(value: Int) {
        writeByte(value and 0xff)
        writeByte((value ushr 8) and 0xff)
        writeByte((value ushr 16) and 0xff)
        writeByte((value ushr 24) and 0xff)
    }

    fun writeBytes(bytes: ByteArray) {
        ensureCapacity(bytes.size)
        bytes.copyInto(buffer, size)
        size += bytes.size
    }

    private fun ensureCapacity(extra: Int) {
        if (size + extra > buffer.size) {
            buffer = buffer.copyOf(max(buffer.size * 2, size + extra))
        }
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)
}
