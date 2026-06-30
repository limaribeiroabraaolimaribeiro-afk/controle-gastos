package com.controlpro.banklistener.domain.usecase

import com.controlpro.banklistener.data.model.BankNotificationImport
import com.controlpro.banklistener.data.model.NotificationType
import com.controlpro.banklistener.data.model.ParsedNotification
import com.controlpro.banklistener.data.repository.AuthRepository
import com.controlpro.banklistener.data.repository.DuplicateImportException
import com.controlpro.banklistener.data.repository.NotificationRepository
import com.controlpro.banklistener.parser.NotificationParser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ProcessNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {

    sealed class ProcessResult {
        data class RequiresConfirmation(val parsed: ParsedNotification, val import: BankNotificationImport) : ProcessResult()
        data class AutoConfirmed(val import: BankNotificationImport) : ProcessResult()
        data class Ignored(val reason: String) : ProcessResult()
        data class Duplicate(val hash: String) : ProcessResult()
        data class Error(val exception: Exception) : ProcessResult()
    }

    suspend fun execute(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long,
        autoMode: Boolean,
        minConfidence: Int
    ): ProcessResult {
        val userId = authRepository.getUserId() ?: return ProcessResult.Error(Exception("Usuário não autenticado"))

        val parsed = NotificationParser.parse(packageName, title, text, timestamp)

        if (parsed.shouldIgnore) {
            return ProcessResult.Ignored(parsed.ignoreReason ?: "Notificação ignorada")
        }

        // Verificar duplicidade no banco
        val isDuplicate = notificationRepository.hashExists(userId, parsed.rawHash)
        if (isDuplicate) {
            return ProcessResult.Duplicate(parsed.rawHash)
        }

        val import = BankNotificationImport(
            userId = userId,
            type = parsed.type.name.lowercase(),
            amount = parsed.amount,
            category = parsed.category.name.lowercase(),
            bankName = parsed.bankName,
            appPackage = parsed.appPackage,
            notificationTime = formatTimestamp(parsed.notificationTime),
            description = parsed.description,
            rawHash = parsed.rawHash,
            status = if (autoMode && parsed.confidenceScore >= minConfidence) "auto_confirmed" else "pending",
            confidenceScore = parsed.confidenceScore
        )

        return if (autoMode && parsed.confidenceScore >= minConfidence) {
            val result = notificationRepository.insertImport(import)
            when {
                result.isSuccess -> ProcessResult.AutoConfirmed(result.getOrThrow())
                result.exceptionOrNull() is DuplicateImportException -> ProcessResult.Duplicate(parsed.rawHash)
                else -> ProcessResult.Error(result.exceptionOrNull() as Exception)
            }
        } else {
            val result = notificationRepository.insertImport(import)
            when {
                result.isSuccess -> ProcessResult.RequiresConfirmation(parsed, result.getOrThrow())
                result.exceptionOrNull() is DuplicateImportException -> ProcessResult.Duplicate(parsed.rawHash)
                else -> ProcessResult.Error(result.exceptionOrNull() as Exception)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val instant = Instant.ofEpochMilli(timestamp)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            Instant.now().toString()
        }
    }
}
