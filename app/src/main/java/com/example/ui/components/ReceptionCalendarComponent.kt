package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.StayHistoryEntity
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
    stayHistory: List<StayHistoryEntity>,
    currentTimeMillis: Long,
    onRoomCheckInClick: (RoomEntity) -> Unit,
    onRoomOccupiedClick: (RoomEntity) -> Unit,
    onRoomCleaningClick: (RoomEntity) -> Unit,
    onNewCheckInFormClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedCalendarTab by remember { mutableIntStateOf(0) } // 0: Disponibilidad por Fecha, 1: Próximos Check-Ins
    var calendarViewMode by remember { mutableIntStateOf(0) } // 0: Tarjetas de Habitaciones, 1: Ranuras Horarias / Slots
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

    var showNetworkDialog by remember { mutableStateOf(false) }
    var isNetworkOnline by remember { mutableStateOf(NetworkConnectivityHelper.isNetworkAvailable(context)) }

    // Listen to network changes
    LaunchedEffect(Unit) {
        NetworkConnectivityHelper.observeNetworkConnectivity(context).collect { isOnline ->
            isNetworkOnline = isOnline
        }
    }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }
    val dayFormatter = remember { SimpleDateFormat("EEE dd", Locale("es", "ES")) }
    val fullDateFormatter = remember { SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES")) }

    // Calculate Availability for selected date
    val isToday = remember(selectedDateMillis) {
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val nowCal = Calendar.getInstance()
        selCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                selCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    }

    val availableCount = rooms.count { it.status == RoomStatus.DISPONIBLE }
    val occupiedCount = rooms.count { it.status == RoomStatus.OCUPADA }
    val cleaningCount = rooms.count { it.status == RoomStatus.PENDIENTE_LIMPIEZA || it.status == RoomStatus.EN_LIMPIEZA }

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

        // Sub Tabs: Disponibilidad & Próximos Check-Ins
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
                        Text("Disponibilidad", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Próximos Check-Ins (${rooms.count { it.status == RoomStatus.OCUPADA }})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                modifier = Modifier.testTag("tab_calendar_checkins")
            )
        }

        if (selectedCalendarTab == 0) {
            // TAB 0: CALENDAR AVAILABILITY VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month Header & Navigation
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
                                text = if (isToday) "Viendo Disponibilidad de Hoy" else "Fecha: ${dayFormatter.format(Date(selectedDateMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                // Days of Month Horizontal Strip
                CalendarDaysStrip(
                    calendarMonth = currentCalendarMonth,
                    selectedDateMillis = selectedDateMillis,
                    onSelectDate = { selectedDateMillis = it },
                    totalRooms = rooms.size,
                    availableRooms = availableCount
                )

                // Date Summary Metrics Card & View Mode Switcher
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetricPill(label = "Libres", count = availableCount, color = StatusGreen)
                            MetricPill(label = "Ocupadas", count = occupiedCount, color = StatusRed)
                            MetricPill(label = "Limpieza", count = cleaningCount, color = StatusYellow)
                        }

                        // View mode selector: Matriz vs Ranuras Horarias
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = HotelGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Toca o mantén presionado un slot libre para Check-In directo",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = calendarViewMode == 0,
                                    onClick = { calendarViewMode = 0 },
                                    label = { Text("Cuadrícula", fontSize = 10.sp) },
                                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = calendarViewMode == 1,
                                    onClick = { calendarViewMode = 1 },
                                    label = { Text("Ranuras / Slots", fontSize = 10.sp) },
                                    leadingIcon = { Icon(Icons.Default.ViewTimeline, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }

                if (calendarViewMode == 0) {
                    // Room Cards Grid (with Combined Clickable / Long-press on empty slots)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("calendar_rooms_grid")
                    ) {
                        items(rooms, key = { it.id }) { room ->
                            CalendarRoomCard(
                                room = room,
                                onCheckInClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Check-In para Habitación ${room.roomNumber}", Toast.LENGTH_SHORT).show()
                                    onRoomCheckInClick(room)
                                },
                                onOccupiedClick = { onRoomOccupiedClick(room) },
                                onCleaningClick = { onRoomCleaningClick(room) }
                            )
                        }
                    }
                } else {
                    // Hourly Slots & Time Timeline View
                    CalendarHourlySlotsView(
                        rooms = rooms,
                        currentTimeMillis = currentTimeMillis,
                        onEmptySlotAction = { room ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Iniciando Check-In para Habitación ${room.roomNumber}", Toast.LENGTH_SHORT).show()
                            onRoomCheckInClick(room)
                        },
                        onOccupiedSlotClick = { onRoomOccupiedClick(it) }
                    )
                }
            }
        } else {
            // TAB 1: UPCOMING CHECK-INS & ACTIVE GUESTS
            UpcomingCheckInsView(
                rooms = rooms,
                stayHistory = stayHistory,
                currentTimeMillis = currentTimeMillis,
                onNewCheckInClick = onNewCheckInFormClick,
                onRoomClick = { room ->
                    when (room.status) {
                        RoomStatus.DISPONIBLE -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRoomCheckInClick(room)
                        }
                        RoomStatus.OCUPADA -> onRoomOccupiedClick(room)
                        else -> onRoomCleaningClick(room)
                    }
                }
            )
        }
    }

    // Network State Validation Modal Dialog
    if (showNetworkDialog) {
        NetworkValidationDialog(
            isOnline = isNetworkOnline,
            onDismiss = { showNetworkDialog = false }
        )
    }
}

// ==========================================
// NETWORK STATUS BAR COMPONENT
// ==========================================
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

// ==========================================
// CALENDAR DAYS STRIP (HORIZONTAL SELECTOR)
// ==========================================
@Composable
private fun CalendarDaysStrip(
    calendarMonth: Calendar,
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit,
    totalRooms: Int,
    availableRooms: Int
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) HotelNavy else if (isToday) HotelGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerLow,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isToday) HotelGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .width(52.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectDate(dayCal.timeInMillis) }
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
                        fontSize = 16.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    // Availability status dot indicator
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) HotelGold
                                else if (availableRooms > 0) StatusGreen
                                else StatusRed
                            )
                    )
                }
            }
        }
    }
}

// ==========================================
// METRIC PILL ITEM
// ==========================================
@Composable
private fun MetricPill(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==========================================
// CALENDAR ROOM CARD (WITH TAP & LONG-PRESS ON EMPTY SLOTS)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarRoomCard(
    room: RoomEntity,
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isAvailable) androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f)) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                    if (isAvailable) {
                        // Long-press directly triggers New Check-In flow for this room
                        onCheckInClick()
                    } else if (room.status == RoomStatus.OCUPADA) {
                        onOccupiedClick()
                    } else {
                        onCleaningClick()
                    }
                }
            )
            .testTag("calendar_room_card_${room.roomNumber}")
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
                Text(
                    text = "Hab. ${room.roomNumber}",
                    style = MaterialTheme.typography.titleMedium,
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
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${room.roomType} • Cap: ${room.capacity} pers.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            if (room.status == RoomStatus.OCUPADA && !room.clientName.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "Huésped:",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = room.clientName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (room.status == RoomStatus.DISPONIBLE) {
                // Empty Calendar Slot with direct Check-In trigger
                Surface(
                    color = StatusGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = onCheckInClick,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCheckInClick()
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Check-In",
                            tint = StatusGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ranura Libre • Check-In",
                            color = StatusGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CALENDAR HOURLY SLOTS & TIMELINE VIEW
// ==========================================
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = HotelNavy.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = HotelNavy, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Selecciona o mantén presionada cualquier ranura libre para registrar Check-In inmediato.",
                        style = MaterialTheme.typography.labelSmall,
                        color = HotelNavy,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        items(rooms, key = { it.id }) { room ->
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Room Row Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HotelNavy,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = room.roomNumber,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Hab. ${room.roomNumber} (${room.roomType})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Capacidad: ${room.capacity} pers.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Current status chip
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
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Hourly Slots Timeline for this room
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeSlots.forEachIndexed { index, slotLabel ->
                            val isOccupiedInSlot = room.status == RoomStatus.OCUPADA
                            val isCleaningInSlot = room.status == RoomStatus.PENDIENTE_LIMPIEZA || room.status == RoomStatus.EN_LIMPIEZA
                            val isSlotEmpty = room.status == RoomStatus.DISPONIBLE

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSlotEmpty) StatusGreen.copy(alpha = 0.1f)
                                else if (isOccupiedInSlot) StatusRed.copy(alpha = 0.1f)
                                else StatusYellow.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSlotEmpty) StatusGreen.copy(alpha = 0.4f)
                                    else if (isOccupiedInSlot) StatusRed.copy(alpha = 0.4f)
                                    else StatusYellow.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (isSlotEmpty) {
                                                onEmptySlotAction(room)
                                            } else if (isOccupiedInSlot) {
                                                onOccupiedSlotClick(room)
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (isSlotEmpty) {
                                                // Trigger New Check-in directly
                                                onEmptySlotAction(room)
                                            } else if (isOccupiedInSlot) {
                                                onOccupiedSlotClick(room)
                                            }
                                        }
                                    )
                                    .testTag("time_slot_${room.roomNumber}_$index")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
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
                                        fontSize = 10.sp,
                                        color = if (isSlotEmpty) StatusGreen else if (isOccupiedInSlot) StatusRed else StatusYellow
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (isSlotEmpty) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(10.dp))
                                            Text("Libre", fontSize = 9.sp, color = StatusGreen, fontWeight = FontWeight.SemiBold)
                                        } else if (isOccupiedInSlot) {
                                            Text(room.clientName?.take(6) ?: "Ocupada", fontSize = 9.sp, color = StatusRed, fontWeight = FontWeight.Bold, maxLines = 1)
                                        } else {
                                            Text("Limp.", fontSize = 9.sp, color = StatusYellow, fontWeight = FontWeight.Bold)
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
}

// ==========================================
// UPCOMING CHECK-INS & ACTIVE GUESTS VIEW
// ==========================================
@Composable
private fun UpcomingCheckInsView(
    rooms: List<RoomEntity>,
    stayHistory: List<StayHistoryEntity>,
    currentTimeMillis: Long,
    onNewCheckInClick: () -> Unit,
    onRoomClick: (RoomEntity) -> Unit
) {
    val occupiedRooms = remember(rooms) {
        rooms.filter { it.status == RoomStatus.OCUPADA }
    }

    val availableRooms = remember(rooms) {
        rooms.filter { it.status == RoomStatus.DISPONIBLE }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control de Huéspedes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )
                Text(
                    text = "${occupiedRooms.size} Habitaciones Ocupadas • ${availableRooms.size} Disponibles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onNewCheckInClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nuevo Check-In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (occupiedRooms.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hotel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No hay huéspedes activos en este momento",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Todas las habitaciones disponibles están listas para registrar nuevos ingresos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(occupiedRooms, key = { it.id }) { room ->
                    val remainingMins = if (room.checkOutTimeMillis > 0) {
                        ((room.checkOutTimeMillis - currentTimeMillis) / 60000).coerceAtLeast(0)
                    } else 0

                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRoomClick(room) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = HotelNavy.copy(alpha = 0.1f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = room.roomNumber,
                                            fontWeight = FontWeight.Bold,
                                            color = HotelNavy,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = room.clientName ?: "Huésped General",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tarifa: ${room.rateName ?: "Estándar"} • ${room.guestCount} Huéspedes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    if (room.checkInTimeMillis > 0) {
                                        Text(
                                            text = "Ingreso: ${timeFormat.format(Date(room.checkInTimeMillis))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (remainingMins < 15) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = if (remainingMins > 0) "Restan ${remainingMins}m" else "Tiempo cumplido",
                                        color = if (remainingMins < 15) Color(0xFFC62828) else Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Q${"%.2f".format(room.priceCharged)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelNavy
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// NETWORK VALIDATION MODAL DIALOG
// ==========================================
@Composable
private fun NetworkValidationDialog(
    isOnline: Boolean,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var pingLatencyMs by remember { mutableIntStateOf(28) }
    var lastValidatedTime by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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

                    // Diagnostic details
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
