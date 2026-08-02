package ua.syt0r.kanji.core.user_data.database

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// ============================================
// CARD DATABASE MANAGER
// Repository for all enhanced card operations
// (tags, flags, notes, history, shortcuts, etc.)
// ============================================

interface CardDatabaseManager {
    // Tags
    suspend fun getAllTags(): List<CardTagData>
    suspend fun createTag(name: String, color: String, parentId: Long? = null): Long
    suspend fun updateTag(id: Long, name: String, color: String, parentId: Long?)
    suspend fun deleteTag(id: Long)
    suspend fun mergeTags(sourceId: Long, targetId: Long)
    suspend fun getTagsForCard(cardKey: String, practiceType: Long): List<CardTagData>
    suspend fun addTagToCard(cardKey: String, practiceType: Long, tagId: Long)
    suspend fun removeTagFromCard(cardKey: String, practiceType: Long, tagId: Long)
    suspend fun getCardsByTag(tagId: Long): List<CardRef>
    suspend fun searchTags(query: String, limit: Int = 20): List<CardTagData>

    // Flags
    suspend fun setFlag(cardKey: String, practiceType: Long, flagType: Int)
    suspend fun removeFlag(cardKey: String, practiceType: Long)
    suspend fun getFlag(cardKey: String, practiceType: Long): Int?
    suspend fun getCardsByFlag(flagType: Int): List<CardRef>
    suspend fun bulkSetFlag(entries: List<Triple<String, Long, Int>>)

    // Notes
    suspend fun setNote(cardKey: String, practiceType: Long, content: String, format: Int = 0)
    suspend fun getNote(cardKey: String, practiceType: Long): String?
    suspend fun deleteNote(cardKey: String, practiceType: Long)

    // Study History
    suspend fun addHistoryEntry(actionType: Int, cardKey: String?, practiceType: Long?, details: String)
    suspend fun getRecentHistory(limit: Int = 50): List<StudyHistoryRow>
    suspend fun getHistoryForCard(cardKey: String, practiceType: Long, limit: Int = 20): List<StudyHistoryRow>

    // Shortcuts
    suspend fun saveShortcut(actionId: String, primaryKey: String, modifierFlags: Int = 0, profileName: String = "default")
    suspend fun getShortcut(actionId: String, profileName: String = "default"): ShortcutRow?
    suspend fun getAllShortcuts(): List<ShortcutRow>
    suspend fun deleteShortcut(actionId: String, profileName: String = "default")
    suspend fun resetShortcuts(profileName: String = "default")

    // Backup metadata
    suspend fun recordBackup(filename: String, fileSize: Long, checksum: String, isAutomatic: Boolean, notes: String = "")
    suspend fun getBackups(limit: Int = 20, offset: Int = 0): List<BackupRow>
    suspend fun deleteBackupMetadata(id: Long)

    // Filtered Decks
    suspend fun createFilteredDeck(name: String, searchQuery: String, maxCards: Int = 9999): Long
    suspend fun getFilteredDecks(): List<FilteredDeckRow>
    suspend fun deleteFilteredDeck(id: Long)

    // History utilities
    suspend fun getHistoryByAction(actionType: Int, limit: Int = 100): List<StudyHistoryRow>
    suspend fun clearHistory(beforeTimestamp: Long = Long.MAX_VALUE)
    suspend fun getTagUsageCount(tagId: Long): Long

    // Change events
    val changesFlow: SharedFlow<Unit>
}

data class CardTagData(
    val id: Long,
    val name: String,
    val color: String,
    val parentId: Long?,
    val createdAt: Instant,
    val modifiedAt: Instant
)

data class CardRef(val cardKey: String, val practiceType: Long)

data class StudyHistoryRow(
    val id: Long,
    val actionType: Int,
    val cardKey: String?,
    val practiceType: Long?,
    val details: String,
    val timestamp: Instant
)

data class ShortcutRow(
    val id: Long,
    val actionId: String,
    val primaryKey: String,
    val modifierFlags: Int,
    val isEnabled: Boolean,
    val profileName: String
)

data class BackupRow(
    val id: Long,
    val filename: String,
    val fileSize: Long,
    val checksum: String,
    val isAutomatic: Boolean,
    val createdAt: Instant,
    val notes: String?
)

data class FilteredDeckRow(
    val id: Long,
    val name: String,
    val searchQuery: String,
    val maxCards: Int,
    val isRescheduled: Boolean,
    val createdAt: Instant
)

class CardDatabaseManagerImpl(
    private val transactionScope: UserDataDatabaseContract.TransactionScope
) : CardDatabaseManager {

    override val changesFlow: SharedFlow<Unit> =
        (transactionScope as? ObservableRepository)?.changesFlow
            ?: error("TransactionScope must be ObservableRepository")

    override suspend fun getAllTags(): List<CardTagData> = transactionScope.readTransaction {
        getAllTags().executeAsList().map { it.toTagData() }
    }

    override suspend fun createTag(name: String, color: String, parentId: Long?): Long =
        transactionScope.writeTransaction {
            val now = Clock.System.now().toEpochMilliseconds()
            insertTag(name, color, parentId, now, now)
            getLastInsertRowId().executeAsOne()
        }

    override suspend fun updateTag(id: Long, name: String, color: String, parentId: Long?) =
        transactionScope.writeTransaction {
            val now = Clock.System.now().toEpochMilliseconds()
            updateTag(name, color, parentId, now, id)
        }

    override suspend fun deleteTag(id: Long) = transactionScope.writeTransaction {
        deleteTag(id)
        deleteUnusedTags()
    }

    override suspend fun mergeTags(sourceId: Long, targetId: Long) =
        transactionScope.writeTransaction {
            mergeTag(targetId, sourceId)
            deleteTag(sourceId)
        }

    override suspend fun getTagsForCard(cardKey: String, practiceType: Long): List<CardTagData> =
        transactionScope.readTransaction {
            getTagsByCard(cardKey, practiceType).executeAsList().map { it.toTagData() }
        }

    override suspend fun addTagToCard(cardKey: String, practiceType: Long, tagId: Long) =
        transactionScope.writeTransaction {
            addTagToCard(cardKey, practiceType, tagId)
        }

    override suspend fun removeTagFromCard(cardKey: String, practiceType: Long, tagId: Long) =
        transactionScope.writeTransaction {
            removeTagFromCard(cardKey, practiceType, tagId)
        }

    override suspend fun getCardsByTag(tagId: Long): List<CardRef> =
        transactionScope.readTransaction {
            getCardsByTag(tagId).executeAsList().map {
                CardRef(it.card_key, it.practice_type)
            }
        }

    override suspend fun searchTags(query: String, limit: Int): List<CardTagData> =
        transactionScope.readTransaction {
            searchTags("%$query%", limit.toLong()).executeAsList().map { it.toTagData() }
        }

    override suspend fun setFlag(cardKey: String, practiceType: Long, flagType: Int) =
        transactionScope.writeTransaction {
            val now = Clock.System.now().toEpochMilliseconds()
            setCardFlag(cardKey, practiceType, flagType.toLong(), now)
        }

    override suspend fun removeFlag(cardKey: String, practiceType: Long) =
        transactionScope.writeTransaction {
            removeCardFlag(cardKey, practiceType)
        }

    override suspend fun getFlag(cardKey: String, practiceType: Long): Int? =
        transactionScope.readTransaction {
            getCardFlag(cardKey, practiceType).executeAsOneOrNull()?.flag_type?.toInt()
        }

    override suspend fun getCardsByFlag(flagType: Int): List<CardRef> =
        transactionScope.readTransaction {
            getCardsByFlag(flagType.toLong()).executeAsList().map {
                CardRef(it.card_key, it.practice_type)
            }
        }

    override suspend fun bulkSetFlag(entries: List<Triple<String, Long, Int>>) =
        transactionScope.writeTransaction {
            val now = Clock.System.now().toEpochMilliseconds()
            entries.forEach { (key, type, flagType) ->
                bulkSetFlag(key, type, flagType.toLong(), now)
            }
        }

    override suspend fun setNote(cardKey: String, practiceType: Long, content: String, format: Int) =
        transactionScope.writeTransaction {
            val now = Clock.System.now().toEpochMilliseconds()
            setCardNote(cardKey, practiceType, content, format.toLong(), now, now)
        }

    override suspend fun getNote(cardKey: String, practiceType: Long): String? =
        transactionScope.readTransaction {
            getCardNote(cardKey, practiceType).executeAsOneOrNull()?.content
        }

    override suspend fun deleteNote(cardKey: String, practiceType: Long) =
        transactionScope.writeTransaction {
            deleteCardNote(cardKey, practiceType)
        }

    override suspend fun addHistoryEntry(
        actionType: Int, cardKey: String?, practiceType: Long?, details: String
    ) = transactionScope.writeTransaction {
        val now = Clock.System.now().toEpochMilliseconds()
        insertStudyHistory(actionType.toLong(), cardKey, practiceType, details, now)
    }

    override suspend fun getRecentHistory(limit: Int): List<StudyHistoryRow> =
        transactionScope.readTransaction {
            getRecentStudyHistory(limit.toLong()).executeAsList().map { it.toHistoryRow() }
        }

    override suspend fun getHistoryForCard(
        cardKey: String, practiceType: Long, limit: Int
    ): List<StudyHistoryRow> = transactionScope.readTransaction {
        getStudyHistoryForCard(cardKey, practiceType, limit.toLong())
            .executeAsList().map { it.toHistoryRow() }
    }

    override suspend fun saveShortcut(
        actionId: String, primaryKey: String, modifierFlags: Int, profileName: String
    ) = transactionScope.writeTransaction {
        insertOrUpdateShortcut(actionId, primaryKey, modifierFlags.toLong(), 1L, profileName)
    }

    override suspend fun getShortcut(actionId: String, profileName: String): ShortcutRow? =
        transactionScope.readTransaction {
            getShortcut(actionId, profileName).executeAsOneOrNull()?.toShortcutRow()
        }

    override suspend fun getAllShortcuts(): List<ShortcutRow> =
        transactionScope.readTransaction {
            getAllShortcuts().executeAsList().map { it.toShortcutRow() }
        }

    override suspend fun deleteShortcut(actionId: String, profileName: String) =
        transactionScope.writeTransaction {
            deleteShortcut(actionId, profileName)
        }

    override suspend fun resetShortcuts(profileName: String) =
        transactionScope.writeTransaction {
            resetShortcutsToProfile(profileName)
        }

    override suspend fun recordBackup(
        filename: String, fileSize: Long, checksum: String,
        isAutomatic: Boolean, notes: String
    ) = transactionScope.writeTransaction {
        val now = Clock.System.now().toEpochMilliseconds()
        insertBackupMetadata(
            filename, fileSize, checksum,
            if (isAutomatic) 1L else 0L, now, notes
        )
    }

    override suspend fun getBackups(limit: Int, offset: Int): List<BackupRow> =
        transactionScope.readTransaction {
            getBackupList(limit.toLong(), offset.toLong()).executeAsList().map {
                BackupRow(
                    id = it.id,
                    filename = it.filename,
                    fileSize = it.file_size,
                    checksum = it.checksum,
                    isAutomatic = it.is_automatic != 0L,
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    notes = it.notes
                )
            }
        }

    override suspend fun deleteBackupMetadata(id: Long) =
        transactionScope.writeTransaction {
            deleteBackupMetadata(id)
        }

    override suspend fun getHistoryByAction(actionType: Int, limit: Int): List<StudyHistoryRow> =
        transactionScope.readTransaction {
            getStudyHistoryByAction(actionType.toLong(), limit.toLong()).executeAsList().map { it.toHistoryRow() }
        }

    override suspend fun clearHistory(beforeTimestamp: Long) =
        transactionScope.writeTransaction {
            clearOldStudyHistory(beforeTimestamp)
        }

    override suspend fun createFilteredDeck(
        name: String, searchQuery: String, maxCards: Int
    ): Long = transactionScope.writeTransaction {
        val now = Clock.System.now().toEpochMilliseconds()
        insertFilteredDeck(name, searchQuery, maxCards.toLong(), 1L, now)
        getLastInsertRowId().executeAsOne()
    }

    override suspend fun getFilteredDecks(): List<FilteredDeckRow> =
        transactionScope.readTransaction {
            getFilteredDecks().executeAsList().map {
                FilteredDeckRow(
                    id = it.id,
                    name = it.name,
                    searchQuery = it.search_query,
                    maxCards = it.max_cards.toInt(),
                    isRescheduled = it.is_rescheduled != 0L,
                    createdAt = Instant.fromEpochMilliseconds(it.created_at)
                )
            }
        }

    override suspend fun deleteFilteredDeck(id: Long) =
        transactionScope.writeTransaction {
            deleteFilteredDeck(id)
        }

    override suspend fun getTagUsageCount(tagId: Long): Long =
        transactionScope.readTransaction {
            getTagUsageCount(tagId).executeAsOne()
        }
}

// ============================================
// Extension mappers for SQLDelight types
// ============================================

private fun ua.syt0r.kanji.core.userdata.db.Tag.toTagData(): CardTagData = CardTagData(
    id = id,
    name = name,
    color = color,
    parentId = parent_id,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    modifiedAt = Instant.fromEpochMilliseconds(modified_at)
)

private fun ua.syt0r.kanji.core.userdata.db.Study_history.toHistoryRow(): StudyHistoryRow = StudyHistoryRow(
    id = id,
    actionType = action_type.toInt(),
    cardKey = card_key,
    practiceType = practice_type,
    details = details ?: "",
    timestamp = Instant.fromEpochMilliseconds(timestamp)
)

private fun ua.syt0r.kanji.core.userdata.db.Keyboard_shortcut.toShortcutRow(): ShortcutRow = ShortcutRow(
    id = id,
    actionId = action_id,
    primaryKey = primary_key,
    modifierFlags = modifier_flags.toInt(),
    isEnabled = is_enabled != 0L,
    profileName = profile_name
)
