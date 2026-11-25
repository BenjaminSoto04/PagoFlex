package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.model.ApiEmpresaRegistroRequest
import com.example.pagoflex.data.model.ApiEmpresaResponse
import com.example.pagoflex.data.remote.EmpresasApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch

class EmpresaRepositoryImpl(
    private val apiService: EmpresasApiService = RetrofitClient.obtenerEmpresas()
) : EmpresaRepository {

    override fun getAll(): Flow<List<EmpresaEntity>> = flow {
        val response = apiService.listarTodos()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.map { it.toEntity() })
        } else {
            emit(emptyList())
        }
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(emptyList())
    }

    override suspend fun getFirst(): EmpresaEntity? {
        return try {
            val response = apiService.listarTodos()
            if (response.isSuccessful) response.body()?.firstOrNull()?.toEntity() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
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



