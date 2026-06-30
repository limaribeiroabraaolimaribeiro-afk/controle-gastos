package com.controlpro.banklistener.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.controlpro.banklistener.data.model.AllowedApp
import com.controlpro.banklistener.ui.theme.BankBlue
import com.controlpro.banklistener.ui.theme.BankGreen

@Composable
fun BanksScreen(
    allowedApps: List<AllowedApp>,
    enabledPackages: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // TopBar
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text("Bancos monitorados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        Text(
            "Ative ou desative os bancos que deseja monitorar. Apenas apps habilitados terão notificações lidas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allowedApps) { app ->
                val isEnabled = app.packageName in enabledPackages
                BankAppItem(app = app, isEnabled = isEnabled, onToggle = { onToggle(app.packageName) })
            }
        }
    }
}

@Composable
private fun BankAppItem(app: AllowedApp, isEnabled: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🏦")
            Column(modifier = Modifier.weight(1f)) {
                Text(app.displayName, fontWeight = FontWeight.SemiBold)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isEnabled) "Monitorando" else "Pausado",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEnabled) BankGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = BankBlue, checkedTrackColor = BankBlue.copy(alpha = 0.35f))
            )
        }
    }
}
