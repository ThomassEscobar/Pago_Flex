package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import androidx.core.util.PatternsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.almacen.AlmacenSesion
import cl.pagoflex.app.nucleo.almacen.SesionUsuario
import cl.pagoflex.app.nucleo.red.ApiAutenticacion
import cl.pagoflex.app.nucleo.dto.LoginRequestDto
import cl.pagoflex.app.nucleo.dto.OlvideRequestDto
import cl.pagoflex.app.nucleo.dto.RegistroRequestDto
import cl.pagoflex.app.nucleo.util.ValidadorRut
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val correo: String = "", val errorCorreo: String? = null,
    val contrasena: String = "", val errorContrasena: String? = null,
    val mostrarContrasena: Boolean = false,
    val nombre: String = "", val errorNombre: String? = null,
    val rut: String = "", val errorRut: String? = null,
    val confirmarContrasena: String = "", val errorConfirmarContrasena: String? = null,
    val rolSeleccionado: String = "USUARIO",
    val empresaIdSeleccionada: String? = null,

    val cargando: Boolean = false,
    val mensajeGlobalError: String? = null,
    val mensajeGlobalExito: String? = null,
    val loginExitoso: Boolean = false,
    val registroExitoso: Boolean = false,
    val correoRecuperacionEnviado: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiAutenticacion: ApiAutenticacion,
    private val almacenSesion: AlmacenSesion
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onCorreoChange(valor: String) {
        _uiState.update { it.copy(correo = valor, errorCorreo = if (!PatternsCompat.EMAIL_ADDRESS.matcher(valor).matches()) "Correo inválido" else null) }
    }

    fun onContrasenaChange(valor: String) {
        _uiState.update { it.copy(contrasena = valor, errorContrasena = if (valor.length < 6) "Mínimo 6 caracteres" else null) }
    }

    fun onNombreChange(valor: String) {
        _uiState.update { it.copy(nombre = valor, errorNombre = if (valor.isBlank()) "Obligatorio" else null) }
    }

    fun onConfirmarContrasenaChange(valor: String) {
        _uiState.update { it.copy(confirmarContrasena = valor, errorConfirmarContrasena = if (valor != it.contrasena) "No coinciden" else null) }
    }

    fun onRutChange(valor: String) {
        val formateado = ValidadorRut.formatearRut(valor)
        val error = if (!ValidadorRut.esRutValido(formateado)) "RUT inválido" else null
        _uiState.update { it.copy(rut = formateado, errorRut = error) }
    }

    fun onRolChange(rol: String) { _uiState.update { it.copy(rolSeleccionado = rol) } }
    fun onEmpresaSeleccionadaChange(idEmpresa: String) { _uiState.update { it.copy(empresaIdSeleccionada = idEmpresa) } }
    fun toggleMostrarContrasena() { _uiState.update { it.copy(mostrarContrasena = !it.mostrarContrasena) } }
    fun limpiarMensajes() { _uiState.update { it.copy(mensajeGlobalError = null, mensajeGlobalExito = null) } }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (estado.errorCorreo != null || estado.errorContrasena != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null, mensajeGlobalExito = null) }
            try {
                val respuesta = apiAutenticacion.iniciarSesion(LoginRequestDto(estado.correo.trim(), estado.contrasena))
                almacenSesion.guardarSesion(
                    token = respuesta.tokenAcceso,
                    usuario = SesionUsuario(
                        id = respuesta.usuario.id,
                        nombre = respuesta.usuario.nombreCompleto,
                        correo = respuesta.usuario.correo,
                        rol = respuesta.usuario.rol ?: "USUARIO",
                        rut = respuesta.usuario.rut ?: "",
                        empresaId = respuesta.usuario.empresaId
                    )
                )
                _uiState.update { it.copy(cargando = false, loginExitoso = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensajeGlobalError = "Credenciales incorrectas") }
            }
        }
    }

    fun registrarCuenta() {
        val estado = _uiState.value
        if (estado.errorNombre != null || estado.errorCorreo != null || estado.errorRut != null || estado.errorContrasena != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null) }
            try {
                val request = RegistroRequestDto(
                    nombreCompleto = estado.nombre,
                    correo = estado.correo,
                    contrasena = estado.contrasena,
                    rut = estado.rut,
                    rol = estado.rolSeleccionado,
                    empresaId = if (estado.rolSeleccionado == "EJECUTIVO") estado.empresaIdSeleccionada else null
                )
                apiAutenticacion.registrar(request)
                _uiState.update { it.copy(cargando = false, registroExitoso = true, mensajeGlobalExito = "Cuenta creada.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensajeGlobalError = "El correo ya existe o error de servidor") }
            }
        }
    }

    fun enviarCorreoRecuperacion(correo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                apiAutenticacion.olvidarContrasena(OlvideRequestDto(correo))
                _uiState.update { it.copy(cargando = false, correoRecuperacionEnviado = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensajeGlobalError = "Error al enviar correo") }
            }
        }
    }
}