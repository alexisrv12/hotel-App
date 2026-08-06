package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.entities.InvoiceEntity
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.HotelGold
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun InvoiceDialog(
    invoice: InvoiceEntity,
    onDismiss: () -> Unit,
    onVoidRequested: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Compute used time text if checkIn & checkOut formats are HH:mm
    val usedTimeText = remember(invoice) {
        try {
            val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
            val inDate = sdf.parse(invoice.checkInTime)
            val outDate = sdf.parse(invoice.checkOutTime)
            if (inDate != null && outDate != null) {
                var diffMs = outDate.time - inDate.time
                if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000L // Next day crossing
                val hours = diffMs / (1000 * 60 * 60)
                val mins = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
                if (hours > 0) "${hours}h ${mins}m" else "${mins} min"
            } else {
                invoice.contractedTime
            }
        } catch (e: Exception) {
            invoice.contractedTime
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COMPROBANTE DE PAGO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Printable Invoice Paper Box using ReceiptTemplate
                ReceiptTemplate(invoice = invoice)

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons for Receptionist: Imprimir / WhatsApp / PDF
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { printInvoiceDocument(context, invoice) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Imprimir", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { shareInvoiceWhatsApp(context, invoice) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shareInvoicePdf(context, invoice) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartir PDF", fontSize = 12.sp)
                        }

                        if (!invoice.isVoided && onVoidRequested != null) {
                            OutlinedButton(
                                onClick = onVoidRequested,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                            ) {
                                Text("Anular", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

fun printInvoiceDocument(context: Context, invoice: InvoiceEntity) {
    printReceipt(
        context = context,
        hotelName = invoice.hotelName,
        hotelAddress = invoice.hotelAddress,
        hotelPhone = invoice.hotelPhone,
        hotelNit = invoice.hotelNit,
        receiptNumber = invoice.invoiceNumber,
        dateString = invoice.dateString,
        timeString = invoice.timeString,
        clientName = invoice.clientName,
        roomNumber = invoice.roomNumber,
        contractedTime = invoice.contractedTime,
        checkInTime = invoice.checkInTime,
        checkOutTime = invoice.checkOutTime,
        paymentMethod = invoice.paymentMethod,
        receptionistName = invoice.receptionistName,
        price = invoice.price,
        discount = invoice.discount,
        totalAmount = invoice.totalAmount
    )
}

fun printReceipt(
    context: Context,
    hotelName: String = "Hotel Rivera",
    hotelAddress: String = "Calle Principal, Ciudad",
    hotelPhone: String = "(502) 7761-0000",
    hotelNit: String = "1234567-8",
    receiptNumber: String,
    dateString: String,
    timeString: String,
    clientName: String,
    roomNumber: String,
    contractedTime: String,
    checkInTime: String,
    checkOutTime: String,
    paymentMethod: String,
    receptionistName: String,
    price: Double,
    discount: Double = 0.0,
    totalAmount: Double
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val webView = WebView(context)
    val htmlContent = """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: sans-serif; padding: 20px; font-size: 13px; color: #1e293b; }
                .center { text-align: center; }
                .bold { font-weight: bold; }
                .header { font-size: 20px; margin-bottom: 4px; color: #0F172A; }
                .divider { border-top: 1px dashed #cbd5e1; margin: 10px 0; }
                .row { display: flex; justify-content: space-between; margin: 5px 0; }
                .total { font-size: 18px; color: #0F172A; margin-top: 10px; }
            </style>
        </head>
        <body>
            <div class="center">
                <div class="header bold">${hotelName}</div>
                <div>${hotelAddress}</div>
                <div>Tel: ${hotelPhone} | NIT: ${hotelNit}</div>
            </div>
            <div class="divider"></div>
            <div class="row"><span class="bold">COMPROBANTE No:</span> <span class="bold">${receiptNumber}</span></div>
            <div class="row"><span>Fecha y Hora:</span> <span>${dateString} ${timeString}</span></div>
            <div class="divider"></div>
            <div class="row"><span>Cliente:</span> <span>${clientName}</span></div>
            <div class="row"><span>Habitación:</span> <span>${roomNumber}</span></div>
            <div class="row"><span>Tiempo Contratado:</span> <span>${contractedTime}</span></div>
            <div class="row"><span>Hora Entrada:</span> <span>${checkInTime}</span></div>
            <div class="row"><span>Hora Salida:</span> <span>${checkOutTime}</span></div>
            <div class="row"><span>Método Pago:</span> <span>${paymentMethod}</span></div>
            <div class="row"><span>Atendido por:</span> <span>${receptionistName}</span></div>
            <div class="divider"></div>
            <div class="row"><span>Subtotal:</span> <span>Q${String.format(Locale.US, "%.2f", price)}</span></div>
            ${if (discount > 0) "<div class=\"row\"><span>Descuento:</span> <span>-Q${String.format(Locale.US, "%.2f", discount)}</span></div>" else ""}
            <div class="row total bold"><span>TOTAL PAGADO:</span> <span>Q${String.format(Locale.US, "%.2f", totalAmount)}</span></div>
            <div class="divider"></div>
            <div class="center bold" style="margin-top: 15px;">¡Gracias por su preferencia!</div>
        </body>
        </html>
    """.trimIndent()

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printAdapter = webView.createPrintDocumentAdapter("Comprobante_${receiptNumber}")
            val jobName = "${hotelName} - ${receiptNumber}"
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}

fun shareInvoiceWhatsApp(context: Context, invoice: InvoiceEntity) {
    shareReceiptWhatsApp(
        context = context,
        hotelName = invoice.hotelName,
        receiptNumber = invoice.invoiceNumber,
        dateString = invoice.dateString,
        timeString = invoice.timeString,
        clientName = invoice.clientName,
        roomNumber = invoice.roomNumber,
        contractedTime = invoice.contractedTime,
        checkInTime = invoice.checkInTime,
        checkOutTime = invoice.checkOutTime,
        paymentMethod = invoice.paymentMethod,
        receptionistName = invoice.receptionistName,
        price = invoice.price,
        discount = invoice.discount,
        totalAmount = invoice.totalAmount
    )
}

fun shareReceiptWhatsApp(
    context: Context,
    hotelName: String = "Hotel Rivera",
    receiptNumber: String,
    dateString: String,
    timeString: String,
    clientName: String,
    roomNumber: String,
    contractedTime: String,
    checkInTime: String,
    checkOutTime: String,
    paymentMethod: String,
    receptionistName: String,
    price: Double,
    discount: Double = 0.0,
    totalAmount: Double
) {
    val message = """
        *${hotelName}*
        _Comprobante de Pago_
        ----------------------------------
        *Comprobante No:* ${receiptNumber}
        *Fecha:* ${dateString} ${timeString}
        ----------------------------------
        *Cliente:* ${clientName}
        *Habitación:* ${roomNumber}
        *Tiempo Contratado:* ${contractedTime}
        *Hora Ingreso:* ${checkInTime}
        *Hora Salida:* ${checkOutTime}
        *Método Pago:* ${paymentMethod}
        *Atendido por:* ${receptionistName}
        ----------------------------------
        *Subtotal:* Q${String.format(Locale.US, "%.2f", price)}
        ${if (discount > 0) "*Descuento:* -Q${String.format(Locale.US, "%.2f", discount)}\n" else ""}*TOTAL PAGADO:* Q${String.format(Locale.US, "%.2f", totalAmount)}
        ----------------------------------
        ¡Gracias por su preferencia!
    """.trimIndent()

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://api.whatsapp.com/send?text=" + java.net.URLEncoder.encode(message, "UTF-8"))
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir Comprobante por WhatsApp"))
    }
}

fun shareInvoicePdf(context: Context, invoice: InvoiceEntity) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(320, 520, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    paint.color = android.graphics.Color.BLACK
    paint.textSize = 14f
    paint.isFakeBoldText = true

    var y = 32f
    canvas.drawText(invoice.hotelName, 20f, y, paint)
    y += 18f
    paint.isFakeBoldText = false
    paint.textSize = 9f
    canvas.drawText(invoice.hotelAddress, 20f, y, paint)
    y += 14f
    canvas.drawText("Tel: ${invoice.hotelPhone} | NIT: ${invoice.hotelNit}", 20f, y, paint)
    y += 20f

    paint.isFakeBoldText = true
    paint.textSize = 11f
    canvas.drawText("COMPROBANTE No: ${invoice.invoiceNumber}", 20f, y, paint)
    y += 16f
    paint.isFakeBoldText = false
    paint.textSize = 10f
    canvas.drawText("Fecha: ${invoice.dateString} ${invoice.timeString}", 20f, y, paint)
    y += 20f

    canvas.drawText("Cliente: ${invoice.clientName}", 20f, y, paint)
    y += 15f
    canvas.drawText("Habitación: ${invoice.roomNumber}", 20f, y, paint)
    y += 15f
    canvas.drawText("Tiempo: ${invoice.contractedTime}", 20f, y, paint)
    y += 15f
    canvas.drawText("Entrada: ${invoice.checkInTime}  |  Salida: ${invoice.checkOutTime}", 20f, y, paint)
    y += 15f
    canvas.drawText("Método Pago: ${invoice.paymentMethod}", 20f, y, paint)
    y += 15f
    canvas.drawText("Atendido por: ${invoice.receptionistName}", 20f, y, paint)
    y += 22f

    paint.isFakeBoldText = true
    paint.textSize = 12f
    canvas.drawText("TOTAL PAGADO: Q${String.format(Locale.US, "%.2f", invoice.totalAmount)}", 20f, y, paint)

    pdfDocument.finishPage(page)

    val file = File(context.cacheDir, "Comprobante_${invoice.invoiceNumber}.pdf")
    try {
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir PDF de Comprobante"))
    } catch (e: Exception) {
        e.printStackTrace()
        shareInvoiceWhatsApp(context, invoice)
    }
}

// --- EXPORT DIALOG COMPOSABLE (EXCLUSIVO PARA GERENTE) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExportRequested: (category: String, format: String) -> Unit
) {

    var selectedCategory by remember { mutableStateOf("Historial") }
    var selectedFormat by remember { mutableStateOf("Excel (.xlsx / .csv)") }
    var expandedCat by remember { mutableStateOf(false) }
    var expandedFormat by remember { mutableStateOf(false) }

    val categories = listOf(
        "Historial de hospedajes",
        "Facturas",
        "Ventas",
        "Inventario de insumos",
        "Inventario de ventas",
        "Reportes diarios",
        "Reportes semanales",
        "Reportes mensuales",
        "Lista de clientes",
        "Movimientos de habitaciones"
    )

    val formats = listOf("Excel (.xlsx / .csv)", "PDF Documento")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportar Información del Sistema", fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Seleccione la categoría de información y el formato deseado:", fontSize = 13.sp)

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = !expandedCat }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría a Exportar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                // Format Selector
                ExposedDropdownMenuBox(
                    expanded = expandedFormat,
                    onExpandedChange = { expandedFormat = !expandedFormat }
                ) {
                    OutlinedTextField(
                        value = selectedFormat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Formato de Archivo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFormat) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFormat,
                        onDismissRequest = { expandedFormat = false }
                    ) {
                        formats.forEach { fmt ->
                            DropdownMenuItem(
                                text = { Text(fmt) },
                                onClick = {
                                    selectedFormat = fmt
                                    expandedFormat = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExportRequested(selectedCategory, selectedFormat)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar Archivo")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
    }
}

@Composable
fun ReceiptTemplate(
    hotelName: String = "Hotel Rivera",
    hotelAddress: String = "Calle Principal, Ciudad",
    hotelPhone: String = "(502) 7761-0000",
    hotelNit: String = "1234567-8",
    receiptNumber: String,
    dateString: String,
    timeString: String,
    clientName: String,
    roomNumber: String,
    contractedTime: String,
    usedTime: String,
    checkInTime: String,
    checkOutTime: String,
    paymentMethod: String,
    receptionistName: String,
    subtotal: Double,
    discount: Double = 0.0,
    totalAmount: Double,
    isVoided: Boolean = false,
    voidReason: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo Placeholder & Hotel Info
            Surface(
                shape = RoundedCornerShape(50),
                color = HotelNavy,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("HR", color = HotelGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = hotelName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = HotelNavy
            )
            Text(text = hotelAddress, fontSize = 12.sp, color = Color.Gray)
            Text(text = "Tel: $hotelPhone", fontSize = 12.sp, color = Color.Gray)
            Text(text = "NIT: $hotelNit", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color.LightGray)

            Spacer(modifier = Modifier.height(8.dp))

            // Invoice Correlative & Date/Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("COMPROBANTE No:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        receiptNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("FECHA Y HORA:", fontSize = 11.sp, color = Color.Gray)
                    Text("$dateString $timeString", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (isVoided) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = StatusRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "DOCUMENTO ANULADO",
                            fontWeight = FontWeight.Bold,
                            color = StatusRed,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!voidReason.isNullOrBlank()) {
                            Text(
                                text = "Motivo: $voidReason",
                                fontSize = 11.sp,
                                color = StatusRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color.LightGray)

            Spacer(modifier = Modifier.height(12.dp))

            // Client & Room Details
            ReceiptDetailRow("Cliente:", clientName)
            ReceiptDetailRow("Habitación:", roomNumber)
            ReceiptDetailRow("Tiempo Contratado:", contractedTime)
            ReceiptDetailRow("Tiempo Utilizado:", usedTime)
            ReceiptDetailRow("Hora de Entrada:", checkInTime)
            ReceiptDetailRow("Hora de Salida:", checkOutTime)
            ReceiptDetailRow("Método de Pago:", paymentMethod)
            ReceiptDetailRow("Atendido Por:", receptionistName)

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color.LightGray)

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", fontSize = 13.sp)
                Text("Q${String.format(Locale.US, "%.2f", subtotal)}", fontSize = 13.sp)
            }

            if (discount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Descuento:", fontSize = 13.sp, color = StatusGreen)
                    Text("-Q${String.format(Locale.US, "%.2f", discount)}", fontSize = 13.sp, color = StatusGreen)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOTAL PAGADO:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                Text(
                    "Q${String.format(Locale.US, "%.2f", totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = HotelNavy
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¡Gracias por su preferencia!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ReceiptTemplate(
    invoice: InvoiceEntity,
    modifier: Modifier = Modifier
) {
    val usedTimeText = remember(invoice) {
        try {
            val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
            val inDate = sdf.parse(invoice.checkInTime)
            val outDate = sdf.parse(invoice.checkOutTime)
            if (inDate != null && outDate != null) {
                var diffMs = outDate.time - inDate.time
                if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000L
                val hours = diffMs / (1000 * 60 * 60)
                val mins = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
                if (hours > 0) "${hours}h ${mins}m" else "${mins} min"
            } else {
                invoice.contractedTime
            }
        } catch (e: Exception) {
            invoice.contractedTime
        }
    }

    ReceiptTemplate(
        hotelName = invoice.hotelName,
        hotelAddress = invoice.hotelAddress,
        hotelPhone = invoice.hotelPhone,
        hotelNit = invoice.hotelNit,
        receiptNumber = invoice.invoiceNumber,
        dateString = invoice.dateString,
        timeString = invoice.timeString,
        clientName = invoice.clientName,
        roomNumber = invoice.roomNumber,
        contractedTime = invoice.contractedTime,
        usedTime = usedTimeText,
        checkInTime = invoice.checkInTime,
        checkOutTime = invoice.checkOutTime,
        paymentMethod = invoice.paymentMethod,
        receptionistName = invoice.receptionistName,
        subtotal = invoice.price,
        discount = invoice.discount,
        totalAmount = invoice.totalAmount,
        isVoided = invoice.isVoided,
        voidReason = invoice.voidReason,
        modifier = modifier
    )
}
