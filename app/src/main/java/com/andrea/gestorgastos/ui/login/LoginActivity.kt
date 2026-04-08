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
import com.andrea.gestorgastos.ui.hogar.HogarActivity
import com.andrea.gestorgastos.ui.hogar.CrearHogarActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("gestor_prefs", MODE_PRIVATE)

        // Si ya hay token guardado, ir directo a gastos
        val savedToken = prefs.getString("token", null)
        if (savedToken != null) {
            RetrofitClient.setToken(savedToken)
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
                    val token = response.body()!!.access_token
                    prefs.edit().putString("token", token).apply()
                    RetrofitClient.setToken(token)

                    // Comprobar si es el primer login
                    val primerLogin = prefs.getBoolean("primer_login_$email", true)

                    if (primerLogin) {
                        // Comprobar si tiene hogar
                        val hogarResponse = RetrofitClient.api.getMiHogar()
                        val tieneHogar = hogarResponse.isSuccessful &&
                                hogarResponse.body()?.get("hogar") != null

                        if (!tieneHogar) {
                            prefs.edit().putBoolean("primer_login_$email", false).apply()
                            startActivity(Intent(this@LoginActivity, CrearHogarActivity::class.java))
                            finish()
                            return@launch
                        }
                    }

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
        startActivity(Intent(this, HogarActivity::class.java))
        finish()
    }
}