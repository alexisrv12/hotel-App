package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.TimeRateEntity
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestCheckInScreen(
    viewModel: HotelViewModel,
    preselectedRoomNumber: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rooms by viewModel.rooms.collectAsState()
    val availableRooms = rooms.filter { it.status == RoomStatus.DISPONIBLE }

    var fullName by remember { mutableStateOf("") }
    var documentId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var selectedRoomNumber by remember {
        mutableStateOf(
            preselectedRoomNumber ?: availableRooms.firstOrNull()?.roomNumber ?: "101"
        )
    }
    var roomDropdownExpanded by remember { mutableStateOf(false) }

    var stayNights by remember { mutableIntStateOf(1) }
    var adultsCount by remember { mutableIntStateOf(1) }
    var paymentMethod by remember { mutableStateOf("Efectivo") }

    // Validation state
    var nameError by remember { mutableStateOf<String?>(null) }
    var documentError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var roomError by remember { mutableStateOf<String?>(null) }

    val selectedRoom = rooms.find { it.roomNumber == selectedRoomNumber }
    val roomRate = selectedRoom?.nightlyRate ?: 120.0
    val totalStayPrice = roomRate * stayNights

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Registro de Check-In de Huésped", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Ingreso de datos obligatorios y asignación de habitación", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Guest Details Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Información del Huésped",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            nameError = null
                        },
                        label = { Text("Nombre Completo *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = documentId,
                        onValueChange = {
                            documentId = it
                            documentError = null
                        },
                        label = { Text("DPI / Pasaporte *") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        isError = documentError != null,
                        supportingText = documentError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                        },
                        label = { Text("Teléfono de Contacto *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico (Opcional)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Room Selection & Stay Duration Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. Selección de Habitación y Estadía",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    // Room Dropdown
                    ExposedDropdownMenuBox(
                        expanded = roomDropdownExpanded,
                        onExpandedChange = { roomDropdownExpanded = !roomDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Habitación $selectedRoomNumber - ${selectedRoom?.roomType ?: "Estándar"} (Q$roomRate/noche)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Habitación Asignada *") },
                            leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomDropdownExpanded) },
                            isError = roomError != null,
                            supportingText = roomError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = roomDropdownExpanded,
                            onDismissRequest = { roomDropdownExpanded = false }
                        ) {
                            if (availableRooms.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay habitaciones disponibles") },
                                    onClick = { roomDropdownExpanded = false }
                                )
                            } else {
                                availableRooms.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text("Hab. ${room.roomNumber} - ${room.roomType} (Q${room.nightlyRate}/noche)") },
                                        onClick = {
                                            selectedRoomNumber = room.roomNumber
                                            roomError = null
                                            roomDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Stay Nights Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Número de Noches:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { if (stayNights > 1) stayNights-- },
                                enabled = stayNights > 1,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                            ) { Text("-") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("$stayNights", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { stayNights++ },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                            ) { Text("+") }
                        }
                    }

                    // Payment Method Options
                    Text("Método de Pago:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Efectivo", "Tarjeta", "Transferencia").forEach { method ->
                            val isSelected = paymentMethod == method
                            Button(
                                onClick = { paymentMethod = method },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) HotelNavy else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(method, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HotelGold.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total a Cobrar:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Habitación $selectedRoomNumber ($stayNights n.)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "Q${String.format("%.2f", totalStayPrice)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HotelGold
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    // Form Validation
                    var hasError = false
                    if (fullName.isBlank()) {
                        nameError = "El nombre del huésped es obligatorio."
                        hasError = true
                    }
                    if (documentId.isBlank()) {
                        documentError = "El documento DPI/Pasaporte es obligatorio."
                        hasError = true
                    }
                    if (phone.isBlank()) {
                        phoneError = "El teléfono de contacto es obligatorio."
                        hasError = true
                    }
                    if (selectedRoom == null) {
                        roomError = "Seleccione una habitación disponible."
                        hasError = true
                    }

                    if (!hasError && selectedRoom != null) {
                        val customRate = TimeRateEntity(
                            name = "$stayNights Noche(s)",
                            durationMinutes = stayNights * 24 * 60L,
                            price = totalStayPrice
                        )

                        val extraNotes = buildString {
                            if (phone.isNotBlank()) append("Tel: $phone ")
                            if (email.isNotBlank()) append("Email: $email ")
                            append("Pago: $paymentMethod")
                        }

                        viewModel.checkInRoom(
                            roomId = selectedRoom.id,
                            clientName = fullName,
                            clientDpi = documentId,
                            guestCount = adultsCount,
                            rate = customRate,
                            notes = extraNotes
                        )

                        Toast.makeText(context, "Check-In exitoso en Habitación $selectedRoomNumber", Toast.LENGTH_LONG).show()
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Completar Check-In de Huésped", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
