package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.HotelDatabase
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.StayHistoryEntity
import com.example.data.entities.TimeRateEntity
import com.example.data.repository.HotelRepository
import com.example.utils.HotelNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BookingBillingBreakdown(
    val basePrice: Double = 0.0,
    val extraGuestsCharge: Double = 0.0,
    val amenitiesCharge: Double = 0.0,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0, // 12% IVA
    val totalAmount: Double = 0.0
)

data class BookingFormState(
    val guestName: String = "",
    val guestDpi: String = "",
    val guestPhone: String = "",
    val guestCount: Int = 1,
    val selectedRoomId: Long? = null,
    val selectedRateId: Long? = null,
    val customHours: Int = 4,
    val selectedPaymentMethod: String = "Efectivo",
    val discountPercent: Double = 0.0,
    val includeBreakfast: Boolean = false,
    val includeLateCheckout: Boolean = false,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false,
    val errorMessage: String? = null
)

class RoomBookingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = HotelDatabase.getDatabase(application)
    private val repository = HotelRepository(db.hotelDao())

    val availableRooms: StateFlow<List<RoomEntity>> = repository.allRooms
        .combine(MutableStateFlow(Unit)) { rooms, _ ->
            rooms.filter { it.status == RoomStatus.DISPONIBLE }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val timeRates: StateFlow<List<TimeRateEntity>> = repository.activeTimeRates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _formState = MutableStateFlow(BookingFormState())
    val formState: StateFlow<BookingFormState> = _formState.asStateFlow()

    private val _billingBreakdown = MutableStateFlow(BookingBillingBreakdown())
    val billingBreakdown: StateFlow<BookingBillingBreakdown> = _billingBreakdown.asStateFlow()

    fun updateGuestName(name: String) {
        _formState.value = _formState.value.copy(guestName = name, errorMessage = null)
    }

    fun updateGuestDpi(dpi: String) {
        _formState.value = _formState.value.copy(guestDpi = dpi)
    }

    fun updateGuestPhone(phone: String) {
        _formState.value = _formState.value.copy(guestPhone = phone)
    }

    fun updateGuestCount(count: Int) {
        val validCount = count.coerceIn(1, 6)
        _formState.value = _formState.value.copy(guestCount = validCount)
        recalculateBilling()
    }

    fun selectRoom(roomId: Long) {
        _formState.value = _formState.value.copy(selectedRoomId = roomId, errorMessage = null)
        recalculateBilling()
    }

    fun selectRate(rateId: Long) {
        _formState.value = _formState.value.copy(selectedRateId = rateId)
        recalculateBilling()
    }

    fun updateCustomHours(hours: Int) {
        _formState.value = _formState.value.copy(customHours = hours.coerceIn(1, 72))
        recalculateBilling()
    }

    fun updatePaymentMethod(method: String) {
        _formState.value = _formState.value.copy(selectedPaymentMethod = method)
    }

    fun updateDiscountPercent(discount: Double) {
        _formState.value = _formState.value.copy(discountPercent = discount.coerceIn(0.0, 100.0))
        recalculateBilling()
    }

    fun toggleBreakfast(included: Boolean) {
        _formState.value = _formState.value.copy(includeBreakfast = included)
        recalculateBilling()
    }

    fun toggleLateCheckout(included: Boolean) {
        _formState.value = _formState.value.copy(includeLateCheckout = included)
        recalculateBilling()
    }

    fun updateNotes(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    private fun recalculateBilling() {
        val state = _formState.value
        val rates = timeRates.value
        val rooms = availableRooms.value

        val selectedRate = rates.find { it.id == state.selectedRateId }
        val selectedRoom = rooms.find { it.id == state.selectedRoomId }

        // Base price calculation
        val basePrice = when {
            selectedRate != null -> selectedRate.price
            selectedRoom != null -> {
                // Approximate fallback rate by room type
                when (selectedRoom.roomType.uppercase()) {
                    "SUITE", "MASTER_SUITE" -> 45.0 * state.customHours
                    "DOBLE", "DOUBLE" -> 30.0 * state.customHours
                    else -> 20.0 * state.customHours
                }
            }
            else -> 100.0
        }

        // Extra guests charge ($10 per guest beyond 2)
        val extraGuests = (state.guestCount - 2).coerceAtLeast(0)
        val extraGuestCharge = extraGuests * 15.0

        // Amenities charge
        var amenitiesCharge = 0.0
        if (state.includeBreakfast) amenitiesCharge += (state.guestCount * 8.0)
        if (state.includeLateCheckout) amenitiesCharge += 20.0

        val subtotal = basePrice + extraGuestCharge + amenitiesCharge
        val discountAmount = subtotal * (state.discountPercent / 100.0)
        val afterDiscount = (subtotal - discountAmount).coerceAtLeast(0.0)
        val taxAmount = afterDiscount * 0.12 // 12% IVA
        val totalAmount = afterDiscount

        _billingBreakdown.value = BookingBillingBreakdown(
            basePrice = basePrice,
            extraGuestsCharge = extraGuestCharge,
            amenitiesCharge = amenitiesCharge,
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount
        )
    }

    fun confirmBookingAndCheckIn(
        receptionistName: String = "Recepción",
        onSuccess: (Long) -> Unit = {}
    ) {
        val state = _formState.value
        if (state.guestName.isBlank()) {
            _formState.value = state.copy(errorMessage = "Por favor ingrese el nombre del huésped")
            return
        }
        val roomId = state.selectedRoomId
        if (roomId == null) {
            _formState.value = state.copy(errorMessage = "Seleccione una habitación disponible")
            return
        }

        _formState.value = state.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val room = db.roomDao().getRoomById(roomId) ?: return@launch
                val rates = timeRates.value
                val selectedRate = rates.find { it.id == state.selectedRateId }
                    ?: TimeRateEntity(
                        name = "${state.customHours} Horas",
                        durationMinutes = state.customHours * 60L,
                        price = _billingBreakdown.value.basePrice
                    )

                val billing = _billingBreakdown.value
                val now = System.currentTimeMillis()
                val durationMinutes = selectedRate.durationMinutes
                val checkOutTimeMillis = now + (durationMinutes * 60 * 1000L)

                // 1. Check in Room locally
                repository.checkInRoom(
                    roomId = roomId,
                    clientName = state.guestName.trim(),
                    clientDpi = state.guestDpi.trim().ifEmpty { null },
                    guestCount = state.guestCount,
                    rate = selectedRate.copy(price = billing.totalAmount),
                    notes = state.notes.ifEmpty { null },
                    receptionistName = receptionistName
                )

                // 2. Persist invoice record locally
                val invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))
                val invoice = InvoiceEntity(
                    invoiceNumber = invoiceNumber,
                    hotelName = "Hotel Rivera",
                    hotelAddress = "Av. Principal #102",
                    hotelPhone = "+502 5555-0199",
                    hotelNit = "1029384-5",
                    dateString = dateStr,
                    timeString = timeStr,
                    roomNumber = room.roomNumber,
                    clientName = state.guestName.trim(),
                    contractedTime = "${durationMinutes / 60} horas",
                    checkInTime = timeStr,
                    checkOutTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(checkOutTimeMillis)),
                    price = billing.subtotal,
                    discount = billing.discountAmount,
                    totalAmount = billing.totalAmount,
                    paymentMethod = state.selectedPaymentMethod,
                    receptionistName = receptionistName,
                    timestampMillis = now
                )
                db.hotelDao().insertInvoice(invoice)

                // 3. Persist transaction to Firestore
                persistBookingToFirestore(
                    roomNumber = room.roomNumber,
                    guestName = state.guestName.trim(),
                    guestDpi = state.guestDpi.trim(),
                    guestCount = state.guestCount,
                    durationMinutes = durationMinutes,
                    billing = billing,
                    paymentMethod = state.selectedPaymentMethod,
                    receptionistName = receptionistName,
                    notes = state.notes
                )

                // 4. Trigger Notification via HotelNotificationHelper
                HotelNotificationHelper.sendGuestCheckInAlert(
                    context = getApplication(),
                    roomNumber = room.roomNumber,
                    guestName = state.guestName.trim(),
                    durationHours = (durationMinutes / 60).toInt().coerceAtLeast(1),
                    totalAmount = billing.totalAmount
                )

                _formState.value = state.copy(
                    isSubmitting = false,
                    submissionSuccess = true
                )
                onSuccess(roomId)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Error al registrar hospedaje: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun persistBookingToFirestore(
        roomNumber: String,
        guestName: String,
        guestDpi: String,
        guestCount: Int,
        durationMinutes: Long,
        billing: BookingBillingBreakdown,
        paymentMethod: String,
        receptionistName: String,
        notes: String
    ) {
        try {
            if (FirebaseApp.getApps(getApplication()).isNotEmpty()) {
                val dbCloud = FirebaseFirestore.getInstance()
                val bookingData = hashMapOf(
                    "bookingId" to UUID.randomUUID().toString(),
                    "roomNumber" to roomNumber,
                    "clientName" to guestName,
                    "clientDpi" to guestDpi,
                    "guestCount" to guestCount,
                    "contractedDurationMinutes" to durationMinutes,
                    "basePrice" to billing.basePrice,
                    "extraGuestsCharge" to billing.extraGuestsCharge,
                    "amenitiesCharge" to billing.amenitiesCharge,
                    "discountAmount" to billing.discountAmount,
                    "taxAmount" to billing.taxAmount,
                    "priceCharged" to billing.totalAmount,
                    "paymentMethod" to paymentMethod,
                    "receptionistName" to receptionistName,
                    "notes" to notes,
                    "checkInTimestamp" to System.currentTimeMillis(),
                    "status" to "OCUPADA"
                )

                // Save to active_stays and billing collections
                val hotelDoc = dbCloud.collection("hotels").document("hotel_rivera_main")
                hotelDoc.collection("active_stays").document("room_$roomNumber").set(bookingData)
                hotelDoc.collection("billing_records").add(bookingData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetForm() {
        _formState.value = BookingFormState()
        _billingBreakdown.value = BookingBillingBreakdown()
    }
}
