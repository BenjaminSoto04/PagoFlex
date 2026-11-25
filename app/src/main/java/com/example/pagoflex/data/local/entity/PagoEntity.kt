package com.example.pagoflex.data.local.entity


data class PagoEntity(
    val id: Long = 0,
    val numeroComprobante: String,
    val idCompromiso: Long,
    val fechaHora: String,
    val montoPagado: Int,
    val canal: String,
    val medioPago: String,
    // Columna heredada de la version anterior; ya no se usa desde la app.
    val fotoUri: String? = null,
    val folioUnico: String? = null,
    val cuotaPagada: Int? = null,
    val fechaVencimientoCuota: String? = null
)
