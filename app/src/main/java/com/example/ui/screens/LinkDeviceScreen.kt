package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.QRScannerView
import com.example.ui.theme.HotelNavy
import com.example.ui.viewmodel.DeviceLinkingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla para autorizar/vincular un nuevo dispositivo con solo dos opciones:
 * 1. Escanear Código QR
 * 2. Ingresar Código PIN
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkDeviceScreen(
    onBackToLogin: () -> Unit,
    deviceLinkingViewModel: DeviceLinkingViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentPin by deviceLinkingViewModel.currentPin.collectAsState()
    val currentQrToken by deviceLinkingViewModel.currentQrSessionToken.collectAsState()
    val pinCountdownText by deviceLinkingViewModel.pinCountdownText.collectAsState()
    val qrCountdownText by deviceLinkingViewModel.qrCountdownText.collectAsState()

    var activeFlow by remember { mutableStateOf<String?>(null) } // null = Options Menu, "QR" = Scan QR flow, "PIN" = Enter PIN flow
    var pinInput by remember { mutableStateOf("") }
    var qrInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var validationStep by remember { mutableStateOf("Iniciando validación...") }
    var showCameraScanner by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vincular Dispositivo",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeFlow != null) {
                                activeFlow = null
                                errorMessage = null
                            } else {
                                onBackToLogin()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HotelNavy)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Option selection menu
            if (activeFlow == null) {
                Text(
                    text = "Autorizar Nuevo Dispositivo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )

                Text(
                    text = "Selecciona el método de autorización generado desde el Módulo Gerente:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Option 1: Scan QR Code
                Card(
                    onClick = {
                        activeFlow = "QR"
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_qr_option_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HotelNavy.copy(alpha = 0.1f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = HotelNavy,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Escanear Código QR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                            Text(
                                text = "Usa la cámara para escanear el QR generado por el Gerente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 2: Enter PIN Code
                Card(
                    onClick = {
                        activeFlow = "PIN"
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enter_pin_option_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HotelNavy.copy(alpha = 0.1f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = HotelNavy,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ingresar Código PIN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                            Text(
                                text = "Escribe el PIN temporal de 6 dígitos provisto por la administración",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (activeFlow == "QR") {
                // QR Flow Sub-screen
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Escanear Código QR",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HotelNavy
                        )

                        Text(
                            text = "Escanea el código QR desde la pantalla del panel Gerente o ingresa el token manualmente:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Divider()

                        if (showCameraScanner) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                QRScannerView(
                                    modifier = Modifier.fillMaxSize(),
                                    onQrCodeScanned = { scannedCode ->
                                        showCameraScanner = false
                                        qrInput = scannedCode
                                        errorMessage = null
                                        scope.launch {
                                            isLoading = true
                                            delay(300)
                                            val success = deviceLinkingViewModel.completeLinkingWithQr(context, scannedCode)
                                            isLoading = false
                                            if (success) {
                                                Toast.makeText(context, "Dispositivo autorizado con éxito.", Toast.LENGTH_LONG).show()
                                                onBackToLogin()
                                            } else {
                                                errorMessage = "El código QR es inválido o ha expirado."
                                            }
                                        }
                                    },
                                    onCloseScanner = { showCameraScanner = false }
                                )
                            }
                        } else {
                            Button(
                                onClick = { showCameraScanner = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escanear con Cámara", fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = HotelNavy.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = HotelNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Vigencia del Token QR: $qrCountdownText",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelNavy
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { qrInput = currentQrToken },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Usar Token QR Activo ($currentQrToken)")
                        }

                        OutlinedTextField(
                            value = qrInput,
                            onValueChange = {
                                qrInput = it
                                errorMessage = null
                            },
                            label = { Text("Token de Código QR *") },
                            leadingIcon = { Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = HotelNavy.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = HotelNavy
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = HotelNavy
                                        )
                                        Text(
                                            text = validationStep,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = HotelNavy
                                        )
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (qrInput.isBlank()) {
                                        errorMessage = "Ingrese un código QR válido."
                                    } else {
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            validationStep = "Conectando al servidor del hotel..."
                                            delay(300)
                                            validationStep = "Verificando vigencia de 5 minutos..."
                                            delay(300)
                                            validationStep = "Registrando autorización de dispositivo..."
                                            delay(200)
                                            val success = deviceLinkingViewModel.completeLinkingWithQr(context, qrInput)
                                            isLoading = false
                                            if (success) {
                                                Toast.makeText(context, "Dispositivo autorizado con éxito.", Toast.LENGTH_LONG).show()
                                                onBackToLogin()
                                            } else {
                                                errorMessage = deviceLinkingViewModel.userMessage.value ?: "El código QR es inválido o ha expirado."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Autorizar Dispositivo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (activeFlow == "PIN") {
                // PIN Flow Sub-screen
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Ingresar Código PIN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HotelNavy
                        )

                        Text(
                            text = "Ingresa el PIN de 6 dígitos generado en el panel Gerente:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Divider()

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = HotelNavy.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = HotelNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Vigencia del PIN: $pinCountdownText",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelNavy
                                )
                            }
                        }

                        TextButton(onClick = { pinInput = currentPin }) {
                            Text("Usar PIN activo ($currentPin)", fontSize = 12.sp)
                        }

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    pinInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Código PIN (6 dígitos) *") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = HotelNavy.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = HotelNavy
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = HotelNavy
                                        )
                                        Text(
                                            text = validationStep,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = HotelNavy
                                        )
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (pinInput.isBlank()) {
                                        errorMessage = "Ingrese un código PIN."
                                    } else {
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            validationStep = "Conectando al servidor del hotel..."
                                            delay(300)
                                            validationStep = "Verificando vigencia de 5 minutos..."
                                            delay(300)
                                            validationStep = "Validando firma de PIN..."
                                            delay(200)
                                            val success = deviceLinkingViewModel.completeLinkingWithPin(context, pinInput)
                                            isLoading = false
                                            if (success) {
                                                Toast.makeText(context, "Dispositivo autorizado con éxito.", Toast.LENGTH_LONG).show()
                                                onBackToLogin()
                                            } else {
                                                errorMessage = deviceLinkingViewModel.userMessage.value ?: "El código PIN es incorrecto o ha expirado."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Autorizar Dispositivo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
