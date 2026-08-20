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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
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

    // UI Navigation & Mode States
    var selectedRole by remember { mutableStateOf(UserRoleMode.MANAGER) }
    var managerMode by remember { mutableStateOf(ManagerLinkingMode.QR) }
    var receptionMode by remember { mutableStateOf(ReceptionistInputMode.PIN) }

    // Real-time network connectivity state
    val isOnline by remember {
        NetworkConnectivityHelper.observeNetworkConnectivity(context)
    }.collectAsState(initial = NetworkConnectivityHelper.isNetworkAvailable(context))

    // Track network failure & pending generation action for the "Retry Connection" button
    var networkFailureMessage by remember { mutableStateOf<String?>(null) }
    var pendingGenerationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // CameraX QR Scanner Dialog State
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var scannedQrResultFromCamera by remember { mutableStateOf<String?>(null) }

    // Observers from ViewModel
    val linkedDevices by viewModel.linkedDevices.collectAsState()
    val currentPin by viewModel.currentPin.collectAsState()
    val currentQrSessionToken by viewModel.currentQrSessionToken.collectAsState()
    val pinCountdownText by viewModel.pinCountdownText.collectAsState()
    val qrCountdownText by viewModel.qrCountdownText.collectAsState()
    val decodedQrPayload by viewModel.decodedQrSessionPayload.collectAsState()
    val pinValidationResult by viewModel.pinValidationResult.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    // Auto-clear network failure state when connection is restored
    LaunchedEffect(isOnline) {
        if (isOnline && networkFailureMessage != null) {
            networkFailureMessage = null
        }
    }

    // Show toast / snackbar on userMessage change
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Handle QR code received from camera scanner
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
                    // Quick Action: Open CameraX QR Scanner
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
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // 1. Persistent Real-Time Network Status Indicator Bar (Fijo)
            NetworkStatusIndicatorBar(isOnline = isOnline)

            // 2. Retry Connection Banner (Appears only when network check fails - Fijo)
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

            // 3. Role Selection Tab Row (Gerente vs Recepcionista - Fijo en la parte superior)
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
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
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
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Vista Recepción", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            // 4. Scrollable Content Area: Toma todo el espacio restante con weight(1f)
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                when (selectedRole) {
                    UserRoleMode.MANAGER -> {
                        ManagerInterfaceContent(
                            managerMode = managerMode,
                            onManagerModeChange = { newMode ->
                                if (NetworkConnectivityHelper.isNetworkAvailable(context)) {
                                    managerMode = newMode
                                    networkFailureMessage = null
                                } else {
                                    networkFailureMessage = "Se requiere conexión a Internet (Wi-Fi o Datos) para cambiar y generar el código de vinculación."
                                    pendingGenerationAction = { managerMode = newMode }
                                    Toast.makeText(
                                        context,
                                        "Se requiere conexión a Internet (Wi-Fi o Datos) para generar el código de vinculación.",
                                        Toast.LENGTH_LONG
                                    ).show()
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
                                    networkFailureMessage = "No se pudo generar el PIN: Se requiere conexión a Internet (Wi-Fi o Datos)."
                                    pendingGenerationAction = { viewModel.generateNewPin() }
                                    Toast.makeText(
                                        context,
                                        "Se requiere conexión a Internet (Wi-Fi o Datos) para generar el código de vinculación.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onGenerateNewQr = {
                                if (NetworkConnectivityHelper.isNetworkAvailable(context)) {
                                    viewModel.generateNewQrToken()
                                    networkFailureMessage = null
                                } else {
                                    networkFailureMessage = "No se pudo generar el Código QR: Se requiere conexión a Internet (Wi-Fi o Datos)."
                                    pendingGenerationAction = { viewModel.generateNewQrToken() }
                                    Toast.makeText(
                                        context,
                                        "Se requiere conexión a Internet (Wi-Fi o Datos) para generar el código de vinculación.",
                                        Toast.LENGTH_LONG
                                    ).show()
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
                            onValidatePin = { inputPin ->
                                viewModel.validatePin(inputPin)
                            },
                            onDecodeQrToken = { token ->
                                viewModel.decodeQrToken(token)
                            },
                            onLinkDevice = { name, userAssigned, deviceId ->
                                viewModel.linkDevice(
                                    name = name,
                                    userAssigned = userAssigned,
                                    deviceId = deviceId
                                )
                            },
                            onClearValidationResult = {
                                viewModel.clearPinValidationResult()
                            },
                            onOpenQrScanner = {
                                showCameraScannerDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // CameraX Scanner Dialog
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
            // Status Dot
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector (QR vs PIN)
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

        // Active Authorization Display Card
        when (managerMode) {
            ManagerLinkingMode.QR -> {
                QrDisplayCard(
                    qrSessionToken = currentQrSessionToken,
                    countdownText = qrCountdownText,
                    onRefreshQr = onGenerateNewQr,
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
                    onRefreshPin = onGenerateNewPin,
                    onCopyPin = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("PIN", currentPin)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "PIN copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Linked Devices List Header
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
                    text = "Dispositivos Vinculados (${linkedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { showManualAddDialog = true },
                modifier = Modifier.testTag("manual_add_device_button")
            ) {
                Text("+ Añadir Manual")
            }
        }

        // Devices Items
        if (linkedDevices.isEmpty()) {
            EmptyDevicesPlaceholder()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                linkedDevices.forEach { device ->
                    LinkedDeviceItemCard(
                        device = device,
                        onUnlink = { onUnlinkDevice(device) },
                        onToggleStatus = {
                            val newStatus = if (device.connectionStatus == DeviceConnectionStatus.CONNECTED) {
                                DeviceConnectionStatus.DISCONNECTED
                            } else {
                                DeviceConnectionStatus.CONNECTED
                            }
                            onUpdateDeviceStatus(device, newStatus)
                        }
                    )
                }
            }
        }
    }

    if (showManualAddDialog) {
        ManualAddDeviceDialog(
            onDismiss = { showManualAddDialog = false },
            onConfirm = { name, assignedUser ->
                onManualLinkDevice(name, assignedUser)
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
    onLinkDevice: (String, String, String) -> Unit,
    onClearValidationResult: () -> Unit,
    onOpenQrScanner: () -> Unit
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    var inputQrToken by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("Tablet Recepción 1") }
    var receptionistName by remember { mutableStateOf("Recepcionista Turno Día") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Autorización de Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ingrese las credenciales provistas por la administración o escanee el código QR con la cámara.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Camera Quick Action Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenQrScanner() }
                .testTag("scan_qr_camera_quick_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Escanear QR con Cámara",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Apunta al código QR del Gerente para auto-vincular",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Mode switch
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = receptionMode == ReceptionistInputMode.PIN,
                onClick = { onReceptionModeChange(ReceptionistInputMode.PIN) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("reception_pin_mode_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Ingresar PIN")
                }
            }

            SegmentedButton(
                selected = receptionMode == ReceptionistInputMode.QR,
                onClick = { onReceptionModeChange(ReceptionistInputMode.QR) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("reception_qr_mode_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Pegar Token QR")
                }
            }
        }

        // Terminal details configuration
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
                    text = "Datos de Identificación del Dispositivo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Nombre del Dispositivo") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Smartphone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = receptionistName,
                    onValueChange = { receptionistName = it },
                    label = { Text("Usuario Responsable") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Action Input forms
        when (receptionMode) {
            ReceptionistInputMode.PIN -> {
                PinInputCard(
                    inputPin = inputPin,
                    onPinChange = {
                        inputPin = it
                        onClearValidationResult()
                    },
                    pinValidationResult = pinValidationResult,
                    onValidateAndLink = {
                        if (inputPin.length == 6) {
                            onValidatePin(inputPin)
                            if (inputPin == currentPin) {
                                onLinkDevice(deviceName, receptionistName, "DEV-${System.currentTimeMillis() % 100000}")
                                Toast.makeText(context, "¡Terminal vinculada exitosamente!", Toast.LENGTH_SHORT).show()
                                inputPin = ""
                            }
                        } else {
                            Toast.makeText(context, "El PIN debe contener 6 dígitos.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            ReceptionistInputMode.QR -> {
                QrInputCard(
                    inputQrToken = inputQrToken,
                    onTokenChange = { inputQrToken = it },
                    decodedQrPayload = decodedQrPayload,
                    onOpenScanner = onOpenQrScanner,
                    onDecodeAndLink = {
                        onDecodeQrToken(inputQrToken)
                        onLinkDevice(deviceName, receptionistName, "DEV-QR-${System.currentTimeMillis() % 100000}")
                        Toast.makeText(context, "¡Terminal vinculada mediante token QR!", Toast.LENGTH_SHORT).show()
                        inputQrToken = ""
                    }
                )
            }
        }
    }
}

// ==========================================
// CAMERAX QR SCANNER DIALOG COMPONENT
// ==========================================
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraXQrScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var hasDetectedCode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Escanear Código QR", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        if (cameraPermissionState.status.isGranted) {
                            IconButton(
                                onClick = {
                                    isTorchOn = !isTorchOn
                                    cameraInstance?.cameraControl?.enableTorch(isTorchOn)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                                    contentDescription = if (isTorchOn) "Apagar Linterna" else "Encender Linterna"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.8f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black)
            ) {
                if (cameraPermissionState.status.isGranted) {
                    // Live Camera View
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val executor = Executors.newSingleThreadExecutor()

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                val multiFormatReader = MultiFormatReader()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    if (!hasDetectedCode) {
                                        val qrText = decodeQrFromImage(imageProxy, multiFormatReader)
                                        if (!qrText.isNullOrBlank()) {
                                            hasDetectedCode = true
                                            previewView.post {
                                                onQrCodeDetected(qrText)
                                            }
                                        }
                                    }
                                    imageProxy.close()
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraInstance = cam
                                } catch (exc: Exception) {
                                    Log.e("CameraXScanner", "Error al iniciar cámara", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Reticle overlay
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(260.dp)
                                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        ) {}
                    }

                    // Guidance bottom chip
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp)
                    ) {
                        Text(
                            text = "Enfoca el código QR de vinculación del Gerente",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                } else {
                    // Permission Request Prompt
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Se requiere permiso de Cámara",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Para escanear códigos QR de vinculación de dispositivos en tiempo real.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() }
                        ) {
                            Text("Conceder Permiso de Cámara")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Decodifica una imagen capturada por CameraX utilizando ZXing MultiFormatReader.
 */
private fun decodeQrFromImage(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    val buffer: ByteBuffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val width = imageProxy.width
    val height = imageProxy.height

    return try {
        val source = PlanarYUVLuminanceSource(
            bytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decodeWithState(binaryBitmap)
        result.text
    } catch (_: Exception) {
        null
    }
}

// ==========================================
// HELPER CARDS & WIDGETS
// ==========================================
@Composable
private fun QrDisplayCard(
    qrSessionToken: String,
    countdownText: String = "05:00",
    onRefreshQr: () -> Unit,
    onCopyToken: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Código QR de Autorización",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Canvas QR Code Visual Representation
            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                color = Color.White
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    QrCanvasMatrix(
                        qrDataString = qrSessionToken,
                        modifier = Modifier.size(180.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Token de Sesión Base64",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = qrSessionToken,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onCopyToken) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Token",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRefreshQr,
                    modifier = Modifier.testTag("generate_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Regenerar QR")
                }
            }
        }
    }
}

@Composable
private fun PinDisplayCard(
    pin: String,
    countdownText: String = "05:00",
    onRefreshPin: () -> Unit,
    onCopyPin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PIN Temporal de 6 Dígitos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Pin Digits Boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pin.padEnd(6, '-').forEach { char ->
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = char.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Expiration and countdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Expira en $countdownText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCopyPin) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar PIN")
                }

                Button(
                    onClick = onRefreshPin,
                    modifier = Modifier.testTag("generate_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo PIN")
                }
            }
        }
    }
}

@Composable
private fun PinInputCard(
    inputPin: String,
    onPinChange: (String) -> Unit,
    pinValidationResult: PinValidationResult?,
    onValidateAndLink: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
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
                text = "Ingresar PIN Autorizado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = inputPin,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        onPinChange(it)
                    }
                },
                label = { Text("PIN de 6 dígitos") },
                placeholder = { Text("Ej. 048291") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_input_field"),
                singleLine = true
            )

            // Feedback Alert Box
            if (pinValidationResult != null) {
                when (pinValidationResult) {
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
                                    text = "¡PIN Válido! Dispositivo verificado con éxito.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.SemiBold
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
                                    text = "El PIN ingresado es incorrecto.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB71C1C),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    is PinValidationResult.InvalidFormat -> {
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
                                    text = "Formato de PIN inválido.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.SemiBold
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
                                    text = "Demasiados intentos. Espere ${pinValidationResult.remainingSeconds}s.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onValidateAndLink,
                enabled = inputPin.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("validate_pin_button")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validar y Autorizar Dispositivo")
            }
        }
    }
}

@Composable
private fun QrInputCard(
    inputQrToken: String,
    onTokenChange: (String) -> Unit,
    decodedQrPayload: String?,
    onOpenScanner: () -> Unit,
    onDecodeAndLink: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Token de Sesión QR",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onOpenScanner,
                    modifier = Modifier.testTag("scan_qr_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir Cámara")
                }
            }

            OutlinedTextField(
                value = inputQrToken,
                onValueChange = onTokenChange,
                label = { Text("Token QR (Base64)") },
                placeholder = { Text("Pegue el token de QR generado o escanee con la cámara...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_input_field"),
                minLines = 2,
                maxLines = 3
            )

            if (!decodedQrPayload.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Payload Decodificado:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = decodedQrPayload ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Button(
                onClick = onDecodeAndLink,
                enabled = inputQrToken.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("link_device_button")
            ) {
                Icon(imageVector = Icons.Default.Link, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decodificar y Vincular Terminal")
            }
        }
    }
}

@Composable
private fun LinkedDeviceItemCard(
    device: DeviceEntity,
    onUnlink: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(device.timestamp) { dateFormat.format(Date(device.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusDot(status = device.connectionStatus)
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${device.deviceId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = device.connectionStatus)
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Asignado a: ${device.userAssigned}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Vinculado el: $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onToggleStatus) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Cambiar Estado",
                            tint = MaterialTheme.colorScheme.primary
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
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor, text) = when (status) {
        DeviceConnectionStatus.CONNECTED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Conectado")
        DeviceConnectionStatus.PENDING -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        else -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Desconectado")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatusDot(status: String) {
    val color = when (status) {
        DeviceConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
        DeviceConnectionStatus.PENDING -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun EmptyDevicesPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No hay dispositivos vinculados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Genere un código QR o PIN arriba para vincular tablets o terminales de recepción.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ManualAddDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var assignedUser by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Dispositivo Manualmente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Nombre del Dispositivo") },
                    placeholder = { Text("Ej. Tablet Mostrador 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assignedUser,
                    onValueChange = { assignedUser = it },
                    label = { Text("Usuario Responsable") },
                    placeholder = { Text("Ej. Ana López") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deviceName, assignedUser) },
                enabled = deviceName.isNotBlank() && assignedUser.isNotBlank()
            ) {
                Text("Vincular")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Matriz visual Canvas para renderizar el código QR sin dependencias externas pesadas.
 */
@Composable
private fun QrCanvasMatrix(
    qrDataString: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val matrixSize = 21
        val cellSize = size.width / matrixSize
        val hash = abs(qrDataString.hashCode())

        // Fondo blanco
        drawRect(color = Color.White, size = size)

        // Patrón determinista basado en el hash del token
        for (row in 0 until matrixSize) {
            for (col in 0 until matrixSize) {
                val isFinderPattern =
                    (row < 7 && col < 7) ||
                    (row < 7 && col >= matrixSize - 7) ||
                    (row >= matrixSize - 7 && col < 7)

                val isBlack = if (isFinderPattern) {
                    val r = if (row < 7) row else row - (matrixSize - 7)
                    val c = if (col < 7) col else col - (matrixSize - 7)
                    r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)
                } else {
                    ((hash * (row + 1) * (col + 1) + row * 17 + col * 31) % 3) == 0
                }

                if (isBlack) {
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}
