package com.example

import android.app.Application
import android.util.Log
import com.example.utils.HotelNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class HotelRiveraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Asegurar la inicialización robusta de Firebase
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:571595793156:android:d59304a53ceea8138d2651")
                        .setProjectId("manager-hotel-r")
                        .setApiKey("AIzaSyAl18319cmBD2io7hCs9vlP1o9jXgM0PVQ")
                        .setStorageBucket("manager-hotel-r.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
            Log.i("HotelRiveraApp", "FirebaseApp inicializado correctamente en Application.onCreate")
        } catch (e: Exception) {
            Log.w("HotelRiveraApp", "Inicialización fallback de FirebaseApp: ${e.message}")
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:571595793156:android:d59304a53ceea8138d2651")
                    .setProjectId("manager-hotel-r")
                    .setApiKey("AIzaSyAl18319cmBD2io7hCs9vlP1o9jXgM0PVQ")
                    .setStorageBucket("manager-hotel-r.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            } catch (ex: Exception) {
                Log.e("HotelRiveraApp", "Error al inicializar FirebaseApp fallback: ${ex.message}")
            }
        }

        try {
            com.example.utils.FirebaseManager.getFirestore(this)
            com.example.utils.FirebaseManager.ensureAuth()
        } catch (e: Exception) {
            Log.w("HotelRiveraApp", "Firestore and Auth pre-warming: ${e.message}")
        }

        // 2. Crear canales de notificación del sistema
        try {
            HotelNotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            Log.w("HotelRiveraApp", "Error al crear canales de notificación: ${e.message}")
        }
    }
}
