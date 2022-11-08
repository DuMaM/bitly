package pl.nowak.bitly

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

        fun bind(item: EcgChartData) {
            ecgLabel.text = item.label
            ecgChart.addEntries(item.lineDataRestricted)
            item.newVal = false
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
