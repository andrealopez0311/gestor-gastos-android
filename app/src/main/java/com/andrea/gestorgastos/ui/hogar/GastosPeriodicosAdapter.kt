package com.andrea.gestorgastos.ui.hogar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemGastoPeriodicoBinding

class GastosPeriodicosAdapter(
    private val onPagar: (Int) -> Unit,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<GastosPeriodicosAdapter.PeriodicoViewHolder>() {

    private var gastos = listOf<Map<String, Any>>()

    fun actualizarGastos(nuevos: List<Map<String, Any>>) {
        gastos = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodicoViewHolder {
        val binding = ItemGastoPeriodicoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PeriodicoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PeriodicoViewHolder, position: Int) {
        holder.bind(gastos[position])
    }

    override fun getItemCount() = gastos.size

    inner class PeriodicoViewHolder(
        private val binding: ItemGastoPeriodicoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gasto: Map<String, Any>) {
            val id = (gasto["id"] as? Double)?.toInt() ?: 0
            val nombre = gasto["nombre"] as? String ?: ""
            val importe = gasto["importe"] as? Double ?: 0.0
            val frecuencia = (gasto["frecuencia"] as? Double)?.toInt() ?: 1
            val reservaMensual = gasto["reserva_mensual"] as? Double ?: 0.0
            val proximoPago = gasto["proximo_pago"] as? String
            val acumulado = gasto["acumulado"] as? Double ?: 0.0
            val diasRestantes = (gasto["dias_restantes"] as? Double)?.toInt()
            val alerta = gasto["alerta"] as? Boolean ?: false
            val listo = gasto["listo"] as? Boolean ?: false

            binding.tvNombrePeriodico.text = nombre
            binding.tvImportePeriodico.text = "%.2f €".format(importe)
            binding.tvReservaMensual.text = "%.2f €/mes".format(reservaMensual)

            val frecuenciaTexto = when (frecuencia) {
                1 -> "Mensual"
                2 -> "Bimestral"
                3 -> "Trimestral"
                6 -> "Semestral"
                12 -> "Anual"
                else -> "Cada $frecuencia meses"
            }
            binding.tvFrecuencia.text = frecuenciaTexto

            if (proximoPago != null) {
                binding.tvProximoPago.text = "Próximo pago: $proximoPago"
            } else {
                binding.tvProximoPago.text = "Sin fecha de pago"
            }

            val progreso = if (importe > 0) ((acumulado / importe) * 100).toInt() else 0
            binding.progressPeriodico.progress = progreso.coerceAtMost(100)

            binding.tvAcumuladoPeriodico.text = "Acumulado: %.2f € / %.2f €".format(acumulado, importe)

            if (alerta && diasRestantes != null) {
                binding.tvAlertaPeriodico.visibility = View.VISIBLE
                binding.tvAlertaPeriodico.text = "⚠️ ${diasRestantes}d"
            } else {
                binding.tvAlertaPeriodico.visibility = View.GONE
            }

            if (listo) {
                binding.progressPeriodico.progressTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#43A047")
                    )
            } else {
                binding.progressPeriodico.progressTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#7B1FA2")
                    )
            }

            binding.btnPagarPeriodico.setOnClickListener { onPagar(id) }
            binding.btnEliminarPeriodico.setOnClickListener { onEliminar(id) }
        }
    }
}