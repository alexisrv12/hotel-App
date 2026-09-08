package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.ProductEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.StayHistoryEntity
import com.example.data.entities.SupplyEntity
import com.example.data.entities.UserEntity
import com.example.data.model.Room
import com.example.data.model.VinculacionToken
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.tasks.await

/**
 * Reporte de diagnóstico de conectividad con Firebase para verificación de estado multidispositivo.
 */
data class DiagnosticReport(
    val isFirebaseInitialized: Boolean,
    val projectId: String,
    val appId: String,
    val authUid: String?,
    val isOnline: Boolean,
    val isFirestoreConnected: Boolean,
    val latencyMs: Long,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

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
    const val COLLECTION_VENTAS = "ventas"
    const val COLLECTION_HISTORIAL_ESTANCIAS = "historial_estancias"
    const val COLLECTION_PRODUCTOS = "productos"
    const val COLLECTION_INSUMOS = "insumos"
    const val COLLECTION_USUARIOS = "usuarios"
    const val COLLECTION_CONFIGURACION = "configuracion"

    const val FIREBASE_PROJECT_ID = "manager-hotel-r"
    const val FIREBASE_API_KEY = "AIzaSyAl18319cmBD2io7hCs9vlP1o9jXgM0PVQ"
    const val FIREBASE_STORAGE_BUCKET = "manager-hotel-r.firebasestorage.app"
    const val FIREBASE_APP_ID = "1:571595793156:android:d59304a53ceea8138d2651"

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
     * Asegura que el usuario cuente con una sesión de Firebase Auth (anónima o credencial existente),
     * permitiendo que las reglas de seguridad de Firestore ('request.auth != null') funcionen sin bloqueos.
     */
    fun ensureAuth(
        onSuccess: ((FirebaseUser?) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.i(TAG, "FirebaseAuth sesión existente verificada: UID=${currentUser.uid}")
                onSuccess?.invoke(currentUser)
                return
            }
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    Log.i(TAG, "FirebaseAuth autenticado anónimamente con éxito: UID=${result.user?.uid}")
                    onSuccess?.invoke(result.user)
                }
                .addOnFailureListener { err ->
                    Log.w(TAG, "FirebaseAuth aviso de autenticación anónima: ${err.message}")
                    onFailure?.invoke(err)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en ensureAuth: ${e.message}", e)
            onFailure?.invoke(e)
        }
    }

    /**
     * Reactiva la conexión de red de Firestore tras recuperar conectividad.
     */
    fun enableNetwork(context: Context) {
        try {
            getFirestore(context).enableNetwork()
            Log.i(TAG, "Firestore red reactivada exitosamente")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al habilitar red en Firestore: ${e.message}")
        }
    }

    /**
     * Pausa la red de Firestore en modo offline para ahorrar recursos.
     */
    fun disableNetwork(context: Context) {
        try {
            getFirestore(context).disableNetwork()
            Log.i(TAG, "Firestore red deshabilitada temporalmente")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al deshabilitar red en Firestore: ${e.message}")
        }
    }

    /**
     * Verifica si el dispositivo tiene conexión a internet activa.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ejecuta una comprobación de diagnóstico que confirma la conectividad con el proyecto Firebase.
     */
    suspend fun runDiagnostics(context: Context): DiagnosticReport {
        val startTime = System.currentTimeMillis()
        val isNet = isNetworkAvailable(context)
        var isInit = false
        var authUid: String? = null
        var isConnected = false
        var errMsg: String? = null

        try {
            val db = getFirestore(context)
            isInit = true

            // Verificar Auth
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                try {
                    val authResult = auth.signInAnonymously().await()
                    authUid = authResult.user?.uid
                } catch (e: Exception) {
                    errMsg = "Auth warning: ${e.message}"
                }
            } else {
                authUid = auth.currentUser?.uid
            }

            // Ping de lectura / escritura en Firestore
            val testDocRef = db.collection("configuracion").document("ping_test")
            testDocRef.set(
                mapOf(
                    "lastPing" to System.currentTimeMillis(),
                    "clientProject" to FIREBASE_PROJECT_ID
                ),
                SetOptions.merge()
            ).await()

            val snapshot = testDocRef.get().await()
            isConnected = snapshot.exists()
        } catch (e: Exception) {
            errMsg = e.message
            Log.w(TAG, "Diagnóstico Firebase aviso: ${e.message}")
        }

        val latency = System.currentTimeMillis() - startTime
        return DiagnosticReport(
            isFirebaseInitialized = isInit,
            projectId = FIREBASE_PROJECT_ID,
            appId = FIREBASE_APP_ID,
            authUid = authUid,
            isOnline = isNet,
            isFirestoreConnected = isConnected,
            latencyMs = latency,
            errorMessage = errMsg
        )
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

    /**
     * Guarda o actualiza una habitación en Firestore de forma idempotente con SetOptions.merge().
     */
    suspend fun saveHabitacionInFirestore(context: Context, room: Room) {
        try {
            val db = getFirestore(context)
            val num = room.roomNumber.ifBlank { room.id }.trim()
            val data = hashMapOf<String, Any>(
                "numero" to num,
                "roomNumber" to num,
                "estado" to room.status,
                "status" to room.status,
                "precio" to room.price,
                "price" to room.price,
                "roomType" to room.roomType,
                "clientName" to (room.clientName ?: ""),
                "clientDpi" to (room.clientDpi ?: ""),
                "checkInTimestamp" to room.checkInTimestamp,
                "checkOutTimestamp" to room.checkOutTimestamp,
                "notes" to (room.notes ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_HABITACIONES).document(num)
                .set(data, SetOptions.merge()).await()
            Log.i(TAG, "Habitación $num guardada exitosamente en Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al guardar habitación en Firestore: ${e.message}")
        }
    }

    /**
     * Elimina una habitación de Firestore.
     */
    suspend fun deleteHabitacionInFirestore(context: Context, roomNumber: String) {
        try {
            val db = getFirestore(context)
            db.collection(COLLECTION_HABITACIONES).document(roomNumber.trim()).delete().await()
            Log.i(TAG, "Habitación $roomNumber eliminada de Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al eliminar habitación en Firestore: ${e.message}")
        }
    }

    /**
     * Guarda una venta en Firestore de forma idempotente para sincronización contable inmediata.
     */
    suspend fun saveVentaInFirestore(context: Context, sale: SaleRecordEntity) {
        try {
            val db = getFirestore(context)
            val docId = "${sale.timestampMillis}_${sale.productName.hashCode()}"
            val data = hashMapOf<String, Any>(
                "id" to docId,
                "productName" to sale.productName,
                "quantity" to sale.quantity,
                "unitPrice" to sale.unitPrice,
                "totalPrice" to sale.totalPrice,
                "profit" to sale.profit,
                "timestampMillis" to sale.timestampMillis,
                "registeredBy" to sale.registeredBy,
                "paymentMethod" to sale.paymentMethod
            )
            db.collection(COLLECTION_VENTAS).document(docId)
                .set(data, SetOptions.merge()).await()
            Log.i(TAG, "Venta de ${sale.productName} sincronizada con Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al guardar venta en Firestore: ${e.message}")
        }
    }

    /**
     * Escucha en tiempo real la colección de ventas de Firestore.
     */
    fun observeVentas(
        context: Context,
        onVentasChanged: (List<SaleRecordEntity>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_VENTAS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Aviso escuchando ventas en Firestore: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val sales = snapshot.documents.mapNotNull { doc ->
                            try {
                                val name = doc.getString("productName") ?: return@mapNotNull null
                                val qty = doc.getLong("quantity")?.toInt() ?: 1
                                val unitP = doc.getDouble("unitPrice") ?: 0.0
                                val totP = doc.getDouble("totalPrice") ?: 0.0
                                val prof = doc.getDouble("profit") ?: 0.0
                                val ts = doc.getLong("timestampMillis") ?: System.currentTimeMillis()
                                val by = doc.getString("registeredBy") ?: "Recepción"
                                val payMethod = doc.getString("paymentMethod") ?: "Efectivo"

                                SaleRecordEntity(
                                    id = 0L,
                                    productName = name,
                                    quantity = qty,
                                    unitPrice = unitP,
                                    totalPrice = totP,
                                    profit = prof,
                                    timestampMillis = ts,
                                    registeredBy = by,
                                    paymentMethod = payMethod
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onVentasChanged(sales)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando listener de ventas: ${e.message}", e)
            null
        }
    }

    /**
     * Guarda un registro de estancia en Firestore al dar salida a un huésped.
     */
    suspend fun saveHistorialEstanciaInFirestore(context: Context, history: StayHistoryEntity) {
        try {
            val db = getFirestore(context)
            val docId = "${history.roomNumber}_${history.checkInTimeMillis}_${history.checkOutTimeMillis}"
            val data = hashMapOf<String, Any>(
                "id" to docId,
                "roomNumber" to history.roomNumber,
                "clientName" to history.clientName,
                "clientDpi" to (history.clientDpi ?: ""),
                "guestCount" to history.guestCount,
                "checkInTimeMillis" to history.checkInTimeMillis,
                "checkOutTimeMillis" to history.checkOutTimeMillis,
                "contractedTimeName" to history.contractedTimeName,
                "contractedDurationMinutes" to history.contractedDurationMinutes,
                "actualDurationMinutes" to history.actualDurationMinutes,
                "priceCharged" to history.priceCharged,
                "paymentMethod" to history.paymentMethod,
                "receptionistName" to history.receptionistName,
                "notes" to (history.notes ?: ""),
                "dateString" to history.dateString
            )
            db.collection(COLLECTION_HISTORIAL_ESTANCIAS).document(docId)
                .set(data, SetOptions.merge()).await()
            Log.i(TAG, "Historial de estancia Hab ${history.roomNumber} sincronizado con Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al guardar historial en Firestore: ${e.message}")
        }
    }

    /**
     * Escucha en tiempo real la colección de historial de estancias de Firestore.
     */
    fun observeHistorialEstancias(
        context: Context,
        onHistorialChanged: (List<StayHistoryEntity>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_HISTORIAL_ESTANCIAS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Aviso escuchando historial de estancias en Firestore: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val historyList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val roomNum = doc.getString("roomNumber") ?: return@mapNotNull null
                                val client = doc.getString("clientName") ?: "Cliente General"
                                val dpi = doc.getString("clientDpi")
                                val guests = doc.getLong("guestCount")?.toInt() ?: 1
                                val inTs = doc.getLong("checkInTimeMillis") ?: 0L
                                val outTs = doc.getLong("checkOutTimeMillis") ?: 0L
                                val rateName = doc.getString("contractedTimeName") ?: "Tarifa General"
                                val contDur = doc.getLong("contractedDurationMinutes") ?: 0L
                                val actDur = doc.getLong("actualDurationMinutes") ?: 0L
                                val charged = doc.getDouble("priceCharged") ?: 0.0
                                val pay = doc.getString("paymentMethod") ?: "Efectivo"
                                val recep = doc.getString("receptionistName") ?: "Recepción"
                                val notes = doc.getString("notes")
                                val dateStr = doc.getString("dateString") ?: ""

                                StayHistoryEntity(
                                    id = 0L,
                                    roomNumber = roomNum,
                                    clientName = client,
                                    clientDpi = dpi,
                                    guestCount = guests,
                                    checkInTimeMillis = inTs,
                                    checkOutTimeMillis = outTs,
                                    contractedTimeName = rateName,
                                    contractedDurationMinutes = contDur,
                                    actualDurationMinutes = actDur,
                                    priceCharged = charged,
                                    paymentMethod = pay,
                                    receptionistName = recep,
                                    notes = notes,
                                    dateString = dateStr
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onHistorialChanged(historyList)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando listener de historial: ${e.message}", e)
            null
        }
    }

    /**
     * Guarda o actualiza un insumo en Firestore.
     */
    suspend fun saveInsumoInFirestore(context: Context, supply: SupplyEntity) {
        try {
            val db = getFirestore(context)
            val docId = supply.name.trim().lowercase()
            val data = hashMapOf<String, Any>(
                "name" to supply.name,
                "unit" to supply.unit,
                "stockCurrent" to supply.stockCurrent,
                "stockMinimum" to supply.stockMinimum,
                "autoDeductQuantityPerStay" to supply.autoDeductQuantityPerStay,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_INSUMOS).document(docId)
                .set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso guardando insumo en Firestore: ${e.message}")
        }
    }

    /**
     * Escucha en tiempo real la colección de insumos de Firestore.
     */
    fun observeInsumos(
        context: Context,
        onInsumosChanged: (List<SupplyEntity>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_INSUMOS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val supplies = snapshot.documents.mapNotNull { doc ->
                        try {
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val unit = doc.getString("unit") ?: "Unidad"
                            val current = doc.getDouble("stockCurrent") ?: 0.0
                            val min = doc.getDouble("stockMinimum") ?: 0.0
                            val deduct = doc.getDouble("autoDeductQuantityPerStay") ?: 0.0
                            SupplyEntity(
                                id = 0L,
                                name = name,
                                unit = unit,
                                stockCurrent = current,
                                stockMinimum = min,
                                autoDeductQuantityPerStay = deduct
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onInsumosChanged(supplies)
                }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Guarda o actualiza un producto en Firestore.
     */
    suspend fun saveProductoInFirestore(context: Context, product: ProductEntity) {
        try {
            val db = getFirestore(context)
            val docId = product.name.trim().lowercase()
            val data = hashMapOf<String, Any>(
                "name" to product.name,
                "price" to product.price,
                "costPrice" to product.costPrice,
                "stock" to product.stock,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_PRODUCTOS).document(docId)
                .set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso guardando producto en Firestore: ${e.message}")
        }
    }

    /**
     * Escucha en tiempo real la colección de productos de Firestore.
     */
    fun observeProductos(
        context: Context,
        onProductosChanged: (List<ProductEntity>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_PRODUCTOS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val products = snapshot.documents.mapNotNull { doc ->
                        try {
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val price = doc.getDouble("price") ?: 0.0
                            val cost = doc.getDouble("costPrice") ?: 0.0
                            val stock = doc.getLong("stock")?.toInt() ?: 0
                            ProductEntity(
                                id = 0L,
                                name = name,
                                price = price,
                                costPrice = cost,
                                stock = stock
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onProductosChanged(products)
                }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Guarda o actualiza un usuario en Firestore.
     */
    suspend fun saveUsuarioInFirestore(context: Context, user: UserEntity) {
        try {
            val db = getFirestore(context)
            val docId = user.username.trim().lowercase()
            val data = hashMapOf<String, Any>(
                "username" to user.username,
                "fullName" to user.fullName,
                "pinCode" to user.pinCode,
                "passwordHash" to user.passwordHash,
                "role" to user.role,
                "isActive" to user.isActive,
                "createdAt" to user.createdAt
            )
            db.collection(COLLECTION_USUARIOS).document(docId)
                .set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso guardando usuario en Firestore: ${e.message}")
        }
    }

    /**
     * Escucha en tiempo real la colección de usuarios de Firestore.
     */
    fun observeUsuarios(
        context: Context,
        onUsuariosChanged: (List<UserEntity>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getFirestore(context)
            db.collection(COLLECTION_USUARIOS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val users = snapshot.documents.mapNotNull { doc ->
                        try {
                            val username = doc.getString("username") ?: return@mapNotNull null
                            val full = doc.getString("fullName") ?: username
                            val pin = doc.getString("pinCode") ?: ""
                            val hash = doc.getString("passwordHash") ?: ""
                            val role = doc.getString("role") ?: "RECEPCION"
                            val active = doc.getBoolean("isActive") ?: true
                            val created = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            UserEntity(
                                id = 0L,
                                username = username,
                                fullName = full,
                                pinCode = pin,
                                passwordHash = hash,
                                role = role,
                                isActive = active,
                                createdAt = created
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUsuariosChanged(users)
                }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina un insumo de Firestore.
     */
    suspend fun deleteInsumoInFirestore(context: Context, name: String) {
        try {
            val db = getFirestore(context)
            db.collection(COLLECTION_INSUMOS).document(name.trim().lowercase()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso eliminando insumo en Firestore: ${e.message}")
        }
    }

    /**
     * Elimina un producto de Firestore.
     */
    suspend fun deleteProductoInFirestore(context: Context, name: String) {
        try {
            val db = getFirestore(context)
            db.collection(COLLECTION_PRODUCTOS).document(name.trim().lowercase()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso eliminando producto en Firestore: ${e.message}")
        }
    }

    /**
     * Elimina un usuario de Firestore.
     */
    suspend fun deleteUsuarioInFirestore(context: Context, username: String) {
        try {
            val db = getFirestore(context)
            db.collection(COLLECTION_USUARIOS).document(username.trim().lowercase()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso eliminando usuario en Firestore: ${e.message}")
        }
    }
}
