package com.controlpro.banklistener.domain.usecase

import com.controlpro.banklistener.data.model.BankNotificationImport
import com.controlpro.banklistener.data.repository.AuthRepository
import com.controlpro.banklistener.data.repository.NotificationRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConfirmImportUseCase(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {

    suspend fun confirm(import: BankNotificationImport): Result<Unit> {
        val userId = authRepository.getUserId()
            ?: return Result.failure(Exception("Usuário não autenticado"))
        val importId = import.id ?: return Result.failure(Exception("ID de importação inválido"))

        // Extrair mês da notificação
        val mes = import.notificationTime?.let { extractMes(it) } ?: getCurrentMes()
        val data = import.notificationTime?.take(10) ?: LocalDate.now().toString()

        val result = if (import.type == "entrada") {
            notificationRepository.createIncome(
                userId = userId,
                mes = mes,
                descricao = import.description ?: "Receita bancária",
                categoria = mapCategory(import.category, isEntrada = true),
                valor = import.amount,
                data = data,
                bankImportId = importId
            )
        } else {
            notificationRepository.createExpense(
                userId = userId,
                mes = mes,
                descricao = import.description ?: "Gasto bancário",
                categoria = mapCategory(import.category, isEntrada = false),
                valor = import.amount,
                data = data,
                bankImportId = importId
            )
        }

        if (result.isSuccess) {
            notificationRepository.updateStatus(importId, "confirmed")
        }

        return result
    }

    suspend fun ignore(importId: String): Result<Unit> {
        return notificationRepository.updateStatus(importId, "ignored")
    }

    private fun extractMes(notificationTime: String): String {
        return try {
            notificationTime.substring(0, 7) // "YYYY-MM"
        } catch (e: Exception) {
            getCurrentMes()
        }
    }

    private fun getCurrentMes(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    private fun mapCategory(category: String?, isEntrada: Boolean): String {
        return when (category?.lowercase()) {
            "pix" -> if (isEntrada) "Pix Recebido" else "Pix Enviado"
            "transferencia" -> if (isEntrada) "Transferência Recebida" else "Transferência"
            "boleto" -> "Boleto"
            "compra" -> "Compra"
            "pagamento" -> "Pagamento"
            "venda" -> "Venda"
            else -> if (isEntrada) "Entrada" else "Outros"
        }
    }
}
