package com.controlpro.banklistener.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlpro.banklistener.ui.components.NotificationItem
import com.controlpro.banklistener.ui.components.StatusCard
import com.controlpro.banklistener.ui.viewmodel.UiState
import com.controlpro.banklistener.ui.theme.BankBlue
import com.controlpro.banklistener.ui.theme.BankGreen

@Composable
fun HomeScreen(
    uiState: UiState,
    onRefresh: () -> Unit,
    onCheckPermission: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onNavigateToPending: () -> Unit,
    onNavigateToBanks: () -> Unit,
    onNavigateToTest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Olá 👋", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        uiState.userEmail?.substringBefore("@") ?: "Usuário",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = BankBlue)
                }
            }
        }

        // Cards de status
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusCard(
                    title = "Permissão",
                    value = if (uiState.notificationPermission) "Ativa" else "Pendente",
                    isActive = uiState.notificationPermission,
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "Serviço",
                    value = if (uiState.serviceConnected) "Conectado" else "Inativo",
                    isActive = uiState.serviceConnected,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Aviso de permissão
        if (!uiState.notificationPermission) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⚠️ Acesso às notificações necessário", fontWeight = FontWeight.Bold)
                        Text(
                            "Para detectar transações bancárias, conceda acesso às notificações nas configurações do Android.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenPermissionSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = BankBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ativar acesso às notificações", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Estatísticas rápidas
        item {
            val pending = uiState.pendingImports.size
            val total = uiState.recentImports.size
            val entradas = uiState.recentImports.count { it.type == "entrada" }
            val saidas = uiState.recentImports.count { it.type == "saida" }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("⏳ Pendentes", "$pending", null),
                    Triple("📥 Entradas", "$entradas", true),
                    Triple("📤 Saídas", "$saidas", false)
                ).forEach { (label, value, isEntrada) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                                color = when(isEntrada) { true -> BankGreen; false -> MaterialTheme.colorScheme.error; null -> MaterialTheme.colorScheme.onSurface })
                        }
                    }
                }
            }
        }

        // Ações rápidas
        item {
            Text("Ações rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("⏳ Ver pendentes de confirmação (${uiState.pendingImports.size})", onClick = onNavigateToPending)
                ActionButton("🏦 Bancos monitorados", onClick = onNavigateToBanks)
                ActionButton("🧪 Testar parser de notificação", onClick = onNavigateToTest)
            }
        }

        // Últimas detecções
        if (uiState.recentImports.isNotEmpty()) {
            item {
                Text("Últimas detecções", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(uiState.recentImports.take(5)) { import ->
                NotificationItem(import = import)
            }
        } else {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhuma transação detectada ainda", fontWeight = FontWeight.SemiBold)
                        Text(
                            "As transações aparecerão aqui quando você receber notificações dos seus bancos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}
