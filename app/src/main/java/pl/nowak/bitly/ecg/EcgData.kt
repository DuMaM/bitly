package pl.nowak.bitly.ecg

data class EcgData(
    var timestamp: UInt,
    var status: UInt,
    var v6_c6: Int,
    var lead1: Int,
    var lead2: Int,
    var v2_c2: Int,
    var v3_c3: Int,
    var v4_c4: Int,
    var v5_c5: Int,
    var v1_c1: Int,
    var lead3: Int,
    var aVR: Int,
    var aVL: Int,
    var aVF: Int
) {
    val LOFF_STATP_MASK = 0xFF000
    val LOFF_STATN_MASK = 0x00FF0
    val GPIO_MASK = 0x0000F

    fun decodeRaw(data: UInt): Int {
        val VREF = 3300
        if (data == 0x7FFFFFu) {
            return VREF
        }

        return VREF
    }

    @kotlin.ExperimentalUnsignedTypes
    companion object {
        fun loadData(data: UIntArray): EcgData {
            return EcgData(
                data[0],
                data[1],
                data[2].convToI32(),
                data[3].convToI32(),
                data[4].convToI32(),
                data[5].convToI32(),
                data[6].convToI32(),
                data[7].convToI32(),
                data[8].convToI32(),
                data[9].convToI32(),
                data[10].convToI32(),
                data[11].convToI32(),
                data[12].convToI32(),
                data[13].convToI32()
            )
        }

        fun UInt.convToI32(): Int {
            return ((this shl 8).toInt()) shl 8
        }

        fun convRawToU24(raw: ByteArray, pos: Int): UInt {
            return ((raw[pos + 0]).toUInt() shl 16) +
                    ((raw[pos + 1]).toUInt() shl 8) +
                    ((raw[pos + 2]).toUInt() shl 0)
        }
    }


    fun convI32ToU24(i32_val: Int): UInt {
        return ((i32_val shl 8).toUInt()) shr 8
    }

    fun convU24ToRaw(u24_val: UInt, pos: Int): UIntArray {
        var raw = UIntArray(3)
        raw[pos + 0] = 0xFFu and (u24_val shr 16)
        raw[pos + 1] = 0xFFu and (u24_val shr 8)
        raw[pos + 2] = 0xFFu and (u24_val shr 0)
        return raw
    }
}
