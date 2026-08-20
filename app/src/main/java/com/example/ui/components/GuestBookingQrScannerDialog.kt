package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.TimeRateEntity
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import org.json.JSONObject
import java.util.Locale

/**
 * Parsed structure of a scanned guest booking confirmation code.
 */
data class GuestBookingDetails(
    val bookingCode: String,
    val guestName: String,
    val dpi: String?,
    val roomNumber: String,
    val guestCount: Int,
    val rateName: String,
    val durationHours: Int,
    val price: Double,
    val advancePaid: Boolean,
    val notes: String?
)

/**
 * QR Code Scanner Dialog specifically designed for receptionists to scan guest booking
 * confirmation codes and execute instant check-in with automatic room assignment and verification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestBookingQrScannerDialog(
    rooms: List<RoomEntity>,
    timeRates: List<TimeRateEntity>,
    viewModel: HotelViewModel,
    onDismiss: () -> Unit,
    onInstantCheckInSuccess: (roomNumber: String, guestName: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Cámara Escáner, 1: Simulador / Códigos Demo
    var scannedResult by remember { mutableStateOf<GuestBookingDetails?>(null) }
    var selectedAssignedRoomId by remember { mutableStateOf<Long?>(null) }
    var selectedTimeRateId by remember { mutableStateOf<Long?>(null) }
    var checkInDoneMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(650.dp)
                .testTag("guest_booking_qr_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HotelNavy.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = HotelNavy, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Escáner de Check-In QR", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HotelNavy)
                            Text("Confirmación Instantánea de Reserva", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs (Scanner vs Simulator/Demos)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = HotelNavy.copy(alpha = 0.06f),
                    contentColor = HotelNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Cámara en Vivo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("tab_qr_camera_live")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Demos / Reservas Web", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("tab_qr_demo_codes")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BODY CONTENT BASED ON SCANNED STATE
                if (scannedResult == null) {
                    if (selectedTab == 0) {
                        // LIVE CAMERA SCANNER VIEW
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Apunte al código QR del comprobante de reserva del huésped.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                QRScannerView(
                                    onQrCodeScanned = { rawCode ->
                                        val parsed = parseGuestBookingCode(rawCode, rooms, timeRates)
                                        if (parsed != null) {
                                            scannedResult = parsed
                                            // Auto-match room
                                            val matchedRoom = rooms.find { it.roomNumber == parsed.roomNumber }
                                            val assignedRoom = if (matchedRoom != null && matchedRoom.status == RoomStatus.DISPONIBLE) {
                                                matchedRoom
                                            } else {
                                                rooms.find { it.status == RoomStatus.DISPONIBLE } ?: matchedRoom
                                            }
                                            selectedAssignedRoomId = assignedRoom?.id

                                            // Auto-match rate
                                            val matchedRate = timeRates.find { it.name.contains(parsed.rateName, ignoreCase = true) }
                                                ?: timeRates.firstOrNull()
                                            selectedTimeRateId = matchedRate?.id

                                            Toast.makeText(context, "¡Reserva escaneada: ${parsed.bookingCode}!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Código no reconocido como reserva de hotel: $rawCode", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onCloseScanner = onDismiss
                                )
                            }
                        }
                    } else {
                        // DEMO RESERVATIONS SIMULATOR (for instant testing)
                        val demoBookings = remember { getDemoBookingPresets() }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Haga clic en cualquiera de las siguientes reservas confirmadas para simular el escaneo instantáneo del huésped:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(demoBookings) { demo ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scannedResult = demo
                                                val matchedRoom = rooms.find { it.roomNumber == demo.roomNumber }
                                                val assignedRoom = if (matchedRoom != null && matchedRoom.status == RoomStatus.DISPONIBLE) {
                                                    matchedRoom
                                                } else {
                                                    rooms.find { it.status == RoomStatus.DISPONIBLE } ?: matchedRoom
                                                }
                                                selectedAssignedRoomId = assignedRoom?.id

                                                val matchedRate = timeRates.find { it.name.contains(demo.rateName, ignoreCase = true) }
                                                    ?: timeRates.firstOrNull()
                                                selectedTimeRateId = matchedRate?.id
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = HotelNavy,
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    ) {
                                                        Text(demo.bookingCode, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                    Text(demo.guestName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                Text("Hab. Sugerida: ${demo.roomNumber} • ${demo.guestCount} personas • ${demo.rateName}", fontSize = 11.sp)
                                                Text("Monto: Q${demo.price} ${if (demo.advancePaid) "(Pre-pagado)" else "(Pago en Recepción)"}", fontSize = 11.sp, color = HotelGold, fontWeight = FontWeight.SemiBold)
                                            }
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Simular", tint = HotelNavy)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // CONFIRMATION CARD FOR SCANNED GUEST BOOKING
                    val details = scannedResult!!
                    val targetRoom = rooms.find { it.id == selectedAssignedRoomId }
                    val targetRate = timeRates.find { it.id == selectedTimeRateId } ?: timeRates.firstOrNull()
                    val isTargetRoomAvailable = targetRoom?.status == RoomStatus.DISPONIBLE

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Success Header Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Código de Reserva Verificado", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusGreen)
                                    Text("Código: ${details.bookingCode}", fontSize = 11.sp, color = StatusGreen)
                                }
                            }
                        }

                        // Guest Information Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Huésped:", fontSize = 12.sp, color = Color.Gray)
                                    Text(details.guestName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                details.dpi?.let { dpi ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("DPI / Identificación:", fontSize = 12.sp, color = Color.Gray)
                                        Text(dpi, fontSize = 12.sp)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cantidad de Personas:", fontSize = 12.sp, color = Color.Gray)
                                    Text("${details.guestCount} personas", fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tarifa Reservada:", fontSize = 12.sp, color = Color.Gray)
                                    Text("${details.rateName} (Q${details.price})", fontWeight = FontWeight.Bold, color = HotelGold, fontSize = 12.sp)
                                }
                                details.notes?.let { notes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Notas de Reserva:", fontSize = 12.sp, color = Color.Gray)
                                        Text(notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Room Assignment Selection
                        Text("Asignación de Habitación:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (!isTargetRoomAvailable && targetRoom != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusYellow.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Habitación ${targetRoom.roomNumber} está ${targetRoom.status}. Por favor seleccione otra habitación disponible:",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        // Available rooms row
                        val availableRooms = rooms.filter { it.status == RoomStatus.DISPONIBLE }
                        if (availableRooms.isEmpty()) {
                            Text(
                                "⚠️ No hay habitaciones libres en este momento. Finalice o limpie una habitación antes de completar el check-in.",
                                color = StatusRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableRooms.take(5).forEach { r ->
                                    val isSelected = r.id == selectedAssignedRoomId
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) HotelNavy else HotelNavy.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .clickable { selectedAssignedRoomId = r.id }
                                            .weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Hab. ${r.roomNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) Color.White else HotelNavy
                                            )
                                            Text(
                                                text = "Libre",
                                                fontSize = 9.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else StatusGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Action Buttons: Confirm Instant Check-In vs Scan Another
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scannedResult = null
                                    selectedTab = 0
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Volver a Escanear", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val finalRoom = rooms.find { it.id == selectedAssignedRoomId }
                                    val finalRate = targetRate ?: timeRates.first()
                                    if (finalRoom != null && finalRoom.status == RoomStatus.DISPONIBLE) {
                                        viewModel.checkInRoom(
                                            roomId = finalRoom.id,
                                            clientName = details.guestName,
                                            clientDpi = details.dpi,
                                            guestCount = details.guestCount,
                                            rate = finalRate,
                                            notes = "Reserva QR #${details.bookingCode} • ${details.notes ?: ""}"
                                        )
                                        onInstantCheckInSuccess(finalRoom.roomNumber, details.guestName)
                                        Toast.makeText(
                                            context,
                                            "✅ Check-In Instantáneo Exitoso: Habitación ${finalRoom.roomNumber} asignada a ${details.guestName}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Por favor seleccione una habitación disponible.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = availableRooms.isNotEmpty() && selectedAssignedRoomId != null,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("confirm_instant_qr_checkin_button")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check-In Instantáneo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses scanned QR code string supporting JSON, Key-Value pipe format, or plain booking ID.
 */
private fun parseGuestBookingCode(
    rawCode: String,
    rooms: List<RoomEntity>,
    timeRates: List<TimeRateEntity>
): GuestBookingDetails? {
    val trimmed = rawCode.trim()

    // 1. Try parsing JSON format
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        try {
            val json = JSONObject(trimmed)
            val bookingCode = json.optString("bookingCode", json.optString("code", "RIV-${(1000..9999).random()}"))
            val guestName = json.optString("guestName", json.optString("name", "Huésped"))
            val dpi = json.optString("dpi").ifBlank { null }
            val roomNumber = json.optString("roomNumber", json.optString("room", "101"))
            val guestCount = json.optInt("guestCount", json.optInt("guests", 1))
            val rateName = json.optString("rateName", json.optString("rate", "Noche Completa"))
            val durationHours = json.optInt("durationHours", json.optInt("hours", 12))
            val price = json.optDouble("price", json.optDouble("totalPrice", 180.0))
            val advancePaid = json.optBoolean("advancePaid", json.optBoolean("paid", false))
            val notes = json.optString("notes").ifBlank { null }

            return GuestBookingDetails(
                bookingCode = bookingCode,
                guestName = guestName,
                dpi = dpi,
                roomNumber = roomNumber,
                guestCount = guestCount,
                rateName = rateName,
                durationHours = durationHours,
                price = price,
                advancePaid = advancePaid,
                notes = notes
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. Try parsing Key-Value pipe format: BOOKING:RIV-894|GUEST:Carlos|ROOM:102
    if (trimmed.contains("|") || trimmed.contains(":")) {
        val pairs = trimmed.split("|").mapNotNull { part ->
            val colonIdx = part.indexOf(':')
            if (colonIdx > 0) {
                part.substring(0, colonIdx).trim().uppercase(Locale.getDefault()) to part.substring(colonIdx + 1).trim()
            } else null
        }.toMap()

        if (pairs.isNotEmpty()) {
            val bookingCode = pairs["BOOKING"] ?: pairs["CODE"] ?: "RIV-${(1000..9999).random()}"
            val guestName = pairs["GUEST"] ?: pairs["NAME"] ?: "Huésped Reserva"
            val dpi = pairs["DPI"]
            val roomNumber = pairs["ROOM"] ?: pairs["HAB"] ?: "101"
            val guestCount = pairs["GUESTS"]?.toIntOrNull() ?: 1
            val rateName = pairs["RATE"] ?: pairs["TARIFA"] ?: "Noche Completa"
            val durationHours = pairs["HOURS"]?.toIntOrNull() ?: 12
            val price = pairs["PRICE"]?.toDoubleOrNull() ?: 180.0
            val advancePaid = pairs["PAID"]?.toBooleanStrictOrNull() ?: false
            val notes = pairs["NOTES"]

            return GuestBookingDetails(
                bookingCode = bookingCode,
                guestName = guestName,
                dpi = dpi,
                roomNumber = roomNumber,
                guestCount = guestCount,
                rateName = rateName,
                durationHours = durationHours,
                price = price,
                advancePaid = advancePaid,
                notes = notes
            )
        }
    }

    // 3. Plain Room or Booking Code match
    val digitsMatch = Regex("""\b\d{1,4}\b""").find(trimmed)?.value
    val matchedRoom = rooms.find { it.roomNumber == trimmed || (digitsMatch != null && it.roomNumber == digitsMatch) }
    if (matchedRoom != null || trimmed.startsWith("RIV-", ignoreCase = true) || trimmed.startsWith("HAB-", ignoreCase = true)) {
        val rNum = matchedRoom?.roomNumber ?: (digitsMatch ?: "101")
        return GuestBookingDetails(
            bookingCode = if (trimmed.startsWith("RIV-", ignoreCase = true)) trimmed else "RIV-${(1000..9999).random()}",
            guestName = "Huésped Confirmado",
            dpi = null,
            roomNumber = rNum,
            guestCount = 2,
            rateName = "Noche Completa",
            durationHours = 12,
            price = 180.0,
            advancePaid = false,
            notes = "Reserva escaneada vía código directo"
        )
    }

    return null
}

/**
 * Returns demo booking presets for testing reception QR workflow.
 */
private fun getDemoBookingPresets(): List<GuestBookingDetails> {
    return listOf(
        GuestBookingDetails(
            bookingCode = "RIV-2026-894",
            guestName = "Carlos Mendoza Gómez",
            dpi = "2984 10293 0101",
            roomNumber = "102",
            guestCount = 2,
            rateName = "Noche Completa",
            durationHours = 12,
            price = 250.0,
            advancePaid = true,
            notes = "Reserva vía Booking.com • Llegada estimada 14:00"
        ),
        GuestBookingDetails(
            bookingCode = "RIV-2026-905",
            guestName = "Lic. Andrea Vásquez",
            dpi = "1829 49201 0101",
            roomNumber = "105",
            guestCount = 1,
            rateName = "Hospedaje 6 Horas",
            durationHours = 6,
            price = 120.0,
            advancePaid = false,
            notes = "Viaje de Negocios • Requiere Factura con NIT"
        ),
        GuestBookingDetails(
            bookingCode = "RIV-2026-918",
            guestName = "Familia Morales Cabrera",
            dpi = "3019 88472 0101",
            roomNumber = "201",
            guestCount = 3,
            rateName = "Día Completo (24h)",
            durationHours = 24,
            price = 350.0,
            advancePaid = true,
            notes = "Cama matrimonial + adicional solicitada"
        )
    )
}
