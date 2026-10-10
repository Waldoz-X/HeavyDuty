package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.BackupImportResult
import com.example.heavyduty.core.domain.repository.BackupRepository
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(private val backupRepository: BackupRepository) {
    suspend operator fun invoke(json: String): BackupImportResult = backupRepository.importSnapshot(json)
}
