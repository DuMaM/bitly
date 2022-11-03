package pl.nowak.bitly.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.nowak.bitly.LeadName

abstract class LeadData {
    abstract val id: Long
    abstract val x: Float
    abstract val y: Float
    abstract val lead: Int
}

@Entity(tableName = "leads_table")
data class LeadEntry constructor(
    @PrimaryKey(autoGenerate = true)
    override var id: Long = 0L,
    override var x: Float = -1.0f,
    override var y: Float = 0.0f,
    override var lead: Int = LeadName.LeadUnknown.ordinal
) : LeadData()