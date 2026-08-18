package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ActiveLinkingCode
import com.example.network.ConnectedClientSession
import com.example.network.HotelLocalServer
import com.example.network.LocalServerStatus
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NetworkConnectionMode {
    LOCAL_SERVER_WIFI,
    REMOTE_INTERNET
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSyncSettingsScreen(
    hotelViewModel: HotelViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val localServer = hotelViewModel.localServer

    val serverStatus by localServer.status.collectAsState()
    val serverIp by localServer.serverIp.collectAsState()
    val serverPort by localServer.serverPort.collectAsState()
    val activeLinkingCode by localServer.activeLinkingCode.collectAsState()
    val connectedClients by localServer.connectedClients.collectAsState()

    var connectionMode by remember { mutableStateOf(NetworkConnectionMode.LOCAL_SERVER_WIFI) }
    var selectedRoleToLink by remember { mutableStateOf("RECEPCION") }
    var portInput by remember { mutableStateOf(serverPort.toString()) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var countdownSeconds by remember { mutableLongStateOf(0L) }

    val rolesList = listOf("Mesero", "Cocina", "Caja", "Recepción", "Gerente")

    // Update countdown timer and QR code
    LaunchedEffect(activeLinkingCode) {
        activeLinkingCode?.let { code ->
            // Generate QR Bitmap
            val qrPayload = JSONObject().apply {
                put("ip", serverIp)
                put("port", serverPort)
                put("businessId", HotelLocalServer.BUSINESS_ID)
                put("token", code.token)
                put("pin", code.pin)
                put("role", code.role)
                put("expiresAt", code.expiresAtMillis)
            }.toString()

            qrBitmap = generateQrBitmap(qrPayload, 512)
        }
    }

    LaunchedEffect(activeLinkingCode?.expiresAtMillis) {
        while (true) {
            val remaining = activeLinkingCode?.remainingSeconds() ?: 0L
            countdownSeconds = remaining
            if (remaining <= 0) break
            delay(1000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Red y Sincronización", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Servidor Local Wi-Fi • Hotel Rivera", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. MODO DE CONEXIÓN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = HotelNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modo de Conexión", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { connectionMode = NetworkConnectionMode.LOCAL_SERVER_WIFI }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = connectionMode == NetworkConnectionMode.LOCAL_SERVER_WIFI,
                            onClick = { connectionMode = NetworkConnectionMode.LOCAL_SERVER_WIFI }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Servidor Local (Wi-Fi)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("La terminal principal (Caja/Gerente) actúa como servidor central sin requerir Internet.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { connectionMode = NetworkConnectionMode.REMOTE_INTERNET }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = connectionMode == NetworkConnectionMode.REMOTE_INTERNET,
                            onClick = { connectionMode = NetworkConnectionMode.REMOTE_INTERNET }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Internet / Servidor Remoto", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Sincronización en la nube mediante base de datos remota.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            if (connectionMode == NetworkConnectionMode.LOCAL_SERVER_WIFI) {
                // 2. ESTADO DEL SERVIDOR CENTRAL
                val (statusColor, statusLabel, statusIcon) = when (serverStatus) {
                    LocalServerStatus.ACTIVO -> Triple(StatusGreen, "Servidor Activo", Icons.Default.CheckCircle)
                    LocalServerStatus.ESPERANDO_DISPOSITIVOS -> Triple(StatusYellow, "Esperando Dispositivos", Icons.Default.Wifi)
                    LocalServerStatus.INICIANDO -> Triple(StatusYellow, "Iniciando Servidor...", Icons.Default.Refresh)
                    LocalServerStatus.DETENIDO -> Triple(StatusRed, "Servidor Detenido", Icons.Default.Stop)
                    LocalServerStatus.ERROR -> Triple(StatusRed, "Error al Iniciar", Icons.Default.Error)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = statusColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Terminal Principal (Servidor)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                                }
                            }

                            Button(
                                onClick = {
                                    if (serverStatus == LocalServerStatus.ACTIVO || serverStatus == LocalServerStatus.ESPERANDO_DISPOSITIVOS) {
                                        localServer.stopServer()
                                    } else {
                                        val p = portInput.toIntOrNull() ?: 8080
                                        localServer.startServer(p)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (serverStatus == LocalServerStatus.ACTIVO || serverStatus == LocalServerStatus.ESPERANDO_DISPOSITIVOS) StatusRed else HotelNavy
                                )
                            ) {
                                Icon(
                                    imageVector = if (serverStatus == LocalServerStatus.ACTIVO || serverStatus == LocalServerStatus.ESPERANDO_DISPOSITIVOS) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (serverStatus == LocalServerStatus.ACTIVO || serverStatus == LocalServerStatus.ESPERANDO_DISPOSITIVOS) "Detener" else "Iniciar")
                            }
                        }

                        HorizontalDivider()

                        // Server Address info
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Dirección del Servidor Local:", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = "http://$serverIp:$serverPort",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = HotelNavy
                                )
                                Text("WebSocket: ws://$serverIp:$serverPort/ws", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { portInput = it },
                                label = { Text("Puerto Configurable") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val newPort = portInput.toIntOrNull()
                                    if (newPort != null && newPort in 1024..65535) {
                                        localServer.setPort(newPort)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                            ) {
                                Text("Aplicar Puerto")
                            }
                        }
                    }
                }

                // 3. QR Y PIN DINÁMICO DE VINCULACIÓN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = HotelGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Vincular Dispositivos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                            }

                            Button(
                                onClick = {
                                    localServer.generateNewLinkingCode(role = selectedRoleToLink.uppercase(), validityMinutes = 10)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HotelGold)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Regenerar")
                            }
                        }

                        Text("Selecciona el rol asignado para el nuevo dispositivo:", fontSize = 12.sp, color = Color.Gray)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rolesList.forEach { role ->
                                val isSelected = selectedRoleToLink.equals(role, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedRoleToLink = role
                                        localServer.generateNewLinkingCode(role = role.uppercase(), validityMinutes = 10)
                                    },
                                    label = { Text(role, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HotelNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // QR Image
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "Código QR de Vinculación",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("Generando...", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            // PIN and Countdown
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("PIN de Vinculación Manual:", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = activeLinkingCode?.pin ?: "------",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = HotelNavy,
                                    letterSpacing = 4.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = StatusYellow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val minutes = countdownSeconds / 60
                                    val seconds = countdownSeconds % 60
                                    Text(
                                        text = String.format("Expira en: %02d:%02d", minutes, seconds),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (countdownSeconds < 60L) StatusRed else Color.Gray
                                    )
                                }

                                Text(
                                    text = "Rol: ${activeLinkingCode?.role ?: selectedRoleToLink}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelGold
                                )
                            }
                        }
                    }
                }

                // 4. DISPOSITIVOS CONECTADOS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Smartphone, contentDescription = null, tint = HotelNavy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dispositivos Conectados", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                            }
                            Surface(
                                shape = CircleShape,
                                color = HotelNavy.copy(alpha = 0.12f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${connectedClients.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HotelNavy)
                                }
                            }
                        }

                        HorizontalDivider()

                        if (connectedClients.isEmpty()) {
                            Text(
                                text = "No hay dispositivos secundarios conectados en este momento. Escanea el código QR o ingresa el PIN desde Mesero, Cocina o Recepción.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            connectedClients.forEach { client ->
                                ConnectedDeviceItemRow(
                                    client = client,
                                    onRevoke = { localServer.revokeDevice(client.deviceId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedDeviceItemRow(
    client: ConnectedClientSession,
    onRevoke: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = StatusGreen,
                    modifier = Modifier.size(10.dp)
                ) {}
                Column {
                    Text(client.deviceName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Rol: ${client.role} • IP: ${client.ipAddress}", fontSize = 12.sp, color = Color.Gray)
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(client.connectedAtMillis))
                    Text("Conectado a las: $timeStr", fontSize = 10.sp, color = Color.Gray)
                }
            }

            IconButton(
                onClick = onRevoke,
                modifier = Modifier.testTag("revoke_device_${client.deviceId}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Desvincular Dispositivo", tint = StatusRed)
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
