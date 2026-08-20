package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import com.example.ui.Screen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.ProductEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.SupplyEntity
import com.example.data.entities.TimeRateEntity
import com.example.data.entities.UserEntity
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import java.util.Locale

// --- 1. GERENTE ROOMS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteRoomsScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var totalRoomsInput by remember { mutableStateOf(rooms.size.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Habitaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showAddRoomDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelGold)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Agregar Habitación")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Room Status Dashboard Grid
            com.example.ui.components.RoomStatusDashboard(
                rooms = rooms,
                onStatusChange = { room, newStatus ->
                    viewModel.updateRoomStatus(room.id, newStatus)
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cantidad Total de Habitaciones:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${rooms.size} registradas en el sistema", fontSize = 12.sp, color = Color.Gray)
                    }
                    OutlinedTextField(
                        value = totalRoomsInput,
                        onValueChange = {
                            totalRoomsInput = it
                            val count = it.toIntOrNull()
                            if (count != null && count in 1..100) {
                                viewModel.setTotalRooms(count)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rooms, key = { it.id }) { room ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Habitación ${room.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Estado: ${room.status}", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.deleteRoom(room.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRoomDialog) {
        var roomNumberText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = { Text("Agregar Habitación") },
            text = {
                OutlinedTextField(
                    value = roomNumberText,
                    onValueChange = { roomNumberText = it },
                    label = { Text("Número de Habitación (e.g. 1, 2, 3...)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomNumberText.isNotBlank()) {
                            viewModel.addRoom(roomNumberText.trim())
                            showAddRoomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) { Text("Guardar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddRoomDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// --- 2. GERENTE RATES SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteRatesScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val rates by viewModel.timeRates.collectAsStateWithLifecycle()
    var editingRate by remember { mutableStateOf<TimeRateEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Precios y Tarifas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelGold)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nueva Tarifa")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rates, key = { it.id }) { rate ->
                    val hoursVal = rate.durationMinutes / 60.0
                    val durationDisplay = if (rate.durationMinutes % 60 == 0L) {
                        val h = rate.durationMinutes / 60
                        if (h == 1L) "1 Hora" else "$h Horas"
                    } else {
                        "${String.format(Locale.US, "%.1f", hoursVal)} Horas (${rate.durationMinutes} min)"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rate.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Duración: $durationDisplay", fontSize = 12.sp, color = Color.Gray)
                                Text("Precio: Q${String.format(Locale.US, "%.2f", rate.price)}", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 15.sp)
                            }
                            Row {
                                IconButton(onClick = { editingRate = rate }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = HotelNavy)
                                }
                                IconButton(onClick = { viewModel.deleteTimeRate(rate.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingRate != null) {
        val isEdit = editingRate != null
        var name by remember { mutableStateOf(editingRate?.name ?: "") }
        var durationHoursText by remember {
            mutableStateOf(
                editingRate?.let {
                    val h = it.durationMinutes / 60.0
                    if (it.durationMinutes % 60 == 0L) (it.durationMinutes / 60).toString() else h.toString()
                } ?: "1"
            )
        }
        var priceText by remember { mutableStateOf(editingRate?.price?.toString() ?: "50.0") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingRate = null
            },
            title = { Text(if (isEdit) "Editar Tarifa" else "Nueva Tarifa por Horas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de Tarifa (e.g. 1 Hora, 2 Horas, 12 Horas)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = durationHoursText,
                        onValueChange = { durationHoursText = it },
                        label = { Text("Duración en Horas (e.g. 1, 2, 3, 12, 24)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Precio en Quetzales (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = durationHoursText.toDoubleOrNull() ?: 1.0
                        val durationMinutes = (hours * 60).toLong()
                        val price = priceText.toDoubleOrNull() ?: 50.0
                        if (name.isNotBlank()) {
                            val newEntity = TimeRateEntity(
                                id = editingRate?.id ?: 0L,
                                name = name,
                                durationMinutes = durationMinutes,
                                price = price
                            )
                            viewModel.saveTimeRate(newEntity)
                            showAddDialog = false
                            editingRate = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) { Text("Guardar") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingRate = null
                }) { Text("Cancelar") }
            }
        )
    }
}

// --- 3. GERENTE HISTORY SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteHistoryScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val historyList by viewModel.stayHistory.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = historyList.filter {
        it.clientName.contains(searchQuery, ignoreCase = true) ||
                it.roomNumber.contains(searchQuery, ignoreCase = true) ||
                it.dateString.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial Completo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filtrar por cliente, habitación o fecha...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Habitación ${item.roomNumber} — ${item.clientName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Q${String.format(Locale.US, "%.2f", item.priceCharged)}", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 16.sp)
                            }
                            Text("DPI: ${item.clientDpi ?: "No registrado"} • Personas: ${item.guestCount}", fontSize = 12.sp, color = Color.Gray)
                            Text("Tarifa: ${item.contractedTimeName} • Pago: ${item.paymentMethod}", fontSize = 12.sp)
                            Text("Fecha: ${item.dateString} • Recepción: ${item.receptionistName}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// --- 4. GERENTE SUPPLIES SCREEN (INVENTARIO DE INSUMOS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteSuppliesScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val supplies by viewModel.supplies.collectAsStateWithLifecycle()
    val lowStockSupplies by viewModel.lowStockSupplies.collectAsStateWithLifecycle()
    var editingSupply by remember { mutableStateOf<SupplyEntity?>(null) }
    var showAddSupplyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario de Insumos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showAddSupplyDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelGold)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nuevo Insumo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Automated Low Stock Inventory Alert Banner
            if (lowStockSupplies.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠ ALERTA DE INVENTARIO BAJO (${lowStockSupplies.size} insumos)",
                                color = StatusRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Los siguientes artículos han caído por debajo del umbral mínimo de seguridad:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        lowStockSupplies.forEach { lowSupply ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("• ${lowSupply.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Stock actual: ${lowSupply.stockCurrent} ${lowSupply.unit} (Mínimo: ${lowSupply.stockMinimum})", fontSize = 11.sp, color = StatusRed)
                                }
                                Button(
                                    onClick = { viewModel.restockSupply(lowSupply.id, 50.0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reabastecer +50", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = HotelNavy.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ℹ Los insumos configurados se descuentan automáticamente del inventario al finalizar cada hospedaje.",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(supplies, key = { it.id }) { supply ->
                    val isLowStock = supply.stockCurrent <= supply.stockMinimum
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLowStock) StatusRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(supply.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (isLowStock) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.height(16.dp))
                                        Text(" ¡Bajo Stock!", color = StatusRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Existencias: ${supply.stockCurrent} ${supply.unit} (Mín: ${supply.stockMinimum})", fontSize = 12.sp)
                                Text("Descuento auto por hospedaje: ${supply.autoDeductQuantityPerStay} ${supply.unit}", fontSize = 12.sp, color = HotelNavy, fontWeight = FontWeight.Medium)
                            }
                            Row {
                                IconButton(onClick = { editingSupply = supply }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = HotelNavy)
                                }
                                IconButton(onClick = { viewModel.deleteSupply(supply.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSupplyDialog || editingSupply != null) {
        val isEdit = editingSupply != null
        var name by remember { mutableStateOf(editingSupply?.name ?: "") }
        var unit by remember { mutableStateOf(editingSupply?.unit ?: "Pieza") }
        var stockCurrentText by remember { mutableStateOf(editingSupply?.stockCurrent?.toString() ?: "50.0") }
        var stockMinText by remember { mutableStateOf(editingSupply?.stockMinimum?.toString() ?: "10.0") }
        var autoDeductText by remember { mutableStateOf(editingSupply?.autoDeductQuantityPerStay?.toString() ?: "1.0") }

        AlertDialog(
            onDismissRequest = {
                showAddSupplyDialog = false
                editingSupply = null
            },
            title = { Text(if (isEdit) "Editar Insumo" else "Nuevo Insumo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Insumo (e.g. Jabón, Papel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unidad (e.g. Pieza, Rollo, Botella)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stockCurrentText,
                        onValueChange = { stockCurrentText = it },
                        label = { Text("Existencia Actual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stockMinText,
                        onValueChange = { stockMinText = it },
                        label = { Text("Stock Mínimo para Alerta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = autoDeductText,
                        onValueChange = { autoDeductText = it },
                        label = { Text("Cantidad a descontar por hospedaje") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val stock = stockCurrentText.toDoubleOrNull() ?: 50.0
                        val min = stockMinText.toDoubleOrNull() ?: 10.0
                        val deduct = autoDeductText.toDoubleOrNull() ?: 1.0
                        if (name.isNotBlank()) {
                            val newSupply = SupplyEntity(
                                id = editingSupply?.id ?: 0L,
                                name = name,
                                unit = unit,
                                stockCurrent = stock,
                                stockMinimum = min,
                                autoDeductQuantityPerStay = deduct
                            )
                            viewModel.saveSupply(newSupply)
                            showAddSupplyDialog = false
                            editingSupply = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) { Text("Guardar") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddSupplyDialog = false
                    editingSupply = null
                }) { Text("Cancelar") }
            }
        )
    }
}

// --- 5. GERENTE SALES SCREEN (VENTAS EXTRA) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteSalesScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val saleRecords by viewModel.saleRecords.collectAsStateWithLifecycle()
    var showAddProductDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario de Ventas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showAddProductDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelGold)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nuevo Producto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Productos Disponibles para Venta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Precio: Q${product.price} • Stock: ${product.stock}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = { viewModel.registerSale(product.id, 1, "Efectivo") },
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                            ) {
                                Text("Vender 1")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var priceText by remember { mutableStateOf("10.0") }
        var costText by remember { mutableStateOf("5.0") }
        var stockText by remember { mutableStateOf("20") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Nuevo Producto de Venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Producto") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Precio Venta (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("Costo (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = stockText, onValueChange = { stockText = it }, label = { Text("Stock Inicial") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = priceText.toDoubleOrNull() ?: 10.0
                        val c = costText.toDoubleOrNull() ?: 5.0
                        val s = stockText.toIntOrNull() ?: 20
                        if (name.isNotBlank()) {
                            viewModel.saveProduct(ProductEntity(name = name, price = p, costPrice = c, stock = s))
                            showAddProductDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) { Text("Guardar") }
            },
            dismissButton = { OutlinedButton(onClick = { showAddProductDialog = false }) { Text("Cancelar") } }
        )
    }
}

// --- 6. GERENTE REPORTS & STATISTICS DASHBOARD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteReportsScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.stayHistory.collectAsStateWithLifecycle()
    val sales by viewModel.saleRecords.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Estadísticas Ventas", "Ocupación Hotelera")

    val totalStaysRevenue = history.sumOf { it.priceCharged }
    val totalSalesRevenue = sales.sumOf { it.totalPrice }
    val grandTotalRevenue = totalStaysRevenue + totalSalesRevenue

    // Payment methods breakdown
    val cashTotal = invoices.filter { !it.isVoided && it.paymentMethod.contains("Efectivo", ignoreCase = true) }.sumOf { it.totalAmount }
    val cardTotal = invoices.filter { !it.isVoided && it.paymentMethod.contains("Tarjeta", ignoreCase = true) }.sumOf { it.totalAmount }
    val transferTotal = invoices.filter { !it.isVoided && (it.paymentMethod.contains("Transferencia", ignoreCase = true) || it.paymentMethod.contains("Depósito", ignoreCase = true)) }.sumOf { it.totalAmount }
    val otherTotal = (grandTotalRevenue - (cashTotal + cardTotal + transferTotal)).coerceAtLeast(0.0)

    // Occupancy statistics
    val totalRoomsCount = rooms.size.coerceAtLeast(1)
    val occupiedRoomsCount = rooms.count { it.status == com.example.data.entities.RoomStatus.OCUPADA }
    val availableRoomsCount = rooms.count { it.status == com.example.data.entities.RoomStatus.DISPONIBLE }
    val cleaningRoomsCount = rooms.count { it.status == com.example.data.entities.RoomStatus.PENDIENTE_LIMPIEZA || it.status == com.example.data.entities.RoomStatus.EN_LIMPIEZA }
    val maintenanceRoomsCount = rooms.count { it.status == com.example.data.entities.RoomStatus.RESERVADA }
    val occupancyRate = (occupiedRoomsCount.toFloat() / totalRoomsCount.toFloat()) * 100f

    val context = androidx.compose.ui.platform.LocalContext.current
    var showExportPdfDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Módulo Gerente • Reportes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Estadísticas de Ventas y Ocupación", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    Button(
                        onClick = { showExportPdfDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelGold, contentColor = HotelNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HotelNavy
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // --- GENERAL SUMMARY TAB ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = HotelNavy),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("INGRESOS TOTALES ACUMULADOS", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Surface(color = HotelGold, shape = RoundedCornerShape(4.dp)) {
                                        Text("GERENTE", color = HotelNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Q${String.format(Locale.US, "%.2f", grandTotalRevenue)}", color = HotelGold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Hospedajes", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("Q${String.format(Locale.US, "%.2f", totalStaysRevenue)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column {
                                        Text("Ventas Extra", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("Q${String.format(Locale.US, "%.2f", totalSalesRevenue)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column {
                                        Text("Tasa Ocupación", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("${String.format(Locale.US, "%.1f", occupancyRate)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HotelGold)
                                    }
                                }
                            }
                        }

                        // Chart: Revenue Breakdown Bar Chart
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = HotelNavy)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Distribución de Ingresos por Fuente", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                val maxRev = grandTotalRevenue.coerceAtLeast(1.0)
                                val stayPct = (totalStaysRevenue / maxRev).toFloat()
                                val salesPct = (totalSalesRevenue / maxRev).toFloat()

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Ingresos por Hospedaje", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Q${String.format(Locale.US, "%.2f", totalStaysRevenue)} (${String.format(Locale.US, "%.1f", stayPct * 100)}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = stayPct,
                                            modifier = Modifier.fillMaxWidth().height(10.dp),
                                            color = HotelNavy,
                                            trackColor = Color.LightGray.copy(alpha = 0.3f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Ingresos por Ventas de Insumos / Tienda", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Q${String.format(Locale.US, "%.2f", totalSalesRevenue)} (${String.format(Locale.US, "%.1f", salesPct * 100)}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HotelGold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = salesPct,
                                            modifier = Modifier.fillMaxWidth().height(10.dp),
                                            color = HotelGold,
                                            trackColor = Color.LightGray.copy(alpha = 0.3f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }

                        // Summary Statistics Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Hospedajes Totales", fontSize = 11.sp, color = Color.Gray)
                                    Text("${history.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                                    Text("Registros en historial", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Ventas Directas", fontSize = 11.sp, color = Color.Gray)
                                    Text("${sales.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HotelGold)
                                    Text("Consumos registrados", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    1 -> {
                        // --- SALES & PAYMENT METHODS TAB ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = HotelNavy)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Desglose por Métodos de Pago", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                val totalPay = (cashTotal + cardTotal + transferTotal + otherTotal).coerceAtLeast(1.0)
                                val cashPct = (cashTotal / totalPay).toFloat()
                                val cardPct = (cardTotal / totalPay).toFloat()
                                val transferPct = (transferTotal / totalPay).toFloat()

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PaymentMethodProgressRow("Efectivo", cashTotal, cashPct, StatusGreen)
                                    PaymentMethodProgressRow("Tarjeta de Crédito / Débito", cardTotal, cardPct, HotelNavy)
                                    PaymentMethodProgressRow("Transferencia / Depósito", transferTotal, transferPct, HotelGold)
                                    if (otherTotal > 0) {
                                        val otherPct = (otherTotal / totalPay).toFloat()
                                        PaymentMethodProgressRow("Otros / No especificado", otherTotal, otherPct, Color.Gray)
                                    }
                                }
                            }
                        }

                        // Recent Sales Transactions
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Últimas Ventas Directas", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                                Spacer(modifier = Modifier.height(8.dp))

                                if (sales.isEmpty()) {
                                    Text("No hay registros de ventas directas aún.", fontSize = 12.sp, color = Color.Gray)
                                } else {
                                    sales.take(5).forEach { sale ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(sale.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Cant: ${sale.quantity} • Registrado por: ${sale.registeredBy} • Pago: ${sale.paymentMethod}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Text("Q${String.format(Locale.US, "%.2f", sale.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusGreen)
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // --- HOTEL OCCUPANCY TAB WITH CUSTOM DONUT CHART ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Estado Actual de Habitaciones", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Donut Chart Rendering using Canvas
                                Box(
                                    modifier = Modifier.size(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(170.dp)) {
                                        val strokeWidth = 28.dp.toPx()
                                        var startAngle = -90f

                                        val slices = listOf(
                                            occupiedRoomsCount to StatusRed,
                                            availableRoomsCount to StatusGreen,
                                            cleaningRoomsCount to StatusYellow,
                                            maintenanceRoomsCount to Color.Gray
                                        )

                                        slices.forEach { (count, color) ->
                                            if (count > 0) {
                                                val sweepAngle = (count.toFloat() / totalRoomsCount.toFloat()) * 360f
                                                drawArc(
                                                    color = color,
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle - 2f, // gap
                                                    useCenter = false,
                                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                )
                                                startAngle += sweepAngle
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${String.format(Locale.US, "%.0f", occupancyRate)}%", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                                        Text("Ocupación", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Donut Chart Legend
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OccupancyLegendItem("Ocupadas", occupiedRoomsCount, totalRoomsCount, StatusRed)
                                    OccupancyLegendItem("Disponibles", availableRoomsCount, totalRoomsCount, StatusGreen)
                                    OccupancyLegendItem("Pendientes de Limpieza", cleaningRoomsCount, totalRoomsCount, StatusYellow)
                                    OccupancyLegendItem("Mantenimiento", maintenanceRoomsCount, totalRoomsCount, Color.Gray)
                                }
                            }
                        }

                        // 30-Day Occupancy & Revenue Interactive Recharts-like Dashboard
                        com.example.ui.components.ThirtyDayOccupancyRevenueDashboard(
                            stayHistory = history,
                            invoices = invoices,
                            saleRecords = sales,
                            totalRoomsCount = totalRoomsCount
                        )

                        // Room Performance Cards
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Resumen de Capacidad", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Capacidad Total: $totalRoomsCount habitaciones", fontSize = 13.sp)
                                Text("Habitaciones Generando Ingreso: $occupiedRoomsCount", fontSize = 13.sp, color = StatusRed, fontWeight = FontWeight.Medium)
                                Text("Habitaciones Listas para Renta: $availableRoomsCount", fontSize = 13.sp, color = StatusGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportPdfDialog) {
        AlertDialog(
            onDismissRequest = { showExportPdfDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = HotelNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Reporte PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Seleccione el tipo de reporte que desea exportar e imprimir en formato PDF:")

                    Button(
                        onClick = {
                            showExportPdfDialog = false
                            val pdf = com.example.util.PdfReportExporter.generateRevenueReportPdf(
                                context = context,
                                totalStaysRevenue = totalStaysRevenue,
                                totalSalesRevenue = totalSalesRevenue,
                                grandTotalRevenue = grandTotalRevenue,
                                cashTotal = cashTotal,
                                cardTotal = cardTotal,
                                transferTotal = transferTotal,
                                historyList = history,
                                salesList = sales
                            )
                            if (pdf != null) {
                                android.widget.Toast.makeText(context, "Reporte de Ingresos PDF generado exitosamente", android.widget.Toast.LENGTH_SHORT).show()
                                com.example.util.PdfReportExporter.openOrSharePdf(context, pdf)
                            } else {
                                android.widget.Toast.makeText(context, "Error al generar archivo PDF", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("1. Reporte de Ingresos y Ventas (PDF)")
                    }

                    Button(
                        onClick = {
                            showExportPdfDialog = false
                            val pdf = com.example.util.PdfReportExporter.generateOccupancyReportPdf(
                                context = context,
                                rooms = rooms
                            )
                            if (pdf != null) {
                                android.widget.Toast.makeText(context, "Reporte de Ocupación PDF generado exitosamente", android.widget.Toast.LENGTH_SHORT).show()
                                com.example.util.PdfReportExporter.openOrSharePdf(context, pdf)
                            } else {
                                android.widget.Toast.makeText(context, "Error al generar archivo PDF", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("2. Reporte de Ocupación y Estado (PDF)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showExportPdfDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodProgressRow(
    title: String,
    amount: Double,
    percentage: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("Q${String.format(Locale.US, "%.2f", amount)} (${String.format(Locale.US, "%.1f", percentage * 100)}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = percentage,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = barColor,
            trackColor = Color.LightGray.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun OccupancyLegendItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = if (total > 0) (count.toFloat() / total.toFloat()) * 100f else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(12.dp)) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("$count habitaciones (${String.format(Locale.US, "%.0f", pct)}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}


// --- 7. GERENTE SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteSettingsScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val settingsList by viewModel.settings.collectAsStateWithLifecycle()
    val roomsList by viewModel.rooms.collectAsStateWithLifecycle()
    val settingsMap = remember(settingsList) { settingsList.associate { it.key to it.value } }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Datos del Hotel", "Facturas y Comprobantes", "Configuración del Hotel")

    // --- 1. DATOS DEL HOTEL STATES ---
    var hotelName by remember(settingsMap) { mutableStateOf(settingsMap["hotel_name"] ?: "Hotel Rivera") }
    var hotelLogoText by remember(settingsMap) { mutableStateOf(settingsMap["hotel_logo_text"] ?: "HR") }
    var hotelAddress by remember(settingsMap) { mutableStateOf(settingsMap["hotel_address"] ?: "Calle Principal 12-34") }
    var hotelMunicipio by remember(settingsMap) { mutableStateOf(settingsMap["hotel_municipio"] ?: "Quetzaltenango") }
    var hotelDepartamento by remember(settingsMap) { mutableStateOf(settingsMap["hotel_departamento"] ?: "Quetzaltenango") }
    var hotelCountry by remember(settingsMap) { mutableStateOf(settingsMap["hotel_country"] ?: "Guatemala") }
    var hotelZip by remember(settingsMap) { mutableStateOf(settingsMap["hotel_zip"] ?: "09001") }
    var hotelPhone1 by remember(settingsMap) { mutableStateOf(settingsMap["hotel_phone1"] ?: "(502) 7761-0000") }
    var hotelPhone2 by remember(settingsMap) { mutableStateOf(settingsMap["hotel_phone2"] ?: "(502) 7761-0001") }
    var hotelWhatsapp by remember(settingsMap) { mutableStateOf(settingsMap["hotel_whatsapp"] ?: "+502 5555 1234") }
    var hotelEmail by remember(settingsMap) { mutableStateOf(settingsMap["hotel_email"] ?: "contacto@hotelrivera.com") }
    var hotelWebsite by remember(settingsMap) { mutableStateOf(settingsMap["hotel_website"] ?: "www.hotelrivera.com") }
    var hotelFacebook by remember(settingsMap) { mutableStateOf(settingsMap["hotel_facebook"] ?: "facebook.com/hotelrivera") }
    var hotelInstagram by remember(settingsMap) { mutableStateOf(settingsMap["hotel_instagram"] ?: "@hotelrivera") }
    var hotelHours by remember(settingsMap) { mutableStateOf(settingsMap["hotel_hours"] ?: "Atención 24 Horas / 7 Días") }
    var currencySymbol by remember(settingsMap) { mutableStateOf(settingsMap["currency_symbol"] ?: "Q") }
    var timeZone by remember(settingsMap) { mutableStateOf(settingsMap["time_zone"] ?: "GMT-6 (Guatemala)") }
    var appLanguage by remember(settingsMap) { mutableStateOf(settingsMap["app_language"] ?: "Español") }

    // --- 2. FACTURAS Y COMPROBANTES STATES ---
    var invoiceHotelName by remember(settingsMap) { mutableStateOf(settingsMap["invoice_hotel_name"] ?: hotelName) }
    var invoiceLogoText by remember(settingsMap) { mutableStateOf(settingsMap["invoice_logo_text"] ?: hotelLogoText) }
    var invoiceAddress by remember(settingsMap) { mutableStateOf(settingsMap["invoice_address"] ?: hotelAddress) }
    var invoicePhone by remember(settingsMap) { mutableStateOf(settingsMap["invoice_phone"] ?: hotelPhone1) }
    var invoiceWhatsapp by remember(settingsMap) { mutableStateOf(settingsMap["invoice_whatsapp"] ?: hotelWhatsapp) }
    var invoiceNit by remember(settingsMap) { mutableStateOf(settingsMap["invoice_nit"] ?: "1234567-8") }
    var invoiceThankyouMsg by remember(settingsMap) { mutableStateOf(settingsMap["invoice_thankyou_msg"] ?: "¡Gracias por su preferencia!") }
    var invoiceFooterMsg by remember(settingsMap) { mutableStateOf(settingsMap["invoice_footer_msg"] ?: "Conserve este comprobante para cualquier reclamo.") }
    var invoiceStartNumber by remember(settingsMap) { mutableStateOf(settingsMap["invoice_start_number"] ?: "0001") }
    var invoicePrefix by remember(settingsMap) { mutableStateOf(settingsMap["invoice_prefix"] ?: "FAC-") }

    // Toggles
    var showLogo by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_logo"]?.toBooleanStrictOrNull() ?: true) }
    var showAddress by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_address"]?.toBooleanStrictOrNull() ?: true) }
    var showPhone by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_phone"]?.toBooleanStrictOrNull() ?: true) }
    var showNit by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_nit"]?.toBooleanStrictOrNull() ?: true) }
    var showReceptionist by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_receptionist"]?.toBooleanStrictOrNull() ?: true) }
    var showPaymentMethod by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_payment_method"]?.toBooleanStrictOrNull() ?: true) }
    var showNotes by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_notes"]?.toBooleanStrictOrNull() ?: true) }
    var showDateTime by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_date"]?.toBooleanStrictOrNull() ?: true) }
    var showQrCode by remember(settingsMap) { mutableStateOf(settingsMap["invoice_show_qr"]?.toBooleanStrictOrNull() ?: true) }

    // --- 3. CONFIGURACIÓN DEL HOTEL STATES ---
    var totalRoomsInput by remember(roomsList) { mutableStateOf(roomsList.size.toString()) }
    var managerPinInput by remember(settingsMap) { mutableStateOf(settingsMap["manager_pin"] ?: "1234") }
    var alertTimeMinutesInput by remember(settingsMap) { mutableStateOf(settingsMap["alert_time_minutes"] ?: "15") }
    var roomPrefixInput by remember(settingsMap) { mutableStateOf(settingsMap["room_prefix"] ?: "Habitación ") }

    var savedFeedbackMessage by remember { mutableStateOf<String?>(null) }

    fun showSavedToast(msg: String) {
        savedFeedbackMessage = msg
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ajustes del Sistema (Gerencia)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Configuración General y Personalización", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HotelNavy
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            if (savedFeedbackMessage != null) {
                Surface(
                    color = StatusGreen.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = savedFeedbackMessage ?: "",
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // ==========================================
                        // TAB 1: DATOS DEL HOTEL
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = HotelNavy)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Información General del Establecimiento", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                                }

                                HorizontalDivider()

                                OutlinedTextField(
                                    value = hotelName,
                                    onValueChange = { hotelName = it },
                                    label = { Text("Nombre del Hotel") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = hotelLogoText,
                                        onValueChange = { hotelLogoText = it },
                                        label = { Text("Iniciales Logo (e.g. HR)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = currencySymbol,
                                        onValueChange = { currencySymbol = it },
                                        label = { Text("Moneda (e.g. Q, $)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = hotelAddress,
                                    onValueChange = { hotelAddress = it },
                                    label = { Text("Dirección") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = hotelMunicipio,
                                        onValueChange = { hotelMunicipio = it },
                                        label = { Text("Municipio") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hotelDepartamento,
                                        onValueChange = { hotelDepartamento = it },
                                        label = { Text("Departamento") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = hotelCountry,
                                        onValueChange = { hotelCountry = it },
                                        label = { Text("País") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hotelZip,
                                        onValueChange = { hotelZip = it },
                                        label = { Text("Código Postal") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = hotelPhone1,
                                        onValueChange = { hotelPhone1 = it },
                                        label = { Text("Teléfono Principal") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hotelPhone2,
                                        onValueChange = { hotelPhone2 = it },
                                        label = { Text("Teléfono Secundario") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = hotelWhatsapp,
                                        onValueChange = { hotelWhatsapp = it },
                                        label = { Text("WhatsApp") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hotelEmail,
                                        onValueChange = { hotelEmail = it },
                                        label = { Text("Correo Electrónico") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = hotelWebsite,
                                        onValueChange = { hotelWebsite = it },
                                        label = { Text("Sitio Web") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hotelHours,
                                        onValueChange = { hotelHours = it },
                                        label = { Text("Horario de Atención") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = hotelFacebook,
                                        onValueChange = { hotelFacebook = it },
                                        label = { Text("Facebook") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hotelInstagram,
                                        onValueChange = { hotelInstagram = it },
                                        label = { Text("Instagram") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = timeZone,
                                        onValueChange = { timeZone = it },
                                        label = { Text("Zona Horaria") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = appLanguage,
                                        onValueChange = { appLanguage = it },
                                        label = { Text("Idioma Aplicación") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.saveSetting("hotel_name", hotelName)
                                        viewModel.saveSetting("hotel_logo_text", hotelLogoText)
                                        viewModel.saveSetting("hotel_address", hotelAddress)
                                        viewModel.saveSetting("hotel_municipio", hotelMunicipio)
                                        viewModel.saveSetting("hotel_departamento", hotelDepartamento)
                                        viewModel.saveSetting("hotel_country", hotelCountry)
                                        viewModel.saveSetting("hotel_zip", hotelZip)
                                        viewModel.saveSetting("hotel_phone1", hotelPhone1)
                                        viewModel.saveSetting("hotel_phone2", hotelPhone2)
                                        viewModel.saveSetting("hotel_whatsapp", hotelWhatsapp)
                                        viewModel.saveSetting("hotel_email", hotelEmail)
                                        viewModel.saveSetting("hotel_website", hotelWebsite)
                                        viewModel.saveSetting("hotel_facebook", hotelFacebook)
                                        viewModel.saveSetting("hotel_instagram", hotelInstagram)
                                        viewModel.saveSetting("hotel_hours", hotelHours)
                                        viewModel.saveSetting("currency_symbol", currencySymbol)
                                        viewModel.saveSetting("time_zone", timeZone)
                                        viewModel.saveSetting("app_language", appLanguage)
                                        showSavedToast("¡Datos del Hotel guardados correctamente!")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar Datos del Hotel", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    1 -> {
                        // ==========================================
                        // TAB 2: FACTURAS Y COMPROBANTES
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = HotelNavy)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Personalización de Comprobante / Factura", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                                }

                                HorizontalDivider()

                                Text("Campos Editables en la Impresión", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                OutlinedTextField(
                                    value = invoiceHotelName,
                                    onValueChange = { invoiceHotelName = it },
                                    label = { Text("Nombre del Hotel en Comprobante") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = invoiceLogoText,
                                        onValueChange = { invoiceLogoText = it },
                                        label = { Text("Logo Iniciales") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = invoiceNit,
                                        onValueChange = { invoiceNit = it },
                                        label = { Text("NIT") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = invoiceAddress,
                                    onValueChange = { invoiceAddress = it },
                                    label = { Text("Dirección del Comprobante") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = invoicePhone,
                                        onValueChange = { invoicePhone = it },
                                        label = { Text("Teléfono Comprobante") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = invoiceWhatsapp,
                                        onValueChange = { invoiceWhatsapp = it },
                                        label = { Text("WhatsApp Comprobante") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = invoicePrefix,
                                        onValueChange = { invoicePrefix = it },
                                        label = { Text("Prefijo Numeración") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = invoiceStartNumber,
                                        onValueChange = { invoiceStartNumber = it },
                                        label = { Text("Número Inicial") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = invoiceThankyouMsg,
                                    onValueChange = { invoiceThankyouMsg = it },
                                    label = { Text("Mensaje de Agradecimiento") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = invoiceFooterMsg,
                                    onValueChange = { invoiceFooterMsg = it },
                                    label = { Text("Mensaje al Pie del Comprobante") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider()

                                Text("Opciones de Visibilidad e Impresión", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                SettingSwitchRow("Mostrar Logotipo / Iniciales", showLogo) { showLogo = it }
                                SettingSwitchRow("Mostrar Dirección", showAddress) { showAddress = it }
                                SettingSwitchRow("Mostrar Teléfono / WhatsApp", showPhone) { showPhone = it }
                                SettingSwitchRow("Mostrar NIT", showNit) { showNit = it }
                                SettingSwitchRow("Mostrar Nombre del Recepcionista", showReceptionist) { showReceptionist = it }
                                SettingSwitchRow("Mostrar Método de Pago", showPaymentMethod) { showPaymentMethod = it }
                                SettingSwitchRow("Mostrar Observaciones / Notas", showNotes) { showNotes = it }
                                SettingSwitchRow("Mostrar Fecha y Hora", showDateTime) { showDateTime = it }
                                SettingSwitchRow("Mostrar Código QR de Verificación", showQrCode) { showQrCode = it }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.saveSetting("invoice_hotel_name", invoiceHotelName)
                                        viewModel.saveSetting("invoice_logo_text", invoiceLogoText)
                                        viewModel.saveSetting("invoice_address", invoiceAddress)
                                        viewModel.saveSetting("invoice_phone", invoicePhone)
                                        viewModel.saveSetting("invoice_whatsapp", invoiceWhatsapp)
                                        viewModel.saveSetting("invoice_nit", invoiceNit)
                                        viewModel.saveSetting("invoice_thankyou_msg", invoiceThankyouMsg)
                                        viewModel.saveSetting("invoice_footer_msg", invoiceFooterMsg)
                                        viewModel.saveSetting("invoice_start_number", invoiceStartNumber)
                                        viewModel.saveSetting("invoice_prefix", invoicePrefix)

                                        viewModel.saveSetting("invoice_show_logo", showLogo.toString())
                                        viewModel.saveSetting("invoice_show_address", showAddress.toString())
                                        viewModel.saveSetting("invoice_show_phone", showPhone.toString())
                                        viewModel.saveSetting("invoice_show_nit", showNit.toString())
                                        viewModel.saveSetting("invoice_show_receptionist", showReceptionist.toString())
                                        viewModel.saveSetting("invoice_show_payment_method", showPaymentMethod.toString())
                                        viewModel.saveSetting("invoice_show_notes", showNotes.toString())
                                        viewModel.saveSetting("invoice_show_date", showDateTime.toString())
                                        viewModel.saveSetting("invoice_show_qr", showQrCode.toString())

                                        showSavedToast("¡Diseño de Comprobante guardado!")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar Personalización de Comprobantes", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // --- LIVE RECEIPT PREVIEW BOX ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = HotelGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Vista Previa del Comprobante", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Surface(color = HotelGold.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text("EN TIEMPO REAL", color = HotelNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Render Customized Receipt Preview Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (showLogo) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = HotelNavy,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(invoiceLogoText, color = HotelGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        Text(
                                            text = invoiceHotelName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = HotelNavy
                                        )

                                        if (showAddress) {
                                            Text(text = invoiceAddress, fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (showPhone) {
                                            Text(text = "Tel: $invoicePhone | WA: $invoiceWhatsapp", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (showNit) {
                                            Text(text = "NIT: $invoiceNit", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("COMPROBANTE No:", fontSize = 10.sp, color = Color.Gray)
                                                Text(
                                                    "$invoicePrefix$invoiceStartNumber",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (showDateTime) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("FECHA Y HORA:", fontSize = 10.sp, color = Color.Gray)
                                                    Text("2026-07-28 16:15", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        ReceiptDetailRow("Cliente:", "Juan Pérez")
                                        ReceiptDetailRow("Habitación:", "Habitación 101")
                                        ReceiptDetailRow("Tiempo Contratado:", "3 Horas")

                                        if (showReceptionist) {
                                            ReceiptDetailRow("Atendido Por:", "Recepción Turno A")
                                        }
                                        if (showPaymentMethod) {
                                            ReceiptDetailRow("Método de Pago:", "Efectivo")
                                        }
                                        if (showNotes) {
                                            ReceiptDetailRow("Observaciones:", "Ninguna")
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("TOTAL PAGADO:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HotelNavy)
                                            Text("$currencySymbol 150.00", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = HotelNavy)
                                        }

                                        if (showQrCode) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(48.dp), tint = HotelNavy)
                                            Text("Escanear para validar", fontSize = 9.sp, color = Color.Gray)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = invoiceThankyouMsg,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )

                                        if (invoiceFooterMsg.isNotBlank()) {
                                            Text(
                                                text = invoiceFooterMsg,
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ==========================================
                        // TAB 3: CONFIGURACIÓN DEL HOTEL
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Hotel, contentDescription = null, tint = HotelNavy)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capacidad y Reglas Operativas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                                }

                                HorizontalDivider()

                                Text("Capacidad de Habitaciones", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                OutlinedTextField(
                                    value = totalRoomsInput,
                                    onValueChange = { totalRoomsInput = it },
                                    label = { Text("Cantidad Total de Habitaciones") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = roomPrefixInput,
                                    onValueChange = { roomPrefixInput = it },
                                    label = { Text("Prefijo Nombre de Habitaciones (e.g. Habitación )") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        val count = totalRoomsInput.toIntOrNull()
                                        if (count != null && count > 0) {
                                            viewModel.setTotalRooms(count)
                                            viewModel.saveSetting("room_prefix", roomPrefixInput)
                                            showSavedToast("¡Habitaciones configuradas a $count!")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Hotel, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Actualizar Inventario de Habitaciones")
                                }

                                HorizontalDivider()

                                Text("Alertas y Seguridad", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                OutlinedTextField(
                                    value = alertTimeMinutesInput,
                                    onValueChange = { alertTimeMinutesInput = it },
                                    label = { Text("Tiempo de Alerta Antes de Finalizar (Minutos)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = managerPinInput,
                                    onValueChange = { managerPinInput = it },
                                    label = { Text("PIN de Acceso Gerente (4 dígitos)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider()

                                Text("Colores e Indicadores de Estado", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = StatusGreen, modifier = Modifier.size(16.dp)) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Disponible (Verde)", fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = StatusRed, modifier = Modifier.size(16.dp)) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ocupada (Rojo)", fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = StatusYellow, modifier = Modifier.size(16.dp)) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pendiente de Limpieza (Amarillo)", fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = Color.Gray, modifier = Modifier.size(16.dp)) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mantenimiento / Reservada (Gris)", fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.saveSetting("alert_time_minutes", alertTimeMinutesInput)
                                        viewModel.saveSetting("manager_pin", managerPinInput)
                                        showSavedToast("¡Alertas y PIN de Gerente actualizados!")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar Configuración del Hotel", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


// --- 8. GERENTE USERS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteUsersScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    var showAddUserDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showAddUserDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelGold)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nuevo Usuario")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users, key = { it.id }) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Rol: ${user.role} • Usuario: ${user.username}", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.deleteUser(user.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        var username by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }
        var pinCode by remember { mutableStateOf("0000") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Crear Usuario de Recepción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Nombre de Usuario") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pinCode, onValueChange = { pinCode = it }, label = { Text("PIN de Acceso") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fullName.isNotBlank()) {
                            val uName = username.ifBlank { "user" }
                            viewModel.saveUser(
                                UserEntity(
                                    username = uName,
                                    fullName = fullName,
                                    pinCode = pinCode,
                                    passwordHash = com.example.utils.SecurityUtils.hashPassword(pinCode),
                                    role = "RECEPCION",
                                    isActive = true,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            showAddUserDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) { Text("Guardar") }
            },
            dismissButton = { OutlinedButton(onClick = { showAddUserDialog = false }) { Text("Cancelar") } }
        )
    }
}

// --- 9. GERENTE BACKUP SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteBackupScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var backupJsonText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo de Información", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = {
                    val json = viewModel.exportBackupJson()
                    backupJsonText = json
                    clipboardManager.setText(AnnotatedString(json))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("Generar y Copiar Copia de Seguridad JSON")
            }

            if (backupJsonText.isNotBlank()) {
                OutlinedTextField(
                    value = backupJsonText,
                    onValueChange = { backupJsonText = it },
                    label = { Text("Copia de Seguridad (JSON)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Button(
                    onClick = { viewModel.importBackupJson(backupJsonText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HotelGold)
                ) {
                    Text("Restaurar Datos desde JSON")
                }
            }
        }
    }
}

// --- 10. GERENTE INVOICES SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteInvoicesScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedInvoiceForView by remember { mutableStateOf<com.example.data.entities.InvoiceEntity?>(null) }
    var invoiceToVoid by remember { mutableStateOf<com.example.data.entities.InvoiceEntity?>(null) }
    var voidReasonText by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }

    val filteredInvoices = invoices.filter { inv ->
        inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.clientName.contains(searchQuery, ignoreCase = true) ||
                inv.dateString.contains(searchQuery, ignoreCase = true) ||
                inv.roomNumber.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Facturas y Comprobantes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar Facturas")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar factura por No., cliente o fecha...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron facturas registradas.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = invoice.invoiceNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = HotelNavy
                                    )
                                    Surface(
                                        color = if (invoice.isVoided) StatusRed.copy(alpha = 0.15f) else StatusGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (invoice.isVoided) "ANULADA" else "VÁLIDA",
                                            color = if (invoice.isVoided) StatusRed else StatusGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Cliente: ${invoice.clientName} • Hab: ${invoice.roomNumber}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Fecha: ${invoice.dateString} ${invoice.timeString} • Método: ${invoice.paymentMethod}", fontSize = 12.sp, color = Color.Gray)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total: Q${String.format(java.util.Locale.US, "%.2f", invoice.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = HotelNavy
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { selectedInvoiceForView = invoice },
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Ver / Reimprimir", fontSize = 11.sp)
                                        }

                                        if (!invoice.isVoided) {
                                            Button(
                                                onClick = {
                                                    invoiceToVoid = invoice
                                                    voidReasonText = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("Anular", fontSize = 11.sp)
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

    selectedInvoiceForView?.let { inv ->
        InvoiceDialog(
            invoice = inv,
            onDismiss = { selectedInvoiceForView = null },
            onVoidRequested = {
                invoiceToVoid = inv
                selectedInvoiceForView = null
            }
        )
    }

    invoiceToVoid?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToVoid = null },
            title = { Text("Anular Factura ${inv.invoiceNumber}", fontWeight = FontWeight.Bold, color = StatusRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Está seguro de anular esta factura? Esta acción quedará registrada en la auditoría.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = voidReasonText,
                        onValueChange = { voidReasonText = it },
                        label = { Text("Motivo de la anulación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReasonText.isNotBlank()) {
                            viewModel.voidInvoice(inv.id, voidReasonText)
                            invoiceToVoid = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Confirmar Anulación")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { invoiceToVoid = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showExportDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExportRequested = { category, format ->
                val csvContent = viewModel.generateCsvExport("facturas")
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Exportacion Facturas - Hotel Rivera")
                    putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Exportar Facturas"))
            }
        )
    }
}

// --- 11. GERENTE AUDIT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteAuditScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }

    val filteredLogs = auditLogs.filter { log ->
        log.username.contains(searchQuery, ignoreCase = true) ||
                log.action.contains(searchQuery, ignoreCase = true) ||
                log.details.contains(searchQuery, ignoreCase = true) ||
                log.dateString.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Auditoría", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar Auditoría")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar en auditoría por usuario, acción o fecha...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay registros de auditoría aún.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = HotelNavy
                                    )
                                    Text(
                                        text = "${log.dateString} ${log.timeString}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Usuario: ${log.username}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExportRequested = { category, format ->
                val csvContent = viewModel.generateCsvExport("auditoria")
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Exportacion Auditoria - Hotel Rivera")
                    putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Exportar Auditoría"))
            }
        )
    }
}
