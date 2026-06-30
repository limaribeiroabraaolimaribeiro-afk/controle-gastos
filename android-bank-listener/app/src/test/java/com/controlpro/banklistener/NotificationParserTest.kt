package com.controlpro.banklistener

import com.controlpro.banklistener.data.model.NotificationCategory
import com.controlpro.banklistener.data.model.NotificationType
import com.controlpro.banklistener.parser.NotificationParser
import com.controlpro.banklistener.parser.SensitiveFilter
import com.controlpro.banklistener.security.HashUtils
import org.junit.Assert.*
import org.junit.Test

class NotificationParserTest {

    private val nubank = "com.nu.production"
    private val inter = "br.com.intermedium"
    private val timestamp = System.currentTimeMillis()

    @Test
    fun `pix recebido nubank R$ 50,00 deve ser detectado como ENTRADA`() {
        val result = NotificationParser.parse(
            nubank, "Pix recebido", "Você recebeu R$ 50,00 de João", timestamp
        )
        assertFalse(result.shouldIgnore)
        assertEquals(NotificationType.ENTRADA, result.type)
        assertEquals(50.0, result.amount, 0.01)
        assertEquals(NotificationCategory.PIX, result.category)
        assertTrue(result.confidenceScore >= 80)
    }

    @Test
    fun `pix enviado R$ 25,90 deve ser detectado como SAIDA`() {
        val result = NotificationParser.parse(
            nubank, "Pix enviado", "Você enviou R$ 25,90 para Maria", timestamp
        )
        assertFalse(result.shouldIgnore)
        assertEquals(NotificationType.SAIDA, result.type)
        assertEquals(25.9, result.amount, 0.01)
        assertEquals(NotificationCategory.PIX, result.category)
    }

    @Test
    fun `compra aprovada R$ 39,99 deve ser detectada como SAIDA compra`() {
        val result = NotificationParser.parse(
            nubank, "Compra aprovada", "Compra aprovada de R$ 39,99 em Supermercado Extra", timestamp
        )
        assertFalse(result.shouldIgnore)
        assertEquals(NotificationType.SAIDA, result.type)
        assertEquals(39.99, result.amount, 0.01)
        assertEquals(NotificationCategory.COMPRA, result.category)
    }

    @Test
    fun `boleto pago R$ 120,00 deve ser detectado como SAIDA boleto`() {
        val result = NotificationParser.parse(
            inter, "Boleto pago", "Boleto pago R$ 120,00 - Conta de Luz CEMIG", timestamp
        )
        assertFalse(result.shouldIgnore)
        assertEquals(NotificationType.SAIDA, result.type)
        assertEquals(120.0, result.amount, 0.01)
        assertEquals(NotificationCategory.BOLETO, result.category)
    }

    @Test
    fun `transferencia recebida R$ 300,00 deve ser ENTRADA`() {
        val result = NotificationParser.parse(
            inter, "Transferência recebida", "Transferência recebida de R$ 300,00 - Pedro S.", timestamp
        )
        assertFalse(result.shouldIgnore)
        assertEquals(NotificationType.ENTRADA, result.type)
        assertEquals(300.0, result.amount, 0.01)
        assertEquals(NotificationCategory.TRANSFERENCIA, result.category)
    }

    @Test
    fun `notificacao com codigo token deve ser ignorada`() {
        val result = NotificationParser.parse(
            nubank, "Código de verificação", "Seu código de acesso é 123456. Não compartilhe.", timestamp
        )
        assertTrue(result.shouldIgnore)
        assertNotNull(result.ignoreReason)
    }

    @Test
    fun `notificacao sem valor deve ser ignorada`() {
        val result = NotificationParser.parse(
            nubank, "Nubank", "Nova funcionalidade disponível no app!", timestamp
        )
        assertTrue(result.shouldIgnore)
    }

    @Test
    fun `notificacao duplicada gera mesmo hash`() {
        val title = "Pix recebido"
        val text = "Você recebeu R$ 50,00 de João"
        val hash1 = HashUtils.buildNotificationHash(nubank, title, text, timestamp)
        val hash2 = HashUtils.buildNotificationHash(nubank, title, text, timestamp)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `notificacoes diferentes geram hashes diferentes`() {
        val hash1 = HashUtils.buildNotificationHash(nubank, "Pix recebido", "R$ 50,00 de João", timestamp)
        val hash2 = HashUtils.buildNotificationHash(nubank, "Pix recebido", "R$ 100,00 de Maria", timestamp)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `valor com milhar deve ser parseado corretamente`() {
        val result = NotificationParser.parse(
            nubank, "Compra aprovada", "Você pagou R$ 1.250,90 - Cartão de crédito", timestamp
        )
        assertFalse(result.shouldIgnore)
        assertEquals(1250.9, result.amount, 0.01)
        assertEquals(NotificationType.SAIDA, result.type)
    }

    @Test
    fun `sensitive filter detecta otp`() {
        assertTrue(SensitiveFilter.containsSensitiveContent("Seu OTP é 123456"))
    }

    @Test
    fun `sensitive filter detecta senha`() {
        assertTrue(SensitiveFilter.containsSensitiveContent("Digite sua senha para confirmar"))
    }

    @Test
    fun `sensitive filter nao bloqueia transacao normal`() {
        assertFalse(SensitiveFilter.containsSensitiveContent("Pix recebido de R$ 50,00 de João"))
    }

    @Test
    fun `hash utils mascara cpf`() {
        val masked = HashUtils.maskSensitiveNumbers("CPF: 123.456.789-00 aprovado")
        assertFalse(masked.contains("123.456.789-00"))
    }

    @Test
    fun `venda recebida deve ser ENTRADA`() {
        val result = NotificationParser.parse(
            "br.com.infinitepay.mobile", "Venda aprovada", "Venda recebida de R$ 85,00 via InfinitePay", timestamp
        )
        assertFalse(result.shouldIgnore)
        assertEquals(NotificationType.ENTRADA, result.type)
        assertEquals(85.0, result.amount, 0.01)
    }
}
