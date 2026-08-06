package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.RoomEntity
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diálogo interactivo para cálculo de estancia y facturación.
 * Computa costos de hospedaje basados en tarifa por noche, número de noches/días,
 * servicios adicionales, impuestos (IVA 12%) y descuentos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceCalculatorDialog(
    rooms: List<RoomEntity>,
    onDismiss: () -> Unit,
    onSaveInvoice: ((InvoiceEntity) -> Unit)? = null
) {
    val context = LocalContext.current

    var selectedRoom by remember { mutableStateOf(rooms.firstOrNull()) }
    var roomDropdownExpanded by remember { mutableStateOf(false) }

    var clientName by remember { mutableStateOf(selectedRoom?.clientName ?: "") }
    var clientDpi by remember { mutableStateOf(selectedRoom?.clientDpi ?: "") }
    var numberOfNightsText by remember { mutableStateOf("1") }
    var nightlyRateText by remember { mutableStateOf(selectedRoom?.nightlyRate?.toString() ?: "150.0") }
    var extraServicesText by remember { mutableStateOf("0.0") }
    var discountText by remember { mutableStateOf("0.0") }
    var taxPercentageText by remember { mutableStateOf("12.0") } // IVA 12% standard

    // Numeric parsing and calculation
    val nights = numberOfNightsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val nightlyRate = nightlyRateText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 150.0
    val extraServices = extraServicesText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val discount = discountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val taxRate = taxPercentageText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 12.0

    val roomSubtotal = nightlyRate * nights
    val totalSubtotal = roomSubtotal + extraServices
    val taxAmount = (totalSubtotal - discount).coerceAtLeast(0.0) * (taxRate / 100.0)
    val grandTotal = (totalSubtotal - discount + taxAmount).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
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
                            shape = RoundedCornerShape(10.dp),
                            color = HotelNavy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = HotelGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Módulo de Facturación",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                            Text(
                                text = "Cálculo de Hospedaje & Resumen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Selector de Habitación
                Text(
                    text = "Habitación & Datos del Huésped:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = roomDropdownExpanded,
                    onExpandedChange = { roomDropdownExpanded = !roomDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedRoom != null) "Habitación ${selectedRoom?.roomNumber} (${selectedRoom?.roomType})" else "Habitación personalizada / General",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar Habitación") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("invoice_calc_room_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = roomDropdownExpanded,
                        onDismissRequest = { roomDropdownExpanded = false }
                    ) {
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text("Hab. ${room.roomNumber} - ${room.roomType} (Q${room.nightlyRate}/noche)") },
                                onClick = {
                                    selectedRoom = room
                                    nightlyRateText = room.nightlyRate.toString()
                                    if (!room.clientName.isNullOrBlank()) {
                                        clientName = room.clientName
                                    }
                                    if (!room.clientDpi.isNullOrBlank()) {
                                        clientDpi = room.clientDpi
                                    }
                                    roomDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre Cliente") },
                        placeholder = { Text("ej. Juan Pérez") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_calc_client_name")
                    )

                    OutlinedTextField(
                        value = clientDpi,
                        onValueChange = { clientDpi = it },
                        label = { Text("NIT / DPI") },
                        placeholder = { Text("CF o NIT") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_calc_client_dpi")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Parámetros de Cálculo
                Text(
                    text = "Tarifa y Duración de la Estancia:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nightlyRateText,
                        onValueChange = { nightlyRateText = it },
                        label = { Text("Tarifa x Noche (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_calc_rate_input")
                    )

                    OutlinedTextField(
                        value = numberOfNightsText,
                        onValueChange = { numberOfNightsText = it },
                        label = { Text("Noches / Días") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_calc_nights_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = extraServicesText,
                        onValueChange = { extraServicesText = it },
                        label = { Text("Servicios Extras (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_calc_extras_input")
                    )

                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Descuento (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_calc_discount_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta de Resumen de Facturación
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HotelNavy.copy(alpha = 0.05f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HotelNavy.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = HotelNavy,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "RESUMEN DE FACTURACIÓN Y COSTOS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }

                        Divider(color = HotelNavy.copy(alpha = 0.15f))

                        // Items Breakdown
                        SummaryRow(
                            label = "Hospedaje (${nights} noche${if (nights > 1) "s" else ""} x Q${"%.2f".format(nightlyRate)}):",
                            value = "Q${"%.2f".format(roomSubtotal)}"
                        )

                        if (extraServices > 0.0) {
                            SummaryRow(
                                label = "Consumos / Servicios Extras:",
                                value = "Q${"%.2f".format(extraServices)}"
                            )
                        }

                        SummaryRow(
                            label = "Subtotal:",
                            value = "Q${"%.2f".format(totalSubtotal)}",
                            fontWeight = FontWeight.SemiBold
                        )

                        if (discount > 0.0) {
                            SummaryRow(
                                label = "Descuento Aplicado:",
                                value = "- Q${"%.2f".format(discount)}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        SummaryRow(
                            label = "Impuesto (IVA 12%):",
                            value = "+ Q${"%.2f".format(taxAmount)}"
                        )

                        Divider(color = HotelNavy.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL A PAGAR:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                            Text(
                                text = "Q${"%.2f".format(grandTotal)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val nowMs = System.currentTimeMillis()
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(nowMs))
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(nowMs))
                            val invNumber = "FAC-${(100000..999999).random()}"

                            val invoice = InvoiceEntity(
                                invoiceNumber = invNumber,
                                hotelName = "Hotel Rivera",
                                hotelAddress = "Calle Principal 4-12, Zona 1",
                                hotelPhone = "+502 7765-4321",
                                hotelNit = clientDpi.ifBlank { "CF" },
                                dateString = dateStr,
                                timeString = timeStr,
                                roomNumber = selectedRoom?.roomNumber ?: "101",
                                clientName = clientName.ifBlank { "Consumidor Final" },
                                contractedTime = "$nights Noche(s)",
                                checkInTime = timeStr,
                                checkOutTime = "Final de Estancia",
                                price = totalSubtotal,
                                discount = discount,
                                totalAmount = grandTotal,
                                paymentMethod = "EFECTIVO",
                                receptionistName = "Recepción",
                                isVoided = false,
                                timestampMillis = nowMs
                            )

                            if (onSaveInvoice != null) {
                                onSaveInvoice(invoice)
                                Toast.makeText(context, "Factura $invNumber registrada con éxito", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("invoice_calc_save_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Factura", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = color
        )
    }
}
