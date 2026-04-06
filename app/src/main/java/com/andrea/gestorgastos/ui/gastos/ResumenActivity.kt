package com.andrea.gestorgastos.ui.gastos

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.andrea.gestorgastos.databinding.ActivityResumenBinding
import com.andrea.gestorgastos.network.RetrofitClient
import kotlinx.coroutines.launch

class ResumenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumenBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: ResumenAdapter
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("gestor_prefs", MODE_PRIVATE)
        token = prefs.getString("token", "") ?: ""

        adapter = ResumenAdapter()
        binding.recyclerResumen.layoutManager = LinearLayoutManager(this)
        binding.recyclerResumen.adapter = adapter

        binding.btnVolver.setOnClickListener {
            finish()
        }

        cargarResumen()
    }

    private fun cargarResumen() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getResumen(token)
                if (response.isSuccessful) {
                    val datos = response.body() ?: emptyList()

                    // Calcular total
                    val total = datos.sumOf { it["total"] as? Double ?: 0.0 }
                    binding.tvTotalMes.text = "%.2f €".format(total)

                    // Actualizar lista
                    adapter.actualizarItems(datos)

                    // Configurar gráfica
                    configurarGrafica(datos)
                } else {
                    Toast.makeText(this@ResumenActivity, "Error al cargar resumen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResumenActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarGrafica(datos: List<Map<String, Any>>) {
        val entries = datos.map { item ->
            val categoria = item["categoria"] as? String ?: ""
            val total = item["total"] as? Double ?: 0.0
            PieEntry(total.toFloat(), categoria)
        }

        val colores = datos.map { item ->
            val colorStr = item["color"] as? String ?: "#1E88E5"
            try {
                Color.parseColor(colorStr)
            } catch (e: Exception) {
                Color.parseColor("#1E88E5")
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = colores
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
        }

        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.WHITE)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(11f)
            animateY(1000)
            invalidate()
        }
    }
}