package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.model.ApiEmpresaRegistroRequest
import com.example.pagoflex.data.model.ApiEmpresaResponse
import com.example.pagoflex.data.remote.EmpresasApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Repositorio de Empresas.
 *
 * Estrategia de integracion:
 * 1. Lecturas: intenta el microservicio de empresas (`http://10.0.2.2:8085`),
 *    guarda/actualiza en Room lo que llega del backend y, si el servicio no
 *    responde, devuelve los datos locales de Room.
 * 2. Escrituras: se persisten siempre en Room (fuente de verdad offline) y se
 *    replican al backend de forma best-effort.
 */
class EmpresaRepositoryImpl(
    private val db: AppDatabase,
    private val apiService: EmpresasApiService = RetrofitClient.obtenerEmpresas()
) : EmpresaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getAll(): Flow<List<EmpresaEntity>> =
        db.empresaDao().getAll().onStart { refrescarEnSegundoPlano { sincronizar() } }

    override suspend fun getFirst(): EmpresaEntity? {
        refrescarEnSegundoPlano { sincronizar() }
        return try {
            db.empresaDao().getFirst()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getCodigo(codigo: String): EmpresaEntity? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerPorCodigo(codigo.trim()) }
        if (remota != null) {
            db.empresaDao().upsert(remota.toEntity())
            return remota.toEntity()
        }
        return try {
            db.empresaDao().getByCodigo(codigo.trim())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getByRut(rut: String): EmpresaEntity? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerPorRut(rut.trim()) }
        if (remota != null) {
            db.empresaDao().upsert(remota.toEntity())
            return remota.toEntity()
        }
        return try {
            db.empresaDao().getByRut(rut.trim())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getById(id: Long): EmpresaEntity? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerPorId(id) }
        if (remota != null) {
            db.empresaDao().upsert(remota.toEntity())
            return remota.toEntity()
        }
        return try {
            db.empresaDao().getById(id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun insert(empresa: EmpresaEntity) {
        try {
            db.empresaDao().insert(empresa)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Replica best-effort: si el backend no responde, Room conserva el dato.
        scope.launch { ejecutarRemoto { apiService.crear(empresa.toRequest()) } }
    }

    override suspend fun update(empresa: EmpresaEntity) {
        try {
            db.empresaDao().update(empresa)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        scope.launch { ejecutarRemoto { apiService.actualizar(empresa.id, empresa.toRequest()) } }
    }

    override suspend fun delete(id: Long) {
        try {
            db.empresaDao().deleteById(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        scope.launch { ejecutarRemoto { apiService.eliminar(id) } }
    }

    private fun refrescarEnSegundoPlano(bloque: suspend () -> Unit) {
        scope.launch { bloque() }
    }

    /** Trae el listado del microservicio y lo persiste en Room (fallback silencioso). */
    private suspend fun sincronizar() {
        val remotas = ejecutarRemoto { apiService.listarTodos() } ?: return
        try {
            db.empresaDao().upsertAll(remotas.map { it.toEntity() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ApiEmpresaResponse.toEntity() = EmpresaEntity(
        id = id,
        codigo = codigo,
        razonSocial = razonSocial,
        nombreFantasia = nombreFantasia,
        rut = rut,
        rubro = rubro,
        email = email,
        telefono = telefono,
        recargo = recargo
    )

    private fun EmpresaEntity.toRequest() = ApiEmpresaRegistroRequest(
        codigo = codigo,
        razonSocial = razonSocial,
        nombreFantasia = nombreFantasia,
        rut = rut,
        rubro = rubro,
        email = email,
        telefono = telefono,
        recargo = recargo
    )
}
