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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountViewModelTest {

    private lateinit var app: Application
    private lateinit var fakeRepo: FakeUserRepository
    private lateinit var viewModel: AccountViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        fakeRepo = FakeUserRepository()
        viewModel = AccountViewModel(app, fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun changePassword_nuevaIgualActual_seteaErrorNew() {
        viewModel.changePassword(1L, "Password123!", "Password123!", "Password123!")

        assertEquals("La nueva contraseña debe ser distinta de la actual", viewModel.errorNew.value)
    }

    @Test
    fun updateProfile_correoDuplicado_seteaErrorEmail() {
        val user = UserEntity(id = 1L, nombre = "Test", email = "test@test.cl", rol = "USUARIO")
        fakeRepo.usuarioPorEmail = UserEntity(id = 2L, nombre = "Otro", email = "dup@test.cl", rol = "USUARIO")

        viewModel.updateProfile(user, "Test", "+56 9 1234 5678", "dup@test.cl", null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("El correo ya está registrado", viewModel.errorEmail.value)
    }
}
