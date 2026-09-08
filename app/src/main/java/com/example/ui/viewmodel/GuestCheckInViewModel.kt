package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.RoomDao
import com.example.data.database.HotelDatabase
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.model.Habitacion
import com.example.data.model.Room
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing guest check-ins and room status transitions in Room database and Firestore.
 */
class GuestCheckInViewModel @JvmOverloads constructor(
    application: Application,
    private val roomDao: RoomDao = HotelDatabase.getDatabase(application).roomDao()
) : AndroidViewModel(application) {

    // Status filter: "ALL", "DISPONIBLE", "OCUPADA", "PENDIENTE_LIMPIEZA"
    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Firestore real-time room list
    private val _firestoreRooms = MutableStateFlow<List<Room>>(emptyList())
    val firestoreRooms: StateFlow<List<Room>> = _firestoreRooms.asStateFlow()
    private var firestoreListener: ListenerRegistration? = null

    // Combined real-time mirror: Rooms from Room DB merged with Firestore
    val allRooms: StateFlow<List<RoomEntity>> = combine(roomDao.getAllRooms(), _firestoreRooms) { localRooms, fsRooms ->
        if (fsRooms.isEmpty()) {
            localRooms
        } else {
            val firestoreMap = fsRooms.associateBy { it.roomNumber.trim().lowercase() }
            val remoteNumbers = fsRooms.map { it.roomNumber.trim().lowercase() }.toSet()
            val mergedFromLocal = localRooms
                .filter { it.roomNumber.trim().lowercase() in remoteNumbers }
                .map { localRoom ->
                    val fsRoom = firestoreMap[localRoom.roomNumber.trim().lowercase()]
                    if (fsRoom != null) {
                        val convertedStatus = when (fsRoom.status.uppercase()) {
                            "DISPONIBLE", "AVAILABLE" -> RoomStatus.DISPONIBLE
                            "OCUPADA", "OCCUPIED" -> RoomStatus.OCUPADA
                            "LIMPIEZA", "CLEANING", "PENDIENTE_LIMPIEZA", "EN_LIMPIEZA" -> RoomStatus.PENDIENTE_LIMPIEZA
                            else -> localRoom.status
                        }
                        val isAvailableOrCleaning = convertedStatus == RoomStatus.DISPONIBLE || convertedStatus == RoomStatus.PENDIENTE_LIMPIEZA
                        val effectiveClientName = if (isAvailableOrCleaning) null else (fsRoom.clientName?.takeIf { it.isNotBlank() } ?: localRoom.clientName)
                        val effectiveClientDpi = if (isAvailableOrCleaning) null else (fsRoom.clientDpi?.takeIf { it.isNotBlank() } ?: localRoom.clientDpi)
                        val effectiveCheckIn = if (isAvailableOrCleaning) 0L else (if (fsRoom.checkInTimestamp > 0L) fsRoom.checkInTimestamp else localRoom.checkInTimeMillis)
                        val effectiveCheckOut = if (isAvailableOrCleaning) 0L else (if (fsRoom.checkOutTimestamp > 0L) fsRoom.checkOutTimestamp else localRoom.checkOutTimeMillis)

                        localRoom.copy(
                            status = convertedStatus,
                            nightlyRate = if (fsRoom.price > 0.0) fsRoom.price else localRoom.nightlyRate,
                            clientName = effectiveClientName,
                            clientDpi = effectiveClientDpi,
                            checkInTimeMillis = effectiveCheckIn,
                            checkOutTimeMillis = effectiveCheckOut,
                            notes = if (!fsRoom.notes.isNullOrBlank()) fsRoom.notes else localRoom.notes
                        )
                    } else {
                        localRoom
                    }
                }
            val localRoomNumbers = localRooms.map { it.roomNumber.trim().lowercase() }.toSet()
            val extraFromFirestore = fsRooms
                .filter { it.roomNumber.trim().lowercase() !in localRoomNumbers }
                .mapIndexed { idx, fs ->
                    val convertedStatus = when (fs.status.uppercase()) {
                        "DISPONIBLE", "AVAILABLE" -> RoomStatus.DISPONIBLE
                        "OCUPADA", "OCCUPIED" -> RoomStatus.OCUPADA
                        "LIMPIEZA", "CLEANING", "PENDIENTE_LIMPIEZA", "EN_LIMPIEZA" -> RoomStatus.PENDIENTE_LIMPIEZA
                        else -> RoomStatus.DISPONIBLE
                    }
                    RoomEntity(
                        id = (10000L + idx),
                        roomNumber = fs.roomNumber,
                        roomType = fs.roomType,
                        status = convertedStatus,
                        nightlyRate = fs.price,
                        clientName = fs.clientName,
                        clientDpi = fs.clientDpi,
                        checkInTimeMillis = if (fs.checkInTimestamp > 0L) fs.checkInTimestamp else 0L,
                        checkOutTimeMillis = if (fs.checkOutTimestamp > 0L) fs.checkOutTimestamp else 0L,
                        notes = fs.notes
                    )
                }
            mergedFromLocal + extraFromFirestore
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered rooms for grid view
    val roomsForGrid: StateFlow<List<RoomEntity>> = combine(allRooms, _selectedFilter) { rooms, filter ->
        when (filter) {
            "ALL" -> rooms
            "DISPONIBLE", "AVAILABLE" -> rooms.filter { it.isAvailable }
            "OCUPADA", "OCCUPIED" -> rooms.filter { it.isOccupied }
            "PENDIENTE_LIMPIEZA", "CLEANING" -> rooms.filter { it.isCleaning }
            else -> rooms
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Feedback messages for UI operations
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        iniciarSincronizacionFirestore()
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun iniciarSincronizacionFirestore() {
        try {
            firestoreListener?.remove()
            val db = com.example.utils.FirebaseManager.getFirestore(getApplication())
            firestoreListener = db.collection(com.example.utils.FirebaseManager.COLLECTION_HABITACIONES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("GuestCheckInVM", "Error en snapshotListener habitaciones: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val lista = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val numero = doc.getString("numero") ?: doc.getString("roomNumber") ?: doc.id
                                val estado = doc.getString("estado") ?: doc.getString("status") ?: "Disponible"
                                val precio = doc.getDouble("precio")
                                    ?: doc.getDouble("price")
                                    ?: (doc.get("precio") as? Number)?.toDouble()
                                    ?: 150.0
                                val tipo = doc.getString("roomType") ?: "Estándar"
                                val cliente = doc.getString("clientName")
                                val dpi = doc.getString("clientDpi")
                                val checkInTs = doc.getLong("checkInTimestamp") ?: 0L
                                val checkOutTs = doc.getLong("checkOutTimestamp") ?: 0L
                                val notas = doc.getString("notes")
                                Room(
                                    id = id,
                                    roomNumber = numero,
                                    status = estado,
                                    price = precio,
                                    roomType = tipo,
                                    clientName = cliente,
                                    clientDpi = dpi,
                                    checkInTimestamp = checkInTs,
                                    checkOutTimestamp = checkOutTs,
                                    notes = notas
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _firestoreRooms.value = lista
                        viewModelScope.launch(Dispatchers.IO) {
                            for (r in lista) {
                                try {
                                    val num = r.roomNumber.ifBlank { r.id }.trim()
                                    val existing = roomDao.getRoomByNumber(num)
                                    val mappedStatus = when {
                                        r.status.contains("Ocup", ignoreCase = true) || r.status == RoomStatus.OCUPADA -> RoomStatus.OCUPADA
                                        r.status.contains("Limp", ignoreCase = true) || r.status == RoomStatus.PENDIENTE_LIMPIEZA -> RoomStatus.PENDIENTE_LIMPIEZA
                                        else -> RoomStatus.DISPONIBLE
                                    }
                                    val isAvailableOrCleaning = mappedStatus == RoomStatus.DISPONIBLE || mappedStatus == RoomStatus.PENDIENTE_LIMPIEZA
                                    val effectiveClientName = if (isAvailableOrCleaning) null else r.clientName?.takeIf { it.isNotBlank() }
                                    val effectiveClientDpi = if (isAvailableOrCleaning) null else r.clientDpi?.takeIf { it.isNotBlank() }
                                    val effectiveCheckIn = if (isAvailableOrCleaning) 0L else (if (r.checkInTimestamp > 0) r.checkInTimestamp else 0L)
                                    val effectiveCheckOut = if (isAvailableOrCleaning) 0L else (if (r.checkOutTimestamp > 0) r.checkOutTimestamp else 0L)

                                    if (existing == null) {
                                        roomDao.insertRoom(
                                            RoomEntity(
                                                roomNumber = num,
                                                status = mappedStatus,
                                                nightlyRate = if (r.price > 0) r.price else 150.0,
                                                clientName = effectiveClientName,
                                                clientDpi = effectiveClientDpi,
                                                checkInTimeMillis = effectiveCheckIn,
                                                checkOutTimeMillis = effectiveCheckOut,
                                                notes = r.notes
                                            )
                                        )
                                    } else {
                                        val shouldUpdate = existing.status != mappedStatus ||
                                                existing.clientName != effectiveClientName ||
                                                existing.clientDpi != effectiveClientDpi ||
                                                existing.checkInTimeMillis != effectiveCheckIn ||
                                                existing.checkOutTimeMillis != effectiveCheckOut ||
                                                existing.notes != r.notes ||
                                                (r.price > 0 && existing.nightlyRate != r.price)

                                        if (shouldUpdate) {
                                            roomDao.updateRoom(
                                                existing.copy(
                                                    status = mappedStatus,
                                                    nightlyRate = if (r.price > 0) r.price else existing.nightlyRate,
                                                    clientName = effectiveClientName,
                                                    clientDpi = effectiveClientDpi,
                                                    checkInTimeMillis = effectiveCheckIn,
                                                    checkOutTimeMillis = effectiveCheckOut,
                                                    notes = r.notes
                                                )
                                            )
                                        }
                                    }
                                } catch (ex: Exception) {
                                    Log.w("GuestCheckInVM", "Aviso sincronizando habitación a RoomDao: ${ex.message}")
                                }
                            }

                            for (change in snapshot.documentChanges) {
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    val deletedDocId = change.document.id
                                    val roomNum = change.document.getString("numero") ?: change.document.getString("roomNumber") ?: deletedDocId
                                    val existing = roomDao.getRoomByNumber(roomNum.trim())
                                    if (existing != null) {
                                        roomDao.deleteRoom(existing)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("GuestCheckInVM", "Error al iniciar listener Firestore: ${e.message}", e)
        }
    }

    /**
     * Executes check-in, transitioning a room's status from 'Available' (DISPONIBLE) to 'Occupied' (OCUPADA)
     * and records check-in timestamp in Firestore as well as local Room database.
     */
    fun checkInGuest(
        roomId: Long,
        clientName: String,
        clientDpi: String = "",
        guestCount: Int = 1,
        rateName: String = "Estándar",
        priceCharged: Double = 0.0,
        contractedDurationMinutes: Long = 240L,
        receptionistName: String = "Recepción",
        notes: String? = null
    ) {
        viewModelScope.launch {
            val existingRoom = roomDao.getRoomById(roomId)
            if (existingRoom == null) {
                _userMessage.value = "Error: La habitación no existe."
                return@launch
            }

            if (!existingRoom.isAvailable) {
                _userMessage.value = "La Habitación ${existingRoom.roomNumber} ya está ocupada o en limpieza."
                return@launch
            }

            val now = System.currentTimeMillis()
            val checkOutTime = now + (contractedDurationMinutes * 60 * 1000)

            val updatedRoom = existingRoom.copy(
                status = RoomStatus.OCUPADA, // Transition status from Available to Occupied
                clientName = clientName,
                clientDpi = clientDpi,
                guestCount = guestCount,
                rateName = rateName,
                priceCharged = if (priceCharged > 0.0) priceCharged else existingRoom.nightlyRate,
                checkInTimeMillis = now,
                checkOutTimeMillis = checkOutTime,
                contractedDurationMinutes = contractedDurationMinutes,
                receptionistName = receptionistName,
                notes = notes
            )

            roomDao.updateRoom(updatedRoom)

            // Registro en Firestore con timestamp de check-in
            try {
                val firestoreData = hashMapOf<String, Any>(
                    "numero" to existingRoom.roomNumber,
                    "roomNumber" to existingRoom.roomNumber,
                    "estado" to "Ocupada",
                    "status" to "Occupied",
                    "clientName" to clientName,
                    "clientDpi" to clientDpi,
                    "guestCount" to guestCount,
                    "rateName" to rateName,
                    "price" to (if (priceCharged > 0.0) priceCharged else existingRoom.nightlyRate),
                    "checkInTimestamp" to now,
                    "checkOutTimestamp" to checkOutTime,
                    "receptionistName" to receptionistName,
                    "notes" to (notes ?: "")
                )
                Firebase.firestore.collection("habitaciones").document(existingRoom.roomNumber)
                    .set(firestoreData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("GuestCheckInVM", "Check-in registrado en Firestore para Habitación ${existingRoom.roomNumber}")
                    }
                    .addOnFailureListener { e ->
                        Log.w("GuestCheckInVM", "Error al actualizar check-in en Firestore: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("GuestCheckInVM", "Excepción al registrar check-in en Firestore: ${e.message}", e)
            }

            _userMessage.value = "¡Check-In exitoso en Habitación ${existingRoom.roomNumber}! Estado actualizado a Ocupada."
        }
    }

    /**
     * Executes check-out, transitioning room status from 'Occupied' to 'Cleaning'.
     */
    fun checkOutGuest(roomId: Long) {
        viewModelScope.launch {
            val room = roomDao.getRoomById(roomId) ?: return@launch
            val updated = room.copy(
                status = RoomStatus.PENDIENTE_LIMPIEZA,
                cleaningStartTimeMillis = System.currentTimeMillis()
            )
            roomDao.updateRoom(updated)

            try {
                Firebase.firestore.collection("habitaciones").document(room.roomNumber)
                    .set(
                        mapOf(
                            "estado" to "Limpieza",
                            "status" to "Cleaning"
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.w("GuestCheckInVM", "Error al actualizar salida en Firestore: ${e.message}")
            }

            _userMessage.value = "Check-Out registrado para Habitación ${room.roomNumber}. Pasa a Limpieza."
        }
    }

    /**
     * Marks cleaning completed, transitioning room back to 'Available'.
     */
    fun completeCleaning(roomId: Long, finishedBy: String = "Personal de Limpieza") {
        viewModelScope.launch {
            val room = roomDao.getRoomById(roomId) ?: return@launch
            val updated = room.copy(
                status = RoomStatus.DISPONIBLE,
                clientName = null,
                clientDpi = null,
                guestCount = 1,
                cleaningFinishedBy = finishedBy
            )
            roomDao.updateRoom(updated)

            try {
                Firebase.firestore.collection("habitaciones").document(room.roomNumber)
                    .set(
                        mapOf(
                            "estado" to "Disponible",
                            "status" to "Available",
                            "clientName" to "",
                            "clientDpi" to ""
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.w("GuestCheckInVM", "Error al actualizar limpieza en Firestore: ${e.message}")
            }

            _userMessage.value = "Habitación ${room.roomNumber} limpia y disponible nuevamente."
        }
    }

    /**
     * Adds a new room to the database and Firestore.
     */
    fun addNewRoom(roomNumber: String, roomType: String = "Estándar", nightlyRate: Double = 150.0) {
        viewModelScope.launch {
            val newRoom = RoomEntity(
                roomNumber = roomNumber,
                roomType = roomType,
                status = RoomStatus.DISPONIBLE,
                nightlyRate = nightlyRate,
                sortOrder = (allRooms.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            )
            roomDao.insertRoom(newRoom)

            try {
                val firestoreData = hashMapOf<String, Any>(
                    "numero" to roomNumber,
                    "roomNumber" to roomNumber,
                    "estado" to "Disponible",
                    "status" to "Available",
                    "precio" to nightlyRate,
                    "price" to nightlyRate,
                    "roomType" to roomType
                )
                Firebase.firestore.collection("habitaciones").document(roomNumber)
                    .set(firestoreData, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("GuestCheckInVM", "Error al agregar habitación en Firestore: ${e.message}")
            }

            _userMessage.value = "Habitación $roomNumber agregada con éxito."
        }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
