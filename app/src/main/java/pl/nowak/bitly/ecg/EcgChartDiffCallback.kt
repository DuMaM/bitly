package pl.nowak.bitly.ecg

import androidx.recyclerview.widget.DiffUtil

class EcgChartDiffCallback : DiffUtil.ItemCallback<EcgChartData>() {

    override fun areItemsTheSame(oldItem: EcgChartData, newItem: EcgChartData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EcgChartData, newItem: EcgChartData): Boolean {
        return if (oldItem.lineDataRestricted.size < newItem.lineDataRestricted.size) {
            // init situation where there is not full window set
            false
        } else {
            // other cases where we are comparing indexes
            oldItem.getLastTimestamp() == newItem.getLastTimestamp()
        }
    }
}
