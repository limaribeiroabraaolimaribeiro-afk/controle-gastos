package com.controlpro.banklistener.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlpro.banklistener.ui.theme.BankGreen
import com.controlpro.banklistener.ui.theme.BankRed
import com.controlpro.banklistener.ui.theme.BankYellow

@Composable
fun StatusCard(
    title: String,
    value: String,
    isActive: Boolean?,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val statusColor = when (isActive) {
        true -> BankGreen
        false -> BankRed
        null -> BankYellow
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = RoundedCornerShape(50),
                color = statusColor
            ) {}
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MoneyBadge(amount: Double, isEntrada: Boolean, modifier: Modifier = Modifier) {
    val color = if (isEntrada) BankGreen else BankRed
    val prefix = if (isEntrada) "+" else "-"
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$prefix R$ ${"%.2f".format(amount).replace(".", ",")}",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
