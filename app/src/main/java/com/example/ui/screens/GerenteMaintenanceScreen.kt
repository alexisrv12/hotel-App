package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entities.MaintenanceRequestEntity
import com.example.ui.HotelViewModel
import com.example.ui.components.MaintenanceRequestFormDialog
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GerenteMaintenanceScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val maintenanceRequests by viewModel.maintenanceRequests.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Todas", "Pendientes", "En Proceso", "Resueltas", "Con Foto 📷")

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Resolution modal state
    var resolvingRequest by remember { mutableStateOf<MaintenanceRequestEntity?>(null) }
    var technicianName by remember { mutableStateOf("") }
    var resolutionNotes by remember { mutableStateOf("") }
    var repairCostText by remember { mutableStateOf("") }

    // Full photo modal state
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }

    // Filter logic
    val filteredList = maintenanceRequests.filter { req ->
        val matchesTab = when (selectedTab) {
            1 -> req.status == "PENDIENTE"
            2 -> req.status == "EN_REPARACION"
            3 -> req.status == "RESUELTO"
            4 -> !req.photoPath.isNullOrBlank()
            else -> true
        }

        val matchesSearch = searchQuery.isBlank() ||
                req.itemName.contains(searchQuery, ignoreCase = true) ||
                req.roomNumber.contains(searchQuery, ignoreCase = true) ||
                req.description.contains(searchQuery, ignoreCase = true) ||
                req.reportedBy.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategoryFilter == null || req.category == selectedCategoryFilter

        matchesTab && matchesSearch && matchesCategory
    }

    val totalCount = maintenanceRequests.size
    val pendingCount = maintenanceRequests.count { it.status == "PENDIENTE" }
    val inProgressCount = maintenanceRequests.count { it.status == "EN_REPARACION" }
    val resolvedCount = maintenanceRequests.count { it.status == "RESUELTO" }
    val urgentCount = maintenanceRequests.count { it.priority == "Urgente" && it.status != "RESUELTO" }

    if (showCreateDialog) {
        MaintenanceRequestFormDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }

    // Photo viewer dialog
    if (selectedPhotoPath != null) {
        Dialog(
            onDismissRequest = { selectedPhotoPath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val fullBitmap = remember(selectedPhotoPath) {
                        try {
                            BitmapFactory.decodeFile(selectedPhotoPath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (fullBitmap != null) {
                        Image(
                            bitmap = fullBitmap.asImageBitmap(),
                            contentDescription = "Foto ampliada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("No se pudo cargar la imagen", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }

                    IconButton(
                        onClick = { selectedPhotoPath = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }
        }
    }

    // Resolve dialog
    resolvingRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { resolvingRequest = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Engineering, contentDescription = null, tint = StatusGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resolver Avería: ${req.itemName}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Habitación / Ubicación: ${req.roomNumber}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = technicianName,
                        onValueChange = { technicianName = it },
                        label = { Text("Técnico o Personal Responsable") },
                        placeholder = { Text("Ej. Juan Pérez (Electricista)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = repairCostText,
                        onValueChange = { repairCostText = it },
                        label = { Text("Costo de Reparación / Repuesto (Q / $)") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = resolutionNotes,
                        onValueChange = { resolutionNotes = it },
                        label = { Text("Detalles de Solución y Notas") },
                        placeholder = { Text("Ej. Se cambió el termostato y fusible quemado...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = repairCostText.toDoubleOrNull() ?: 0.0
                        viewModel.updateMaintenanceStatus(
                            requestId = req.id,
                            newStatus = "RESUELTO",
                            assignedTechnician = technicianName.ifBlank { activeUser },
                            resolutionNotes = resolutionNotes,
                            repairCost = cost
                        )
                        resolvingRequest = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirmar como Resuelto", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { resolvingRequest = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Control de Mantenimiento y Averías", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Registro fotográfico, técnicos y estado de reparación", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo Reporte", tint = HotelGold)
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
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = HotelGold,
                contentColor = HotelNavy,
                modifier = Modifier.testTag("fab_new_maintenance_report")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reportar Avería", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // KPI Summary Header
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = HotelNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ESTADO DE INSTALACIONES", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text("$pendingCount Pendientes • $inProgressCount En Proceso", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (urgentCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusRed,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$urgentCount Urgente(s)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Total: $totalCount", fontSize = 12.sp, color = Color.White)
                        Text("• Pendientes: $pendingCount", fontSize = 12.sp, color = StatusYellow, fontWeight = FontWeight.Bold)
                        Text("• En Reparación: $inProgressCount", fontSize = 12.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                        Text("• Resueltas: $resolvedCount", fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HotelNavy
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) }
                    )
                }
            }

            // Search Bar & Category filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por habitación, artículo o técnico...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allCategories = listOf("Plomería", "Electricidad", "Climatización (A/C)", "Cerrajería", "Electrónica / TV", "Mobiliario & Camas")
                    allCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HotelGold.copy(alpha = 0.3f),
                                selectedLabelColor = HotelNavy
                            )
                        )
                    }
                }
            }

            // Main List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay averías registradas en esta vista.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Use el botón '+' para crear un nuevo reporte con foto integrada.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { req ->
                        MaintenanceTicketCard(
                            request = req,
                            onViewPhoto = { path -> selectedPhotoPath = path },
                            onStartRepair = {
                                viewModel.updateMaintenanceStatus(req.id, "EN_REPARACION", assignedTechnician = activeUser)
                            },
                            onResolve = {
                                resolvingRequest = req
                                technicianName = req.assignedTechnician ?: activeUser
                                resolutionNotes = req.resolutionNotes ?: ""
                                repairCostText = req.repairCost?.toString() ?: ""
                            },
                            onDelete = {
                                viewModel.deleteMaintenanceRequest(req.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceTicketCard(
    request: MaintenanceRequestEntity,
    onViewPhoto: (String) -> Unit,
    onStartRepair: () -> Unit,
    onResolve: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatted = remember(request.reportedTimestamp) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(request.reportedTimestamp))
    }

    val (statusColor, statusText) = when (request.status) {
        "PENDIENTE" -> StatusYellow to "PENDIENTE"
        "EN_REPARACION" -> Color(0xFF3B82F6) to "EN REPARACIÓN"
        "RESUELTO" -> StatusGreen to "RESUELTO"
        else -> Color.Gray to request.status
    }

    val (priorityColor, priorityText) = when (request.priority) {
        "Urgente" -> StatusRed to "🚨 Urgente"
        "Alta" -> Color(0xFFF97316) to "Alta"
        "Media" -> StatusYellow to "Media"
        else -> Color(0xFF64748B) to "Baja"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("maintenance_card_${request.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HotelNavy
                    ) {
                        Text(
                            text = request.roomNumber,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = request.category,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, priorityColor)
                    ) {
                        Text(
                            text = priorityText,
                            color = priorityColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor
                    ) {
                        Text(
                            text = statusText,
                            color = if (statusColor == StatusYellow) HotelNavy else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Item Name
            Text(
                text = request.itemName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Description
            if (request.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = request.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Photo Attachment Section if available
            if (!request.photoPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val bitmap = remember(request.photoPath) {
                    try {
                        BitmapFactory.decodeFile(request.photoPath)
                    } catch (e: Exception) {
                        null
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onViewPhoto(request.photoPath) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto avería",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = HotelGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fotografía adjunta de la avería", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Toque para ver imagen completa en alta resolución", fontSize = 11.sp, color = Color.Gray)
                    }

                    Icon(Icons.Default.Visibility, contentDescription = "Ver Foto", tint = HotelNavy, modifier = Modifier.size(20.dp))
                }
            }

            // Technician / Resolution info if resolved
            if (request.status == "RESUELTO") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "✓ Resuelto por: ${request.assignedTechnician ?: "Personal de Mantenimiento"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen
                        )
                        if (!request.resolutionNotes.isNullOrBlank()) {
                            Text(text = "Solución: ${request.resolutionNotes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (request.repairCost != null && request.repairCost > 0) {
                            Text(text = "Costo: $${String.format(Locale.US, "%.2f", request.repairCost)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HotelGold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer info and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reportado por: ${request.reportedBy}", fontSize = 11.sp, color = Color.Gray)
                    Text(dateFormatted, fontSize = 10.sp, color = Color.Gray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (request.status == "PENDIENTE") {
                        Button(
                            onClick = onStartRepair,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Iniciar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (request.status != "RESUELTO") {
                        Button(
                            onClick = onResolve,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resolver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
