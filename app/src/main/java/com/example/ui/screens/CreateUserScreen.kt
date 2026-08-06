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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.UserEntity
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelNavy
import com.example.utils.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla para registrar/crear un nuevo usuario en la base de datos de autenticación del sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    hotelViewModel: HotelViewModel,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existingUsers by hotelViewModel.users.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var emailOrUsername by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("RECEPCION") } // "RECEPCION" or "GERENTE"
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crear Usuario",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al Login",
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
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HotelNavy.copy(alpha = 0.1f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = HotelNavy,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "Registro de Nuevo Usuario",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    Text(
                        text = "Ingresa los datos requeridos para registrar un usuario en la base de datos de autenticación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Divider()

                    // Nombre Completo
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            errorMessage = null
                        },
                        label = { Text("Nombre Completo *") },
                        placeholder = { Text("ej. María Rodríguez") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_user_fullname_input")
                    )

                    // Correo / Usuario
                    OutlinedTextField(
                        value = emailOrUsername,
                        onValueChange = {
                            emailOrUsername = it
                            errorMessage = null
                        },
                        label = { Text("Correo Electrónico / Usuario *") },
                        placeholder = { Text("ej. usuario@hotel.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_user_email_input")
                    )

                    // Selección de Rol
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Rol de Acceso en el Sistema:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedRole == "RECEPCION",
                                onClick = { selectedRole = "RECEPCION" },
                                label = { Text("Recepción") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HotelNavy,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedRole == "GERENTE",
                                onClick = { selectedRole = "GERENTE" },
                                label = { Text("Gerente") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HotelNavy,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Contraseña *") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_user_password_input")
                    )

                    // Confirmar Contraseña
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Confirmar Contraseña *") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_user_confirm_password_input")
                    )

                    // Display error message
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        Button(
                            onClick = {
                                val trimmedName = fullName.trim()
                                val trimmedEmail = emailOrUsername.trim().lowercase()
                                val trimmedPwd = password.trim()
                                val trimmedConfirm = confirmPassword.trim()

                                when {
                                    trimmedName.isBlank() -> {
                                        errorMessage = "Ingrese el nombre completo del usuario."
                                    }
                                    trimmedEmail.isBlank() -> {
                                        errorMessage = "Ingrese el correo electrónico o usuario."
                                    }
                                    trimmedEmail.length < 3 -> {
                                        errorMessage = "El correo/usuario debe tener al menos 3 caracteres."
                                    }
                                    trimmedPwd.isBlank() -> {
                                        errorMessage = "Ingrese una contraseña."
                                    }
                                    trimmedPwd.length < 4 -> {
                                        errorMessage = "La contraseña debe tener al menos 4 caracteres."
                                    }
                                    trimmedPwd != trimmedConfirm -> {
                                        errorMessage = "Las contraseñas no coinciden. Verifique la confirmación."
                                    }
                                    existingUsers.any { it.username.lowercase() == trimmedEmail } -> {
                                        errorMessage = "El correo o usuario '$trimmedEmail' ya se encuentra registrado."
                                    }
                                    else -> {
                                        scope.launch {
                                            isLoading = true
                                            delay(500)

                                            val passwordHash = SecurityUtils.hashPassword(trimmedPwd)
                                            val newUser = UserEntity(
                                                username = trimmedEmail,
                                                fullName = trimmedName,
                                                pinCode = if (trimmedPwd.all { it.isDigit() }) trimmedPwd else "",
                                                passwordHash = passwordHash,
                                                role = selectedRole,
                                                isActive = true,
                                                createdAt = System.currentTimeMillis()
                                            )

                                            hotelViewModel.saveUser(newUser)
                                            isLoading = false

                                            Toast.makeText(
                                                context,
                                                "Usuario creado exitosamente. Ya puedes iniciar sesión.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            onBackToLogin()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_user_submit_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Registrar Usuario",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    TextButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.testTag("create_user_cancel_button")
                    ) {
                        Text(
                            text = "¿Ya tienes cuenta? Volver al inicio de sesión",
                            color = HotelNavy,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
