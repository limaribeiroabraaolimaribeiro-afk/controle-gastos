package com.controlpro.banklistener.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.controlpro.banklistener.data.model.BankNotificationImport

@Composable
fun NotificationItem(
    import: BankNotificationImport,
    showActions: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    onIgnore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEntrada = import.type == "entrada"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        import.bankName ?: "Banco",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        import.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    import.notificationTime?.let { time ->
                        Text(
                            time.take(16).replace("T", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                MoneyBadge(import.amount, isEntrada)
            }

            if (showActions && (onConfirm != null || onIgnore != null)) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onConfirm?.let {
                        Button(
                            onClick = it,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lançar", color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                    onIgnore?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ignorar")
                        }
                    }
                }
            }

            // Badge de status
            val statusLabel = when (import.status) {
                "confirmed" -> "✅ Confirmado"
                "auto_confirmed" -> "⚡ Auto"
                "ignored" -> "🚫 Ignorado"
                else -> "⏳ Pendente"
            }
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
