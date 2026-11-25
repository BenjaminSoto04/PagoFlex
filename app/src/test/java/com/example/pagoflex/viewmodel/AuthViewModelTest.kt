package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.pagoflex.data.FakeUserRepository
import com.example.pagoflex.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuthViewModelTest {

    private lateinit var app: Application
    private lateinit var fakeRepo: FakeUserRepository
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        fakeRepo = FakeUserRepository()
        viewModel = AuthViewModel(app, fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_emailInvalido_noLlamaRepo() {
        viewModel.login("correo-invalido", "Password123!")

        assertFalse(fakeRepo.loginLlamado)
        assertEquals("Ingresa un correo válido, por ejemplo nombre@dominio.cl", viewModel.errorEmail.value)
    }

    @Test
    fun login_cuentaDeshabilitada_seteaFlag() {
        fakeRepo.loginResult = Result.success(
            UserEntity(id = 1L, nombre = "Test", email = "test@test.cl", rol = "USUARIO", activo = false)
        )

        viewModel.login("test@test.cl", "Password123!")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.cuentaDeshabilitada.value)
        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun register_datosValidos_registraYGuardaSesion() {
        fakeRepo.registerResult = Result.success(1L)
        fakeRepo.usuarioGetById = UserEntity(id = 1L, nombre = "Test", email = "test@test.cl", rol = "USUARIO")

        viewModel.register(
            nombre = "Test",
            email = "test@test.cl",
            password = "Password123!",
            rol = "USUARIO",
            preguntaSeguridad = "¿Cuál es el nombre de tu primera mascota?",
            respuestaSeguridad = "perro"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.currentUser.value)
        assertEquals("test@test.cl", viewModel.currentUser.value?.email)
    }
}
