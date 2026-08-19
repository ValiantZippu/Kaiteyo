package ua.syt0r.kanji.desktop.engine.plugin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginSandboxTest {

    private val json = Json { encodeDefaults = true }

    private fun manifest(
        id: String = "sample-dict",
        capabilities: Set<PluginCapability> = setOf(PluginCapability.ReadDictionary, PluginCapability.SearchDictionary)
    ): String = json.encodeToString(
        Sandboxed            SandboxedPluginManifest(
            id = id,
            name = "Sample Dictionary",
            version = "1.0.0",
            minApiVersion = 1,
            capabilities = capabilities,
            entrypoints = listOf("dictionary")
        )
    )

    @Test
    fun validManifestApprovesWithResolvedPermissions() {
        val decision = PluginSandbox().inspect(manifest())
        assertTrue(decision.approved, "reason: ${decision.reason}")
        assertEquals(
            setOf(PluginCapability.ReadDictionary, PluginCapability.SearchDictionary),
            decision.permissions.capabilities
        )
    }

    @Test
    fun networkCapabilityDeniedWithoutApproval() {
        val decision = PluginSandbox().inspect(
            manifest(capabilities = setOf(PluginCapability.Network))
        )
        assertFalse(decision.approved)
        assertTrue(decision.reason.contains("Network"))
    }

    @Test
    fun networkCapabilityApprovedWhenUserApproves() {
        val decision = PluginSandbox().inspect(
            manifest(capabilities = setOf(PluginCapability.Network)),
            userApproved = setOf(PluginCapability.Network)
        )
        assertTrue(decision.approved)
        assertTrue(PluginCapability.Network in decision.permissions.capabilities)
    }

    @Test
    fun invalidJsonRejected() {
        val decision = PluginSandbox().inspect("{ not json")
        assertFalse(decision.approved)
        assertTrue(decision.reason.contains("JSON"))
    }

    @Test
    fun invalidManifestRejected() {
        val bad = json.encodeToString(
            Sandboxed            Sandboxed            SandboxedPluginManifest(
                id = "UPPER_CASE",
                name = "Bad",
                version = "not-semver",
                minApiVersion = 99,
                capabilities = emptySet(),
                entrypoints = listOf("")
            )
        )
        val decision = PluginSandbox().inspect(bad)
        assertFalse(decision.approved)
        assertTrue(decision.reason.contains("id"))
        assertTrue(decision.reason.contains("version"))
        assertTrue(decision.reason.contains("minApiVersion"))
        assertTrue(decision.reason.contains("entrypoints"))
    }

    @Test
    fun unknownCapabilitiesDroppedByDefault() {
        // A capability the platform does not know about cannot be declared in
        // the enum — the sandbox only grants from the allow-list, so a
        // manifest declaring only a non-grantable capability is rejected.
        val decision = PluginSandbox().inspect(
            manifest(capabilities = setOf(PluginCapability.WriteAppData))
        )
        assertFalse(decision.approved)
        assertTrue(decision.reason.contains("WriteAppData"))
    }

    @Test
    fun validatorRejectsDuplicateEntrypoints() {
        val result = PluginManifestValidator.validate(
            Sandboxed            SandboxedPluginManifest(
                id = "dup",
                name = "Dup",
                version = "1.0.0",
                capabilities = emptySet(),
                entrypoints = listOf("dict", "dict")
            )
        )
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("unique") })
    }
}
