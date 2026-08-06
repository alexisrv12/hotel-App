package com.example.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity

/**
 * Utility manager responsible for creating notification channels and firing
 * local system notifications to alert the Manager about new device access requests.
 */
object DeviceNotificationManager {

    const val CHANNEL_ID = "device_access_requests_channel"
    private const val CHANNEL_NAME = "Solicitudes de Acceso a Dispositivos"
    private const val CHANNEL_DESCRIPTION = "Notificaciones locales para alertar al Gerente sobre solicitudes de vinculación de terminales"

    /**
     * Initializes the notification channel required for Android 8.0 (API 26) and above.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Sends a local notification to alert the Manager that a device is requesting access.
     */
    @SuppressLint("MissingPermission")
    fun notifyNewDeviceAccessRequest(context: Context, device: DeviceEntity) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            device.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (device.connectionStatus) {
            DeviceConnectionStatus.PENDING -> "esperando aprobación del Gerente"
            DeviceConnectionStatus.CONNECTED -> "vinculado y activo"
            else -> "solicitando conexión"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("📱 Nueva Solicitud de Acceso: ${device.name}")
            .setContentText("El dispositivo '${device.name}' (${device.userAssigned}) está $statusText.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("El dispositivo '${device.name}' con ID (${device.deviceId}) asignado a ${device.userAssigned} está $statusText en el sistema.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(device.id.toInt(), builder.build())
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission on Android 13+
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends a notification when a device is disconnected or loses heartbeat.
     */
    @SuppressLint("MissingPermission")
    fun notifyDeviceDisconnected(context: Context, device: DeviceEntity) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ Dispositivo Desconectado: ${device.name}")
            .setContentText("La terminal '${device.name}' (${device.deviceId}) se ha desconectado del sistema.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((device.id + 10000).toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
