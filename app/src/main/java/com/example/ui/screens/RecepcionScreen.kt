package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.example.ui.components.GuestBookingQrScannerDialog
import com.example.ui.components.InventoryThresholdAlertBanner
import com.example.ui.components.MaintenanceRequestFormDialog
import com.example.ui.components.QRScannerView
import com.example.ui.components.ReceptionCalendarComponent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.TimeRateEntity
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecepcionScreen(
    viewModel: HotelViewModel,
    rooms: List<RoomEntity>,
    timeRates: List<TimeRateEntity>,
    currentTimeMillis: Long,
    activeUser: String,
    onNavigateToCheckInForm: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val stayHistory by viewModel.stayHistory.collectAsStateWithLifecycle()
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    val lowStockSupplies by viewModel.lowStockSupplies.collectAsStateWithLifecycle()
    var selectedReceptionTab by remember { mutableIntStateOf(0) } // 0: Cuadrícula, 1: Calendario & Check-Ins
    var selectedFilter by remember { mutableStateOf("TODAS") }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog States
    var selectedRoomForCheckIn by remember { mutableStateOf<RoomEntity?>(null) }
    var selectedRoomForOccupied by remember { mutableStateOf<RoomEntity?>(null) }
    var selectedRoomForCleaning by remember { mutableStateOf<RoomEntity?>(null) }
    var showRecentHistoryDialog by remember { mutableStateOf(false) }
    var showQRScannerDialog by remember { mutableStateOf(false) }
    var showInvoiceCalculatorDialog by remember { mutableStateOf(false) }
    var showMaintenanceReportDialog by remember { mutableStateOf(false) }
    var selectedRoomForMaintenance by remember { mutableStateOf<String?>(null) }
    var activeInvoiceToShow by remember { mutableStateOf<InvoiceEntity?>(null) }

    // Calculate Summary Counts
    val countAvailable = rooms.count { it.status == RoomStatus.DISPONIBLE }
    val countOccupied = rooms.count { it.status == RoomStatus.OCUPADA }
    val countCleaning = rooms.count { it.status == RoomStatus.PENDIENTE_LIMPIEZA || it.status == RoomStatus.EN_LIMPIEZA }

    // Filtered rooms
    val filteredRooms = rooms.filter { room ->
        val matchesSearch = room.roomNumber.contains(searchQuery, ignoreCase = true) ||
                (room.clientName?.contains(searchQuery, ignoreCase = true) == true)
        val matchesFilter = when (selectedFilter) {
            "DISPONIBLE" -> room.status == RoomStatus.DISPONIBLE
            "OCUPADA" -> room.status == RoomStatus.OCUPADA
            "LIMPIEZA" -> room.status == RoomStatus.PENDIENTE_LIMPIEZA || room.status == RoomStatus.EN_LIMPIEZA
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Recepción", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Hotel Rivera", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { selectedReceptionTab = if (selectedReceptionTab == 0) 1 else 0 },
                        modifier = Modifier.testTag("toggle_calendar_view_button")
                    ) {
                        Icon(
                            imageVector = if (selectedReceptionTab == 0) Icons.Default.CalendarMonth else Icons.Default.GridView,
                            contentDescription = "Alternar Vista de Calendario / Cuadrícula"
                        )
                    }
                    IconButton(
                        onClick = onNavigateToCheckInForm,
                        modifier = Modifier.testTag("new_checkin_form_top_button")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Formulario de Check-In de Huésped")
                    }
                    IconButton(
                        onClick = { showQRScannerDialog = true },
                        modifier = Modifier.testTag("scan_qr_code_top_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR de Habitación / Reserva")
                    }
                    IconButton(
                        onClick = {
                            selectedRoomForMaintenance = null
                            showMaintenanceReportDialog = true
                        },
                        modifier = Modifier.testTag("report_broken_item_top_button")
                    ) {
                        Icon(Icons.Default.Handyman, contentDescription = "Reportar Avería o Mantenimiento")
                    }
                    IconButton(
                        onClick = { showInvoiceCalculatorDialog = true },
                        modifier = Modifier.testTag("open_invoice_calculator_top_button")
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculadora de Facturación")
                    }
                    IconButton(onClick = { showRecentHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Historial Reciente")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Reception Navigation Tabs: Cuadrícula vs Calendario
            TabRow(
                selectedTabIndex = selectedReceptionTab,
                containerColor = HotelNavy.copy(alpha = 0.05f),
                contentColor = HotelNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedReceptionTab == 0,
                    onClick = { selectedReceptionTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Habitaciones (Cuadrícula)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_reception_grid")
                )
                Tab(
                    selected = selectedReceptionTab == 1,
                    onClick = { selectedReceptionTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Calendario y Check-Ins", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_reception_calendar")
                )
            }

            if (selectedReceptionTab == 1) {
                // CALENDAR & UPCOMING CHECK-INS VIEW COMPONENT
                ReceptionCalendarComponent(
                    rooms = rooms,
                    timeRates = timeRates,
                    reservations = reservations,
                    stayHistory = stayHistory,
                    currentTimeMillis = currentTimeMillis,
                    viewModel = viewModel,
                    onRoomCheckInClick = { selectedRoomForCheckIn = it },
                    onRoomOccupiedClick = { selectedRoomForOccupied = it },
                    onRoomCleaningClick = { selectedRoomForCleaning = it },
                    onNewCheckInFormClick = onNavigateToCheckInForm,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // DEFAULT GRID VIEW
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Automated Low Stock Inventory Threshold Alert Banner
                    if (lowStockSupplies.isNotEmpty()) {
                        InventoryThresholdAlertBanner(
                            lowStockSupplies = lowStockSupplies,
                            viewModel = viewModel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Status Counters Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusCounterCard(
                            title = "Disponibles",
                            count = countAvailable,
                            color = StatusGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatusCounterCard(
                            title = "Ocupadas",
                            count = countOccupied,
                            color = StatusRed,
                            modifier = Modifier.weight(1f)
                        )
                        StatusCounterCard(
                            title = "Limpieza",
                            count = countCleaning,
                            color = StatusYellow,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Search and Filter Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar habitación o cliente...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedFilter == "TODAS",
                                onClick = { selectedFilter = "TODAS" },
                                label = { Text("Todas (${rooms.size})") }
                            )
                            FilterChip(
                                selected = selectedFilter == "DISPONIBLE",
                                onClick = { selectedFilter = "DISPONIBLE" },
                                label = { Text("Disponibles ($countAvailable)") }
                            )
                            FilterChip(
                                selected = selectedFilter == "OCUPADA",
                                onClick = { selectedFilter = "OCUPADA" },
                                label = { Text("Ocupadas ($countOccupied)") }
                            )
                            FilterChip(
                                selected = selectedFilter == "LIMPIEZA",
                                onClick = { selectedFilter = "LIMPIEZA" },
                                label = { Text("Limpieza ($countCleaning)") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Room Cards Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 165.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRooms, key = { it.id }) { room ->
                            RoomCard(
                                room = room,
                                currentTimeMillis = currentTimeMillis,
                                onClick = {
                                    when (room.status) {
                                        RoomStatus.DISPONIBLE -> selectedRoomForCheckIn = room
                                        RoomStatus.OCUPADA -> selectedRoomForOccupied = room
                                        RoomStatus.PENDIENTE_LIMPIEZA, RoomStatus.EN_LIMPIEZA -> selectedRoomForCleaning = room
                                        else -> selectedRoomForCheckIn = room
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- CHECK-IN FORM DIALOG ---
    selectedRoomForCheckIn?.let { room ->
        CheckInDialog(
            room = room,
            timeRates = timeRates.filter { it.isActive },
            onDismiss = { selectedRoomForCheckIn = null },
            onConfirm = { clientName, dpi, guests, selectedRate, notes ->
                viewModel.checkInRoom(
                    roomId = room.id,
                    clientName = clientName,
                    clientDpi = dpi,
                    guestCount = guests,
                    rate = selectedRate,
                    notes = notes
                )
                selectedRoomForCheckIn = null
            }
        )
    }

    // --- OCCUPIED ROOM ACTIONS DIALOG ---
    selectedRoomForOccupied?.let { room ->
        OccupiedRoomDialog(
            room = room,
            timeRates = timeRates.filter { it.isActive },
            currentTimeMillis = currentTimeMillis,
            onDismiss = { selectedRoomForOccupied = null },
            onExtend = { extraMins, extraPrice ->
                viewModel.extendStay(room.id, extraMins, extraPrice)
                selectedRoomForOccupied = null
            },
            onFinishStay = { paymentMethod, finalPrice, discount, generateInvoice, notes ->
                val checkInStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(room.checkInTimeMillis))
                val checkOutStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentTimeMillis))
                val clientName = room.clientName ?: "Cliente General"
                val rateName = room.rateName ?: "Tarifa General"
                val roomNum = room.roomNumber

                viewModel.finishStay(room.id, paymentMethod, finalPrice, notes)
                selectedRoomForOccupied = null

                if (generateInvoice) {
                    viewModel.generateInvoice(
                        roomNumber = roomNum,
                        clientName = clientName,
                        contractedTime = rateName,
                        checkInTime = checkInStr,
                        checkOutTime = checkOutStr,
                        price = finalPrice,
                        discount = discount,
                        paymentMethod = paymentMethod,
                        onComplete = { createdInv ->
                            activeInvoiceToShow = createdInv
                        }
                    )
                }
            },
            onReportMaintenance = {
                selectedRoomForMaintenance = room.roomNumber
                showMaintenanceReportDialog = true
            }
        )
    }

    // --- CLEANING ROOM DIALOG ---
    selectedRoomForCleaning?.let { room ->
        CleaningDialog(
            room = room,
            onDismiss = { selectedRoomForCleaning = null },
            onUpdateStatus = { newStatus ->
                viewModel.updateRoomCleaningStatus(room.id, newStatus)
                selectedRoomForCleaning = null
            },
            onReportMaintenance = {
                selectedRoomForMaintenance = room.roomNumber
                showMaintenanceReportDialog = true
            }
        )
    }

    // --- RECENT HISTORY DIALOG ---
    if (showRecentHistoryDialog) {
        RecentHistoryDialog(
            viewModel = viewModel,
            onDismiss = { showRecentHistoryDialog = false }
        )
    }

    // --- GUEST BOOKING QR CODE SCANNER MODULE (ZXing camera + Instant Check-In) ---
    if (showQRScannerDialog) {
        GuestBookingQrScannerDialog(
            rooms = rooms,
            timeRates = timeRates.filter { it.isActive },
            viewModel = viewModel,
            onDismiss = { showQRScannerDialog = false },
            onInstantCheckInSuccess = { roomNum, guestName ->
                showQRScannerDialog = false
            }
        )
    }

    // --- MAINTENANCE & BROKEN ITEM REPORT DIALOG (CameraX + Room Form) ---
    if (showMaintenanceReportDialog) {
        MaintenanceRequestFormDialog(
            viewModel = viewModel,
            initialRoomNumber = selectedRoomForMaintenance,
            onDismiss = {
                showMaintenanceReportDialog = false
                selectedRoomForMaintenance = null
            }
        )
    }

    // --- INVOICE CALCULATOR MODULE DIALOG ---
    if (showInvoiceCalculatorDialog) {
        InvoiceCalculatorDialog(
            rooms = rooms,
            onDismiss = { showInvoiceCalculatorDialog = false },
            onSaveInvoice = { invoice ->
                viewModel.insertInvoice(invoice)
            }
        )
    }

    // --- INVOICE DISPLAY DIALOG ---
    activeInvoiceToShow?.let { inv ->
        InvoiceDialog(
            invoice = inv,
            onDismiss = { activeInvoiceToShow = null }
        )
    }
}

@Composable
fun StatusCounterCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun RoomCard(
    room: RoomEntity,
    currentTimeMillis: Long,
    onClick: () -> Unit
) {
    val statusColor = when (room.status) {
        RoomStatus.DISPONIBLE -> StatusGreen
        RoomStatus.OCUPADA -> StatusRed
        RoomStatus.PENDIENTE_LIMPIEZA, RoomStatus.EN_LIMPIEZA -> StatusYellow
        RoomStatus.RESERVADA -> StatusBlue
        else -> StatusGreen
    }

    val remainingMillis = maxOf(0L, room.checkOutTimeMillis - currentTimeMillis)
    val isTimer15Min = room.status == RoomStatus.OCUPADA && remainingMillis in 1..900_000L
    val isTimeEnded = room.status == RoomStatus.OCUPADA && remainingMillis <= 0L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("room_card_${room.roomNumber}")
            .border(
                width = if (isTimer15Min || isTimeEnded) 2.dp else 1.dp,
                color = if (isTimeEnded) StatusRed else if (isTimer15Min) StatusYellow else statusColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Room Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hab. ${room.roomNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (room.status) {
                            RoomStatus.DISPONIBLE -> "Disponible"
                            RoomStatus.OCUPADA -> if (isTimeEnded) "Tiempo Finalizado" else "Ocupada"
                            RoomStatus.PENDIENTE_LIMPIEZA -> "Pend. Limpieza"
                            RoomStatus.EN_LIMPIEZA -> "En Limpieza"
                            RoomStatus.RESERVADA -> "Reservada"
                            else -> room.status
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (room.status == RoomStatus.OCUPADA) {
                // Client Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = room.clientName ?: "Cliente",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${room.rateName ?: ""} • Q${String.format(Locale.US, "%.2f", room.priceCharged)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Timer Banner
                val timeStr = formatMillisToTime(remainingMillis)
                val totalMillis = room.contractedDurationMinutes * 60 * 1000L
                val progress = if (totalMillis > 0) (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f) else 0f

                Surface(
                    color = if (isTimeEnded) StatusRed.copy(alpha = 0.15f)
                            else if (isTimer15Min) StatusYellow.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isTimeEnded || isTimer15Min) Icons.Default.Alarm else Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isTimeEnded) StatusRed else if (isTimer15Min) StatusYellow else HotelNavy
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTimeEnded) "¡TIEMPO FINALIZADO!" else "Restante:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimeEnded) StatusRed else if (isTimer15Min) StatusYellow else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (isTimeEnded) "00:00:00" else timeStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimeEnded) StatusRed else if (isTimer15Min) StatusYellow else HotelNavy
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (isTimeEnded) StatusRed else if (isTimer15Min) StatusYellow else StatusGreen,
                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Check-in / Out times
                val inTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(room.checkInTimeMillis))
                val outTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(room.checkOutTimeMillis))

                Text(
                    text = "Entrada: $inTimeStr • Salida: $outTimeStr",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (room.status == RoomStatus.DISPONIBLE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(StatusGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = StatusGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tocar para Hospedar",
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(StatusYellow.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = StatusYellow)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (room.status == RoomStatus.EN_LIMPIEZA) "En Limpieza..." else "Tocar para Limpiar",
                            color = StatusYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// --- FORMULARIO DE HOSPEDAJE (CHECK-IN DIALOG) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInDialog(
    room: RoomEntity,
    timeRates: List<TimeRateEntity>,
    onDismiss: () -> Unit,
    onConfirm: (clientName: String, dpi: String?, guests: Int, rate: TimeRateEntity, notes: String?) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var selectedRate by remember { mutableStateOf(timeRates.firstOrNull()) }
    var expandedRateDropdown by remember { mutableStateOf(false) }
    var advancePaymentText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Efectivo") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val ratePrice = selectedRate?.price ?: 0.0
    val advanceAmount = advancePaymentText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val pendingPayment = (ratePrice - advanceAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hospedar en Habitación ${room.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = StatusRed, fontSize = 12.sp)
                }

                // 1. Datos del Huésped
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Datos del Huésped",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = {
                            clientName = it
                            errorMessage = null
                        },
                        label = { Text("Nombre Completo *") },
                        placeholder = { Text("Ej. Juan Pérez o Clientes Varios") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_client_name")
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
                                errorMessage = null
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
                                errorMessage = null
                            },
                            label = { Text("C/F", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Rate Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedRateDropdown,
                    onExpandedChange = { expandedRateDropdown = !expandedRateDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedRate?.let { "${it.name} - Q${String.format(Locale.US, "%.2f", it.price)}" } ?: "Seleccionar Tiempo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Tiempo / Tarifa *") },
                        leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRateDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRateDropdown,
                        onDismissRequest = { expandedRateDropdown = false }
                    ) {
                        timeRates.forEach { rate ->
                            DropdownMenuItem(
                                text = { Text("${rate.name} — Q${String.format(Locale.US, "%.2f", rate.price)}") },
                                onClick = {
                                    selectedRate = rate
                                    expandedRateDropdown = false
                                }
                            )
                        }
                    }
                }

                // 2. Anticipo y Pago Pendiente
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Anticipo y Método de Pago",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    OutlinedTextField(
                        value = advancePaymentText,
                        onValueChange = { advancePaymentText = it },
                        label = { Text("Registro de Anticipo / Depósito recibido (Q)") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Advance Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = advancePaymentText == "0" || advancePaymentText.isEmpty(),
                            onClick = { advancePaymentText = "0" },
                            label = { Text("Sin Anticipo (Q0)", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = advancePaymentText == String.format(Locale.US, "%.2f", ratePrice * 0.5),
                            onClick = { advancePaymentText = String.format(Locale.US, "%.2f", ratePrice * 0.5) },
                            label = { Text("50% (Q${String.format(Locale.US, "%.0f", ratePrice * 0.5)})", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = advancePaymentText == String.format(Locale.US, "%.2f", ratePrice),
                            onClick = { advancePaymentText = String.format(Locale.US, "%.2f", ratePrice) },
                            label = { Text("100% Total", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    // Payment Method Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Método:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterChip(
                            selected = paymentMethod == "Efectivo",
                            onClick = { paymentMethod = "Efectivo" },
                            label = { Text("Efectivo", fontSize = 9.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = paymentMethod == "Tarjeta",
                            onClick = { paymentMethod = "Tarjeta" },
                            label = { Text("Tarjeta", fontSize = 9.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = paymentMethod == "Transferencia",
                            onClick = { paymentMethod = "Transferencia" },
                            label = { Text("Transf.", fontSize = 9.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    // Financial Summary Breakdown Card with Pending Payment
                    Surface(
                        color = if (pendingPayment > 0) MaterialTheme.colorScheme.surfaceContainerHighest else StatusGreen.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (pendingPayment > 0) HotelGold.copy(alpha = 0.5f) else StatusGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total a Pagar:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Q${String.format(Locale.US, "%.2f", ratePrice)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Anticipo Recibido:", fontSize = 12.sp, color = StatusGreen)
                                Text("Q${String.format(Locale.US, "%.2f", advanceAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusGreen)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Saldo Pendiente:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (pendingPayment > 0) StatusRed else StatusGreen
                                )
                                Text(
                                    "Q${String.format(Locale.US, "%.2f", pendingPayment)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (pendingPayment > 0) StatusRed else StatusGreen
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones / Notas") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isBlank()) {
                        errorMessage = "Por favor ingrese el nombre del cliente."
                        return@Button
                    }
                    if (selectedRate == null) {
                        errorMessage = "Por favor seleccione un tipo de tiempo."
                        return@Button
                    }
                    val finalNotes = if (advanceAmount > 0) {
                        val payNote = "Anticipo: Q${"%.2f".format(advanceAmount)} ($paymentMethod) | Pendiente: Q${"%.2f".format(pendingPayment)}"
                        if (notes.isBlank()) payNote else "$notes | $payNote"
                    } else notes.ifBlank { null }

                    onConfirm(clientName.trim(), null, 1, selectedRate!!, finalNotes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("Guardar Hospedaje")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// --- OCCUPIED ROOM DIALOG (COBRAR, EXTENDER TIEMPO, FINALIZAR) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccupiedRoomDialog(
    room: RoomEntity,
    timeRates: List<TimeRateEntity>,
    currentTimeMillis: Long,
    onDismiss: () -> Unit,
    onExtend: (extraMins: Long, extraPrice: Double) -> Unit,
    onFinishStay: (paymentMethod: String, finalPrice: Double, discount: Double, generateInvoice: Boolean, notes: String?) -> Unit,
    onReportMaintenance: () -> Unit = {}
) {
    var mode by remember { mutableStateOf("MAIN") } // "MAIN", "EXTEND", "FINISH"

    // Extend mode state
    var selectedExtraRate by remember { mutableStateOf(timeRates.firstOrNull()) }
    var expandedExtraRate by remember { mutableStateOf(false) }

    // Finish stay state
    var paymentMethod by remember { mutableStateOf("Efectivo") }
    var finalPriceText by remember { mutableStateOf(room.priceCharged.toString()) }
    var discountText by remember { mutableStateOf("0.0") }
    var shouldGenerateInvoice by remember { mutableStateOf(true) }
    var finishNotes by remember { mutableStateOf("") }

    val remainingMillis = maxOf(0L, room.checkOutTimeMillis - currentTimeMillis)
    val isTimeEnded = remainingMillis <= 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Habitación ${room.roomNumber} - ${room.clientName}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isTimeEnded) {
                    Surface(
                        color = StatusRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¡El tiempo contratado ha terminado!",
                                color = StatusRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (mode == "MAIN") {
                    Text("Tiempo Contratado: ${room.rateName}")
                    Text("Precio Cobrado: Q${String.format(Locale.US, "%.2f", room.priceCharged)}")
                    Text("Tiempo Restante: ${formatMillisToTime(remainingMillis)}")

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { mode = "FINISH" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) {
                        Text("Cobrar y Finalizar Hospedaje")
                    }

                    Button(
                        onClick = { mode = "EXTEND" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                    ) {
                        Text("Extender Tiempo")
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onReportMaintenance()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HotelNavy)
                    ) {
                        Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reportar Avería en Habitación 📷", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                } else if (mode == "EXTEND") {
                    Text("Seleccione la extensión de tiempo:", fontWeight = FontWeight.Bold)

                    ExposedDropdownMenuBox(
                        expanded = expandedExtraRate,
                        onExpandedChange = { expandedExtraRate = !expandedExtraRate }
                    ) {
                        OutlinedTextField(
                            value = selectedExtraRate?.let { "${it.name} (+Q${it.price})" } ?: "Seleccionar",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Extensión") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExtraRate) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedExtraRate,
                            onDismissRequest = { expandedExtraRate = false }
                        ) {
                            timeRates.forEach { rate ->
                                DropdownMenuItem(
                                    text = { Text("${rate.name} (+Q${rate.price})") },
                                    onClick = {
                                        selectedExtraRate = rate
                                        expandedExtraRate = false
                                    }
                                )
                            }
                        }
                    }
                } else if (mode == "FINISH") {
                    Text("Confirmar Cobro y Cierre de Hospedaje", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = finalPriceText,
                        onValueChange = { finalPriceText = it },
                        label = { Text("Precio (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Descuento (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shouldGenerateInvoice = !shouldGenerateInvoice }
                    ) {
                        Checkbox(
                            checked = shouldGenerateInvoice,
                            onCheckedChange = { shouldGenerateInvoice = it }
                        )
                        Text("Generar e Imprimir Factura", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = HotelNavy)
                    }

                    Text("Método de Pago:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Efectivo", "Tarjeta", "Transferencia").forEach { method ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { paymentMethod = method }
                            ) {
                                RadioButton(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method }
                                )
                                Text(method, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = finishNotes,
                        onValueChange = { finishNotes = it },
                        label = { Text("Notas de salida (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = StatusYellow.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ Se descontarán los insumos configurados automáticamente y la habitación pasará a 'Pendiente de limpieza'.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (mode == "EXTEND") {
                Button(
                    onClick = {
                        selectedExtraRate?.let { rate ->
                            onExtend(rate.durationMinutes, rate.price)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("Confirmar Extensión")
                }
            } else if (mode == "FINISH") {
                Button(
                    onClick = {
                        val price = finalPriceText.toDoubleOrNull() ?: room.priceCharged
                        val discount = discountText.toDoubleOrNull() ?: 0.0
                        onFinishStay(paymentMethod, price, discount, shouldGenerateInvoice, finishNotes.ifBlank { null })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Text(if (shouldGenerateInvoice) "Finalizar y Generar Factura" else "Finalizar Hospedaje")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                if (mode != "MAIN") mode = "MAIN" else onDismiss()
            }) {
                Text(if (mode != "MAIN") "Atrás" else "Cerrar")
            }
        }
    )
}

// --- CLEANING DIALOG ---
@Composable
fun CleaningDialog(
    room: RoomEntity,
    onDismiss: () -> Unit,
    onUpdateStatus: (newStatus: String) -> Unit,
    onReportMaintenance: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Limpieza Habitación ${room.roomNumber}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Estado Actual: ${if (room.status == RoomStatus.EN_LIMPIEZA) "En Limpieza" else "Pendiente de Limpieza"}")

                if (room.status == RoomStatus.PENDIENTE_LIMPIEZA) {
                    Button(
                        onClick = { onUpdateStatus(RoomStatus.EN_LIMPIEZA) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusYellow)
                    ) {
                        Text("Iniciar Limpieza")
                    }
                }

                Button(
                    onClick = { onUpdateStatus(RoomStatus.DISPONIBLE) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Text("Marcar Habitación Limpia y Disponible")
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onReportMaintenance()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HotelNavy)
                ) {
                    Icon(Icons.Default.Handyman, contentDescription = null, tint = HotelNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reportar Avería con Foto 📷", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- RECENT HISTORY DIALOG ---
@Composable
fun RecentHistoryDialog(
    viewModel: HotelViewModel,
    onDismiss: () -> Unit
) {
    val historyList by viewModel.stayHistory.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial Reciente de Hospedajes", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (historyList.isEmpty()) {
                    Text("No hay registros recientes aún.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList.take(20)) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Hab. ${item.roomNumber} — ${item.clientName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Q${String.format(Locale.US, "%.2f", item.priceCharged)}", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 13.sp)
                                    }
                                    Text("Tiempo: ${item.contractedTimeName} • Pago: ${item.paymentMethod}", fontSize = 11.sp)
                                    Text("Fecha: ${item.dateString}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

fun formatMillisToTime(millis: Long): String {
    if (millis <= 0) return "00:00:00"
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
