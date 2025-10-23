package com.example.pos

import android.app.Application
import com.example.pos.data.PosDatabase

class PosApplication : Application() {
    val database: PosDatabase by lazy { 
        try {
            PosDatabase.getDatabase(this)
        } catch (e: Exception) {
            // Log the error and return a fallback or throw a more descriptive error
            android.util.Log.e("PosApplication", "Failed to initialize database", e)
            throw RuntimeException("Failed to initialize database: ${e.message}", e)
        }
    }
}