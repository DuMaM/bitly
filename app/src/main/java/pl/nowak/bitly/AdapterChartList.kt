package pl.nowak.bitly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterChartList : RecyclerView.Adapter<AdapterChartList.ViewHolderSmallChart>() {
    var charts = mutableListOf<Float>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = charts.size
    override fun onBindViewHolder(holder: ViewHolderSmallChart, position: Int) {
        val item = charts[position]
        holder.ecgChart.updateData(item, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSmallChart {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.ecg_chart_item, parent, false)
        return ViewHolderSmallChart(view)
    }


    class ViewHolderSmallChart(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ecgLabel: TextView = itemView.findViewById(R.id.ecgLineLabel)
        val ecgChart: ViewSmallChart = itemView.findViewById(R.id.ecgChart)
    }
}