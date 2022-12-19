package pl.nowak.bitly.ecg

import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class EcgChartData(
    val label: String,
    val id: Int,
    val size: Int
) {
    var lineDataRestricted: kotlin.collections.ArrayDeque<Entry> = kotlin.collections.ArrayDeque(size)
    var loadedValSize: Int = 0
    val mutex = Mutex()

    @Volatile
    var newVal: Boolean = false

    fun clean() {
        lineDataRestricted.clear()
        Timber.i("Lead: " + label + " was cleared")
    }

    fun load(): List<Entry>? {
        return runBlocking {
            mutex.withLock {
                if (lineDataRestricted.isEmpty()) {
                    return@runBlocking null
                }

                var tmpVal = loadedValSize
                loadedValSize = 0
                if (tmpVal > size) {
                    tmpVal = size
                }

                newVal = false
                if (tmpVal == 0) {
                    return@withLock emptyList()
                } else {
                    return@withLock lineDataRestricted.toList().takeLast(tmpVal)
                }
            }
        }
    }

    fun getLastTimestamp(): Float {
        return if (lineDataRestricted.size > 0) {
            return lineDataRestricted.last().x
        } else {
            Float.NaN
        }
    }

    suspend fun update(x: Float, y: Float) {
        mutex.withLock {
            // add to the end
            // time in ms, value in mv
            val entry = Entry(x / 1000000, y / (12 * 1000000))
            if (lineDataRestricted.size > size) {
                lineDataRestricted.removeFirst()
            }
            lineDataRestricted.add(entry)
            newVal = true
            loadedValSize++
        }
    }
}
