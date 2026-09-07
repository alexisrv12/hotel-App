package com.example.utils

import android.content.Context
import android.util.Log
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.model.Room
import com.example.data.model.VinculacionToken
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.tasks.await

/**
 * Gestor centralizado de Firebase para Hotel Rivera.
 * Proporciona almacenamiento en la nube sólido en Firestore y sincronización
 * en tiempo real para vincular dos o más dispositivos mediante PIN o código QR.
 */
object FirebaseManager {

    private const val TAG = "FirebaseManager"

    const val COLLECTION_VINCULACIONES = "vinculaciones"
    const val COLLECTION_DISPOSITIVOS = "dispositivos"
    const val COLLECTION_HABITACIONES = "habitaciones"
    const val COLLECTION_CONFIGURACION = "configuracion"

    private const val FIREBASE_PROJECT_ID = "manager-hotel-r"
    private const val FIREBASE_API_KEY = "AIzaSyAl18319cmBD2io7hCs9vlP1o9jXgM0PVQ"
    private const val FIREBASE_STORAGE_BUCKET = "manager-hotel-r.firebasestorage.app"
    private const val FIREBASE_APP_ID = "1:571595793156:android:d59304a53ceea8138d2651"

    @Volatile
    private var isInitialized = false

    /**
     * Asegura la inicialización segura de FirebaseApp y la caché offline de Firestore.
     */
    fun getFirestore(context: Context): FirebaseFirestore {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    try {
                        if (FirebaseApp.getApps(context).isEmpty()) {
                            val options = FirebaseOptions.Builder()
                                .setApplicationId(FIREBASE_APP_ID)
                                .setProjectId(FIREBASE_PROJECT_ID)
                                .setApiKey(FIREBASE_API_KEY)
                                .setStorageBucket(FIREBASE_STORAGE_BUCKET)
                                .build()
                            FirebaseApp.initializeApp(context.applicationContext, options)
                            Log.i(TAG, "FirebaseApp inicializada con proyecto $FIREBASE_PROJECT_ID")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Aviso durante FirebaseApp.initializeApp: ${e.message}")
                    }

                    try {
                        val db = Firebase.firestore
                        db.firestoreSettings = firestoreSettings {
                            setLocalCacheSettings(persistentCacheSettings {})
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Firestore cache settings ya aplicados: ${e.message}")
                    }
                    isInitialized = true
                }
            }
        }
        return Firebase.firestore
    }

    /**
     * Crea o actualiza una sesión de vinculación en Firestore accesible por todos los dispositivos.
     */
    suspend fun createVinculacionSession(
        context: Context,
        pin: String,
        qrToken: String,
        role: String = "RECEPCION",
        durationMinutes: Long = 15
    ): Result<VinculacionToken> {
        return try {
            val db = getFirestore(context)
            val now = System.currentTimeMillis()
            val expiresAt = now + (durationMinutes * 60 * 1000L)

            val tokenObj = VinculacionToken(
                id = pin,
                pin = pin,
                qrToken = qrToken,
                fechaCreacion = now,
                fechaExpiracion = expiresAt,
                activo = true,
                estado = "PENDIENTE",
                hotelName = "Hotel Rivera",
                rol = role
            )

            // Guardar con ID = PIN para búsqueda directa e instantánea
            db.collection(COLLECTION_VINCULACIONES).document(pin).set(tokenObj).await()

            // Si el qrToken difiere del PIN, guardar también un índice con docId = qrToken
            if (qrToken.isNotBlank() && qrToken != pin) {
                db.collection(COLLECTION_VINCULACIONES).document(qrToken).set(tokenObj).await()
            }

            Log.i(TAG, "Sesión de vinculación creada en Firestore con PIN: $pin")
            Result.success(tokenObj)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear sesión de vinculación en Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Valida un PIN o Token QR contra Firebase Firestore para autorizar un dispositivo.
     */
    suspend fun validatePinOrQr(
        context: Context,
        input: String,
        deviceId: String,
        deviceName: String
    ): Result<VinculacionToken> {
        return try {
            val db = getFirestore(context)
            val trimmed = input.trim()
            val now = System.currentTimeMillis()

            var matchedDoc = db.collection(COLLECTION_VINCULACIONES).document(trimmed).get().await()

            // Si no se encuentra por document ID, buscar por el campo 'pin'
            if (!matchedDoc.exists()) {
                val queryByPin = db.collection(COLLECTION_VINCULACIONES)
                    .whereEqualTo("pin", trimmed)
                    .whereEqualTo("activo", true)
                    .get()
                    .await()
                matchedDoc = queryByPin.documents.firstOrNull() ?: matchedDoc
            }

            // Si no se encuentra, buscar por el campo 'qrToken'
            if (!matchedDoc.exists()) {
                val queryByQr = db.collection(COLLECTION_VINCULACIONES)
                    .whereEqualTo("qrToken", trimmed)
                    .whereEqualTo("activo", true)
                    .get()
                    .await()
                matchedDoc = queryByQr.documents.firstOrNull() ?: matchedDoc
            }

            // Si el input parece contener un PIN de 6 dígitos dentro de un texto más largo (ej. QR)
            if (!matchedDoc.exists()) {
                val pinRegex = Regex("\\b\\d{6}\\b")
                val foundPin = pinRegex.find(trimmed)?.value
                if (foundPin != null) {
                    val fallbackDoc = db.collection(COLLECTION_VINCULACIONES).document(foundPin).get().await()
                    if (fallbackDoc.exists()) {
                        matchedDoc = fallbackDoc
                    }
                }
            }

            if (!matchedDoc.exists()) {
                return Result.failure(Exception("El PIN o código QR ingresado no existe o no es válido."))
            }

            val activo = matchedDoc.getBoolean("activo") ?: false
            if (!activo) {
                return Result.failure(Exception("Este código de vinculación ha sido desactivado o revocado."))
            }

            val fechaExpiracion = matchedDoc.getLong("fechaExpiracion")
                ?: (matchedDoc.getLong("fechaCreacion")?.plus(15 * 60 * 1000L) ?: (now + 10000L))

            if (now > fechaExpiracion) {
                return Result.failure(Exception("El código de vinculación ha expirado. Solicite un nuevo código en Gerencia."))
            }

            val assignedRole = matchedDoc.getString("rol") ?: matchedDoc.getString("role") ?: "RECEPCION"
            val effectivePin = matchedDoc.getString("pin") ?: trimmed
            val effectiveQrToken = matchedDoc.getString("qrToken") ?: trimmed

            // Actualizar la vinculación en Firestore marcándola como vinculada con la información del dispositivo
            matchedDoc.reference.update(
                mapOf(
                    "estado" to "VINCULADO",
                    "dispositivoVinculadoId" to deviceId,
                    "dispositivoVinculadoNombre" to deviceName,
                    "fechaVinculacion" to now
                )
            ).await()

            // Registrar inmediatamente el nuevo dispositivo en la colección 'dispositivos' de Firestore
            registerDeviceInFirestore(
                context = context,
                deviceId = deviceId,
                deviceName = deviceName,
                role = assignedRole,
                userAssigned = "$assignedRole@hotelrivera.com".lowercase()
            )

            val tokenResult = VinculacionToken(
                id = matchedDoc.id,
                pin = effectivePin,
                qrToken = effectiveQrToken,
                activo = true,
                estado = "VINCULADO",
                rol = assignedRole,
                dispositivoVinculadoId = deviceId,
                dispositivoVinculadoNombre = deviceName
            )

            Log.i(TAG, "Dispositivo $deviceName ($deviceId) vinculado exitosamente con rol $assignedRole")
            Result.success(tokenResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error validando PIN/QR en Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Revoca una sesión de vinculación en Firestore.
     */
    suspend fun revokeVinculacionSession(context: Context, pinOrToken: String): Boolean {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_VINCULACIONES).document(pinOrToken)
                .update(mapOf("activo" to false, "estado" to "REVOCADO"))
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error revocando sesión $pinOrToken: ${e.message}")
            false
        }
    }

    /**
     * Registra o actualiza un dispositivo en la colección 'dispositivos' de Firestore.
     */
    suspend fun registerDeviceInFirestore(
        context: Context,
        deviceId: String,
        deviceName: String,
        role: String,
        userAssigned: String
    ) {
        try {
            val db = getFirestore(context)
            val now = System.currentTimeMillis()
            val deviceData = mapOf(
                "deviceId" to deviceId,
                "name" to deviceName,
                "userAssigned" to userAssigned,
                "role" to role,
                "connectionStatus" to DeviceConnectionStatus.CONNECTED,
                "realTimeConnectivityStatus" to RealTimeConnectivityStatus.ACTIVE,
                "isAuthorized" to true,
                "lastHeartbeat" to now,
                "timestamp" to now
            )
            db.collection(COLLECTION_DISPOSITIVOS).document(deviceId)
                .set(deviceData, SetOptions.merge())
                .await()
            Log.i(TAG, "Dispositivo $deviceId registrado en Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Error al registrar dispositivo en Firestore: ${e.message}")
        }
    }

    /**
     * Actualiza el latido o estado de conectividad en Firestore.
     */
    suspend fun updateDeviceHeartbeat(context: Context, deviceId: String) {
        try {
            val db = getFirestore(context)
            val now = System.currentTimeMillis()
            db.collection(COLLECTION_DISPOSITIVOS).document(deviceId)
                .update(
                    mapOf(
                        "lastHeartbeat" to now,
                        "realTimeConnectivityStatus" to RealTimeConnectivityStatus.ACTIVE,
                        "connectionStatus" to DeviceConnectionStatus.CONNECTED
                    )
                ).await()
        } catch (e: Exception) {
            Log.d(TAG, "Heartbeat Firestore: ${e.message}")
        }
    }

    /**
     * Revoca la autorización de un dispositivo en Firestore.
     */
    suspend fun revokeDeviceInFirestore(context: Context, deviceId: String) {
        try {
            val db = getFirestore(context)
            db.collection(COLLECTION_DISPOSITIVOS).document(deviceId)
                .update(
                    mapOf(
                        "isAuthorized" to false,
                        "connectionStatus" to DeviceConnectionStatus.DISCONNECTED,
                        "realTimeConnectivityStatus" to RealTimeConnectivityStatus.DISCONNECTED
                    )
                ).await()
            Log.i(TAG, "Dispositivo $deviceId revocado en Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Error al revocar dispositivo en Firestore: ${e.message}")
        }
    }

    /**
     * Escucha en tiempo real la lista de dispositivos vinculados desde Firestore.
     */
    fun observeLinkedDevices(
        context: Context,
        onDevicesChanged: (List<DeviceEntity>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_DISPOSITIVOS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error escuchando dispositivos en Firestore: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val now = System.currentTimeMillis()
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val devId = doc.getString("deviceId") ?: doc.id
                                val name = doc.getString("name") ?: "Terminal Móvil"
                                val user = doc.getString("userAssigned") ?: "Recepción"
                                val connStatus = doc.getString("connectionStatus") ?: DeviceConnectionStatus.CONNECTED
                                val isAuth = doc.getBoolean("isAuthorized") ?: true
                                val lastHb = doc.getLong("lastHeartbeat") ?: now
                                val ts = doc.getLong("timestamp") ?: now

                                val computedStatus = if (isAuth && (now - lastHb <= 45000L)) {
                                    RealTimeConnectivityStatus.ACTIVE
                                } else {
                                    RealTimeConnectivityStatus.DISCONNECTED
                                }

                                DeviceEntity(
                                    deviceId = devId,
                                    name = name,
                                    userAssigned = user,
                                    connectionStatus = if (isAuth) connStatus else DeviceConnectionStatus.DISCONNECTED,
                                    realTimeConnectivityStatus = computedStatus,
                                    lastHeartbeat = lastHb,
                                    timestamp = ts
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onDevicesChanged(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando listener de dispositivos: ${e.message}", e)
            null
        }
    }

    /**
     * Escucha en tiempo real la colección 'habitaciones' de Firestore para sincronización instantánea.
     */
    fun observeHabitaciones(
        context: Context,
        onRoomsChanged: (List<Room>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_HABITACIONES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error observando habitaciones en Firestore: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val rooms = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val num = doc.getString("numero") ?: doc.getString("roomNumber") ?: doc.id
                                val estado = doc.getString("estado") ?: doc.getString("status") ?: "Disponible"
                                val precio = doc.getDouble("precio") ?: doc.getDouble("price") ?: 150.0
                                val tipo = doc.getString("roomType") ?: "Estándar"
                                val cliente = doc.getString("clientName")
                                val dpi = doc.getString("clientDpi")
                                val checkIn = doc.getLong("checkInTimestamp") ?: 0L
                                val checkOut = doc.getLong("checkOutTimestamp") ?: 0L
                                val notas = doc.getString("notes")

                                Room(
                                    id = id,
                                    roomNumber = num,
                                    status = estado,
                                    price = precio,
                                    roomType = tipo,
                                    clientName = cliente,
                                    clientDpi = dpi,
                                    checkInTimestamp = checkIn,
                                    checkOutTimestamp = checkOut,
                                    notes = notas
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onRoomsChanged(rooms)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando listener de habitaciones: ${e.message}", e)
            null
        }
    }
}
