package ua.syt0r.kanji.desktop.engine.media

import java.awt.AWTException
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

// ============================================
// KAITEYO MEDIA TRAY
// Optional system-tray controller for the media
// engine (java.awt only — no extra dependencies).
// While media is playing, the tray shows the
// current item and offers Play/Pause, Previous,
// Next, Stop and a jump back to the app. The tray
// is started lazily on first playback and only on
// desktops that support it; absence is never an
// error, just a missing nicety.
// ============================================

/**
 * Thin wrapper around [SystemTray]. [onAction] receives one of the action
 * names below and the media engine decides what to do with it.
 */
class MediaTray(private val onAction: (String) -> Unit) {

    private var trayIcon: TrayIcon? = null
    private var itemLabel: MenuItem? = null
    private var playPause: MenuItem? = null
    private var lastNotifyAtMs: Long = 0L

    val supported: Boolean
        get() = runCatching { SystemTray.isSupported() }.getOrDefault(false)

    /** Current tooltip line — updated whenever the media changes. */
    var label: String = ""
        set(value) {
            field = value
            trayIcon?.toolTip = "Kaiteyo · $value"
            itemLabel?.label = "Now: ${value.take(40)}"
        }

    /** Bring the tray icon up (idempotent). */
    fun start() {
        if (!supported || trayIcon != null) return
        runCatching {
            val popup = PopupMenu()
            itemLabel = MenuItem("Now: —").also { it.isEnabled = false; popup.add(it) }
            popup.addSeparator()
            playPause = MenuItem("Play").also { it.addActionListener { onAction("toggle") }; popup.add(it) }
            popup.add(MenuItem("Previous").also { it.addActionListener { onAction("previous") } })
            popup.add(MenuItem("Next").also { it.addActionListener { onAction("next") } })
            popup.add(MenuItem("Stop").also { it.addActionListener { onAction("stop") } })
            popup.addSeparator()
            popup.add(MenuItem("Open Kaiteyo media").also { it.addActionListener { onAction("show") } })
            popup.add(MenuItem("Quit media").also { it.addActionListener { onAction("quit-media") } })

            val icon = buildIcon()
            val tray = SystemTray.getSystemTray()
            trayIcon = TrayIcon(icon, "Kaiteyo · media").also {
                it.isImageAutoSize = true
                it.popupMenu = popup
                it.addActionListener { onAction("toggle") } // single click
                tray.add(it)
            }
        }
    }

    /** Mirror playing state into the menu label. */
    fun setPlaying(playing: Boolean) {
        playPause?.label = if (playing) "Pause" else "Play"
    }

    /**
     * Show a tray balloon (Windows/macOS style toast). Silent no-op when the
     * tray is not up or when a notification was shown in the last [notifyCooldownMs]
     * — coalesces pause/resume chatter into at most one balloon per transition.
     */
    fun notify(title: String, message: String, cooldownMs: Long = 2500) {
        val icon = trayIcon ?: return
        val now = System.currentTimeMillis()
        if (now - lastNotifyAtMs < cooldownMs) return
        lastNotifyAtMs = now
        runCatching {
            icon.displayMessage(title, message.take(80), TrayIcon.MessageType.NONE)
        }
    }

    /**
     * Show an OS notification balloon (only when the tray icon is up).
     * Used for background events — mined cards, episode changes — while
     * Kaiteyo is running but not focused.
     */
    fun notify(title: String, message: String) {
        runCatching { trayIcon?.displayMessage(title, message, TrayIcon.MessageType.INFO) }
    }

    /** Remove the tray icon (app shutdown). */
    fun stop() {
        runCatching { trayIcon?.let { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        itemLabel = null
        playPause = null
    }

    /** A small round icon with 書 (the Kaiteyo mark) drawn on it. */
    private fun buildIcon(): Image {
        val size = SystemTray.getSystemTray().trayIconSize
        val img = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        try {
            g.color = java.awt.Color(0xFF6750A4.toInt())
            g.fillOval(0, 0, size.width - 1, size.height - 1)
            g.color = java.awt.Color.WHITE
            g.font = g.font.deriveFont(java.awt.Font.BOLD, (size.height * 0.72f).toFloat())
            val fm = g.fontMetrics
            val s = "書"
            val x = (size.width - fm.stringWidth(s)) / 2
            val y = (size.height - fm.height) / 2 + fm.ascent
            g.drawString(s, x, y)
        } finally {
            g.dispose()
        }
        return img
    }
}
