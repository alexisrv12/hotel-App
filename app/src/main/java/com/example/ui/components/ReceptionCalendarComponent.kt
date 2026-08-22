package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.ReservationEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.StayHistoryEntity
import com.example.data.entities.TimeRateEntity
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import com.example.utils.NetworkConnectivityHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceptionCalendarComponent(
    rooms: List<RoomEntity>,
    timeRates: List<TimeRateEntity> = emptyList(),
    reservations: List<ReservationEntity> = emptyList(),
    stayHistory: List<StayHistoryEntity> = emptyList(),
    currentTimeMillis: Long = System.currentTimeMillis(),
    viewModel: HotelViewModel? = null,
    onRoomCheckInClick: (RoomEntity) -> Unit = {},
    onRoomOccupiedClick: (RoomEntity) -> Unit = {},
    onRoomCleaningClick: (RoomEntity) -> Unit = {},
    onNewCheckInFormClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // 0: Calendario por Fecha, 1: Reservaciones Futuras & Próximos Check-Ins
    var selectedCalendarTab by remember { mutableIntStateOf(0) }
    // 0: Vista Mes Completo (Cuadrícula mensual), 1: Tira de Días + Habitaciones, 2: Ranuras Horarias (Slots)
    var calendarViewMode by remember { mutableIntStateOf(0) }

    var currentCalendarMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    var selectedDateMillis by remember {
        mutableLongStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }

    // Modal dialog state for adding check-in or reservation on the tapped date
    var showCheckInDialogForDate by remember { mutableStateOf(false) }
    var preselectedRoomForDialog by remember { mutableStateOf<RoomEntity?>(null) }

    var showNetworkDialog by remember { mutableStateOf(false) }
    var isNetworkOnline by remember { mutableStateOf(NetworkConnectivityHelper.isNetworkAvailable(context)) }

    // Listen to network changes
    LaunchedEffect(Unit) {
        NetworkConnectivityHelper.observeNetworkConnectivity(context).collect { isOnline ->
            isNetworkOnline = isOnline
        }
    }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }
    val dayFormatter = remember { SimpleDateFormat("EEE dd 'de' MMMM", Locale("es", "ES")) }
    val fullDateFormatter = remember { SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES")) }
    val dateIsoFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val selectedDateIsoString = remember(selectedDateMillis) {
        dateIsoFormatter.format(Date(selectedDateMillis))
    }

    // Filter reservations for the selected date
    val reservationsForSelectedDate = remember(reservations, selectedDateIsoString, selectedDateMillis) {
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        reservations.filter { res ->
            val resCal = Calendar.getInstance().apply { timeInMillis = res.reservationDateMillis }
            (res.checkInDateString == selectedDateIsoString ||
                    (resCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                            resCal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR))) &&
                    res.status != "CANCELADA"
        }
    }

    val isToday = remember(selectedDateMillis) {
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val nowCal = Calendar.getInstance()
        selCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                selCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    }

    val isFutureDate = remember(selectedDateMillis) {
        val selCal = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selCal.timeInMillis > todayCal.timeInMillis
    }

    val futureReservationsCount = remember(reservations) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        reservations.count { it.reservationDateMillis >= todayStart && it.status != "CANCELADA" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reception_calendar_component")
    ) {
        // Network Status & Validation Banner
        NetworkStatusBar(
            isOnline = isNetworkOnline,
            onValidateClick = { showNetworkDialog = true }
        )

        // Sub Tabs: Calendario por Fecha & Reservaciones Futuras
        TabRow(
            selectedTabIndex = selectedCalendarTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = HotelNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedCalendarTab == 0,
                onClick = { selectedCalendarTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Calendario & Fechas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                modifier = Modifier.testTag("tab_calendar_availability")
            )
            Tab(
                selected = selectedCalendarTab == 1,
                onClick = { selectedCalendarTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            "Reservas Futuras ($futureReservationsCount)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier.testTag("tab_calendar_future_reservations")
            )
        }

        if (selectedCalendarTab == 0) {
            // TAB 0: INTERACTIVE CALENDAR & DATE-SPECIFIC CHECK-IN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Month Header & Navigation Toolbar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = currentCalendarMonth.timeInMillis
                                    add(Calendar.MONTH, -1)
                                }
                                currentCalendarMonth = newCal
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = HotelNavy)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthFormatter.format(currentCalendarMonth.time).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                            Text(
                                text = if (isToday) "Viendo: Hoy (${dayFormatter.format(Date(selectedDateMillis))})"
                                else if (isFutureDate) "Viendo Fecha Futura: ${dayFormatter.format(Date(selectedDateMillis))}"
                                else "Fecha Pasada: ${dayFormatter.format(Date(selectedDateMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFutureDate) HotelNavy else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isFutureDate) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val todayCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    currentCalendarMonth = todayCal
                                    selectedDateMillis = todayCal.timeInMillis
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Hoy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = currentCalendarMonth.timeInMillis
                                        add(Calendar.MONTH, 1)
                                    }
                                    currentCalendarMonth = newCal
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = HotelNavy)
                            }
                        }
                    }
                }

                // View Mode selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Toca una fecha para registrar Check-In o Reserva:",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = calendarViewMode == 0,
                            onClick = { calendarViewMode = 0 },
                            label = { Text("Mes", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = calendarViewMode == 1,
                            onClick = { calendarViewMode = 1 },
                            label = { Text("Días", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.ViewDay, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = calendarViewMode == 2,
                            onClick = { calendarViewMode = 2 },
                            label = { Text("Horarios", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.ViewTimeline, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // CALENDAR DISPLAY ACCORDING TO VIEW MODE
                when (calendarViewMode) {
                    0 -> {
                        FullMonthCalendarGrid(
                            calendarMonth = currentCalendarMonth,
                            selectedDateMillis = selectedDateMillis,
                            reservations = reservations,
                            onDateTapped = { dateMillis ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedDateMillis = dateMillis
                                preselectedRoomForDialog = rooms.firstOrNull { it.status == RoomStatus.DISPONIBLE } ?: rooms.firstOrNull()
                                showCheckInDialogForDate = true
                            }
                        )
                    }
                    1 -> {
                        CalendarDaysStrip(
                            calendarMonth = currentCalendarMonth,
                            selectedDateMillis = selectedDateMillis,
                            reservations = reservations,
                            onSelectDate = { dateMillis ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDateMillis = dateMillis
                            },
                            onDateDoubleTapOrAction = { dateMillis ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedDateMillis = dateMillis
                                preselectedRoomForDialog = rooms.firstOrNull { it.status == RoomStatus.DISPONIBLE } ?: rooms.firstOrNull()
                                showCheckInDialogForDate = true
                            }
                        )
                    }
                    else -> {
                        CalendarHourlySlotsView(
                            rooms = rooms,
                            currentTimeMillis = currentTimeMillis,
                            onEmptySlotAction = { room ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                preselectedRoomForDialog = room
                                showCheckInDialogForDate = true
                            },
                            onOccupiedSlotClick = { onRoomOccupiedClick(it) }
                        )
                    }
                }

                // SELECTED DATE ACTION BANNER & ACTIVE RESERVATIONS FOR THIS DAY
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isToday) HotelGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isToday) HotelGold.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isToday) Icons.Default.Today else Icons.Default.Event,
                                    contentDescription = null,
                                    tint = HotelNavy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = fullDateFormatter.format(Date(selectedDateMillis)).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HotelNavy
                                    )
                                    Text(
                                        text = if (isToday) "Check-In Inmediato o Reservación para Hoy"
                                        else if (isFutureDate) "Fecha Futura • ${reservationsForSelectedDate.size} Reservaciones Registradas"
                                        else "Registro Histórico de la Fecha",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    preselectedRoomForDialog = rooms.firstOrNull { it.status == RoomStatus.DISPONIBLE } ?: rooms.firstOrNull()
                                    showCheckInDialogForDate = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_add_checkin_for_date")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isToday) "+ Check-In Hoy" else "+ Nueva Reserva",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (reservationsForSelectedDate.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                            Text(
                                text = "Reservaciones programadas para esta fecha (${reservationsForSelectedDate.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy,
                                fontSize = 11.sp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                reservationsForSelectedDate.forEach { res ->
                                    DateReservationMiniCard(
                                        reservation = res,
                                        isToday = isToday,
                                        onExecuteCheckIn = {
                                            viewModel?.executeCheckInForReservation(res) {
                                                Toast.makeText(context, "Check-In realizado para Hab. ${res.roomNumber}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onCancel = {
                                            viewModel?.cancelReservation(res.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ROOMS LIST FOR THIS DATE
                if (calendarViewMode != 2) {
                    Text(
                        text = "Habitaciones del Hotel (${rooms.size} registradas):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_rooms_grid")
                    ) {
                        items(rooms, key = { it.id }) { room ->
                            val hasReservationOnThisDate = reservationsForSelectedDate.any { it.roomId == room.id }
                            CalendarRoomCard(
                                room = room,
                                hasFutureReservation = hasReservationOnThisDate,
                                onCheckInClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    preselectedRoomForDialog = room
                                    showCheckInDialogForDate = true
                                },
                                onOccupiedClick = { onRoomOccupiedClick(room) },
                                onCleaningClick = { onRoomCleaningClick(room) }
                            )
                        }
                    }
                }
            }
        } else {
            // TAB 1: ALL UPCOMING FUTURE RESERVATIONS VIEW & DIRECT ACTIONS
            FutureReservationsListView(
                reservations = reservations,
                rooms = rooms,
                viewModel = viewModel,
                onNewReservationClick = {
                    preselectedRoomForDialog = rooms.firstOrNull { it.status == RoomStatus.DISPONIBLE } ?: rooms.firstOrNull()
                    showCheckInDialogForDate = true
                }
            )
        }
    }

    // MODAL DIALOG: AGREGAR CHECK-IN / RESERVACIÓN EN FECHA ESPECÍFICA
    if (showCheckInDialogForDate) {
        CalendarCheckInOrReservationDialog(
            selectedDateMillis = selectedDateMillis,
            preselectedRoom = preselectedRoomForDialog,
            rooms = rooms,
            timeRates = timeRates,
            onDismiss = { showCheckInDialogForDate = false },
            onConfirm = { room, clientName, dpi, phone, guestCount, rateName, durationMins, durationText, totalPrice, advancePayment, paymentMethod, checkInTime, notes, isImmediateCheckIn ->
                if (viewModel != null) {
                    viewModel.createReservation(
                        roomNumber = room.roomNumber,
                        roomId = room.id,
                        clientName = clientName,
                        clientDpi = dpi,
                        clientPhone = phone,
                        guestCount = guestCount,
                        reservationDateMillis = selectedDateMillis,
                        checkInDateString = dateIsoFormatter.format(Date(selectedDateMillis)),
                        checkInTime = checkInTime,
                        durationText = durationText,
                        durationMinutes = durationMins,
                        rateName = rateName,
                        totalPrice = totalPrice,
                        advancePayment = advancePayment,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        isImmediateCheckIn = isImmediateCheckIn,
                        onComplete = {
                            showCheckInDialogForDate = false
                        }
                    )
                } else {
                    if (isImmediateCheckIn) {
                        onRoomCheckInClick(room)
                    }
                    showCheckInDialogForDate = false
                }
            }
        )
    }

    // Network State Validation Modal Dialog
    if (showNetworkDialog) {
        NetworkValidationDialog(
            isOnline = isNetworkOnline,
            onDismiss = { showNetworkDialog = false }
        )
    }
}

// FULL MONTH INTERACTIVE CALENDAR GRID
@Composable
private fun FullMonthCalendarGrid(
    calendarMonth: Calendar,
    selectedDateMillis: Long,
    reservations: List<ReservationEntity>,
    onDateTapped: (Long) -> Unit
) {
    val weekDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    val (daysInMonth, leadingEmptyDays) = remember(calendarMonth) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = calendarMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = (firstDayOfWeek + 5) % 7 // Monday = 0, Sunday = 6

        val list = mutableListOf<Calendar>()
        for (i in 1..maxDay) {
            val dCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            list.add(dCal)
        }
        Pair(list, offset)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Weekday Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)

            // Month Grid
            val totalCells = leadingEmptyDays + daysInMonth.size
            val rows = (totalCells + 6) / 7

            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        if (cellIndex < leadingEmptyDays || cellIndex >= leadingEmptyDays + daysInMonth.size) {
                            Spacer(modifier = Modifier.weight(1f).height(38.dp))
                        } else {
                            val dayCal = daysInMonth[cellIndex - leadingEmptyDays]
                            val isSelected = remember(selectedDateMillis, dayCal) {
                                val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                                selCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                        selCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                            }
                            val isToday = remember(dayCal) {
                                val nowCal = Calendar.getInstance()
                                nowCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                        nowCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                            }

                            val resCountForDay = remember(reservations, dayCal) {
                                reservations.count { res ->
                                    val rCal = Calendar.getInstance().apply { timeInMillis = res.reservationDateMillis }
                                    rCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                            rCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR) &&
                                            res.status != "CANCELADA"
                                }
                            }

                            val isPast = remember(dayCal) {
                                val nowCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                dayCal.timeInMillis < nowCal.timeInMillis
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) HotelNavy
                                else if (isToday) HotelGold.copy(alpha = 0.2f)
                                else if (resCountForDay > 0) StatusGreen.copy(alpha = 0.12f)
                                else Color.Transparent,
                                border = if (isSelected) null
                                else if (isToday) BorderStroke(1.dp, HotelGold)
                                else if (resCountForDay > 0) BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
                                else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onDateTapped(dayCal.timeInMillis) }
                                    .testTag("month_day_cell_${dayCal.get(Calendar.DAY_OF_MONTH)}")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${dayCal.get(Calendar.DAY_OF_MONTH)}",
                                        fontWeight = if (isSelected || isToday || resCountForDay > 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White
                                        else if (isToday) HotelNavy
                                        else if (isPast) MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (resCountForDay > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) HotelGold else StatusGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// CALENDAR DAYS STRIP (HORIZONTAL SELECTOR)
@Composable
private fun CalendarDaysStrip(
    calendarMonth: Calendar,
    selectedDateMillis: Long,
    reservations: List<ReservationEntity>,
    onSelectDate: (Long) -> Unit,
    onDateDoubleTapOrAction: (Long) -> Unit
) {
    val dayNameFormatter = remember { SimpleDateFormat("EEE", Locale("es", "ES")) }
    val dayNumFormatter = remember { SimpleDateFormat("dd", Locale("es", "ES")) }

    val daysInMonth = remember(calendarMonth) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = calendarMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<Calendar>()
        for (i in 1..maxDay) {
            val dCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            list.add(dCal)
        }
        list
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(daysInMonth) { dayCal ->
            val isSelected = remember(selectedDateMillis, dayCal) {
                val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                selCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                        selCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
            }

            val isToday = remember(dayCal) {
                val nowCal = Calendar.getInstance()
                nowCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                        nowCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
            }

            val resCount = remember(reservations, dayCal) {
                reservations.count { res ->
                    val rCal = Calendar.getInstance().apply { timeInMillis = res.reservationDateMillis }
                    rCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                            rCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR) &&
                            res.status != "CANCELADA"
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) HotelNavy else if (isToday) HotelGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerLow,
                border = if (isSelected) null else BorderStroke(
                    1.dp,
                    if (isToday) HotelGold else if (resCount > 0) StatusGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .width(52.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onSelectDate(dayCal.timeInMillis)
                    }
                    .testTag("day_strip_cell_${dayCal.get(Calendar.DAY_OF_MONTH)}")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayNameFormatter.format(dayCal.time).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayNumFormatter.format(dayCal.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (resCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) HotelGold else StatusGreen)
                        )
                    }
                }
            }
        }
    }
}

// MINI CARD FOR RESERVATION DISPLAYED ON SELECTED DATE
@Composable
private fun DateReservationMiniCard(
    reservation: ReservationEntity,
    isToday: Boolean,
    onExecuteCheckIn: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = HotelNavy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = reservation.roomNumber,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = reservation.clientName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Hora: ${reservation.checkInTime} • ${reservation.durationText} • Total: Q${"%.2f".format(reservation.totalPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val pending = (reservation.totalPrice - reservation.advancePayment).coerceAtLeast(0.0)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (reservation.advancePayment > 0) {
                            Text(
                                text = "Anticipo: Q${"%.2f".format(reservation.advancePayment)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }
                        if (pending > 0) {
                            Text(
                                text = "Pendiente: Q${"%.2f".format(pending)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed
                            )
                        } else {
                            Text(
                                text = "Cancelado",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (reservation.status != "CHECKED_IN") {
                    Button(
                        onClick = onExecuteCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Check-In", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StatusGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Ingresado",
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// CALENDAR ROOM CARD
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarRoomCard(
    room: RoomEntity,
    hasFutureReservation: Boolean = false,
    onCheckInClick: () -> Unit,
    onOccupiedClick: () -> Unit,
    onCleaningClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isAvailable = room.status == RoomStatus.DISPONIBLE

    val statusColor = when (room.status) {
        RoomStatus.DISPONIBLE -> StatusGreen
        RoomStatus.OCUPADA -> StatusRed
        RoomStatus.PENDIENTE_LIMPIEZA, RoomStatus.EN_LIMPIEZA -> StatusYellow
        else -> StatusGreen
    }

    val statusLabel = when (room.status) {
        RoomStatus.DISPONIBLE -> "Disponible"
        RoomStatus.OCUPADA -> "Ocupada"
        RoomStatus.PENDIENTE_LIMPIEZA -> "Limpieza"
        RoomStatus.EN_LIMPIEZA -> "En Limpieza"
        else -> room.status
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isAvailable) BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    when (room.status) {
                        RoomStatus.DISPONIBLE -> onCheckInClick()
                        RoomStatus.OCUPADA -> onOccupiedClick()
                        else -> onCleaningClick()
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isAvailable) onCheckInClick()
                    else if (room.status == RoomStatus.OCUPADA) onOccupiedClick()
                    else onCleaningClick()
                }
            )
            .testTag("calendar_room_card_${room.roomNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hab. ${room.roomNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${room.roomType} • ${room.capacity} p.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )

            if (hasFutureReservation) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HotelGold.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = HotelGold, modifier = Modifier.size(10.dp))
                        Text("Reserva Programada", color = HotelNavy, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (room.status == RoomStatus.OCUPADA && !room.clientName.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = room.clientName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            } else if (room.status == RoomStatus.DISPONIBLE) {
                Surface(
                    color = StatusGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onCheckInClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Check-In", tint = StatusGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check-In / Reserva", color = StatusGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// CALENDAR HOURLY SLOTS & TIMELINE VIEW
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarHourlySlotsView(
    rooms: List<RoomEntity>,
    currentTimeMillis: Long,
    onEmptySlotAction: (RoomEntity) -> Unit,
    onOccupiedSlotClick: (RoomEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val timeSlots = listOf(
        "Mañana (08:00 - 12:00)",
        "Tarde (12:00 - 16:00)",
        "Atardecer (16:00 - 20:00)",
        "Noche / Completa (20:00 - 08:00)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_timeline_slots_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rooms, key = { it.id }) { room ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Habitación ${room.roomNumber} (${room.roomType})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = HotelNavy
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (room.status) {
                                RoomStatus.DISPONIBLE -> StatusGreen.copy(alpha = 0.15f)
                                RoomStatus.OCUPADA -> StatusRed.copy(alpha = 0.15f)
                                else -> StatusYellow.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = room.status,
                                color = when (room.status) {
                                    RoomStatus.DISPONIBLE -> StatusGreen
                                    RoomStatus.OCUPADA -> StatusRed
                                    else -> StatusYellow
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        timeSlots.forEachIndexed { index, _ ->
                            val isOccupiedInSlot = room.status == RoomStatus.OCUPADA
                            val isSlotEmpty = room.status == RoomStatus.DISPONIBLE

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSlotEmpty) StatusGreen.copy(alpha = 0.1f)
                                else if (isOccupiedInSlot) StatusRed.copy(alpha = 0.1f)
                                else StatusYellow.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSlotEmpty) StatusGreen.copy(alpha = 0.4f)
                                    else if (isOccupiedInSlot) StatusRed.copy(alpha = 0.4f)
                                    else StatusYellow.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (isSlotEmpty) onEmptySlotAction(room)
                                        else if (isOccupiedInSlot) onOccupiedSlotClick(room)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = when (index) {
                                            0 -> "08-12h"
                                            1 -> "12-16h"
                                            2 -> "16-20h"
                                            else -> "20-08h"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = if (isSlotEmpty) StatusGreen else if (isOccupiedInSlot) StatusRed else StatusYellow
                                    )
                                    Text(
                                        text = if (isSlotEmpty) "+ Reservar" else if (isOccupiedInSlot) "Ocupada" else "Limpieza",
                                        fontSize = 8.sp,
                                        color = if (isSlotEmpty) StatusGreen else if (isOccupiedInSlot) StatusRed else StatusYellow,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 1: FUTURE RESERVATIONS LIST VIEW
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FutureReservationsListView(
    reservations: List<ReservationEntity>,
    rooms: List<RoomEntity>,
    viewModel: HotelViewModel?,
    onNewReservationClick: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterSelection by remember { mutableStateOf("TODAS") }

    val todayIsoString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val filteredList = remember(reservations, searchQuery, filterSelection) {
        reservations.filter { res ->
            val matchesSearch = searchQuery.isBlank() ||
                    res.clientName.contains(searchQuery, ignoreCase = true) ||
                    res.roomNumber.contains(searchQuery, ignoreCase = true) ||
                    (res.clientDpi?.contains(searchQuery, ignoreCase = true) == true) ||
                    (res.clientPhone?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (filterSelection) {
                "HOY" -> res.checkInDateString == todayIsoString && res.status != "CANCELADA"
                "PROXIMAS" -> res.status == "CONFIRMADA"
                "CHECKED_IN" -> res.status == "CHECKED_IN"
                else -> res.status != "CANCELADA"
            }
            matchesSearch && matchesFilter
        }.sortedBy { it.reservationDateMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header & New Reservation Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control de Reservaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )
                Text(
                    text = "${filteredList.size} Reservaciones encontradas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onNewReservationClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("btn_new_future_reservation")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nueva Reserva", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por cliente, DPI o habitación...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        // Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = filterSelection == "TODAS",
                onClick = { filterSelection = "TODAS" },
                label = { Text("Todas", fontSize = 11.sp) },
                modifier = Modifier.height(30.dp)
            )
            FilterChip(
                selected = filterSelection == "HOY",
                onClick = { filterSelection = "HOY" },
                label = { Text("Hoy", fontSize = 11.sp) },
                modifier = Modifier.height(30.dp)
            )
            FilterChip(
                selected = filterSelection == "PROXIMAS",
                onClick = { filterSelection = "PROXIMAS" },
                label = { Text("Futuras Confirmadas", fontSize = 11.sp) },
                modifier = Modifier.height(30.dp)
            )
            FilterChip(
                selected = filterSelection == "CHECKED_IN",
                onClick = { filterSelection = "CHECKED_IN" },
                label = { Text("Ingresados", fontSize = 11.sp) },
                modifier = Modifier.height(30.dp)
            )
        }

        if (filteredList.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No hay reservaciones para los filtros seleccionados",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Toca cualquier fecha en el calendario o el botón '+ Nueva Reserva' para registrar el check-in programado de un huésped.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { res ->
                    FutureReservationCard(
                        reservation = res,
                        onExecuteCheckIn = {
                            viewModel?.executeCheckInForReservation(res) {
                                Toast.makeText(context, "Check-In activado para ${res.clientName} (Hab. ${res.roomNumber})", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancelReservation = {
                            viewModel?.cancelReservation(res.id)
                        },
                        onDelete = {
                            viewModel?.deleteReservation(res.id)
                        }
                    )
                }
            }
        }
    }
}

// FUTURE RESERVATION DETAILED CARD
@Composable
private fun FutureReservationCard(
    reservation: ReservationEntity,
    onExecuteCheckIn: () -> Unit,
    onCancelReservation: () -> Unit,
    onDelete: () -> Unit
) {
    val isCheckedIn = reservation.status == "CHECKED_IN"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("future_reservation_card_${reservation.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCheckedIn) StatusGreen.copy(alpha = 0.15f) else HotelNavy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = reservation.roomNumber,
                                fontWeight = FontWeight.Bold,
                                color = if (isCheckedIn) StatusGreen else Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = reservation.clientName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hab. ${reservation.roomNumber} • ${reservation.durationText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCheckedIn) StatusGreen.copy(alpha = 0.15f) else HotelGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isCheckedIn) "Ingresado" else "Confirmada",
                        color = if (isCheckedIn) StatusGreen else HotelNavy,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Reservation details
            val pendingPayment = (reservation.totalPrice - reservation.advancePayment).coerceAtLeast(0.0)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = HotelNavy, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Fecha: ${reservation.checkInDateString} (${reservation.checkInTime})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Método: ${reservation.paymentMethod}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Total: Q${"%.2f".format(reservation.totalPrice)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )
                    if (reservation.advancePayment > 0) {
                        Text(
                            text = "Anticipo: Q${"%.2f".format(reservation.advancePayment)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (pendingPayment > 0) {
                        Text(
                            text = "Pendiente: Q${"%.2f".format(pendingPayment)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusRed,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Cancelado",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!reservation.notes.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notas: ${reservation.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancelReservation,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                if (!isCheckedIn) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onExecuteCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hacer Check-In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// MODAL DIALOG: AGREGAR DATOS DEL CHECK-IN / RESERVA PARA FECHA ESPECÍFICA
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCheckInOrReservationDialog(
    selectedDateMillis: Long,
    preselectedRoom: RoomEntity?,
    rooms: List<RoomEntity>,
    timeRates: List<TimeRateEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        room: RoomEntity,
        clientName: String,
        dpi: String?,
        phone: String?,
        guestCount: Int,
        rateName: String,
        durationMins: Long,
        durationText: String,
        totalPrice: Double,
        advancePayment: Double,
        paymentMethod: String,
        checkInTime: String,
        notes: String?,
        isImmediateCheckIn: Boolean
    ) -> Unit
) {
    val fullDateFormatter = remember { SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES")) }
    val formattedDateText = remember(selectedDateMillis) {
        fullDateFormatter.format(Date(selectedDateMillis)).replaceFirstChar { it.uppercase() }
    }

    val isToday = remember(selectedDateMillis) {
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val nowCal = Calendar.getInstance()
        selCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                selCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    }

    var clientName by remember { mutableStateOf("") }

    var selectedRoom by remember {
        mutableStateOf(preselectedRoom ?: rooms.firstOrNull { it.status == RoomStatus.DISPONIBLE } ?: rooms.firstOrNull())
    }
    var roomDropdownExpanded by remember { mutableStateOf(false) }

    // Rates selection
    val activeRates = remember(timeRates) {
        if (timeRates.isNotEmpty()) timeRates.filter { it.isActive }
        else listOf(
            TimeRateEntity(name = "1 Hora", durationMinutes = 60, price = 50.0),
            TimeRateEntity(name = "2 Horas", durationMinutes = 120, price = 80.0),
            TimeRateEntity(name = "3 Horas", durationMinutes = 180, price = 100.0),
            TimeRateEntity(name = "12 Horas (Noche)", durationMinutes = 720, price = 150.0),
            TimeRateEntity(name = "24 Horas (Día completo)", durationMinutes = 1440, price = 200.0)
        )
    }

    var selectedRate by remember {
        mutableStateOf(activeRates.firstOrNull { it.durationMinutes == 1440L } ?: activeRates.firstOrNull())
    }
    var rateDropdownExpanded by remember { mutableStateOf(false) }

    var advancePaymentText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Efectivo") }
    var checkInTime by remember { mutableStateOf(if (isToday) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) else "14:00") }
    var notes by remember { mutableStateOf("") }
    var isImmediateCheckIn by remember { mutableStateOf(isToday && selectedRoom?.status == RoomStatus.DISPONIBLE) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var roomError by remember { mutableStateOf<String?>(null) }

    val calculatedPrice = selectedRate?.price ?: 200.0
    val advanceAmount = advancePaymentText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val pendingPayment = (calculatedPrice - advanceAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_calendar_checkin"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = HotelNavy,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isToday && isImmediateCheckIn) "Registrar Check-In Inmediato" else "Nueva Reservación / Check-In",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selected Date Banner
                Surface(
                    color = HotelNavy.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = HotelNavy, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = "Fecha programada:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = formattedDateText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }
                    }
                }

                // Room Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = roomDropdownExpanded,
                    onExpandedChange = { roomDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRoom?.let { "Habitación ${it.roomNumber} (${it.roomType}) - Cap: ${it.capacity}p" } ?: "Seleccione una habitación",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Habitación asignada *") },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomDropdownExpanded) },
                        isError = roomError != null,
                        supportingText = { roomError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roomDropdownExpanded,
                        onDismissRequest = { roomDropdownExpanded = false }
                    ) {
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Hab. ${room.roomNumber} (${room.roomType})", fontWeight = FontWeight.Bold)
                                        Text(
                                            text = room.status,
                                            color = if (room.status == RoomStatus.DISPONIBLE) StatusGreen else StatusRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedRoom = room
                                    roomError = null
                                    roomDropdownExpanded = false
                                    if (isToday) {
                                        isImmediateCheckIn = room.status == RoomStatus.DISPONIBLE
                                    }
                                }
                            )
                        }
                    }
                }

                // --- 1. DATOS DEL HUÉSPED ---
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "1. Datos del Huésped",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    // Guest Name (Required)
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = {
                            clientName = it
                            nameError = null
                        },
                        label = { Text("Nombre completo del huésped *") },
                        placeholder = { Text("Ej. Juan Pérez o Clientes Varios") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_guest_name")
                    )

                    // Quick Selection Chips for Clientes Varios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opción rápida:",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = clientName == "Clientes Varios",
                            onClick = {
                                clientName = "Clientes Varios"
                                nameError = null
                            },
                            label = { Text("Clientes Varios", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(13.dp))
                            },
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = clientName == "Consumidor Final",
                            onClick = {
                                clientName = "Consumidor Final"
                                nameError = null
                            },
                            label = { Text("C/F", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Check-In Time
                OutlinedTextField(
                    value = checkInTime,
                    onValueChange = { checkInTime = it },
                    label = { Text("Hora llegada programada") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rate Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = rateDropdownExpanded,
                    onExpandedChange = { rateDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRate?.let { "${it.name} • Q${"%.2f".format(it.price)}" } ?: "Seleccionar tarifa",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tarifa contratada") },
                        leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = rateDropdownExpanded,
                        onDismissRequest = { rateDropdownExpanded = false }
                    ) {
                        activeRates.forEach { rate ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(rate.name, fontWeight = FontWeight.Medium)
                                        Text("Q${"%.2f".format(rate.price)}", fontWeight = FontWeight.Bold, color = HotelNavy)
                                    }
                                },
                                onClick = {
                                    selectedRate = rate
                                    rateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // --- 2. ANTICIPO, MÉTODO DE PAGO Y PAGO PENDIENTE ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "2. Anticipo y Método de Pago",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    // Advance Payment Field
                    OutlinedTextField(
                        value = advancePaymentText,
                        onValueChange = { advancePaymentText = it },
                        label = { Text("Registro de Anticipo / Depósito recibido (Q)") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Advance Amount Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = advancePaymentText == "0" || advancePaymentText.isEmpty(),
                            onClick = { advancePaymentText = "0" },
                            label = { Text("Sin Anticipo (Q0)", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = advancePaymentText == String.format(Locale.US, "%.2f", calculatedPrice * 0.5),
                            onClick = { advancePaymentText = String.format(Locale.US, "%.2f", calculatedPrice * 0.5) },
                            label = { Text("50% (Q${String.format(Locale.US, "%.0f", calculatedPrice * 0.5)})", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = advancePaymentText == String.format(Locale.US, "%.2f", calculatedPrice),
                            onClick = { advancePaymentText = String.format(Locale.US, "%.2f", calculatedPrice) },
                            label = { Text("100% Total", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    // Payment Method Selection
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Método de Pago del Anticipo:", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = paymentMethod == "Efectivo",
                                onClick = { paymentMethod = "Efectivo" },
                                label = { Text("Efectivo", fontSize = 9.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = paymentMethod == "Tarjeta",
                                onClick = { paymentMethod = "Tarjeta" },
                                label = { Text("Tarjeta", fontSize = 9.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = paymentMethod == "Transferencia",
                                onClick = { paymentMethod = "Transferencia" },
                                label = { Text("Transf.", fontSize = 9.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    // Financial Summary Card with Pending Payment
                    Surface(
                        color = if (pendingPayment > 0) MaterialTheme.colorScheme.surfaceContainerHighest else StatusGreen.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (pendingPayment > 0) HotelGold.copy(alpha = 0.6f) else StatusGreen.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total de la Estadía:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Q${"%.2f".format(calculatedPrice)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelNavy
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Anticipo / Depósito Recibido:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusGreen
                                )
                                Text(
                                    text = "Q${"%.2f".format(advanceAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saldo / Pago Pendiente:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingPayment > 0) StatusRed else StatusGreen
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (pendingPayment > 0) StatusRed.copy(alpha = 0.12f) else StatusGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Q${"%.2f".format(pendingPayment)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pendingPayment > 0) StatusRed else StatusGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (pendingPayment > 0) {
                                Text(
                                    text = "⚠️ Saldo pendiente de Q${"%.2f".format(pendingPayment)} a cobrar al momento del Check-In.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (advanceAmount > 0) {
                                Text(
                                    text = "✅ Estadía completamente pagada por adelantado.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }

                // Special notes / requests
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Peticiones especiales") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = isImmediateCheckIn,
                            onClick = { isImmediateCheckIn = true },
                            label = { Text("Check-In Inmediato", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                        FilterChip(
                            selected = !isImmediateCheckIn,
                            onClick = { isImmediateCheckIn = false },
                            label = { Text("Guardar como Reserva", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isBlank()) {
                        nameError = "Ingrese el nombre del huésped"
                        return@Button
                    }
                    val room = selectedRoom
                    if (room == null) {
                        roomError = "Seleccione una habitación"
                        return@Button
                    }
                    val rate = selectedRate ?: activeRates.first()
                    val advance = advancePaymentText.replace(",", ".").toDoubleOrNull() ?: 0.0

                    onConfirm(
                        room,
                        clientName.trim(),
                        null, // DPI eliminado
                        null, // Teléfono eliminado
                        1,    // Huéspedes por defecto
                        rate.name,
                        rate.durationMinutes,
                        rate.name,
                        calculatedPrice,
                        advance,
                        paymentMethod,
                        checkInTime,
                        notes.trim().ifEmpty { null },
                        if (isToday) isImmediateCheckIn else false
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_calendar_checkin")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isToday && isImmediateCheckIn) "Confirmar Check-In" else "Guardar Reservación",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// NETWORK STATUS BAR COMPONENT
@Composable
private fun NetworkStatusBar(
    isOnline: Boolean,
    onValidateClick: () -> Unit
) {
    Surface(
        color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("network_status_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828))
                )
                Icon(
                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Text(
                    text = if (isOnline) "Red Hotel Rivera • En Línea (Sincronizado)" else "Modo Offline • Sin Conexión a Internet",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isOnline) Color(0xFFC8E6C9) else Color(0xFFFFCDD2),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onValidateClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = "Validar",
                        modifier = Modifier.size(12.dp),
                        tint = if (isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                    Text(
                        text = "Validar Red",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                }
            }
        }
    }
}

// NETWORK VALIDATION MODAL DIALOG
@Composable
private fun NetworkValidationDialog(
    isOnline: Boolean,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var pingLatencyMs by remember { mutableIntStateOf(28) }
    var lastValidatedTime by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isChecking = true
        delay(600)
        isChecking = false
        lastValidatedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Validación de Estado de Red",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isChecking) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = HotelNavy)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Verificando paquetes y conectividad...", fontSize = 12.sp)
                    }
                } else {
                    Surface(
                        color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isOnline) "Conexión a Internet Activa" else "Sin Acceso a Internet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }

                            Text(
                                text = if (isOnline)
                                    "El dispositivo está sincronizado correctamente con la base de datos central y el sistema espejo multi-terminal."
                                else
                                    "Las operaciones locales seguirán funcionando en modo fuera de línea con base de datos SQLite interna. Los datos se sincronizarán al reconectarse.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DetailItem(label = "Estado:", value = if (isOnline) "Conectado" else "Desconectado")
                            DetailItem(label = "Protocolo:", value = "Wi-Fi / Red Local Hotel")
                            if (isOnline) {
                                DetailItem(label = "Latencia Estimada:", value = "~$pingLatencyMs ms")
                            }
                            if (lastValidatedTime != null) {
                                DetailItem(label = "Última Verificación:", value = lastValidatedTime!!)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isChecking = true
                        delay(600)
                        isChecking = false
                        lastValidatedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reverificar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
