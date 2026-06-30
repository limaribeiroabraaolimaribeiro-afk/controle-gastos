package com.controlpro.banklistener.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class NotificationType { ENTRADA, SAIDA, UNKNOWN }

enum class NotificationCategory {
    PIX, PAGAMENTO, COMPRA, TRANSFERENCIA, BOLETO, VENDA, OUTRO
}

enum class ImportStatus { PENDING, CONFIRMED, IGNORED, AUTO_CONFIRMED }

@Serializable
data class BankNotificationImport(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val type: String,
    val amount: Double,
    val category: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("app_package") val appPackage: String? = null,
    @SerialName("notification_time") val notificationTime: String? = null,
    val description: String? = null,
    @SerialName("raw_hash") val rawHash: String,
    val status: String = "pending",
    @SerialName("confidence_score") val confidenceScore: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

data class ParsedNotification(
    val type: NotificationType,
    val category: NotificationCategory,
    val amount: Double,
    val bankName: String,
    val appPackage: String,
    val notificationTime: Long,
    val description: String,
    val rawHash: String,
    val confidenceScore: Int,
    val shouldIgnore: Boolean = false,
    val ignoreReason: String? = null
)
