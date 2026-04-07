package com.andrea.gestorgastos.ui.hogar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityEgresosBinding
import com.andrea.gestorgastos.model.GastoComunRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class EgresosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEgresosBinding
    private lateinit var adapterComunes: GastosComunesAdapter
    private lateinit var adapterPeriodicos: GastosPeriodicosAdapter
    private var mostrandoComunes = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEgresosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapterComunes = GastosComunesAdapter { id -> eliminarGastoComun(id) }
        adapterPeriodicos = GastosPeriodicosAdapter(
            onPagar = { id -> registrarPago(id) },
            onEliminar = { id -> eliminarPeriodico(id) }
        )

        binding.recyclerEgresos.layoutManager = LinearLayoutManager(this)
        binding.recyclerEgresos.adapter = adapterComunes

        cargarDatos()

        binding.btnVolverEgresos.setOnClickListener { finish() }

        binding.btnTabComunes.setOnClickListener {
            mostrandoComunes = true
            binding.recyclerEgresos.adapter = adapterComunes
            binding.btnAnadirEgreso.text = "+ Añadir Gasto Común"
            binding.btnTabComunes.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935"))
            binding.btnTabPeriodicos.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))
        }

        binding.btnTabPeriodicos.setOnClickListener {
            mostrandoComunes = false
            binding.recyclerEgresos.adapter = adapterPeriodicos
            binding.btnAnadirEgreso.text = "+ Añadir Gasto Periódico"
            binding.btnTabPeriodicos.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7B1FA2"))
            binding.btnTabComunes.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))
        }

        binding.btnAnadirEgreso.setOnClickListener {
            if (mostrandoComunes) {
                mostrarDialogoAgregarComun()
            } else {
                startActivity(android.content.Intent(this, GastosPeriodicosActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getEgresos()
                if (response.isSuccessful) {
                    val body = response.body()
                    val totalEgresos = body?.get("total_egresos") as? Double ?: 0.0
                    val totalComunes = body?.get("total_comunes") as? Double ?: 0.0
                    val totalPeriodicos = body?.get("total_periodicos") as? Double ?: 0.0
                    val gastosComunes = body?.get("gastos_comunes") as? List<Map<String, Any>> ?: emptyList()
                    val gastosPeriodicos = body?.get("gastos_periodicos") as? List<Map<String, Any>> ?: emptyList()

                    binding.tvTotalEgresos.text = "%.2f €".format(totalEgresos)
                    binding.tvTotalComunes.text = "%.2f €".format(totalComunes)
                    binding.tvTotalPeriodicos.text = "%.2f €".format(totalPeriodicos)

                    adapterComunes.actualizarGastos(gastosComunes)
                    adapterPeriodicos.actualizarGastos(gastosPeriodicos)
                }
            } catch (e: Exception) {
                Toast.makeText(this@EgresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoAgregarComun() {
        lifecycleScope.launch {
            try {
                val categoriasResponse = RetrofitClient.api.getCategorias()
                if (!categoriasResponse.isSuccessful) return@launch
                val categorias = categoriasResponse.body() ?: return@launch
                val nombresCategorias = categorias.map { it.nombre }.toTypedArray()
                var categoriaSeleccionadaId = categorias[0].id

                AlertDialog.Builder(this@EgresosActivity)
                    .setTitle("Selecciona categoría")
                    .setItems(nombresCategorias) { _, which ->
                        categoriaSeleccionadaId = categorias[which].id
                        mostrarDialogoImporteComun(categoriaSeleccionadaId)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@EgresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoImporteComun(categoriaId: Int) {
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
                    Toast.makeText(this@EgresosActivity, "Gasto añadido ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EgresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarGastoComun(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar gasto")
            .setMessage("¿Seguro que quieres eliminar este gasto?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarGastoComun(id)
                        if (response.isSuccessful) {
                            Toast.makeText(this@EgresosActivity, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@EgresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarPago(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Registrar pago")
            .setMessage("¿Confirmas que has pagado este gasto periódico?")
            .setPositiveButton("Confirmar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.registrarPago(id)
                        if (response.isSuccessful) {
                            Toast.makeText(this@EgresosActivity, "Pago registrado ✅", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@EgresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                            Toast.makeText(this@EgresosActivity, "Eliminado", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@EgresosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}