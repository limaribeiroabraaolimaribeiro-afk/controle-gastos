package com.controlpro.banklistener.data.repository

import com.controlpro.banklistener.data.model.BankNotificationImport
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NotificationRepository {

    private val supabase = SupabaseProvider.client
    private val table = "bank_notification_imports"

    suspend fun insertImport(import: BankNotificationImport): Result<BankNotificationImport> {
        return try {
            val result = supabase.postgrest[table].insert(import) {
                select()
            }.decodeSingle<BankNotificationImport>()
            Result.success(result)
        } catch (e: Exception) {
            // Verificar se é erro de duplicidade (unique constraint)
            if (e.message?.contains("unique") == true || e.message?.contains("23505") == true) {
                Result.failure(DuplicateImportException("Notificação já registrada anteriormente"))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getPendingImports(userId: String): Result<List<BankNotificationImport>> {
        return try {
            val result = supabase.postgrest[table]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "pending")
                    }
                    order("notification_time", Order.DESCENDING)
                    limit(50)
                }
                .decodeList<BankNotificationImport>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentImports(userId: String, limit: Int = 30): Result<List<BankNotificationImport>> {
        return try {
            val result = supabase.postgrest[table]
                .select {
                    filter { eq("user_id", userId) }
                    order("notification_time", Order.DESCENDING)
                    this.limit(limit.toLong())
                }
                .decodeList<BankNotificationImport>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatus(id: String, status: String): Result<Unit> {
        return try {
            supabase.postgrest[table].update(
                buildJsonObject { put("status", status) }
            ) {
                filter { eq("id", id) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hashExists(userId: String, rawHash: String): Boolean {
        return try {
            val result = supabase.postgrest[table]
                .select(Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("raw_hash", rawHash)
                    }
                    limit(1)
                }
                .decodeList<BankNotificationImport>()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createExpense(
        userId: String,
        mes: String,
        descricao: String,
        categoria: String,
        valor: Double,
        data: String,
        bankImportId: String?
    ): Result<Unit> {
        return try {
            supabase.postgrest["expenses"].insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("mes", mes)
                    put("descricao", descricao)
                    put("categoria", categoria)
                    put("valor", valor)
                    put("data", data)
                    put("origem", "notificacao_banco")
                    bankImportId?.let { put("bank_import_id", it) }
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createIncome(
        userId: String,
        mes: String,
        descricao: String,
        categoria: String,
        valor: Double,
        data: String,
        bankImportId: String?
    ): Result<Unit> {
        return try {
            supabase.postgrest["income_entries"].insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("mes", mes)
                    put("descricao", descricao)
                    put("categoria", categoria)
                    put("valor", valor)
                    put("data", data)
                    put("origem", "notificacao_banco")
                    bankImportId?.let { put("bank_import_id", it) }
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DuplicateImportException(message: String) : Exception(message)
