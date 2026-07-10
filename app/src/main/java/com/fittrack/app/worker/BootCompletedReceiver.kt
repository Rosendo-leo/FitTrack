package com.fittrack.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Só precisa existir: receber BOOT_COMPLETED faz o sistema instanciar o processo do
 * app, o que roda FitTrackApplication.onCreate() e já reagenda lembretes, sync do
 * Drive e checagem de update — sem precisar duplicar essa lógica aqui.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
