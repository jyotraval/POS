package com.example.pos

import android.app.Application
import com.example.pos.data.PosDatabase
import com.example.pos.data.entity.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PosApplication : Application() {
    val database: PosDatabase by lazy {
        try {
            PosDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("PosApplication", "Failed to initialize database", e)
            throw RuntimeException("Failed to initialize database: ${e.message}", e)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Ensure the default category exists on first run or after destructive migrations.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categoryDao = database.categoryDao()
                val existing = try {
                    categoryDao.getAllCategories().firstOrNull()?.firstOrNull { it.name.equals("default01", ignoreCase = true) }
                } catch (e: Exception) {
                    null
                }

                if (existing == null) {
                    categoryDao.insert(Category(name = "default01"))
                    android.util.Log.i("PosApplication", "Inserted default category 'default01'")
                }
            } catch (e: Exception) {
                android.util.Log.e("PosApplication", "Failed to ensure default category", e)
            }
        }
    }
}