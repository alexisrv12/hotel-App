package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow

val StatusOrange = Color(0xFFEA580C)

@Composable
fun RoomStatusDashboard(
    rooms: List<RoomEntity>,
    onRoomClick: (RoomEntity) -> Unit = {},
    onStatusChange: (RoomEntity, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<String?>(null) } // null = All
    var selectedRoomForDetail by remember { mutableStateOf<RoomEntity?>(null) }

    val filteredRooms = if (selectedFilter == null) rooms else rooms.filter {
        if (selectedFilter == RoomStatus.PENDIENTE_LIMPIEZA) {
            it.status == RoomStatus.PENDIENTE_LIMPIEZA || it.status == RoomStatus.EN_LIMPIEZA
        } else {
            it.status == selectedFilter
        }
    }

    val countAvailable = rooms.count { it.status == RoomStatus.DISPONIBLE }
    val countOccupied = rooms.count { it.status == RoomStatus.OCUPADA }
    val countCleaning = rooms.count { it.status == RoomStatus.PENDIENTE_LIMPIEZA || it.status == RoomStatus.EN_LIMPIEZA }
    val countMaintenance = rooms.count { it.status == "MANTENIMIENTO" }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Filter Chips with Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("Todas (${rooms.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == RoomStatus.DISPONIBLE,
                onClick = { selectedFilter = RoomStatus.DISPONIBLE },
                label = { Text("Disponible ($countAvailable)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusGreen.copy(alpha = 0.2f),
                    selectedLabelColor = StatusGreen
                )
            )
            FilterChip(
                selected = selectedFilter == RoomStatus.OCUPADA,
                onClick = { selectedFilter = RoomStatus.OCUPADA },
                label = { Text("Ocupado ($countOccupied)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusRed.copy(alpha = 0.2f),
                    selectedLabelColor = StatusRed
                )
            )
            FilterChip(
                selected = selectedFilter == RoomStatus.PENDIENTE_LIMPIEZA,
                onClick = { selectedFilter = RoomStatus.PENDIENTE_LIMPIEZA },
                label = { Text("Limpieza ($countCleaning)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusYellow.copy(alpha = 0.2f),
                    selectedLabelColor = StatusYellow
                )
            )
        }

        // Room Grid Layout
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            items(filteredRooms) { room ->
                RoomStatusCard(
                    room = room,
                    onClick = {
                        selectedRoomForDetail = room
                        onRoomClick(room)
                    }
                )
            }
        }
    }

    // Room Status Detail & Quick Changer Dialog
    selectedRoomForDetail?.let { room ->
        Dialog(onDismissRequest = { selectedRoomForDetail = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Habitación ${room.roomNumber}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tipo: ${room.roomType} • Tarifa: Q${room.nightlyRate}/noche",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (room.status == RoomStatus.OCUPADA) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusRed.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Huésped: ${room.clientName ?: "Sin Nombre"}", fontWeight = FontWeight.Bold, color = StatusRed)
                                Text("DPI/Doc: ${room.clientDpi ?: "N/A"}", fontSize = 12.sp)
                                Text("Personas: ${room.guestCount}", fontSize = 12.sp)
                            }
                        }
                    }

                    Text("Cambiar Estado de la Habitación:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusOptionButton("Disponible (Verde)", StatusGreen, Icons.Default.CheckCircle) {
                            onStatusChange(room, RoomStatus.DISPONIBLE)
                            selectedRoomForDetail = null
                        }
                        StatusOptionButton("Ocupada (Rojo)", StatusRed, Icons.Default.Hotel) {
                            onStatusChange(room, RoomStatus.OCUPADA)
                            selectedRoomForDetail = null
                        }
                        StatusOptionButton("Limpieza (Amarillo)", StatusYellow, Icons.Default.CleaningServices) {
                            onStatusChange(room, RoomStatus.PENDIENTE_LIMPIEZA)
                            selectedRoomForDetail = null
                        }
                        StatusOptionButton("Mantenimiento (Naranja)", StatusOrange, Icons.Default.Build) {
                            onStatusChange(room, "MANTENIMIENTO")
                            selectedRoomForDetail = null
                        }
                    }

                    TextButton(
                        onClick = { selectedRoomForDetail = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusOptionButton(
    label: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun RoomStatusCard(
    room: RoomEntity,
    onClick: () -> Unit
) {
    val statusColor = when (room.status) {
        RoomStatus.DISPONIBLE -> StatusGreen
        RoomStatus.OCUPADA -> StatusRed
        RoomStatus.PENDIENTE_LIMPIEZA, RoomStatus.EN_LIMPIEZA -> StatusYellow
        else -> StatusOrange
    }

    val statusIcon = when (room.status) {
        RoomStatus.DISPONIBLE -> Icons.Default.CheckCircle
        RoomStatus.OCUPADA -> Icons.Default.Hotel
        RoomStatus.PENDIENTE_LIMPIEZA, RoomStatus.EN_LIMPIEZA -> Icons.Default.CleaningServices
        else -> Icons.Default.Build
    }

    val statusText = when (room.status) {
        RoomStatus.DISPONIBLE -> "Disponible"
        RoomStatus.OCUPADA -> "Ocupada"
        RoomStatus.PENDIENTE_LIMPIEZA, RoomStatus.EN_LIMPIEZA -> "Limpieza"
        else -> "Mantenimiento"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hab. ${room.roomNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusText,
                        tint = statusColor,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (room.status == RoomStatus.OCUPADA && !room.clientName.isNullOrBlank()) {
                Text(
                    text = room.clientName ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "${room.roomType} • Q${room.nightlyRate}/n",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
