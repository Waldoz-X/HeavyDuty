package com.example.heavyduty.core.domain.repository

/**
 * Exports/imports the user's own training data (custom exercises, routines, workout history,
 * body weight log) as a portable JSON snapshot — no server, no account, just a file the user
 * controls. Import is purely additive: nothing existing is ever deleted or overwritten, so a
 * botched or repeated import can duplicate data but can never destroy it.
 *
 * Not covered: Programs/ProgramWeeks, Calendar entries, and user preferences — a session's
 * link back to the routine it came from is also dropped, since the routine is re-created with
 * a new id on import. The session's own data (name, sets, duration) is preserved regardless.
 */
interface BackupRepository {
    suspend fun exportSnapshot(): String
    suspend fun importSnapshot(json: String): BackupImportResult
}

data class BackupImportResult(
    val customExercisesImported: Int,
    val routinesImported: Int,
    val sessionsImported: Int,
    val bodyWeightEntriesImported: Int,
    val skippedReferences: Int,
)
