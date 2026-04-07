package com.andrea.gestorgastos.ui.hogar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemCuotaBinding

class CuotasAdapter(
    private val onPagar: (Int) -> Unit,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<CuotasAdapter.CuotaViewHolder>() {

    private var cuotas = listOf<Map<String, Any>>()

    fun actualizarCuotas(nuevas: List<Map<String, Any>>) {
        cuotas = nuevas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuotaViewHolder {
        val binding = ItemCuotaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CuotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CuotaViewHolder, position: Int) {
        holder.bind(cuotas[position])
    }

    override fun getItemCount() = cuotas.size

    inner class CuotaViewHolder(
        private val binding: ItemCuotaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cuota: Map<String, Any>) {
            val id = (cuota["id"] as? Double)?.toInt() ?: 0
            val nombre = cuota["nombre"] as? String ?: ""
            val importe = cuota["importe"] as? Double ?: 0.0
            val fechaPago = cuota["fecha_pago"] as? String ?: ""
            val diasRestantes = (cuota["dias_restantes"] as? Double)?.toInt()
            val alerta = cuota["alerta"] as? Boolean ?: false
            val cubierta = cuota["cubierta"] as? Boolean ?: false

            binding.tvNombreCuota.text = nombre
            binding.tvImporteCuota.text = "%.2f €".format(importe)
            binding.tvFechaCuota.text = "Vence: $fechaPago"

            if (alerta && diasRestantes != null) {
                binding.tvAlertaCuota.visibility = View.VISIBLE
                binding.tvAlertaCuota.text = "⚠️ Faltan $diasRestantes días"
            } else {
                binding.tvAlertaCuota.visibility = View.GONE
            }

            if (cubierta) {
                binding.tvCubiertaCuota.visibility = View.VISIBLE
                binding.tvCubiertaCuota.text = "✅ Fondo suficiente"
            } else {
                binding.tvCubiertaCuota.visibility = View.GONE
            }

            binding.btnPagarCuota.setOnClickListener { onPagar(id) }
            binding.btnEliminarCuota.setOnClickListener { onEliminar(id) }
        }
    }
}