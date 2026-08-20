package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.ui.viewmodel.DeviceLinkingViewModel
import com.example.utils.NetworkConnectivityHelper
import com.example.utils.PinValidationResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

enum class UserRoleMode {
    MANAGER, RECEPTIONIST
}

enum class ManagerLinkingMode {
    QR, PIN
}

enum class ReceptionistInputMode {
    PIN, QR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLinkingScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceLinkingViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedRole by remember { mutableStateOf(UserRoleMode.MANAGER) }
    var managerMode by remember { mutableStateOf(ManagerLinkingMode.QR) }
    var receptionMode by remember { mutableStateOf(ReceptionistInputMode.PIN) }

    val isOnline by remember {
        NetworkConnectivityHelper.observeNetworkConnectivity(context)
    }.collectAsState(initial = NetworkConnectivityHelper.isNetworkAvailable(context))

    var networkFailureMessage by remember { mutableStateOf<String?>(null) }
    var pendingGenerationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var scannedQrResultFromCamera by remember { mutableStateOf<String?>(null) }

    val linkedDevices by viewModel.linkedDevices.collectAsState()
    val currentPin by viewModel.currentPin.collectAsState()
    val currentQrSessionToken by viewModel.currentQrSessionToken.collectAsState()
    val pinCountdownText by viewModel.pinCountdownText.collectAsState()
    val qrCountdownText by viewModel.qrCountdownText.collectAsState()
    val decodedQrPayload by viewModel.decodedQrSessionPayload.collectAsState()
    val pinValidationResult by viewModel.pinValidationResult.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(isOnline) {
        if (isOnline && networkFailureMessage != null) {
            networkFailureMessage = null
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(scannedQrResultFromCamera) {
        scannedQrResultFromCamera?.let { token ->
            viewModel.decodeQrToken(token)
            selectedRole = UserRoleMode.RECEPTIONIST
            receptionMode = ReceptionistInputMode.QR
            scannedQrResultFromCamera = null
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
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Vinculación de Dispositivos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gestión de terminales y sincronización",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCameraScannerDialog = true },
                        modifier = Modifier.testTag("open_camera_scanner_appbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear QR con Cámara",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            NetworkStatusIndicatorBar(isOnline = isOnline)

            AnimatedVisibility(
                visible = networkFailureMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                networkFailureMessage?.let { errorMsg ->
                    RetryConnectionBanner(
                        errorMessage = errorMsg,
                        onRetry = {
                            val available = NetworkConnectivityHelper.isNetworkAvailable(context)
                            if (available) {
                                networkFailureMessage = null
                                pendingGenerationAction?.invoke()
                                pendingGenerationAction = null
                                Toast.makeText(context, "Conexión restablecida con éxito.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Aún no hay conexión a Internet. Verifique su red Wi-Fi o datos.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onDismiss = {
                            networkFailureMessage = null
                            pendingGenerationAction = null
                        }
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedRole.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedRole.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedRole.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedRole == UserRoleMode.MANAGER,
                    onClick = { selectedRole = UserRoleMode.MANAGER },
                    modifier = Modifier.testTag("role_manager_tab"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Vista Gerente", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedRole == UserRoleMode.RECEPTIONIST,
                    onClick = { selectedRole = UserRoleMode.RECEPTIONIST },
                    modifier = Modifier.testTag("role_reception_tab"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Vista Recepción", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            when (selectedRole) {
                UserRoleMode.MANAGER -> {
                    ManagerInterfaceContent(
                        managerMode = managerMode,
                        onManagerModeChange = { newMode ->
                            if (NetworkConnectivityHelper.isNetworkAvailable(context)) {
                                managerMode = newMode
                                networkFailureMessage = null
                            } else {
                                networkFailureMessage = "Se requiere conexión a Internet para cambiar el modo de vinculación."
                                pendingGenerationAction = { managerMode = newMode }
                                Toast.makeText(context, "Se requiere conexión a Internet.", Toast.LENGTH_LONG).show()
                            }
                        },
                        currentPin = currentPin,
                        currentQrSessionToken = currentQrSessionToken,
                        pinCountdownText = pinCountdownText,
                        qrCountdownText = qrCountdownText,
                        linkedDevices = linkedDevices,
                        onGenerateNewPin = {
                            if (NetworkConnectivityHelper.isNetworkAvailable(context)) {
                                viewModel.generateNewPin()
                                networkFailureMessage = null
                            } else {
                                networkFailureMessage = "No se pudo generar el PIN: Sin conexión a Internet."
                                pendingGenerationAction = { viewModel.generateNewPin() }
                                Toast.makeText(context, "Se requiere conexión a Internet.", Toast.LENGTH_LONG).show()
                            }
                        },
                        onGenerateNewQr = {
                            if (NetworkConnectivityHelper.isNetworkAvailable(context)) {
                                viewModel.generateNewQrToken()
                                networkFailureMessage = null
                            } else {
                                networkFailureMessage = "No se pudo generar el QR: Sin conexión a Internet."
                                pendingGenerationAction = { viewModel.generateNewQrToken() }
                                Toast.makeText(context, "Se requiere conexión a Internet.", Toast.LENGTH_LONG).show()
                            }
                        },
                        onUnlinkDevice = { viewModel.unlinkDevice(it, context) },
                        onUpdateDeviceStatus = { device, status ->
                            viewModel.updateDeviceStatus(device, status)
                        },
                        onManualLinkDevice = { name, userAssigned ->
                            viewModel.linkDevice(name = name, userAssigned = userAssigned)
                        }
                    )
                }

                UserRoleMode.RECEPTIONIST -> {
                    ReceptionistInterfaceContent(
                        receptionMode = receptionMode,
                        onReceptionModeChange = { receptionMode = it },
                        currentPin = currentPin,
                        pinValidationResult = pinValidationResult,
                        decodedQrPayload = decodedQrPayload,
                        linkedDevices = linkedDevices,
                        onValidatePin = { viewModel.validatePin(it) },
                        onDecodeQrToken = { viewModel.decodeQrToken(it) },
                        onLinkDevice = { name, userAssigned, deviceId ->
                            viewModel.linkDevice(name = name, userAssigned = userAssigned, deviceId = deviceId ?: "DEV-${System.currentTimeMillis().toString().takeLast(6)}")
                        },
                        onClearValidationResult = { viewModel.clearPinValidationResult() },
                        onOpenQrScanner = { showCameraScannerDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showCameraScannerDialog) {
        CameraXQrScannerDialog(
            onDismiss = { showCameraScannerDialog = false },
            onQrCodeDetected = { rawQrString ->
                showCameraScannerDialog = false
                scannedQrResultFromCamera = rawQrString
                Toast.makeText(context, "Código QR escaneado con éxito", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==========================================
// PERSISTENT STATUS INDICATOR BAR
// ==========================================
@Composable
private fun NetworkStatusIndicatorBar(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        label = "status_bg_color"
    )
    val contentColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
    val dotColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusText = if (isOnline) "En línea • Conexión a Internet activa" else "Sin conexión a Internet • Modo desconectado"

    Surface(
        color = backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .testTag("network_status_indicator_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Icon(
                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// ==========================================
// RETRY CONNECTION BANNER (ON NETWORK FAILURE)
// ==========================================
@Composable
private fun RetryConnectionBanner(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("retry_connection_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Fallo de Conexión de Red",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBF360C)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE65100),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("retry_connection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reintentar Conexión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// MANAGER INTERFACE CONTENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerInterfaceContent(
    managerMode: ManagerLinkingMode,
    onManagerModeChange: (ManagerLinkingMode) -> Unit,
    currentPin: String,
    currentQrSessionToken: String,
    pinCountdownText: String = "05:00",
    qrCountdownText: String = "05:00",
    linkedDevices: List<DeviceEntity>,
    onGenerateNewPin: () -> Unit,
    onGenerateNewQr: () -> Unit,
    onUnlinkDevice: (DeviceEntity) -> Unit,
    onUpdateDeviceStatus: (DeviceEntity, String) -> Unit,
    onManualLinkDevice: (String, String) -> Unit
) {
    val context = LocalContext.current
    var showManualAddDialog by remember { mutableStateOf(false) }
    var pendingNetworkConfirmationMode by remember { mutableStateOf<ManagerLinkingMode?>(null) }

    val activeCount = remember(linkedDevices) {
        linkedDevices.count { it.isCurrentlyActive() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compact Device Counters Row
        CompactDeviceCountersRow(
            totalDevices = linkedDevices.size,
            activeDevices = activeCount
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Generación de Código de Vinculación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Seleccione el método que proporcionará al personal de recepción para autorizar su dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = managerMode == ManagerLinkingMode.QR,
                        onClick = { onManagerModeChange(ManagerLinkingMode.QR) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("qr_option_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Código QR Temporal")
                        }
                    }

                    SegmentedButton(
                        selected = managerMode == ManagerLinkingMode.PIN,
                        onClick = { onManagerModeChange(ManagerLinkingMode.PIN) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("pin_option_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("PIN Seguro")
                        }
                    }
                }
            }
        }

        when (managerMode) {
            ManagerLinkingMode.QR -> {
                QrDisplayCard(
                    qrSessionToken = currentQrSessionToken,
                    countdownText = qrCountdownText,
                    onRefreshQr = { pendingNetworkConfirmationMode = ManagerLinkingMode.QR },
                    onCopyToken = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("QR Token", currentQrSessionToken)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Token QR copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            ManagerLinkingMode.PIN -> {
                PinDisplayCard(
                    pin = currentPin,
                    countdownText = pinCountdownText,
                    onRefreshPin = { pendingNetworkConfirmationMode = ManagerLinkingMode.PIN },
                    onCopyPin = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("PIN Vinculación", currentPin)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "PIN copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        LinkedDevicesCard(
            devices = linkedDevices,
            onUnlink = onUnlinkDevice,
            onToggleStatus = { device ->
                val newStatus = if (device.connectionStatus == DeviceConnectionStatus.CONNECTED) {
                    DeviceConnectionStatus.DISCONNECTED
                } else {
                    DeviceConnectionStatus.CONNECTED
                }
                onUpdateDeviceStatus(device, newStatus)
            },
            onAddDeviceClick = { showManualAddDialog = true }
        )

        // Section for the last 3 linking events
        RecentLinkingEventsCard(devices = linkedDevices)
    }

    if (pendingNetworkConfirmationMode != null) {
        NetworkConfirmationDialog(
            isQr = pendingNetworkConfirmationMode == ManagerLinkingMode.QR,
            onDismiss = { pendingNetworkConfirmationMode = null },
            onConfirm = {
                val mode = pendingNetworkConfirmationMode
                pendingNetworkConfirmationMode = null
                if (mode == ManagerLinkingMode.QR) {
                    onGenerateNewQr()
                } else if (mode == ManagerLinkingMode.PIN) {
                    onGenerateNewPin()
                }
            }
        )
    }

    if (showManualAddDialog) {
        ManualLinkDeviceDialog(
            onDismiss = { showManualAddDialog = false },
            onConfirm = { name, userAssigned ->
                onManualLinkDevice(name, userAssigned)
                showManualAddDialog = false
            }
        )
    }
}

// ==========================================
// RECEPTIONIST INTERFACE CONTENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceptionistInterfaceContent(
    receptionMode: ReceptionistInputMode,
    onReceptionModeChange: (ReceptionistInputMode) -> Unit,
    currentPin: String,
    pinValidationResult: PinValidationResult?,
    decodedQrPayload: String?,
    linkedDevices: List<DeviceEntity>,
    onValidatePin: (String) -> Unit,
    onDecodeQrToken: (String) -> Unit,
    onLinkDevice: (String, String, String?) -> Unit,
    onClearValidationResult: () -> Unit,
    onOpenQrScanner: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var qrTokenInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Autorización de Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ingrese el PIN proporcionado por el Gerente o escanee el Código QR generado en la pantalla principal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = receptionMode == ReceptionistInputMode.PIN,
                        onClick = {
                            onReceptionModeChange(ReceptionistInputMode.PIN)
                            onClearValidationResult()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("reception_pin_option_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Validar por PIN")
                        }
                    }

                    SegmentedButton(
                        selected = receptionMode == ReceptionistInputMode.QR,
                        onClick = {
                            onReceptionModeChange(ReceptionistInputMode.QR)
                            onClearValidationResult()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("reception_qr_option_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Escanear QR")
                        }
                    }
                }
            }
        }

        when (receptionMode) {
            ReceptionistInputMode.PIN -> {
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Validación de PIN de Seguridad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    pinInput = it
                                    if (it.length == 6) {
                                        onValidatePin(it)
                                    }
                                }
                            },
                            label = { Text("PIN de 6 dígitos") },
                            placeholder = { Text("Ej: 482910") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reception_pin_input")
                        )

                        Button(
                            onClick = {
                                if (pinInput.isNotBlank()) {
                                    onValidatePin(pinInput)
                                } else {
                                    Toast.makeText(context, "Ingrese el PIN de vinculación", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("validate_pin_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validar y Autorizar Terminal")
                        }

                        pinValidationResult?.let { result ->
                            when (result) {
                                is PinValidationResult.Valid -> {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32)
                                            )
                                            Text(
                                                text = "PIN válido. Terminal autorizada correctamente.",
                                                color = Color(0xFF2E7D32),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                is PinValidationResult.IncorrectPin -> {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp),
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
                                                tint = Color(0xFFC62828)
                                            )
                                            Text(
                                                text = "PIN incorrecto. Verifique el PIN generado.",
                                                color = Color(0xFFC62828),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                is PinValidationResult.InvalidFormat -> {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp),
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
                                                tint = Color(0xFFC62828)
                                            )
                                            Text(
                                                text = "Formato de PIN inválido (debe tener entre 4 y 8 dígitos).",
                                                color = Color(0xFFC62828),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                is PinValidationResult.RateLimited -> {
                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = Color(0xFFE65100)
                                            )
                                            Text(
                                                text = "Demasiados intentos. Espere ${result.remainingSeconds} segundos.",
                                                color = Color(0xFFE65100),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ReceptionistInputMode.QR -> {
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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Escaneo de Código QR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = onOpenQrScanner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_start_camera_qr_scan"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir Cámara y Escanear QR", fontWeight = FontWeight.Bold)
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "O pegue el token de sesión manualmente:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = qrTokenInput,
                            onValueChange = { qrTokenInput = it },
                            label = { Text("Token QR de Sesión") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reception_qr_token_input")
                        )

                        OutlinedButton(
                            onClick = {
                                if (qrTokenInput.isNotBlank()) {
                                    onDecodeQrToken(qrTokenInput)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Validar Token Manualmente")
                        }

                        decodedQrPayload?.let { payload ->
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "QR Decodificado con Éxito:",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = payload,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// QR DISPLAY CARD
// ==========================================
@Composable
private fun QrDisplayCard(
    qrSessionToken: String,
    countdownText: String,
    onRefreshQr: () -> Unit,
    onCopyToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_display_card"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Código QR de Vinculación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SimulatedQrCanvas(token = qrSessionToken)
                }
            }

            Text(
                text = "Pida a la terminal de recepción que escanee este código desde la pestaña 'Vista Recepción'",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyToken,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar Token")
                }

                Button(
                    onClick = onRefreshQr,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Regenerar QR")
                }
            }
        }
    }
}

// ==========================================
// PIN DISPLAY CARD
// ==========================================
@Composable
private fun PinDisplayCard(
    pin: String,
    countdownText: String,
    onRefreshPin: () -> Unit,
    onCopyPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pin_display_card"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "PIN de Vinculación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = pin.ifBlank { "------" },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            Text(
                text = "Proporcione este PIN al personal de recepción para ingresar manualmente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyPin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar PIN")
                }

                Button(
                    onClick = onRefreshPin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generar Nuevo")
                }
            }
        }
    }
}

// ==========================================
// LINKED DEVICES MANAGEMENT CARD
// ==========================================
@Composable
private fun LinkedDevicesCard(
    devices: List<DeviceEntity>,
    onUnlink: (DeviceEntity) -> Unit,
    onToggleStatus: (DeviceEntity) -> Unit,
    onAddDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("linked_devices_card"),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dispositivos Vinculados (${devices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAddDeviceClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Manual", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (devices.isEmpty()) {
                Text(
                    text = "No hay terminales vinculadas actualmente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                devices.forEach { device ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        DeviceItemRow(
                            device = device,
                            onUnlink = { onUnlink(device) },
                            onToggleStatus = { onToggleStatus(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItemRow(
    device: DeviceEntity,
    onUnlink: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isConnected = device.connectionStatus == DeviceConnectionStatus.CONNECTED

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Asignado a: ${device.userAssigned} • Estado: ${device.connectionStatus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Lock else Icons.Default.CheckCircle,
                        contentDescription = "Alternar Estado",
                        tint = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                IconButton(onClick = onUnlink) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Desvincular",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ==========================================
// SIMULATED QR CANVAS
// ==========================================
@Composable
private fun SimulatedQrCanvas(token: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(160.dp)) {
        val hash = abs(token.hashCode())
        val gridSize = 10
        val cellSize = size.width / gridSize

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val shouldDraw = ((hash + i * 7 + j * 13) % 2 == 0) ||
                        (i in 0..2 && j in 0..2) ||
                        (i in 7..9 && j in 0..2) ||
                        (i in 0..2 && j in 7..9)
                if (shouldDraw) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(i * cellSize, j * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

// ==========================================
// MANUAL LINK DEVICE DIALOG
// ==========================================
@Composable
private fun ManualLinkDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var userAssigned by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Vincular Terminal Manualmente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Dispositivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = userAssigned,
                    onValueChange = { userAssigned = it },
                    label = { Text("Usuario Asignado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, userAssigned.ifBlank { "General" })
                            }
                        }
                    ) {
                        Text("Vincular")
                    }
                }
            }
        }
    }
}

// ==========================================
// CAMERAX QR SCANNER DIALOG
// ==========================================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXQrScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeDetected: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreviewContent(
                    onQrCodeDetected = onQrCodeDetected,
                    onClose = onDismiss
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permiso de Cámara Requerido",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Se requiere acceso a la cámara para escanear el código QR de vinculación.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Conceder Permiso")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    onQrCodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isTorchEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val executor = Executors.newSingleThreadExecutor()
                    val reader = MultiFormatReader()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val buffer: ByteBuffer = imageProxy.planes[0].buffer
                        val data = ByteArray(buffer.remaining())
                        buffer.get(data)

                        val source = PlanarYUVLuminanceSource(
                            data,
                            imageProxy.width,
                            imageProxy.height,
                            0,
                            0,
                            imageProxy.width,
                            imageProxy.height,
                            false
                        )
                        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                        try {
                            val result = reader.decodeWithState(binaryBitmap)
                            val rawText = result.text
                            if (!rawText.isNullOrBlank()) {
                                ContextCompat.getMainExecutor(ctx).execute {
                                    onQrCodeDetected(rawText)
                                }
                            }
                        } catch (_: Exception) {
                            // Frame sin QR decodificable
                        } finally {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = cam
                    } catch (exc: Exception) {
                        Log.e("CameraX", "Fallo al iniciar cámara", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isTorchEnabled = !isTorchEnabled
                    cameraControl?.cameraControl?.enableTorch(isTorchEnabled)
                }
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Linterna",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ==========================================
// COMPACT DEVICE COUNTERS ROW
// ==========================================
@Composable
private fun CompactDeviceCountersRow(
    totalDevices: Int,
    activeDevices: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_device_counters_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Total Dispositivos
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = totalDevices.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Total dispositivos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Dispositivos Activos
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFF2E7D32)
                        )
                    }
                }
                Column {
                    Text(
                        text = activeDevices.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Dispositivos activos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// NETWORK CONFIRMATION DIALOG
// ==========================================
@Composable
private fun NetworkConfirmationDialog(
    isQr: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val isOnline = NetworkConnectivityHelper.isNetworkAvailable(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isOnline) Icons.Default.NetworkCheck else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (isQr) "Generar Nuevo Código QR" else "Generar Nuevo PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isOnline) "Red activa y verificada" else "Sin conexión de red estable",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }

                Text(
                    text = if (isOnline) {
                        "¿Desea generar un nuevo código ${if (isQr) "QR" else "PIN"} de vinculación? El código anterior quedará invalidado y el nuevo tendrá 5 minutos de vigencia."
                    } else {
                        "Atención: Se detectó señal de red inestable o desconectada. Generar un código en este estado podría impedir que las terminales lo validen hasta reconectarse."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOnline) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                )
            ) {
                Text(if (isOnline) "Confirmar y Generar" else "Generar de todos modos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ==========================================
// RECENT LINKING EVENTS CARD (LAST 3 EVENTS)
// ==========================================
@Composable
private fun RecentLinkingEventsCard(
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val recentEvents = remember(devices) {
        devices.sortedByDescending { it.timestamp }.take(3)
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_linking_events_card"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Últimos Eventos de Vinculación (${recentEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (recentEvents.isEmpty()) {
                Text(
                    text = "No hay eventos recientes registrados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                recentEvents.forEach { event ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (event.connectionStatus == DeviceConnectionStatus.CONNECTED) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (event.connectionStatus == DeviceConnectionStatus.CONNECTED) Icons.Default.CheckCircle else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (event.connectionStatus == DeviceConnectionStatus.CONNECTED) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = event.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Usuario: ${event.userAssigned}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Text(
                                    text = dateFormat.format(Date(event.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
