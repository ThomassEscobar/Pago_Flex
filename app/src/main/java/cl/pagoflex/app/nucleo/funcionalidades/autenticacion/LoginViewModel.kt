package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.dto.LoginRequestDto
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import cl.pagoflex.app.nucleo.local.repositorio.PagoFlexRepository
import cl.pagoflex.app.nucleo.red.ApiAutenticacion
import cl.pagoflex.app.nucleo.util.ValidadorRut
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val rut: String = "",
    val errorRut: String? = null,
    val confirmarContrasena: String = "",
    val errorConfirmarContrasena: String? = null,
    val rolSeleccionado: String = "USUARIO",

    // Datos de la empresa (cuando rolSeleccionado == "EJECUTIVO")
    val nombreEmpresa: String = "",
    val errorNombreEmpresa: String? = null,
    val tipoEmpresa: String = "Empresa",
    val rutEmpresa: String = "",
    val errorRutEmpresa: String? = null,
    val correoEmpresa: String = "",
    val errorCorreoEmpresa: String? = null,

    val cargando: Boolean = false,
    val mensajeGlobalError: String? = null,
    val mensajeGlobalExito: String? = null,
    val usuarioAutenticado: UsuarioEntity? = null,
    val registroExitoso: Boolean = false,
    val recuperacionExitosa: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiAutenticacion: ApiAutenticacion,
    private val repository: PagoFlexRepository,
    private val organizacionDao: OrganizacionDao
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

    fun onRutChange(valor: String) {
        val formateado = ValidadorRut.formatearRut(valor)
        val error = when {
            formateado.isBlank() -> "El RUT es obligatorio"
            !ValidadorRut.esRutValido(formateado) -> "RUT inválido (ej: 12.345.678-9)"
            else -> null
        }
        _uiState.update { it.copy(rut = formateado, errorRut = error) }
    }

    fun onConfirmarContrasenaChange(valor: String) {
        val error = if (valor != _uiState.value.contrasena) "Las contraseñas no coinciden" else null
        _uiState.update { it.copy(confirmarContrasena = valor, errorConfirmarContrasena = error) }
    }

    fun onRolChange(rol: String) {
        _uiState.update {
            it.copy(
                rolSeleccionado = rol,
                errorNombreEmpresa = null,
                errorRutEmpresa = null,
                errorCorreoEmpresa = null
            )
        }
    }

    fun onNombreEmpresaChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El nombre de la empresa es obligatorio"
            valor.trim().length < 3 -> "Ingresa al menos 3 caracteres"
            else -> null
        }
        _uiState.update { it.copy(nombreEmpresa = valor, errorNombreEmpresa = error) }
    }

    fun onTipoEmpresaChange(tipo: String) {
        _uiState.update { it.copy(tipoEmpresa = tipo) }
    }

    fun onRutEmpresaChange(valor: String) {
        val formateado = ValidadorRut.formatearRut(valor)
        val error = when {
            formateado.isBlank() -> "El RUT de la empresa es obligatorio"
            !ValidadorRut.esRutValido(formateado) -> "RUT de empresa inválido (ej: 76.123.456-7)"
            else -> null
        }
        _uiState.update { it.copy(rutEmpresa = formateado, errorRutEmpresa = error) }
    }

    fun onCorreoEmpresaChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El correo de contacto es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches() -> "Formato de correo inválido"
            else -> null
        }
        _uiState.update { it.copy(correoEmpresa = valor, errorCorreoEmpresa = error) }
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
                }

                val usuarioLocal = repository.iniciarSesion(actualizado.correo.trim(), actualizado.contrasena)
                if (usuarioLocal != null) {
                    val prefsBloqueo = context.getSharedPreferences("pagoflex_seguridad_global", Context.MODE_PRIVATE)
                    val motivoSuspension = prefsBloqueo.getString("bloqueo_motivo_${usuarioLocal.id}", null)
                    if (!motivoSuspension.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                cargando = false,
                                mensajeGlobalError = "Cuenta suspendida de PagoFlex: $motivoSuspension"
                            )
                        }
                    } else {
                        _uiState.update { it.copy(cargando = false, usuarioAutenticado = usuarioLocal) }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeGlobalError = "Correo o contraseña incorrectos. Verifica tus datos e intenta nuevamente."
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
        onRutChange(estado.rut)
        onCorreoChange(estado.correo)
        onContrasenaChange(estado.contrasena)
        onConfirmarContrasenaChange(estado.confirmarContrasena)

        if (estado.rolSeleccionado == "EJECUTIVO") {
            onNombreEmpresaChange(estado.nombreEmpresa)
            onRutEmpresaChange(estado.rutEmpresa)
            onCorreoEmpresaChange(estado.correoEmpresa)
        }

        val act = _uiState.value
        if (act.errorNombre != null ||
            act.errorRut != null ||
            act.errorCorreo != null ||
            act.errorContrasena != null ||
            act.errorConfirmarContrasena != null
        ) return

        if (act.rolSeleccionado == "EJECUTIVO" &&
            (act.errorNombreEmpresa != null || act.errorRutEmpresa != null || act.errorCorreoEmpresa != null)
        ) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null) }

            val empresaIdFinal: Int? = if (act.rolSeleccionado == "EJECUTIVO") {
                val rutEmpresaFormateado = ValidadorRut.formatearRut(act.rutEmpresa)
                val orgsExistentes = repository.observarOrganizaciones().first()
                val yaRegistrada = orgsExistentes.find {
                    it.rut.equals(rutEmpresaFormateado, ignoreCase = true)
                }

                if (yaRegistrada != null) {
                    yaRegistrada.id
                } else {
                    organizacionDao.insertar(
                        OrganizacionEntity(
                            nombre = act.nombreEmpresa.trim(),
                            tipo = act.tipoEmpresa,
                            rut = rutEmpresaFormateado,
                            correoContacto = act.correoEmpresa.trim()
                        )
                    )
                    val orgsActualizadas = repository.observarOrganizaciones().first()
                    orgsActualizadas.find {
                        it.rut.equals(rutEmpresaFormateado, ignoreCase = true)
                    }?.id ?: orgsActualizadas.maxByOrNull { it.id }?.id
                }
            } else {
                null
            }

            val nuevoUsuario = UsuarioEntity(
                nombre = act.nombre.trim(),
                correo = act.correo.trim(),
                clave = act.contrasena,
                rol = act.rolSeleccionado,
                rut = ValidadorRut.formatearRut(act.rut),
                empresaId = empresaIdFinal
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