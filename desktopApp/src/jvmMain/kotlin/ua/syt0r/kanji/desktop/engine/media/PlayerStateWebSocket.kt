package ua.syt0r.kanji.desktop.engine.media

import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

// ============================================
// KAITEYO PLAYER STATE WEBSOCKET
// A small RFC6455 WebSocket server (zero extra
// dependencies — java.net only) that broadcasts
// live player state as JSON every 500 ms and
// accepts control commands from external tools:
//
//   ws://127.0.0.1:8765
//
//   → {"media":"ep01.mkv","positionMs":12345,...}
//   ← {"command":"play"} / {"command":"seek","positionMs":9000}
//
// This powers texthooker-style external tools,
// browser integrations and future plugins without
// exposing any filesystem access.
// ============================================

class PlayerStateWebSocket(
    private val stateProvider: () -> String,
    private val onCommand: (String) -> Unit
) {

    /** Port the server binds to; read before [start]. */
    var port: Int = 8765

    @Volatile
    var lastError: String? = null
        private set

    private val clients = CopyOnWriteArrayList<Socket>()
    private val connections = AtomicInteger(0)
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private var broadcastThread: Thread? = null
    @Volatile private var running = false

    val isRunning: Boolean get() = running
    val clientCount: Int get() = clients.size
    val totalConnections: Int get() = connections.get()

    // ------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------

    fun start() {
        if (running) return
        running = true
        connections.set(0)
        try {
            serverSocket = ServerSocket(port)
        } catch (e: Exception) {
            lastError = e.message
            running = false
            return
        }
        lastError = null
        acceptThread = Thread({ acceptLoop() }, "kaiteyo-ws-accept").apply { isDaemon = true; start() }
        broadcastThread = Thread({ broadcastLoop() }, "kaiteyo-ws-broadcast").apply { isDaemon = true; start() }
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        clients.forEach { runCatching { it.close() } }
        clients.clear()
    }

    private fun acceptLoop() {
        while (running) {
            val socket = try {
                serverSocket?.accept() ?: break
            } catch (_: Exception) {
                if (running) continue else break
            }
            Thread({ handleClient(socket) }, "kaiteyo-ws-client").apply {
                isDaemon = true
                start()
            }
        }
    }

    // ------------------------------------------------------------
    // Per-client handling
    // ------------------------------------------------------------

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 0
            if (!performHandshake(socket)) {
                runCatching { socket.close() }
                return
            }
            connections.incrementAndGet()
            clients.add(socket)
            readFrames(socket)
        } catch (_: Exception) {
            // client disconnected / protocol error
        } finally {
            clients.remove(socket)
            runCatching { socket.close() }
        }
    }

    /** Read the HTTP upgrade request and answer with a 101 handshake. */
    private fun performHandshake(socket: Socket): Boolean {
        val input = socket.getInputStream()
        val request = StringBuilder()
        val buf = ByteArray(1024)
        var total = 0
        // Read until the header terminator (max 8 KB to stay safe).
        while (total < 8192) {
            val n = input.read(buf)
            if (n < 0) return false
            request.append(String(buf, 0, n, StandardCharsets.ISO_8859_1))
            total += n
            if (request.contains("\r\n\r\n")) break
        }
        val headers = request.split("\r\n")
        val keyLine = headers.firstOrNull { it.startsWith("Sec-WebSocket-Key:") } ?: return false
        val key = keyLine.substringAfter(':').trim()
        if (key.isBlank()) return false
        val accept = acceptKey(key)
        val response = buildString {
            append("HTTP/1.1 101 Switching Protocols\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Accept: ").append(accept).append("\r\n")
            append("\r\n")
        }
        socket.getOutputStream().write(response.toByteArray(StandardCharsets.ISO_8859_1))
        socket.getOutputStream().flush()
        return true
    }

    private fun acceptKey(key: String): String {
        val sha1 = MessageDigest.getInstance("SHA-1")
        val digest = sha1.digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(StandardCharsets.ISO_8859_1))
        return Base64.getEncoder().encodeToString(digest)
    }

    /** Read client frames; text frames become commands, close/ping handled. */
    private fun readFrames(socket: Socket) {
        val input = socket.getInputStream()
        while (running && !socket.isClosed) {
            val first = input.read()
            if (first < 0) return
            val second = input.read()
            if (second < 0) return
            val opcode = first and 0x0F
            val masked = second and 0x80 != 0
            var length = (second and 0x7F).toLong()
            if (length == 126L) length = readLong(input, 2)
            else if (length == 127L) length = readLong(input, 8)
            if (length > 1_048_576) return // refuse oversized frames
            val maskKey = if (masked) {
                val key = ByteArray(4)
                readFully(input, key)
                key
            } else null
            val payload = ByteArray(length.toInt())
            readFully(input, payload)
            if (maskKey != null) {
                for (i in payload.indices) payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
            when (opcode) {
                0x1 -> { // text frame
                    val text = String(payload, StandardCharsets.UTF_8)
                    if (text.isNotBlank()) onCommand(text)
                }
                0x8 -> return // close
                0x9 -> writePong(socket) // ping
                else -> Unit
            }
        }
    }

    private fun readFully(input: InputStream, target: ByteArray) {
        var off = 0
        while (off < target.size) {
            val n = input.read(target, off, target.size - off)
            if (n < 0) throw java.io.IOException("Unexpected end of stream")
            off += n
        }
    }

    private fun readLong(input: InputStream, bytes: Int): Long {
        var value = 0L
        repeat(bytes) {
            val b = input.read()
            if (b < 0) throw java.io.IOException("Unexpected end of stream")
            value = (value shl 8) or b.toLong()
        }
        return value
    }

    private fun writePong(socket: Socket) {
        synchronized(socket) {
            socket.getOutputStream().write(byteArrayOf(0x8A.toByte(), 0x00))
            socket.getOutputStream().flush()
        }
    }

    // ------------------------------------------------------------
    // Broadcast
    // ------------------------------------------------------------

    private fun broadcastLoop() {
        while (running) {
            try {
                val snapshot = stateProvider()
                if (snapshot.isNotBlank() && clients.isNotEmpty()) {
                    clients.forEach { client ->
                        runCatching { writeTextFrame(client, snapshot) }
                    }
                }
            } catch (_: Exception) {
                // transient — keep broadcasting
            }
            try {
                Thread.sleep(500)
            } catch (_: InterruptedException) {
                return
            }
        }
    }

    private fun writeTextFrame(socket: Socket, text: String) {
        val payload = text.toByteArray(StandardCharsets.UTF_8)
        synchronized(socket) {
            val out: OutputStream = socket.getOutputStream()
            out.write(0x81)
            when {
                payload.size < 126 -> out.write(payload.size)
                payload.size <= 0xFFFF -> {
                    out.write(126)
                    out.write(payload.size shr 8)
                    out.write(payload.size and 0xFF)
                }
                else -> {
                    out.write(127)
                    var len = payload.size.toLong()
                    for (i in 7 downTo 0) out.write(((len shr (i * 8)) and 0xFF).toInt())
                }
            }
            out.write(payload)
            out.flush()
        }
    }
}
