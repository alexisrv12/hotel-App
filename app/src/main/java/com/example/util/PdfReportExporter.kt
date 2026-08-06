package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.entities.RoomEntity
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.StayHistoryEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    fun generateRevenueReportPdf(
        context: Context,
        hotelName: String = "Rivera Hotel & Restaurante",
        totalStaysRevenue: Double,
        totalSalesRevenue: Double,
        grandTotalRevenue: Double,
        cashTotal: Double,
        cardTotal: Double,
        transferTotal: Double,
        historyList: List<StayHistoryEntity>,
        salesList: List<SaleRecordEntity>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#1B2A4A") // HotelNavy
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#D4AF37") // HotelGold
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#1B2A4A")
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 40f

            // Header Banner
            canvas.drawRect(20f, y, 575f, y + 50f, headerBgPaint)
            canvas.drawText(hotelName.uppercase(), 35f, y + 25f, headerTextPaint.apply { textSize = 14f })
            val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText("REPORTE OFICIAL DE INGRESOS - GENERADO: $timestamp", 35f, y + 40f, headerTextPaint.apply { textSize = 9f })

            y += 70f

            // Summary Section
            canvas.drawText("1. RESUMEN FINANCIERO Y VENTAS", 20f, y, titlePaint)
            y += 15f
            canvas.drawLine(20f, y, 575f, y, linePaint)
            y += 20f

            canvas.drawText("Ingresos por Hospedajes:", 30f, y, textPaint)
            canvas.drawText("Q${String.format(Locale.US, "%.2f", totalStaysRevenue)}", 200f, y, boldPaint)

            canvas.drawText("Ingresos por Ventas Directas:", 320f, y, textPaint)
            canvas.drawText("Q${String.format(Locale.US, "%.2f", totalSalesRevenue)}", 480f, y, boldPaint)

            y += 20f

            canvas.drawText("GRAN TOTAL DE INGRESOS:", 30f, y, subtitlePaint.apply { textSize = 12f })
            canvas.drawText("Q${String.format(Locale.US, "%.2f", grandTotalRevenue)}", 200f, y, subtitlePaint)

            y += 30f

            // Payment Breakdown
            canvas.drawText("2. DESGLOSE POR MÉTODO DE PAGO", 20f, y, titlePaint)
            y += 15f
            canvas.drawLine(20f, y, 575f, y, linePaint)
            y += 20f

            canvas.drawText("• Efectivo: Q${String.format(Locale.US, "%.2f", cashTotal)}", 30f, y, textPaint)
            canvas.drawText("• Tarjeta Crédito/Débito: Q${String.format(Locale.US, "%.2f", cardTotal)}", 200f, y, textPaint)
            canvas.drawText("• Transferencia/Depósito: Q${String.format(Locale.US, "%.2f", transferTotal)}", 380f, y, textPaint)

            y += 35f

            // Detailed Stays History Table
            canvas.drawText("3. HISTORIAL RECIENTE DE HOSPEDAJES", 20f, y, titlePaint)
            y += 15f

            // Table Headers
            canvas.drawRect(20f, y, 575f, y + 20f, headerBgPaint)
            canvas.drawText("Hab.", 25f, y + 14f, headerTextPaint)
            canvas.drawText("Huésped", 70f, y + 14f, headerTextPaint)
            canvas.drawText("Tarifa / Tiempo", 220f, y + 14f, headerTextPaint)
            canvas.drawText("Fecha", 370f, y + 14f, headerTextPaint)
            canvas.drawText("Monto (Q)", 490f, y + 14f, headerTextPaint)

            y += 20f

            val itemsToShow = historyList.take(15)
            for (stay in itemsToShow) {
                if (y > 780f) break
                y += 18f
                canvas.drawText(stay.roomNumber, 25f, y, textPaint)
                canvas.drawText(stay.clientName.take(22), 70f, y, textPaint)
                canvas.drawText(stay.contractedTimeName.take(20), 220f, y, textPaint)
                canvas.drawText(stay.dateString, 370f, y, textPaint)
                canvas.drawText("Q${String.format(Locale.US, "%.2f", stay.priceCharged)}", 490f, y, boldPaint)
                canvas.drawLine(20f, y + 4f, 575f, y + 4f, linePaint)
            }

            // Footer
            canvas.drawText("Reporte generado por Gerencia - Sistema Rivera Hotel", 20f, 820f, textPaint.apply { textSize = 8f })

            pdfDocument.finishPage(page)

            val pdfFile = File(context.cacheDir, "Reporte_Ingresos_Hotel.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateOccupancyReportPdf(
        context: Context,
        hotelName: String = "Rivera Hotel & Restaurante",
        rooms: List<RoomEntity>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#1B2A4A")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#D4AF37")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#1B2A4A")
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 40f

            // Header Banner
            canvas.drawRect(20f, y, 575f, y + 50f, headerBgPaint)
            canvas.drawText(hotelName.uppercase(), 35f, y + 25f, headerTextPaint.apply { textSize = 14f })
            val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText("REPORTE OFICIAL DE OCUPACIÓN Y HABITACIONES - $timestamp", 35f, y + 40f, headerTextPaint.apply { textSize = 9f })

            y += 70f

            val totalRooms = rooms.size.coerceAtLeast(1)
            val occupied = rooms.count { it.status == "OCUPADA" }
            val available = rooms.count { it.status == "DISPONIBLE" }
            val cleaning = rooms.count { it.status == "PENDIENTE_LIMPIEZA" || it.status == "EN_LIMPIEZA" }
            val maintenance = rooms.count { it.status == "MANTENIMIENTO" }
            val occupancyRate = (occupied.toFloat() / totalRooms.toFloat()) * 100f

            // Summary Section
            canvas.drawText("1. ESTADÍSTICAS GENERALES DE OCUPACIÓN", 20f, y, titlePaint)
            y += 15f
            canvas.drawLine(20f, y, 575f, y, linePaint)
            y += 20f

            canvas.drawText("Total de Habitaciones: $totalRooms", 30f, y, boldPaint)
            canvas.drawText("Habitaciones Ocupadas: $occupied", 200f, y, boldPaint)
            canvas.drawText("Habitaciones Disponibles: $available", 380f, y, boldPaint)

            y += 20f

            canvas.drawText("En Limpieza / Desinfección: $cleaning", 30f, y, textPaint)
            canvas.drawText("Mantenimiento: $maintenance", 200f, y, textPaint)
            canvas.drawText("Tasa de Ocupación:", 380f, y, subtitlePaint)
            canvas.drawText("${String.format(Locale.US, "%.1f", occupancyRate)}%", 490f, y, subtitlePaint)

            y += 35f

            // Detailed Room List Table
            canvas.drawText("2. ESTADO DETALLADO POR HABITACIÓN", 20f, y, titlePaint)
            y += 15f

            canvas.drawRect(20f, y, 575f, y + 20f, headerBgPaint)
            canvas.drawText("Hab.", 25f, y + 14f, headerTextPaint)
            canvas.drawText("Tipo", 80f, y + 14f, headerTextPaint)
            canvas.drawText("Estado", 200f, y + 14f, headerTextPaint)
            canvas.drawText("Huésped Actual", 320f, y + 14f, headerTextPaint)
            canvas.drawText("Tarifa / Noche", 480f, y + 14f, headerTextPaint)

            y += 20f

            for (room in rooms) {
                if (y > 780f) break
                y += 18f
                canvas.drawText(room.roomNumber, 25f, y, boldPaint)
                canvas.drawText(room.roomType, 80f, y, textPaint)
                canvas.drawText(room.status, 200f, y, boldPaint)
                canvas.drawText(room.clientName ?: "N/A", 320f, y, textPaint)
                canvas.drawText("Q${room.nightlyRate}", 480f, y, textPaint)
                canvas.drawLine(20f, y + 4f, 575f, y + 4f, linePaint)
            }

            canvas.drawText("Reporte generado por Gerencia - Sistema Rivera Hotel", 20f, 820f, textPaint.apply { textSize = 8f })

            pdfDocument.finishPage(page)

            val pdfFile = File(context.cacheDir, "Reporte_Ocupacion_Hotel.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openOrSharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Abrir o Compartir Reporte PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
