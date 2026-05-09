package com.example.pos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.entity.Settings
import com.example.pos.util.Converters

@Database(
    entities = [
        Settings::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PosDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        fun getDatabase(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "pos_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
