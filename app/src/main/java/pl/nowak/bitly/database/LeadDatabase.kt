package pl.nowak.bitly.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LeadEntry::class], version = 1, exportSchema = false)
abstract class LeadDatabase : RoomDatabase() {
    abstract val leadDatabaseDao: LeadDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: LeadDatabase? = null

        fun getInstance(context: Context): LeadDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        LeadDatabase::class.java,
                        "ecg_leads_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}