package com.andrea.gestorgastos.ui.hogar

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityGastosPeriodicosBinding
import com.andrea.gestorgastos.model.GastoPeriodicoRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class GastosPeriodicosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGastosPeriodicosBinding
    private lateinit var adapter: GastosPeriodicosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGastosPeriodicosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GastosPeriodicosAdapter(
            onPagar = { id -> mostrarDialogoPagar(id) },
            onEliminar = { id -> eliminarPeriodico(id) },
            onEditar = { gasto -> mostrarDialogoEditar(gasto) }
        )
        binding.recyclerPeriodicos.layoutManager = LinearLayoutManager(this)
        binding.recyclerPeriodicos.adapter = adapter

        cargarDatos()

        binding.btnVolverPeriodicos.setOnClickListener { finish() }
        binding.btnAgregarPeriodico.setOnClickListener { mostrarDialogoAgregar() }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getGastosPeriodicos()
                if (response.isSuccessful) {
                    val body = response.body()
                    val totalReserva = body?.get("total_reserva_mensual") as? Double ?: 0.0
                    val gastos = body?.get("gastos") as? List<Map<String, Any>> ?: emptyList()

                    binding.tvTotalReserva.text = "%.2f €/mes".format(totalReserva)
                    adapter.actualizarGastos(gastos)

                    // Comprobar alertas
                    val hayAlertas = gastos.any { it["alerta"] as? Boolean == true }
                    if (hayAlertas) {
                        binding.tvAlertaPeriodicos.visibility = View.VISIBLE
                        binding.tvAlertaPeriodicos.text = "⚠️ Hay pagos próximos en menos de 30 días"
                    } else {
                        binding.tvAlertaPeriodicos.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@GastosPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoAgregar() {
        val nombreInput = android.widget.EditText(this)
        nombreInput.hint = "Nombre (ej: Seguro hogar)"

        val importeInput = android.widget.EditText(this)
        importeInput.hint = "Importe total (ej: 360)"
        importeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val frecuenciaInput = android.widget.EditText(this)
        frecuenciaInput.hint = "Cada cuántos meses (ej: 12)"
        frecuenciaInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val fechaInput = android.widget.EditText(this)
        fechaInput.hint = "Próximo pago (ej: 2026-12-01)"

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(nombreInput)
        layout.addView(importeInput)
        layout.addView(frecuenciaInput)
        layout.addView(fechaInput)

        AlertDialog.Builder(this)
            .setTitle("Añadir gasto periódico")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = nombreInput.text.toString().trim()
                val importe = importeInput.text.toString().toDoubleOrNull()
                val frecuencia = frecuenciaInput.text.toString().toIntOrNull()
                val fecha = fechaInput.text.toString().trim().ifEmpty { null }

                if (nombre.isEmpty() || importe == null || frecuencia == null) {
                    Toast.makeText(this, "Rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                crearPeriodico(nombre, importe, frecuencia, fecha)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoPagar(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Registrar pago")
            .setMessage("¿Confirmas que has pagado este gasto? Se reiniciará el contador y se calculará el próximo pago.")
            .setPositiveButton("Confirmar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.registrarPago(id)
                        if (response.isSuccessful) {
                            Toast.makeText(this@GastosPeriodicosActivity, "Pago registrado ✅", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@GastosPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearPeriodico(nombre: String, importe: Double, frecuencia: Int, fecha: String?) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearGastoPeriodico(
                    GastoPeriodicoRequest(nombre, importe, frecuencia, fecha)
                )
                if (response.isSuccessful) {
                    val reserva = response.body()?.get("reserva_mensual") as? Double ?: 0.0
                    Toast.makeText(
                        this@GastosPeriodicosActivity,
                        "Creado ✅ Reserva: %.2f €/mes".format(reserva),
                        Toast.LENGTH_LONG
                    ).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@GastosPeriodicosActivity, "Error al crear", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GastosPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarPeriodico(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar gasto periódico")
            .setMessage("¿Seguro que quieres eliminar este gasto periódico?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarGastoPeriodico(id)
                        if (response.isSuccessful) {
                            Toast.makeText(this@GastosPeriodicosActivity, "Eliminado", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@GastosPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun mostrarDialogoEditar(gasto: Map<String, Any>) {
        val id = (gasto["id"] as? Double)?.toInt() ?: 0
        val nombreActual = gasto["nombre"] as? String ?: ""
        val importeActual = gasto["importe"] as? Double ?: 0.0
        val frecuenciaActual = (gasto["frecuencia"] as? Double)?.toInt() ?: 1
        val proximoActual = gasto["proximo_pago"] as? String ?: ""

        val nombreInput = android.widget.EditText(this)
        nombreInput.hint = "Nombre"
        nombreInput.setText(nombreActual)

        val importeInput = android.widget.EditText(this)
        importeInput.hint = "Importe en €"
        importeInput.setText(importeActual.toString())
        importeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val frecuenciaInput = android.widget.EditText(this)
        frecuenciaInput.hint = "Frecuencia en meses"
        frecuenciaInput.setText(frecuenciaActual.toString())
        frecuenciaInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val fechaInput = android.widget.EditText(this)
        fechaInput.hint = "Próximo pago (YYYY-MM-DD)"
        fechaInput.setText(proximoActual)

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(nombreInput)
        layout.addView(importeInput)
        layout.addView(frecuenciaInput)
        layout.addView(fechaInput)

        AlertDialog.Builder(this)
            .setTitle("Editar gasto periódico")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val importe = importeInput.text.toString().toDoubleOrNull()
                val frecuencia = frecuenciaInput.text.toString().toIntOrNull()
                if (importe == null || importe <= 0) {
                    Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.editarGastoPeriodico(
                            id,
                            com.andrea.gestorgastos.model.EditarGastoPeriodicoRequest(
                                nombre = nombreInput.text.toString().ifEmpty { null },
                                importe = importe,
                                frecuencia = frecuencia,
                                proximo_pago = fechaInput.text.toString().ifEmpty { null }
                            )
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(this@GastosPeriodicosActivity, "Gasto actualizado ✅", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@GastosPeriodicosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}