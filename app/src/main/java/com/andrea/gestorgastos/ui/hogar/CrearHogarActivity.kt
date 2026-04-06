package com.andrea.gestorgastos.ui.hogar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.andrea.gestorgastos.databinding.ActivityCrearHogarBinding
import com.andrea.gestorgastos.model.HogarRequest
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class CrearHogarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearHogarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearHogarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCrearHogar.setOnClickListener {
            val nombre = binding.etNombreHogar.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre para el hogar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            crearHogar(nombre)
        }
    }

    private fun crearHogar(nombre: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.crearHogar(HogarRequest(nombre))
                if (response.isSuccessful) {
                    Toast.makeText(this@CrearHogarActivity, "Hogar creado ✅", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@CrearHogarActivity, HogarActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@CrearHogarActivity, "Error al crear el hogar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CrearHogarActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}