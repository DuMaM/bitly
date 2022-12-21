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

@SuppressLint("MissingPermission")
class EcgChartViewModel(application: Application) : AndroidViewModel(application) {
    var chartsDataList: MutableLiveData<List<EcgChartData_Test>>
    private var size = 2000

    private val scope = CoroutineScope(Dispatchers.Main) // the scope of MyUIClass, uses Dispatchers.Main
    private val database = getDatabase(application)
    private val leadsRepository = EcgDataRepository(database, application)

    @Volatile
    private var isBlocked = false


    private fun <T> MutableLiveData<T>.forceRefresh() {
        this.postValue(value)
    }

    private suspend fun triggerUpdateWithDelay(duration: Duration) {
        if (isBlocked) {
            return
        }

        isBlocked = true
        scope.launch(Job() + Dispatchers.Default) {
            delay(duration)
            chartsDataList.forceRefresh()
            isBlocked = false
        }
    }

    init {
        // remove error
        // Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before calling Utils.convertDpToPixel(...). Otherwise conversion does not take place.
        chartsDataList = MutableLiveData(
            mutableListOf(
                EcgChartData_Test("Lead V1", 8, size),
                EcgChartData_Test("Lead V2", 4, size),
                EcgChartData_Test("Lead V3", 5, size),
                EcgChartData_Test("Lead V4", 6, size),
                EcgChartData_Test("Lead V5", 7, size),
                EcgChartData_Test("Lead V6", 1, size),
                EcgChartData_Test("Lead I", 2, size),
                EcgChartData_Test("Lead II", 3, size),
                EcgChartData_Test("Lead III", 9, size),
                EcgChartData_Test("Lead aVR", 10, size),
                EcgChartData_Test("Lead aVL", 11, size),
                EcgChartData_Test("Lead aVF", 12, size)
            )
        )

        Timber.i("created")
    }

    fun dbClean() {
        scope.async(Job() + Dispatchers.IO) {
            database.leadDao.clear()
            chartsDataList.value?.forEach {
                it.clean()
            }
            //chartsDataList.forceRefresh()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                leadsRepository.getData()?.collect { input ->
                    input.data.forEach {
                        chartsDataList.value?.get(it.lead)?.update(it.x, it.y)
                    }

                    launch(Job() + Dispatchers.IO) {
                        database.leadDao.insert(input.data)
                    }

                    //triggerUpdateWithDelay(5.milliseconds)
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