package com.example.data.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.database.HotelDatabase
import com.example.data.repository.HotelFirestoreRepository
import com.example.data.repository.SessionDataStoreRepository
import com.example.utils.HotelNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Enhanced Background & Foreground Service that guarantees continuous background
 * execution, real-time timer tracking, 15-min and time-ended alerts with sound/vibration,
 * Firestore synchronization, and non-volatile background persistence for Hotel Rivera.
 */
class HotelFirestoreNotificationService : Service() {

    companion object {
        private const val TAG = "HotelBgService"
        const val DEFAULT_HOTEL_ID = "hotel_rivera_main"

        private var instance: HotelFirestoreNotificationService? = null

        fun start(context: Context) {
            try {
                val intent = Intent(context, HotelFirestoreNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting background service", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, HotelFirestoreNotificationService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping background service", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var timerMonitorJob: Job? = null
    private var isInitialized = false

    // Track which alerts have already been fired per room and stay
    // Key: "roomId_15min_checkInTime" or "roomId_ended_checkInTime"
    private val triggeredAlerts = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        HotelNotificationHelper.createNotificationChannels(this)
        startAsForeground()
        startLocalRoomTimerMonitoring()
        startListeningToFirestore()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (!isInitialized) {
            startLocalRoomTimerMonitoring()
            startListeningToFirestore()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        try {
            val initialNotification = HotelNotificationHelper.buildForegroundServiceNotification(
                context = this,
                occupiedRoomsCount = 0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    HotelNotificationHelper.NOTIFICATION_ID_BACKGROUND_SERVICE,
                    initialNotification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    }
                )
            } else {
                startForeground(
                    HotelNotificationHelper.NOTIFICATION_ID_BACKGROUND_SERVICE,
                    initialNotification
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground", e)
        }
    }

    /**
     * Continuously monitors occupied rooms in Room DB in the background
     * and triggers alerts for 15-minute warnings and time expirations.
     */
    private fun startLocalRoomTimerMonitoring() {
        timerMonitorJob?.cancel()
        timerMonitorJob = serviceScope.launch {
            try {
                val db = HotelDatabase.getDatabase(applicationContext)
                val hotelDao = db.hotelDao()

                // Collect rooms flow to stay updated
                hotelDao.getAllRooms().collectLatest { rooms ->
                    while (isActive) {
                        val now = System.currentTimeMillis()
                        val occupiedRooms = rooms.filter { it.status == "OCUPADA" }

                        // Update foreground notification with current count
                        try {
                            val updatedNotification = HotelNotificationHelper.buildForegroundServiceNotification(
                                context = this@HotelFirestoreNotificationService,
                                occupiedRoomsCount = occupiedRooms.size,
                                totalRoomsCount = rooms.size.coerceAtLeast(10)
                            )
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            notificationManager.notify(
                                HotelNotificationHelper.NOTIFICATION_ID_BACKGROUND_SERVICE,
                                updatedNotification
                            )
                        } catch (e: Exception) {
                            // Ignored
                        }

                        // Check each occupied room's remaining time
                        for (room in occupiedRooms) {
                            val checkOutTime = room.checkOutTimeMillis
                            val checkInTime = room.checkInTimeMillis

                            if (checkOutTime > 0) {
                                val remainingMillis = checkOutTime - now
                                val remainingMinutes = (remainingMillis / (1000 * 60)).toInt()

                                val key15Min = "${room.id}_15min_$checkInTime"
                                val keyEnded = "${room.id}_ended_$checkInTime"

                                // Case 1: 15 Minutes Warning Alert
                                if (remainingMillis in 1..(15 * 60 * 1000) && !triggeredAlerts.contains(key15Min)) {
                                    triggeredAlerts.add(key15Min)
                                    val guest = room.clientName ?: "Huésped"
                                    HotelNotificationHelper.sendRoom15MinWarningAlert(
                                        context = this@HotelFirestoreNotificationService,
                                        roomNumber = room.roomNumber,
                                        guestName = guest,
                                        remainingMinutes = remainingMinutes.coerceAtLeast(1)
                                    )
                                    triggerBackgroundSoundAndVibrate(isUrgent = false)
                                    Log.d(TAG, "Dispatched 15-min warning alert for Room ${room.roomNumber}")
                                }

                                // Case 2: Time Concluded Alert
                                if (remainingMillis <= 0 && !triggeredAlerts.contains(keyEnded)) {
                                    triggeredAlerts.add(keyEnded)
                                    val guest = room.clientName ?: "Huésped"
                                    HotelNotificationHelper.sendRoomTimeEndedAlert(
                                        context = this@HotelFirestoreNotificationService,
                                        roomNumber = room.roomNumber,
                                        guestName = guest
                                    )
                                    triggerBackgroundSoundAndVibrate(isUrgent = true)
                                    Log.d(TAG, "Dispatched Time Concluded alert for Room ${room.roomNumber}")
                                }
                            }
                        }

                        // Sleep 2.5 seconds before next evaluation loop
                        delay(2500L)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background timer monitor", e)
            }
        }
    }

    /**
     * Plays alarm sounds and vibration even when app is in background or screen is off.
     */
    @SuppressLint("MissingPermission")
    private fun triggerBackgroundSoundAndVibrate(isUrgent: Boolean) {
        serviceScope.launch(Dispatchers.Main) {
            try {
                // Acquire temporary wake lock for sound delivery
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HotelRivera:AlarmWakeLock"
                )
                wakeLock?.acquire(3000L)

                // 1. Play Tone
                try {
                    val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    val toneType = if (isUrgent) ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK else ToneGenerator.TONE_CDMA_HIGH_L
                    toneG.startTone(toneType, if (isUrgent) 1800 else 1000)
                } catch (e: Exception) {
                    try {
                        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                        ringtone?.play()
                    } catch (e2: Exception) {
                        // Ignore
                    }
                }

                // 2. Vibrate
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = if (isUrgent) longArrayOf(0, 500, 200, 500, 200, 700) else longArrayOf(0, 400, 200, 400)
                    val amplitudes = if (isUrgent) intArrayOf(0, 255, 0, 255, 0, 255) else intArrayOf(0, 200, 0, 200)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (isUrgent) 1500L else 800L)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sound/Vibration alert failed", e)
            }
        }
    }

    private fun startListeningToFirestore() {
        serviceScope.launch {
            try {
                val db = HotelDatabase.getDatabase(applicationContext)
                val hotelDao = db.hotelDao()
                val deviceDao = db.deviceDao()
                val sessionRepo = SessionDataStoreRepository(applicationContext)
                val firestoreRepo = HotelFirestoreRepository.getInstance(application, hotelDao, deviceDao, sessionRepo)
                firestoreRepo.startRealtimeListeners()

                if (FirebaseApp.getApps(this@HotelFirestoreNotificationService).isEmpty()) {
                    Log.d(TAG, "Firebase not yet initialized in context")
                    return@launch
                }

                firestore = FirebaseFirestore.getInstance()
                val firestoreDb = firestore ?: return@launch

                // Clean up previous listeners if any
                activeListeners.forEach { it.remove() }
                activeListeners.clear()

                // 1. Listen for Active Stays (Check-Ins / Check-Outs)
                val checkInsRef = firestoreDb.collection("hotels")
                    .document(DEFAULT_HOTEL_ID)
                    .collection("active_stays")

                val checkInRegistration = checkInsRef.addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen active_stays failed", error)
                        return@addSnapshotListener
                    }

                    snapshots?.documentChanges?.forEach { dc ->
                        val data = dc.document.data
                        val roomNumber = data["roomNumber"]?.toString() ?: dc.document.id
                        val guestName = data["clientName"]?.toString() ?: "Huésped"
                        val durationHours = (data["contractedDurationMinutes"] as? Number)?.toInt()?.let { it / 60 } ?: 4
                        val price = (data["priceCharged"] as? Number)?.toDouble() ?: 0.0

                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                HotelNotificationHelper.sendGuestCheckInAlert(
                                    context = applicationContext,
                                    roomNumber = roomNumber,
                                    guestName = guestName,
                                    durationHours = durationHours.coerceAtLeast(1),
                                    totalAmount = price
                                )
                            }
                            DocumentChange.Type.REMOVED -> {
                                HotelNotificationHelper.sendGuestCheckOutAlert(
                                    context = applicationContext,
                                    roomNumber = roomNumber,
                                    totalAmount = price
                                )
                            }
                            DocumentChange.Type.MODIFIED -> {
                                // Stay updated / extended
                            }
                        }
                    }
                }
                activeListeners.add(checkInRegistration)

                // 2. Listen for Maintenance / Housekeeping Requests
                val housekeepingRef = firestoreDb.collection("hotels")
                    .document(DEFAULT_HOTEL_ID)
                    .collection("housekeeping_tasks")

                val housekeepingRegistration = housekeepingRef.addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen housekeeping_tasks failed", error)
                        return@addSnapshotListener
                    }

                    snapshots?.documentChanges?.forEach { dc ->
                        if (dc.type == DocumentChange.Type.ADDED) {
                            val data = dc.document.data
                            val roomNumber = data["roomNumber"]?.toString() ?: "N/A"
                            val priority = data["priority"]?.toString() ?: "Alta"
                            val notes = data["notes"]?.toString() ?: "Limpieza / Mantenimiento solicitado"

                            HotelNotificationHelper.sendMaintenanceTaskAlert(
                                context = applicationContext,
                                roomNumber = roomNumber,
                                taskDescription = notes,
                                priority = priority
                            )
                        }
                    }
                }
                activeListeners.add(housekeepingRegistration)

                isInitialized = true
                Log.d(TAG, "Firestore realtime listeners successfully attached")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firestore notification listener", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerMonitorJob?.cancel()
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        serviceScope.cancel()
        instance = null
    }
}
