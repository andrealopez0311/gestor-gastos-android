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
