package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EqPresetEntity::class], version = 1, exportSchema = false)
abstract class EqDatabase : RoomDatabase() {
    abstract fun eqPresetDao(): EqPresetDao

    companion object {
        @Volatile
        private var INSTANCE: EqDatabase? = null

        fun getInstance(context: Context): EqDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EqDatabase::class.java,
                    "voideq_equltra_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
