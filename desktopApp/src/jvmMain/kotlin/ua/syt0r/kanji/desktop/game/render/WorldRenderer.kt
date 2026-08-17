package ua.syt0r.kanji.desktop.game.render

import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.engine.Direction
import ua.syt0r.kanji.desktop.game.engine.camera.Camera
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.render.RenderBackend
import ua.syt0r.kanji.desktop.game.engine.render.RenderColor
import ua.syt0r.kanji.desktop.game.time.TimePhase
import ua.syt0r.kanji.desktop.game.time.tint
import ua.syt0r.kanji.desktop.game.time.tintAlpha
import ua.syt0r.kanji.desktop.game.world.ObjectKind
import ua.syt0r.kanji.desktop.game.world.WorldObject
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * Draws the world for the vertical slice: a stylized Japanese seaside town —
 * sand and sea, station and shop, vending machines and benches, signs that
 * carry real Japanese, animated characters, weather and a day/night tint.
 * The art direction is "stylized realism" (spec §80): readable shapes, warm
 * palette, no raw polygon chase.
 */
class WorldRenderer(private val session: GameSession) {

    private val palette = Palette()

    fun render(backend: RenderBackend, camera: Camera) {
        val view = camera.visibleWorldRect()
        drawGround(backend, camera, view)
        drawObjects(backend, camera, view)
        drawCharacters(backend, camera)
        drawLocationTags(backend, camera)
        drawWeather(backend, camera, view)
        drawTimeTint(backend, camera)
        drawSeasonTint(backend, camera)
        drawDebugOverlays(backend, camera, view)
    }

    // ------------------------------------------------------------
    // Ground
    // ------------------------------------------------------------

    private fun drawGround(backend: RenderBackend, camera: Camera, view: ua.syt0r.kanji.desktop.game.engine.geom.Rect) {
        val grid = session.tileGrid
        val tileSize = grid.tileSize
        val time = session.engine.time.elapsedSeconds

        val minTx = floor(view.x / tileSize).toInt().coerceAtLeast(0)
        val maxTx = floor(view.right / tileSize).toInt().coerceAtMost(grid.width - 1)
        val minTy = floor(view.y / tileSize).toInt().coerceAtLeast(0)
        val maxTy = floor(view.bottom / tileSize).toInt().coerceAtMost(grid.height - 1)

        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                val color = grid.tileColor(tx, ty)
                if (color == null) continue
                val world = Vec2(tx * tileSize, ty * tileSize)
                val screen = camera.worldToScreen(world)
                var c = RenderColor.rgb(color)
                if (grid.isAnimatedAt(tx, ty)) {
                    // Gentle water shimmer.
                    val wave = 0.04f * sin(time * 2f + (tx + ty) * 0.8f)
                    c = c.blend(RenderColor.White, max(0f, wave))
                }
                backend.drawRect(screen.x, screen.y, tileSize * camera.zoom, tileSize * camera.zoom, c)
            }
        }
    }

    // ------------------------------------------------------------
    // Objects (depth-sorted by feet position)
    // ------------------------------------------------------------

    private fun drawObjects(backend: RenderBackend, camera: Camera, view: ua.syt0r.kanji.desktop.game.engine.geom.Rect) {
        val cell = session.world.cell(session.player.state.cellId) ?: return
        val drawables = cell.objects
            .filter { view.overlaps(
                ua.syt0r.kanji.desktop.game.engine.geom.Rect(
                    it.position.x - it.size.x / 2f,
                    it.position.y - it.size.y,
                    it.size.x,
                    it.size.y
                )
            ) }
            .sortedBy { it.position.y }
        for (obj in drawables) {
            drawObject(backend, camera, obj)
        }
    }

    private fun drawObject(backend: RenderBackend, camera: Camera, obj: WorldObject) {
        val p = camera.worldToScreen(obj.position.toVec2())
        val zoom = camera.zoom
        when (obj.kind) {
            ObjectKind.Shop -> drawShop(backend, p, zoom, obj)
            ObjectKind.Station -> drawStation(backend, p, zoom, obj)
            ObjectKind.House -> drawHouse(backend, p, zoom, obj)
            ObjectKind.VendingMachine -> drawVendingMachine(backend, p, zoom, obj)
            ObjectKind.Bench -> drawBench(backend, p, zoom)
            ObjectKind.Sign -> drawSign(backend, p, zoom, obj)
            ObjectKind.Mailbox -> drawMailbox(backend, p, zoom)
            ObjectKind.Tree -> drawTree(backend, p, zoom)
            ObjectKind.Boat -> drawBoat(backend, p, zoom)
            ObjectKind.Lighthouse -> drawLighthouse(backend, p, zoom)
            ObjectKind.Shrine -> drawShrine(backend, p, zoom)
            ObjectKind.Cat -> drawAnimal(backend, p, zoom, palette.cat, 10f)
            ObjectKind.Bird -> drawAnimal(backend, p, zoom, palette.bird, 7f)
            ObjectKind.Lantern -> drawLantern(backend, p, zoom)
            ObjectKind.NoticeBoard -> drawSign(backend, p, zoom, obj)
            ObjectKind.BusStop -> drawSign(backend, p, zoom, obj)
            ObjectKind.Bicycle -> drawBicycle(backend, p, zoom)
            ObjectKind.Well -> drawWell(backend, p, zoom)
            ObjectKind.Fence -> drawFence(backend, p, zoom)
            ObjectKind.BeachTowel -> drawBeachTowel(backend, p, zoom)
            ObjectKind.Door, ObjectKind.CameraSpot, ObjectKind.PhoneBooth, ObjectKind.Stalls -> {
                // Decor/spot objects: draw their label as a floating tag.
                if (obj.label.isNotBlank()) drawLabel(backend, p, obj.label, zoom, palette.ink)
            }
        }
        // The object's Japanese label hovers above it when it has one
        // (signs drawn separately keep their own board text).
        if (obj.label.isNotBlank() && obj.kind !in labelOnBoardKinds) {
            drawLabel(backend, Vec2(p.x, p.y - obj.size.y * zoom - 6f), obj.label, 13f * zoom, palette.ink)
        }
    }

    private val labelOnBoardKinds = setOf(ObjectKind.Sign, ObjectKind.NoticeBoard, ObjectKind.BusStop, ObjectKind.Shop, ObjectKind.Station)

    private fun drawShop(backend: RenderBackend, p: Vec2, zoom: Float, obj: WorldObject) {
        val w = 92f * zoom
        val h = 78f * zoom
        val x = p.x - w / 2f
        val y = p.y - h
        // Body
        backend.drawRect(x, y, w, h, palette.shopBody, 4f * zoom)
        // Awning stripes
        val stripe = 12f * zoom
        var sx = x
        var i = 0
        while (sx < x + w) {
            backend.drawRect(sx, y, stripe, 16f * zoom, if (i % 2 == 0) palette.accentRed else palette.cream)
            sx += stripe
            i++
        }
        // Door + window
        backend.drawRect(x + w / 2f - 14f * zoom, y + h - 30f * zoom, 28f * zoom, 30f * zoom, palette.doorBrown, 2f * zoom)
        backend.drawRect(x + 6f * zoom, y + 26f * zoom, 18f * zoom, 18f * zoom, palette.windowBlue, 3f * zoom)
        backend.drawRect(x + w - 24f * zoom, y + 26f * zoom, 18f * zoom, 18f * zoom, palette.windowBlue, 3f * zoom)
        if (obj.label.isNotBlank()) {
            drawLabel(backend, Vec2(p.x, y - 8f * zoom), obj.label, 14f * zoom, palette.ink)
        }
    }

    private fun drawStation(backend: RenderBackend, p: Vec2, zoom: Float, obj: WorldObject) {
        val w = 120f * zoom
        val h = 84f * zoom
        val x = p.x - w / 2f
        val y = p.y - h
        backend.drawRect(x, y, w, h, palette.stationBody, 4f * zoom)
        // Roof
        backend.drawRect(x - 8f * zoom, y - 10f * zoom, w + 16f * zoom, 14f * zoom, palette.stationRoof, 3f * zoom)
        // Platform canopy posts
        backend.drawRect(x - 8f * zoom, y + h - 6f * zoom, 8f * zoom, 6f * zoom, palette.postGrey)
        backend.drawRect(x + w, y + h - 6f * zoom, 8f * zoom, 6f * zoom, palette.postGrey)
        // Big signboard — the station's own name (浜中駅 / 鎌倉駅 …)
        backend.drawRect(p.x - 46f * zoom, y - 34f * zoom, 92f * zoom, 26f * zoom, palette.ink, 3f * zoom)
        drawLabel(backend, Vec2(p.x, y - 21f * zoom), obj.label.ifBlank { "駅" }, 13f * zoom, palette.cream)
        // Ticket window
        backend.drawRect(x + w / 2f - 20f * zoom, y + h - 26f * zoom, 40f * zoom, 26f * zoom, palette.windowBlue, 2f * zoom)
        if (obj.label.isNotBlank()) {
            drawLabel(backend, Vec2(p.x, y - 46f * zoom), obj.label, 12f * zoom, palette.ink)
        }
    }

    private fun drawHouse(backend: RenderBackend, p: Vec2, zoom: Float, obj: WorldObject) {
        val w = 84f * zoom
        val h = 64f * zoom
        val x = p.x - w / 2f
        val y = p.y - h
        backend.drawRect(x, y, w, h, palette.houseBody, 3f * zoom)
        // Roof
        backend.drawRect(x - 6f * zoom, y - 10f * zoom, w + 12f * zoom, 12f * zoom, palette.houseRoof, 2f * zoom)
        backend.drawRect(x + w / 2f - 10f * zoom, y + h - 26f * zoom, 20f * zoom, 26f * zoom, palette.doorBrown, 2f * zoom)
        backend.drawRect(x + 8f * zoom, y + 16f * zoom, 16f * zoom, 16f * zoom, palette.windowBlue, 3f * zoom)
        backend.drawRect(x + w - 24f * zoom, y + 16f * zoom, 16f * zoom, 16f * zoom, palette.windowBlue, 3f * zoom)
    }

    private fun drawVendingMachine(backend: RenderBackend, p: Vec2, zoom: Float, obj: WorldObject) {
        val w = 40f * zoom
        val h = 62f * zoom
        val x = p.x - w / 2f
        val y = p.y - h
        backend.drawRect(x, y, w, h, palette.machineBody, 5f * zoom)
        backend.drawRect(x + 5f * zoom, y + 6f * zoom, w - 10f * zoom, 26f * zoom, palette.machineGlass, 3f * zoom)
        // Drink slots
        for (row in 0..1) {
            for (col in 0..1) {
                backend.drawRect(
                    x + 8f * zoom + col * 14f * zoom,
                    y + 38f * zoom + row * 10f * zoom,
                    10f * zoom,
                    6f * zoom,
                    if ((row + col) % 2 == 0) palette.accentRed else palette.accentTeal,
                    1f * zoom
                )
            }
        }
        if (obj.label.isNotBlank()) {
            drawLabel(backend, Vec2(p.x, y - 8f * zoom), obj.label, 11f * zoom, palette.ink)
        }
    }

    private fun drawBench(backend: RenderBackend, p: Vec2, zoom: Float) {
        val w = 56f * zoom
        val x = p.x - w / 2f
        val y = p.y - 22f * zoom
        backend.drawRect(x, y, w, 8f * zoom, palette.wood, 3f * zoom)
        backend.drawRect(x, y + 8f * zoom, w, 6f * zoom, palette.woodDark, 3f * zoom)
        backend.drawRect(x + 4f * zoom, y + 14f * zoom, 6f * zoom, 10f * zoom, palette.woodDark)
        backend.drawRect(x + w - 10f * zoom, y + 14f * zoom, 6f * zoom, 10f * zoom, palette.woodDark)
    }

    private fun drawSign(backend: RenderBackend, p: Vec2, zoom: Float, obj: WorldObject) {
        val w = 72f * zoom
        val h = 34f * zoom
        val x = p.x - w / 2f
        val y = p.y - h - 26f * zoom
        // Post
        backend.drawRect(p.x - 2f * zoom, y + h, 4f * zoom, 28f * zoom, palette.postGrey)
        // Board
        val boardColor = obj.accent?.let { RenderColor.rgb(it) } ?: palette.signBoard
        backend.drawRect(x, y, w, h, boardColor, 3f * zoom)
        backend.drawRect(x + 3f * zoom, y + 3f * zoom, w - 6f * zoom, h - 6f * zoom, RenderColor.White.withAlpha(0.25f), 2f * zoom)
        if (obj.label.isNotBlank()) {
            drawLabel(backend, Vec2(p.x, y + h / 2f), obj.label, 14f * zoom, palette.ink)
        }
    }

    private fun drawMailbox(backend: RenderBackend, p: Vec2, zoom: Float) {
        val w = 26f * zoom
        val h = 30f * zoom
        val x = p.x - w / 2f
        val y = p.y - h
        backend.drawRect(x, y + 10f * zoom, w, h - 10f * zoom, palette.mailboxRed, 3f * zoom)
        backend.drawRect(x + 2f * zoom, y + 14f * zoom, w - 4f * zoom, 8f * zoom, RenderColor.White.withAlpha(0.2f), 2f * zoom)
        backend.drawRect(p.x - 3f * zoom, y + h - 6f * zoom, 6f * zoom, 8f * zoom, palette.postGrey)
    }

    private fun drawTree(backend: RenderBackend, p: Vec2, zoom: Float) {
        val sway = 0.02f * sin(session.engine.time.elapsedSeconds * 1.4f + p.x)
        val cx = p.x + sway * 20f * zoom
        val trunkH = 26f * zoom
        backend.drawRect(cx - 5f * zoom, p.y - trunkH, 10f * zoom, trunkH, palette.trunkBrown, 3f * zoom)
        backend.drawCircle(Vec2(cx, p.y - trunkH - 16f * zoom), 22f * zoom, palette.treeGreen)
        backend.drawCircle(Vec2(cx - 12f * zoom, p.y - trunkH - 8f * zoom), 14f * zoom, palette.treeGreen)
        backend.drawCircle(Vec2(cx + 12f * zoom, p.y - trunkH - 8f * zoom), 14f * zoom, palette.treeGreen)
    }

    private fun drawBoat(backend: RenderBackend, p: Vec2, zoom: Float) {
        val w = 60f * zoom
        val h = 22f * zoom
        val x = p.x - w / 2f
        val y = p.y - h
        backend.drawRect(x, y, w, h, palette.boatBrown, 6f * zoom)
        backend.drawRect(p.x - 3f * zoom, y - 24f * zoom, 6f * zoom, 24f * zoom, palette.postGrey)
        backend.drawPolyline(
            listOf(Vec2(p.x, y - 24f * zoom), Vec2(p.x + 20f * zoom, y - 6f * zoom), Vec2(p.x, y - 6f * zoom)),
            palette.cream,
            3f * zoom
        )
    }

    private fun drawLighthouse(backend: RenderBackend, p: Vec2, zoom: Float) {
        val h = 90f * zoom
        val w = 34f * zoom
        val x = p.x - w / 2f
        val y = p.y - h
        backend.drawRect(x, y, w, h, palette.lighthouseWhite, 4f * zoom)
        // Red stripe
        backend.drawRect(x, y + h * 0.55f, w, h * 0.2f, palette.accentRed)
        // Light room
        backend.drawRect(x - 4f * zoom, y - 12f * zoom, w + 8f * zoom, 14f * zoom, palette.lighthouseTop, 3f * zoom)
        // Light beam at night
        if (session.clock.phase == TimePhase.Night) {
            backend.drawEllipse(Vec2(p.x + 40f * zoom, y - 6f * zoom), 30f * zoom, 12f * zoom, RenderColor.rgb(0xFFF3B0).withAlpha(0.35f))
        }
    }

    private fun drawShrine(backend: RenderBackend, p: Vec2, zoom: Float) {
        val w = 64f * zoom
        val x = p.x - w / 2f
        val top = p.y - 58f * zoom
        backend.drawRect(x, top, w, 10f * zoom, palette.shrineRed, 2f * zoom)
        backend.drawRect(x, top + 10f * zoom, w, 6f * zoom, palette.shrineRed)
        backend.drawRect(x + 6f * zoom, top + 16f * zoom, 8f * zoom, 36f * zoom, palette.shrineRed)
        backend.drawRect(x + w - 14f * zoom, top + 16f * zoom, 8f * zoom, 36f * zoom, palette.shrineRed)
        backend.drawRect(x + 2f * zoom, top + 24f * zoom, w - 4f * zoom, 4f * zoom, palette.shrineRed)
    }

    private fun drawAnimal(backend: RenderBackend, p: Vec2, zoom: Float, color: RenderColor, radius: Float) {
        val r = radius * zoom
        backend.drawCircle(Vec2(p.x, p.y - r), r, color)
        backend.drawCircle(Vec2(p.x - r * 0.8f, p.y - r * 0.6f), r * 0.3f, color)
        backend.drawCircle(Vec2(p.x + r * 0.8f, p.y - r * 0.6f), r * 0.3f, color)
    }

    private fun drawLantern(backend: RenderBackend, p: Vec2, zoom: Float) {
        backend.drawRect(p.x - 2f * zoom, p.y - 26f * zoom, 4f * zoom, 26f * zoom, palette.postGrey)
        backend.drawRect(p.x - 7f * zoom, p.y - 34f * zoom, 14f * zoom, 10f * zoom, palette.lanternRed, 2f * zoom)
    }

    private fun drawFence(backend: RenderBackend, p: Vec2, zoom: Float) {
        val w = 60f * zoom
        backend.drawRect(p.x - w / 2f, p.y - 18f * zoom, w, 4f * zoom, palette.wood)
        backend.drawRect(p.x - w / 2f, p.y - 8f * zoom, w, 4f * zoom, palette.wood)
        for (i in 0..3) {
            backend.drawRect(p.x - w / 2f + i * 15f * zoom, p.y - 24f * zoom, 4f * zoom, 18f * zoom, palette.wood)
        }
    }

    private fun drawBicycle(backend: RenderBackend, p: Vec2, zoom: Float) {
        backend.drawCircle(Vec2(p.x - 12f * zoom, p.y - 10f * zoom), 9f * zoom, palette.bikeGrey)
        backend.drawCircle(Vec2(p.x + 12f * zoom, p.y - 10f * zoom), 9f * zoom, palette.bikeGrey)
        backend.drawLine(Vec2(p.x - 12f * zoom, p.y - 10f * zoom), Vec2(p.x, p.y - 26f * zoom), palette.bikeGrey, 3f * zoom)
        backend.drawLine(Vec2(p.x + 12f * zoom, p.y - 10f * zoom), Vec2(p.x, p.y - 26f * zoom), palette.bikeGrey, 3f * zoom)
    }

    private fun drawWell(backend: RenderBackend, p: Vec2, zoom: Float) {
        val w = 30f * zoom
        backend.drawRect(p.x - w / 2f, p.y - 20f * zoom, w, 20f * zoom, palette.stoneGrey, 4f * zoom)
        backend.drawRect(p.x - w / 2f - 4f * zoom, p.y - 22f * zoom, w + 8f * zoom, 6f * zoom, palette.stoneGrey, 2f * zoom)
        backend.drawRect(p.x - 1f * zoom, p.y - 34f * zoom, 2f * zoom, 14f * zoom, palette.postGrey)
    }

    private fun drawBeachTowel(backend: RenderBackend, p: Vec2, zoom: Float) {
        val w = 40f * zoom
        backend.drawRect(p.x - w / 2f, p.y - 10f * zoom, w, 18f * zoom, palette.towelTeal, 3f * zoom)
        backend.drawRect(p.x - w / 2f + 6f * zoom, p.y - 10f * zoom, 6f * zoom, 18f * zoom, RenderColor.White.withAlpha(0.35f))
    }

    // ------------------------------------------------------------
    // Characters (player + NPCs)
    // ------------------------------------------------------------

    private fun drawCharacters(backend: RenderBackend, camera: Camera) {
        val drawables = mutableListOf<Pair<Float, () -> Unit>>()

        val playerScreen = camera.worldToScreen(session.player.entity.position)
        drawables.add(session.player.entity.position.y to {
            drawCharacter(
                backend = backend,
                p = playerScreen,
                zoom = camera.zoom,
                shirt = palette.playerShirt,
                pants = palette.playerPants,
                hair = palette.playerHair,
                hat = true,
                facing = session.player.entity.facing,
                walkPhase = session.player.animation.phase,
                moving = session.player.entity.velocity.length() > 8f
            )
        })

        for (npc in session.npcDirector.allNpcs()) {
            if (!npc.entity.active) continue
            val screen = camera.worldToScreen(npc.entity.position)
            val def = npc.definition
            drawables.add(npc.entity.position.y to {
                drawCharacter(
                    backend = backend,
                    p = screen,
                    zoom = camera.zoom,
                    shirt = RenderColor.rgb(def.appearance.shirtColor),
                    pants = RenderColor.rgb(def.appearance.pantsColor),
                    hair = RenderColor.rgb(def.appearance.hairColor),
                    hat = false,
                    facing = npc.entity.facing,
                    walkPhase = if (npc.isMoving) (npc.entity.position.x + npc.entity.position.y) / 40f else 0f,
                    moving = npc.isMoving,
                    scale = 0.95f
                )
            })
        }

        drawables.sortedBy { it.first }.forEach { it.second() }
    }

    private fun drawCharacter(
        backend: RenderBackend,
        p: Vec2,
        zoom: Float,
        shirt: RenderColor,
        pants: RenderColor,
        hair: RenderColor,
        hat: Boolean,
        facing: Direction,
        walkPhase: Float,
        moving: Boolean,
        scale: Float = 1f
    ) {
        val s = zoom * scale
        val bob = if (moving) abs(sin(walkPhase * kotlin.math.PI.toFloat() * 2f)) * 3f * s else 0f
        val bodyY = p.y - 16f * s + bob

        // Shadow
        backend.drawEllipse(Vec2(p.x, p.y), 14f * s, 5f * s, RenderColor.Black.withAlpha(0.18f))

        // Legs (walk cycle)
        val legSwing = if (moving) sin(walkPhase * kotlin.math.PI.toFloat() * 2f) * 5f * s else 0f
        backend.drawRect(p.x - 6f * s + legSwing, bodyY + 6f * s, 5f * s, 10f * s, pants, 2f * s)
        backend.drawRect(p.x + 1f * s - legSwing, bodyY + 6f * s, 5f * s, 10f * s, pants, 2f * s)

        // Body
        backend.drawRect(p.x - 9f * s, bodyY, 18f * s, 16f * s, shirt, 4f * s)

        // Arms
        backend.drawRect(p.x - 12f * s, bodyY + 2f * s, 4f * s, 11f * s, shirt, 2f * s)
        backend.drawRect(p.x + 8f * s, bodyY + 2f * s, 4f * s, 11f * s, shirt, 2f * s)

        // Head
        backend.drawCircle(Vec2(p.x, bodyY - 12f * s), 8f * s, palette.skin)

        // Hair (simple cap; direction-aware fringe)
        when (facing) {
            Direction.Up -> backend.drawCircle(Vec2(p.x, bodyY - 13f * s), 8.4f * s, hair)
            Direction.Down -> {
                backend.drawCircle(Vec2(p.x, bodyY - 12.4f * s), 8.4f * s, hair)
                backend.drawRect(p.x - 8.4f * s, bodyY - 20f * s, 16.8f * s, 5f * s, hair, 2f * s)
            }
            Direction.Left -> {
                backend.drawCircle(Vec2(p.x, bodyY - 12f * s), 8.4f * s, hair)
                backend.drawRect(p.x - 9f * s, bodyY - 20f * s, 6f * s, 8f * s, hair, 2f * s)
            }
            Direction.Right -> {
                backend.drawCircle(Vec2(p.x, bodyY - 12f * s), 8.4f * s, hair)
                backend.drawRect(p.x + 3f * s, bodyY - 20f * s, 6f * s, 8f * s, hair, 2f * s)
            }
        }

        if (hat) {
            // Straw hat — the player's summer identity.
            backend.drawEllipse(Vec2(p.x, bodyY - 19f * s), 12f * s, 4f * s, palette.straw)
            backend.drawRect(p.x - 6f * s, bodyY - 24f * s, 12f * s, 6f * s, palette.straw, 2f * s)
        }
    }

    // ------------------------------------------------------------
    // Location tags + interaction highlight
    // ------------------------------------------------------------

    private fun drawLocationTags(backend: RenderBackend, camera: Camera) {
        val region = session.world.region(session.player.state.regionId) ?: return
        for (location in region.locations) {
            if (location.id !in session.state.worldState.discoveredLocations) continue
            val p = camera.worldToScreen(location.anchor.toVec2())
            drawLabel(backend, Vec2(p.x, p.y - 34f), "${location.name}  ${location.nameJp}", 12f, palette.tagInk)
        }
    }

    private fun drawWeather(backend: RenderBackend, camera: Camera, view: ua.syt0r.kanji.desktop.game.engine.geom.Rect) {
        val rain = session.weather.rainIntensity()
        if (rain <= 0.02f && session.weather.current != ua.syt0r.kanji.desktop.game.time.WeatherKind.Snow) return
        val zoom = camera.zoom
        val startY = camera.worldToScreen(Vec2(view.x, view.y)).y
        val endY = camera.worldToScreen(Vec2(view.x, view.bottom)).y
        val step = 26f * zoom
        val t = System.nanoTime() / 1e9f
        var seed = 7
        var y = startY
        while (y < endY) {
            val offset = (seed * 37) % 53
            val x = (seed * 61) % 97 * zoom * 1.7f
            if (session.weather.current == ua.syt0r.kanji.desktop.game.time.WeatherKind.Snow) {
                // Snow (spec §41): soft flakes drifting slowly down and swaying,
                // instead of the rain's hard streaks.
                val sway = kotlin.math.sin(t * 1.4f + seed) * 5f * zoom
                val fall = ((t * 9f + seed * 13f) % 60f) * zoom
                backend.drawEllipse(
                    Vec2(x + sway, startY + (y - startY) + fall),
                    2.2f * zoom, 2.2f * zoom,
                    RenderColor.White.withAlpha(0.8f)
                )
            } else {
                backend.drawLine(
                    Vec2(x, y),
                    Vec2(x + 4f, y - 10f),
                    RenderColor.White.withAlpha(0.35f * rain),
                    1.5f
                )
            }
            seed += 3
            y += step
        }
    }

    private fun drawTimeTint(backend: RenderBackend, camera: Camera) {
        val tint = session.clock.lightTint()
        if (tint <= 0.01f) return
        // A translucent night/evening overlay over the whole viewport — the
        // world visibly changes with the time of day (spec §40).
        backend.drawRect(
            0f, 0f, camera.viewportWidth, camera.viewportHeight,
            RenderColor.rgb(0x1B2A52).withAlpha(tint)
        )
    }

    private fun drawSeasonTint(backend: RenderBackend, camera: Camera) {
        val season = session.seasons.current
        val alpha = season.tintAlpha
        if (alpha <= 0.01f) return
        // Each season shifts the palette (spec §42): spring is fresh green,
        // autumn is warm amber, winter is cool blue. Applied after the
        // time-of-day tint so both read together.
        backend.drawRect(
            0f, 0f, camera.viewportWidth, camera.viewportHeight,
            RenderColor.rgb(season.tint.toInt()).withAlpha(alpha)
        )
    }

    private fun drawDebugOverlays(backend: RenderBackend, camera: Camera, view: ua.syt0r.kanji.desktop.game.engine.geom.Rect) {
        val debug = session.debug
        if (!debug.enabled) return
        if (debug.showCollision) {
            val grid = session.tileGrid
            val tileSize = grid.tileSize
            val minTx = floor(view.x / tileSize).toInt().coerceAtLeast(0)
            val maxTx = floor(view.right / tileSize).toInt().coerceAtMost(grid.width - 1)
            val minTy = floor(view.y / tileSize).toInt().coerceAtLeast(0)
            val maxTy = floor(view.bottom / tileSize).toInt().coerceAtMost(grid.height - 1)
            for (ty in minTy..maxTy) {
                for (tx in minTx..maxTx) {
                    if (grid.isSolidAt(tx, ty)) {
                        val p = camera.worldToScreen(Vec2(tx * tileSize, ty * tileSize))
                        backend.drawRect(p.x, p.y, tileSize * camera.zoom, tileSize * camera.zoom, RenderColor.rgb(0xFF5252).withAlpha(0.4f))
                    }
                }
            }
        }
        if (debug.showInteractionBounds) {
            session.currentInteractable?.let { target ->
                val p = camera.worldToScreen(target.position)
                backend.drawCircle(p, target.radius * camera.zoom, RenderColor.rgb(0x40C4FF).withAlpha(0.5f))
            }
            // Player interaction radius
            val pp = camera.worldToScreen(session.player.entity.position)
            backend.drawCircle(pp, session.interaction.interactRadius * camera.zoom, RenderColor.rgb(0x40C4FF).withAlpha(0.25f))
        }
    }

    private fun drawLabel(backend: RenderBackend, at: Vec2, text: String, size: Float, color: RenderColor) {
        if (text.isBlank()) return
        val measured = backend.measureText(text, size)
        backend.drawRect(
            at.x - measured.x / 2f - 6f,
            at.y - measured.y / 2f - 3f,
            measured.x + 12f,
            measured.y + 6f,
            RenderColor.White.withAlpha(0.75f),
            4f
        )
        backend.drawText(text, at, size, color)
    }

    // ------------------------------------------------------------
    // Palette — the slice's warm summer town identity
    // ------------------------------------------------------------

    private class Palette {
        val grass = RenderColor.rgb(0x8BC34A)
        val sand = RenderColor.rgb(0xEED9A0)
        val water = RenderColor.rgb(0x4FC3F7)
        val road = RenderColor.rgb(0x5D6470)
        val sidewalk = RenderColor.rgb(0xC9CFD6)
        val path = RenderColor.rgb(0xD8C9A3)

        val shopBody = RenderColor.rgb(0xF7E7CE)
        val stationBody = RenderColor.rgb(0xE8E4DA)
        val stationRoof = RenderColor.rgb(0x6E7B8B)
        val houseBody = RenderColor.rgb(0xF4E3C6)
        val houseRoof = RenderColor.rgb(0xA66B4F)
        val doorBrown = RenderColor.rgb(0x6D4C41)
        val windowBlue = RenderColor.rgb(0x9AD1F5)
        val wood = RenderColor.rgb(0x8D6E63)
        val woodDark = RenderColor.rgb(0x6D4C41)
        val trunkBrown = RenderColor.rgb(0x795548)
        val treeGreen = RenderColor.rgb(0x66BB6A)
        val postGrey = RenderColor.rgb(0x78909C)
        val signBoard = RenderColor.rgb(0xFFE082)
        val machineBody = RenderColor.rgb(0xECEFF1)
        val machineGlass = RenderColor.rgb(0x80DEEA)
        val accentRed = RenderColor.rgb(0xE57373)
        val accentTeal = RenderColor.rgb(0x4DB6AC)
        val mailboxRed = RenderColor.rgb(0xE53935)
        val boatBrown = RenderColor.rgb(0x8D6E63)
        val lighthouseWhite = RenderColor.rgb(0xF5F5F5)
        val lighthouseTop = RenderColor.rgb(0xCFD8DC)
        val shrineRed = RenderColor.rgb(0xD32F2F)
        val lanternRed = RenderColor.rgb(0xEF5350)
        val stoneGrey = RenderColor.rgb(0xB0BEC5)
        val towelTeal = RenderColor.rgb(0x80CBC4)
        val bikeGrey = RenderColor.rgb(0x546E7A)

        val playerShirt = RenderColor.rgb(0xFFFFFF)
        val playerPants = RenderColor.rgb(0x546E7A)
        val playerHair = RenderColor.rgb(0x3E2723)
        val straw = RenderColor.rgb(0xF5DEA2)
        val skin = RenderColor.rgb(0xF2C49B)

        val cat = RenderColor.rgb(0x90A4AE)
        val bird = RenderColor.rgb(0x90A4AE)

        val ink = RenderColor.rgb(0x37474F)
        val tagInk = RenderColor.rgb(0x1B5E20)
        val cream = RenderColor.rgb(0xFFF8E1)
    }
}
