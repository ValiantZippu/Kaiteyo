package ua.syt0r.kanji.desktop.engine.jdata.extensions

import ua.syt0r.kanji.desktop.engine.jdata.profiles.DataPart

// ============================================================
// EXTENSION NAMESPACE SYSTEM
// The core stays stable; optional datasets attach under namespaces
// (pitchAccent, grammar, examples, frequencySourceX…). An extension
// declares which DataParts it provides, so consumers can detect what
// an installation actually contains instead of assuming it exists.
// ============================================================

data class ExtensionNamespace(
    val id: String,
    val displayName: String,
    val version: String = "1",
    val description: String = "",
    val provides: Set<DataPart> = emptySet()
)

class ExtensionRegistry {

    private val namespaces = linkedMapOf<String, ExtensionNamespace>()

    fun register(namespace: ExtensionNamespace): ExtensionRegistry {
        namespaces[namespace.id] = namespace
        return this
    }

    fun get(id: String): ExtensionNamespace? = namespaces[id]

    fun all(): List<ExtensionNamespace> = namespaces.values.sortedBy { it.id }

    fun provides(part: DataPart): List<ExtensionNamespace> =
        namespaces.values.filter { part in it.provides }.sortedBy { it.id }

    fun contains(id: String): Boolean = id in namespaces
}

object BuiltinExtensions {

    val PitchAccent = ExtensionNamespace(
        id = "pitchAccent",
        displayName = "Pitch accent",
        version = "1",
        description = "Positional pitch-accent markers (downstep model) per reading.",
        provides = setOf(DataPart.Pitch)
    )

    val Grammar = ExtensionNamespace(
        id = "grammar",
        displayName = "Grammar points",
        version = "1",
        description = "Grammar points with meaning, formation, JLPT and examples.",
        provides = setOf(DataPart.Grammar)
    )

    val Examples = ExtensionNamespace(
        id = "examples",
        displayName = "Example sentences",
        version = "1",
        description = "Example sentences referencing vocabulary entities by stable ID.",
        provides = setOf(DataPart.Examples)
    )

    val FrequencySources = ExtensionNamespace(
        id = "frequencySources",
        displayName = "Frequency sources",
        version = "1",
        description = "Additional per-corpus frequency observations (rank/score, source-scoped).",
        provides = setOf(DataPart.Frequency)
    )

    fun registryWithBuiltins(): ExtensionRegistry =
        ExtensionRegistry()
            .register(PitchAccent)
            .register(Grammar)
            .register(Examples)
            .register(FrequencySources)
}
