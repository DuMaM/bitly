package pl.nowak.bitly.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LeadEntry::class], version = 3, exportSchema = false)
abstract class LeadDatabase : RoomDatabase() {
    abstract val leadDao: LeadDao
}

@Volatile
private lateinit var INSTANCE: LeadDatabase

fun getDatabase(context: Context): LeadDatabase {
    synchronized(LeadDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                LeadDatabase::class.java,
                "ecg_leads_database"
            ).fallbackToDestructiveMigration()
                .build()
        }

        return INSTANCE
    }
}

