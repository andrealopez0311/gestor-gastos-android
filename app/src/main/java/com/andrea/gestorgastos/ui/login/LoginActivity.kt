package com.andrea.gestorgastos.ui.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.andrea.gestorgastos.R
import com.andrea.gestorgastos.databinding.ActivityLoginBinding
import com.andrea.gestorgastos.model.LoginRequest
import com.andrea.gestorgastos.model.RegisterRequest
import com.andrea.gestorgastos.network.RetrofitClient
import com.andrea.gestorgastos.ui.gastos.GastosActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("gestor_prefs", MODE_PRIVATE)

        // Si ya hay token guardado, ir directo a gastos
        if (prefs.getString("token", null) != null) {
            irAGastos()
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            login(email, password)
        }

        binding.btnRegistro.setOnClickListener {
            val nombre = "Usuario"
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Rellena email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registro(nombre, email, password)
        }
    }

    private fun login(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = "Bearer ${response.body()!!.access_token}"
                    prefs.edit().putString("token", token).apply()
                    irAGastos()
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registro(nombre: String, email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.registro(RegisterRequest(nombre, email, password))
                if (response.isSuccessful) {
                    Toast.makeText(this@LoginActivity, "Cuenta creada, ahora inicia sesión", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LoginActivity, "Error al registrar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun irAGastos() {
        startActivity(Intent(this, GastosActivity::class.java))
        finish()
    }
}