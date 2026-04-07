package com.andrea.gestorgastos.ui.hogar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityFondoPeriodicosBinding
import com.andrea.gestorgastos.model.CuotaRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class FondoPeriodicosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFondoPeriodicosBinding
    private lateinit var adapter: CuotasAdapter
    private var gastosPeriodicos = listOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFondoPeriodicosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CuotasAdapter(
            onPagar = { id -> confirmarPago(id) },
            onEliminar = { id -> eliminarCuota(id) }
        )
        binding.recyclerCuotas.layoutManager = LinearLayoutManager(this)
        binding.recyclerCuotas.adapter = adapter

        cargarDatos()

        binding.btnVolverFondo.setOnClickListener { finish() }
        binding.btnAcumularMes.setOnClickListener { acumularMes() }
        binding.btnAnadirCuota.setOnClickListener { mostrarDialogoAnadirCuota() }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getFondoPeriodicos()
                if (response.isSuccessful) {
                    val body = response.body()
                    val acumulado = body?.get("acumulado") as? Double ?: 0.0
                    val reserva = body?.get("reserva_mensual") as? Double ?: 0.0
                    val cuotas = body?.get("cuotas_pendientes") as? List<Map<String, Any>> ?: emptyList()

                    binding.tvAcumuladoFondo.text = "%.2f €".format(acumulado)
                    binding.tvReservaFondo.text = "%.2f €/mes".format(reserva)
                    adapter.actualizarCuotas(cuotas)
                }

                // Cargar gastos periódicos para el diálogo de cuotas
                val periodicosResponse = RetrofitClient.api.getGastosPeriodicos()
                if (periodicosResponse.isSuccessful) {
                    val body = periodicosResponse.body()
                    gastosPeriodicos = body?.get("gastos") as? List<Map<String, Any>> ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FondoPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun acumularMes() {
        AlertDialog.Builder(this)
            .setTitle("Acumular reserva")
            .setMessage("¿Quieres añadir la reserva mensual de todos los gastos periódicos al fondo?")
            .setPositiveButton("Acumular") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.acumularMes()
                        if (response.isSuccessful) {
                            val cantidad = response.body()?.get("cantidad") as? Double ?: 0.0
                            Toast.makeText(
                                this@FondoPeriodicosActivity,
                                "%.2f € añadidos al fondo ✅".format(cantidad),
                                Toast.LENGTH_SHORT
                            ).show()
                            cargarDatos()
                        } else {
                            val error = response.errorBody()?.string() ?: "Error"
                            Toast.makeText(this@FondoPeriodicosActivity, error, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@FondoPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAnadirCuota() {
        if (gastosPeriodicos.isEmpty()) {
            Toast.makeText(this, "No tienes gastos periódicos creados", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = gastosPeriodicos.map { it["nombre"] as? String ?: "" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("¿A qué gasto pertenece?")
            .setItems(nombres) { _, which ->
                val gastoId = (gastosPeriodicos[which]["id"] as? Double)?.toInt() ?: 0
                mostrarDialogoCuota(gastoId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCuota(gastoId: Int) {
        val importeInput = android.widget.EditText(this)
        importeInput.hint = "Importe de la cuota en €"
        importeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val fechaInput = android.widget.EditText(this)
        fechaInput.hint = "Fecha de pago (ej: 2026-05-15)"

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(importeInput)
        layout.addView(fechaInput)

        AlertDialog.Builder(this)
            .setTitle("Añadir cuota irregular")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val importe = importeInput.text.toString().toDoubleOrNull()
                val fecha = fechaInput.text.toString().trim()
                if (importe == null || importe <= 0) {
                    Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (fecha.isEmpty()) {
                    Toast.makeText(this, "Introduce una fecha", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                crearCuota(gastoId, importe, fecha)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearCuota(gastoId: Int, importe: Double, fecha: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearCuota(
                    CuotaRequest(gastoId, importe, fecha)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@FondoPeriodicosActivity, "Cuota añadida ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@FondoPeriodicosActivity, "Error al crear", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FondoPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarPago(cuotaId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Pagar cuota")
            .setMessage("¿Confirmas el pago? Se descontará del fondo común de periódicos.")
            .setPositiveButton("Pagar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.pagarCuota(cuotaId)
                        if (response.isSuccessful) {
                            Toast.makeText(this@FondoPeriodicosActivity, "Cuota pagada ✅", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        } else {
                            val error = response.errorBody()?.string() ?: "Error"
                            Toast.makeText(this@FondoPeriodicosActivity, error, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@FondoPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCuota(cuotaId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuota")
            .setMessage("¿Seguro que quieres eliminar esta cuota?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarCuota(cuotaId)
                        if (response.isSuccessful) {
                            Toast.makeText(this@FondoPeriodicosActivity, "Cuota eliminada", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@FondoPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}