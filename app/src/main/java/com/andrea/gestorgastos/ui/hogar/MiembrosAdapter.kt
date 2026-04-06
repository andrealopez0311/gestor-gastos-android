package com.andrea.gestorgastos.ui.hogar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andrea.gestorgastos.databinding.ItemMiembroBinding

class MiembrosAdapter : RecyclerView.Adapter<MiembrosAdapter.MiembroViewHolder>() {

    private var miembros = listOf<Map<String, Any>>()

    fun actualizarMiembros(nuevos: List<Map<String, Any>>) {
        miembros = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiembroViewHolder {
        val binding = ItemMiembroBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MiembroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MiembroViewHolder, position: Int) {
        holder.bind(miembros[position])
    }

    override fun getItemCount() = miembros.size

    inner class MiembroViewHolder(
        private val binding: ItemMiembroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(miembro: Map<String, Any>) {
            val nombre = miembro["nombre"] as? String ?: ""
            val email = miembro["email"] as? String ?: ""
            val rol = miembro["rol"] as? String ?: "miembro"

            binding.tvNombreMiembro.text = nombre
            binding.tvEmailMiembro.text = email
            binding.tvRolMiembro.text = rol.uppercase()
            binding.tvAvatar.text = nombre.firstOrNull()?.uppercase() ?: "?"

            if (rol == "admin") {
                binding.tvRolMiembro.setBackgroundColor(
                    android.graphics.Color.parseColor("#1E88E5")
                )
            } else {
                binding.tvRolMiembro.setBackgroundColor(
                    android.graphics.Color.parseColor("#43A047")
                )
            }
        }
    }
}