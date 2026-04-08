package com.andrea.gestorgastos.ui.hogar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityIngresosBinding
import com.andrea.gestorgastos.model.IngresoRequest
import com.andrea.gestorgastos.model.PresupuestoRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class IngresosActivity : AppCompatActivity() {

    private var montoAhorrosPendiente: Double = 0.0
    private lateinit var binding: ActivityIngresosBinding
    private lateinit var adapter: IngresosAdapter

    companion object {
        const val REQUEST_CREAR_FONDO = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngresosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = IngresosAdapter { id -> eliminarIngreso(id) }
        binding.recyclerIngresos.layoutManager = LinearLayoutManager(this)
        binding.recyclerIngresos.adapter = adapter

        cargarDatos()

        binding.btnVolverIngresos.setOnClickListener { finish() }
        binding.btnRegistrarIngreso.setOnClickListener { mostrarDialogoIngreso() }
        binding.btnConfigurarPresupuesto.setOnClickListener { mostrarDialogoPresupuesto() }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CREAR_FONDO && montoAhorrosPendiente > 0) {
            val monto = montoAhorrosPendiente
            montoAhorrosPendiente = 0.0
            mostrarDialogoSeleccionFondo(monto)
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
                    adapter.actualizarIngresos(ingresos)
                }

                val presupuestoResponse = RetrofitClient.api.getPresupuesto()
                if (presupuestoResponse.isSuccessful) {
                    val p = presupuestoResponse.body()
                    val ahorro = p?.get("porcentaje_ahorro") as? Double ?: 20.0
                    binding.tvPresupuesto.text = "Ahorro: ${ahorro.toInt()}%"
                }
            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoIngreso() {
        val fuenteInput = android.widget.EditText(this)
        fuenteInput.hint = "Fuente (ej: Sueldo, Freelance)"

        val importeInput = android.widget.EditText(this)
        importeInput.hint = "Importe en €"
        importeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val descInput = android.widget.EditText(this)
        descInput.hint = "Descripción (opcional)"

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(fuenteInput)
        layout.addView(importeInput)
        layout.addView(descInput)

        AlertDialog.Builder(this)
            .setTitle("Añadir ingreso")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val fuente = fuenteInput.text.toString().trim()
                val importe = importeInput.text.toString().toDoubleOrNull()
                if (importe == null || importe <= 0) {
                    Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                registrarIngreso(importe, descInput.text.toString(), fuente)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoPresupuesto() {
        val ahorroInput = android.widget.EditText(this)
        ahorroInput.hint = "% Ahorro (ej: 20)"
        ahorroInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(ahorroInput)

        AlertDialog.Builder(this)
            .setTitle("Configurar % de ahorro")
            .setMessage("El resto se distribuirá automáticamente según tus egresos y mesada.")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val ahorro = ahorroInput.text.toString().toDoubleOrNull() ?: 0.0
                if (ahorro < 0 || ahorro > 100) {
                    Toast.makeText(this, "Porcentaje inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val comunes = (100 - ahorro) / 2
                val personal = (100 - ahorro) / 2
                configurarPresupuesto(ahorro, comunes, personal)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarIngreso(importe: Double, descripcion: String, fuente: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearIngreso(
                    IngresoRequest(importe, descripcion, fuente)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@IngresosActivity, "Ingreso añadido ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                    mostrarDialogoAhorro(importe)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                    val code = response.code()
                    Toast.makeText(this@IngresosActivity, "Error $code: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Excepción: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoAhorro(ingresoImporte: Double) {
        lifecycleScope.launch {
            try {
                val presupuestoResponse = RetrofitClient.api.getPresupuesto()
                val pctAhorro = (presupuestoResponse.body()?.get("porcentaje_ahorro") as? Double) ?: 20.0
                val montoAhorro = ingresoImporte * pctAhorro / 100

                val fondosResponse = RetrofitClient.api.getAhorros()
                val fondos = fondosResponse.body()?.get("fondos") as? List<*> ?: emptyList<Any>()

                if (!fondosResponse.isSuccessful || fondos.isEmpty()) {
                    runOnUiThread {
                        AlertDialog.Builder(this@IngresosActivity)
                            .setTitle("💰 Sin fondos de ahorro")
                            .setMessage("Tienes %.2f € para ahorrar. Crea un fondo primero.".format(montoAhorro))
                            .setPositiveButton("Crear fondo") { _, _ ->
                                montoAhorrosPendiente = montoAhorro
                                startActivityForResult(
                                    android.content.Intent(this@IngresosActivity, AhorroActivity::class.java),
                                    REQUEST_CREAR_FONDO
                                )
                            }
                            .setNegativeButton("Después", null)
                            .show()
                    }
                    return@launch
                }

                mostrarDialogoSeleccionFondo(montoAhorro)

            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoSeleccionFondo(montoAhorro: Double) {
        lifecycleScope.launch {
            try {
                val fondosResponse = RetrofitClient.api.getAhorros()
                val body = fondosResponse.body()
                val fondos = body?.get("fondos") as? List<*> ?: emptyList<Any>()
                val listaFondos = fondos.filterIsInstance<Map<String, Any>>()

                if (listaFondos.isEmpty()) {
                    Toast.makeText(this@IngresosActivity, "No hay fondos disponibles", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val nombres = listaFondos.map {
                    val nombre = it["nombre"]?.toString() ?: "Fondo"
                    val acumulado = (it["acumulado"] as? Double) ?: 0.0
                    "$nombre · %.2f€".format(acumulado)
                }.toTypedArray()

                runOnUiThread {
                    AlertDialog.Builder(this@IngresosActivity)
                        .setTitle("💰 ¿A qué fondo van %.2f€?".format(montoAhorro))
                        .setItems(nombres) { _, which ->
                            val fondo = listaFondos[which]
                            val fondoId = (fondo["id"] as? Double)?.toInt() ?: 0
                            anadirAlFondo(fondoId, montoAhorro)
                        }
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun anadirAlFondo(fondoId: Int, cantidad: Double) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.actualizarAhorro(
                    fondoId,
                    com.andrea.gestorgastos.model.ActualizarAhorroRequest(cantidad)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@IngresosActivity, "Ahorro añadido al fondo ✅", Toast.LENGTH_SHORT).show()
                } else {
                    val error = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(this@IngresosActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarIngreso(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar ingreso")
            .setMessage("¿Seguro que quieres eliminar este ingreso?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarIngreso(id)
                        if (response.isSuccessful) {
                            Toast.makeText(this@IngresosActivity, "Ingreso eliminado", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarPresupuesto(ahorro: Double, comunes: Double, personal: Double) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearPresupuesto(
                    PresupuestoRequest(ahorro, comunes, personal)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@IngresosActivity, "Presupuesto guardado ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IngresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}