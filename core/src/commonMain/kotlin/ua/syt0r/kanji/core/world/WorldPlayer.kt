package ua.syt0r.kanji.core.world

import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// WORLD — PLAYER CONTROLLER & CAMERA
// ------------------------------------------------------------
// The player is a first-class world entity: a position, a facing
// direction, a movement state, and camera modes. Movement is
// frame-rate independent (meter-per-second speeds). Camera
// supports third-person and first-person with smooth switching.
// ============================================================

/**
 * Player movement state.
 */
enum class MovementState(val label: String) {
    Idle("Idle"),
    Walking("Walking"),
    Running("Running"),
    Sitting("Sitting"),
    Swimming("Swimming"),
    OnVehicle("On vehicle"),
    OnTrain("On train")
}

/**
 * Camera modes.
 */
enum class CameraMode(val label: String) {
    ThirdPerson("Third person"),
    FirstPerson("First person")
}

/**
 * Player facing — yaw (degrees around Y, 0 = -Z) and pitch.
 */
data class Facing(
    val yawDegrees: Float = 0f,
    val pitchDegrees: Float = 0f
)

/**
 * The player entity.
 */
data class WorldPlayer(
    val id: String = "player",
    val position: WorldPosition = WorldPosition.Zero,
    val facing: Facing = Facing(),
    val movement: MovementState = MovementState.Idle,
    val cameraMode: CameraMode = CameraMode.ThirdPerson,
    /** Speed in meters/second. */
    val speed: Double = 0.0,
    val isGrounded: Boolean = true,
    val inWater: Boolean = false,
    val onVehicleId: String? = null,
    val currentLocationId: String? = null,
    /** Session distance walked. */
    val distanceWalkedMeters: Double = 0.0,
    val playTimeSeconds: Double = 0.0
)

/**
 * Player settings.
 */
data class PlayerSettings(
    val walkSpeed: Double = 3.5,
    val runSpeed: Double = 7.0,
    val swimSpeed: Double = 2.5,
    val turnSpeedDegrees: Float = 180f,
    val gravity: Double = -9.81,
    val jumpVelocity: Double = 4.5
)

/**
 * A movement input frame.
 */
data class MovementInput(
    /** Forward/back -1..1. */
    val forward: Float = 0f,
    /** Strafe left/right -1..1. */
    val strafe: Float = 0f,
    /** Turn left/right -1..1 (keyboard/analog). */
    val turn: Float = 0f,
    val running: Boolean = false,
    val jump: Boolean = false,
    val crouch: Boolean = false,
    val sit: Boolean = false,
    val interact: Boolean = false,
    val switchCamera: Boolean = false
)

/**
 * The player controller — converts input into position updates
 * and exposes the resulting player + camera state.
 */
class PlayerController(
    private val settings: PlayerSettings = PlayerSettings(),
    /** Terrain height lookup: (x, z) → ground level in meters. */
    private val terrainHeight: (Double, Double) -> Double = { _, _ -> 0.0 },
    /** Water level lookup: (x, z) → water level or null if dry. */
    private val waterLevel: (Double, Double) -> Double? = { _, _ -> null },
    /** Collision bounds lookup: (x, z, radius) → whether blocked. */
    private val isBlocked: (Double, Double, Double) -> Boolean = { _, _, _ -> false }
) {

    var player: WorldPlayer = WorldPlayer()
        private set

    /** Applies one frame of input. Returns the updated player. */
    fun update(input: MovementInput, deltaSeconds: Double): WorldPlayer {
        val current = player

        // Camera mode switch
        var cameraMode = current.cameraMode
        if (input.switchCamera) {
            cameraMode = if (cameraMode == CameraMode.ThirdPerson) CameraMode.FirstPerson else CameraMode.ThirdPerson
        }

        // Sitting is a toggle that stops movement
        if (input.sit && current.movement != MovementState.Sitting) {
            player = current.copy(
                movement = MovementState.Sitting,
                speed = 0.0,
                cameraMode = cameraMode
            )
            return player
        }
        if (current.movement == MovementState.Sitting && !input.sit) {
            player = current.copy(movement = MovementState.Idle)
            return player
        }

        if (current.movement == MovementState.Sitting) return player

        // Turning
        val yaw = current.facing.yawDegrees + input.turn * settings.turnSpeedDegrees * deltaSeconds.toFloat()

        // Movement direction from yaw + input
        val yawRad = Math.toRadians(yaw.toDouble())
        val forwardVec = WorldPosition(-sin(yawRad), 0.0, -cos(yawRad))
        val strafeVec = WorldPosition(-cos(yawRad), 0.0, sin(yawRad))

        val isRunning = input.running && input.forward > 0.1f
        val baseSpeed = when {
            isRunning -> settings.runSpeed
            input.forward != 0f || input.strafe != 0f -> settings.walkSpeed
            else -> 0.0
        }
        val speed = if (current.inWater) settings.swimSpeed else baseSpeed

        val move = forwardVec * (input.forward * speed * deltaSeconds) +
                strafeVec * (input.strafe * speed * deltaSeconds)

        var newPos = current.position + move

        // Collision: revert blocked axis
        val collisionRadius = 0.35
        if (isBlocked(newPos.x, newPos.z, collisionRadius)) {
            // Try to slide along axes
            val tryX = current.position.copy(x = newPos.x)
            if (!isBlocked(tryX.x, tryX.z, collisionRadius)) {
                newPos = tryX
            } else {
                val tryZ = current.position.copy(z = newPos.z)
                if (!isBlocked(tryZ.x, tryZ.z, collisionRadius)) {
                    newPos = tryZ
                } else {
                    newPos = current.position
                }
            }
        }

        // Ground + water
        val ground = terrainHeight(newPos.x, newPos.z)
        val water = waterLevel(newPos.x, newPos.z)
        val inWater = water != null && ground < water
        val y = if (inWater) {
            // Float on water surface
            water!!.coerceAtLeast(ground + 0.8)
        } else {
            ground.coerceAtLeast(0.0)
        }
        newPos = newPos.copy(y = y)

        val movement = when {
            inWater -> MovementState.Swimming
            (input.forward != 0f || input.strafe != 0f) && isRunning -> MovementState.Running
            (input.forward != 0f || input.strafe != 0f) -> MovementState.Walking
            else -> MovementState.Idle
        }

        val distance = move.length()
        player = current.copy(
            position = newPos,
            facing = Facing(yawDegrees = yaw, pitchDegrees = current.facing.pitchDegrees),
            movement = movement,
            speed = if (movement == MovementState.Idle) 0.0 else speed,
            cameraMode = cameraMode,
            inWater = inWater,
            isGrounded = !inWater && newPos.y <= ground + 0.01,
            distanceWalkedMeters = current.distanceWalkedMeters + distance,
            playTimeSeconds = current.playTimeSeconds + deltaSeconds
        )
        return player
    }

    /** Teleports the player (e.g. fast travel, world load). */
    fun teleport(position: WorldPosition, facing: Facing = Facing()) {
        player = player.copy(position = position, facing = facing, speed = 0.0)
    }

    /** Switches between third-person and first-person camera. */
    fun switchCamera() {
        player = player.copy(
            cameraMode = if (player.cameraMode == CameraMode.ThirdPerson) CameraMode.FirstPerson else CameraMode.ThirdPerson
        )
    }

    /** Boards a vehicle or train. */
    fun board(vehicleId: String) {
        player = player.copy(
            movement = MovementState.OnVehicle,
            onVehicleId = vehicleId,
            speed = 0.0
        )
    }

    /** Alights from a vehicle or train. */
    fun alight(position: WorldPosition) {
        player = player.copy(
            position = position,
            movement = MovementState.Idle,
            onVehicleId = null,
            speed = 0.0
        )
    }

    /** Enters a location interior. */
    fun enterLocation(locationId: String) {
        player = player.copy(currentLocationId = locationId)
    }

    /** Exits a location interior. */
    fun exitLocation() {
        player = player.copy(currentLocationId = null)
    }

    /** The camera position (first-person = eye height). */
    fun cameraPosition(): WorldPosition {
        val eyeHeight = if (player.cameraMode == CameraMode.FirstPerson) 1.6 else 0.0
        return player.position.copy(y = player.position.y + eyeHeight)
    }

    /** Camera look direction from facing. */
    fun cameraLookDirection(): WorldPosition {
        val yawRad = Math.toRadians(player.facing.yawDegrees.toDouble())
        val pitchRad = Math.toRadians(player.facing.pitchDegrees.toDouble())
        return WorldPosition(
            x = -sin(yawRad) * cos(pitchRad),
            y = sin(pitchRad),
            z = -cos(yawRad) * cos(pitchRad)
        )
    }

    /** Third-person camera position behind the player. */
    fun thirdPersonCameraPosition(distance: Double = 4.0): WorldPosition {
        val yawRad = Math.toRadians(player.facing.yawDegrees.toDouble())
        val behind = WorldPosition(
            x = sin(yawRad) * distance,
            y = 0.0,
            z = cos(yawRad) * distance
        )
        return player.position + behind + WorldPosition(0.0, 1.5, 0.0)
    }
}
