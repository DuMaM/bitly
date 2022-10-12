package pl.nowak.bitly.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.nowak.bitly.LeadName

@Entity(tableName = "leads_table")
data class LeadEntry constructor(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    val x: Float = -1.0f,
    val y: Float = 0.0f,
    val lead: LeadName = LeadName.LeadUnknown
)