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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlpro.banklistener.ui.theme.BankGreen

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text("Privacidade", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        Text("🔒", fontSize = 48.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Text(
            "Sua privacidade é nossa prioridade",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        val sections = listOf(
            "O que lemos" to "Lemos apenas notificações dos bancos e carteiras digitais que você autorizar explicitamente na lista de bancos monitorados.",
            "O que NÃO salvamos" to "Nunca salvamos:\n• Senhas ou PINs\n• Códigos de autenticação / OTP / 2FA\n• Número completo do cartão ou CVV\n• CPF ou CNPJ completo\n• Dados de agência ou conta corrente\n• Tokens de acesso bancário",
            "O que salvamos" to "Salvamos apenas:\n• Valor da transação\n• Tipo (entrada/saída)\n• Categoria (Pix, Compra, etc.)\n• Banco de origem\n• Data e hora\n• Descrição resumida gerada pelo app",
            "Hash de segurança" to "Geramos um hash SHA-256 do conteúdo normalizado da notificação para evitar duplicidade. Este hash não permite reconstruir o texto original.",
            "Armazenamento" to "Seus dados são armazenados no Supabase com Row Level Security (RLS) ativo. Somente você pode acessar seus próprios dados.",
            "Sem acesso a terceiros" to "Não compartilhamos seus dados com anunciantes, terceiros ou qualquer serviço além do Supabase para armazenamento seguro."
        )

        sections.forEach { (title, body) ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier.size(8.dp).padding(top = 6.dp),
                            shape = RoundedCornerShape(50),
                            color = BankGreen
                        ) {}
                        Text(title, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}
