package pl.nowak.bitly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import pl.nowak.bitly.databinding.EcgChartItemBinding
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AdapterChartList :
    ListAdapter<EcgChartData, AdapterChartList.ViewHolderSmallChart>(EcgChartDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolderSmallChart, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSmallChart {
        return ViewHolderSmallChart.from(parent)
    }

    class ViewHolderSmallChart private constructor(private val binding: EcgChartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val ecgLabel: TextView = binding.ecgLineLabel
        private val ecgChart: ViewSmallChart = binding.ecgChart

        fun bind(item: EcgChartData) {
            if (item.chart.entryCount == 0) {
                item.chart = ecgChart.defaultDataSettings(item.chart)
                ecgChart.defaultAxisSettings()
            }

            ecgLabel.text = item.label
            ecgChart.data = LineData(item.chart)
        }

        private fun setData(count: Int, range: Double): BarData {
            // now in hours
            val now: Long = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis())
            val values: ArrayList<BarEntry> = ArrayList()

            // count = hours
            val to = (now + count).toFloat()

            // increment by 1 hour
            var x = now.toFloat()
            while (x < to) {
                val y: Double = Random.nextDouble(range, 50.0)
                values.add(BarEntry(x, y.toFloat())) // add one entry per hour
                x++
            }

            // create a dataset and give it a type
            val set1 = BarDataSet(values, "DataSet 1")
            set1.axisDependency = YAxis.AxisDependency.LEFT
            set1.color = ColorTemplate.getHoloBlue()
            set1.valueTextColor = ColorTemplate.getHoloBlue()
            set1.setDrawValues(false)
            set1.highLightColor = Color.rgb(244, 117, 117)

            // create a data object with the data sets
            val data = BarData(set1)
            data.setValueTextColor(Color.WHITE)
            data.setValueTextSize(9f)

            // set data
            return data
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolderSmallChart {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = EcgChartItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolderSmallChart(binding)
            }
        }
    }
}

class EcgChartDiffCallback : DiffUtil.ItemCallback<EcgChartData>() {

    private fun getLastTimestamp(item: LineDataSet): Float {
        return if (item.entryCount > 0) {
            val lastIndex = item.entryCount - 1
            item.getEntryForIndex(lastIndex).x
        } else {
            Float.NaN
        }
    }

    override fun areItemsTheSame(oldItem: EcgChartData, newItem: EcgChartData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EcgChartData, newItem: EcgChartData): Boolean {
        val oldChart = oldItem.chart
        val newChart = newItem.chart

        // init situation where there is not full window set
        if (oldChart.entryCount < newChart.entryCount) {
            return true
        }

        // full window is set
        if (oldChart.entryCount > 0 && newChart.entryCount > 0) {
            return getLastTimestamp(oldChart) != getLastTimestamp(newChart)
        }

        return false
    }
}
