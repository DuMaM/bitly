package pl.nowak.bitly.ecg

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import pl.nowak.bitly.database.getDatabase
import pl.nowak.bitly.repository.EcgDataRepository
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SuppressLint("MissingPermission")
class EcgChartViewModel(application: Application) : AndroidViewModel(application) {
    var chartsDataList: MutableLiveData<List<EcgChartData>>
    var size = 40

    private fun <T> MutableLiveData<T>.forceRefresh() {
        this.postValue(value)
    }

    private val scope = CoroutineScope(Dispatchers.Main) // the scope of MyUIClass, uses Dispatchers.Main

    @Volatile
    private var isBlocked = false
    private suspend fun triggerUpdateWithDelay(duration: Duration) {
        if (isBlocked) {
            return
        }

        isBlocked = true
        scope.launch(Job()) {
            delay(duration)
            chartsDataList.forceRefresh()
            Timber.d("Graph update triggered")
            chartsDataList.value?.forEach {
                Timber.d("${it.lineDataRestricted.toArray().contentToString()}")
            }
            isBlocked = false
        }
    }

//    private var mutex = Mutex()
//    private suspend fun triggerUpdateWithDelay(duration: Duration) {
//        if (mutex.isLocked) {
//            return
//        }
//
//        mutex.withLock {
//            scope.launch {
//                delay(duration)
//                chartsDataList.forceRefresh()
//                Timber.d("Graph update triggered")
//            }
//        }
//    }


    init {
        // remove error
        // Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before calling Utils.convertDpToPixel(...). Otherwise conversion does not take place.
        chartsDataList = MutableLiveData(
            mutableListOf(
                EcgChartData("Lead V1", 8, size),
                EcgChartData("Lead V2", 4, size),
                EcgChartData("Lead V3", 5, size),
                EcgChartData("Lead V4", 6, size),
                EcgChartData("Lead V5", 7, size),
                EcgChartData("Lead V6", 1, size),
                EcgChartData("Lead I", 2, size),
                EcgChartData("Lead II", 3, size),
                EcgChartData("Lead III", 9, size),
                EcgChartData("Lead aVR", 10, size),
                EcgChartData("Lead aVL", 11, size),
                EcgChartData("Lead aVF", 12, size)
            )
        )

        Timber.i("created")
    }

    private val database = getDatabase(application)
    private val leadsRepository = EcgDataRepository(database, application)

    init {
        viewModelScope.launch {
            while (true) {
                leadsRepository.getData()?.collect {
                    it.apply {
                        chartsDataList.value?.get(0)?.update(timestamp.toFloat(), v1_c1.toFloat())
                        chartsDataList.value?.get(1)?.update(timestamp.toFloat(), v2_c2.toFloat())
                        chartsDataList.value?.get(2)?.update(timestamp.toFloat(), v3_c3.toFloat())
                        chartsDataList.value?.get(3)?.update(timestamp.toFloat(), v4_c4.toFloat())
                        chartsDataList.value?.get(4)?.update(timestamp.toFloat(), v5_c5.toFloat())
                        chartsDataList.value?.get(5)?.update(timestamp.toFloat(), v6_c6.toFloat())
                        chartsDataList.value?.get(6)?.update(timestamp.toFloat(), lead1.toFloat())
                        chartsDataList.value?.get(7)?.update(timestamp.toFloat(), lead2.toFloat())
                        chartsDataList.value?.get(8)?.update(timestamp.toFloat(), lead3.toFloat())
                        chartsDataList.value?.get(9)?.update(timestamp.toFloat(), aVR.toFloat())
                        chartsDataList.value?.get(10)?.update(timestamp.toFloat(), aVL.toFloat())
                        chartsDataList.value?.get(11)?.update(timestamp.toFloat(), aVF.toFloat())
                    }
                    triggerUpdateWithDelay(1.seconds)
                }
            }
        }
        Timber.i("created")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("destroyed")
    }
}

//                ecgData.apply {
//                    val timestamp = System.currentTimeMillis().toFloat()
//                    database.leadDao.insert(
//                        LeadEntry(y = v1_c1.toFloat(), x = timestamp, lead = LeadName.LeadV1.ordinal),
//                        LeadEntry(y = v2_c2.toFloat(), x = timestamp, lead = LeadName.LeadV2.ordinal),
//                        LeadEntry(y = v3_c3.toFloat(), x = timestamp, lead = LeadName.LeadV3.ordinal),
//                        LeadEntry(y = v4_c4.toFloat(), x = timestamp, lead = LeadName.LeadV4.ordinal),
//                        LeadEntry(y = v5_c5.toFloat(), x = timestamp, lead = LeadName.LeadV5.ordinal),
//                        LeadEntry(y = v6_c6.toFloat(), x = timestamp, lead = LeadName.LeadV6.ordinal),
//                        LeadEntry(y = lead1.toFloat(), x = timestamp, lead = LeadName.LeadI.ordinal),
//                        LeadEntry(y = lead2.toFloat(), x = timestamp, lead = LeadName.LeadII.ordinal),
//                        LeadEntry(y = lead3.toFloat(), x = timestamp, lead = LeadName.LeadIII.ordinal),
//                        LeadEntry(y = aVF.toFloat(), x = timestamp, lead = LeadName.LeadAVL.ordinal),
//                        LeadEntry(y = aVR.toFloat(), x = timestamp, lead = LeadName.LeadAVR.ordinal),
//                        LeadEntry(y = aVF.toFloat(), x = timestamp, lead = LeadName.LeadAVF.ordinal)
//                    )
//                }
