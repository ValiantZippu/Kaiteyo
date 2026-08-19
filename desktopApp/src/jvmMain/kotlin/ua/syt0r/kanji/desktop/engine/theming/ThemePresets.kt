package ua.syt0r.kanji.desktop.engine.theming

// ============================================
// BUILT-IN THEME PRESETS
// Curated palettes shipped with Kaiteyo.
// ============================================

object ThemePresets {

    val Signature = KaiteyoTheme(
        id = "signature", name = "Signature", description = "Lime + Orange studio",
        tags = listOf("dark", "default"), favorite = true
    )

    val Oled = KaiteyoTheme(
        id = "oled", name = "OLED Black", description = "True black",
        effects = ThemeEffects(oled = true),
        tags = listOf("dark", "oled")
    )

    val DarkGray = KaiteyoTheme(
        id = "dark-gray", name = "Dark Gray", description = "Softer dark",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#121212", surface = "#1A1A1A",
            surfaceElevated = "#242424", surfaceInteractive = "#2E2E2E", border = "#2A2A2A"
        ),
        tags = listOf("dark")
    )

    val Light = KaiteyoTheme(
        id = "light", name = "Light", description = "Clean light",
        baseMode = "light",
        colors = ThemeColors(
            background = "#F5F5F5", surface = "#EEEEEE",
            surfaceElevated = "#E8E8E8", surfaceInteractive = "#FCFCFC", border = "#D0D0D0",
            textPrimary = "#1A1A1A", textSecondary = "#606060", textMuted = "#A0A0A0", textInverse = "#F0F0F0",
            primary = "#9CE85E", primaryDark = "#7BC848", onPrimary = "#050505",
            hover = "#E6E6E6", selection = "#D9F0C2",
            sidebar = "#EFEFEF", navigation = "#EFEFEF", window = "#F5F5F5",
            dialog = "#FFFFFF", popup = "#FFFFFF", launchpad = "#EFEFEF", bubble = "#FFFFFF",
            shadow = "#33000000"
        ),
        tags = listOf("light")
    )

    val Reading = KaiteyoTheme(
        id = "reading", name = "Reading", description = "Warm paper tones",
        baseMode = "sepia",
        colors = ThemeColors(
            background = "#F5F0E8", surface = "#EDE5D8",
            surfaceElevated = "#E5DCC8", surfaceInteractive = "#F8F4EE", border = "#D4C8B8",
            textPrimary = "#3D3028", textSecondary = "#7A6B5D", textMuted = "#A89888", textInverse = "#F5F0E8",
            primary = "#B4894A", secondary = "#8A6A3A", onPrimary = "#F8F4EE",
            hover = "#E5DCC8", selection = "#E0D3BD",
            sidebar = "#EDE5D8", navigation = "#EDE5D8", window = "#F5F0E8",
            dialog = "#F8F4EE", popup = "#F8F4EE", launchpad = "#EDE5D8", bubble = "#F8F4EE",
            shadow = "#33302820"
        ),
        tags = listOf("light", "reading")
    )

    val Cream = KaiteyoTheme(
        id = "cream", name = "Cream", description = "Warm cream paper",
        baseMode = "cream",
        colors = ThemeColors(
            background = "#F7F3E8", surface = "#EDE6D4",
            surfaceElevated = "#E5DCC0", surfaceInteractive = "#FAF7F0", border = "#DED1BC",
            textPrimary = "#3A2F22", textSecondary = "#6B5B47", textMuted = "#988A75", textInverse = "#F7F3E8",
            primary = "#C2A25A", secondary = "#A67C39", onPrimary = "#F7F3E8",
            hover = "#EDE6D4", selection = "#E5DCC0",
            sidebar = "#EDE6D4", navigation = "#EDE6D4", window = "#F7F3E8",
            dialog = "#FAF7F0", popup = "#FAF7F0", launchpad = "#EDE6D4", bubble = "#FAF7F0",
            shadow = "#338A6E4A"
        ),
        tags = listOf("light", "warm", "reading")
    )

    val Paper = KaiteyoTheme(
        id = "paper", name = "Paper", description = "Clean off-white",
        baseMode = "paper",
        colors = ThemeColors(
            background = "#FCFAF5", surface = "#F5F2E8",
            surfaceElevated = "#EDE9DE", surfaceInteractive = "#FFFFFF", border = "#E6E0D4",
            textPrimary = "#2A2A2A", textSecondary = "#575757", textMuted = "#888888", textInverse = "#FCFAF5",
            primary = "#7BC8FF", secondary = "#FEAB57", onPrimary = "#0A0A0A",
            hover = "#EDE9DE", selection = "#DCE2EC",
            sidebar = "#F5F2E8", navigation = "#F5F2E8", window = "#FCFAF5",
            dialog = "#FFFFFF", popup = "#FFFFFF", launchpad = "#F5F2E8", bubble = "#FFFFFF",
            shadow = "#33606060"
        ),
        tags = listOf("light", "clean")
    )

    val Midnight = KaiteyoTheme(
        id = "midnight", name = "Midnight", description = "Deep blue dark",
        baseMode = "midnight",
        colors = ThemeColors(
            background = "#0A0D1A", surface = "#121622",
            surfaceElevated = "#1A1F30", surfaceInteractive = "#232940", border = "#2A324A",
            textPrimary = "#EAEAFF", textSecondary = "#B8B8D0", textMuted = "#808098", textInverse = "#0A0D1A",
            primary = "#7BC8FF", secondary = "#A78BFA", onPrimary = "#0A0D1A",
            hover = "#1C2131", selection = "#323B5C",
            sidebar = "#0A0D1A", navigation = "#0A0D1A", window = "#0A0D1A",
            dialog = "#1A1F30", popup = "#1A1F30", launchpad = "#0A0D1A", bubble = "#232940",
            shadow = "#33000000"
        ),
        tags = listOf("dark", "blue", "night")
    )


    val Solarized = KaiteyoTheme(
        id = "solarized", name = "Solarized", description = "Ethan Schoonover's palette",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#002B36", surface = "#073642",
            surfaceElevated = "#0A3A47", surfaceInteractive = "#14424E", border = "#335E6B",
            textPrimary = "#839496", textSecondary = "#657B83", textMuted = "#586E75", textInverse = "#FDF6E3",
            primary = "#268BD2", primaryDark = "#1E74A8", secondary = "#2AA198", secondaryDark = "#23867E",
            tertiary = "#B58900", onPrimary = "#002B36", onSecondary = "#002B36",
            error = "#DC322F", success = "#859900", warning = "#CB4B16", info = "#6C71C4",
            link = "#268BD2", hover = "#14424E", selection = "#1E4B58",
            sidebar = "#002B36", navigation = "#002B36", window = "#002B36",
            dialog = "#073642", popup = "#0A3A47", launchpad = "#002B36", bubble = "#14424E"
        ),
        tags = listOf("dark", "classic")
    )

    val Nord = KaiteyoTheme(
        id = "nord", name = "Nord", description = "Arctic, north-bluish",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#2E3440", surface = "#3B4252",
            surfaceElevated = "#434C5E", surfaceInteractive = "#4C566A", border = "#4C566A",
            textPrimary = "#ECEFF4", textSecondary = "#D8DEE9", textMuted = "#7B88A1", textInverse = "#2E3440",
            primary = "#88C0D0", primaryDark = "#5E81AC", secondary = "#81A1C1", secondaryDark = "#5E81AC",
            tertiary = "#B48EAD", onPrimary = "#2E3440", onSecondary = "#2E3440",
            error = "#BF616A", success = "#A3BE8C", warning = "#EBCB8B", info = "#5E81AC",
            link = "#88C0D0", hover = "#4C566A", selection = "#3B5A75",
            sidebar = "#2E3440", navigation = "#2E3440", window = "#2E3440",
            dialog = "#3B4252", popup = "#434C5E", launchpad = "#2E3440", bubble = "#4C566A"
        ),
        tags = listOf("dark", "nord")
    )

    val Catppuccin = KaiteyoTheme(
        id = "catppuccin", name = "Catppuccin", description = "Soothing pastel Mocha",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#1E1E2E", surface = "#313244",
            surfaceElevated = "#45475A", surfaceInteractive = "#585B70", border = "#585B70",
            textPrimary = "#CDD6F4", textSecondary = "#A6ADC8", textMuted = "#7F849C", textInverse = "#1E1E2E",
            primary = "#89B4FA", primaryDark = "#74A7F0", secondary = "#CBA6F7", secondaryDark = "#B89BE4",
            tertiary = "#F5C2E7", onPrimary = "#1E1E2E", onSecondary = "#1E1E2E",
            error = "#F38BA8", success = "#A6E3A1", warning = "#F9E2AF", info = "#89DCEB",
            link = "#89B4FA", hover = "#585B70", selection = "#3E4A8A",
            sidebar = "#181825", navigation = "#181825", window = "#1E1E2E",
            dialog = "#313244", popup = "#45475A", launchpad = "#181825", bubble = "#585B70"
        ),
        tags = listOf("dark", "pastel")
    )

    val Gruvbox = KaiteyoTheme(
        id = "gruvbox", name = "Gruvbox", description = "Retro groove",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#282828", surface = "#3C3836",
            surfaceElevated = "#504945", surfaceInteractive = "#665C54", border = "#665C54",
            textPrimary = "#EBDBB2", textSecondary = "#D5C4A1", textMuted = "#928374", textInverse = "#282828",
            primary = "#B8BB26", primaryDark = "#98971A", secondary = "#FB4934", secondaryDark = "#CC241D",
            tertiary = "#83A598", onPrimary = "#282828", onSecondary = "#FBF1C7",
            error = "#FB4934", success = "#B8BB26", warning = "#FABD2F", info = "#458588",
            link = "#83A598", hover = "#665C54", selection = "#4E4B38",
            sidebar = "#282828", navigation = "#282828", window = "#282828",
            dialog = "#3C3836", popup = "#504945", launchpad = "#282828", bubble = "#665C54"
        ),
        tags = listOf("dark", "retro")
    )

    val TokyoNight = KaiteyoTheme(
        id = "tokyo-night", name = "Tokyo Night", description = "Neon Tokyo dusk",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#1A1B26", surface = "#16161E",
            surfaceElevated = "#24283B", surfaceInteractive = "#2A2E3F", border = "#3B4261",
            textPrimary = "#C0CAF5", textSecondary = "#A9B1D6", textMuted = "#565F89", textInverse = "#16161E",
            primary = "#7AA2F7", primaryDark = "#5C7EE6", secondary = "#BB9AF7", secondaryDark = "#9D7CD8",
            tertiary = "#7DCFFF", onPrimary = "#16161E", onSecondary = "#16161E",
            error = "#F7768E", success = "#9ECE6A", warning = "#E0AF68", info = "#2AC3DE",
            link = "#7AA2F7", hover = "#2A2E3F", selection = "#33467C",
            sidebar = "#16161E", navigation = "#16161E", window = "#1A1B26",
            dialog = "#24283B", popup = "#2A2E3F", launchpad = "#16161E", bubble = "#2A2E3F"
        ),
        tags = listOf("dark", "neon")
    )

    val Dracula = KaiteyoTheme(
        id = "dracula", name = "Dracula", description = "Dark theme with bright hues",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#282A36", surface = "#21222C",
            surfaceElevated = "#343746", surfaceInteractive = "#44475A", border = "#44475A",
            textPrimary = "#F8F8F2", textSecondary = "#BFBFBF", textMuted = "#6272A4", textInverse = "#282A36",
            primary = "#BD93F9", primaryDark = "#A67BE3", secondary = "#FF79C6", secondaryDark = "#E85EB2",
            tertiary = "#8BE9FD", onPrimary = "#282A36", onSecondary = "#282A36",
            error = "#FF5555", success = "#50FA7B", warning = "#F1FA8C", info = "#8BE9FD",
            link = "#8BE9FD", hover = "#44475A", selection = "#4A4470",
            sidebar = "#21222C", navigation = "#21222C", window = "#282A36",
            dialog = "#343746", popup = "#44475A", launchpad = "#21222C", bubble = "#44475A"
        ),
        tags = listOf("dark", "vibrant")
    )

    val NothingOS = KaiteyoTheme(
        id = "nothing-os", name = "Nothing OS", description = "Monochrome minimalism",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#0F0F0F", surface = "#1A1A1A",
            surfaceElevated = "#232323", surfaceInteractive = "#2D2D2D", border = "#2E2E2E",
            textPrimary = "#FFFFFF", textSecondary = "#9E9E9E", textMuted = "#5F5F5F", textInverse = "#0F0F0F",
            primary = "#D5D5D5", primaryDark = "#B0B0B0", secondary = "#A6A6A6", secondaryDark = "#8A8A8A",
            tertiary = "#757575", onPrimary = "#0F0F0F", onSecondary = "#0F0F0F",
            error = "#E5484D", success = "#46A758", warning = "#FFB224", info = "#0091FF",
            link = "#0091FF", hover = "#2D2D2D", selection = "#333333",
            sidebar = "#0F0F0F", navigation = "#0F0F0F", window = "#0F0F0F",
            dialog = "#1A1A1A", popup = "#232323", launchpad = "#0F0F0F", bubble = "#2D2D2D"
        ),
        tags = listOf("dark", "mono", "minimal")
    )

    val Material = KaiteyoTheme(
        id = "material", name = "Material", description = "Material You feel",
        effects = ThemeEffects(material = true),
        colors = ThemeColors(
            primary = "#BB86FC", primaryDark = "#9C6FE0", secondary = "#03DAC6", secondaryDark = "#00B8A6",
            tertiary = "#CF6679", onPrimary = "#050505", onSecondary = "#050505",
            error = "#CF6679", success = "#03DAC6", warning = "#F7B267", info = "#4F8EF7",
            link = "#BB86FC"
        ),
        tags = listOf("material")
    )

    val Glass = KaiteyoTheme(
        id = "glass", name = "Glass", description = "Frosted translucency",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#0A0E14", surface = "#141A26",
            surfaceElevated = "#1A2232", surfaceInteractive = "#222C40", border = "#2E3A52",
            textPrimary = "#E8F0FF", textSecondary = "#A8B8D0", textMuted = "#5F6B85", textInverse = "#0A0E14",
            primary = "#6EC9FF", primaryDark = "#4DA8E8", secondary = "#9B8CFF", secondaryDark = "#7B6CE0",
            tertiary = "#FFB86C", onPrimary = "#0A0E14", onSecondary = "#0A0E14",
            error = "#FF6B6B", success = "#7CEE9B", warning = "#FFC66B", info = "#6EC9FF",
            link = "#6EC9FF", hover = "#222C40", selection = "#2B3B58",
            sidebar = "#0D1320", navigation = "#0D1320", window = "#0A0E14",
            dialog = "#182032", popup = "#1A2232", launchpad = "#0D1320", bubble = "#222C40"
        ),
        effects = ThemeEffects(blur = true, transparency = true, glassOpacity = 0.75f),
        tags = listOf("dark", "glass")
    )

    val CottonCandy = KaiteyoTheme(
        id = "cotton", name = "Cotton Candy", description = "Pastel",
        colors = ThemeColors(
            primary = "#D4A5F0", secondary = "#FFB5C5", tertiary = "#A0D2FF"
        ),
        tags = listOf("pastel")
    )

    val Ocean = KaiteyoTheme(
        id = "ocean", name = "Ocean", description = "Cool blue",
        colors = ThemeColors(
            primary = "#00D4AA", secondary = "#00A8FF", tertiary = "#0D47A1"
        ),
        tags = listOf("cool")
    )

    val Forest = KaiteyoTheme(
        id = "forest", name = "Forest", description = "Earthy green",
        colors = ThemeColors(
            primary = "#81C784", secondary = "#A5D6A7", tertiary = "#5D4037"
        ),
        tags = listOf("nature")
    )

    val all: List<KaiteyoTheme> = listOf(
        Signature, Oled, DarkGray, Light, Reading,
        Cream, Paper, Midnight,
        Solarized, Nord, Catppuccin, Gruvbox, TokyoNight, Dracula,
        NothingOS, Material, Glass,
        CottonCandy, Ocean, Forest
    )

    val default: KaiteyoTheme = Signature

    fun byId(id: String): KaiteyoTheme = all.firstOrNull { it.id == id } ?: default
}
