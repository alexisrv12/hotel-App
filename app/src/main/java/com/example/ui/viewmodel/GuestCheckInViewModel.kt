package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.RoomDao
import com.example.data.database.HotelDatabase
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.repository.HotelFirestoreRepository
import com.example.data.repository.SessionDataStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing guest check-ins and room status transitions in Room database with Firestore real-time sync.
 */
class GuestCheckInViewModel @JvmOverloads constructor(
    application: Application,
    private val roomDao: RoomDao = HotelDatabase.getDatabase(application).roomDao()
) : AndroidViewModel(application) {

    private val db = HotelDatabase.getDatabase(application)
    private val sessionRepo = SessionDataStoreRepository(application)
    private val firestoreRepo = HotelFirestoreRepository.getInstance(
        application.applicationContext,
        db.hotelDao(),
        db.deviceDao(),
        sessionRepo
    )

    // Status filter: "ALL", "DISPONIBLE", "OCUPADA", "PENDIENTE_LIMPIEZA"
    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // All rooms Flow directly from Firestore in real-time
    val allRooms: StateFlow<List<RoomEntity>> = firestoreRepo.getRoomsFlow().stateIn(
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

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    /**
     * Executes check-in, transitioning a room's status from 'Available' (DISPONIBLE) to 'Occupied' (OCUPADA) and syncing in real-time.
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
            firestoreRepo.syncRoomUpdate(updatedRoom)
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
            firestoreRepo.syncRoomUpdate(updated)
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
            firestoreRepo.syncRoomUpdate(updated)
            _userMessage.value = "Habitación ${room.roomNumber} limpia y disponible nuevamente."
        }
    }

    /**
     * Adds a new room to the database and syncs to Firestore.
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
            firestoreRepo.syncRoomUpdate(newRoom)
            _userMessage.value = "Habitación $roomNumber agregada con éxito."
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
