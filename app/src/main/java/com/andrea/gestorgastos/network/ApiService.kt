package com.andrea.gestorgastos.network

import com.andrea.gestorgastos.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── USUARIOS ──
    @POST("usuarios/registro")
    suspend fun registro(@Body request: RegisterRequest): Response<Map<String, Any>>

    @POST("usuarios/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ── CATEGORIAS ──
    @GET("categorias/")
    suspend fun getCategorias(): Response<List<Categoria>>

    // ── GASTOS PERSONALES ──
    @GET("gastos/")
    suspend fun getGastos(): Response<List<Gasto>>

    @POST("gastos/")
    suspend fun crearGasto(@Body request: GastoRequest): Response<Map<String, Any>>

    @DELETE("gastos/{id}")
    suspend fun eliminarGasto(@Path("id") id: Int): Response<Map<String, Any>>

    @GET("gastos/resumen")
    suspend fun getResumen(): Response<List<Map<String, Any>>>

    // ── HOGARES ──
    @POST("hogares/")
    suspend fun crearHogar(@Body request: HogarRequest): Response<Map<String, Any>>

    @GET("hogares/mio")
    suspend fun getMiHogar(): Response<Map<String, Any>>

    @POST("hogares/invitar")
    suspend fun invitarMiembro(@Body request: InvitarRequest): Response<Map<String, Any>>

    @GET("hogares/miembros")
    suspend fun getMiembros(): Response<List<Map<String, Any>>>

    // ── INGRESOS ──
    @POST("ingresos/")
    suspend fun crearIngreso(@Body request: IngresoRequest): Response<Map<String, Any>>

    @GET("ingresos/")
    suspend fun getMisIngresos(): Response<List<Map<String, Any>>>

    @GET("ingresos/hogar")
    suspend fun getIngresosHogar(): Response<Map<String, Any>>

    // ── PRESUPUESTO ──
    @POST("presupuesto/")
    suspend fun crearPresupuesto(@Body request: PresupuestoRequest): Response<Map<String, Any>>

    @GET("presupuesto/")
    suspend fun getPresupuesto(): Response<Map<String, Any>>

    @GET("presupuesto/resumen")
    suspend fun getResumenHogar(): Response<Map<String, Any>>

    // ── GASTOS COMUNES ──
    @GET("gastos-comunes/")
    suspend fun getGastosComunes(): Response<List<Map<String, Any>>>

    @POST("gastos-comunes/")
    suspend fun crearGastoComun(@Body request: GastoComunRequest): Response<Map<String, Any>>

    @DELETE("gastos-comunes/{id}")
    suspend fun eliminarGastoComun(@Path("id") id: Int): Response<Map<String, Any>>

    // ── AHORRO ──
    @GET("ahorro/")
    suspend fun getAhorros(): Response<List<Map<String, Any>>>

    @POST("ahorro/")
    suspend fun crearAhorro(@Body request: AhorroRequest): Response<Map<String, Any>>

    @PUT("ahorro/{id}")
    suspend fun actualizarAhorro(
        @Path("id") id: Int,
        @Body request: ActualizarAhorroRequest
    ): Response<Map<String, Any>>

    @DELETE("ahorro/{id}")
    suspend fun eliminarAhorro(@Path("id") id: Int): Response<Map<String, Any>>

    // ── GASTOS PERIÓDICOS ──
    @GET("gastos-periodicos/")
    suspend fun getGastosPeriodicos(): Response<Map<String, Any>>

    @POST("gastos-periodicos/")
    suspend fun crearGastoPeriodico(@Body request: GastoPeriodicoRequest): Response<Map<String, Any>>

    @PUT("gastos-periodicos/{id}/acumular")
    suspend fun acumularReserva(@Path("id") id: Int): Response<Map<String, Any>>

    @PUT("gastos-periodicos/{id}/pagar")
    suspend fun registrarPago(@Path("id") id: Int): Response<Map<String, Any>>

    @DELETE("gastos-periodicos/{id}")
    suspend fun eliminarGastoPeriodico(@Path("id") id: Int): Response<Map<String, Any>>

    @GET("gastos-periodicos/resumen-presupuesto")
    suspend fun getResumenConPeriodicos(): Response<Map<String, Any>>

}

