package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.ui.viewmodel.DeviceLinkingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkingHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceLinkingViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val linkedDevices by viewModel.linkedDevices.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var deviceToDelete by remember { mutableStateOf<DeviceEntity?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // Toast/Snackbar on feedback
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Filter logic
    val filteredList = remember(linkedDevices, searchQuery, selectedStatusFilter) {
        linkedDevices.filter { device ->
            val matchesSearch = searchQuery.isBlank() ||
                device.name.contains(searchQuery, ignoreCase = true) ||
                device.userAssigned.contains(searchQuery, ignoreCase = true) ||
                device.deviceId.contains(searchQuery, ignoreCase = true) ||
                (device.ipAddress?.contains(searchQuery, ignoreCase = true) == true)

            val matchesStatus = when (selectedStatusFilter) {
                "CONNECTED" -> device.connectionStatus == DeviceConnectionStatus.CONNECTED
                "DISCONNECTED" -> device.connectionStatus == DeviceConnectionStatus.DISCONNECTED
                "PENDING" -> device.connectionStatus == DeviceConnectionStatus.PENDING
                else -> true
            }

            matchesSearch && matchesStatus
        }
    }

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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Historial de Vinculaciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Registros de dispositivos guardados en base de datos",
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
                    if (linkedDevices.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirmDialog = true },
                            modifier = Modifier.testTag("clear_all_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Borrar todo el historial",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
            // Metrics / Stats Summary Banner
            HistoryStatsOverview(
                devices = linkedDevices,
                modifier = Modifier.padding(16.dp)
            )

            // Search and Filters Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, usuario, ID o IP...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    singleLine = true
                )

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == "ALL",
                        onClick = { selectedStatusFilter = "ALL" },
                        label = { Text("Todos (${linkedDevices.size})") },
                        modifier = Modifier.testTag("filter_all_chip")
                    )

                    FilterChip(
                        selected = selectedStatusFilter == "CONNECTED",
                        onClick = { selectedStatusFilter = "CONNECTED" },
                        label = {
                            Text("Conectados (${linkedDevices.count { it.connectionStatus == DeviceConnectionStatus.CONNECTED }})")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5E9),
                            selectedLabelColor = Color(0xFF1B5E20)
                        ),
                        modifier = Modifier.testTag("filter_connected_chip")
                    )

                    FilterChip(
                        selected = selectedStatusFilter == "DISCONNECTED",
                        onClick = { selectedStatusFilter = "DISCONNECTED" },
                        label = {
                            Text("Desconectados (${linkedDevices.count { it.connectionStatus == DeviceConnectionStatus.DISCONNECTED }})")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFB71C1C)
                        ),
                        modifier = Modifier.testTag("filter_disconnected_chip")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // History Records List
            if (filteredList.isEmpty()) {
                EmptyHistoryView(
                    isFilterActive = searchQuery.isNotEmpty() || selectedStatusFilter != "ALL",
                    onClearFilters = {
                        searchQuery = ""
                        selectedStatusFilter = "ALL"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_records_list"),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { it.id }
                    ) { device ->
                        HistoryDeviceCard(
                            device = device,
                            onToggleStatus = {
                                val newStatus = if (device.connectionStatus == DeviceConnectionStatus.CONNECTED) {
                                    DeviceConnectionStatus.DISCONNECTED
                                } else {
                                    DeviceConnectionStatus.CONNECTED
                                }
                                viewModel.updateDeviceStatus(device, newStatus)
                            },
                            onDelete = {
                                deviceToDelete = device
                            },
                            onCopyDetails = {
                                copyToClipboard(
                                    context = context,
                                    label = "Detalles de Dispositivo",
                                    text = "Nombre: ${device.name}\nID: ${device.deviceId}\nUsuario: ${device.userAssigned}\nEstado: ${device.connectionStatus}\nIP: ${device.ipAddress ?: "N/A"}"
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Single Device Deletion Dialog
    deviceToDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar Registro") },
            text = {
                Text("¿Está seguro de que desea eliminar el registro de '${device.name}' (${device.deviceId}) del historial de vinculaciones?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unlinkDevice(device, context)
                        deviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_device_button")
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Clear All History Dialog
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Vaciar Historial Completo") },
            text = {
                Text("Esta acción eliminará permanentemente TODOS los registros de dispositivos vinculados almacenados en la base de datos. ¿Desea continuar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory(context)
                        showClearAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_all_history_button")
                ) {
                    Text("Vaciar Todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HistoryStatsOverview(
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val total = devices.size
    val connectedCount = devices.count { it.connectionStatus == DeviceConnectionStatus.CONNECTED }
    val disconnectedCount = devices.count { it.connectionStatus == DeviceConnectionStatus.DISCONNECTED }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Total Dispositivos",
            value = total.toString(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Conectados",
            value = connectedCount.toString(),
            color = Color(0xFFE8F5E9),
            contentColor = Color(0xFF1B5E20),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Desconectados",
            value = disconnectedCount.toString(),
            color = Color(0xFFFFEBEE),
            contentColor = Color(0xFFB71C1C),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoryDeviceCard(
    device: DeviceEntity,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onCopyDetails: () -> Unit
) {
    val isConnected = device.connectionStatus == DeviceConnectionStatus.CONNECTED
    val formattedDate = remember(device.timestamp) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            sdf.format(Date(device.timestamp))
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header (Icon, Name, Status Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ID: ${device.deviceId}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (isConnected) "CONECTADO" else "DESCONECTADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Details Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Usuario Asignado: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = device.userAssigned,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Registrado el: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!device.ipAddress.isNull_or_blank_safe()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Dirección IP: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = device.ipAddress ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Card Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.testTag("toggle_status_button_${device.id}")
                ) {
                    Text(if (isConnected) "Desconectar" else "Reconectar")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCopyDetails,
                        modifier = Modifier.testTag("copy_device_button_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Datos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_device_button_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Registro",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(
    isFilterActive: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = if (isFilterActive) "No se encontraron coincidencias" else "Sin historial de dispositivos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isFilterActive) {
                    "Intente cambiar la búsqueda o restablecer los filtros aplicados."
                } else {
                    "Aún no hay ningún dispositivo vinculado en la base de datos local."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (isFilterActive) {
                Button(onClick = onClearFilters) {
                    Text("Limpiar Filtros")
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun String?.isNull_or_blank_safe(): Boolean {
    return this == null || this.trim().isEmpty()
}
