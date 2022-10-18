package pl.nowak.bitly

fun numberToByteArray(data: Number, size: Int = 4): ByteArray = ByteArray(size) { i -> (data.toLong() shr (i * 8)).toByte() }
