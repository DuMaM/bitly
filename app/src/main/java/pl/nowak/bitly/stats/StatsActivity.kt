package pl.nowak.bitly.stats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.XYPlot
import pl.nowak.bitly.databinding.ActivityStatsBinding

class StatsActivity : AppCompatActivity() {

    // view
    private lateinit var binding: ActivityStatsBinding

    // view charts - small
    private lateinit var mJitterView: XYPlot
    private lateinit var mPingView: XYPlot
    private lateinit var mTransferSpeed: XYPlot

    override fun onCreate(savedInstanceState: Bundle?) {
        // init activity
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init mini graphs
        mJitterView = binding.chartJitterView
        mPingView = binding.chartPingView
        mTransferSpeed = binding.chartTransferSpeedView
    }
}