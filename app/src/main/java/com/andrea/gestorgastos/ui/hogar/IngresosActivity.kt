package com.andrea.gestorgastos.ui.hogar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.andrea.gestorgastos.databinding.ActivityIngresosBinding
import com.andrea.gestorgastos.model.IngresoRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class IngresosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIngresosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngresosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarDatos()

        binding.btnVolverIngresos.setOnClickListener { finish() }

        binding.btnRegistrarIngreso.setOnClickListener {
            mostrarDialogoIngreso()
        }

        binding.btnConfigurarPresupuesto.setOnClickListener {
            mostrarDialogoPresupuesto()
        }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getIngresosHogar()
                if (response.isSuccessful) {
                    val body = response.body()
                    val total = body?.get("total") as? Double ?: 0.0
                    val ingresos = body?.get("ingresos") as? List<Map<String, Any>> ?: emptyList()

                    binding.tvTotalIngresos.text = "%.2f €".format(total)

                    val texto = ingresos.joinToString("\n") { ingreso ->
                        val nombre = ingreso["nombre"] as? String ?: ""
                        val importe = ingreso["importe"] as? Double ?: 0.0
                        val desc = ingreso["descripcion"] as? String ?: ""
                        "• $nombre: %.2f € ($desc)".format(importe)
                    }
                    binding.tvDetalleIngresos.text = texto.ifEmpty { "Sin ingresos registrados este mes" }
                }

                val presupuestoResponse = RetrofitClient.api.getPresupuesto()
                if (presupuestoResponse.isSuccessful) {
                    val p = presupuestoResponse.body()
                    val ahorro = p?.get("porcentaje_ahorro") as? Double ?: 20.0
                    val comunes = p?.get("porcentaje_comunes") as? Double ?: 50.0
                    val personal = p?.get("porcentaje_personal") as? Double ?: 30.0
                    binding.tvPresupuesto.text = "Ahorro: ${ahorro.toInt()}% · Comunes: ${comunes.toInt()}% · Personal: ${personal.toInt()}%"
                }

            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoIngreso() {
        val input = android.widget.EditText(this)
        input.hint = "Tu sueldo o ingreso del mes"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val descInput = android.widget.EditText(this)
        descInput.hint = "Descripción (ej: Sueldo enero)"

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(input)
        layout.addView(descInput)

        AlertDialog.Builder(this)
            .setTitle("Registrar ingreso")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val importe = input.text.toString().toDoubleOrNull()
                if (importe == null || importe <= 0) {
                    Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                registrarIngreso(importe, descInput.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoPresupuesto() {
        val ahorroInput = android.widget.EditText(this)
        ahorroInput.hint = "% Ahorro (ej: 20)"
        ahorroInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val comunesInput = android.widget.EditText(this)
        comunesInput.hint = "% Gastos comunes (ej: 50)"
        comunesInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val personalInput = android.widget.EditText(this)
        personalInput.hint = "% Personal (ej: 30)"
        personalInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(ahorroInput)
        layout.addView(comunesInput)
        layout.addView(personalInput)

        AlertDialog.Builder(this)
            .setTitle("Configurar presupuesto (deben sumar 100%)")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val ahorro = ahorroInput.text.toString().toDoubleOrNull() ?: 0.0
                val comunes = comunesInput.text.toString().toDoubleOrNull() ?: 0.0
                val personal = personalInput.text.toString().toDoubleOrNull() ?: 0.0
                if (ahorro + comunes + personal != 100.0) {
                    Toast.makeText(this, "Los porcentajes deben sumar 100", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                configurarPresupuesto(ahorro, comunes, personal)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarIngreso(importe: Double, descripcion: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearIngreso(
                    IngresoRequest(importe, descripcion)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@IngresosActivity, "Ingreso registrado ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@IngresosActivity, "Error al registrar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarPresupuesto(ahorro: Double, comunes: Double, personal: Double) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearPresupuesto(
                    com.andrea.gestorgastos.model.PresupuestoRequest(ahorro, comunes, personal)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@IngresosActivity, "Presupuesto guardado ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@IngresosActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}