package com.andrea.gestorgastos.ui.gastos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityAhorroPersonalBinding
import com.andrea.gestorgastos.model.AhorroPersonalRequest
import com.andrea.gestorgastos.model.AnadirAhorroPersonalRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class AhorroPersonalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAhorroPersonalBinding
    private lateinit var adapter: AhorroPersonalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAhorroPersonalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AhorroPersonalAdapter(
            onAnadir = { id -> mostrarDialogoAnadir(id) },
            onEliminar = { id -> eliminarFondo(id) }
        )
        binding.recyclerAhorroPersonal.layoutManager = LinearLayoutManager(this)
        binding.recyclerAhorroPersonal.adapter = adapter

        cargarDatos()

        binding.btnVolverAhorroPersonal.setOnClickListener { finish() }
        binding.btnCrearFondoPersonal.setOnClickListener { mostrarDialogoCrear() }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getAhorrosPersonales()
                if (response.isSuccessful) {
                    val body = response.body()
                    val disponible = body?.get("disponible_mesada") as? Double ?: 0.0
                    val fondos = body?.get("fondos") as? List<Map<String, Any>> ?: emptyList()

                    binding.tvDisponibleAhorroPersonal.text = "%.2f €".format(disponible)

                    if (disponible < 0) {
                        binding.tvDisponibleAhorroPersonal.setTextColor(
                            android.graphics.Color.parseColor("#E53935")
                        )
                    } else {
                        binding.tvDisponibleAhorroPersonal.setTextColor(
                            android.graphics.Color.parseColor("#1E88E5")
                        )
                    }

                    adapter.actualizarFondos(fondos)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AhorroPersonalActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCrear() {
        val nombreInput = android.widget.EditText(this)
        nombreInput.hint = "Nombre (ej: Viaje a París)"

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
            .setTitle("Crear fondo personal")
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

    private fun mostrarDialogoAnadir(id: Int) {
        val input = android.widget.EditText(this)
        input.hint = "Cantidad a añadir en €"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Añadir a mi ahorro")
            .setView(layout)
            .setPositiveButton("Añadir") { _, _ ->
                val cantidad = input.text.toString().toDoubleOrNull()
                if (cantidad == null || cantidad <= 0) {
                    Toast.makeText(this, "Cantidad inválida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                anadirAhorro(id, cantidad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearFondo(nombre: String, meta: Double?) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearAhorroPersonal(
                    AhorroPersonalRequest(nombre, meta)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@AhorroPersonalActivity, "Fondo creado ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@AhorroPersonalActivity, "Error al crear", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AhorroPersonalActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun anadirAhorro(id: Int, cantidad: Double) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.anadirAhorroPersonal(
                    id, AnadirAhorroPersonalRequest(cantidad)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@AhorroPersonalActivity, "Ahorro añadido ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    val error = response.errorBody()?.string() ?: "Error"
                    Toast.makeText(this@AhorroPersonalActivity, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AhorroPersonalActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarFondo(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar fondo")
            .setMessage("¿Seguro que quieres eliminar este fondo personal?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarAhorroPersonal(id)
                        if (response.isSuccessful) {
                            Toast.makeText(this@AhorroPersonalActivity, "Fondo eliminado", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@AhorroPersonalActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}