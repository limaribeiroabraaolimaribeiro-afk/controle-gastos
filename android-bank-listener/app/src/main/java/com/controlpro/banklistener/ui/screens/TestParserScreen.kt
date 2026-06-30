package com.controlpro.banklistener.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.controlpro.banklistener.ui.theme.BankBlue
import com.controlpro.banklistener.ui.theme.BankGreen

@Composable
fun TestParserScreen(
    onTest: (String) -> String,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    val examples = listOf(
        "Pix recebido de R$ 50,00 no Nubank",
        "Pix enviado R$ 25,90 para João",
        "Compra aprovada de R$ 39,99 em Supermercado Extra",
        "Boleto pago R$ 120,00 - Conta de Luz",
        "Transferência recebida R$ 300,00 - Pedro",
        "Código de acesso: 123456 para login",
        "Notificação sem valor monetário relevante",
        "Você pagou R$ 1.250,90 - Cartão de crédito"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text("Testar Parser", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        Text(
            "Cole ou digite um texto de notificação bancária para ver como o parser vai interpretá-la:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it; result = null },
            label = { Text("Texto da notificação") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(14.dp),
            maxLines = 5
        )

        Button(
            onClick = { if (inputText.isNotBlank()) result = onTest(inputText) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BankBlue)
        ) {
            Text("Analisar notificação", fontWeight = FontWeight.Bold)
        }

        result?.let { res ->
            Surface(
                color = if (res.startsWith("IGNORADO")) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        else BankGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Resultado:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Text(res, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Exemplos prontos
        HorizontalDivider()
        Text("Exemplos para testar:", fontWeight = FontWeight.SemiBold)
        examples.forEach { example ->
            OutlinedButton(
                onClick = { inputText = example; result = null },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(example, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
        }

        Spacer(Modifier.height(60.dp))
    }
}
