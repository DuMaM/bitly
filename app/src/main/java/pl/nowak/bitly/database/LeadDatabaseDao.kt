package pl.nowak.bitly.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.nowak.bitly.LeadName

@Dao
interface LeadDatabaseDao {
    @Query("select * from leads_table")
    fun getLead(): List<LeadEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg lead: LeadEntry)

    @Query("SELECT * from leads_table WHERE id = :key")
    fun get(key: Long): LeadEntry

    @Query("SELECT * from leads_table WHERE lead = :leadName LIMIT :size")
    fun get(leadName: LeadName, size: Long = -1): LiveData<List<LeadEntry>>

    @Query("DELETE FROM leads_table")
    fun clear()
}