package com.andrea.gestorgastos.ui.hogar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemIngresoBinding

class IngresosAdapter(
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<IngresosAdapter.IngresoViewHolder>() {

    private var ingresos = listOf<Map<String, Any>>()

    fun actualizarIngresos(nuevos: List<Map<String, Any>>) {
        ingresos = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngresoViewHolder {
        val binding = ItemIngresoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return IngresoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngresoViewHolder, position: Int) {
        holder.bind(ingresos[position])
    }

    override fun getItemCount() = ingresos.size

    inner class IngresoViewHolder(
        private val binding: ItemIngresoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ingreso: Map<String, Any>) {
            val id = (ingreso["id"] as? Double)?.toInt() ?: 0
            val importe = ingreso["importe"] as? Double ?: 0.0
            val descripcion = ingreso["descripcion"] as? String ?: ""
            val fuente = ingreso["fuente"] as? String ?: ""
            val nombre = ingreso["nombre"] as? String ?: ""

            binding.tvFuenteIngreso.text = fuente.ifEmpty { "Ingreso" }
            binding.tvNombreIngreso.text = nombre
            binding.tvDescripcionIngreso.text = descripcion.ifEmpty { "" }
            binding.tvImporteIngreso.text = "%.2f €".format(importe)

            binding.btnEliminarIngreso.setOnClickListener {
                onEliminar(id)
            }
        }
    }
}