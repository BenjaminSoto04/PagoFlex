package com.example.pagoflex.data

import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.model.Pago

object DatosSimulados {
    // Usuario simulado: Camila Rojas (UF-01)
    const val ID_USUARIO_ACTUAL = 1L

    // Compromisos ficticios del usuario
    val compromisos = listOf(

        Compromiso(
            id = 101L,
            folio = "CP-2026-0101",
            idUsuario = 1L,
            idEmpresa = 8L,
            empresa = "Plaza Oriente",
            concepto = "Arriendo departamento octubre",
            numeroCuota = 10,
            totalCuotas = 12,
            monto = 520000,
            fechaVencimiento = "10-10-2026",
            estado = EstadoCompromiso.PENDIENTE,
            recargoAplicado = 0
        ),

        Compromiso(
            id = 102L,
            folio = "CP-2026-0102",
            idUsuario = 1L,
            idEmpresa = 6L,
            empresa = "Club Los Halcones",
            concepto = "Cuota social octubre",
            numeroCuota = 10,
            totalCuotas = 12,
            monto = 15000,
            fechaVencimiento = "05-10-2026",
            estado = EstadoCompromiso.VENCIDO,
            recargoAplicado = 2000
        ),

        Compromiso(
            id = 103L,
            folio = "CP-2026-0103",
            idUsuario = 1L,
            idEmpresa = 5L,
            empresa = "Manos Abiertas",
            concepto = "Aporte mensual octubre",
            numeroCuota = null,
            totalCuotas = null,
            monto = 10000,
            fechaVencimiento = "15-10-2026",
            estado = EstadoCompromiso.PENDIENTE,
            recargoAplicado = 0
        ),

        Compromiso(
            id = 104L,
            folio = "CP-2026-0104",
            idUsuario = 1L,
            idEmpresa = 3L,
            empresa = "Crédito Andino",
            concepto = "Crédito de consumo",
            numeroCuota = 7,
            totalCuotas = 24,
            monto = 86400,
            fechaVencimiento = "25-09-2026",
            estado = EstadoCompromiso.PAGADO,
            recargoAplicado = 0
        ),

        Compromiso(
            id = 105L,
            folio = "CP-2026-0105",
            idUsuario = 1L,
            idEmpresa = 3L,
            empresa = "Crédito Andino",
            concepto = "Crédito de consumo",
            numeroCuota = 8,
            totalCuotas = 24,
            monto = 86400,
            fechaVencimiento = "25-10-2026",
            estado = EstadoCompromiso.PENDIENTE,
            recargoAplicado = 0
        )
    )

    // Pagos anteriores del usuario
    val pagos = listOf(

        Pago(
            id = 398L,
            numeroComprobante = "CPR-000398",
            idCompromiso = 89L,
            fechaHora = "09-09-2026 13:05",
            montoPagado = 520000,
            canal = "Portal de pagos",
            medioPago = "Pago simulado"
        ),

        Pago(
            id = 402L,
            numeroComprobante = "CPR-000402",
            idCompromiso = 90L,
            fechaHora = "07-09-2026 18:40",
            montoPagado = 17000,
            canal = "Portal de pagos",
            medioPago = "Pago simulado"
        ),

        Pago(
            id = 405L,
            numeroComprobante = "CPR-000405",
            idCompromiso = 91L,
            fechaHora = "15-09-2026 08:22",
            montoPagado = 10000,
            canal = "Agente conversacional",
            medioPago = "Pago simulado"
        ),

        Pago(
            id = 451L,
            numeroComprobante = "CPR-000451",
            idCompromiso = 104L,
            fechaHora = "24-09-2026 10:42",
            montoPagado = 86400,
            canal = "Portal de pagos",
            medioPago = "Tarjeta de débito"
        )
    )
}