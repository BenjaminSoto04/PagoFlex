package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.repository.ChatRepository
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.data.repository.ReporteRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import com.example.pagoflex.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardAgenteViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepo: ChatRepository = RepositoryProvider.chat(application)
    private val userRepo: UserRepository = RepositoryProvider.users(application)
    private val reporteRepo: ReporteRepository = RepositoryProvider.reportes(application)
    private val agenteId = SessionManager.userId(application)

    data class Resumen(
        val solicitudesActivas: Int = 0,
        val solicitudesPendientes: Int = 0,
        val reportesTotales: Int = 0,
        val reportesAsignados: Int = 0,
        val usuariosAsignados: Int = 0
    )

    data class Estado(
        val resumen: Resumen = Resumen(),
        val reportes: List<Reporte> = emptyList(),
        val nombresUsuarios: Map<Long, String> = emptyMap(),
        val usuariosAsignados: Set<Long> = emptySet()
    )

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userRepo.getAll(),
                chatRepo.getAllConversaciones(),
                reporteRepo.getAll()
            ) { usuarios, conversaciones, reportes ->
                val asignados = usuarios
                    .filter { it.rol == "USUARIO" && it.agenteAsignadoId == agenteId }
                    .map { it.id }
                    .toSet()

                val conversacionesAsignadas = conversaciones.filter { it.idUsuario in asignados }
                val activas = conversacionesAsignadas.count {
                    !it.estado.equals("FINALIZADO", ignoreCase = true)
                }
                val pendientes = conversacionesAsignadas.count {
                    it.estado.equals("PENDIENTE", ignoreCase = true)
                }
                val reportesAsignados = reportes.count { it.idUsuario in asignados }

                Estado(
                    resumen = Resumen(
                        solicitudesActivas = activas,
                        solicitudesPendientes = pendientes,
                        reportesTotales = reportes.size,
                        reportesAsignados = reportesAsignados,
                        usuariosAsignados = asignados.size
                    ),
                    reportes = reportes,
                    nombresUsuarios = usuarios.associate { it.id to it.nombre },
                    usuariosAsignados = asignados
                )
            }.collect { _estado.value = it }
        }
    }
}
