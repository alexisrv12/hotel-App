package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.ui.viewmodel.DeviceLinkingViewModel
import com.example.utils.PinValidationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    // UI States
    var selectedRole by remember { mutableStateOf(UserRoleMode.MANAGER) }
    var managerMode by remember { mutableStateOf(ManagerLinkingMode.QR) }
    var receptionMode by remember { mutableStateOf(ReceptionistInputMode.PIN) }

    // Observe ViewModel flows
    val linkedDevices by viewModel.linkedDevices.collectAsState()
    val currentPin by viewModel.currentPin.collectAsState()
    val currentQrSessionToken by viewModel.currentQrSessionToken.collectAsState()
    val pinCountdownText by viewModel.pinCountdownText.collectAsState()
    val qrCountdownText by viewModel.qrCountdownText.collectAsState()
    val decodedQrPayload by viewModel.decodedQrSessionPayload.collectAsState()
    val pinValidationResult by viewModel.pinValidationResult.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    // Show toast / snackbar on userMessage change
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
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
            // Role Selection Tab Row (Gerente vs Recepcionista)
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

            // Main Content Area
            when (selectedRole) {
                UserRoleMode.MANAGER -> {
                    ManagerInterfaceContent(
                        managerMode = managerMode,
                        onManagerModeChange = { managerMode = it },
                        currentPin = currentPin,
                        currentQrSessionToken = currentQrSessionToken,
                        pinCountdownText = pinCountdownText,
                        qrCountdownText = qrCountdownText,
                        linkedDevices = linkedDevices,
                        onGenerateNewPin = { viewModel.generateNewPin() },
                        onGenerateNewQr = { viewModel.generateNewQrToken() },
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
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// MANAGER INTERFACE CONTENT
// ==========================================
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                            Text("PIN Seguro de 6 Dígitos")
                        }
                    }
                }
            }
        }

        // Display Active Token according to mode
        when (managerMode) {
            ManagerLinkingMode.QR -> {
                QrDisplayCard(
                    qrSessionToken = currentQrSessionToken,
                    countdownText = qrCountdownText,
                    onRefreshQr = onGenerateNewQr,
                    onCopyToken = {
                        copyToClipboard(context, "Código QR Token", currentQrSessionToken)
                    }
                )
            }

            ManagerLinkingMode.PIN -> {
                PinDisplayCard(
                    pin = currentPin,
                    countdownText = pinCountdownText,
                    onRefreshPin = onGenerateNewPin,
                    onCopyPin = {
                        copyToClipboard(context, "PIN de Vinculación", currentPin)
                    }
                )
            }
        }

        // Linked Devices Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
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
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Registrar Manual")
            }
        }

        // Linked Devices List
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
        ManualDeviceAddDialog(
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
@Composable
private fun ReceptionistInterfaceContent(
    receptionMode: ReceptionistInputMode,
    onReceptionModeChange: (ReceptionistInputMode) -> Unit,
    currentPin: String,
    pinValidationResult: PinValidationResult?,
    decodedQrPayload: String?,
    linkedDevices: List<DeviceEntity>,
    onValidatePin: (String) -> PinValidationResult,
    onDecodeQrToken: (String) -> String?,
    onLinkDevice: (String, String, String) -> Unit,
    onClearValidationResult: () -> Unit
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    var inputQrToken by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("Caja Recepción 1") }
    var userAssigned by remember { mutableStateOf("Recepcionista Turno Día") }
    var generatedDeviceId by remember {
        mutableStateOf("DEV-" + (1000..9999).random())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                    text = "Vinculación de Terminal de Recepción",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ingrese los datos de la estación y valide el PIN o código QR proporcionado por la gerencia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input Mode Toggle
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = receptionMode == ReceptionistInputMode.PIN,
                        onClick = {
                            onReceptionModeChange(ReceptionistInputMode.PIN)
                            onClearValidationResult()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
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
                            Text("Ingresar PIN")
                        }
                    }

                    SegmentedButton(
                        selected = receptionMode == ReceptionistInputMode.QR,
                        onClick = {
                            onReceptionModeChange(ReceptionistInputMode.QR)
                            onClearValidationResult()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
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
                            Text("Escanear/Pegar QR")
                        }
                    }
                }
            }
        }

        // Form Fields (Device Details)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Información del Dispositivo Local",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Nombre de la Terminal / Dispositivo") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Smartphone, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = userAssigned,
                    onValueChange = { userAssigned = it },
                    label = { Text("Usuario Asignado / Operador") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_assigned_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID del Dispositivo: $generatedDeviceId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { generatedDeviceId = "DEV-" + (1000..9999).random() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerar ID",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Input according to Mode (PIN vs QR)
        when (receptionMode) {
            ReceptionistInputMode.PIN -> {
                PinInputCard(
                    inputPin = inputPin,
                    onPinChange = { inputPin = it },
                    pinValidationResult = pinValidationResult,
                    onValidateAndLink = {
                        val result = onValidatePin(inputPin)
                        if (result is PinValidationResult.Valid) {
                            if (deviceName.isBlank()) {
                                Toast.makeText(context, "Ingrese el nombre del dispositivo", Toast.LENGTH_SHORT).show()
                            } else {
                                onLinkDevice(deviceName, userAssigned, generatedDeviceId)
                                inputPin = ""
                            }
                        }
                    }
                )
            }

            ReceptionistInputMode.QR -> {
                QrInputCard(
                    inputQrToken = inputQrToken,
                    onTokenChange = { inputQrToken = it },
                    decodedQrPayload = decodedQrPayload,
                    onDecodeAndLink = {
                        if (inputQrToken.isBlank()) {
                            Toast.makeText(context, "Ingrese o pegue un token de QR válido", Toast.LENGTH_SHORT).show()
                            return@QrInputCard
                        }
                        val decoded = onDecodeQrToken(inputQrToken)
                        if (decoded != null && decoded.startsWith("LINK_SESSION")) {
                            if (deviceName.isBlank()) {
                                Toast.makeText(context, "Ingrese el nombre del dispositivo", Toast.LENGTH_SHORT).show()
                            } else {
                                onLinkDevice(deviceName, userAssigned, generatedDeviceId)
                                inputQrToken = ""
                            }
                        } else {
                            Toast.makeText(context, "Token QR inválido o expirarado", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Currently Registered Devices Overview for Reception
        Text(
            text = "Estado de Sincronización Local",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (linkedDevices.isEmpty()) {
            EmptyDevicesPlaceholder()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                linkedDevices.forEach { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatusDot(status = device.connectionStatus)
                                Column {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Asignado a: ${device.userAssigned}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            StatusBadge(status = device.connectionStatus)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
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
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Escanear con Terminal Recepción",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Válido durante $countdownText (5 min máx)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
private fun QrCanvasMatrix(
    qrDataString: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val darkColor = Color(0xFF1E1E1E)

    Canvas(modifier = modifier) {
        val gridSize = 19
        val cellSize = size.width / gridSize
        val hash = abs(qrDataString.hashCode())

        // Draw background
        drawRect(color = Color.White, size = size)

        // Draw QR Finder Patterns (Corners)
        fun drawFinderPattern(startX: Int, startY: Int) {
            val left = startX * cellSize
            val top = startY * cellSize
            val patternSize = 5 * cellSize
            // Outer black square
            drawRect(
                color = darkColor,
                topLeft = Offset(left, top),
                size = Size(patternSize, patternSize)
            )
            // Inner white square
            drawRect(
                color = Color.White,
                topLeft = Offset(left + cellSize, top + cellSize),
                size = Size(patternSize - 2 * cellSize, patternSize - 2 * cellSize)
            )
            // Center black square
            drawRect(
                color = darkColor,
                topLeft = Offset(left + 2 * cellSize, top + 2 * cellSize),
                size = Size(cellSize, cellSize)
            )
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(gridSize - 5, 0)
        drawFinderPattern(0, gridSize - 5)

        // Deterministic pseudo modules based on hash & position
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                // Skip finder pattern zones
                val inTopLeft = row < 5 && col < 5
                val inTopRight = row < 5 && col >= gridSize - 5
                val inBottomLeft = row >= gridSize - 5 && col < 5
                if (inTopLeft || inTopRight || inBottomLeft) continue

                val isBitOn = ((row * 31 + col * 17 + hash) % 3 == 0) || ((row + col + hash) % 5 == 0)
                if (isBitOn) {
                    drawRect(
                        color = if ((row + col) % 2 == 0) darkColor else primaryColor,
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize * 0.95f, cellSize * 0.95f)
                    )
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

            // Digit slots display
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pin.forEach { digit ->
                    Surface(
                        modifier = Modifier.size(width = 42.dp, height = 54.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = digit.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Válido por 5 minutos ($countdownText restante). Dictar o mostrar al operador de recepción.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

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
                                    text = "PIN Incorrecto. Verifique con gerencia.",
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
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF6C00)
                                )
                                Text(
                                    text = "Formato de PIN no válido (debe tener 6 dígitos numéricos).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    is PinValidationResult.RateLimited -> {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Límite excedido. Intente de nuevo en ${pinValidationResult.remainingSeconds} seg.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB71C1C),
                                modifier = Modifier.padding(12.dp)
                            )
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
                Icon(imageVector = Icons.Default.Link, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validar PIN y Vincular")
            }
        }
    }
}

@Composable
private fun QrInputCard(
    inputQrToken: String,
    onTokenChange: (String) -> Unit,
    decodedQrPayload: String?,
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
            Text(
                text = "Escanear / Pegar Código QR",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = inputQrToken,
                onValueChange = onTokenChange,
                label = { Text("Token QR (Base64)") },
                placeholder = { Text("Pegue el token de QR generado aquí...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_input_field"),
                minLines = 2,
                maxLines = 3
            )

            if (!decodedQrPayload.isNullByBlank()) {
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
private fun StatusDot(status: String) {
    val color = when (status) {
        DeviceConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
        DeviceConnectionStatus.DISCONNECTED -> Color(0xFFC62828)
        else -> Color(0xFFEF6C00)
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        DeviceConnectionStatus.CONNECTED -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF1B5E20),
            "Conectado"
        )

        DeviceConnectionStatus.DISCONNECTED -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFB71C1C),
            "Desconectado"
        )

        else -> Triple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            "Pendiente"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun EmptyDevicesPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "No hay dispositivos vinculados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Utilice el código PIN o QR para autorizar terminales de recepción.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ManualDeviceAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var userAssigned by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Dispositivo Manualmente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Dispositivo") },
                    placeholder = { Text("Ej. Pos Recepción 2") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = userAssigned,
                    onValueChange = { userAssigned = it },
                    label = { Text("Usuario Asignado") },
                    placeholder = { Text("Ej. Recepcionista Noche") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, userAssigned.ifBlank { "Sin asignar" })
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun String?.isNullByBlank(): Boolean {
    return this.isNullOrBlank()
}
