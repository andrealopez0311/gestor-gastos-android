package com.andrea.gestorgastos.model

data class Gasto(
    val id: Int,
    val categoria: String,
    val descripcion: String,
    val importe: Double,
    val fecha: String
)

data class GastoRequest(
    val categoria_id: Int,
    val descripcion: String,
    val importe: Double,
    val fecha: String? = null
)

data class Categoria(
    val id: Int,
    val nombre: String,
    val icono: String,
    val color: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class HogarRequest(val nombre: String)

data class InvitarRequest(val email: String)

data class Hogar(
    val id: Int,
    val nombre: String,
    val creador_id: Int
)

data class Miembro(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: String
)

data class IngresoRequest(
    val importe: Double,
    val descripcion: String = "",
    val fuente: String = "",
    val mes: Int? = null,
    val anio: Int? = null
)

data class PresupuestoRequest(
    val porcentaje_ahorro: Double,
    val porcentaje_comunes: Double,
    val porcentaje_personal: Double
)

data class GastoComunRequest(
    val categoria_id: Int,
    val descripcion: String = "",
    val importe: Double
)

data class AhorroRequest(
    val nombre: String,
    val meta: Double? = null
)

data class ActualizarAhorroRequest(
    val cantidad: Double
)

data class GastoPeriodicoRequest(
    val nombre: String,
    val importe: Double,
    val frecuencia: Int,
    val proximo_pago: String? = null
)

data class AñadirAhorroRequest(
    val cantidad: Double,
    val fondo_id: Int
)

data class AhorroPersonalRequest(
    val nombre: String,
    val meta: Double? = null
)

data class AnadirAhorroPersonalRequest(
    val cantidad: Double
)
