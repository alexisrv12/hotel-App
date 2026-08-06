package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.viewmodel.GuestCheckInViewModel

/**
 * Color palette definition for Room status indicators:
 * Green = Available (DISPONIBLE)
 * Red = Occupied (OCUPADA)
 * Orange/Amber = Cleaning (PENDIENTE_LIMPIEZA)
 */
object RoomStatusColors {
    val AvailableGreenBg = Color(0xFFE8F5E9)
    val AvailableGreenBorder = Color(0xFF81C784)
    val AvailableGreenText = Color(0xFF1B5E20)
    val AvailableGreenButton = Color(0xFF2E7D32)

    val OccupiedRedBg = Color(0xFFFFEBEE)
    val OccupiedRedBorder = Color(0xFFE57373)
    val OccupiedRedText = Color(0xFFB71C1C)
    val OccupiedRedButton = Color(0xFFC62828)

    val CleaningOrangeBg = Color(0xFFFFF3E0)
    val CleaningOrangeBorder = Color(0xFFFFB74D)
    val CleaningOrangeText = Color(0xFFE65100)
    val CleaningOrangeButton = Color(0xFFEF6C00)
}

/**
 * Compose UI component displaying a grid of hotel rooms with status color coding (Green for Available, Red for Occupied).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelRoomGridScreen(
    modifier: Modifier = Modifier,
    viewModel: GuestCheckInViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val allRooms by viewModel.allRooms.collectAsState()
    val roomsForGrid by viewModel.roomsForGrid.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val uiMessage by viewModel.userMessage.collectAsState()

    var roomForCheckIn by remember { mutableStateOf<RoomEntity?>(null) }
    var roomForDetail by remember { mutableStateOf<RoomEntity?>(null) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val availableCount = allRooms.count { it.isAvailable }
    val occupiedCount = allRooms.count { it.isOccupied }
    val cleaningCount = allRooms.count { it.isCleaning }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HotelNavy,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MeetingRoom,
                                    contentDescription = null,
                                    tint = HotelGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Matriz de Habitaciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Estado en Tiempo Real & Check-In de Huéspedes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddRoomDialog = true },
                        modifier = Modifier.testTag("add_new_room_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar Habitación",
                            tint = HotelNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // Metrics Summary Banner
            RoomMetricsSummaryHeader(
                totalCount = allRooms.size,
                availableCount = availableCount,
                occupiedCount = occupiedCount,
                cleaningCount = cleaningCount,
                modifier = Modifier.padding(16.dp)
            )

            // Status Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text("Todas (${allRooms.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HotelNavy,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_all_rooms")
                )

                FilterChip(
                    selected = selectedFilter == "DISPONIBLE",
                    onClick = { viewModel.setFilter("DISPONIBLE") },
                    label = { Text("Disponibles ($availableCount)") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RoomStatusColors.AvailableGreenButton)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoomStatusColors.AvailableGreenBg,
                        selectedLabelColor = RoomStatusColors.AvailableGreenText
                    ),
                    modifier = Modifier.testTag("filter_available_rooms")
                )

                FilterChip(
                    selected = selectedFilter == "OCUPADA",
                    onClick = { viewModel.setFilter("OCUPADA") },
                    label = { Text("Ocupadas ($occupiedCount)") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RoomStatusColors.OccupiedRedButton)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoomStatusColors.OccupiedRedBg,
                        selectedLabelColor = RoomStatusColors.OccupiedRedText
                    ),
                    modifier = Modifier.testTag("filter_occupied_rooms")
                )

                FilterChip(
                    selected = selectedFilter == "PENDIENTE_LIMPIEZA",
                    onClick = { viewModel.setFilter("PENDIENTE_LIMPIEZA") },
                    label = { Text("Limpieza ($cleaningCount)") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RoomStatusColors.CleaningOrangeButton)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoomStatusColors.CleaningOrangeBg,
                        selectedLabelColor = RoomStatusColors.CleaningOrangeText
                    ),
                    modifier = Modifier.testTag("filter_cleaning_rooms")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Room Cards Grid
            if (roomsForGrid.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bed,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No hay habitaciones registradas en este estado.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 165.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("hotel_rooms_grid")
                ) {
                    items(
                        items = roomsForGrid,
                        key = { it.id }
                    ) { room ->
                        RoomGridCard(
                            room = room,
                            onCheckInClick = { roomForCheckIn = room },
                            onCheckOutClick = { viewModel.checkOutGuest(room.id) },
                            onFinishCleaningClick = { viewModel.completeCleaning(room.id) },
                            onCardClick = { roomForDetail = room }
                        )
                    }
                }
            }
        }
    }

    // Check-In Form Dialog
    if (roomForCheckIn != null) {
        GuestCheckInDialog(
            room = roomForCheckIn!!,
            onDismiss = { roomForCheckIn = null },
            onConfirmCheckIn = { clientName, clientDpi, guestCount, price, duration ->
                viewModel.checkInGuest(
                    roomId = roomForCheckIn!!.id,
                    clientName = clientName,
                    clientDpi = clientDpi,
                    guestCount = guestCount,
                    priceCharged = price,
                    contractedDurationMinutes = duration
                )
                roomForCheckIn = null
            }
        )
    }

    // Room Details Dialog
    if (roomForDetail != null) {
        RoomDetailsDialog(
            room = roomForDetail!!,
            onDismiss = { roomForDetail = null },
            onCheckIn = {
                val r = roomForDetail
                roomForDetail = null
                roomForCheckIn = r
            },
            onCheckOut = {
                viewModel.checkOutGuest(roomForDetail!!.id)
                roomForDetail = null
            },
            onFinishCleaning = {
                viewModel.completeCleaning(roomForDetail!!.id)
                roomForDetail = null
            }
        )
    }

    // Add Room Dialog
    if (showAddRoomDialog) {
        AddRoomDialog(
            onDismiss = { showAddRoomDialog = false },
            onAddRoom = { number, type, rate ->
                viewModel.addNewRoom(number, type, rate)
                showAddRoomDialog = false
            }
        )
    }
}

@Composable
private fun RoomMetricsSummaryHeader(
    totalCount: Int,
    availableCount: Int,
    occupiedCount: Int,
    cleaningCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricPill(
                label = "Disponibles",
                count = availableCount,
                color = RoomStatusColors.AvailableGreenButton,
                bgColor = RoomStatusColors.AvailableGreenBg
            )

            Divider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            MetricPill(
                label = "Ocupadas",
                count = occupiedCount,
                color = RoomStatusColors.OccupiedRedButton,
                bgColor = RoomStatusColors.OccupiedRedBg
            )

            Divider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            MetricPill(
                label = "En Limpieza",
                count = cleaningCount,
                color = RoomStatusColors.CleaningOrangeButton,
                bgColor = RoomStatusColors.CleaningOrangeBg
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RoomGridCard(
    room: RoomEntity,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onFinishCleaningClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val (bgColor, borderColor, textColor, buttonColor, statusTitle) = when {
        room.isAvailable -> Tuple5(
            RoomStatusColors.AvailableGreenBg,
            RoomStatusColors.AvailableGreenBorder,
            RoomStatusColors.AvailableGreenText,
            RoomStatusColors.AvailableGreenButton,
            "DISPONIBLE"
        )
        room.isOccupied -> Tuple5(
            RoomStatusColors.OccupiedRedBg,
            RoomStatusColors.OccupiedRedBorder,
            RoomStatusColors.OccupiedRedText,
            RoomStatusColors.OccupiedRedButton,
            "OCUPADA"
        )
        else -> Tuple5(
            RoomStatusColors.CleaningOrangeBg,
            RoomStatusColors.CleaningOrangeBorder,
            RoomStatusColors.CleaningOrangeText,
            RoomStatusColors.CleaningOrangeButton,
            "EN LIMPIEZA"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("room_card_${room.roomNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Room Number & Status Badge
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
                        imageVector = Icons.Default.Hotel,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Hab. ${room.roomNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = buttonColor
                ) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Tipo: ${room.roomType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Tarifa: Q${room.nightlyRate}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Guest Name info if occupied
            if (room.isOccupied && !room.clientName.isNull_or_blank_safe()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = textColor
                    )
                    Text(
                        text = room.clientName ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button
            when {
                room.isAvailable -> {
                    Button(
                        onClick = onCheckInClick,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("checkin_button_${room.roomNumber}"),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check-In", fontSize = 12.sp)
                    }
                }
                room.isOccupied -> {
                    Button(
                        onClick = onCheckOutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("checkout_button_${room.roomNumber}"),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check-Out", fontSize = 12.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onFinishCleaningClick,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("finish_cleaning_button_${room.roomNumber}"),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Listo", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestCheckInDialog(
    room: RoomEntity,
    onDismiss: () -> Unit,
    onConfirmCheckIn: (clientName: String, clientDpi: String, guestCount: Int, price: Double, durationMinutes: Long) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientDpi by remember { mutableStateOf("") }
    var guestCountText by remember { mutableStateOf("1") }
    var customPriceText by remember { mutableStateOf(room.nightlyRate.toString()) }
    var durationMinutesText by remember { mutableStateOf("240") }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = RoomStatusColors.AvailableGreenButton
                )
                Text("Check-In Habitación ${room.roomNumber}")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Registrar ingreso de huésped en la base de datos (Transición a 'OCUPADA').",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = {
                        clientName = it
                        showError = false
                    },
                    label = { Text("Nombre del Huésped *") },
                    isError = showError && clientName.isBlank(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkin_input_client_name")
                )

                OutlinedTextField(
                    value = clientDpi,
                    onValueChange = { clientDpi = it },
                    label = { Text("DPI / Documento de Identidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkin_input_client_dpi")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = guestCountText,
                        onValueChange = { guestCountText = it },
                        label = { Text("Huéspedes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("checkin_input_guest_count")
                    )

                    OutlinedTextField(
                        value = customPriceText,
                        onValueChange = { customPriceText = it },
                        label = { Text("Precio (Q)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("checkin_input_price")
                    )
                }

                if (showError) {
                    Text(
                        text = "El nombre del huésped es obligatorio.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isBlank()) {
                        showError = true
                    } else {
                        val guests = guestCountText.toIntOrNull() ?: 1
                        val price = customPriceText.toDoubleOrNull() ?: room.nightlyRate
                        val duration = durationMinutesText.toLongOrNull() ?: 240L
                        onConfirmCheckIn(clientName, clientDpi, guests, price, duration)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoomStatusColors.AvailableGreenButton),
                modifier = Modifier.testTag("confirm_checkin_button")
            ) {
                Text("Confirmar Check-In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun RoomDetailsDialog(
    room: RoomEntity,
    onDismiss: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onFinishCleaning: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle - Habitación ${room.roomNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Tipo: ${room.roomType}")
                Text("• Estado actual: ${room.status}")
                Text("• Tarifa Base: Q${room.nightlyRate}")
                if (room.isOccupied) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("• Huésped: ${room.clientName ?: "No registrado"}")
                    Text("• DPI: ${room.clientDpi ?: "N/A"}")
                    Text("• Cantidad de Huéspedes: ${room.guestCount}")
                }
            }
        },
        confirmButton = {
            when {
                room.isAvailable -> Button(onClick = onCheckIn) { Text("Realizar Check-In") }
                room.isOccupied -> Button(onClick = onCheckOut) { Text("Registrar Check-Out") }
                else -> Button(onClick = onFinishCleaning) { Text("Completar Limpieza") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun AddRoomDialog(
    onDismiss: () -> Unit,
    onAddRoom: (number: String, type: String, rate: Double) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Estándar") }
    var rateText by remember { mutableStateOf("150.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Habitación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Número de Habitación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_room_number")
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Tipo (ej. Suite, Estándar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_room_type")
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Tarifa por Noche (Q)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("add_room_rate")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank()) {
                        val rate = rateText.toDoubleOrNull() ?: 150.0
                        onAddRoom(number, type, rate)
                    }
                },
                modifier = Modifier.testTag("save_new_room_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)

private fun String?.isNull_or_blank_safe(): Boolean {
    return this == null || this.trim().isEmpty()
}
