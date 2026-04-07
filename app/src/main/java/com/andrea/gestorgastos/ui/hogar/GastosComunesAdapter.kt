package com.andrea.gestorgastos.ui.hogar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemGastoComunBinding

class GastosComunesAdapter(
    private val onEliminar: (Int) -> Unit,
    private val onEditar: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<GastosComunesAdapter.GastoComunViewHolder>() {

    private var gastos = listOf<Map<String, Any>>()

    fun actualizarGastos(nuevos: List<Map<String, Any>>) {
        gastos = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoComunViewHolder {
        val binding = ItemGastoComunBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GastoComunViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GastoComunViewHolder, position: Int) {
        holder.bind(gastos[position])
    }

    override fun getItemCount() = gastos.size

    inner class GastoComunViewHolder(
        private val binding: ItemGastoComunBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gasto: Map<String, Any>) {
            val id = (gasto["id"] as? Double)?.toInt() ?: 0
            val categoria = gasto["categoria"] as? String ?: ""
            val descripcion = gasto["descripcion"] as? String ?: ""
            val importe = gasto["importe"] as? Double ?: 0.0
            val fecha = gasto["fecha"] as? String ?: ""
            val anadidoPor = gasto["añadido_por"] as? String ?: ""

            binding.tvCategoriaComun.text = categoria
            binding.tvDescripcionComun.text = descripcion.ifEmpty { "Sin descripción" }
            binding.tvImporteComun.text = "%.2f €".format(importe)
            binding.tvFechaComun.text = fecha
            binding.tvAnadidoPor.text = "Añadido por $anadidoPor"

            binding.btnEliminarComun.setOnClickListener { onEliminar(id) }
            binding.btnEditarComun.setOnClickListener { onEditar(gasto) }
        }
    }
}