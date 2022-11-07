package pl.nowak.bitly

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import pl.nowak.bitly.databinding.EcgChartItemBinding
import pl.nowak.bitly.ecg.EcgChart
import pl.nowak.bitly.ecg.EcgChartData
import pl.nowak.bitly.ecg.EcgChartDiffCallback


class EcgChartListAdapter : ListAdapter<EcgChartData, EcgChartListAdapter.EcgChartViewHolder>(EcgChartDiffCallback()) {

    override fun onBindViewHolder(holder: EcgChartViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EcgChartViewHolder {
        return EcgChartViewHolder.from(parent)
    }

    class EcgChartViewHolder private constructor(private val binding: EcgChartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val ecgLabel: TextView = binding.ecgLineLabel
        private val ecgChart: EcgChart = binding.ecgChart
        private val setIndex = 0

        fun bind(item: EcgChartData) {
            ecgLabel.text = item.label
            addEntries(item)
        }

        private fun addEntries(item: EcgChartData) {
            ecgChart.apply {
                if (data == null) {
                    var lineSet = LineDataSet(emptyList<Entry>().toMutableList(), "data")
                    data = LineData(lineSet)
                }

                item.lineDataRestricted.forEach {
                    data.addEntry(it, setIndex)
                }

                if (item.lineDataRestricted.size > 1) {
                    xAxis.axisMaximum = item.lineDataRestricted.last().x
                    xAxis.axisMinimum = item.lineDataRestricted.first().x
                }

                // notify data has been updates
                data.notifyDataChanged()

                // let the chart know it's data has changed
                notifyDataSetChanged()

                // limit the number of visible entries
                // ecgChart.setVisibleXRangeMaximum(120f)
                // ecgChart.setVisibleYRange(30, AxisDependency.LEFT);

                // move to the latest entry
                //moveViewToX(item.getLastTimestamp())

                // this automatically refreshes the chart (calls invalidate())
                // chart.moveViewTo(data.getXValCount()-7, 55f,
                // AxisDependency.LEFT);
                invalidate()
            }
        }

        companion object {
            fun from(parent: ViewGroup): EcgChartViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = EcgChartItemBinding.inflate(layoutInflater, parent, false)
                return EcgChartViewHolder(binding)
            }
        }
    }
}
