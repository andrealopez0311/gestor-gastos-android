package com.andrea.gestorgastos.ui.hogar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemAhorroBinding

class AhorroAdapter(
    private val onAnadir: (Int) -> Unit,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<AhorroAdapter.AhorroViewHolder>() {

    private var ahorros = listOf<Map<String, Any>>()

    fun actualizarAhorros(nuevos: List<Map<String, Any>>) {
        ahorros = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AhorroViewHolder {
        val binding = ItemAhorroBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AhorroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AhorroViewHolder, position: Int) {
        holder.bind(ahorros[position])
    }

    override fun getItemCount() = ahorros.size

    inner class AhorroViewHolder(
        private val binding: ItemAhorroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ahorro: Map<String, Any>) {
            val id = (ahorro["id"] as? Double)?.toInt() ?: 0
            val nombre = ahorro["nombre"] as? String ?: ""
            val meta = ahorro["meta"] as? Double
            val acumulado = ahorro["acumulado"] as? Double ?: 0.0
            val progreso = (ahorro["progreso"] as? Double)?.toInt() ?: 0

            binding.tvNombreAhorro.text = nombre
            binding.tvAcumuladoAhorro.text = "%.2f €".format(acumulado)
            binding.progressAhorro.progress = progreso

            if (meta != null) {
                binding.tvMetaAhorro.text = "Meta: %.2f € · %d%%".format(meta, progreso)
            } else {
                binding.tvMetaAhorro.text = "Sin meta definida · %.2f € ahorrados".format(acumulado)
            }

            binding.btnAnadirAhorro.setOnClickListener {
                onAnadir(id)
            }

            binding.btnEliminarAhorro.setOnClickListener {
                onEliminar(id)
            }
        }
    }
}