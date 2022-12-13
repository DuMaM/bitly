package pl.nowak.bitly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.utils.Utils
import pl.nowak.bitly.databinding.ActivityMainBinding
import pl.nowak.bitly.ecg.EcgChartViewModel
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    // view
    private lateinit var binding: ActivityMainBinding

    // viewModels
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val viewModelCharts: EcgChartViewModel by lazy {
        ViewModelProvider(this)[EcgChartViewModel::class.java]
    }

    // ecg recycle view
    private lateinit var ecgChartsView: RecyclerView

    // service
    private val multiplePermissions: Int = 100
    private var permissionList = arrayOf(
        Manifest.permission.BLUETOOTH_ADMIN,
        // Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        // Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.mainViewModel = viewModel       // same as in previous line
        binding.ecgViewModel = viewModelCharts  // same as in previous line
        binding.lifecycleOwner = this           // allow data to update actions
        setContentView(binding.root)            // show this main activity
        Timber.i("binging set")

        /**
         * This line is important one, and removes bellow error
         * `Utils NOT INITIALIZED. You need to call Utils.init(...)
         * at least once before calling Utils.convertDpToPixel(...).
         * Otherwise conversion does not take place.
         */
        Utils.init(this)

        val adapter = EcgChartListAdapter()
        ecgChartsView = binding.recycleChartList
        ecgChartsView.adapter = adapter
        ecgChartsView.layoutManager = LinearLayoutManager(this)
        viewModelCharts.chartsDataList.observe(this) {
            it?.let {
                adapter.submitList(it.toMutableList())
                // Timber.i("new data received")
            }
        }

        Timber.i("ecgCharts views are set and app is observing live data")
        Timber.i("init finished")
    }

    override fun onResume() {
        super.onResume()
        checkBlePermission()
        Timber.i("resumed")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val checkGranted = checkGranted@{
            if (grantResults.isEmpty()) {
                Timber.e("Not granted access")
                return@checkGranted
            }

            grantResults.forEachIndexed { index, it ->
                if (it == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this@MainActivity, permissions[index]) == PackageManager.PERMISSION_GRANTED
                ) {
                    Timber.i("${permissions[index]} Granted access")
                } else {
                    Toast.makeText(applicationContext, "Please grant us ${permissions[index]}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        when (requestCode) {
            multiplePermissions -> checkGranted()
        }
        Timber.i("All access granted")
    }

    // function to check permissions
    private fun checkBlePermission() {

        val permRequest = {
            Timber.d("Requesting permissions for app BLE connect, scan, location, admin")
            requestPermissions(permissionList, multiplePermissions)
        }

        permissionList.forEachIndexed { _, item: String ->
            if (ContextCompat.checkSelfPermission(this@MainActivity, item) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, item)) {
                    Toast.makeText(applicationContext, "Please allow for this permission $item", Toast.LENGTH_SHORT).show()
                } else {
                    permRequest()
                }
            }
        }
    }
}
