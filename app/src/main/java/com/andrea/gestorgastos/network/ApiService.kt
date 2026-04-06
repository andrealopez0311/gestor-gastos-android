package com.andrea.gestorgastos.network

import com.andrea.gestorgastos.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("usuarios/registro")
    suspend fun registro(@Body request: RegisterRequest): Response<Map<String, Any>>

    @POST("usuarios/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("categorias/")
    suspend fun getCategorias(@Header("Authorization") token: String): Response<List<Categoria>>

    @GET("gastos/")
    suspend fun getGastos(@Header("Authorization") token: String): Response<List<Gasto>>

    @POST("gastos/")
    suspend fun crearGasto(
        @Header("Authorization") token: String,
        @Body request: GastoRequest
    ): Response<Map<String, Any>>

    @DELETE("gastos/{id}")
    suspend fun eliminarGasto(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Map<String, Any>>

    @GET("gastos/resumen")
    suspend fun getResumen(@Header("Authorization") token: String): Response<List<Map<String, Any>>>
}