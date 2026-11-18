package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.dto.LoginRequestDto
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import cl.pagoflex.app.nucleo.local.repositorio.PagoFlexRepository
import cl.pagoflex.app.nucleo.red.ApiAutenticacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val correo: String = "",
    val errorCorreo: String? = null,
    val contrasena: String = "",
    val errorContrasena: String? = null,
    val mostrarContrasena: Boolean = false,
    val nombre: String = "",
    val errorNombre: String? = null,
    val confirmarContrasena: String = "",
    val errorConfirmarContrasena: String? = null,
    val rolSeleccionado: String = "USUARIO",
    val cargando: Boolean = false,
    val mensajeGlobalError: String? = null,
    val mensajeGlobalExito: String? = null,
    val usuarioAutenticado: UsuarioEntity? = null,
    val registroExitoso: Boolean = false,
    val recuperacionExitosa: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiAutenticacion: ApiAutenticacion,
    private val repository: PagoFlexRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onCorreoChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El correo es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches() -> "Formato de correo inválido (ej: usuario@dominio.cl)"
            else -> null
        }
        _uiState.update { it.copy(correo = valor, errorCorreo = error, mensajeGlobalError = null) }
    }

    fun onContrasenaChange(valor: String) {
        val error = when {
            valor.isBlank() -> "La contraseña es obligatoria"
            valor.length < 6 -> "Debe tener al menos 6 caracteres"
            else -> null
        }
        _uiState.update { it.copy(contrasena = valor, errorContrasena = error, mensajeGlobalError = null) }
    }

    fun onNombreChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El nombre completo es obligatorio"
            valor.trim().length < 3 -> "Ingresa al menos 3 caracteres"
            else -> null
        }
        _uiState.update { it.copy(nombre = valor, errorNombre = error) }
    }

    fun onConfirmarContrasenaChange(valor: String) {
        val error = if (valor != _uiState.value.contrasena) "Las contraseñas no coinciden" else null
        _uiState.update { it.copy(confirmarContrasena = valor, errorConfirmarContrasena = error) }
    }

    fun onRolChange(rol: String) {
        _uiState.update { it.copy(rolSeleccionado = rol) }
    }

    fun toggleMostrarContrasena() {
        _uiState.update { it.copy(mostrarContrasena = !it.mostrarContrasena) }
    }

    fun limpiarMensajes() {
        _uiState.update {
            it.copy(
                mensajeGlobalError = null,
                mensajeGlobalExito = null,
                registroExitoso = false,
                recuperacionExitosa = false
            )
        }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        onCorreoChange(estado.correo)
        onContrasenaChange(estado.contrasena)

        val actualizado = _uiState.value
        if (actualizado.errorCorreo != null || actualizado.errorContrasena != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null) }
            try {
                try {
                    apiAutenticacion.iniciarSesion(LoginRequestDto(actualizado.correo.trim(), actualizado.contrasena))
                } catch (_: Exception) {
                    // Respaldo offline-first con Room
                }

                val usuarioLocal = repository.iniciarSesion(actualizado.correo.trim(), actualizado.contrasena)
                if (usuarioLocal != null) {
                    _uiState.update { it.copy(cargando = false, usuarioAutenticado = usuarioLocal) }
                } else {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeGlobalError = "Credenciales incorrectas. Prueba con usuario@pagoflex.cl / 123456 o crea una cuenta."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensajeGlobalError = e.message ?: "Error al iniciar sesión") }
            }
        }
    }

    fun registrarCuenta() {
        val estado = _uiState.value
        onNombreChange(estado.nombre)
        onCorreoChange(estado.correo)
        onContrasenaChange(estado.contrasena)
        onConfirmarContrasenaChange(estado.confirmarContrasena)

        val act = _uiState.value
        if (act.errorNombre != null || act.errorCorreo != null || act.errorContrasena != null || act.errorConfirmarContrasena != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null) }
            val nuevoUsuario = UsuarioEntity(
                nombre = act.nombre.trim(),
                correo = act.correo.trim(),
                clave = act.contrasena,
                rol = act.rolSeleccionado
            )
            val resultado = repository.registrarUsuario(nuevoUsuario)
            resultado.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            registroExitoso = true,
                            mensajeGlobalExito = "Cuenta creada exitosamente."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(cargando = false, mensajeGlobalError = error.message) }
                }
            )
        }
    }

    fun restablecerContrasena() {
        val estado = _uiState.value
        onCorreoChange(estado.correo)
        onContrasenaChange(estado.contrasena)
        onConfirmarContrasenaChange(estado.confirmarContrasena)

        val act = _uiState.value
        if (act.errorCorreo != null || act.errorContrasena != null || act.errorConfirmarContrasena != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null) }
            val actualizado = repository.recuperarClave(act.correo.trim(), act.contrasena)
            if (actualizado) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        recuperacionExitosa = true,
                        mensajeGlobalExito = "Contraseña actualizada correctamente."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        mensajeGlobalError = "No existe ninguna cuenta asociada a ese correo."
                    )
                }
            }
        }
    }
}