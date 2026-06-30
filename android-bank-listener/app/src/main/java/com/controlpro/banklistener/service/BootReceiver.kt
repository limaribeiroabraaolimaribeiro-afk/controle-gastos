package com.controlpro.banklistener.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Dispositivo reiniciado — NotificationListenerService será reativado automaticamente pelo Android")
            // O NotificationListenerService é gerenciado pelo sistema Android
            // e é reativado automaticamente após reinicialização se a permissão foi concedida
        }
    }
}
