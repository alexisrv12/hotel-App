package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.HotelViewModel
import com.example.ui.components.QRScannerView
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.viewmodel.DeviceLinkingViewModel
import com.example.utils.QrCodeImageDecoder
import androidx.compose.material.icons.filled.Image
import com.example.ui.viewmodel.LoginViewModel

/**
 * Pantalla de Inicio de Sesión (Login Screen)
 * Pantalla principal al abrir la aplicación.
 */
@Composable
fun LoginScreen(
    hotelViewModel: HotelViewModel,
    loginViewModel: LoginViewModel = viewModel(),
    deviceLinkingViewModel: DeviceLinkingViewModel = viewModel(),
    onNavigateToLinkDevice: (() -> Unit)? = null,
    onNavigateToCreateUser: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val emailInput by loginViewModel.email.collectAsState()
    val passwordInput by loginViewModel.password.collectAsState()
    val emailError by loginViewModel.emailError.collectAsState()
    val passwordError by loginViewModel.passwordError.collectAsState()
    val loginError by loginViewModel.loginError.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var showDeviceLinkingDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Hotel Branding Header
            Surface(
                shape = CircleShape,
                color = HotelNavy,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Hotel,
                        contentDescription = "Hotel Logo",
                        tint = HotelGold,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HOTEL RIVERA",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = HotelNavy,
                letterSpacing = 2.sp
            )

            Text(
                text = "Sistema de Gestión y Recepción",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Login Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    Text(
                        text = "Ingresa tus credenciales para acceder a tu panel correspondiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider()

                    // Campo de Correo Electrónico con estado de error explícito
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { loginViewModel.onEmailChange(it) },
                        label = { Text("Correo Electrónico *") },
                        placeholder = { Text("ej. usuario@hotel.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon", tint = HotelNavy)
                        },
                        isError = emailError != null,
                        supportingText = {
                            if (emailError != null) {
                                Text(text = emailError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HotelNavy,
                            focusedLabelColor = HotelNavy
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    // Campo de Contraseña con comprobación de complejidad y error explícito
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { loginViewModel.onPasswordChange(it) },
                        label = { Text("Contraseña *") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Password Icon", tint = HotelNavy)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) {
                                Text(text = passwordError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                loginViewModel.performLogin(hotelViewModel)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HotelNavy,
                            focusedLabelColor = HotelNavy
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    // Error global de login
                    AnimatedVisibility(visible = loginError != null) {
                        loginError?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Circular progress indicator during login
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = HotelNavy,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else {
                        // Botón 1: Iniciar Sesión (Primary Action)
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                loginViewModel.performLogin(hotelViewModel)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button")
                        ) {
                            Text(
                                text = "Iniciar Sesión",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Botón 2: Vincular Dispositivo (Authorization Only)
                    OutlinedButton(
                        onClick = {
                            if (onNavigateToLinkDevice != null) {
                                onNavigateToLinkDevice()
                            } else {
                                showDeviceLinkingDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_link_device_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhonelinkSetup,
                            contentDescription = null,
                            tint = HotelNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vincular Dispositivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HotelNavy
                        )
                    }

                    // Botón 3: Crear Usuario
                    if (onNavigateToCreateUser != null) {
                        TextButton(
                            onClick = onNavigateToCreateUser,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_create_user_button")
                        ) {
                            Text(
                                text = "¿No tienes cuenta? Crear usuario",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal de Vinculación de Dispositivo (Únicamente dos opciones: QR o PIN)
    if (showDeviceLinkingDialog) {
        DeviceLinkingOptionsDialog(
            deviceLinkingViewModel = deviceLinkingViewModel,
            onDismiss = { showDeviceLinkingDialog = false },
            onLinkingSuccess = {
                showDeviceLinkingDialog = false
                Toast.makeText(context, "Dispositivo autorizado exitosamente", Toast.LENGTH_LONG).show()
            }
        )
    }
}

/**
 * Diálogo de Vinculación de Dispositivo
 * Muestra ÚNICAMENTE dos opciones:
 * 1. Escanear código QR
 * 2. Ingresar código PIN
 */
@Composable
private fun DeviceLinkingOptionsDialog(
    deviceLinkingViewModel: DeviceLinkingViewModel,
    onDismiss: () -> Unit,
    onLinkingSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(0) } // 0 = QR, 1 = PIN
    var pinInput by remember { mutableStateOf("") }
    var qrInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCameraActive by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = QrCodeImageDecoder.decodeQrFromUri(context, uri)
            if (!decoded.isNullOrBlank()) {
                qrInput = decoded
                errorMessage = null
                val success = deviceLinkingViewModel.completeLinkingWithQr(context, decoded)
                if (success) {
                    Toast.makeText(context, "Dispositivo vinculado correctamente con imagen QR.", Toast.LENGTH_SHORT).show()
                    onLinkingSuccess()
                } else {
                    errorMessage = "El código QR en la imagen no es válido o ha expirado."
                }
            } else {
                errorMessage = "No se pudo detectar un código QR legible en la imagen seleccionada."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhonelinkSetup,
                        contentDescription = null,
                        tint = HotelNavy,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Vincular Dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Selecciona el método de autorización dinámico generado en el Módulo Gerente:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TabRow(
                    selectedTabIndex = selectedOption,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedOption == 0,
                        onClick = {
                            selectedOption = 0
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
                        modifier = Modifier.testTag("option_qr_tab")
                    )

                    Tab(
                        selected = selectedOption == 1,
                        onClick = {
                            selectedOption = 1
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
                        modifier = Modifier.testTag("option_pin_tab")
                    )
                }

                if (selectedOption == 0) {
                    // Opción 1: Escáner de Cámara Real y Fallback de Imagen
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isCameraActive) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            ) {
                                QRScannerView(
                                    modifier = Modifier.fillMaxSize(),
                                    onQrCodeScanned = { scannedText ->
                                        qrInput = scannedText
                                        errorMessage = null
                                        val success = deviceLinkingViewModel.completeLinkingWithQr(context, scannedText)
                                        if (success) {
                                            Toast.makeText(context, "Dispositivo vinculado inmediatamente.", Toast.LENGTH_SHORT).show()
                                            onLinkingSuccess()
                                        } else {
                                            errorMessage = "Código QR no válido o expirado."
                                        }
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
                                Text("Activar Cámara en Vivo")
                            }
                        }

                        // Botón de fallo manual "Subir imagen de QR"
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_qr_image_button")
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Subir imagen de QR")
                        }

                        OutlinedTextField(
                            value = qrInput,
                            onValueChange = {
                                qrInput = it
                                errorMessage = null
                            },
                            label = { Text("Código QR / Token") },
                            placeholder = { Text("Escanear o ingresar token") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_token_input")
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                if (qrInput.isBlank()) {
                                    errorMessage = "PIN o código no válido"
                                } else {
                                    val success = deviceLinkingViewModel.completeLinkingWithQr(context, qrInput)
                                    if (success) {
                                        onLinkingSuccess()
                                    } else {
                                        errorMessage = "PIN o código no válido"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_qr_linking_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vincular Dispositivo", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Opción 2: Ingresar Código PIN Dinámico
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Ingresa el PIN de 6 dígitos activo provisto por la administración:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    pinInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Código PIN de 6 dígitos *") },
                            placeholder = { Text("Ingresa el PIN") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_code_input")
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                if (pinInput.isBlank()) {
                                    errorMessage = "PIN o código no válido"
                                } else {
                                    val success = deviceLinkingViewModel.completeLinkingWithPin(context, pinInput)
                                    if (success) {
                                        onLinkingSuccess()
                                    } else {
                                        errorMessage = "PIN o código no válido"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_pin_linking_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vincular Dispositivo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
