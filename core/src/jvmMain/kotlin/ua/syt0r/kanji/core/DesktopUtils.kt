package ua.syt0r.kanji.core

import java.io.File

fun getUserDataDirectory(): File {
    val osName = System.getProperty("os.name")
    val userDir = System.getProperty("user.home")
    return when {
        osName.contains("linux", true) -> {
            File("$userDir/.local/share/kaiteyo/")
        }

        osName.contains("windows", true) -> {
            File("$userDir/AppData/Local/Kaiteyo")
        }

        osName.contains("mac", true) -> {
            File("$userDir/Library/Kaiteyo")
        }

        else -> File("KaiteyoData/")
    }
}

fun getUserPreferencesFile(): File {
    return File(getUserDataDirectory(), "user.preferences_pb")
}