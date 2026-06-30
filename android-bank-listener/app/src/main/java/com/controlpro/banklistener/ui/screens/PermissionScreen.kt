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
import com.controlpro.banklistener.ui.theme.BankRed

@Composable
fun PermissionScreen(
    isPermissionGranted: Boolean,
    onOpenSettings: () -> Unit,
    onCheckPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(if (isPermissionGranted) "🔔" else "🔕", fontSize = 56.sp, textAlign = TextAlign.Center)

        Text(
            "Acesso às Notificações",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Surface(
            color = if (isPermissionGranted) BankGreen.copy(alpha = 0.15f) else BankRed.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isPermissionGranted) "✅" else "❌")
                Text(
                    if (isPermissionGranted) "Permissão concedida" else "Permissão não concedida",
                    fontWeight = FontWeight.Bold,
                    color = if (isPermissionGranted) BankGreen else BankRed
                )
            }
        }

        Text(
            "Para detectar transações bancárias, o Android exige que você conceda manualmente o acesso às notificações nas configurações do sistema.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        // Passo a passo
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Como ativar:", fontWeight = FontWeight.Bold)
                listOf(
                    "1. Toque em \"Abrir configurações\" abaixo",
                    "2. Encontre \"Controle PRO Leitor Bancário\"",
                    "3. Ative o acesso às notificações",
                    "4. Volte ao app e toque em \"Já ativei\""
                ).forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!isPermissionGranted) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BankBlue)
            ) {
                Text("Abrir configurações", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        OutlinedButton(
            onClick = onCheckPermission,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isPermissionGranted) "Permissão verificada ✅" else "Já ativei — Verificar")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
