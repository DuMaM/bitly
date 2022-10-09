package pl.nowak.bitly

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

class EcgChartViewModel : ViewModel() {
    var chartsDataList: MutableLiveData<List<EcgChartData>>

    init {
        Timber.i("Charts Data View Model created")

        // remove error
        // Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before calling Utils.convertDpToPixel(...). Otherwise conversion does not take place.
        chartsDataList = MutableLiveData(
            listOf(
                EcgChartData("Lead V1", 1),
                EcgChartData("Lead V2", 2),
                EcgChartData("Lead V3", 3),
                EcgChartData("Lead V4", 4),
                EcgChartData("Lead V5", 5),
                EcgChartData("Lead V6", 6),
                EcgChartData("Lead I", 7),
                EcgChartData("Lead II", 8),
                EcgChartData("Lead III", 9),
                EcgChartData("Lead aVR", 10),
                EcgChartData("Lead aVL", 11),
                EcgChartData("Lead aVF", 12)
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("Charts data Model destroyed")
    }
}

