package ua.syt0r.kanji.core.knowledge.nodes

// ============================================================
// NODE TYPE REGISTRY — the node layer as code (ADR-0013)
// ------------------------------------------------------------
// Faithful to docs/architecture/nodes/NODE_TYPE_REGISTRY.md:
// every nodeType in Kaiteyo, its family, its parent, and its
// implementation status. The registry is the single source of
// truth for "what kinds of things exist in Kaiteyo's world" —
// adding a type is a registry change (plus a schemaVersion bump
// when storage changes), never an ad-hoc inline string.
//
// Families and statuses match the doc exactly:
//   CURRENT = exists today in some form
//   TARGET  = specified, not implemented
//   FUTURE  = intentionally postponed
// ============================================================

/** The six node families (§76) plus the SYSTEM catch-all. */
enum class NodeFamily(val label: String) {
    Language("LANGUAGE"),
    Learning("LEARNING"),
    Media("MEDIA"),
    World("WORLD"),
    Gameplay("GAMEPLAY"),
    User("USER"),
    System("SYSTEM")
}

/** Whether a node type exists today or is blueprint-only. */
enum class NodeStatus {
    Current,
    Target,
    Future
}

/**
 * Every node type in Kaiteyo (registry NODE_TYPE_REGISTRY.md).
 * [parent] is the registry "Parent" column (a supertype, not an
 * ownership edge). [schemaVersion] is bumped when the type's
 * fields change — the universal contract (Node) carries the
 * per-instance version.
 */
enum class NodeType(
    val id: String,
    val family: NodeFamily,
    val parent: NodeType? = null,
    val status: NodeStatus = NodeStatus.Target
) {

    // ── LANGUAGE ────────────────────────────────────────────
    Script("script", NodeFamily.Language, status = NodeStatus.Current),
    Kana("kana", NodeFamily.Language, parent = Script, status = NodeStatus.Current),
    Kanji("kanji", NodeFamily.Language, status = NodeStatus.Current),
    Component("component", NodeFamily.Language, parent = Kanji),
    Radical("radical", NodeFamily.Language, parent = Kanji),
    Vocabulary("vocabulary", NodeFamily.Language, status = NodeStatus.Current),
    Expression("expression", NodeFamily.Language, parent = Vocabulary),
    Reading("reading", NodeFamily.Language),
    Meaning("meaning", NodeFamily.Language),
    Grammar("grammar", NodeFamily.Language),
    Conjugation("conjugation", NodeFamily.Language),
    Sentence("sentence", NodeFamily.Language),
    Paragraph("paragraph", NodeFamily.Language, parent = Sentence),
    Story("story", NodeFamily.Language, parent = Paragraph),
    PitchPattern("pitch_pattern", NodeFamily.Language, parent = Vocabulary),
    FrequencyEntry("frequency_entry", NodeFamily.Language),
    Pronunciation("pronunciation", NodeFamily.Language, status = NodeStatus.Current),

    // ── LEARNING ────────────────────────────────────────────
    Course("course", NodeFamily.Learning),
    Lesson("lesson", NodeFamily.Learning, parent = Course),
    Topic("topic", NodeFamily.Learning, parent = Lesson),
    Objective("objective", NodeFamily.Learning),
    Exercise("exercise", NodeFamily.Learning),
    Question("question", NodeFamily.Learning),
    Exam("exam", NodeFamily.Learning),
    Deck("deck", NodeFamily.Learning, status = NodeStatus.Current),
    Note("note", NodeFamily.Learning, parent = Deck, status = NodeStatus.Current),
    Card("card", NodeFamily.Learning, status = NodeStatus.Current),
    Review("review", NodeFamily.Learning, parent = Card, status = NodeStatus.Current),
    StudySession("study_session", NodeFamily.Learning, status = NodeStatus.Current),
    UserKnowledge("user_knowledge", NodeFamily.Learning),
    MasteryState("mastery_state", NodeFamily.Learning, parent = UserKnowledge),

    // ── MEDIA ───────────────────────────────────────────────
    MediaSource("media_source", NodeFamily.Media),
    Series("series", NodeFamily.Media, parent = MediaSource),
    Anime("anime", NodeFamily.Media, parent = Series),
    Movie("movie", NodeFamily.Media, parent = Series),
    Episode("episode", NodeFamily.Media, parent = Series),
    Video("video", NodeFamily.Media, parent = Episode, status = NodeStatus.Current),
    Audio("audio", NodeFamily.Media, parent = MediaSource, status = NodeStatus.Current),
    SubtitleTrack("subtitle_track", NodeFamily.Media, status = NodeStatus.Current),
    SubtitleLine("subtitle_line", NodeFamily.Media, parent = SubtitleTrack, status = NodeStatus.Current),
    Scene("scene", NodeFamily.Media, parent = Episode),
    Screenshot("screenshot", NodeFamily.Media),
    Clip("clip", NodeFamily.Media),
    MiningEvent("mining_event", NodeFamily.Media, status = NodeStatus.Current),

    // ── WORLD ───────────────────────────────────────────────
    World("world", NodeFamily.World),
    Region("region", NodeFamily.World, parent = World),
    Prefecture("prefecture", NodeFamily.World, parent = Region),
    City("city", NodeFamily.World, parent = Prefecture),
    District("district", NodeFamily.World, parent = City),
    Neighborhood("neighborhood", NodeFamily.World, parent = District),
    MapCell("map_cell", NodeFamily.World),
    Street("street", NodeFamily.World),
    Building("building", NodeFamily.World, parent = Street),
    Interior("interior", NodeFamily.World, parent = Building),
    Landmark("landmark", NodeFamily.World),
    Station("station", NodeFamily.World),
    Road("road", NodeFamily.World),
    Railway("railway", NodeFamily.World),
    Beach("beach", NodeFamily.World),
    Park("park", NodeFamily.World),
    Shop("shop", NodeFamily.World, parent = Building),
    Restaurant("restaurant", NodeFamily.World, parent = Building),
    School("school", NodeFamily.World, parent = Building),
    Aquarium("aquarium", NodeFamily.World),
    Shrine("shrine", NodeFamily.World),
    Temple("temple", NodeFamily.World),
    NaturalFeature("natural_feature", NodeFamily.World),

    // ── GAMEPLAY ────────────────────────────────────────────
    Player("player", NodeFamily.Gameplay),
    Avatar("avatar", NodeFamily.Gameplay, parent = Player),
    Npc("npc", NodeFamily.Gameplay),
    NpcSchedule("npc_schedule", NodeFamily.Gameplay, parent = Npc),
    Interaction("interaction", NodeFamily.Gameplay),
    Activity("activity", NodeFamily.Gameplay),
    Quest("quest", NodeFamily.Gameplay),
    QuestObjective("quest_objective", NodeFamily.Gameplay, parent = Quest),
    StoryBeat("story_beat", NodeFamily.Gameplay),
    Dialogue("dialogue", NodeFamily.Gameplay),
    Discovery("discovery", NodeFamily.Gameplay),
    Collection("collection", NodeFamily.Gameplay),
    Photograph("photograph", NodeFamily.Gameplay),
    Achievement("achievement", NodeFamily.Gameplay, status = NodeStatus.Current),
    Reward("reward", NodeFamily.Gameplay),
    Event("event", NodeFamily.Gameplay),
    Season("season", NodeFamily.Gameplay, parent = World),
    WeatherState("weather_state", NodeFamily.Gameplay),
    DayNightState("day_night_state", NodeFamily.Gameplay, parent = World),

    // ── USER ────────────────────────────────────────────────
    Profile("profile", NodeFamily.User, status = NodeStatus.Current),
    Preferences("preferences", NodeFamily.User, parent = Profile, status = NodeStatus.Current),
    Goal("goal", NodeFamily.User, parent = Profile),
    KnowledgeState("knowledge_state", NodeFamily.User, parent = Profile),
    StudyHistory("study_history", NodeFamily.User, parent = Profile, status = NodeStatus.Current),
    MediaHistory("media_history", NodeFamily.User, parent = Profile, status = NodeStatus.Current),
    JourneyProgress("journey_progress", NodeFamily.User, parent = Profile),
    DiscoveryHistory("discovery_history", NodeFamily.User, parent = Profile),
    QuestProgress("quest_progress", NodeFamily.User, parent = Profile),
    ExamHistory("exam_history", NodeFamily.User, parent = Profile, status = NodeStatus.Current);

    companion object {
        private val byIdMap = entries.associateBy { it.id }

        /** Registry lookup — the only way to resolve a nodeType string. */
        fun byId(id: String): NodeType? = byIdMap[id]

        /** All types in a family, in registry order. */
        fun ofFamily(family: NodeFamily): List<NodeType> = entries.filter { it.family == family }
    }
}
