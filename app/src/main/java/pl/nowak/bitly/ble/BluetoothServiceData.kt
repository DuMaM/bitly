package pl.nowak.bitly.ble

// check UIntRange
data class BluetoothServiceData(
    val status: UInt,
    val v6_c6: UInt,
    val lead1: UInt,
    val lead2: UInt,
    val v2_c2: UInt,
    val v3_c3: UInt,
    val v4_c4: UInt,
    val v5_c5: UInt,
    val v1_c1: UInt,
    val lead3: UInt,
    val aVR: UInt,
    val aVL: UInt,
    val aVF: UInt
)
