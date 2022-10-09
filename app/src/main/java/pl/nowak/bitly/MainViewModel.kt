package pl.nowak.bitly

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

class MainViewModel : ViewModel() {
    private val _testStatus = MutableLiveData("DISCONNECTED")
    val testStatus: LiveData<String>
        get() = _testStatus

    fun updateConnectionStatus(text: String) {
        _testStatus.value = text.uppercase()
    }

    init {
        Timber.i("Main App View Model created")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("Main App View Model destroyed")
    }
}
