package com.example.pagoflex.data.remote

import retrofit2.Response
import retrofit2.http.GET

/**
 * Servicio para obtener indicadores económicos de Chile (UF y Dólar) desde mindicador.cl
 */
interface IndicadoresApiService {

    /**
     * Obtiene los valores actuales de UF, Dólar Americana, Dólar Canadiense y otras monedas
     */
    @GET("api")
    suspend fun obtenerIndicadores(): Response<com.example.pagoflex.model.IndicadoresData>

}
