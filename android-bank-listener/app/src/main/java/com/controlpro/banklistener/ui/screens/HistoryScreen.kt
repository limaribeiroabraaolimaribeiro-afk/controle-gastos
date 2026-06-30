package com.controlpro.banklistener.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.controlpro.banklistener.data.model.BankNotificationImport
import com.controlpro.banklistener.ui.components.NotificationItem
import com.controlpro.banklistener.ui.theme.BankBlue

@Composable
fun HistoryScreen(
    recentImports: List<BankNotificationImport>,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf("todos") }

    val filtered = when (filter) {
        "entrada" -> recentImports.filter { it.type == "entrada" }
        "saida" -> recentImports.filter { it.type == "saida" }
        "pendente" -> recentImports.filter { it.status == "pending" }
        "ignorado" -> recentImports.filter { it.status == "ignored" }
        else -> recentImports
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text("Histórico", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        // Filtros
        val filters = listOf("todos" to "Todos", "entrada" to "Entradas", "saida" to "Saídas", "pendente" to "Pendentes", "ignorado" to "Ignorados")
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BankBlue.copy(alpha = 0.25f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum lançamento neste filtro", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "${filtered.size} lançamento(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(filtered, key = { it.id ?: it.rawHash }) { import ->
                    NotificationItem(import = import)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
