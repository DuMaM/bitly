package pl.nowak.bitly

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.nowak.bitly.databinding.ActivityStatsBinding

class ActivityStats : AppCompatActivity() {

    // view
    private lateinit var binding: ActivityStatsBinding

    // view charts - small
    private lateinit var mJitterView: SmallChart
    private lateinit var mPingView: SmallChart
    private lateinit var mTransferSpeed: SmallChart

    override fun onCreate(savedInstanceState: Bundle?) {
        // init activity
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init mini graphs
        mJitterView = binding.chartJitterView
        mPingView = binding.chartPingView
        mTransferSpeed = binding.chartTransferSpeedView
        mJitterView.default()
        mPingView.default()
        mTransferSpeed.default()
    }
}