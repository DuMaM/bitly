package pl.nowak.bitly.stats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.nowak.bitly.databinding.ActivityStatsBinding

class StatsActivity : AppCompatActivity() {

    // view
    private lateinit var binding: ActivityStatsBinding

    // view charts - small
    private lateinit var mJitterView: SmallChartView
    private lateinit var mPingView: SmallChartView
    private lateinit var mTransferSpeed: SmallChartView

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