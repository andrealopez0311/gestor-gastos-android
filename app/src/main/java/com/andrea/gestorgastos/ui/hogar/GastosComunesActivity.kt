package com.andrea.gestorgastos.ui.hogar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityGastosComunesBinding
import com.andrea.gestorgastos.model.GastoComunRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class GastosComunesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGastosComunesBinding
    private lateinit var adapter: GastosComunesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGastosComunesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GastosComunesAdapter { gastoId ->
            eliminarGastoComun(gastoId)
        }
        binding.recyclerGastosComunes.layoutManager = LinearLayoutManager(this)
        binding.recyclerGastosComunes.adapter = adapter

        cargarDatos()

        binding.btnVolverComunes.setOnClickListener { finish() }

        binding.btnAgregarGastoComun.setOnClickListener {
            mostrarDialogoAgregarGasto()
        }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getGastosComunes()
                if (response.isSuccessful) {
                    val gastos = response.body() ?: emptyList()
                    adapter.actualizarGastos(gastos)
                    val total = gastos.sumOf { it["importe"] as? Double ?: 0.0 }
                    binding.tvGastadoComunes.text = "%.2f €".format(total)
                }

                val resumenResponse = RetrofitClient.api.getResumenHogar()
                if (resumenResponse.isSuccessful) {
                    val real = resumenResponse.body()?.get("real") as? Map<*, *>
                    val disponible = real?.get("disponible_comunes") as? Double ?: 0.0
                    binding.tvDisponibleComunes.text = "%.2f €".format(disponible)
                }
            } catch (e: Exception) {
                Toast.makeText(this@GastosComunesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoAgregarGasto() {
        lifecycleScope.launch {
            try {
                val categoriasResponse = RetrofitClient.api.getCategorias()
                if (!categoriasResponse.isSuccessful) return@launch
                val categorias = categoriasResponse.body() ?: return@launch
                val nombresCategorias = categorias.map { it.nombre }.toTypedArray()
                var categoriaSeleccionadaId = categorias[0].id

                AlertDialog.Builder(this@GastosComunesActivity)
                    .setTitle("Selecciona categoría")
                    .setItems(nombresCategorias) { _, which ->
                        categoriaSeleccionadaId = categorias[which].id
                        mostrarDialogoImporte(categoriaSeleccionadaId)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@GastosComunesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoImporte(categoriaId: Int) {
        val input = android.widget.EditText(this)
        input.hint = "Importe en €"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val descInput = android.widget.EditText(this)
        descInput.hint = "Descripción (opcional)"

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(input)
        layout.addView(descInput)

        AlertDialog.Builder(this)
            .setTitle("Añadir gasto común")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val importe = input.text.toString().toDoubleOrNull()
                if (importe == null || importe <= 0) {
                    Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                crearGastoComun(categoriaId, importe, descInput.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearGastoComun(categoriaId: Int, importe: Double, descripcion: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearGastoComun(
                    GastoComunRequest(categoriaId, descripcion, importe)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@GastosComunesActivity, "Gasto añadido ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@GastosComunesActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GastosComunesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarGastoComun(gastoId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar gasto")
            .setMessage("¿Seguro que quieres eliminar este gasto?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarGastoComun(gastoId)
                        if (response.isSuccessful) {
                            Toast.makeText(this@GastosComunesActivity, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@GastosComunesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}