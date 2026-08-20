package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SharedAppState
import com.example.data.model.SharedRoomState
import com.example.data.model.SharedTask
import com.example.data.model.SyncState
import com.example.ui.components.ConflictResolutionDialog
import com.example.ui.components.PeerCriticalAlertBanner
import com.example.ui.components.SessionSecurityExpiredDialog
import com.example.ui.components.SyncLogOverlaySheet
import com.example.ui.components.SyncStatusVisualCue
import com.example.ui.viewmodel.CriticalAlertNotification
import com.example.ui.viewmodel.SharedSyncViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Pantalla de Control y Espejo Bidireccional en Tiempo Real.
 * Incorpora persistencia local garantizada en Room, pantalla Sync Health con gráficos
 * Recharts-style, botón global Snackbar "Deshacer (Undo)" y seguridad de inactividad de 1 mes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMirrorDashboardScreen(
    viewModel: SharedSyncViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sharedState by viewModel.sharedState.collectAsStateWithLifecycle()
    val currentRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val isSyncLogVisible by viewModel.isSyncLogOverlayVisible.collectAsStateWithLifecycle()
    val isSyncHealthVisible by viewModel.isSyncHealthVisible.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Estado local para el banner de alerta crítica del dispositivo par
    var currentPeerAlert by remember { mutableStateOf<CriticalAlertNotification?>(null) }

    // Diálogos de acción rápida
    var showCheckInDialogForRoom by remember { mutableStateOf<String?>(null) }
    var guestNameInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Escuchar alertas críticas provenientes del peer
    LaunchedEffect(Unit) {
        viewModel.peerCriticalAlert.collectLatest { alert ->
            currentPeerAlert = alert
        }
    }

    // Escuchar eventos de Deshacer (Undo) para disparar el Snackbar global
    LaunchedEffect(Unit) {
        viewModel.undoSnackbarEvent.collectLatest { undoAction ->
            val result = snackbarHostState.showSnackbar(
                message = "${undoAction.description} aplicada",
                actionLabel = "DESHACER",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastSyncAction()
            }
        }
    }

    // Mostrar feedback Toast
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedbackMessage()
        }
    }

    // Si la pantalla de Sync Health está activa, mostrar la vista de salud y métricas Recharts
    if (isSyncHealthVisible) {
        SyncHealthScreen(
            viewModel = viewModel,
            onBack = { viewModel.toggleSyncHealthScreen(false) }
        )
        return
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("global_sync_undo_snackbar")
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Espejo en Tiempo Real",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Rol activo: $currentRole • Room DB Local Activa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Cue visual del estado de sincronización (Pulsating en offline)
                    SyncStatusVisualCue(
                        syncState = sharedState.syncStatus,
                        onForceSyncClick = {
                            viewModel.forceSyncQueuedData()
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Botón para abrir la pantalla de Salud de Sincronización (Recharts Bar Chart)
                    IconButton(
                        onClick = { viewModel.toggleSyncHealthScreen(true) },
                        modifier = Modifier.testTag("open_sync_health_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Salud de Red y Gráficos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Botón para alternar Rol (para testing fácil en una sola pantalla)
                    IconButton(
                        onClick = {
                            val newRole = if (currentRole == "GERENTE") "RECEPCION" else "GERENTE"
                            val newName = if (newRole == "GERENTE") "Terminal Gerente" else "Tablet Recepción"
                            viewModel.setDeviceRole(newRole, newName)
                            Toast.makeText(context, "Cambiado a $newRole", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("switch_device_role_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar Rol")
                    }

                    // Botón para abrir el Sync Log Overlay
                    IconButton(
                        onClick = { viewModel.toggleSyncLogOverlay(true) },
                        modifier = Modifier.testTag("open_sync_log_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Historial Sync")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. BANNER DE ALERTA CRÍTICA SUTIL (Cuando el Peer hace Check-In)
                PeerCriticalAlertBanner(
                    alert = currentPeerAlert,
                    onDismiss = { currentPeerAlert = null }
                )

                // 2. TARJETA DE ESTADO GENERAL Y CAJA COMPARTIDA
                CashRegisterAndSyncHeader(
                    sharedState = sharedState,
                    currentRole = currentRole,
                    onAdjustBalance = {
                        val newAmount = sharedState.cashRegister.currentBalance + 120.0
                        viewModel.updateCashBalance(newAmount)
                    },
                    onTriggerConflict = {
                        viewModel.triggerSimulatedConflict("101")
                    },
                    onOpenHealth = {
                        viewModel.toggleSyncHealthScreen(true)
                    }
                )

                // 3. SELECTOR DE VISTAS (Habitaciones / Tareas)
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Habitaciones (${sharedState.activeRooms.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Tareas en Vivo (${sharedState.activeTasks.size})") }
                    )
                }

                if (selectedTab == 0) {
                    // MATRIZ DE HABITACIONES EN VIVO
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sharedState.activeRooms.values.toList(), key = { it.roomNumber }) { room ->
                            SharedRoomCard(
                                room = room,
                                onCheckInClick = {
                                    guestNameInput = ""
                                    showCheckInDialogForRoom = room.roomNumber
                                },
                                onCheckOutClick = {
                                    viewModel.changeRoomStatus(room.roomNumber, "AVAILABLE", context)
                                },
                                onCleaningClick = {
                                    viewModel.changeRoomStatus(room.roomNumber, "CLEANING", context)
                                }
                            )
                        }
                    }
                } else {
                    // LISTA DE TAREAS COMPARTIDAS
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sharedState.activeTasks, key = { it.id }) { task ->
                            SharedTaskItemCard(
                                task = task,
                                onToggle = { isChecked ->
                                    viewModel.toggleTask(task.id, isChecked)
                                }
                            )
                        }
                    }
                }
            }

            // 4. OVERLAY DE HISTORIAL DE ACCIONES SINCRONIZADAS (SYNC LOG)
            if (isSyncLogVisible) {
                SyncLogOverlaySheet(
                    syncLogs = sharedState.syncLogs,
                    onDismiss = { viewModel.toggleSyncLogOverlay(false) }
                )
            }

            // 5. DIÁLOGO DE RESOLUCIÓN DE CONFLICTOS DE CONCURRENCIA
            sharedState.pendingConflict?.let { conflict ->
                ConflictResolutionDialog(
                    conflict = conflict,
                    onResolve = { chosenState ->
                        viewModel.resolveConflict(conflict.roomNumber, chosenState)
                    }
                )
            }

            // 6. DIÁLOGO DE SEGURIDAD: SESIÓN EXPIRADA TRAS 1 MES DE INACTIVIDAD
            SessionSecurityExpiredDialog(
                isExpired = sharedState.isSessionExpired,
                onReauthenticate = { pin ->
                    viewModel.reauthenticateExpiredSession(pin)
                }
            )

            // Diálogo para ingresar Check-In
            if (showCheckInDialogForRoom != null) {
                AlertDialog(
                    onDismissRequest = { showCheckInDialogForRoom = null },
                    title = { Text("Registrar Check-In (Hab. $showCheckInDialogForRoom)") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "El cambio se reflejará instantáneamente en el dispositivo par ($currentRole -> Peer)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = guestNameInput,
                                onValueChange = { guestNameInput = it },
                                label = { Text("Nombre del Huésped") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("guest_name_input_field")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val name = if (guestNameInput.isNotBlank()) guestNameInput else "Huésped Express"
                                showCheckInDialogForRoom?.let { roomNum ->
                                    viewModel.performCheckIn(roomNum, name, context)
                                }
                                showCheckInDialogForRoom = null
                            },
                            modifier = Modifier.testTag("confirm_checkin_button")
                        ) {
                            Text("Confirmar Check-In")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCheckInDialogForRoom = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CashRegisterAndSyncHeader(
    sharedState: SharedAppState,
    currentRole: String,
    onAdjustBalance: () -> Unit,
    onTriggerConflict: () -> Unit,
    onOpenHealth: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Arqueo de Caja (Espejo Activo)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "$${"%.2f".format(sharedState.cashRegister.currentBalance)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onAdjustBalance,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_balance_quick_button")
                    ) {
                        Text("+$120", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = onOpenHealth,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_health_metrics_button")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salud Sync", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Última edición por: ${sharedState.lastUpdatedByDevice.ifBlank { "Sin cambios recientes" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )

                if (sharedState.lastUndoAction != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Deshacer disponible",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedRoomCard(
    room: SharedRoomState,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onCleaningClick: () -> Unit
) {
    val isOccupied = room.status == "OCCUPIED"
    val isCleaning = room.status == "CLEANING"

    val statusBg = when {
        isOccupied -> Color(0xFFFFEBEE)
        isCleaning -> Color(0xFFFFF3E0)
        else -> Color(0xFFE8F5E9)
    }

    val statusColor = when {
        isOccupied -> Color(0xFFC62828)
        isCleaning -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_card_${room.roomNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = room.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isOccupied && !room.guestName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = room.guestName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = if (isCleaning) "En limpieza profunda" else "Lista para check-in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Modificado por: ${room.lastUpdatedBy}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Botones de acción según estado
            if (!isOccupied) {
                Button(
                    onClick = onCheckInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("room_checkin_${room.roomNumber}"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Check-In", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onCheckOutClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_checkout_${room.roomNumber}"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Check-Out", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = onCleaningClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Limpieza", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedTaskItemCard(
    task: SharedTask,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${task.roomNumber} • Asignado a: ${task.assignedTo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.updatedByRole.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = task.updatedByRole,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
