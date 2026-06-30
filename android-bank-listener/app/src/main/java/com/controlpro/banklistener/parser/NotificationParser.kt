package com.controlpro.banklistener.parser

import com.controlpro.banklistener.data.model.NotificationCategory
import com.controlpro.banklistener.data.model.NotificationType
import com.controlpro.banklistener.data.model.ParsedNotification
import com.controlpro.banklistener.security.HashUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationParser {

    // Padrões de valor monetário em português do Brasil
    private val AMOUNT_PATTERNS = listOf(
        Regex("""R\$\s*(\d{1,3}(?:\.\d{3})*(?:,\d{2})?|\d+(?:,\d{2})?)"""),
        Regex("""(\d{1,3}(?:\.\d{3})*,\d{2})\s*reais"""),
        Regex("""BRL\s*(\d+(?:[.,]\d{2})?)"""),
        Regex("""de\s+R\$\s*(\d[\d.,]*)"""),
        Regex("""no\s+valor\s+de\s+R?\$?\s*(\d[\d.,]*)"""),
        Regex("""valor[:\s]+R?\$?\s*(\d[\d.,]*)""")
    )

    // Padrões de entrada (receita)
    private val ENTRY_PATTERNS = listOf(
        "pix recebido", "você recebeu", "voce recebeu", "recebeu pix",
        "transferência recebida", "transferencia recebida", "recebemos",
        "entrou na conta", "crédito recebido", "credito recebido",
        "venda recebida", "pagamento recebido", "recebimento", "you received",
        "pix creditado", "ted recebida", "doc recebido", "depósito recebido",
        "deposito recebido", "entrada realizada", "recebimento aprovado"
    )

    // Padrões de saída (despesa)
    private val EXIT_PATTERNS = listOf(
        "pix enviado", "você enviou", "voce enviou", "enviou pix",
        "pagamento realizado", "pagamento efetuado", "compra aprovada",
        "compra realizada", "débito realizado", "debito realizado",
        "boleto pago", "transferência enviada", "transferencia enviada",
        "transferência realizada", "transferencia realizada",
        "você pagou", "voce pagou", "valor debitado", "debitado",
        "ted enviada", "ted realizada", "doc enviado", "doc realizado",
        "cobrança paga", "cobranca paga", "fatura paga", "pix debitado",
        "saque realizado", "pagamento aprovado"
    )

    // Categoria por palavras-chave
    private val CATEGORY_MAP = mapOf(
        NotificationCategory.PIX to listOf("pix"),
        NotificationCategory.TRANSFERENCIA to listOf("transferência", "transferencia", "ted", "doc"),
        NotificationCategory.BOLETO to listOf("boleto", "fatura", "conta de luz", "conta de água", "conta de agua"),
        NotificationCategory.COMPRA to listOf("compra", "cartão", "cartao", "crédito", "credito", "débito", "debito", "tap"),
        NotificationCategory.PAGAMENTO to listOf("pagamento", "pago", "cobrança", "cobranca"),
        NotificationCategory.VENDA to listOf("venda", "maquininha", "stone", "infinitepay", "pagseguro")
    )

    fun parse(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long
    ): ParsedNotification {
        val bankName = BankAppRegistry.getBankName(packageName)
        val combined = "$title $text"
        val lower = combined.lowercase()

        // Verificar conteúdo sensível primeiro
        if (SensitiveFilter.containsSensitiveContent(combined)) {
            return ignoredResult(
                packageName, bankName, timestamp,
                SensitiveFilter.getSensitiveReason(combined) ?: "Conteúdo sensível detectado",
                combined
            )
        }

        val amount = extractAmount(combined)

        // Sem valor = sem lançamento
        if (amount <= 0.0) {
            return ignoredResult(packageName, bankName, timestamp, "Nenhum valor monetário encontrado", combined)
        }

        val type = detectType(lower)
        if (type == NotificationType.UNKNOWN) {
            return ignoredResult(packageName, bankName, timestamp, "Tipo de transação não reconhecido", combined)
        }

        val category = detectCategory(lower)
        val description = buildDescription(type, category, amount, bankName)
        val confidence = calculateConfidence(lower, amount, type, category)
        val hash = HashUtils.buildNotificationHash(packageName, title, text, timestamp)
        val timeStr = formatTimestamp(timestamp)

        return ParsedNotification(
            type = type,
            category = category,
            amount = amount,
            bankName = bankName,
            appPackage = packageName,
            notificationTime = timestamp,
            description = description,
            rawHash = hash,
            confidenceScore = confidence,
            shouldIgnore = false
        )
    }

    private fun extractAmount(text: String): Double {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues[1]
                .replace(".", "")
                .replace(",", ".")
            return raw.toDoubleOrNull() ?: continue
        }
        return 0.0
    }

    private fun detectType(lower: String): NotificationType {
        if (ENTRY_PATTERNS.any { lower.contains(it) }) return NotificationType.ENTRADA
        if (EXIT_PATTERNS.any { lower.contains(it) }) return NotificationType.SAIDA
        return NotificationType.UNKNOWN
    }

    private fun detectCategory(lower: String): NotificationCategory {
        for ((cat, keywords) in CATEGORY_MAP) {
            if (keywords.any { lower.contains(it) }) return cat
        }
        return NotificationCategory.OUTRO
    }

    private fun buildDescription(
        type: NotificationType,
        category: NotificationCategory,
        amount: Double,
        bankName: String
    ): String {
        val typeStr = when (type) {
            NotificationType.ENTRADA -> "recebido"
            NotificationType.SAIDA -> "enviado"
            else -> ""
        }
        val catStr = when (category) {
            NotificationCategory.PIX -> "Pix"
            NotificationCategory.TRANSFERENCIA -> "Transferência"
            NotificationCategory.BOLETO -> "Boleto"
            NotificationCategory.COMPRA -> "Compra"
            NotificationCategory.PAGAMENTO -> "Pagamento"
            NotificationCategory.VENDA -> "Venda"
            NotificationCategory.OUTRO -> "Transação"
        }
        val amountStr = "R$ %.2f".format(amount).replace(".", ",")
        return "$catStr $typeStr $amountStr — $bankName"
    }

    private fun calculateConfidence(
        lower: String,
        amount: Double,
        type: NotificationType,
        category: NotificationCategory
    ): Int {
        var score = 0

        if (amount > 0) score += 30
        if (type != NotificationType.UNKNOWN) score += 30
        if (category != NotificationCategory.OUTRO) score += 20

        // Bônus por padrões específicos de alto nível
        if (lower.contains("pix")) score += 10
        if (lower.contains("r$") || lower.contains("brl")) score += 5
        if (lower.contains("aprovad")) score += 5

        return score.coerceAtMost(100)
    }

    private fun ignoredResult(
        packageName: String,
        bankName: String,
        timestamp: Long,
        reason: String,
        rawText: String
    ): ParsedNotification {
        val hash = HashUtils.sha256("$packageName|${rawText.take(80)}|$timestamp")
        return ParsedNotification(
            type = NotificationType.UNKNOWN,
            category = NotificationCategory.OUTRO,
            amount = 0.0,
            bankName = bankName,
            appPackage = packageName,
            notificationTime = timestamp,
            description = "",
            rawHash = hash,
            confidenceScore = 0,
            shouldIgnore = true,
            ignoreReason = reason
        )
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
