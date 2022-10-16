package pl.nowak.bitly.ecg

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pl.nowak.bitly.database.LeadDatabase
import pl.nowak.bitly.database.getDatabase
import pl.nowak.bitly.repository.EcgDataRepository
import timber.log.Timber

class EcgChartViewModel(application: Application) : AndroidViewModel(application) {
    var chartsDataList: MutableLiveData<List<EcgChartData>>
    var size = 12

    private val database: LeadDatabase = getDatabase(application)
    private val leadsRepository = EcgDataRepository(database)

    init {
        Timber.i("Charts Data View Model created")

        // remove error
        // Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before calling Utils.convertDpToPixel(...). Otherwise conversion does not take place.
        chartsDataList = MutableLiveData(
            listOf(
                EcgChartData("Lead V1", 1, size),
                EcgChartData("Lead V2", 2, size),
                EcgChartData("Lead V3", 3, size),
                EcgChartData("Lead V4", 4, size),
                EcgChartData("Lead V5", 5, size),
                EcgChartData("Lead V6", 6, size),
                EcgChartData("Lead I", 7, size),
                EcgChartData("Lead II", 8, size),
                EcgChartData("Lead III", 9, size),
                EcgChartData("Lead aVR", 10, size),
                EcgChartData("Lead aVL", 11, size),
                EcgChartData("Lead aVF", 12, size)
            )
        )

        viewModelScope.launch {
            leadsRepository.refreshData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("Charts data Model destroyed")
    }
}

