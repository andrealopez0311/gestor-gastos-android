package com.andrea.gestorgastos.ui.hogar

import com.andrea.gestorgastos.ui.hogar.EgresosActivity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andrea.gestorgastos.databinding.ActivityHogarBinding
import com.andrea.gestorgastos.model.InvitarRequest
import com.andrea.gestorgastos.network.RetrofitClient
import com.andrea.gestorgastos.ui.gastos.GastosActivity
import com.andrea.gestorgastos.ui.gastos.ResumenActivity
import kotlinx.coroutines.launch
import com.andrea.gestorgastos.ui.hogar.GastosPeriodicosActivity

class HogarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHogarBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var miembrosAdapter: MiembrosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHogarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("gestor_prefs", MODE_PRIVATE)

        miembrosAdapter = MiembrosAdapter()
        binding.recyclerMiembros.layoutManager = LinearLayoutManager(this)
        binding.recyclerMiembros.adapter = miembrosAdapter

        cargarDatos()

        binding.btnInvitar.setOnClickListener {
            mostrarDialogoInvitar()
        }

        binding.btnIngresos.setOnClickListener {
            startActivity(Intent(this, IngresosActivity::class.java))
        }

        binding.btnGastosComunes.setOnClickListener {
            startActivity(Intent(this, GastosComunesActivity::class.java))
        }

        binding.btnGastosPersonales.setOnClickListener {
            startActivity(Intent(this, GastosActivity::class.java))
        }

        binding.btnAhorro.setOnClickListener {
            startActivity(Intent(this, AhorroActivity::class.java))
        }

        binding.btnPeriodicos.setOnClickListener {
            startActivity(Intent(this, GastosPeriodicosActivity::class.java))
        }

        binding.btnEgresos.setOnClickListener {
            startActivity(Intent(this, EgresosActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                // Cargar info del hogar
                val hogarResponse = RetrofitClient.api.getMiHogar()
                if (hogarResponse.isSuccessful) {
                    val body = hogarResponse.body()
                    val hogar = body?.get("hogar") as? Map<*, *>
                    val miembros = body?.get("miembros") as? List<Map<String, Any>>

                    hogar?.let {
                        binding.tvNombreHogar.text = "🏠 ${it["nombre"]}"
                    }

                    miembros?.let {
                        miembrosAdapter.actualizarMiembros(it)
                    }
                }

                // Cargar resumen financiero
                val resumenResponse = RetrofitClient.api.getResumenHogar()
                if (resumenResponse.isSuccessful) {
                    val resumen = resumenResponse.body()
                    val montos = resumen?.get("montos") as? Map<*, *>
                    val real = resumen?.get("real") as? Map<*, *>

                    val ingresoTotal = resumen?.get("ingreso_total") as? Double ?: 0.0
                    val ahorro = montos?.get("ahorro") as? Double ?: 0.0
                    val egresosResponse = RetrofitClient.api.getEgresos()
                    if (egresosResponse.isSuccessful) {
                        val totalEgresos = egresosResponse.body()?.get("total_egresos") as? Double ?: 0.0
                        binding.tvEgresosTotal.text = "%.2f €".format(totalEgresos)
                    }

                    binding.tvIngresoTotal.text = "%.2f €".format(ingresoTotal)
                    binding.tvAhorro.text = "%.2f €".format(ahorro)

                }

            } catch (e: Exception) {
                Toast.makeText(this@HogarActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoInvitar() {
        val input = android.widget.EditText(this)
        input.hint = "Email del usuario a invitar"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)
        layout.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Invitar miembro")
            .setView(layout)
            .setPositiveButton("Invitar") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Escribe un email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                invitarMiembro(email)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun invitarMiembro(email: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.invitarMiembro(InvitarRequest(email))
                if (response.isSuccessful) {
                    Toast.makeText(this@HogarActivity, "Miembro invitado ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@HogarActivity, "Error al invitar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HogarActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}