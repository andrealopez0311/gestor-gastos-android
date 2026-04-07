package com.andrea.gestorgastos.ui.hogar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityAhorroBinding
import com.andrea.gestorgastos.model.AhorroRequest
import com.andrea.gestorgastos.model.ActualizarAhorroRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class AhorroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAhorroBinding
    private lateinit var adapter: AhorroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAhorroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AhorroAdapter(
            onAnadir = { ahorroId, _ -> mostrarDialogoAnadir(ahorroId) },
            onEliminar = { ahorroId -> eliminarAhorro(ahorroId) }
        )
        binding.recyclerAhorros.layoutManager = LinearLayoutManager(this)
        binding.recyclerAhorros.adapter = adapter

        cargarAhorros()

        binding.btnVolverAhorro.setOnClickListener { finish() }
        binding.btnCrearFondo.setOnClickListener { mostrarDialogoCrearFondo() }
    }

    private fun cargarAhorros() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getAhorros()
                if (response.isSuccessful) {
                    val body = response.body()
                    val fondos = body?.get("fondos") as? List<Map<String, Any>> ?: emptyList()
                    adapter.actualizarAhorros(fondos)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AhorroActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCrearFondo() {
        val nombreInput = android.widget.EditText(this)
        nombreInput.hint = "Nombre del fondo (ej: Vacaciones)"

        val metaInput = android.widget.EditText(this)
        metaInput.hint = "Meta en € (opcional)"
        metaInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(nombreInput)
        layout.addView(metaInput)

        AlertDialog.Builder(this)
            .setTitle("Crear fondo de ahorro")
            .setView(layout)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = nombreInput.text.toString().trim()
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val meta = metaInput.text.toString().toDoubleOrNull()
                crearFondo(nombre, meta)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAnadir(ahorroId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getDisponibleAhorro()
                val body = response.body()
                val disponible = when (val d = body?.get("disponible_para_ahorrar")) {
                    is Double -> d
                    is Int -> d.toDouble()
                    is Long -> d.toDouble()
                    else -> 0.0
                }

                val presupuestoResponse = RetrofitClient.api.getPresupuesto()
                val pctAhorro = (presupuestoResponse.body()?.get("porcentaje_ahorro") as? Double)?.toInt() ?: 20

                runOnUiThread {
                    if (disponible <= 0) {
                        AlertDialog.Builder(this@AhorroActivity)
                            .setTitle("💰 Límite del mes alcanzado")
                            .setMessage("Ya has ahorrado el $pctAhorro%% de los ingresos del mes. Solo puedes añadir ahorro voluntario de tu mesada.")
                            .setPositiveButton("Ahorro voluntario") { _, _ ->
                                mostrarDialogoCantidad(ahorroId, true)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        val opciones = arrayOf(
                            "💰 Ahorro del mes ($pctAhorro%%) · %.2f€ disponibles".format(disponible),
                            "🎯 Ahorro voluntario (de mi mesada)"
                        )
                        AlertDialog.Builder(this@AhorroActivity)
                            .setTitle("¿Qué tipo de ahorro?")
                            .setItems(opciones) { _, which ->
                                mostrarDialogoCantidad(ahorroId, which == 1)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AhorroActivity, "Error: ${e.javaClass.name}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoCantidad(ahorroId: Int, esVoluntario: Boolean) {
        val input = android.widget.EditText(this)
        input.hint = "Cantidad a añadir en €"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(input)

        AlertDialog.Builder(this)
            .setTitle(if (esVoluntario) "🎯 Ahorro voluntario" else "💰 Añadir al ahorro")
            .setView(layout)
            .setPositiveButton("Añadir") { _, _ ->
                val cantidad = input.text.toString().toDoubleOrNull()
                if (cantidad == null || cantidad <= 0) {
                    Toast.makeText(this, "Cantidad inválida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                anadirAhorro(ahorroId, cantidad, esVoluntario)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearFondo(nombre: String, meta: Double?) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearAhorro(AhorroRequest(nombre, meta))
                if (response.isSuccessful) {
                    Toast.makeText(this@AhorroActivity, "Fondo creado ✅", Toast.LENGTH_SHORT).show()
                    cargarAhorros()
                } else {
                    Toast.makeText(this@AhorroActivity, "Error al crear", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AhorroActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun anadirAhorro(ahorroId: Int, cantidad: Double, esVoluntario: Boolean) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.actualizarAhorro(
                    ahorroId, ActualizarAhorroRequest(cantidad, esVoluntario)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@AhorroActivity, "Ahorro actualizado ✅", Toast.LENGTH_SHORT).show()
                    cargarAhorros()
                } else {
                    val error = response.errorBody()?.string() ?: "Error"
                    Toast.makeText(this@AhorroActivity, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val msg = "${e.javaClass.name}: ${e.message} / cause: ${e.cause}"
                Toast.makeText(this@AhorroActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun eliminarAhorro(ahorroId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar fondo")
            .setMessage("¿Seguro que quieres eliminar este fondo de ahorro?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarAhorro(ahorroId)
                        if (response.isSuccessful) {
                            Toast.makeText(this@AhorroActivity, "Fondo eliminado", Toast.LENGTH_SHORT).show()
                            cargarAhorros()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@AhorroActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}