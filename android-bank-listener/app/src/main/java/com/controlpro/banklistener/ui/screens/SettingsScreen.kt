package com.controlpro.banklistener.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.controlpro.banklistener.ui.theme.BankBlue
import com.controlpro.banklistener.ui.theme.BankRed

@Composable
fun SettingsScreen(
    userEmail: String?,
    autoMode: Boolean,
    minConfidence: Int,
    onAutoModeToggle: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text("Configurações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        // Conta
        SettingSection(title = "Conta") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Usuário logado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(userEmail ?: "—", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = BankRed.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sair da conta", color = BankRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Modo automático
        SettingSection(title = "Lançamento automático") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lançar automaticamente", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Registra notificações confiáveis sem pedir confirmação (confiança ≥ $minConfidence%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (autoMode) {
                            Text(
                                "⚠️ Ativo: lançamentos automáticos habilitados",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Switch(
                        checked = autoMode,
                        onCheckedChange = { onAutoModeToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BankBlue,
                            checkedTrackColor = BankBlue.copy(alpha = 0.35f)
                        )
                    )
                }
            }
        }

        // Privacidade
        SettingSection(title = "Privacidade e Segurança") {
            Button(
                onClick = onNavigateToPrivacy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("🔒 Ver política de privacidade", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Sobre
        SettingSection(title = "Sobre") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Controle PRO Leitor Bancário", fontWeight = FontWeight.Bold)
                    Text("Versão 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Integrado com Supabase Auth + Database", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Android 8.0+ (API 26+)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        content()
    }
}
