package com.controlpro.banklistener.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlpro.banklistener.ui.theme.BankBlue
import com.controlpro.banklistener.ui.theme.BankGreen

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Ícone / Logo
        Surface(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(28.dp),
            color = BankBlue.copy(alpha = 0.18f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🏦", fontSize = 48.sp)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Controle PRO",
                style = MaterialTheme.typography.headlineMedium,
                color = BankBlue,
                textAlign = TextAlign.Center
            )
            Text(
                "Leitor Bancário",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        Text(
            "Detecte automaticamente Pix, compras, boletos e transferências nas notificações dos seus bancos e registre no Controle de Gastos PRO.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Features
        val features = listOf(
            "💰" to "Detecta valores em tempo real",
            "🔒" to "Nunca salva senhas ou dados sensíveis",
            "⚡" to "Sincroniza com o Controle de Gastos PRO",
            "🏦" to "Suporta Nubank, Inter, Itaú, C6 e mais"
        )
        features.forEach { (icon, text) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(icon, fontSize = 20.sp)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BankBlue)
        ) {
            Text("Começar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
