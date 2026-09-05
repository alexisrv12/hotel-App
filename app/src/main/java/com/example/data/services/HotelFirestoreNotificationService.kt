package com.example.data.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.utils.HotelNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service and Realtime Manager that listens for new guest check-ins, check-outs,
 * and maintenance requests from Cloud Firestore and triggers local notifications
 * using HotelNotificationHelper.
 */
class HotelFirestoreNotificationService : Service() {

    companion object {
        private const val TAG = "FirestoreNotifService"
        const val DEFAULT_HOTEL_ID = "hotel_rivera_main"

        private var instance: HotelFirestoreNotificationService? = null

        fun start(context: Context) {
            try {
                val intent = Intent(context, HotelFirestoreNotificationService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting service", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        HotelNotificationHelper.createNotificationChannels(this)
        startListeningToFirestore()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isInitialized) {
            startListeningToFirestore()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListeningToFirestore() {
        serviceScope.launch {
            try {
                if (FirebaseApp.getApps(this@HotelFirestoreNotificationService).isEmpty()) {
                    Log.d(TAG, "Firebase not yet initialized in context")
                    return@launch
                }

                firestore = FirebaseFirestore.getInstance()
                val db = firestore ?: return@launch

                // Clean up previous listeners if any
                activeListeners.forEach { it.remove() }
                activeListeners.clear()

                // 1. Listen for Active Stays (Check-Ins / Check-Outs)
                val checkInsRef = db.collection("hotels")
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
                                // Trigger notification for new guest check-in
                                HotelNotificationHelper.sendGuestCheckInAlert(
                                    context = applicationContext,
                                    roomNumber = roomNumber,
                                    guestName = guestName,
                                    durationHours = durationHours.coerceAtLeast(1),
                                    totalAmount = price
                                )
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Trigger notification for check-out
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
                val housekeepingRef = db.collection("hotels")
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
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        serviceScope.cancel()
        instance = null
    }
}
