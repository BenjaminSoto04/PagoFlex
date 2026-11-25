package com.example.pagoflex.data.model

/**
 * DTOs del microservicio de pagos (puerto 8084).
 * Mapeados 1:1 con `PagoResponse`, `PagoRegistroRequest` y
 * `ComprobanteResponse` de PagoFlexMS.
 */
data class ApiPagoResponse(
    val id: Long? = null,
    val folioUnico: String? = null,
    val numeroComprobante: String? = null,
    val usuarioId: Long? = null,
    val compromisoId: Long? = null,
    val empresaId: Long? = null,
    val montoPagado: Int? = null,
    val cuotaPagada: Int? = null,
    val metodoPago: String? = null,
    val fechaPago: String? = null,
    val fechaVencimientoCuota: String? = null,
    val canal: String? = null,
    val fotoUri: String? = null,
    val comprobante: ApiComprobanteResponse? = null
)

data class ApiPagoRegistroRequest(
    val usuarioId: Long,
    val compromisoId: Long,
    val empresaId: Long,
    val montoPagado: Int,
    val cuotaPagada: Int? = null,
    val metodoPago: String,
    val fechaPago: String? = null,
    val fechaVencimientoCuota: String? = null,
    val folioUnico: String? = null,
    val numeroComprobante: String? = null,
    val canal: String? = null,
    val fotoUri: String? = null,
    val comprobanteUri: String? = null,
    val comprobanteFecha: String? = null
)

data class ApiComprobanteResponse(
    val id: Long? = null,
    val idPago: Long? = null,
    val uri: String? = null,
    val fecha: String? = null
)
