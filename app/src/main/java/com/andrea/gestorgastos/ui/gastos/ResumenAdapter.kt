package com.andrea.gestorgastos.ui.gastos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemResumenBinding

class ResumenAdapter : RecyclerView.Adapter<ResumenAdapter.ResumenViewHolder>() {

    private var items = listOf<Map<String, Any>>()

    fun actualizarItems(nuevosItems: List<Map<String, Any>>) {
        items = nuevosItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResumenViewHolder {
        val binding = ItemResumenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ResumenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResumenViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ResumenViewHolder(
        private val binding: ItemResumenBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Map<String, Any>) {
            val categoria = item["categoria"] as? String ?: ""
            val total = item["total"] as? Double ?: 0.0
            val color = item["color"] as? String ?: "#1E88E5"

            binding.tvCategoriaResumen.text = categoria
            binding.tvTotalCategoria.text = "%.2f €".format(total)

            try {
                binding.viewColor.setBackgroundColor(Color.parseColor(color))
            } catch (e: Exception) {
                binding.viewColor.setBackgroundColor(Color.parseColor("#1E88E5"))
            }
        }
    }
}