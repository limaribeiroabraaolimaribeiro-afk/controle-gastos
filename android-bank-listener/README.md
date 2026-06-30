# Controle PRO — Leitor Bancário Android

App Android nativo que lê notificações de bancos e carteiras digitais, detecta transações financeiras e registra automaticamente no **Controle de Gastos PRO** via Supabase.

---

## Stack

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **NotificationListenerService** — leitura de notificações
- **Supabase Auth** — login com e-mail/senha
- **Supabase Database** + **RLS** — dados isolados por usuário
- **Coroutines** — operações assíncronas
- **EncryptedSharedPreferences** — armazenamento seguro do token
- **DataStore** — preferências do app
- **Arquitetura**: data / domain / parser / service / security / ui

---

## Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 11+
- Android 8.0+ (API 26)
- Conta no [Supabase](https://supabase.com)

---

## Configuração

### 1. Supabase

Execute o SQL em `../sql/supabase_schema.sql` no SQL Editor do seu projeto Supabase para criar as tabelas:
- `bank_notification_imports`
- `income_entries`
- `expenses`

### 2. Credenciais

Edite `app/build.gradle.kts` e substitua:

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://seu-projeto.supabase.co\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"sua-anon-key-aqui\"")
```

> **Nunca use a `service_role` key no app Android.**  
> Use apenas `SUPABASE_ANON_KEY` (publishable/anon key).

### 3. Build

```bash
cd android-bank-listener
./gradlew assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

---

## Como ativar o Acesso às Notificações

O Android exige que o usuário conceda manualmente o acesso às notificações:

1. Abra o app **Controle PRO Leitor Bancário**
2. Faça login com sua conta do Controle de Gastos PRO
3. Na tela **Acesso às Notificações**, toque em **"Abrir configurações"**
4. Na lista do sistema, encontre **"Controle PRO Leitor Bancário"**
5. Ative o acesso
6. Volte ao app e toque em **"Já ativei — Verificar"**

> Esta permissão é obrigatória e não pode ser concedida programaticamente por restrições do Android.

---

## Bancos suportados

| Banco / Carteira | Package Name |
|---|---|
| Nubank | com.nu.production |
| PicPay | com.picpay |
| Mercado Pago | com.mercadopago.wallet |
| Caixa Econômica | br.com.gabba.Caixa |
| Banco Inter | br.com.intermedium |
| Itaú | com.itau |
| Bradesco | com.bradesco |
| Santander | br.com.santander.benio |
| Banco do Brasil | br.com.bb.android |
| C6 Bank | com.c6bank.app |
| InfinitePay | br.com.infinitepay.mobile |
| Stone | br.com.stone.bank |
| PagBank | br.com.uol.ps.myaccount |
| Neon | br.com.neonapp |
| Will Bank | com.will.bank |

Adicione mais bancos editando `DefaultBankApps.kt` e habilitando-os na tela **Bancos monitorados**.

---

## Segurança e Privacidade

- **Não** salva texto bruto das notificações
- **Não** salva senhas, PINs, tokens, CVV, CPF completo ou dados bancários sensíveis
- Filtra automaticamente notificações com palavras sensíveis (OTP, senha, token, código...)
- Usa **SHA-256** como hash anti-duplicidade (não reversível)
- **RLS ativo** no Supabase — cada usuário só acessa seus próprios dados
- Token de sessão armazenado em **EncryptedSharedPreferences** (AES-256-GCM)

---

## Arquitetura

```
app/src/main/java/com/controlpro/banklistener/
├── data/
│   ├── model/          # BankNotification.kt, AllowedApp.kt
│   ├── repository/     # NotificationRepository.kt, AuthRepository.kt, SupabaseClient.kt
│   └── local/          # SecurePreferences.kt, AppPreferences.kt
├── domain/
│   └── usecase/        # ProcessNotificationUseCase.kt, ConfirmImportUseCase.kt
├── parser/
│   ├── NotificationParser.kt   # Parser com regex e regras em pt-BR
│   ├── BankAppRegistry.kt      # Registro de apps bancários
│   └── SensitiveFilter.kt      # Filtro de conteúdo sensível
├── security/
│   └── HashUtils.kt            # SHA-256 + máscara de dados sensíveis
├── service/
│   ├── BankNotificationListenerService.kt  # NotificationListenerService
│   └── BootReceiver.kt
└── ui/
    ├── theme/           # Color.kt, Theme.kt, Type.kt
    ├── screens/         # Todas as telas Compose
    ├── components/      # StatusCard.kt, NotificationItem.kt
    ├── navigation/      # AppNavigation.kt
    └── viewmodel/       # MainViewModel.kt
```

---

## Fluxo de funcionamento

```
Notificação chega
       ↓
BankNotificationListenerService.onNotificationPosted()
       ↓
Verifica se package está na lista de apps permitidos
       ↓
SensitiveFilter → contém dados sensíveis? → IGNORAR
       ↓
NotificationParser → extrai valor, tipo, categoria
       ↓
Sem valor ou tipo desconhecido? → IGNORAR
       ↓
HashUtils.buildNotificationHash() → verificar duplicidade
       ↓
Duplicado? → IGNORAR
       ↓
Modo automático + confiança ≥ 85%? → Salvar como auto_confirmed
       ↓
Caso contrário → Salvar como pending + Notificação de confirmação
       ↓
Usuário confirma → Criar expense ou income_entry no Supabase
       ↓
PWA sincroniza e exibe saldo atualizado
```

---

## Executar testes

```bash
./gradlew test
```

Os testes cobrem:
- Pix recebido / enviado
- Compra aprovada
- Boleto pago
- Transferência recebida
- Notificações sensíveis (devem ser ignoradas)
- Notificações sem valor (devem ser ignoradas)
- Duplicidade de hash
- Valores com milhar (R$ 1.250,90)
