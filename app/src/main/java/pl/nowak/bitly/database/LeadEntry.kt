package pl.nowak.bitly.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.nowak.bitly.LeadName

abstract class LeadDataInteface {
    abstract val x: Float
    abstract val y: Float
    abstract val lead: Int
}

@Entity(tableName = "leads_table")
data class LeadEntry constructor(
    override var x: Float = -1.0f,
    override var y: Float = 0.0f,
    override var lead: Int = LeadName.LeadUnknown.ordinal,
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L
) : LeadDataInteface()