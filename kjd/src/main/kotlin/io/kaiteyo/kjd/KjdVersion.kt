package io.kaiteyo.kjd

/**
 * Version information for the KJD Japanese language data platform.
 *
 * The platform is versioned independently of any consuming application.
 * Generated databases embed [GENERATOR_VERSION] so consumers can verify
 * compatibility with their SDK.
 */
object KjdVersion {
    /** Semantic version of the KJD SDK / generator. */
    const val SDK_VERSION: String = "1.0.0"

    /** Version of the canonical data schema — keep in sync with [io.kaiteyo.kjd.db.Schema.SCHEMA_VERSION]. */
    const val SCHEMA_VERSION: Int = 2

    /** Default database filename used by the CLI and generator. */
    const val DEFAULT_DATABASE_NAME: String = "kjd-japanese.db"

    /** The generator version written into every generated database. */
    val GENERATOR_VERSION: String = SDK_VERSION

    /** Human-readable platform name used in manifests and attribution. */
    const val PLATFORM_NAME: String = "Kaiteyo Japanese Data Platform (KJD)"
}
