package com.controlpro.banklistener.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlpro.banklistener.data.model.BankNotificationImport
import com.controlpro.banklistener.ui.components.NotificationItem

@Composable
fun PendingScreen(
    pendingImports: List<BankNotificationImport>,
    onConfirm: (BankNotificationImport) -> Unit,
    onIgnore: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                }
                Text("Pendentes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Atualizar")
            }
        }

        if (pendingImports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhum lançamento pendente", fontWeight = FontWeight.Bold)
                    Text(
                        "Todos os lançamentos foram confirmados ou ignorados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "${pendingImports.size} lançamento(s) aguardando confirmação",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(pendingImports, key = { it.id ?: it.rawHash }) { import ->
                    NotificationItem(
                        import = import,
                        showActions = true,
                        onConfirm = { onConfirm(import) },
                        onIgnore = { import.id?.let { onIgnore(it) } }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
