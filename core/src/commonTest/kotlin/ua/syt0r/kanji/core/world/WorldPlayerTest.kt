package ua.syt0r.kanji.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.test.Test

class WorldPlayerTest {

    private fun flatController(): PlayerController =
        PlayerController(
            settings = PlayerSettings(walkSpeed = 3.5, runSpeed = 7.0),
            terrainHeight = { _, _ -> 0.0 },
            waterLevel = { _, _ -> null },
            isBlocked = { _, _, _ -> false }
        )

    // ------------------------------------------------------------------
    // Movement
    // ------------------------------------------------------------------

    @Test
    fun idleInputStaysIdle() {
        val controller = flatController()
        controller.update(MovementInput(), 0.1)
        assertEquals(MovementState.Idle, controller.player.movement)
        assertEquals(0.0, controller.player.speed, 0.001)
    }

    @Test
    fun forwardInputMoves() {
        val controller = flatController()
        val start = controller.player.position
        controller.update(MovementInput(forward = 1f), 1.0)
        val pos = controller.player.position
        assertTrue(pos.horizontalDistanceTo(start) > 2.0) // 3.5 m/s * 1 s
        assertEquals(MovementState.Walking, controller.player.movement)
    }

    @Test
    fun runningIsFaster() {
        val controller = flatController()
        controller.update(MovementInput(forward = 1f, running = true), 1.0)
        // 7 m/s * 1 s
        assertTrue(controller.player.position.length() > 5.0)
        assertEquals(MovementState.Running, controller.player.movement)
    }

    @Test
    fun distanceTracked() {
        val controller = flatController()
        controller.update(MovementInput(forward = 1f), 1.0)
        assertTrue(controller.player.distanceWalkedMeters > 3.0)
    }

    @Test
    fun playTimeAccumulates() {
        val controller = flatController()
        controller.update(MovementInput(), 0.5)
        controller.update(MovementInput(), 0.5)
        assertEquals(1.0, controller.player.playTimeSeconds, 0.01)
    }

    @Test
    fun turningChangesYaw() {
        val controller = flatController()
        controller.update(MovementInput(turn = 1f), 1.0)
        // 180 deg/s * 1 s
        assertTrue(controller.player.facing.yawDegrees > 100f)
    }

    // ------------------------------------------------------------------
    // Terrain & water
    // ------------------------------------------------------------------

    @Test
    fun playerFollowsTerrainHeight() {
        val controller = PlayerController(
            terrainHeight = { x, z -> 12.0 + x * 0.1 },
            waterLevel = { _, _ -> null },
            isBlocked = { _, _, _ -> false }
        )
        controller.update(MovementInput(forward = 1f), 1.0)
        val expected = 12.0 + controller.player.position.x * 0.1
        assertEquals(expected, controller.player.position.y, 0.01)
    }

    @Test
    fun playerSwimsInWater() {
        val controller = PlayerController(
            terrainHeight = { _, _ -> -5.0 },
            waterLevel = { _, _ -> 0.0 },
            isBlocked = { _, _, _ -> false }
        )
        controller.update(MovementInput(forward = 1f), 1.0)
        assertEquals(MovementState.Swimming, controller.player.movement)
        assertTrue(controller.player.inWater)
        // Floats above the water floor
        assertTrue(controller.player.position.y > 0.0)
    }

    @Test
    fun playerSinksToGroundOnLand() {
        val controller = flatController()
        controller.update(MovementInput(forward = 1f), 1.0)
        assertEquals(0.0, controller.player.position.y, 0.01)
        assertTrue(controller.player.isGrounded)
    }

    // ------------------------------------------------------------------
    // Collision
    // ------------------------------------------------------------------

    @Test
    fun blockedPositionPreventsMovement() {
        val controller = PlayerController(
            terrainHeight = { _, _ -> 0.0 },
            waterLevel = { _, _ -> null },
            isBlocked = { _, _, _ -> true }
        )
        val start = controller.player.position
        controller.update(MovementInput(forward = 1f), 1.0)
        assertEquals(start, controller.player.position)
    }

    // ------------------------------------------------------------------
    // Camera modes
    // ------------------------------------------------------------------

    @Test
    fun cameraModeStartsThirdPerson() {
        val controller = flatController()
        assertEquals(CameraMode.ThirdPerson, controller.player.cameraMode)
    }

    @Test
    fun switchCameraToggles() {
        val controller = flatController()
        controller.update(MovementInput(switchCamera = true), 0.1)
        assertEquals(CameraMode.FirstPerson, controller.player.cameraMode)
        controller.update(MovementInput(switchCamera = true), 0.1)
        assertEquals(CameraMode.ThirdPerson, controller.player.cameraMode)
    }

    @Test
    fun firstPersonCameraIsAtEyeHeight() {
        val controller = flatController()
        controller.update(MovementInput(switchCamera = true), 0.1)
        val cam = controller.cameraPosition()
        assertEquals(1.6, cam.y, 0.01)
    }

    @Test
    fun thirdPersonCameraIsBehindPlayer() {
        val controller = flatController()
        controller.update(MovementInput(), 0.1)
        val cam = controller.thirdPersonCameraPosition(distance = 4.0)
        assertTrue(cam.horizontalDistanceTo(controller.player.position) > 3.5)
    }

    @Test
    fun cameraLookDirectionIsNormalized() {
        val controller = flatController()
        val look = controller.cameraLookDirection()
        val length = kotlin.math.sqrt(look.x * look.x + look.y * look.y + look.z * look.z)
        assertEquals(1.0, length, 0.01)
    }

    // ------------------------------------------------------------------
    // Sitting & vehicles
    // ------------------------------------------------------------------

    @Test
    fun sitStopsMovement() {
        val controller = flatController()
        controller.update(MovementInput(sit = true), 0.1)
        assertEquals(MovementState.Sitting, controller.player.movement)
        val pos = controller.player.position
        controller.update(MovementInput(forward = 1f), 1.0)
        assertEquals(pos, controller.player.position)
    }

    @Test
    fun unsitResumesIdle() {
        val controller = flatController()
        controller.update(MovementInput(sit = true), 0.1)
        controller.update(MovementInput(sit = false), 0.1)
        assertEquals(MovementState.Idle, controller.player.movement)
    }

    @Test
    fun boardAndAlightVehicle() {
        val controller = flatController()
        controller.board("car-1")
        assertEquals(MovementState.OnVehicle, controller.player.movement)
        assertEquals("car-1", controller.player.onVehicleId)
        val pos = WorldPosition(10.0, 0.0, 10.0)
        controller.alight(pos)
        assertEquals(MovementState.Idle, controller.player.movement)
        assertTrue(controller.player.onVehicleId == null)
        assertEquals(pos, controller.player.position)
    }

    @Test
    fun teleport() {
        val controller = flatController()
        controller.teleport(WorldPosition(100.0, 5.0, 100.0))
        assertEquals(WorldPosition(100.0, 5.0, 100.0), controller.player.position)
    }
}
