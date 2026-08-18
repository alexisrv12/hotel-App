package com.example.ui.screens

import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Image
import com.example.ui.components.QRScannerView
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.viewmodel.DeviceLinkingViewModel
import com.example.utils.QrCodeImageDecoder

/**
 * First-Start Setup Wizard Screen (Asistente de Configuración para el Primer Inicio)
 * Appears when device is not yet linked to a system or after unlinking by Manager.
 */
@Composable
fun FirstStartSetupWizardScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceLinkingViewModel = viewModel(),
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val currentStep by viewModel.wizardStep.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val verificationCode by viewModel.generatedVerificationCode.collectAsState()
    val currentPin by viewModel.currentPin.collectAsState()
    val currentQrToken by viewModel.currentQrSessionToken.collectAsState()
    val uiMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Branding Header
            WizardBrandingHeader()

            // Step Progress Indicator
            WizardStepProgress(currentStep = currentStep)

            Spacer(modifier = Modifier.height(4.dp))

            // Animated Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WizardStepAnimation"
            ) { step ->
                when (step) {
                    1 -> Step1EmailRequest(
                        initialEmail = email,
                        onSendCode = { inputEmail ->
                            viewModel.sendVerificationCode(inputEmail)
                        }
                    )
                    2 -> Step2CodeVerification(
                        email = email,
                        sentCode = verificationCode,
                        onVerifyCode = { inputCode ->
                            viewModel.verifyEmailCode(inputCode)
                        },
                        onResendCode = {
                            viewModel.sendVerificationCode(email)
                        },
                        onBackToStep1 = {
                            viewModel.setWizardStep(1)
                        }
                    )
                    3 -> Step3DeviceLinkingMethod(
                        activeManagerPin = currentPin,
                        activeManagerQr = currentQrToken,
                        onConfirmPin = { pin ->
                            val success = viewModel.completeLinkingWithPin(context, pin)
                            if (success) onSetupComplete()
                        },
                        onConfirmQr = { qr ->
                            val success = viewModel.completeLinkingWithQr(context, qr)
                            if (success) onSetupComplete()
                        },
                        onBackToStep2 = {
                            viewModel.setWizardStep(2)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardBrandingHeader() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = HotelNavy,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = HotelGold,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Hotel,
                        contentDescription = null,
                        tint = HotelNavy,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Text(
                text = "Asistente de Configuración",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Hotel Rivera - Vinculación de Dispositivo",
                style = MaterialTheme.typography.bodySmall,
                color = HotelGold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WizardStepProgress(currentStep: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = "Paso $currentStep de 3",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )

                Text(
                    text = when (currentStep) {
                        1 -> "Correo Electrónico"
                        2 -> "Código de Verificación"
                        else -> "Método de Vinculación"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = HotelNavy,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepProgressCircle(stepNumber = 1, activeStep = currentStep, label = "Paso 1")
                StepProgressCircle(stepNumber = 2, activeStep = currentStep, label = "Paso 2")
                StepProgressCircle(stepNumber = 3, activeStep = currentStep, label = "Paso 3")
            }
        }
    }
}

@Composable
private fun StepProgressCircle(stepNumber: Int, activeStep: Int, label: String) {
    val isCompleted = activeStep > stepNumber
    val isCurrent = activeStep == stepNumber

    val bgColor = when {
        isCompleted -> Color(0xFF2E7D32)
        isCurrent -> HotelNavy
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isCompleted || isCurrent -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) HotelNavy else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Paso 1: Request a real email address
 */
@Composable
private fun Step1EmailRequest(
    initialEmail: String,
    onSendCode: (email: String) -> Unit
) {
    var emailInput by remember { mutableStateOf(initialEmail) }
    var isError by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = HotelNavy,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Paso 1: Correo Electrónico",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ingresa tu correo institucional o de recepcionista",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            Text(
                text = "Para comenzar el proceso de vinculación de este dispositivo, introduce una dirección de correo electrónico válida:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    isError = false
                },
                label = { Text("Correo Electrónico *") },
                placeholder = { Text("ejemplo@hotelrivera.com") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                },
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_email_input")
            )

            if (isError) {
                Text(
                    text = "Por favor ingresa una dirección de correo electrónico válida.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val trimmed = emailInput.trim()
                    val isValid = trimmed.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
                    if (isValid) {
                        onSendCode(trimmed)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("wizard_send_code_button")
            ) {
                Icon(imageVector = Icons.Default.MarkEmailRead, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar Código de Verificación", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Paso 2: Send verification code to email and validate
 */
@Composable
private fun Step2CodeVerification(
    email: String,
    sentCode: String,
    onVerifyCode: (code: String) -> Boolean,
    onResendCode: () -> Unit,
    onBackToStep1: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = HotelNavy,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Paso 2: Código de Verificación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Valida el código enviado a tu correo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            Text(
                text = "Hemos enviado un código de seguridad de 6 dígitos a:",
                style = MaterialTheme.typography.bodyMedium
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Simulated Email Banner so the user easily sees the test code!
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF8E1),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = Color(0xFFE65100)
                    )
                    Column {
                        Text(
                            text = "📧 Notificación de Correo (Simulada):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Tu código de verificación es: $sentCode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBF360C)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = codeInput,
                onValueChange = {
                    if (it.length <= 6) {
                        codeInput = it
                        isError = false
                    }
                },
                label = { Text("Código de 6 dígitos *") },
                placeholder = { Text("123456") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_code_input")
            )

            if (isError) {
                Text(
                    text = "Código incorrecto. Verifica el código e intenta de nuevo.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToStep1,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Atrás")
                }

                Button(
                    onClick = {
                        val valid = onVerifyCode(codeInput)
                        if (!valid) isError = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .testTag("wizard_verify_code_button")
                ) {
                    Text("Validar Código", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = onResendCode,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reenviar Código de Verificación")
            }
        }
    }
}

/**
 * Paso 3: Linking method selection (Scan QR or Enter PIN)
 */
@Composable
private fun Step3DeviceLinkingMethod(
    activeManagerPin: String,
    activeManagerQr: String,
    onConfirmPin: (pin: String) -> Unit,
    onConfirmQr: (qrToken: String) -> Unit,
    onBackToStep2: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = PIN, 1 = QR
    var pinInput by remember { mutableStateOf("") }
    var qrInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCameraActive by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = QrCodeImageDecoder.decodeQrFromUri(context, uri)
            if (!decoded.isNullOrBlank()) {
                qrInput = decoded
                isError = false
                onConfirmQr(decoded)
            } else {
                isError = true
                errorMessage = "No se pudo detectar un código QR legible en la imagen seleccionada."
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = HotelNavy,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Paso 3: Vinculación del Dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Elige el método generado desde el módulo Gerente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            // Manager Info Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Nota: El código PIN o QR debe haber sido generado previamente desde el Módulo Gerente > Vincular Dispositivos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Tabs for PIN vs QR
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        isError = false
                        errorMessage = null
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Código PIN")
                        }
                    },
                    modifier = Modifier.testTag("wizard_tab_pin")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        isError = false
                        errorMessage = null
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Cámara / QR")
                        }
                    },
                    modifier = Modifier.testTag("wizard_tab_qr")
                )
            }

            if (selectedTab == 0) {
                // Method 1: PIN Code
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingresa el código PIN de 6 dígitos activo provisto por el Módulo Gerente:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                pinInput = it
                                isError = false
                                errorMessage = null
                            }
                        },
                        label = { Text("Código PIN de 6 dígitos *") },
                        placeholder = { Text("Ingresa el PIN") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null)
                        },
                        isError = isError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_pin_input")
                    )

                    if (isError) {
                        Text(
                            text = errorMessage ?: "PIN o código no válido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (pinInput.isBlank()) {
                                isError = true
                                errorMessage = "PIN o código no válido"
                            } else {
                                onConfirmPin(pinInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wizard_submit_pin_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vincular y Autorizar Dispositivo", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Method 2: Live Camera QR Scanner + Image Upload Fallback
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isCameraActive) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            QRScannerView(
                                modifier = Modifier.fillMaxSize(),
                                onQrCodeScanned = { scanned ->
                                    qrInput = scanned
                                    isError = false
                                    onConfirmQr(scanned)
                                },
                                onCloseScanner = { isCameraActive = false }
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { isCameraActive = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Activar Escáner de Cámara")
                        }
                    }

                    // Fallback button to upload QR image
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_upload_qr_image_button")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subir imagen de QR")
                    }

                    OutlinedTextField(
                        value = qrInput,
                        onValueChange = {
                            qrInput = it
                            isError = false
                            errorMessage = null
                        },
                        label = { Text("Código QR Token *") },
                        placeholder = { Text("Escanear o ingresar token") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                        },
                        isError = isError,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_qr_input")
                    )

                    if (isError) {
                        Text(
                            text = errorMessage ?: "PIN o código no válido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (qrInput.isBlank()) {
                                isError = true
                                errorMessage = "PIN o código no válido"
                            } else {
                                onConfirmQr(qrInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wizard_submit_qr_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vincular mediante Código QR", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onBackToStep2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Atrás")
            }
        }
    }
}
