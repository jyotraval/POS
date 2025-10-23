package com.example.pos.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import java.text.SimpleDateFormat
import java.util.*

object PrinterUtils {
    private const val DEFAULT_PRINTER_DPI = 203
    private const val DEFAULT_PRINTER_WIDTH_MM = 58
    private const val DEFAULT_PRINTER_CHARS_PER_LINE = 32

    fun testPrint(
        context: Context,
        macAddress: String,
        stallName: String,
        address: String,
        phone: String,
        paddingTop: Int,
        paddingBottom: Int
    ): Boolean {
        try {
            val printer = connectPrinter(macAddress) ?: return false

            val testText = getTestReceiptText(
                stallName,
                address,
                phone,
                paddingTop,
                paddingBottom
            )

            printer.printFormattedText(testText)
            printer.disconnectPrinter()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun connectPrinter(macAddress: String): EscPosPrinter? {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                return null
            }
            
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
            val connection = BluetoothPrintersConnections.selectFirstPaired()
            
            return EscPosPrinter(
                connection,
                DEFAULT_PRINTER_DPI,
                DEFAULT_PRINTER_WIDTH_MM.toFloat(),
                DEFAULT_PRINTER_CHARS_PER_LINE
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getTestReceiptText(
        stallName: String,
        address: String,
        phone: String,
        paddingTop: Int,
        paddingBottom: Int
    ): String {
        val date = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        
        return "[C]<b>$stallName</b>\n" +
            "[C]$address\n" +
            "[C]$phone\n" +
            "[C]--------------------------------\n" +
            "[L]Date: $date\n" +
            "[C]*** TEST PRINT ***\n" +
            "[C]--------------------------------\n" +
            "[L]Printer connection successful!\n" +
            "[L]\n".repeat(paddingBottom)
    }

    fun printReceipt(
        macAddress: String,
        stallName: String,
        address: String,
        phone: String,
        txnId: String,
        buyerName: String?,
        buyerPhone: String?,
        items: List<ReceiptItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paddingTop: Int,
        paddingBottom: Int
    ): Boolean {
        try {
            val printer = connectPrinter(macAddress) ?: return false
            
            val receiptText = getReceiptText(
                stallName,
                address,
                phone,
                txnId,
                buyerName,
                buyerPhone,
                items,
                subtotal,
                discount,
                total,
                paddingTop,
                paddingBottom
            )

            printer.printFormattedText(receiptText)
            printer.disconnectPrinter()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    data class ReceiptItem(
        val name: String,
        val quantity: Int,
        val unitPrice: Double,
        val total: Double
    )

    private fun getReceiptText(
        stallName: String,
        address: String,
        phone: String,
        txnId: String,
        buyerName: String?,
        buyerPhone: String?,
        items: List<ReceiptItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paddingTop: Int,
        paddingBottom: Int
    ): String {
        val date = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        val sb = StringBuilder()

        // Header
        sb.append("[L]\n".repeat(paddingTop))
        sb.append("[C]<b>$stallName</b>\n")
        sb.append("[C]$address\n")
        sb.append("[C]$phone\n")
        sb.append("[C]--------------------------------\n")
        
        // Transaction info
        sb.append("[L]Date: $date\n")
        sb.append("[L]Txn ID: $txnId\n")
        if (!buyerName.isNullOrBlank()) {
            sb.append("[L]Buyer: $buyerName\n")
        }
        if (!buyerPhone.isNullOrBlank()) {
            sb.append("[L]Phone: $buyerPhone\n")
        }
        sb.append("[C]--------------------------------\n")
        
        // Items
        sb.append("[L]<b>Item      Qty  Price  Total</b>\n")
        items.forEach { item ->
            val name = item.name.take(8).padEnd(8)
            val qty = item.quantity.toString().padStart(3)
            val price = String.format("%.1f", item.unitPrice).padStart(6)
            val total = String.format("%.1f", item.total).padStart(6)
            sb.append("[L]$name  $qty  $price  $total\n")
        }
        
        // Totals
        sb.append("[C]--------------------------------\n")
        sb.append("[R]Subtotal: ${String.format("%.2f", subtotal)}\n")
        if (discount > 0) {
            sb.append("[R]Discount: -${String.format("%.2f", discount)}\n")
        }
        sb.append("[R]<b>Total: ${String.format("%.2f", total)}</b>\n")
        
        // Footer
        sb.append("[C]--------------------------------\n")
        sb.append("[C]Thank You!\n")
        sb.append("[L]\n".repeat(paddingBottom))

        return sb.toString()
    }
}