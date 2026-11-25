package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiComprobanteResponse
import com.example.pagoflex.data.model.ApiPagoRegistroRequest
import com.example.pagoflex.data.model.ApiPagoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Microservicio de pagos (PagoFlexMS).
 * Base URL: http://10.0.2.2:8084/
 *
 * Endpoints mapeados desde `PagoController` (`/api/pagos`).
 */
interface PagosApiService {

    @POST("api/pagos")
    suspend fun registrar(@Body request: ApiPagoRegistroRequest): Response<ApiPagoResponse>

    @GET("api/pagos")
    suspend fun listarTodos(): Response<List<ApiPagoResponse>>

    @GET("api/pagos/{id}")
    suspend fun obtenerPorId(@Path("id") id: Long): Response<ApiPagoResponse>

    @GET("api/pagos/folio/{folioUnico}")
    suspend fun obtenerPorFolioUnico(@Path("folioUnico") folioUnico: String): Response<ApiPagoResponse>

    @GET("api/pagos/usuario/{usuarioId}")
    suspend fun listarPorUsuario(@Path("usuarioId") usuarioId: Long): Response<List<ApiPagoResponse>>

    @GET("api/pagos/compromiso/{compromisoId}")
    suspend fun listarPorCompromiso(@Path("compromisoId") compromisoId: Long): Response<List<ApiPagoResponse>>

    @GET("api/pagos/empresa/{empresaId}")
    suspend fun listarPorEmpresa(@Path("empresaId") empresaId: Long): Response<List<ApiPagoResponse>>

    @GET("api/pagos/{id}/comprobante")
    suspend fun obtenerComprobantePorPago(@Path("id") id: Long): Response<ApiComprobanteResponse>

    @GET("api/pagos/folio/{folioUnico}/comprobante")
    suspend fun obtenerComprobantePorFolio(@Path("folioUnico") folioUnico: String): Response<ApiComprobanteResponse>
}
