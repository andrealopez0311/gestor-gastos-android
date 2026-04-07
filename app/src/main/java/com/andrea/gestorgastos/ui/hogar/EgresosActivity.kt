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

        adapterComunes = GastosComunesAdapter(
            onEliminar = { id -> eliminarGastoComun(id) },
            onEditar = { gasto -> mostrarDialogoEditarComun(gasto) }
        )
        adapterPeriodicos = GastosPeriodicosAdapter(
            onPagar = { id -> registrarPago(id) },
            onEliminar = { id -> eliminarPeriodico(id) },
            onEditar = { gasto -> mostrarDialogoEditarPeriodico(gasto) }
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
    private fun mostrarDialogoEditarComun(gasto: Map<String, Any>) {
        val id = (gasto["id"] as? Double)?.toInt() ?: 0
        val importeActual = gasto["importe"] as? Double ?: 0.0
        val descripcionActual = gasto["descripcion"] as? String ?: ""
        val fechaActual = gasto["fecha"] as? String ?: ""

        val importeInput = android.widget.EditText(this)
        importeInput.hint = "Importe en €"
        importeInput.setText(importeActual.toString())
        importeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val descInput = android.widget.EditText(this)
        descInput.hint = "Descripción"
        descInput.setText(descripcionActual)

        val fechaInput = android.widget.EditText(this)
        fechaInput.hint = "Fecha (YYYY-MM-DD)"
        fechaInput.setText(fechaActual)

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(importeInput)
        layout.addView(descInput)
        layout.addView(fechaInput)

        AlertDialog.Builder(this)
            .setTitle("Editar gasto común")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val importe = importeInput.text.toString().toDoubleOrNull()
                if (importe == null || importe <= 0) {
                    Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.editarGastoComun(
                            id,
                            com.andrea.gestorgastos.model.EditarGastoComunRequest(
                                descripcion = descInput.text.toString(),
                                importe = importe,
                                fecha = fechaInput.text.toString().ifEmpty { null }
                            )
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(this@EgresosActivity, "Gasto actualizado ✅", Toast.LENGTH_SHORT).show()
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

    private fun mostrarDialogoEditarPeriodico(gasto: Map<String, Any>) {
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
                            Toast.makeText(this@EgresosActivity, "Gasto actualizado ✅", Toast.LENGTH_SHORT).show()
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