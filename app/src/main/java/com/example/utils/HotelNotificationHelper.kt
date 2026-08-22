package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

/**
 * Notification Helper class for Hotel Rivera management system.
 * Handles Android 13+ POST_NOTIFICATIONS runtime checks, notification channel setup,
 * and delivers real-time alerts for guest check-ins, check-outs, and maintenance tasks.
 */
object HotelNotificationHelper {

    // Notification Channel IDs
    const val CHANNEL_CHECKINS = "hotel_checkin_alerts_channel"
    const val CHANNEL_MAINTENANCE = "hotel_maintenance_tasks_channel"
    const val CHANNEL_SYSTEM_ALERTS = "hotel_system_alerts_channel"
    const val CHANNEL_INVENTORY_ALERTS = "hotel_inventory_alerts_channel"
    const val CHANNEL_ROOM_TIMERS = "hotel_room_timers_alerts_channel"
    const val CHANNEL_BACKGROUND_SERVICE = "hotel_background_service_channel"

    const val NOTIFICATION_ID_BACKGROUND_SERVICE = 10001
    private const val NOTIFICATION_ID_CHECKIN_BASE = 20000
    private const val NOTIFICATION_ID_MAINTENANCE_BASE = 30000
    private const val NOTIFICATION_ID_SYSTEM_BASE = 40000
    private const val NOTIFICATION_ID_INVENTORY_BASE = 50000
    private const val NOTIFICATION_ID_TIMERS_BASE = 60000

    /**
     * Checks whether the application has granted POST_NOTIFICATIONS permission.
     * Always returns true on Android 12 (API 32) and below.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Creates and registers notification channels for check-ins, maintenance, and alerts.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Guest Check-Ins Channel
            val checkInChannel = NotificationChannel(
                CHANNEL_CHECKINS,
                "Check-ins y Huéspedes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas inmediatas de nuevos registros de huéspedes y asignación de habitaciones"
                enableVibration(true)
                enableLights(true)
            }

            // 2. Maintenance & Housekeeping Tasks Channel
            val maintenanceChannel = NotificationChannel(
                CHANNEL_MAINTENANCE,
                "Mantenimiento y Limpieza",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de habitaciones que requieren limpieza urgente o mantenimiento técnico"
                enableVibration(true)
                enableLights(true)
            }

            // 3. System & Proximity Alerts Channel
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM_ALERTS,
                "Alertas del Sistema y Proximidad",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de auditoría, proximidad de recepción y turnos de personal"
            }

            // 4. Room Inventory Low Stock Alerts Channel
            val inventoryChannel = NotificationChannel(
                CHANNEL_INVENTORY_ALERTS,
                "Alertas de Inventario Bajo Umbral",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas automáticas cuando insumos o productos caen por debajo del umbral mínimo"
                enableVibration(true)
                enableLights(true)
            }

            // 5. Room Timers & Expiration Alerts Channel (High Priority with Sound and Vibration)
            val timersChannel = NotificationChannel(
                CHANNEL_ROOM_TIMERS,
                "Temporizadores y Alertas de Tiempo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de 15 minutos restantes y alertas de tiempo cumplido en habitaciones"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // 6. Persistent Background Service Channel
            val backgroundChannel = NotificationChannel(
                CHANNEL_BACKGROUND_SERVICE,
                "Servicio en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo continuo de habitaciones y temporizadores cuando la aplicación está en segundo plano"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(checkInChannel, maintenanceChannel, systemChannel, inventoryChannel, timersChannel, backgroundChannel)
            )
        }
    }

    /**
     * Dispatches an alert notification when a new guest checks in.
     */
    @SuppressLint("MissingPermission")
    fun sendGuestCheckInAlert(
        context: Context,
        roomNumber: String,
        guestName: String,
        durationHours: Int,
        totalAmount: Double = 0.0
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_CHECKIN_BASE + (roomNumber.toIntOrNull() ?: 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CHECKINS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("🛎️ Nuevo Check-In: Habitación $roomNumber")
            .setContentText("Huésped: $guestName • Tiempo: ${durationHours}h")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "El huésped $guestName se ha registrado exitosamente en la Habitación $roomNumber.\n" +
                            "Tiempo contratado: $durationHours horas.\n" +
                            if (totalAmount > 0) "Monto registrado: $${String.format("%.2f", totalAmount)}" else "Check-in activo."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_CHECKIN_BASE + (roomNumber.toIntOrNull() ?: 1)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dispatches a notification for urgent maintenance or cleaning required in a room.
     */
    @SuppressLint("MissingPermission")
    fun sendMaintenanceTaskAlert(
        context: Context,
        roomNumber: String,
        taskDescription: String,
        priority: String = "Alta"
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MAINTENANCE_BASE + (roomNumber.toIntOrNull() ?: 2),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🧹 Tarea de Mantenimiento: Hab. $roomNumber")
            .setContentText("Prioridad $priority: $taskDescription")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Se requiere atención en Habitación $roomNumber.\n" +
                            "Prioridad: $priority\n" +
                            "Detalle: $taskDescription"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_MAINTENANCE_BASE + (roomNumber.toIntOrNull() ?: 2)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dispatches a notification when staff reports a broken item with optional photo evidence.
     */
    @SuppressLint("MissingPermission")
    fun sendBrokenItemReportAlert(
        context: Context,
        roomNumber: String,
        itemName: String,
        category: String,
        priority: String,
        hasPhoto: Boolean,
        reportedBy: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MAINTENANCE_BASE + 1000 + (roomNumber.toIntOrNull() ?: 99),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val photoBadge = if (hasPhoto) " 📷 [Foto Adjunta]" else ""
        val builder = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🛠️ Reporte de Avería: $itemName ($roomNumber)$photoBadge")
            .setContentText("Prioridad: $priority • Reportado por: $reportedBy")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "¡Nuevo reporte de daño/mantenimiento registrado!\n" +
                            "• Ubicación: $roomNumber\n" +
                            "• Objeto/Avería: $itemName ($category)\n" +
                            "• Nivel de Urgencia: $priority\n" +
                            "• Reportado por: $reportedBy\n" +
                            (if (hasPhoto) "• Evidencia fotográfica capturada con cámara integrada disponible." else "• Sin foto adjunta.")
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_MAINTENANCE_BASE + 1000 + (roomNumber.toIntOrNull() ?: 99)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dispatches a notification for guest check-out and room release.
     */
    @SuppressLint("MissingPermission")
    fun sendGuestCheckOutAlert(
        context: Context,
        roomNumber: String,
        totalAmount: Double
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_CHECKINS)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("🚪 Habitación $roomNumber Liberada")
            .setContentText("Check-out completado. Total liquidado: $${String.format("%.2f", totalAmount)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_CHECKIN_BASE + 500 + (roomNumber.toIntOrNull() ?: 0)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dispatches an automated notification when an inventory item falls below its defined minimum threshold.
     */
    @SuppressLint("MissingPermission")
    fun sendLowStockAlert(
        context: Context,
        itemId: Long,
        itemName: String,
        currentStock: Double,
        minimumStock: Double,
        unit: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (NOTIFICATION_ID_INVENTORY_BASE + itemId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_INVENTORY_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ Inventario Bajo Umbral: $itemName")
            .setContentText("Existencia: $currentStock $unit (Mínimo definido: $minimumStock $unit)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "¡Alerta de Inventario de Habitaciones!\n" +
                            "El insumo '$itemName' ha caído por debajo del umbral mínimo de seguridad configurado.\n" +
                            "Stock Actual: $currentStock $unit\n" +
                            "Umbral Mínimo: $minimumStock $unit\n" +
                            "Se requiere reabastecimiento para mantener operativas las habitaciones."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (NOTIFICATION_ID_INVENTORY_BASE + itemId).toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Builds the persistent ongoing Foreground Service notification.
     */
    fun buildForegroundServiceNotification(
        context: Context,
        occupiedRoomsCount: Int,
        totalRoomsCount: Int = 10
    ): android.app.Notification {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BACKGROUND_SERVICE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentTitle = "🏨 Hotel Rivera • Servicio en Segundo Plano"
        val contentText = if (occupiedRoomsCount > 0) {
            "$occupiedRoomsCount de $totalRoomsCount habitaciones ocupadas • Monitoreo de tiempo activo"
        } else {
            "Monitoreo de temporizadores y reservaciones activo"
        }

        return NotificationCompat.Builder(context, CHANNEL_BACKGROUND_SERVICE)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Dispatches a high-priority 15-minute remaining warning alert for an occupied room.
     */
    @SuppressLint("MissingPermission")
    fun sendRoom15MinWarningAlert(
        context: Context,
        roomNumber: String,
        guestName: String,
        remainingMinutes: Int = 15
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TIMERS_BASE + 100 + (roomNumber.toIntOrNull() ?: 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ROOM_TIMERS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⏰ ¡Alerta 15 Minutos! Habitación $roomNumber")
            .setContentText("El tiempo del huésped $guestName está por finalizar (${remainingMinutes} min restantes).")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "¡Atención Recepción!\n" +
                            "La Habitación $roomNumber ($guestName) tiene menos de 15 minutos de estancia restante.\n" +
                            "Por favor contactar al huésped para consulta de extensión o preparación de salida."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_TIMERS_BASE + 100 + (roomNumber.toIntOrNull() ?: 1)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dispatches a high-priority alarm notification when a room's contracted time has expired.
     */
    @SuppressLint("MissingPermission")
    fun sendRoomTimeEndedAlert(
        context: Context,
        roomNumber: String,
        guestName: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TIMERS_BASE + 200 + (roomNumber.toIntOrNull() ?: 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ROOM_TIMERS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("🚨 ¡TIEMPO CUMPLIDO! Habitación $roomNumber")
            .setContentText("El tiempo de estancia para $guestName ha concluido.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "¡TIEMPO CONCLUIDO!\n" +
                            "El período contratado en la Habitación $roomNumber para $guestName ha finalizado.\n" +
                            "Se requiere proceder con el cobro / Check-Out o cobro de tiempo adicional."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 800, 300, 800, 300, 800))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_TIMERS_BASE + 200 + (roomNumber.toIntOrNull() ?: 1)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
