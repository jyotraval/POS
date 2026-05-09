package com.example.pos.util

import android.content.Context
import android.net.Uri
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.entity.Category
import com.example.pos.data.entity.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.InputStream
import java.text.NumberFormat
import java.util.*

object InventoryUtils {
    
    data class InventoryItem(
        val item: String,
        val category: String,
        val price: String
    )
    
    data class ImportResult(
        val success: Boolean,
        val message: String,
        val itemsAdded: Int,
        val categoriesAdded: Int,
        val errors: List<String>
    )
    
    /**
     * Import inventory from CSV file
     */
    suspend fun importFromCsv(
        context: Context,
        uri: Uri,
        categoryDao: CategoryDao,
        itemDao: ItemDao
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            
            val fileName = getFileName(context, uri) ?: "unknown"
            val fileExtension = fileName.substringAfterLast('.', "").lowercase()
            
            // Only support CSV files for now
            if (fileExtension != "csv") {
                inputStream?.close()
                return@withContext ImportResult(
                    success = false,
                    message = "Only CSV files are supported. Please convert your Excel file to CSV format.",
                    itemsAdded = 0,
                    categoriesAdded = 0,
                    errors = listOf("File extension '$fileExtension' is not supported. Please use .csv files.")
                )
            }
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            reader.close()
            inputStream?.close()
            
            if (lines.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    message = "File is empty",
                    itemsAdded = 0,
                    categoriesAdded = 0,
                    errors = emptyList()
                )
            }
            
            // Delegate to content importer to share logic
            val content = lines.joinToString("\n")
            return@withContext importFromCsvContent(content, categoryDao, itemDao)
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error reading file: ${e.message}",
                itemsAdded = 0,
                categoriesAdded = 0,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    /**
     * Import inventory from raw CSV content (string). Allows paste-based import without a file.
     */
    suspend fun importFromCsvContent(
        content: String,
        categoryDao: CategoryDao,
        itemDao: ItemDao
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

            if (lines.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    message = "Content is empty",
                    itemsAdded = 0,
                    categoriesAdded = 0,
                    errors = emptyList()
                )
            }

            // Skip header row if it exists and normalize column names
            val dataLines = if (isHeaderRow(lines.first())) {
                lines.drop(1)
            } else {
                lines
            }

            val inventoryItems = mutableListOf<InventoryItem>()
            val errors = mutableListOf<String>()

            dataLines.forEachIndexed { index, line ->
                try {
                    val parts = line.split(",").map { it.trim() }
                    if (parts.size >= 3) {
                        val category = parts[1].ifBlank { "Defaultlt" }
                        inventoryItems.add(
                            InventoryItem(
                                item = parts[0],
                                category = category,
                                price = parts[2]
                            )
                        )
                    } else {
                        errors.add("Line ${index + 1}: Invalid format - expected 3 columns")
                    }
                } catch (e: Exception) {
                    errors.add("Line ${index + 1}: ${e.message}")
                }
            }

            if (inventoryItems.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    message = "No valid items found in content",
                    itemsAdded = 0,
                    categoriesAdded = 0,
                    errors = errors
                )
            }

            // Process categories and items
            val existingCategoriesFlow = categoryDao.getAllCategories()
            val existingCategories = existingCategoriesFlow.first().associateBy { it.name.lowercase() }
            val categoriesToAdd = mutableSetOf<String>()
            val itemsToAdd = mutableListOf<Item>()

            inventoryItems.forEach { inventoryItem ->
                // Validate price
                val price = try {
                    val cleanPrice = inventoryItem.price.replace("[^\\d.]".toRegex(), "")
                    cleanPrice.toDouble()
                } catch (e: Exception) {
                    errors.add("Item '${inventoryItem.item}': Invalid price '${inventoryItem.price}'")
                    return@forEach
                }

                if (price <= 0) {
                    errors.add("Item '${inventoryItem.item}': Price must be greater than 0")
                    return@forEach
                }

                // Check if category exists (use Defaultlt if blank)
                val categoryKey = inventoryItem.category.lowercase()
                if (!existingCategories.containsKey(categoryKey)) {
                    categoriesToAdd.add(inventoryItem.category)
                }
            }

            // Add new categories
            val categoryMap = existingCategories.toMutableMap()
            categoriesToAdd.forEach { categoryName ->
                val category = Category(name = categoryName)
                val categoryId = categoryDao.insert(category)
                categoryMap[categoryName.lowercase()] = category.copy(id = categoryId)
            }

            // Add items
            inventoryItems.forEach { inventoryItem ->
                val price = try {
                    val cleanPrice = inventoryItem.price.replace("[^\\d.]".toRegex(), "")
                    cleanPrice.toDouble()
                } catch (e: Exception) {
                    return@forEach // Already handled above
                }

                val category = categoryMap[inventoryItem.category.lowercase()]
                if (category != null) {
                    val item = Item(
                        name = inventoryItem.item,
                        price = price,
                        categoryId = category.id
                    )
                    itemsToAdd.add(item)
                }
            }

            // Insert items in batch
            itemsToAdd.forEach { item ->
                itemDao.insert(item)
            }

            ImportResult(
                success = true,
                message = "Successfully imported ${itemsToAdd.size} items and ${categoriesToAdd.size} categories",
                itemsAdded = itemsToAdd.size,
                categoriesAdded = categoriesToAdd.size,
                errors = errors
            )

        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error processing content: ${e.message}",
                itemsAdded = 0,
                categoriesAdded = 0,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }
    
    /**
     * Export inventory to CSV file
     */
    suspend fun exportToCsv(
        context: Context,
        categoryDao: CategoryDao,
        itemDao: ItemDao
    ): String = withContext(Dispatchers.IO) {
        try {
            val categoriesFlow = categoryDao.getAllCategories()
            val itemsFlow = itemDao.getAllItems()
            val categories = categoriesFlow.first()
            val items = itemsFlow.first()
            
            val categoryMap = categories.associateBy { it.id }
            
            val csvContent = StringBuilder()
            csvContent.appendLine("Item,Category,Price")
            
            items.forEach { item ->
                val category = categoryMap[item.categoryId]
                csvContent.appendLine("${item.name},${category?.name ?: "Unknown"},${item.price}")
            }
            
            // Save to file
            val fileName = "inventory_export_${System.currentTimeMillis()}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            val writer = FileWriter(file)
            writer.write(csvContent.toString())
            writer.close()
            
            file.absolutePath
            
        } catch (e: Exception) {
            throw Exception("Error exporting inventory: ${e.message}")
        }
    }
    
    /**
     * Get sample CSV content for user reference
     */
    fun getSampleCsvContent(): String {
        return """
            Item,Category,Price
            Apple,Fruits,1.50
            Banana,Fruits,0.80
            Bread,Bakery,2.00
            Milk,Dairy,3.50
            Rice,Groceries,4.00
        """.trimIndent()
    }
    
    /**
     * Helper function to get file name from URI
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        it.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Read CSV file and return lines
     */
    private fun readCsvFile(inputStream: InputStream): List<String> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        reader.close()
        return lines
    }
    
    /**
     * Check if the first row is a header row by normalizing column names
     */
    private fun isHeaderRow(firstLine: String): Boolean {
        val normalizedLine = firstLine.lowercase().trim()
        return normalizedLine.contains("item") && 
               normalizedLine.contains("category") && 
               normalizedLine.contains("price")
    }
}
