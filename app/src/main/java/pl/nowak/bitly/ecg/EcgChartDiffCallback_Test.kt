package pl.nowak.bitly.ecg

import androidx.recyclerview.widget.DiffUtil

class EcgChartDiffCallback_Test : DiffUtil.ItemCallback<EcgChartData_Test>() {

    override fun areItemsTheSame(oldItem: EcgChartData_Test, newItem: EcgChartData_Test): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EcgChartData_Test, newItem: EcgChartData_Test): Boolean {
        return !oldItem.newVal
    }
}
