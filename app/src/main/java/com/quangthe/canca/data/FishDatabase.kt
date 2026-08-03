package com.quangthe.canca.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FishTicket::class,
        FishSheet::class,
        FishCell::class,
        AppSettings::class
    ],
    version = 7,
    exportSchema = false
)
abstract class FishDatabase : RoomDatabase() {
    abstract fun fishDao(): FishDao

    companion object {
        @Volatile
        private var INSTANCE: FishDatabase? = null

        fun getDatabase(context: Context): FishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FishDatabase::class.java,
                    "fish_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
