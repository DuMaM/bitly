package pl.nowak.bitly.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LeadDao {
    @Query("select * from leads_table")
    fun getLeads(): List<LeadEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: Iterable<LeadEntry>)

    @Query("SELECT * from leads_table WHERE id = :key")
    fun getEntry(key: Long): LeadEntry

    @Query("SELECT * from leads_table WHERE lead = :leadName LIMIT :size")
    fun getLead(leadName: Int, size: Int = -1): LiveData<List<LeadEntry>>

    @Query("DELETE FROM leads_table")
    fun clear()
}