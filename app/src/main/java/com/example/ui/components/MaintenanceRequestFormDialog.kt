package com.example.ui.components

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import java.io.File

/**
 * Dialog form for reporting broken items and submitting maintenance requests.
 * Features room selector, broken item suggestion chips, category selector,
 * priority levels, notes, and direct integration with CameraX photo capture module.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MaintenanceRequestFormDialog(
    viewModel: HotelViewModel,
    initialRoomNumber: String? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val rooms by viewModel.rooms.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    // Form states
    var selectedLocation by remember {
        mutableStateOf(initialRoomNumber?.let { "Habitación $it" } ?: if (rooms.isNotEmpty()) "Habitación ${rooms.first().roomNumber}" else "Habitación 1")
    }
    var isLocationDropdownExpanded by remember { mutableStateOf(false) }

    var itemName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Plomería") }
    var selectedPriority by remember { mutableStateOf("Media") }
    var description by remember { mutableStateOf("") }
    var reporterName by remember { mutableStateOf(activeUser) }
    var photoPath by remember { mutableStateOf<String?>(null) }

    // Camera and Photo preview dialogs
    var showCameraDialog by remember { mutableStateOf(false) }
    var showFullScreenPhoto by remember { mutableStateOf(false) }

    val categories = listOf(
        "Plomería",
        "Electricidad",
        "Climatización (A/C)",
        "Cerrajería",
        "Electrónica / TV",
        "Mobiliario & Camas",
        "Infraestructura",
        "Otro"
    )

    val quickItemSuggestions = listOf(
        "Aire Acondicionado (No enfría)",
        "Fuga en Grifo / Lavamanos",
        "Inodoro Obstruido",
        "Ducha Sin Agua Caliente",
        "Luz / Foco Quemado",
        "Cerradura Electrónica Trabada",
        "Televisor Sin Señal",
        "Enchufe Dañado",
        "Ventana / Vidrio Roto"
    )

    val locationOptions = buildList {
        rooms.forEach { add("Habitación ${it.roomNumber}") }
        add("Recepción Principal")
        add("Pasillo 1er Nivel")
        add("Pasillo 2do Nivel")
        add("Lavandería & Bodega")
        add("Área de Parqueo")
        add("Áreas Comunes")
    }

    if (showCameraDialog) {
        CameraPhotoCaptureDialog(
            onPhotoCaptured = { path ->
                photoPath = path
                showCameraDialog = false
                Toast.makeText(context, "Fotografía adjuntada con éxito.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCameraDialog = false }
        )
    }

    if (showFullScreenPhoto && photoPath != null) {
        Dialog(
            onDismissRequest = { showFullScreenPhoto = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val fullBitmap = remember(photoPath) {
                        try {
                            BitmapFactory.decodeFile(photoPath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (fullBitmap != null) {
                        Image(
                            bitmap = fullBitmap.asImageBitmap(),
                            contentDescription = "Foto ampliada de avería",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    IconButton(
                        onClick = { showFullScreenPhoto = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(HotelGold.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = null,
                            tint = HotelNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reportar Avería o Mantenimiento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = HotelNavy
                        )
                        Text(
                            text = "Notificación inmediata al personal técnico y gerencia",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Location / Room Selector
                Text(
                    text = "UBICACIÓN / HABITACIÓN *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = isLocationDropdownExpanded,
                    onExpandedChange = { isLocationDropdownExpanded = !isLocationDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedLocation,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLocationDropdownExpanded) },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = HotelNavy) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("maintenance_location_field"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isLocationDropdownExpanded,
                        onDismissRequest = { isLocationDropdownExpanded = false }
                    ) {
                        locationOptions.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc, fontWeight = if (loc == selectedLocation) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedLocation = loc
                                    isLocationDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Item Name / Damage title
                Text(
                    text = "OBJETO O AVERÍA DETECTADA *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    placeholder = { Text("Ej. Aire acondicionado gotea, cerradura rota...") },
                    leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusYellow) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("maintenance_item_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Quick suggestions chips
                Spacer(modifier = Modifier.height(6.dp))
                Text("Sugerencias rápidas:", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickItemSuggestions.take(6).forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (itemName == suggestion) HotelGold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (itemName == suggestion) androidx.compose.foundation.BorderStroke(1.dp, HotelGold) else null,
                            modifier = Modifier.clickable {
                                itemName = suggestion
                                when {
                                    suggestion.contains("Aire") -> selectedCategory = "Climatización (A/C)"
                                    suggestion.contains("Grifo") || suggestion.contains("Inodoro") || suggestion.contains("Ducha") -> selectedCategory = "Plomería"
                                    suggestion.contains("Luz") || suggestion.contains("Enchufe") -> selectedCategory = "Electricidad"
                                    suggestion.contains("Cerradura") -> selectedCategory = "Cerrajería"
                                    suggestion.contains("Televisor") -> selectedCategory = "Electrónica / TV"
                                }
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Selector
                Text(
                    text = "CATEGORÍA TÉCNICA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HotelNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority Level
                Text(
                    text = "NIVEL DE PRIORIDAD *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val priorities = listOf("Baja", "Media", "Alta", "Urgente")
                    priorities.forEach { p ->
                        val (pColor, pTextColor) = when (p) {
                            "Baja" -> Color(0xFF64748B) to Color.White
                            "Media" -> StatusYellow to HotelNavy
                            "Alta" -> Color(0xFFF97316) to Color.White
                            "Urgente" -> StatusRed to Color.White
                            else -> HotelNavy to Color.White
                        }
                        val isSelected = selectedPriority == p

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) pColor else pColor.copy(alpha = 0.15f)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, pColor) else null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPriority = p }
                                .testTag("priority_chip_$p")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (p == "Urgente") "🚨 $p" else p,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) pTextColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Text Field
                Text(
                    text = "DETALLES Y OBSERVACIONES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Indique detalles específicos sobre la falla o daño para el técnico...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .testTag("maintenance_description_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(14.dp))

                // INTEGRATED CAMERA PHOTO ATTACHMENT SECTION
                Text(
                    text = "EVIDENCIA FOTOGRÁFICA (CÁMARA INTEGRADA)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HotelGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        if (photoPath == null) {
                            // No photo attached yet: Camera button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showCameraDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_camera_capture_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = HotelGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tomar Foto con Cámara", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Opcional: Tome una foto del daño usando el módulo de cámara integrado de CameraX.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        } else {
                            // Photo attached: Display thumbnail and management actions
                            val thumbnailBitmap = remember(photoPath) {
                                try {
                                    photoPath?.let { BitmapFactory.decodeFile(it) }
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                        .border(2.dp, HotelGold, RoundedCornerShape(8.dp))
                                        .clickable { showFullScreenPhoto = true }
                                ) {
                                    if (thumbnailBitmap != null) {
                                        Image(
                                            bitmap = thumbnailBitmap.asImageBitmap(),
                                            contentDescription = "Miniatura avería",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Foto adjunta", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusGreen)
                                    }
                                    Text("Toque la imagen para ampliar", fontSize = 11.sp, color = Color.Gray)

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showFullScreenPhoto = true },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ver", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showCameraDialog = true },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cambiar", fontSize = 11.sp)
                                        }

                                        IconButton(
                                            onClick = { photoPath = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Foto", tint = StatusRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reported by staff input
                Text(
                    text = "REPORTADO POR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = reporterName,
                    onValueChange = { reporterName = it },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = HotelNavy) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("maintenance_reporter_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isBlank()) {
                        Toast.makeText(context, "Por favor describa el objeto o avería reportada.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanRoomNumber = selectedLocation.removePrefix("Habitación ").trim()

                    viewModel.reportBrokenItem(
                        roomNumber = cleanRoomNumber,
                        itemName = itemName.trim(),
                        category = selectedCategory,
                        priority = selectedPriority,
                        description = description.trim(),
                        photoPath = photoPath,
                        reportedBy = reporterName.trim(),
                        onSuccess = {
                            onSuccess()
                            onDismiss()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelGold, contentColor = HotelNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_maintenance_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar Reporte de Avería", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    )
}
