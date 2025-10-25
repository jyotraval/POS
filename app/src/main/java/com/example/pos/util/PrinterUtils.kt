package com.example.pos.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import java.text.SimpleDateFormat
import java.util.*

object PrinterUtils {
    private const val DEFAULT_PRINTER_DPI = 203
    private const val DEFAULT_PRINTER_WIDTH_MM = 58
    private const val DEFAULT_PRINTER_CHARS_PER_LINE = 32
    
    // DEBUG MODE - Set to true when you don't have a printer
    private const val DEBUG_MODE = false

    private fun hasBluetoothPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isPrinterPaired(macAddress: String, context: Context): Boolean {
        try {
            if (!hasBluetoothPermissions(context)) {
                android.util.Log.w("PrinterUtils", "Bluetooth permissions not granted")
                return false
            }
            
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return false
            }
            
            val pairedDevices = bluetoothAdapter.bondedDevices
            for (device in pairedDevices) {
                if (device.address.equals(macAddress, ignoreCase = true)) {
                    android.util.Log.d("PrinterUtils", "Printer $macAddress is paired")
                    return true
                }
            }
            
            android.util.Log.w("PrinterUtils", "Printer $macAddress is not paired")
            return false
        } catch (e: SecurityException) {
            android.util.Log.e("PrinterUtils", "Security exception - Bluetooth permission denied", e)
            return false
        } catch (e: Exception) {
            android.util.Log.e("PrinterUtils", "Error checking printer pairing", e)
            return false
        }
    }

    fun listPairedDevices(): List<String> {
        val devices = mutableListOf<String>()
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return devices
            }
            
            val pairedDevices = bluetoothAdapter.bondedDevices
            for (device in pairedDevices) {
                val deviceInfo = "${device.name} (${device.address})"
                devices.add(deviceInfo)
                android.util.Log.d("PrinterUtils", "Paired device: $deviceInfo")
            }
        } catch (e: Exception) {
            android.util.Log.e("PrinterUtils", "Error listing paired devices", e)
        }
        return devices
    }

    data class BluetoothDeviceInfo(
        val name: String,
        val address: String,
        val isPrinter: Boolean = false
    )

    fun getPairedBluetoothDevices(context: Context): List<BluetoothDeviceInfo> {
        val devices = mutableListOf<BluetoothDeviceInfo>()
        try {
            if (!hasBluetoothPermissions(context)) {
                android.util.Log.w("PrinterUtils", "Bluetooth permissions not granted")
                return devices
            }
            
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                android.util.Log.e("PrinterUtils", "Bluetooth adapter is null")
                return devices
            }
            
            if (!bluetoothAdapter.isEnabled) {
                android.util.Log.e("PrinterUtils", "Bluetooth is not enabled")
                return devices
            }
            
            android.util.Log.d("PrinterUtils", "Getting paired devices...")
            val pairedDevices = bluetoothAdapter.bondedDevices
            android.util.Log.d("PrinterUtils", "Found ${pairedDevices.size} paired devices")
            
            for (device in pairedDevices) {
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address
                val isPrinter = isLikelyPrinter(deviceName)
                
                val deviceInfo = BluetoothDeviceInfo(
                    name = deviceName,
                    address = deviceAddress,
                    isPrinter = isPrinter
                )
                devices.add(deviceInfo)
                android.util.Log.d("PrinterUtils", "Paired device: $deviceName ($deviceAddress) - Printer: $isPrinter")
            }
            
            android.util.Log.d("PrinterUtils", "Returning ${devices.size} devices")
        } catch (e: SecurityException) {
            android.util.Log.e("PrinterUtils", "Security exception - Bluetooth permission denied", e)
        } catch (e: Exception) {
            android.util.Log.e("PrinterUtils", "Error getting paired devices", e)
            e.printStackTrace()
        }
        return devices
    }

    private fun isLikelyPrinter(deviceName: String?): Boolean {
        if (deviceName == null) return false
        val name = deviceName.lowercase()
        return name.contains("printer") || 
               name.contains("pos") || 
               name.contains("thermal") || 
               name.contains("receipt") ||
               name.contains("epson") ||
               name.contains("star") ||
               name.contains("citizen") ||
               name.contains("zebra")
    }

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
            android.util.Log.d("PrinterUtils", "Starting test print with MAC: $macAddress")
            
            if (DEBUG_MODE) {
                android.util.Log.d("PrinterUtils", "🔧 DEBUG MODE: Simulating test print")
                val testText = getTestReceiptText(
                    stallName,
                    address,
                    phone,
                    paddingTop,
                    paddingBottom
                )
                android.util.Log.d("PrinterUtils", "📄 DEBUG - Test Receipt Content:")
                android.util.Log.d("PrinterUtils", testText)
                android.util.Log.d("PrinterUtils", "✅ DEBUG MODE: Test print completed successfully")
                return true
            }
            
            // Check if printer is paired first
            if (!isPrinterPaired(macAddress, context)) {
                android.util.Log.e("PrinterUtils", "Printer $macAddress is not paired")
                return false
            }
            
            val printer = connectPrinter(macAddress)
            if (printer == null) {
                android.util.Log.e("PrinterUtils", "Failed to connect to printer")
                return false
            }

            val testText = getTestReceiptText(
                stallName,
                address,
                phone,
                paddingTop,
                paddingBottom
            )

            android.util.Log.d("PrinterUtils", "Printing test text: $testText")
            printer.printFormattedText(testText)
            printer.disconnectPrinter()
            
            android.util.Log.d("PrinterUtils", "Test print completed successfully")
            return true
        } catch (e: Exception) {
            android.util.Log.e("PrinterUtils", "Test print failed", e)
            e.printStackTrace()
            return false
        }
    }

    private fun connectPrinter(macAddress: String): EscPosPrinter? {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                android.util.Log.e("PrinterUtils", "Bluetooth adapter not available")
                return null
            }
            
            if (!bluetoothAdapter.isEnabled) {
                android.util.Log.e("PrinterUtils", "Bluetooth is not enabled")
                return null
            }
            
            android.util.Log.d("PrinterUtils", "Looking for printer with MAC: $macAddress")
            
            // Get all paired devices
            val pairedDevices = bluetoothAdapter.bondedDevices
            android.util.Log.d("PrinterUtils", "Found ${pairedDevices.size} paired devices")
            
            var targetDevice: BluetoothDevice? = null
            for (device in pairedDevices) {
                android.util.Log.d("PrinterUtils", "Paired device: ${device.name} (${device.address})")
                if (device.address.equals(macAddress, ignoreCase = true)) {
                    targetDevice = device
                    break
                }
            }
            
            if (targetDevice == null) {
                android.util.Log.e("PrinterUtils", "Printer with MAC $macAddress not found in paired devices")
                return null
            }
            
            android.util.Log.d("PrinterUtils", "Found target device: ${targetDevice.name} (${targetDevice.address})")
            
            // Try to create connection using the specific device
            val connection = try {
                BluetoothPrintersConnections.selectFirstPaired()
            } catch (e: Exception) {
                android.util.Log.e("PrinterUtils", "Failed to create Bluetooth connection", e)
                null
            }
            
            if (connection == null) {
                android.util.Log.e("PrinterUtils", "Failed to create printer connection")
                return null
            }
            
            val printer = EscPosPrinter(
                connection,
                DEFAULT_PRINTER_DPI,
                DEFAULT_PRINTER_WIDTH_MM.toFloat(),
                DEFAULT_PRINTER_CHARS_PER_LINE
            )
            
            android.util.Log.d("PrinterUtils", "Printer connection created successfully")
            return printer
            
        } catch (e: Exception) {
            android.util.Log.e("PrinterUtils", "Failed to connect to printer", e)
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
        context: Context,
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
            android.util.Log.d("PrinterUtils", "Starting receipt print with MAC: $macAddress")
            
            if (DEBUG_MODE) {
                android.util.Log.d("PrinterUtils", "🔧 DEBUG MODE: Simulating receipt print")
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
                android.util.Log.d("PrinterUtils", "🧾 DEBUG - Receipt Content:")
                android.util.Log.d("PrinterUtils", receiptText)
                android.util.Log.d("PrinterUtils", "✅ DEBUG MODE: Receipt print completed successfully")
                return true
            }
            
            // Check if printer is paired first
            if (!isPrinterPaired(macAddress, context)) {
                android.util.Log.e("PrinterUtils", "Printer $macAddress is not paired")
                return false
            }
            
            val printer = connectPrinter(macAddress)
            if (printer == null) {
                android.util.Log.e("PrinterUtils", "Failed to connect to printer for receipt")
                return false
            }
            
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

            android.util.Log.d("PrinterUtils", "Printing receipt: $receiptText")
            printer.printFormattedText(receiptText)
            printer.disconnectPrinter()
            
            android.util.Log.d("PrinterUtils", "Receipt print completed successfully")
            return true
        } catch (e: Exception) {
            android.util.Log.e("PrinterUtils", "Receipt print failed", e)
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