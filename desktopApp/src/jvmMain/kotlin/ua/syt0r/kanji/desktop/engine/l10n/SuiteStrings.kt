package ua.syt0r.kanji.desktop.engine.l10n

import java.util.Locale

// ============================================
// KAITEYO DESKTOP SUITE — LOCALIZATION
// The suite (JVM-only) owns its own UI strings;
// the shared core has its own Strings interface
// (English/Japanese) for the cross-platform app.
// Same convention here: an interface + two real
// implementations, resolved by locale. Adding a
// string requires editing the interface and both
// implementations (the interface enforces it).
// ============================================

interface SuiteStrings {

    // ---- Curriculum ----------------------------------------------
    val curriculumTitle: String
    val curriculumSubtitle: String
    val startCourse: String
    val switchCourse: String
    val courseCompletion: String
    val nextObjective: String
    val lessonComplete: String
    val noActiveCourse: String
    val objectivesLabel: String
    val notAvailable: String

    // ---- Knowledge graph -----------------------------------------
    val graphTitle: String
    val graphSubtitle: String
    val graphSearchPlaceholder: String
    val graphNoResults: String
    val componentsLabel: String
    val readingsLabel: String
    val meaningsLabel: String
    val relatedWordsLabel: String
    val jlptLabel: String
    val frequencyLabel: String
    val seenInMediaLabel: String
    val knowledgeLabel: String
    val backToGraph: String

    // ---- Reading -------------------------------------------------
    val readingTitle: String
    val importFile: String
    val pasteText: String
    val mineSentence: String
    val openDictionary: String
    val closeLookup: String

    // ---- Graph actions (Phase 4) ---------------------------------
    val practiceLabel: String
    val findPathLabel: String
    val pathSearchLabel: String
    val pathFromLabel: String
    val pathTargetPlaceholder: String
    val pathSameNode: String
    val pathNotfound: String
    val pathBlankError: String

    // ---- Grammar ------------------------------------------------
    val grammarTitle: String
    val startGrammarSession: String
    val builtInPatternsLabel: String
    val fromYourCardsLabel: String

    // ---- Exams ---------------------------------------------------
    val examsTitle: String
    val weeklyAssessmentLabel: String
    val mistakesReviewLabel: String
    val kanjiWorkshopLabel: String
    val tabTakeExam: String
    val tabResults: String
    val tabAnalytics: String
    val examSetupTitle: String
    val startExamButton: String
    val startJlptSimulation: String
    val questionsLabel: String
    val timeLimitLabel: String
    val noneLabel: String
    val scopeLabel: String
    val allDecks: String
    val jlptBandLabel: String
    val anyLevel: String
    val quickExams: String
    val takeAgain: String
    val doneButton: String
    val examNoContentTitle: String
    val examNoContentMessage: String
    val noMatchesForExam: String
    val nothingStudiedThisWeek: String
    val noMistakesRecorded: String

    // ---- Reading chrome (Phase 13) -------------------------------
    val readingSubtitle: String
    val libraryLabel: String
    val librarySubtitle: String
    val historyLabel: String
    val historySubtitle: String
    val noDocumentsTitle: String
    val noDocumentsBody: String
    val percentRead: String
    val bookmarkSuffix: String
    val removeFromLibrary: String
    val readLookupMine: String
    val tipClickWord: String
    val tipClickWordBody: String
    val tipBookmarks: String
    val tipBookmarksBody: String
    val tipSearch: String
    val tipSearchBody: String
    val tipProgress: String
    val tipProgressBody: String
    val noClipboardText: String
    val clipboardTextEmpty: String
    val readerSearchPlaceholder: String
    val readerMatchesSuffix: String

    // ---- Shortcuts -----------------------------------------------
    val keyboardShortcutsTitle: String
    val shortcutsEnabledOf: String
    val resetAll: String
    val rebindPlaceholder: String

    // ---- Plugins -------------------------------------------------
    val pluginsInstalledTab: String
    val pluginsMarketplaceTab: String
    val communityPluginsTitle: String
    val noPluginsTitle: String
    val noPluginsMessage: String
    val refreshLabel: String
    val installButton: String
    val updateButton: String
    val installedBadge: String
    val loadingMarketplace: String
    val marketplaceEmpty: String
    val uninstallConfirmTitle: String
    val pluginsCountSubtitle: String
    val enabledBadge: String
    val disabledBadge: String
    val noDescription: String
    val unknownAuthor: String
    val uninstallConfirmMessage: String
    val uninstallButton: String
    val uninstallActionDesc: String
    val marketplaceOfflineToast: String
    val marketplaceOfflineSubtitle: String
    val marketplaceOnlineSubtitle: String
    val marketplaceFetchingMessage: String
    val marketplaceEmptyMessage: String
    val installingLabel: String
    val downloadsSuffix: String
    val starsSuffix: String
    val installedVersionSuffix: String

    // ---- Library --------------------------------------------------
    val collectionsButton: String
    val allDecksLabel: String
    val studyAction: String
    val openButton: String
    val newFolderButton: String
    val newDeckButton: String
    val selectButton: String
    val exitSelectButton: String
    val selectAllButton: String
    val archiveButton: String
    val exportButton: String
    val deleteButton: String
    val folderNamePlaceholder: String
    val thisFolderEmpty: String
    val noDecksFound: String
    val nothingHereYet: String
    val newFolderTitle: String
    val deleteDecksTitle: String
    val favoritesOnlyLabel: String
    val gridViewDesc: String
    val listViewDesc: String
    val deckActionsDesc: String
    val studyTheseLabel: String

    // ---- Stats ----------------------------------------------------
    val reviewsLabel: String
    val newCardsLabel: String
    val forgottenLabel: String
    val accuracyLabel: String
    val retention7dLabel: String
    val studyStreakLabel: String
    val learningStreakLabel: String
    val reviewStreakLabel: String
    val bestStreakLabel: String
    val learningSpeedLabel: String
    val learningOverviewTitle: String
    val learningOverviewSubtitle: String
    val dailyGoalsLabel: String
    val knowledgeProfileTitle: String
    val knowledgeProfileSubtitle: String
    val overallCoverageLabel: String
    val theoreticalJlptLabel: String
    val vocabFrequencyCoverageLabel: String
    val writingHistoryTitle: String
    val writingHistorySubtitle: String
    val examAnalyticsTitle: String
    val examAnalyticsSubtitle: String
    val examsTakenLabel: String
    val averageScoreLabel: String
    val studyAccuracyLabel: String
    val examRecognitionLabel: String
    val examProductionLabel: String
    val examWritingLabel: String
    val longestLabel: String
    val writingAttemptsLabel: String
    val writingAccuracyLabel: String
    val reviewsTodayLabel: String
    val accuracyTodayLabel: String
    val kanjiStudiedLabel: String
    val kanjiEstablishedLabel: String
    val vocabularyLabel: String
    val vocabEstablishedLabel: String
    val kanaStudiedLabel: String
    val kanaEstablishedLabel: String
    val kanaWritingAccuracyLabel: String
    val againReviewsLabel: String
    val writingMistakesLabel: String
    val examMistakesLabel: String
    val lapsedCardsLabel: String
    val weakestWritingLabel: String
    val confidenceLabel: String
    val accuracyByExamTypeLabel: String
    val scoreTrendLabel: String
    val recentExamsLabel: String
    val accuracyTrendByCharLabel: String

    // ---- Dashboard ------------------------------------------------
    val quickActions: String
    val studyButton: String
    val writingButton: String
    val browseButton: String
    val newCardButton: String
    val libraryButton: String
    val dueNowLabel: String
    val newLabel: String
    val masteredLabel: String
    val studyTimeLabel: String
    val totalCardsLabel: String
    val thisWeekLabel: String
    val streakLabel: String
    val suspendedLabel: String
    val recalledLabel: String
    val favoriteCollections: String
    val immersionTitle: String
    val immersionSubtitle: String
    val openMediaButton: String
    val watchedTodayLabel: String
    val mediaStudyLabel: String
    val minedTodayLabel: String
    val lookupsTodayLabel: String
    val minedAll7dLabel: String
    val recentActivityTitle: String
    val viewAllLabel: String
    val activityHeatmapTitle: String
    val reviewPaceTitle: String
    val goalsTitle: String
    val goalsSubtitle: String
    val allStatsLabel: String
    val studyNowLabel: String
    val extraReviewLabel: String
    val studyWeekLabel: String
    val writingPracticeTitle: String
    val writingPracticeSubtitle: String
    val weakSpotsTitle: String
    val weakSpotsSubtitle: String
    val jlptCoverageTitle: String
    val jlptCoverageSubtitle: String
    val studyJlptButton: String
    val dueForecastTitle: String
    val dueForecastSubtitle: String
    val pinnedDecksTitle: String
    val recentImportsTitle: String
    val recommendedForYouTitle: String
    val recommendedSubtitle: String
    val welcomeTitle: String
    val welcomeBody: String
    val createDeckButton: String
    val importContentButton: String
    val exploreDictionaryButton: String
    val tryBrowseButton: String
    val noDataYetLabel: String

    // ---- Mining ---------------------------------------------------
    val miningTitle: String
    val thisWeekLabel2: String
    val thisMonthLabel: String
    val sourcesLabel: String
    val recentlyMinedLabel: String
    val nothingMinedYet: String
    val templatesLabel: String
    val newTemplateButton: String
    val useTemplateButton: String
    val mineNewCardTitle: String
    val headwordLabel: String
    val readingLabel: String
    val sentenceLabel: String
    val sentencePlaceholder: String
    val tagsLabel: String
    val tagsPlaceholder: String
    val destinationLabel: String
    val createCardButton: String
    val cancelButton: String

    // ---- Dictionary manager --------------------------------------
    val dictionariesTitle: String
    val dictionariesSubtitle: String
    val installDictionary: String
    val lookUpButton: String
    val hideLookup: String
    val noDictionariesTitle: String
    val noDictionariesMessage: String
}

object EnglishSuiteStrings : SuiteStrings {
    override val curriculumTitle = "Curriculum"
    override val curriculumSubtitle = "Structured courses measured against your real study data"
    override val startCourse = "Start course"
    override val switchCourse = "Switch course"
    override val courseCompletion = "Course"
    override val nextObjective = "Next objective"
    override val lessonComplete = "Lesson complete"
    override val noActiveCourse = "Pick a course to begin"
    override val objectivesLabel = "Objectives"
    override val notAvailable = "deck not installed"

    override val graphTitle = "Knowledge Graph"
    override val graphSubtitle = "Explore how kanji, words and components connect"
    override val graphSearchPlaceholder = "Search kanji or vocabulary…"
    override val graphNoResults = "No entry found — try a kanji or a word from your dictionaries"
    override val componentsLabel = "Components"
    override val readingsLabel = "Readings"
    override val meaningsLabel = "Meanings"
    override val relatedWordsLabel = "Words containing"
    override val jlptLabel = "JLPT"
    override val frequencyLabel = "Frequency"
    override val seenInMediaLabel = "Seen in media"
    override val knowledgeLabel = "Your knowledge"
    override val backToGraph = "Back to graph"

    override val readingTitle = "Reading"
    override val importFile = "Import file"
    override val pasteText = "Paste text"
    override val mineSentence = "Mine sentence"
    override val openDictionary = "Open dictionary"
    override val closeLookup = "Close lookup (Esc)"

    override val practiceLabel = "Practice"
    override val findPathLabel = "Find path"
    override val pathSearchLabel = "Search"
    override val pathFromLabel = "Find a relation path from"
    override val pathTargetPlaceholder = "Target expression (e.g. 食事)…"
    override val pathSameNode = "Same expression — already there."
    override val pathNotfound = "No relation path found within 4 hops."
    override val pathBlankError = "Type an expression to find the path to."

    override val grammarTitle = "Grammar"
    override val startGrammarSession = "Start grammar session"
    override val builtInPatternsLabel = "built-in patterns"
    override val fromYourCardsLabel = "from your cards"

    override val examsTitle = "Exams"
    override val weeklyAssessmentLabel = "Weekly assessment"
    override val mistakesReviewLabel = "Mistakes review"
    override val kanjiWorkshopLabel = "Kanji workshop"
    override val tabTakeExam = "Take exam"
    override val tabResults = "Results"
    override val tabAnalytics = "Analytics"
    override val examSetupTitle = "Exam setup"
    override val startExamButton = "Start exam"
    override val startJlptSimulation = "Start JLPT simulation"
    override val questionsLabel = "Questions"
    override val timeLimitLabel = "Time limit"
    override val noneLabel = "none"
    override val scopeLabel = "Scope"
    override val allDecks = "All decks"
    override val jlptBandLabel = "JLPT band"
    override val anyLevel = "Any level"
    override val quickExams = "Quick exams"
    override val takeAgain = "Take again"
    override val doneButton = "Done"
    override val examNoContentTitle = "Nothing to test yet"
    override val examNoContentMessage = "Exams are generated from real learning content. Study in the Library or Review first, then return here."
    override val noMatchesForExam = "No content matches this exam configuration"
    override val nothingStudiedThisWeek = "Nothing studied this week yet"
    override val noMistakesRecorded = "No mistakes recorded yet"

    override val readingSubtitle = "Native reading workspace — TXT, Markdown, HTML"
    override val libraryLabel = "Library"
    override val librarySubtitle = "document(s) · reading history below"
    override val historyLabel = "History"
    override val historySubtitle = "Recently read documents"
    override val noDocumentsTitle = "Nothing opened yet"
    override val noDocumentsBody = "Open a TXT, Markdown or HTML file (or paste clipboard text) to start reading. Click any Japanese word while reading to look it up — and mine it into a card."
    override val percentRead = "% read"
    override val bookmarkSuffix = "bookmark(s)"
    override val removeFromLibrary = "Remove from library"
    override val readLookupMine = "Read. Look up. Mine."
    override val tipClickWord = "Click any Japanese word"
    override val tipClickWordBody = "A dictionary card opens with the word's reading and definitions. Mine it and it lands in Review as a new card with the sentence attached."
    override val tipBookmarks = "Bookmarks & highlights"
    override val tipBookmarksBody = "Bookmark your current position with the star button. Highlights and bookmarks persist across restarts."
    override val tipSearch = "Search inside the document"
    override val tipSearchBody = "Use the find box to jump between every occurrence of a word or phrase."
    override val tipProgress = "Progress is real"
    override val tipProgressBody = "Your position and % read are tracked per document and recorded in reading history."
    override val noClipboardText = "No text on the clipboard"
    override val clipboardTextEmpty = "Clipboard text is empty"
    override val readerSearchPlaceholder = "Find in document…"
    override val readerMatchesSuffix = "matches"

    override val keyboardShortcutsTitle = "Keyboard shortcuts"
    override val shortcutsEnabledOf = "enabled of"
    override val resetAll = "Reset all"
    override val rebindPlaceholder = "Chord, e.g. Ctrl+Shift+K"

    override val pluginsInstalledTab = "Installed"
    override val pluginsMarketplaceTab = "Marketplace"
    override val communityPluginsTitle = "Community plugins"
    override val noPluginsTitle = "No plugins installed"
    override val noPluginsMessage = "Visit the Marketplace tab to discover and install plugins."
    override val refreshLabel = "Refresh"
    override val installButton = "Install"
    override val updateButton = "Update"
    override val installedBadge = "Installed"
    override val loadingMarketplace = "Loading marketplace…"
    override val marketplaceEmpty = "Marketplace is empty"
    override val uninstallConfirmTitle = "Uninstall plugin"
    override val pluginsCountSubtitle = "installed · enabled"
    override val enabledBadge = "enabled"
    override val disabledBadge = "disabled"
    override val noDescription = "No description"
    override val unknownAuthor = "Unknown"
    override val uninstallConfirmMessage = "Remove '%1\$s'? Contributed commands and panels will disappear."
    override val uninstallButton = "Uninstall"
    override val uninstallActionDesc = "Uninstall %1\$s"
    override val marketplaceOfflineToast = "Marketplace offline — showing featured plugins"
    override val marketplaceOfflineSubtitle = "GitHub unreachable — showing the featured catalog."
    override val marketplaceOnlineSubtitle = "Curated from GitHub — install with one click."
    override val marketplaceFetchingMessage = "Fetching the plugin index from GitHub."
    override val marketplaceEmptyMessage = "No plugins are published to the index yet."
    override val installingLabel = "Installing…"
    override val downloadsSuffix = "downloads"
    override val starsSuffix = "stars"
    override val installedVersionSuffix = "installed v"

    override val collectionsButton = "Collections"
    override val allDecksLabel = "All decks"
    override val studyAction = "Study"
    override val openButton = "Open"
    override val newFolderButton = "New folder"
    override val newDeckButton = "New deck"
    override val selectButton = "Select"
    override val exitSelectButton = "Exit select"
    override val selectAllButton = "Select all"
    override val archiveButton = "Archive"
    override val exportButton = "Export"
    override val deleteButton = "Delete"
    override val folderNamePlaceholder = "Folder name"
    override val thisFolderEmpty = "This folder is empty"
    override val noDecksFound = "No decks found"
    override val nothingHereYet = "Nothing here yet"
    override val newFolderTitle = "New folder"
    override val deleteDecksTitle = "Delete selected decks?"
    override val favoritesOnlyLabel = "Favorites only"
    override val gridViewDesc = "Grid view"
    override val listViewDesc = "List view"
    override val deckActionsDesc = "Deck actions"
    override val studyTheseLabel = "Study these"

    override val reviewsLabel = "Reviews"
    override val newCardsLabel = "New cards"
    override val forgottenLabel = "Forgotten"
    override val accuracyLabel = "Accuracy"
    override val retention7dLabel = "Retention (7d)"
    override val studyStreakLabel = "Study streak"
    override val learningStreakLabel = "Learning streak"
    override val reviewStreakLabel = "Review streak"
    override val bestStreakLabel = "Best streak"
    override val learningSpeedLabel = "Learning speed"
    override val learningOverviewTitle = "Learning Overview"
    override val learningOverviewSubtitle = "Due, streaks, goals, writing, mistakes and coverage — from real review events"
    override val dailyGoalsLabel = "Daily goals"
    override val knowledgeProfileTitle = "Knowledge Profile"
    override val knowledgeProfileSubtitle = "Study-based estimate from real stages and events — not a certification"
    override val overallCoverageLabel = "Overall estimated coverage"
    override val theoreticalJlptLabel = "Theoretical JLPT coverage"
    override val vocabFrequencyCoverageLabel = "Vocabulary frequency coverage"
    override val writingHistoryTitle = "Writing History"
    override val writingHistorySubtitle = "Real stroke evaluations per attempt — shape, direction and order mistakes"
    override val examAnalyticsTitle = "Exam Analytics"
    override val examAnalyticsSubtitle = "Study vs exam performance — recognition vs production, from real exam results"
    override val examsTakenLabel = "Exams taken"
    override val averageScoreLabel = "Average score"
    override val studyAccuracyLabel = "Study accuracy"
    override val examRecognitionLabel = "Exam recognition"
    override val examProductionLabel = "Exam production"
    override val examWritingLabel = "Exam writing"
    override val longestLabel = "Longest"
    override val writingAttemptsLabel = "Writing attempts"
    override val writingAccuracyLabel = "Writing accuracy"
    override val reviewsTodayLabel = "Reviews today"
    override val accuracyTodayLabel = "Accuracy today"
    override val kanjiStudiedLabel = "Kanji studied"
    override val kanjiEstablishedLabel = "Kanji established"
    override val vocabularyLabel = "Vocabulary"
    override val vocabEstablishedLabel = "Vocab established"
    override val kanaStudiedLabel = "Kana studied"
    override val kanaEstablishedLabel = "Kana established"
    override val kanaWritingAccuracyLabel = "Kana writing accuracy"
    override val againReviewsLabel = "Again reviews"
    override val writingMistakesLabel = "Writing mistakes"
    override val examMistakesLabel = "Exam mistakes"
    override val lapsedCardsLabel = "Lapsed cards"
    override val weakestWritingLabel = "Weakest writing"
    override val confidenceLabel = "Confidence"
    override val accuracyByExamTypeLabel = "Accuracy by exam type"
    override val scoreTrendLabel = "Score trend"
    override val recentExamsLabel = "Recent exams"
    override val accuracyTrendByCharLabel = "Accuracy trend by character"

    override val quickActions = "Quick actions"
    override val studyButton = "Study"
    override val writingButton = "Writing"
    override val browseButton = "Browse"
    override val newCardButton = "New card"
    override val libraryButton = "Library"
    override val dueNowLabel = "Due now"
    override val newLabel = "New"
    override val masteredLabel = "Mastered"
    override val studyTimeLabel = "Study time"
    override val totalCardsLabel = "Total cards"
    override val thisWeekLabel = "This week"
    override val streakLabel = "Streak"
    override val suspendedLabel = "Suspended"
    override val recalledLabel = "Recalled"
    override val favoriteCollections = "favorite collections"
    override val immersionTitle = "Immersion"
    override val immersionSubtitle = "Media activity today"
    override val openMediaButton = "Open Media"
    override val watchedTodayLabel = "Watched today"
    override val mediaStudyLabel = "Media study"
    override val minedTodayLabel = "Mined today"
    override val lookupsTodayLabel = "Lookups today"
    override val minedAll7dLabel = "Mined all (7d)"
    override val recentActivityTitle = "Recent Activity"
    override val viewAllLabel = "View all"
    override val activityHeatmapTitle = "Activity Heatmap"
    override val reviewPaceTitle = "Review Pace"
    override val goalsTitle = "Goals"
    override val goalsSubtitle = "Progress toward your targets"
    override val allStatsLabel = "All stats"
    override val studyNowLabel = "Study now"
    override val extraReviewLabel = "Extra review"
    override val studyWeekLabel = "Study"
    override val writingPracticeTitle = "Writing practice"
    override val writingPracticeSubtitle = "Weakest area from your real attempts"
    override val weakSpotsTitle = "Weak Spots"
    override val weakSpotsSubtitle = "Cards that need attention"
    override val jlptCoverageTitle = "JLPT coverage"
    override val jlptCoverageSubtitle = "Estimated study coverage from real SRS stages — not a prediction"
    override val studyJlptButton = "Study JLPT"
    override val dueForecastTitle = "Due forecast"
    override val dueForecastSubtitle = "Expected review workload, next 14 days"
    override val pinnedDecksTitle = "Pinned decks"
    override val recentImportsTitle = "Recent imports"
    override val recommendedForYouTitle = "Recommended for you"
    override val recommendedSubtitle = "Based on your current workload"
    override val welcomeTitle = "Welcome to Kaiteyo"
    override val welcomeBody = "Your Japanese study workspace is ready. Create or import a deck to begin — then explore the dictionary and mine your first cards."
    override val createDeckButton = "Create a deck"
    override val importContentButton = "Import content"
    override val exploreDictionaryButton = "Explore dictionary"
    override val tryBrowseButton = "Try Browse"
    override val noDataYetLabel = "No data yet"

    override val miningTitle = "Mining"
    override val thisWeekLabel2 = "This week"
    override val thisMonthLabel = "This month"
    override val sourcesLabel = "Sources"
    override val recentlyMinedLabel = "Recently mined"
    override val nothingMinedYet = "Nothing mined yet"
    override val templatesLabel = "Templates"
    override val newTemplateButton = "New template"
    override val useTemplateButton = "Use"
    override val mineNewCardTitle = "Mine a new card"
    override val headwordLabel = "Headword"
    override val readingLabel = "Reading"
    override val sentenceLabel = "Sentence"
    override val sentencePlaceholder = "Sentence context"
    override val tagsLabel = "Tags"
    override val tagsPlaceholder = "tags, comma, separated"
    override val destinationLabel = "Destination"
    override val createCardButton = "Create card"
    override val cancelButton = "Cancel"

    override val dictionariesTitle = "Dictionaries"
    override val dictionariesSubtitle = "installed · enabled · lookup by kanji, kana, romaji or English"
    override val installDictionary = "Install dictionary"
    override val lookUpButton = "Look up"
    override val hideLookup = "Hide lookup"
    override val noDictionariesTitle = "No dictionaries installed"
    override val noDictionariesMessage = "Install a Yomitan-compatible dictionary (ZIP or folder) to start looking up Japanese text. A bundled kanji dictionary is seeded on first run."
}

object JapaneseSuiteStrings : SuiteStrings {
    override val curriculumTitle = "カリキュラム"
    override val curriculumSubtitle = "実際の学習データに基づく構造化コース"
    override val startCourse = "コースを始める"
    override val switchCourse = "コースを切り替える"
    override val courseCompletion = "コース"
    override val nextObjective = "次の目標"
    override val lessonComplete = "レッスン完了"
    override val noActiveCourse = "コースを選択してください"
    override val objectivesLabel = "目標"
    override val notAvailable = "デッキ未導入"

    override val graphTitle = "知識グラフ"
    override val graphSubtitle = "漢字・単語・部品のつながりを探る"
    override val graphSearchPlaceholder = "漢字や単語を検索…"
    override val graphNoResults = "見つかりませんでした — 辞書にある漢字や単語を試してください"
    override val componentsLabel = "構成要素"
    override val readingsLabel = "読み"
    override val meaningsLabel = "意味"
    override val relatedWordsLabel = "この漢字を含む単語"
    override val jlptLabel = "JLPT"
    override val frequencyLabel = "頻度"
    override val seenInMediaLabel = "メディアで見た"
    override val knowledgeLabel = "あなたの知識"
    override val backToGraph = "グラフに戻る"

    override val readingTitle = "リーディング"
    override val importFile = "ファイルを開く"
    override val pasteText = "テキストを貼り付け"
    override val mineSentence = "文をマイニング"
    override val openDictionary = "辞書を開く"
    override val closeLookup = "閉じる (Esc)"

    override val practiceLabel = "練習"
    override val findPathLabel = "経路を探す"
    override val pathSearchLabel = "検索"
    override val pathFromLabel = "関係経路を探す:"
    override val pathTargetPlaceholder = "目標の表現（例: 食事）…"
    override val pathSameNode = "同じ表現です。"
    override val pathNotfound = "4ホップ以内の経路は見つかりませんでした。"
    override val pathBlankError = "経路の目標を入力してください。"

    override val grammarTitle = "文法"
    override val startGrammarSession = "文法セッションを始める"
    override val builtInPatternsLabel = "内蔵パターン"
    override val fromYourCardsLabel = "カードから"

    override val examsTitle = "試験"
    override val weeklyAssessmentLabel = "週間評価"
    override val mistakesReviewLabel = "間違いの復習"
    override val kanjiWorkshopLabel = "漢字ワークショップ"
    override val tabTakeExam = "試験を受ける"
    override val tabResults = "結果"
    override val tabAnalytics = "分析"
    override val examSetupTitle = "試験の設定"
    override val startExamButton = "試験を開始"
    override val startJlptSimulation = "JLPT模擬試験を開始"
    override val questionsLabel = "問題数"
    override val timeLimitLabel = "制限時間"
    override val noneLabel = "なし"
    override val scopeLabel = "範囲"
    override val allDecks = "すべてのデッキ"
    override val jlptBandLabel = "JLPTレベル"
    override val anyLevel = "全レベル"
    override val quickExams = "クイック試験"
    override val takeAgain = "もう一度受ける"
    override val doneButton = "完了"
    override val examNoContentTitle = "まだテストできる内容がありません"
    override val examNoContentMessage = "試験は実際の学習内容から生成されます。先にライブラリや復習で学習してください。"
    override val noMatchesForExam = "この試験設定に一致する内容がありません"
    override val nothingStudiedThisWeek = "今週はまだ学習していません"
    override val noMistakesRecorded = "まだ記録された間違いがありません"

    override val readingSubtitle = "ネイティブ読書ワークスペース — TXT・Markdown・HTML"
    override val libraryLabel = "ライブラリ"
    override val librarySubtitle = "件のドキュメント · 読書履歴は下に"
    override val historyLabel = "履歴"
    override val historySubtitle = "最近読んだドキュメント"
    override val noDocumentsTitle = "まだ何も開いていません"
    override val noDocumentsBody = "TXT・Markdown・HTMLファイルを開くか、クリップボードのテキストを貼り付けて読み始めましょう。読書中に日本語の単語をクリックすると辞書カードが開き、カードにマイニングできます。"
    override val percentRead = "%読了"
    override val bookmarkSuffix = "件のブックマーク"
    override val removeFromLibrary = "ライブラリから削除"
    override val readLookupMine = "読む。調べる。マイニング。"
    override val tipClickWord = "日本語の単語をクリック"
    override val tipClickWordBody = "単語の読みと意味の辞書カードが開きます。マイニングすると文ごとカードになって復習に加わります。"
    override val tipBookmarks = "ブックマークとハイライト"
    override val tipBookmarksBody = "スターで現在位置をブックマーク。ハイライトとブックマークは再起動後も保持されます。"
    override val tipSearch = "文書内を検索"
    override val tipSearchBody = "検索ボックスで単語やフレーズの出現箇所をジャンプできます。"
    override val tipProgress = "進捗は本物"
    override val tipProgressBody = "位置と%読了はドキュメントごとに記録され、読書履歴に残ります。"
    override val noClipboardText = "クリップボードにテキストがありません"
    override val clipboardTextEmpty = "クリップボードのテキストが空です"
    override val readerSearchPlaceholder = "文書内を検索…"
    override val readerMatchesSuffix = "件ヒット"

    override val keyboardShortcutsTitle = "キーボードショートカット"
    override val shortcutsEnabledOf = "件有効 / 全"
    override val resetAll = "すべてリセット"
    override val rebindPlaceholder = "キー、例: Ctrl+Shift+K"

    override val pluginsInstalledTab = "インストール済み"
    override val pluginsMarketplaceTab = "マーケットプレイス"
    override val communityPluginsTitle = "コミュニティプラグイン"
    override val noPluginsTitle = "プラグインがありません"
    override val noPluginsMessage = "マーケットプレイスタブでプラグインを探してインストールできます。"
    override val refreshLabel = "更新"
    override val installButton = "インストール"
    override val updateButton = "更新"
    override val installedBadge = "導入済み"
    override val loadingMarketplace = "マーケットプレイスを読み込み中…"
    override val marketplaceEmpty = "マーケットプレイスは空です"
    override val uninstallConfirmTitle = "プラグインをアンインストール"
    override val pluginsCountSubtitle = "インストール済み · 有効"
    override val enabledBadge = "有効"
    override val disabledBadge = "無効"
    override val noDescription = "説明なし"
    override val unknownAuthor = "不明"
    override val uninstallConfirmMessage = "「%1\$s」を削除しますか？ 追加されたコマンドとパネルは消えます。"
    override val uninstallButton = "アンインストール"
    override val uninstallActionDesc = "%1\$s をアンインストール"
    override val marketplaceOfflineToast = "マーケットプレイスがオフライン — おすすめプラグインを表示中"
    override val marketplaceOfflineSubtitle = "GitHub に接続できません — 厳選カタログを表示中。"
    override val marketplaceOnlineSubtitle = "GitHub から厳選 — ワンクリックでインストール。"
    override val marketplaceFetchingMessage = "GitHub からプラグイン一覧を取得中。"
    override val marketplaceEmptyMessage = "インデックスに公開されたプラグインはまだありません。"
    override val installingLabel = "インストール中…"
    override val downloadsSuffix = "ダウンロード"
    override val starsSuffix = "スター"
    override val installedVersionSuffix = "インストール済み v"

    override val collectionsButton = "コレクション"
    override val allDecksLabel = "すべてのデッキ"
    override val studyAction = "学習"
    override val openButton = "開く"
    override val newFolderButton = "新しいフォルダ"
    override val newDeckButton = "新しいデッキ"
    override val selectButton = "選択"
    override val exitSelectButton = "選択を終了"
    override val selectAllButton = "すべて選択"
    override val archiveButton = "アーカイブ"
    override val exportButton = "エクスポート"
    override val deleteButton = "削除"
    override val folderNamePlaceholder = "フォルダ名"
    override val thisFolderEmpty = "このフォルダは空です"
    override val noDecksFound = "デッキが見つかりません"
    override val nothingHereYet = "まだ何もありません"
    override val newFolderTitle = "新しいフォルダ"
    override val deleteDecksTitle = "選択したデッキを削除しますか？"
    override val favoritesOnlyLabel = "お気に入りのみ"
    override val gridViewDesc = "グリッド表示"
    override val listViewDesc = "リスト表示"
    override val deckActionsDesc = "デッキ操作"
    override val studyTheseLabel = "これらを学習"

    override val reviewsLabel = "復習"
    override val newCardsLabel = "新規カード"
    override val forgottenLabel = "忘れた"
    override val accuracyLabel = "正答率"
    override val retention7dLabel = "定着率（7日）"
    override val studyStreakLabel = "学習連続日数"
    override val learningStreakLabel = "学習ストリーク"
    override val reviewStreakLabel = "復習ストリーク"
    override val bestStreakLabel = "最長記録"
    override val learningSpeedLabel = "学習スピード"
    override val learningOverviewTitle = "学習概要"
    override val learningOverviewSubtitle = "実際の復習イベントから集計した期限・連続・目標・書き取り・間違い・カバレッジ"
    override val dailyGoalsLabel = "1日の目標"
    override val knowledgeProfileTitle = "知識プロフィール"
    override val knowledgeProfileSubtitle = "実際の段階とイベントに基づく推定 — 認定ではありません"
    override val overallCoverageLabel = "推定カバレッジ全体"
    override val theoreticalJlptLabel = "理論上の JLPT カバレッジ"
    override val vocabFrequencyCoverageLabel = "語彙頻度カバレッジ"
    override val writingHistoryTitle = "書き取り履歴"
    override val writingHistorySubtitle = "試行ごとの実際の筆順評価 — 形・方向・順序の間違い"
    override val examAnalyticsTitle = "試験分析"
    override val examAnalyticsSubtitle = "学習と試験の成績比較 — 認識と産出、実際の試験結果から"
    override val examsTakenLabel = "受験数"
    override val averageScoreLabel = "平均点"
    override val studyAccuracyLabel = "学習正答率"
    override val examRecognitionLabel = "試験・認識"
    override val examProductionLabel = "試験・産出"
    override val examWritingLabel = "試験・書き取り"
    override val longestLabel = "最長"
    override val writingAttemptsLabel = "書き取り回数"
    override val writingAccuracyLabel = "書き取り正答率"
    override val reviewsTodayLabel = "今日の復習"
    override val accuracyTodayLabel = "今日の正答率"
    override val kanjiStudiedLabel = "学習した漢字"
    override val kanjiEstablishedLabel = "定着した漢字"
    override val vocabularyLabel = "語彙"
    override val vocabEstablishedLabel = "定着した語彙"
    override val kanaStudiedLabel = "学習したかな"
    override val kanaEstablishedLabel = "定着したかな"
    override val kanaWritingAccuracyLabel = "かな書き取り正答率"
    override val againReviewsLabel = "Again 復習"
    override val writingMistakesLabel = "書き取りミス"
    override val examMistakesLabel = "試験ミス"
    override val lapsedCardsLabel = "失念カード"
    override val weakestWritingLabel = "最も苦手な書き取り"
    override val confidenceLabel = "信頼度"
    override val accuracyByExamTypeLabel = "試験タイプ別正答率"
    override val scoreTrendLabel = "スコア推移"
    override val recentExamsLabel = "最近の試験"
    override val accuracyTrendByCharLabel = "文字別正答率推移"

    override val quickActions = "クイックアクション"
    override val studyButton = "学習"
    override val writingButton = "書き取り"
    override val browseButton = "ブラウズ"
    override val newCardButton = "新規カード"
    override val libraryButton = "ライブラリ"
    override val dueNowLabel = "期限切れ"
    override val newLabel = "新規"
    override val masteredLabel = "習得済み"
    override val studyTimeLabel = "学習時間"
    override val totalCardsLabel = "カード総数"
    override val thisWeekLabel = "今週"
    override val streakLabel = "連続日数"
    override val suspendedLabel = "保留中"
    override val recalledLabel = "思い出せた"
    override val favoriteCollections = "お気に入りコレクション"
    override val immersionTitle = "イマージョン"
    override val immersionSubtitle = "今日のメディア活動"
    override val openMediaButton = "メディアを開く"
    override val watchedTodayLabel = "今日視聴"
    override val mediaStudyLabel = "メディア学習"
    override val minedTodayLabel = "今日の採掘"
    override val lookupsTodayLabel = "今日の検索"
    override val minedAll7dLabel = "採掘合計（7日）"
    override val recentActivityTitle = "最近のアクティビティ"
    override val viewAllLabel = "すべて表示"
    override val activityHeatmapTitle = "アクティビティヒートマップ"
    override val reviewPaceTitle = "復習ペース"
    override val goalsTitle = "目標"
    override val goalsSubtitle = "目標への進捗"
    override val allStatsLabel = "全統計"
    override val studyNowLabel = "今すぐ学習"
    override val extraReviewLabel = "追加復習"
    override val studyWeekLabel = "学習"
    override val writingPracticeTitle = "書き取り練習"
    override val writingPracticeSubtitle = "実際の取り組みから見えた弱点"
    override val weakSpotsTitle = "弱点"
    override val weakSpotsSubtitle = "注意が必要なカード"
    override val jlptCoverageTitle = "JLPT カバレッジ"
    override val jlptCoverageSubtitle = "実際のSRS段階から推定した学習カバレッジ — 予測ではありません"
    override val studyJlptButton = "JLPT を学習"
    override val dueForecastTitle = "期限予測"
    override val dueForecastSubtitle = "今後14日間の復習負荷の見込み"
    override val pinnedDecksTitle = "ピン留めしたデッキ"
    override val recentImportsTitle = "最近のインポート"
    override val recommendedForYouTitle = "あなたへのおすすめ"
    override val recommendedSubtitle = "現在の学習量に基づく"
    override val welcomeTitle = "Kaiteyo へようこそ"
    override val welcomeBody = "日本語学習ワークスペースの準備ができました。デッキを作成またはインポートして始めましょう — 辞書を探索して最初のカードを採掘できます。"
    override val createDeckButton = "デッキを作成"
    override val importContentButton = "コンテンツをインポート"
    override val exploreDictionaryButton = "辞書を探索"
    override val tryBrowseButton = "ブラウズを試す"
    override val noDataYetLabel = "データがまだありません"

    override val miningTitle = "採掘"
    override val thisWeekLabel2 = "今週"
    override val thisMonthLabel = "今月"
    override val sourcesLabel = "ソース"
    override val recentlyMinedLabel = "最近採掘したもの"
    override val nothingMinedYet = "まだ採掘されたものはありません"
    override val templatesLabel = "テンプレート"
    override val newTemplateButton = "新規テンプレート"
    override val useTemplateButton = "使用"
    override val mineNewCardTitle = "新規カードを採掘"
    override val headwordLabel = "見出し語"
    override val readingLabel = "読み"
    override val sentenceLabel = "例文"
    override val sentencePlaceholder = "文脈の例文"
    override val tagsLabel = "タグ"
    override val tagsPlaceholder = "タグ、カンマ区切り"
    override val destinationLabel = "保存先"
    override val createCardButton = "カードを作成"
    override val cancelButton = "キャンセル"

    override val dictionariesTitle = "辞書"
    override val dictionariesSubtitle = "導入済み · 有効 · 漢字・かな・ローマ字・英語で検索"
    override val installDictionary = "辞書をインストール"
    override val lookUpButton = "調べる"
    override val hideLookup = "検索を隠す"
    override val noDictionariesTitle = "辞書がインストールされていません"
    override val noDictionariesMessage = "Yomitan互換の辞書（ZIPまたはフォルダ）をインストールすると日本語のテキストを調べられます。初回起動時に内蔵の漢字辞書が導入されます。"
}

/** Resolve a suite string against the current JVM locale. */
fun resolveSuiteString(block: SuiteStrings.() -> String): String {
    val impl = if (Locale.getDefault().language == "ja") JapaneseSuiteStrings else EnglishSuiteStrings
    return block(impl)
}
