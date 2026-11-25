package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.model.ApiEmpresaRegistroRequest
import com.example.pagoflex.data.model.ApiEmpresaResponse
import com.example.pagoflex.data.remote.EmpresasApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class EmpresaRepositoryImpl(
    private val apiService: EmpresasApiService = RetrofitClient.obtenerEmpresas()
) : EmpresaRepository {

    private fun empresasPredeterminadas() = listOf(
        EmpresaEntity(1, "EMP-001", "Enel Distribución Chile S.A.", "Enel", "96.800.570-7", "Electricidad", "contacto@enel.cl", "6006960000", 0),
        EmpresaEntity(2, "EMP-002", "Aguas Andinas S.A.", "Aguas Andinas", "61.808.000-5", "Agua Potable", "clientes@aguasandinas.cl", "227312400", 0),
        EmpresaEntity(3, "EMP-003", "Movistar Chile S.A.", "Movistar", "96.568.740-1", "Telecomunicaciones", "soporte@movistar.cl", "6006003000", 0),
        EmpresaEntity(4, "EMP-004", "Metrogas S.A.", "Metrogas", "96.722.460-K", "Gas Natural", "atencion@metrogas.cl", "6003378000", 0)
    )

    override fun getAll(): Flow<List<EmpresaEntity>> = flow {
        val response = apiService.listarTodos()
        val remotas = if (response.isSuccessful && response.body() != null) {
            response.body()!!.map { it.toEntity() }
        } else {
            emptyList()
        }
        if (remotas.isNotEmpty()) {
            emit(remotas)
        } else {
            val creadas = empresasPredeterminadas().mapNotNull { emp ->
                try {
                    val res = apiService.crear(emp.toRequest())
                    if (res.isSuccessful) res.body()?.toEntity() else emp
                } catch (_: Exception) {
                    emp
                }
            }
            emit(creadas)
        }
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(empresasPredeterminadas())
    }

    override suspend fun getFirst(): EmpresaEntity? {
        return try {
            val response = apiService.listarTodos()
            val primera = if (response.isSuccessful) response.body()?.firstOrNull()?.toEntity() else null
            primera ?: empresasPredeterminadas().firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            empresasPredeterminadas().firstOrNull()
        }
    }

    override suspend fun getCodigo(codigo: String): EmpresaEntity? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerPorCodigo(codigo.trim()) }
        return remota?.toEntity()
    }

    override suspend fun getByRut(rut: String): EmpresaEntity? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerPorRut(rut.trim()) }
        return remota?.toEntity()
    }

    override suspend fun getById(id: Long): EmpresaEntity? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerPorId(id) }
        return remota?.toEntity()
    }

    override suspend fun insert(empresa: EmpresaEntity) {
        val response = apiService.crear(empresa.toRequest())
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo registrar la empresa en el backend")
        }
    }

    override suspend fun update(empresa: EmpresaEntity) {
        val response = apiService.actualizar(empresa.id, empresa.toRequest())
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo actualizar la empresa en el backend")
        }
    }

    override suspend fun delete(id: Long) {
        val response = apiService.eliminar(id)
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo eliminar la empresa en el backend")
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
