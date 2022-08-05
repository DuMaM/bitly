package pl.nowak.bitly

enum class Direction {
    BT_TEST_TYPE_UNKNOWN,
    BT_TEST_TYPE_RESET,
    BT_TEST_TYPE_SIMPLE,
    BT_TEST_TYPE_BER,
    BT_TEST_TYPE_ANALOG,
    BT_TEST_TYPE_SIM
}

class Metrics {
    /** Number of GATT writes received. */
    public val write_count: UInt = 0u

    /** Number of bytes received. */
    public val write_len: UInt = 0u

    /** Transfer speed in bits per second. */
    public val write_rate: UInt = 0u

    /** error count if BER is enabled. **/
    public val error_count: Int = 0
}
