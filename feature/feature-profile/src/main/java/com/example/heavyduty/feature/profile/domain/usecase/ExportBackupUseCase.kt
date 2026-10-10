package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.BackupRepository
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(private val backupRepository: BackupRepository) {
    suspend operator fun invoke(): String = backupRepository.exportSnapshot()
}
