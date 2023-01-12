package pl.nowak.bitly.ecg

import pl.nowak.bitly.LeadName
import pl.nowak.bitly.database.LeadEntry
import timber.log.Timber

data class EcgData(
    var status: UInt,
    var data: List<LeadEntry>
) {
    @ExperimentalUnsignedTypes
    companion object {
        fun loadData(data: UIntArray): EcgData {
            val timestamp = data[0].toFloat()
            // here is why it was implemented this way
            // https://hackernoon.com/squeezing-performance-from-sqlite-insertions-with-room-d769512f8330
            return EcgData(
                data[1], listOf(
                    LeadEntry(timestamp, data[2].convToI32().toFloat(), LeadName.LeadV6.ordinal),
                    LeadEntry(timestamp, data[3].convToI32().toFloat(), LeadName.LeadI.ordinal),
                    LeadEntry(timestamp, data[4].convToI32().toFloat(), LeadName.LeadII.ordinal),
                    LeadEntry(timestamp, data[5].convToI32().toFloat(), LeadName.LeadV2.ordinal),
                    LeadEntry(timestamp, data[6].convToI32().toFloat(), LeadName.LeadV3.ordinal),
                    LeadEntry(timestamp, data[7].convToI32().toFloat(), LeadName.LeadV4.ordinal),
                    LeadEntry(timestamp, data[8].convToI32().toFloat(), LeadName.LeadV5.ordinal),
                    LeadEntry(timestamp, data[9].convToI32().toFloat(), LeadName.LeadV1.ordinal),
                    LeadEntry(timestamp, data[10].convToI32().toFloat(), LeadName.LeadIII.ordinal),
                    LeadEntry(timestamp, data[11].convToI32().toFloat(), LeadName.LeadAVR.ordinal),
                    LeadEntry(timestamp, data[12].convToI32().toFloat(), LeadName.LeadAVL.ordinal),
                    LeadEntry(timestamp, data[13].convToI32().toFloat(), LeadName.LeadAVF.ordinal)
                )
            )
        }

        @ExperimentalUnsignedTypes
        fun UInt.convToI32(): Int {
            return if (this >= 0x800000u) {
                (0xFF000000u or this).toInt()
            } else {
                this.toInt()
            }
        }

        @ExperimentalUnsignedTypes
        fun convRawToU24(raw: ByteArray, pos: Int): UInt {
            return ((raw[pos + 0]).toUInt() shl 16) +
                    ((raw[pos + 1]).toUInt() shl 8) +
                    ((raw[pos + 2]).toUInt() shl 0)
        }
    }

    @ExperimentalUnsignedTypes
    fun convI32ToU24(i32_val: Int): UInt {
        return ((i32_val shl 8).toUInt()) shr 8
    }

    @ExperimentalUnsignedTypes
    fun convU24ToRaw(u24_val: UInt, pos: Int): UIntArray {
        val raw = UIntArray(3)
        raw[pos + 0] = 0xFFu and (u24_val shr 16)
        raw[pos + 1] = 0xFFu and (u24_val shr 8)
        raw[pos + 2] = 0xFFu and (u24_val shr 0)
        return raw
    }
}
