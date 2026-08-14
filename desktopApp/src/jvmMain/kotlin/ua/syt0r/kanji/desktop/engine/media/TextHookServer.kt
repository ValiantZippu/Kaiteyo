package ua.syt0r.kanji.desktop.engine.media

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

// ============================================
// KAITEYO TEXT HOOK SERVER
// A tiny TCP server for external text sources
// (visual-novel texthookers, game extractors,
// browser scripts, mpv scripts). Any client can
// send Japanese text lines; every non-blank line
// is pushed into the dictionary workflow just
// like selecting text in a subtitle:
//
//   echo "今日は学校に行かなかった。" | nc 127.0.0.1 8766
//   → dictionary panel opens with the line
//
// Sending "CLEAR" clears the current lookup.
// Zero dependencies — java.net only.
// ============================================

class TextHookServer(
    private val onText: (String) -> Unit
) {

    /** Port the server binds to; read before [start]. */
    var port: Int = 8766

    @Volatile
    var lastError: String? = null
        private set

    private val clients = CopyOnWriteArrayList<Socket>()
    private val connections = AtomicInteger(0)
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
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
        acceptThread = Thread({ acceptLoop() }, "kaiteyo-texthook-accept").apply { isDaemon = true; start() }
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
            Thread({ handleClient(socket) }, "kaiteyo-texthook-client").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            connections.incrementAndGet()
            clients.add(socket)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            while (running && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val text = line.trim()
                if (text.isNotEmpty()) onText(text)
            }
        } catch (_: Exception) {
            // client disconnected
        } finally {
            clients.remove(socket)
            runCatching { socket.close() }
        }
    }
}
