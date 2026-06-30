package com.controlpro.banklistener.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.controlpro.banklistener.MainActivity
import com.controlpro.banklistener.R
import com.controlpro.banklistener.data.local.AppPreferences
import com.controlpro.banklistener.data.local.SecurePreferences
import com.controlpro.banklistener.data.model.NotificationType
import com.controlpro.banklistener.data.repository.AuthRepository
import com.controlpro.banklistener.data.repository.NotificationRepository
import com.controlpro.banklistener.domain.usecase.ProcessNotificationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {

    private val TAG = "BankNLService"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var processUseCase: ProcessNotificationUseCase
    private lateinit var appPreferences: AppPreferences

    companion object {
        var isServiceRunning = false
        const val CHANNEL_ID = "bank_listener_channel"
        const val CHANNEL_CONFIRM_ID = "bank_confirm_channel"
        const val NOTIF_ID_BASE = 9000
        private var notifCounter = 0
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        Log.i(TAG, "BankNotificationListenerService iniciado")

        val securePrefs = SecurePreferences(applicationContext)
        val authRepo = AuthRepository(securePrefs)
        val notifRepo = NotificationRepository()

        processUseCase = ProcessNotificationUseCase(notifRepo, authRepo)
        appPreferences = AppPreferences(applicationContext)

        createNotificationChannels()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        job.cancel()
        Log.i(TAG, "BankNotificationListenerService encerrado")
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName ?: return

        scope.launch {
            try {
                val enabledPackages = appPreferences.enabledPackagesFlow.first()
                if (packageName !in enabledPackages) return@launch

                val notification = sbn.notification ?: return@launch
                val extras = notification.extras ?: return@launch

                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return@launch
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
                val fullText = if (bigText.length > text.length) bigText else text

                val autoMode = appPreferences.autoModeFlow.first()
                val minConfidence = appPreferences.minConfidenceFlow.first()

                val result = processUseCase.execute(
                    packageName = packageName,
                    title = title,
                    text = fullText,
                    timestamp = sbn.postTime,
                    autoMode = autoMode,
                    minConfidence = minConfidence
                )

                when (result) {
                    is ProcessNotificationUseCase.ProcessResult.RequiresConfirmation -> {
                        Log.i(TAG, "Notificação pendente de confirmação: ${result.parsed.description}")
                        showConfirmationNotification(result.parsed.description, result.import.id ?: "")
                    }
                    is ProcessNotificationUseCase.ProcessResult.AutoConfirmed -> {
                        Log.i(TAG, "Lançamento automático: ${result.import.description}")
                    }
                    is ProcessNotificationUseCase.ProcessResult.Ignored -> {
                        Log.d(TAG, "Ignorado: ${result.reason}")
                    }
                    is ProcessNotificationUseCase.ProcessResult.Duplicate -> {
                        Log.d(TAG, "Duplicado, ignorando: ${result.hash.take(8)}")
                    }
                    is ProcessNotificationUseCase.ProcessResult.Error -> {
                        Log.e(TAG, "Erro ao processar: ${result.exception.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exceção ao processar notificação", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Não precisamos fazer nada quando notificação é removida
    }

    private fun showConfirmationNotification(description: String, importId: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("screen", "pending")
            putExtra("import_id", importId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, importId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_CONFIRM_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💰 Transação detectada")
            .setContentText(description)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_BASE + (++notifCounter), notification)
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Leitor Bancário",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Serviço de leitura de notificações bancárias" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONFIRM_ID,
                "Confirmações Bancárias",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificações para confirmar lançamentos bancários" }
        )
    }
}
