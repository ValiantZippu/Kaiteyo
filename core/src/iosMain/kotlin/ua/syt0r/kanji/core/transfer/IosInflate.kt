package ua.syt0r.kanji.core.transfer

import kotlin.math.max

// ============================================================
// DEFLATE (RFC 1951) decompressor — pure Kotlin for Kotlin/Native.
//
// Kotlin/Native has no java.util.zip, so APKG import needs its own
// inflate for the deflated `collection.anki2` entries produced by
// Anki and by Kaiteyo's own JVM/Android exports. Supports stored,
// fixed-Huffman and dynamic-Huffman blocks plus 32 KiB LZ77
// back-references. Not fast, but correct and dependency-free.
// ============================================================

internal fun inflate(data: ByteArray, expectedSize: Int): ByteArray {
    if (data.isEmpty()) return ByteArray(0)
    return Inflater(data, expectedSize).inflate()
}

/** LSB-first bit reader over a byte array. */
private class BitReader(private val data: ByteArray) {
    private var bitPos = 0

    fun readBits(count: Int): Int {
        if (bitPos + count > data.size * 8) error("Truncated DEFLATE stream")
        var value = 0
        for (i in 0 until count) {
            val byte = data[bitPos ushr 3].toInt() and 0xff
            if ((byte ushr (bitPos and 7)) and 1 != 0) value = value or (1 shl i)
            bitPos++
        }
        return value
    }

    fun alignToByte() {
        bitPos = (bitPos + 7) and 7.inv()
    }

    fun readByte(): Int {
        if (bitPos + 8 > data.size * 8) error("Truncated DEFLATE stream")
        val value = data[bitPos ushr 3].toInt() and 0xff
        bitPos += 8
        return value
    }
}

/**
 * Canonical Huffman decoding table. Symbols are ordered by code length
 * (ascending, then ascending symbol value within a length) and decoded by
 * walking the code bit by bit against the per-length symbol counts — the
 * classic inflate table walk.
 */
private class Huffman(codeLengths: IntArray) {
    private val counts = IntArray(16)
    private val symbols: IntArray

    init {
        codeLengths.forEach { if (it in 1..15) counts[it]++ }
        symbols = IntArray(codeLengths.size)
        var index = 0
        for (bits in 1..15) {
            for (symbol in codeLengths.indices) {
                if (codeLengths[symbol] == bits) {
                    symbols[index] = symbol
                    index++
                }
            }
        }
    }

    fun decode(reader: BitReader): Int {
        var code = 0
        var first = 0
        var index = 0
        for (length in 1..15) {
            code = code or reader.readBits(1)
            val count = counts[length]
            if (code - first < count) {
                return symbols[index + (code - first)]
            }
            index += count
            first = (first + count) shl 1
            code = code shl 1
        }
        error("Invalid Huffman code in DEFLATE stream")
    }
}

private class Inflater(private val data: ByteArray, expectedSize: Int) {
    private val reader = BitReader(data)
    // The zip header's uncompressed size is untrusted — cap the initial
    // allocation so a corrupt/malicious entry can't force a giant
    // pre-allocation; the doubling ensureCapacity grows as needed.
    private var output = if (expectedSize in 1..MAX_INITIAL_ALLOCATION) {
        ByteArray(expectedSize)
    } else {
        ByteArray(65536)
    }
    private var outPos = 0

    fun inflate(): ByteArray {
        var finalBlock = false
        while (!finalBlock) {
            finalBlock = reader.readBits(1) == 1
            when (reader.readBits(2)) {
                0 -> readStoredBlock()
                1 -> inflateBlock(FIXED_LITLEN, FIXED_DIST)
                2 -> {
                    val (litLen, dist) = readDynamicTables()
                    inflateBlock(litLen, dist)
                }
                else -> error("Invalid DEFLATE block type")
            }
        }
        return output.copyOf(outPos)
    }

    private fun ensureCapacity(extra: Int) {
        if (outPos + extra > output.size) {
            output = output.copyOf(max(output.size * 2, outPos + extra))
        }
    }

    private fun readStoredBlock() {
        reader.alignToByte()
        val length = reader.readBits(16)
        val check = reader.readBits(16)
        if (length xor check != 0xFFFF) error("Corrupt DEFLATE stored block")
        ensureCapacity(length)
        repeat(length) { output[outPos++] = reader.readByte().toByte() }
    }

    private fun inflateBlock(litLen: Huffman, dist: Huffman) {
        while (true) {
            val symbol = litLen.decode(reader)
            when {
                symbol < 256 -> {
                    ensureCapacity(1)
                    output[outPos++] = symbol.toByte()
                }
                symbol == 256 -> return
                else -> {
                    val lengthIndex = symbol - 257
                    if (lengthIndex >= LENGTH_BASE.size) error("Invalid length symbol $symbol")
                    val length = LENGTH_BASE[lengthIndex] + reader.readBits(LENGTH_EXTRA[lengthIndex])
                    val distSymbol = dist.decode(reader)
                    if (distSymbol >= DIST_BASE.size) error("Invalid distance symbol $distSymbol")
                    val distance = DIST_BASE[distSymbol] + reader.readBits(DIST_EXTRA[distSymbol])
                    if (distance > outPos) error("Corrupt DEFLATE stream — distance $distance exceeds output")
                    ensureCapacity(length)
                    repeat(length) {
                        output[outPos] = output[outPos - distance]
                        outPos++
                    }
                }
            }
        }
    }

    private fun readDynamicTables(): Pair<Huffman, Huffman> {
        val hlit = reader.readBits(5) + 257
        val hdist = reader.readBits(5) + 1
        val hclen = reader.readBits(4) + 4
        val codeLengthLengths = IntArray(19)
        repeat(hclen) { i -> codeLengthLengths[CODE_LENGTH_ORDER[i]] = reader.readBits(3) }
        val codeLengthTree = Huffman(codeLengthLengths)

        val codeLengths = IntArray(hlit + hdist)
        var index = 0
        while (index < codeLengths.size) {
            val symbol = codeLengthTree.decode(reader)
            when {
                symbol < 16 -> codeLengths[index++] = symbol
                symbol == 16 -> {
                    val previous = if (index > 0) codeLengths[index - 1] else 0
                    val repeat = 3 + reader.readBits(2)
                    repeat(repeat) { codeLengths[index++] = previous }
                }
                symbol == 17 -> {
                    val repeat = 3 + reader.readBits(3)
                    repeat(repeat) { codeLengths[index++] = 0 }
                }
                symbol == 18 -> {
                    val repeat = 11 + reader.readBits(7)
                    repeat(repeat) { codeLengths[index++] = 0 }
                }
                else -> error("Invalid code length symbol $symbol")
            }
        }

        return Huffman(codeLengths.copyOfRange(0, hlit)) to
            Huffman(codeLengths.copyOfRange(hlit, codeLengths.size))
    }

    private companion object {
        const val MAX_INITIAL_ALLOCATION = 32 * 1024 * 1024

        val FIXED_LITLEN: Huffman by lazy {
            val lengths = IntArray(288) {
                when (it) {
                    in 0..143 -> 8
                    in 144..255 -> 9
                    in 256..279 -> 7
                    else -> 8
                }
            }
            Huffman(lengths)
        }

        val FIXED_DIST: Huffman by lazy {
            Huffman(IntArray(30) { 5 })
        }

        val CODE_LENGTH_ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

        // Length/distance symbol tables from RFC 1951.
        val LENGTH_BASE = intArrayOf(
            3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59,
            67, 83, 99, 115, 131, 163, 195, 227, 258
        )
        val LENGTH_EXTRA = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3,
            4, 4, 4, 4, 5, 5, 5, 5, 0
        )
        val DIST_BASE = intArrayOf(
            1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769,
            1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
        )
        val DIST_EXTRA = intArrayOf(
            0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8,
            9, 9, 10, 10, 11, 11, 12, 12, 13, 13
        )
    }
}
