package com.mamang.datameter.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QuotaAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DataMeterDatabase : RoomDatabase() {

    abstract fun quotaAlertDao(): QuotaAlertDao

    companion object {
        @Volatile
        private var instance: DataMeterDatabase? = null

        fun getInstance(context: Context): DataMeterDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DataMeterDatabase::class.java,
                    "datameter_local.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
