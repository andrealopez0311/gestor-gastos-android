package com.andrea.gestorgastos.ui.gastos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemAhorroPersonalBinding

class AhorroPersonalAdapter(
    private val onAnadir: (Int) -> Unit,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<AhorroPersonalAdapter.AhorroPersonalViewHolder>() {

    private var fondos = listOf<Map<String, Any>>()

    fun actualizarFondos(nuevos: List<Map<String, Any>>) {
        fondos = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AhorroPersonalViewHolder {
        val binding = ItemAhorroPersonalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AhorroPersonalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AhorroPersonalViewHolder, position: Int) {
        holder.bind(fondos[position])
    }

    override fun getItemCount() = fondos.size

    inner class AhorroPersonalViewHolder(
        private val binding: ItemAhorroPersonalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fondo: Map<String, Any>) {
            val id = (fondo["id"] as? Double)?.toInt() ?: 0
            val nombre = fondo["nombre"] as? String ?: ""
            val meta = fondo["meta"] as? Double
            val acumulado = fondo["acumulado"] as? Double ?: 0.0
            val progreso = (fondo["progreso"] as? Double)?.toInt() ?: 0

            binding.tvNombreAhorroPersonal.text = nombre
            binding.tvAcumuladoAhorroPersonal.text = "%.2f €".format(acumulado)
            binding.progressAhorroPersonal.progress = progreso

            if (meta != null) {
                binding.tvMetaAhorroPersonal.text = "Meta: %.2f € · %d%%".format(meta, progreso)
            } else {
                binding.tvMetaAhorroPersonal.text = "Sin meta · %.2f € ahorrados".format(acumulado)
            }

            binding.btnAnadirAhorroPersonal.setOnClickListener { onAnadir(id) }
            binding.btnEliminarAhorroPersonal.setOnClickListener { onEliminar(id) }
        }
    }
}