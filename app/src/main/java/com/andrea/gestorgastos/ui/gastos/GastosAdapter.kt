package com.andrea.gestorgastos.ui.gastos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemGastoBinding
import com.andrea.gestorgastos.model.Gasto

class GastosAdapter(
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<GastosAdapter.GastoViewHolder>() {

    private var gastos = listOf<Gasto>()

    fun actualizarGastos(nuevosGastos: List<Gasto>) {
        gastos = nuevosGastos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val binding = ItemGastoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GastoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        holder.bind(gastos[position])
    }

    override fun getItemCount(): Int = gastos.size

    inner class GastoViewHolder(
        private val binding: ItemGastoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gasto: Gasto) {
            binding.tvCategoria.text = gasto.categoria
            binding.tvDescripcion.text = gasto.descripcion.ifEmpty { "Sin descripción" }
            binding.tvImporte.text = "%.2f €".format(gasto.importe)
            binding.tvFecha.text = gasto.fecha

            binding.btnEliminar.setOnClickListener {
                onEliminar(gasto.id)
            }
        }
    }
}