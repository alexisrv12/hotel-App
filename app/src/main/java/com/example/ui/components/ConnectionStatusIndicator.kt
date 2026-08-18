package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ClientConnectionState
import com.example.network.ClientSyncInfo
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectionStatusChip(
    syncInfo: ClientSyncInfo,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val (bgColor, iconColor, statusText, statusIcon) = when (syncInfo.state) {
        ClientConnectionState.CONECTADO -> {
            if (syncInfo.pendingOperationsCount > 0) {
                listOf(
                    StatusYellow.copy(alpha = 0.15f),
                    StatusYellow,
                    "Sincronizando (${syncInfo.pendingOperationsCount})",
                    Icons.Default.CloudSync
                )
            } else {
                listOf(
                    StatusGreen.copy(alpha = 0.15f),
                    StatusGreen,
                    "🟢 Conectado",
                    Icons.Default.CloudDone
                )
            }
        }
        ClientConnectionState.SINCRONIZANDO -> {
            listOf(
                StatusYellow.copy(alpha = 0.18f),
                StatusYellow,
                "🟡 Sincronizando...",
                Icons.Default.CloudSync
            )
        }
        ClientConnectionState.DESCONECTADO -> {
            listOf(
                StatusRed.copy(alpha = 0.15f),
                StatusRed,
                if (syncInfo.pendingOperationsCount > 0) "🔴 Sin conexión (${syncInfo.pendingOperationsCount} pendientes)" else "🔴 Sin conexión",
                Icons.Default.CloudOff
            )
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { showDialog = true }
            .testTag("connection_status_chip"),
        color = bgColor as Color,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor as Color,
                modifier = Modifier.size(8.dp)
            ) {}
            Text(
                text = statusText as String,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = statusIcon as androidx.compose.ui.graphics.vector.ImageVector,
                        contentDescription = null,
                        tint = iconColor as Color
                    )
                    Text("Estado de Red y Sincronización", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Servidor Central:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = if (syncInfo.serverAddress.isNotBlank()) syncInfo.serverAddress else "No configurado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Última Sincronización:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = if (syncInfo.lastSyncTimestamp > 0) {
                                    SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(Date(syncInfo.lastSyncTimestamp))
                                } else {
                                    "Pendiente"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Operaciones Pendientes en Cola:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = "${syncInfo.pendingOperationsCount} cambios guardados localmente",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncInfo.pendingOperationsCount > 0) StatusYellow else StatusGreen
                            )
                        }
                    }

                    if (syncInfo.errorMessage != null) {
                        Text(
                            text = "Aviso: ${syncInfo.errorMessage}",
                            fontSize = 12.sp,
                            color = StatusRed
                        )
                    } else {
                        Text(
                            text = "En modo Servidor Local (Wi-Fi), los pedidos y habitaciones se sincronizan en tiempo real sin requerir Internet.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSyncNow()
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sincronizar Ahora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
