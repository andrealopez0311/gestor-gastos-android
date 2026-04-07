package com.andrea.gestorgastos.ui.gastos

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityGastosBinding
import com.andrea.gestorgastos.model.GastoRequest
import com.andrea.gestorgastos.network.RetrofitClient
import com.andrea.gestorgastos.ui.hogar.CrearHogarActivity
import com.andrea.gestorgastos.ui.hogar.HogarActivity
import com.andrea.gestorgastos.ui.login.LoginActivity
import kotlinx.coroutines.launch
import com.andrea.gestorgastos.ui.gastos.AhorroPersonalActivity

class GastosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGastosBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: GastosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGastosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("gestor_prefs", MODE_PRIVATE)

        adapter = GastosAdapter { gastoId -> eliminarGasto(gastoId) }
        binding.recyclerGastos.layoutManager = LinearLayoutManager(this)
        binding.recyclerGastos.adapter = adapter

        cargarGastos()
        cargarMesada()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMiHogar()
                if (response.isSuccessful) {
                    val hogar = response.body()?.get("hogar")
                    if (hogar == null) {
                        startActivity(Intent(this@GastosActivity, CrearHogarActivity::class.java))
                    }
                }
            } catch (e: Exception) { }
        }

        binding.btnAgregarGasto.setOnClickListener {
            mostrarDialogoAgregarGasto()
        }

        binding.btnCerrarSesion.setOnClickListener {
            prefs.edit().remove("token").apply()
            RetrofitClient.setToken("")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnResumen.setOnClickListener {
            startActivity(Intent(this, ResumenActivity::class.java))
        }

        binding.btnHogar.setOnClickListener {
            startActivity(Intent(this, HogarActivity::class.java))
        }

        binding.btnAhorroPersonal.setOnClickListener {
            startActivity(Intent(this, AhorroPersonalActivity::class.java))
        }

    }

    private fun cargarGastos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getGastos()
                if (response.isSuccessful) {
                    adapter.actualizarGastos(response.body() ?: emptyList())
                } else {
                    Toast.makeText(this@GastosActivity, "Error al cargar gastos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GastosActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
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

                AlertDialog.Builder(this@GastosActivity)
                    .setTitle("Nueva categoría")
                    .setItems(nombresCategorias) { _, which ->
                        categoriaSeleccionadaId = categorias[which].id
                        mostrarDialogoImporte(categoriaSeleccionadaId)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@GastosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            .setTitle("Añadir gasto")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val importe = input.text.toString().toDoubleOrNull()
                if (importe == null || importe <= 0) {
                    Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                crearGasto(categoriaId, importe, descInput.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearGasto(categoriaId: Int, importe: Double, descripcion: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearGasto(
                    GastoRequest(categoriaId, descripcion, importe)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@GastosActivity, "Gasto añadido ✅", Toast.LENGTH_SHORT).show()
                    cargarGastos()
                } else {
                    Toast.makeText(this@GastosActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GastosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarGasto(gastoId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar gasto")
            .setMessage("¿Seguro que quieres eliminar este gasto?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.eliminarGasto(gastoId)
                        if (response.isSuccessful) {
                            Toast.makeText(this@GastosActivity, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                            cargarGastos()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@GastosActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cargarMesada() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getResumenHogar()
                if (response.isSuccessful) {
                    val body = response.body()
                    val personal = body?.get("personal") as? Map<*, *>
                    val mesada = personal?.get("mesada") as? Double ?: 0.0
                    val gastado = personal?.get("gastado") as? Double ?: 0.0
                    val disponible = personal?.get("disponible") as? Double ?: 0.0

                    binding.tvMesada.text = "%.2f €".format(mesada)
                    binding.tvGastadoMesada.text = "%.2f €".format(gastado)
                    binding.tvDisponibleMesada.text = "%.2f €".format(disponible)

                    if (disponible < 0) {
                        binding.tvDisponibleMesada.setTextColor(
                            android.graphics.Color.parseColor("#E53935")
                        )
                    } else {
                        binding.tvDisponibleMesada.setTextColor(
                            android.graphics.Color.parseColor("#43A047")
                        )
                    }
                }
            } catch (e: Exception) {
                // Si falla no bloqueamos la pantalla
            }
        }
    }
}