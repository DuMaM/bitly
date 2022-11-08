package pl.nowak.bitly.ecg

import androidx.recyclerview.widget.DiffUtil

class EcgChartDiffCallback : DiffUtil.ItemCallback<EcgChartData>() {

    override fun areItemsTheSame(oldItem: EcgChartData, newItem: EcgChartData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EcgChartData, newItem: EcgChartData): Boolean {
        return !oldItem.newVal
    }
}
