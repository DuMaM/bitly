package pl.nowak.bitly

import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import pl.nowak.bitly.database.getDatabase
import pl.nowak.bitly.repository.EcgDataRepository
import timber.log.Timber

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _testStatus = MutableLiveData("DISCONNECTED")
    val testStatus: LiveData<String>
        get() = _testStatus

    private val database = getDatabase(application)
    private val leadsRepository = EcgDataRepository(database, application)

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    fun disconnect() = leadsRepository.disconnect()

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    fun advertise() = leadsRepository.advertise()

    fun updateConnectionStatus(text: String) {
        _testStatus.value = text.uppercase()
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    override fun onCleared() {
        super.onCleared()
        disconnect()
        Timber.i("Main App View Model destroyed")
    }
}
