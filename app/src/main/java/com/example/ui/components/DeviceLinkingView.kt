package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.Screen
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.viewmodel.LinkingUiState
import com.example.ui.viewmodel.LinkingViewModel
import com.example.utils.QrScannerManager
import com.journeyapps.barcodescanner.ScanContract
import kotlinx.coroutines.delay

/**
 * Componente de Compose para la vista de 'Vincular Dispositivo'.
 * Incluye:
 * 1. Campo de texto para PIN de 6 dígitos.
 * 2. Botón de 'Escanear QR' que activa el Intent nativo de ZXing.
 * 3. Animación de carga y sincronización de datos iniciales.
 */
@Composable
fun DeviceLinkingView(
    modifier: Modifier = Modifier,
    viewModel: LinkingViewModel = viewModel(),
    onLinkingSuccess: (Screen) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by viewModel.uiState.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val wifiIpInfo by viewModel.wifiIpInfo.collectAsState()
    val discoveredLanDevices by viewModel.discoveredLanDevices.collectAsState()
    val isScanningLan by viewModel.isScanningLan.collectAsState()

    var targetIpInput by remember { mutableStateOf("") }
    var terminalNameInput by remember { mutableStateOf("Terminal Recepción") }

    // ActivityResultLauncher for ZXing Intent Scan
    val zxingScanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            viewModel.processScannedQr(context, result.contents)
        } else {
            Toast.makeText(context, "Escaneo de QR cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { targetScreen ->
            onLinkingSuccess(targetScreen)
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HotelNavy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = HotelNavy,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = "Vincular Dispositivo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )
                    Text(
                        text = "Autorización de terminal segura",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            when (val state = uiState) {
                is LinkingUiState.Validating, is LinkingUiState.Syncing -> {
                    // Animación de Carga y Sincronización Inicial
                    SyncingDataLoadingAnimation(
                        statusMessage = when (state) {
                            is LinkingUiState.Validating -> state.message
                            is LinkingUiState.Syncing -> state.statusMessage
                            else -> "Sincronizando..."
                        },
                        progress = if (state is LinkingUiState.Syncing) state.progress else null
                    )
                }

                is LinkingUiState.Success -> {
                    val successState = state as LinkingUiState.Success
                    LaunchedEffect(successState) {
                        delay(1500L)
                        onLinkingSuccess(successState.targetScreen)
                    }

                    // Pantalla de Éxito
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "¡Dispositivo Autorizado!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Rol asignado: ${state.role}\nRedirigiendo a su módulo...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is LinkingUiState.Idle, is LinkingUiState.Error -> {
                    // Formulario de Vinculación: PIN y Botón Escanear QR

                    // 1. Sección de Escaneo QR (ZXing Intent)
                    Text(
                        text = "Opción 1: Escanear Código QR",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val options = QrScannerManager.createScanOptions(
                                promptText = "Apunta al código QR del Hotel Rivera para vincular"
                            )
                            zxingScanLauncher.launch(options)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HotelNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_scan_qr_zxing")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = HotelGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Escanear QR con ZXing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  O INGRESA PIN  ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Divider(modifier = Modifier.weight(1f))
                    }

                    // 2. Sección de PIN Manual
                    Text(
                        text = "Opción 2: Código PIN de 6 dígitos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { viewModel.onPinChange(it) },
                        label = { Text("PIN de Autorización (6 dígitos)") },
                        placeholder = { Text("••••••") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = HotelNavy)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                viewModel.linkWithPin(context)
                            }
                        ),
                        isError = state is LinkingUiState.Error,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_linking_pin")
                    )

                    if (state is LinkingUiState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = state.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.linkWithPin(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HotelGold
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_submit_linking_pin")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = HotelNavy
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vincular con PIN",
                            fontWeight = FontWeight.Bold,
                            color = HotelNavy
                        )
                    }

                    // Separador visual
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text(
                            text = "O POR RED LOCAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Divider(modifier = Modifier.weight(1f))
                    }

                    // 3. Sección de Conexión Wi-Fi / IP
                    Text(
                        text = "Opción 3: Conectar por Wi-Fi / IP Local",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = HotelNavy.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Red Wi-Fi:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(wifiIpInfo.wifiSsid, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Mi IP:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(wifiIpInfo.localIpAddress, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = HotelNavy)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.scanLocalNetwork() },
                        enabled = !isScanningLan,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isScanningLan) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buscando en red local...", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detectar Servidores en Red Wi-Fi", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (discoveredLanDevices.isNotEmpty()) {
                        discoveredLanDevices.forEach { dev ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(dev.deviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text("${dev.ipAddress}:${dev.port}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(
                                        onClick = { targetIpInput = dev.ipAddress }
                                    ) {
                                        Text("Usar IP")
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetIpInput,
                        onValueChange = { targetIpInput = it },
                        label = { Text("IP del Servidor (Gerente)") },
                        placeholder = { Text("Ej: 192.168.1.100") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Router, contentDescription = null, tint = HotelNavy)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_linking_ip")
                    )

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.linkWithIpAddress(
                                context = context,
                                ipAddress = targetIpInput,
                                deviceName = terminalNameInput
                            )
                        },
                        enabled = targetIpInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HotelNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_submit_linking_ip")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vincular por Red Wi-Fi (IP)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animación de carga y sincronización de datos iniciales.
 */
@Composable
private fun SyncingDataLoadingAnimation(
    statusMessage: String,
    progress: Float?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_sync")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HotelNavy.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(HotelNavy.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = HotelNavy,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Sincronizando Terminal...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HotelNavy
            )

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = HotelGold,
                    trackColor = HotelNavy.copy(alpha = 0.15f)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = HotelNavy,
                    strokeWidth = 3.dp
                )
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
