package com.controlpro.banklistener.parser

object SensitiveFilter {

    private val SENSITIVE_KEYWORDS = listOf(
        "código", "codigo", "senha", "token", "autenticação", "autenticacao",
        "verificação", "verificacao", "otp", "login", "acesso",
        "cartão virtual", "cartao virtual", "cvv", "agência", "agencia",
        "conta corrente", "conta poupança", "cpf", "cnpj", "protocolo",
        "recuperação", "recuperacao", "redefinir", "alterar senha",
        "dois fatores", "2fa", "pin", "autorizacao", "autorização",
        "código de acesso", "codigo de acesso", "chave pix cadastrada",
        "limite alterado", "limite de crédito", "limite de credito"
    )

    fun containsSensitiveContent(text: String): Boolean {
        val lower = text.lowercase()
        return SENSITIVE_KEYWORDS.any { lower.contains(it) }
    }

    fun getSensitiveReason(text: String): String? {
        val lower = text.lowercase()
        return SENSITIVE_KEYWORDS.firstOrNull { lower.contains(it) }?.let {
            "Contém dado sensível: \"$it\""
        }
    }
}
