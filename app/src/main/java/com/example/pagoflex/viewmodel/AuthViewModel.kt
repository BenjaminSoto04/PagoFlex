package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.PreguntasSeguridad
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.styles.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: UserRepository = RepositoryProvider.users(application)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Estados de error por campo en Login / Registro
    private val _errorNombre = MutableStateFlow<String?>(null)
    val errorNombre: StateFlow<String?> = _errorNombre.asStateFlow()

    private val _errorEmail = MutableStateFlow<String?>(null)
    val errorEmail: StateFlow<String?> = _errorEmail.asStateFlow()

    private val _errorPassword = MutableStateFlow<String?>(null)
    val errorPassword: StateFlow<String?> = _errorPassword.asStateFlow()

    private val _errorTelefono = MutableStateFlow<String?>(null)
    val errorTelefono: StateFlow<String?> = _errorTelefono.asStateFlow()

    private val _errorEmpresa = MutableStateFlow<String?>(null)
    val errorEmpresa: StateFlow<String?> = _errorEmpresa.asStateFlow()

    private val _errorPreguntaSeguridad = MutableStateFlow<String?>(null)
    val errorPreguntaSeguridad: StateFlow<String?> = _errorPreguntaSeguridad.asStateFlow()

    private val _errorRespuestaSeguridad = MutableStateFlow<String?>(null)
    val errorRespuestaSeguridad: StateFlow<String?> = _errorRespuestaSeguridad.asStateFlow()

    private val _errorGeneral = MutableStateFlow<String?>(null)
    val errorGeneral: StateFlow<String?> = _errorGeneral.asStateFlow()

    // Estados para Recuperación de Contraseña con Pregunta de Seguridad
    private val _preguntasRecuperacion = MutableStateFlow<List<String>>(
        PreguntasSeguridad.LISTA.shuffled().take(4)
    )
    val preguntasRecuperacion: StateFlow<List<String>> = _preguntasRecuperacion.asStateFlow()

    private var ultimoUsuarioRecuperacionId: Long? = null

    private val _errorRecuperacionEmail = MutableStateFlow<String?>(null)
    val errorRecuperacionEmail: StateFlow<String?> = _errorRecuperacionEmail.asStateFlow()

    private val _errorRecuperacionPregunta = MutableStateFlow<String?>(null)
    val errorRecuperacionPregunta: StateFlow<String?> = _errorRecuperacionPregunta.asStateFlow()

    private val _errorRecuperacionRespuesta = MutableStateFlow<String?>(null)
    val errorRecuperacionRespuesta: StateFlow<String?> = _errorRecuperacionRespuesta.asStateFlow()

    private val _errorNuevaPassword = MutableStateFlow<String?>(null)
    val errorNuevaPassword: StateFlow<String?> = _errorNuevaPassword.asStateFlow()

    private val _errorConfirmarPassword = MutableStateFlow<String?>(null)
    val errorConfirmarPassword: StateFlow<String?> = _errorConfirmarPassword.asStateFlow()

    private val _mensajeRecuperacion = MutableStateFlow<String?>(null)
    val mensajeRecuperacion: StateFlow<String?> = _mensajeRecuperacion.asStateFlow()

    fun login(email: String, password: String) {
        _errorNombre.value = null
        _errorEmail.value = Validaciones.correo(email)
        _errorPassword.value = if (password.isEmpty()) "Ingresa tu contraseña" else null
        _errorGeneral.value = null
        if (_errorEmail.value != null || _errorPassword.value != null) return

        viewModelScope.launch {
            _loading.value = true
            repo.login(email.trim(), password).onSuccess { user ->
                _currentUser.value = user
                SessionManager.save(getApplication(), user.id, user.rol)
            }.onFailure { e ->
                val mensaje = e.message.orEmpty()
                when {
                    mensaje.contains("correo", ignoreCase = true) ||
                            mensaje.contains("usuario", ignoreCase = true) ->
                        _errorEmail.value = "No existe una cuenta registrada con este correo"
                    mensaje.contains("contraseña", ignoreCase = true) ->
                        _errorPassword.value = "La contraseña es incorrecta"
                    else ->
                        _errorGeneral.value = mensaje.ifBlank { "No pudimos iniciar sesión. Inténtalo nuevamente." }
                }
            }
            _loading.value = false
        }
    }

    fun register(
        nombre: String,
        email: String,
        password: String,
        rol: String,
        telefono: String? = null,
        empresa: String? = null,
        preguntaSeguridad: String = "",
        respuestaSeguridad: String = ""
    ) {
        _errorNombre.value = Validaciones.nombre(nombre)
        _errorEmail.value = Validaciones.correo(email)
        _errorPassword.value = Validaciones.contrasena(password)
        _errorTelefono.value = Validaciones.telefono(telefono.orEmpty())
        _errorEmpresa.value = Validaciones.empresa(empresa.orEmpty())
        _errorPreguntaSeguridad.value = if (preguntaSeguridad.isBlank()) {
            "Selecciona una pregunta de seguridad"
        } else {
            null
        }
        _errorRespuestaSeguridad.value = if (respuestaSeguridad.trim().isEmpty()) {
            "Ingresa la respuesta a tu pregunta de seguridad"
        } else {
            null
        }
        _errorGeneral.value = null

        val hayErrores = listOf(
            _errorNombre.value,
            _errorEmail.value,
            _errorPassword.value,
            _errorTelefono.value,
            _errorEmpresa.value,
            _errorPreguntaSeguridad.value,
            _errorRespuestaSeguridad.value
        ).any { it != null }
        if (hayErrores) return

        viewModelScope.launch {
            _loading.value = true
            repo.register(
                nombre = nombre.trim(),
                email = email.trim(),
                password = password,
                rol = rol,
                telefono = telefono,
                empresa = empresa,
                preguntaSeguridad = preguntaSeguridad.trim(),
                respuestaSeguridad = respuestaSeguridad.trim()
            ).onSuccess { id ->
                val user = repo.getById(id)
                _currentUser.value = user
                if (user != null) SessionManager.save(getApplication(), user.id, user.rol)
            }.onFailure { e ->
                val mensaje = e.message.orEmpty()
                if (mensaje.contains("correo", ignoreCase = true)) {
                    _errorEmail.value = "Ya existe una cuenta registrada con este correo"
                } else {
                    _errorGeneral.value = mensaje.ifBlank { "No pudimos crear la cuenta. Inténtalo nuevamente." }
                }
            }
            _loading.value = false
        }
    }

    /**
     * Busca el usuario por correo/código y genera una lista aleatoria de preguntas
     * que incluye obligatoriamente la pregunta que ese usuario registró.
     */
    fun actualizarPreguntasParaUsuario(identificador: String) {
        val limpio = identificador.trim()
        if (limpio.isEmpty()) {
            ultimoUsuarioRecuperacionId = null
            _preguntasRecuperacion.value = PreguntasSeguridad.LISTA.shuffled().take(4)
            return
        }
        viewModelScope.launch {
            val user = repo.getByEmail(limpio)
            if (user != null) {
                if (ultimoUsuarioRecuperacionId != user.id) {
                    ultimoUsuarioRecuperacionId = user.id
                    val preguntaReal = user.preguntaSeguridad.ifBlank { PreguntasSeguridad.PREGUNTA_DEFAULT }
                    val otras = PreguntasSeguridad.LISTA
                        .filterNot { it.equals(preguntaReal, ignoreCase = true) }
                        .shuffled()
                        .take(3)
                    _preguntasRecuperacion.value = (otras + preguntaReal).shuffled()
                }
            } else {
                ultimoUsuarioRecuperacionId = null
            }
        }
    }

    fun verificarPreguntaSeguridad(
        identificador: String,
        preguntaSeleccionada: String,
        respuestaIngresada: String,
        onExito: (String) -> Unit
    ) {
        _errorRecuperacionEmail.value = if (identificador.trim().isEmpty()) {
            "Ingresa tu correo electrónico o usuario"
        } else {
            null
        }
        _errorRecuperacionPregunta.value = if (preguntaSeleccionada.isBlank()) {
            "Selecciona tu pregunta de seguridad"
        } else {
            null
        }
        _errorRecuperacionRespuesta.value = if (respuestaIngresada.trim().isEmpty()) {
            "Ingresa la respuesta de seguridad"
        } else {
            null
        }
        _errorGeneral.value = null

        if (
            _errorRecuperacionEmail.value != null ||
            _errorRecuperacionPregunta.value != null ||
            _errorRecuperacionRespuesta.value != null
        ) {
            return
        }

        viewModelScope.launch {
            _loading.value = true
            val user = repo.getByEmail(identificador.trim())
            if (user == null) {
                _errorRecuperacionEmail.value = "No encontramos una cuenta con este correo o usuario"
                _loading.value = false
                return@launch
            }

            // Asegura que la pregunta del usuario esté siempre en el selector
            if (_preguntasRecuperacion.value.none { it.equals(user.preguntaSeguridad, ignoreCase = true) }) {
                val preguntaReal = user.preguntaSeguridad.ifBlank { PreguntasSeguridad.PREGUNTA_DEFAULT }
                val otras = PreguntasSeguridad.LISTA
                    .filterNot { it.equals(preguntaReal, ignoreCase = true) }
                    .shuffled()
                    .take(3)
                _preguntasRecuperacion.value = (otras + preguntaReal).shuffled()
            }

            val preguntaCorrecta = preguntaSeleccionada.trim().equals(
                user.preguntaSeguridad.trim(),
                ignoreCase = true
            )
            if (!preguntaCorrecta) {
                _errorRecuperacionPregunta.value = "La pregunta seleccionada no coincide con la registrada en tu cuenta"
                _loading.value = false
                return@launch
            }

            val respuestaNormalizada = PreguntasSeguridad.normalizarRespuesta(respuestaIngresada)
            val respuestaGuardada = PreguntasSeguridad.normalizarRespuesta(user.respuestaSeguridad)

            if (respuestaNormalizada != respuestaGuardada) {
                _errorRecuperacionRespuesta.value = "La respuesta ingresada no es correcta"
                _loading.value = false
                return@launch
            }

            _loading.value = false
            onExito(user.email)
        }
    }

    fun guardarNuevaPasswordRecuperada(
        email: String,
        nuevaPassword: String,
        confirmarPassword: String,
        onExito: () -> Unit = {}
    ) {
        _errorNuevaPassword.value = Validaciones.contrasena(nuevaPassword)
        _errorConfirmarPassword.value = Validaciones.confirmacion(nuevaPassword, confirmarPassword)
        _errorGeneral.value = null
        _mensajeRecuperacion.value = null

        if (_errorNuevaPassword.value != null || _errorConfirmarPassword.value != null) return

        viewModelScope.launch {
            _loading.value = true
            repo.resetPassword(email.trim(), nuevaPassword).onSuccess {
                _mensajeRecuperacion.value = "Tu contraseña ha sido actualizada correctamente."
                onExito()
            }.onFailure { e ->
                _errorGeneral.value = e.message ?: "No fue posible actualizar la contraseña."
            }
            _loading.value = false
        }
    }

    fun logout() {
        _currentUser.value = null
        SessionManager.clear(getApplication())
    }

    fun seleccionarSesion(user: UserEntity) {
        _currentUser.value = user
        SessionManager.save(getApplication(), user.id, user.rol)
        limpiarErrores()
    }

    fun refreshUser() {
        val id = SessionManager.userId(getApplication())
        if (id > 0) viewModelScope.launch { _currentUser.value = repo.getById(id) }
    }

    fun limpiarErrorNombre() { _errorNombre.value = null }
    fun limpiarErrorEmail() { _errorEmail.value = null }
    fun limpiarErrorPassword() { _errorPassword.value = null }
    fun limpiarErrorTelefono() { _errorTelefono.value = null }
    fun limpiarErrorEmpresa() { _errorEmpresa.value = null }
    fun limpiarErrorPreguntaSeguridad() { _errorPreguntaSeguridad.value = null }
    fun limpiarErrorRespuestaSeguridad() { _errorRespuestaSeguridad.value = null }
    fun limpiarErrorGeneral() { _errorGeneral.value = null }

    fun limpiarErrorRecuperacionEmail() { _errorRecuperacionEmail.value = null }
    fun limpiarErrorRecuperacionPregunta() { _errorRecuperacionPregunta.value = null }
    fun limpiarErrorRecuperacionRespuesta() { _errorRecuperacionRespuesta.value = null }
    fun limpiarErroresNuevaPassword() {
        _errorNuevaPassword.value = null
        _errorConfirmarPassword.value = null
        _errorGeneral.value = null
    }

    fun limpiarEstadoRecuperacion() {
        ultimoUsuarioRecuperacionId = null
        _preguntasRecuperacion.value = PreguntasSeguridad.LISTA.shuffled().take(4)
        _errorRecuperacionEmail.value = null
        _errorRecuperacionPregunta.value = null
        _errorRecuperacionRespuesta.value = null
        _errorNuevaPassword.value = null
        _errorConfirmarPassword.value = null
        _errorGeneral.value = null
        _mensajeRecuperacion.value = null
    }

    fun limpiarErrores() {
        _errorNombre.value = null
        _errorEmail.value = null
        _errorPassword.value = null
        _errorTelefono.value = null
        _errorEmpresa.value = null
        _errorPreguntaSeguridad.value = null
        _errorRespuestaSeguridad.value = null
        _errorGeneral.value = null
    }
}