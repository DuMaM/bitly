package pl.nowak.bitly.ecg

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.androidplot.Plot
import com.androidplot.util.Redrawer
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.XYPlot
import pl.nowak.bitly.databinding.EcgChartItemTestBinding


class EcgChartListAdapterTest : ListAdapter<EcgChartData_Test, EcgChartListAdapterTest.EcgChartViewTestHolder>(EcgChartDiffCallback_Test()) {

    override fun onBindViewHolder(holder: EcgChartViewTestHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EcgChartViewTestHolder {
        return EcgChartViewTestHolder.from(parent)
    }

    class EcgChartViewTestHolder private constructor(binding: EcgChartItemTestBinding, val redrawer: Redrawer) : RecyclerView.ViewHolder(
        binding
            .root
    ) {
        private val ecgLabel: TextView = binding.ecgLineLabel
        private val ecgChart: XYPlot = binding.ecgChart

        fun bind(item: EcgChartData_Test) {
            ecgLabel.text = item.label

            if (ecgChart.registry.isEmpty) {
                // add data series only once
                val dataFormat = LineAndPointFormatter(Color.RED, Color.RED, null, null)
                ecgChart.addSeries(item, dataFormat)
            }
        }

        companion object {
            fun from(parent: ViewGroup): EcgChartViewTestHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = EcgChartItemTestBinding.inflate(layoutInflater, parent, false)

                val plot: XYPlot = binding.ecgChart
                plot.renderMode = Plot.RenderMode.USE_BACKGROUND_THREAD

                //plot.setRangeBoundaries(-200, 200, BoundaryMode.AUTO)
                plot.setDomainBoundaries(0, 2000, BoundaryMode.AUTO)
                plot.legend.isVisible = false

                // reduce the number of range labels
                plot.linesPerRangeLabel = 3
                val redrawer = Redrawer(plot, 10f, true)
                // PanZoom.attach(plot)

                return EcgChartViewTestHolder(binding, redrawer)
            }
        }
    }
}
