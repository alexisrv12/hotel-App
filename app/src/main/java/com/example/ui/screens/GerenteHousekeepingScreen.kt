package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.HousekeepingTaskEntity
import com.example.data.entities.RoomStatus
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteHousekeepingScreen(
    viewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val housekeepingTasks by viewModel.housekeepingTasks.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val users by viewModel.users.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Todas", "Pendientes", "En Proceso", "Completadas")

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val filteredTasks = when (selectedTab) {
        1 -> housekeepingTasks.filter { it.status == "PENDIENTE" }
        2 -> housekeepingTasks.filter { it.status == "EN_PROCESO" }
        3 -> housekeepingTasks.filter { it.status == "COMPLETADA" }
        else -> housekeepingTasks
    }

    val totalTasks = housekeepingTasks.size.coerceAtLeast(1)
    val completedCount = housekeepingTasks.count { it.status == "COMPLETADA" }
    val pendingCount = housekeepingTasks.count { it.status == "PENDIENTE" }
    val inProgressCount = housekeepingTasks.count { it.status == "EN_PROCESO" }
    val completionPercentage = (completedCount.toFloat() / totalTasks.toFloat()) * 100f

    val roomsNeedingCleaning = rooms.filter {
        it.status == RoomStatus.PENDIENTE_LIMPIEZA || it.status == RoomStatus.EN_LIMPIEZA
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cronograma de Limpieza", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Asignación de habitaciones y seguimiento del personal", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
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
                onClick = { showAddTaskDialog = true },
                containerColor = HotelGold,
                contentColor = HotelNavy
            ) {
                Icon(Icons.Default.Add, contentDescription = "Asignar Limpieza")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary KPI Header
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
                            Text("PROGRESO DE LIMPIEZA HOY", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text("$completedCount de ${housekeepingTasks.size} Habitaciones Limpias", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.0f", completionPercentage)}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HotelGold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = completionPercentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = HotelGold,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Pendientes: $pendingCount", fontSize = 12.sp, color = StatusYellow, fontWeight = FontWeight.Bold)
                        Text("• En Proceso: $inProgressCount", fontSize = 12.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                        Text("• Completadas: $completedCount", fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
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
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                    )
                }
            }

            // Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay tareas de limpieza en este estado.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        HousekeepingTaskCard(
                            task = task,
                            onStart = {
                                viewModel.updateHousekeepingStatus(task.id, "EN_PROCESO", setRoomAvailable = false)
                            },
                            onComplete = {
                                viewModel.updateHousekeepingStatus(task.id, "COMPLETADA", setRoomAvailable = true)
                            },
                            onDelete = {
                                viewModel.deleteHousekeepingTask(task.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog to Assign New Housekeeping Task
    if (showAddTaskDialog) {
        var roomNumberText by remember {
            mutableStateOf(roomsNeedingCleaning.firstOrNull()?.roomNumber ?: rooms.firstOrNull()?.roomNumber ?: "101")
        }
        var staffNameText by remember { mutableStateOf(users.firstOrNull { it.role == "RECEPCIONISTA" }?.fullName ?: "Personal de Limpieza") }
        var priorityText by remember { mutableStateOf("Normal") }
        var notesText by remember { mutableStateOf("") }

        var roomDropdownExpanded by remember { mutableStateOf(false) }
        var priorityDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = HotelNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Asignar Tarea de Limpieza", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Room Dropdown
                    ExposedDropdownMenuBox(
                        expanded = roomDropdownExpanded,
                        onExpandedChange = { roomDropdownExpanded = !roomDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Habitación $roomNumberText",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Habitación a Limpiar") },
                            leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = roomDropdownExpanded,
                            onDismissRequest = { roomDropdownExpanded = false }
                        ) {
                            rooms.forEach { r ->
                                val statusLabel = if (r.status == RoomStatus.PENDIENTE_LIMPIEZA || r.status == RoomStatus.EN_LIMPIEZA) " (Requiere Limpieza)" else " (${r.status})"
                                DropdownMenuItem(
                                    text = { Text("Hab. ${r.roomNumber}$statusLabel") },
                                    onClick = {
                                        roomNumberText = r.roomNumber
                                        roomDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Assigned Staff Input
                    OutlinedTextField(
                        value = staffNameText,
                        onValueChange = { staffNameText = it },
                        label = { Text("Asignar a Encargado/a de Limpieza") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Priority Dropdown
                    ExposedDropdownMenuBox(
                        expanded = priorityDropdownExpanded,
                        onExpandedChange = { priorityDropdownExpanded = !priorityDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = priorityText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Prioridad") },
                            leadingIcon = { Icon(Icons.Default.PriorityHigh, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = priorityDropdownExpanded,
                            onDismissRequest = { priorityDropdownExpanded = false }
                        ) {
                            listOf("Normal", "Alta", "Urgente").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p, fontWeight = if (p == "Urgente") FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        priorityText = p
                                        priorityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Notes/Instructions
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Instrucciones / Notas Especiales") },
                        placeholder = { Text("e.g., Desinfección profunda, cambio de toallas...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (staffNameText.isNotBlank()) {
                            viewModel.assignHousekeepingTask(
                                roomNumber = roomNumberText,
                                staffName = staffNameText,
                                priority = priorityText,
                                notes = notesText
                            )
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("Asignar Tarea")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HousekeepingTaskCard(
    task: HousekeepingTaskEntity,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (task.status) {
        "COMPLETADA" -> StatusGreen
        "EN_PROCESO" -> Color(0xFF2563EB)
        else -> StatusYellow
    }

    val priorityColor = when (task.priority) {
        "Urgente" -> StatusRed
        "Alta" -> Color(0xFFEA580C)
        else -> Color.Gray
    }

    val timeFormatted = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(task.assignedTimestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = HotelNavy.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(task.roomNumber, fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Habitación ${task.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Encargado: ${task.assignedStaffName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusRed.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Estado: ${task.status}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text("Asignado: $timeFormatted", fontSize = 11.sp, color = Color.Gray)
            }

            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Notas: ${task.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (task.status == "PENDIENTE") {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar Limpieza", fontSize = 12.sp)
                    }
                }

                if (task.status != "COMPLETADA") {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Marcar Completada", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "✓ Limpieza completada exitosamente",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
            }
        }
    }
}
