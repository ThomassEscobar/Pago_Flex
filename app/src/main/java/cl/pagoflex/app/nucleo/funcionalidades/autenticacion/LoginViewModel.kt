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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
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

    // Empresas asociadas (ordenadas alfabéticamente) y validación de Ejecutivo
    val empresasDisponibles: List<OrganizacionEntity> = emptyList(),
    val empresaSeleccionada: OrganizacionEntity? = null,
    val errorEmpresaSeleccionada: String? = null,
    val rutEmpresa: String = "",
    val errorRutEmpresa: String? = null,
    val correoEmpresa: String = "",
    val errorCorreoEmpresa: String? = null,

    val cargando: Boolean = false,
    val mensajeGlobalError: String? = null,
    val mensajeGlobalExito: String? = null,
    val usuarioAutenticado: UsuarioEntity? = null,
    val registroExitoso: Boolean = false,
    val correoRecuperacionEnviado: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiAutenticacion: ApiAutenticacion,
    private val repository: PagoFlexRepository,
    private val organizacionDao: OrganizacionDao
) : ViewModel() {

    companion object {
        private val ROLES_REGISTRO_PUBLICO = setOf("USUARIO", "EJECUTIVO")
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observarEmpresasOrdenadas()
    }

    private fun observarEmpresasOrdenadas() {
        viewModelScope.launch {
            repository.observarOrganizaciones().collect { lista ->
                val ordenadas = lista.sortedBy { it.nombre.lowercase(Locale.getDefault()) }
                _uiState.update { estado ->
                    val seleccionadaActualizada = estado.empresaSeleccionada?.let { sel ->
                        ordenadas.find { it.id == sel.id }
                    }
                    estado.copy(
                        empresasDisponibles = ordenadas,
                        empresaSeleccionada = seleccionadaActualizada
                    )
                }
            }
        }
    }

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
        val rolSeguro = if (rol in ROLES_REGISTRO_PUBLICO) rol else "USUARIO"
        _uiState.update {
            it.copy(
                rolSeleccionado = rolSeguro,
                errorEmpresaSeleccionada = null,
                errorRutEmpresa = null,
                errorCorreoEmpresa = null,
                mensajeGlobalError = null
            )
        }
    }

    fun onEmpresaSeleccionadaChange(empresa: OrganizacionEntity) {
        _uiState.update {
            it.copy(
                empresaSeleccionada = empresa,
                errorEmpresaSeleccionada = null,
                mensajeGlobalError = null
            )
        }
        val estadoActual = _uiState.value
        if (estadoActual.rutEmpresa.isNotBlank()) {
            onRutEmpresaChange(estadoActual.rutEmpresa)
        }
        if (estadoActual.correoEmpresa.isNotBlank()) {
            onCorreoEmpresaChange(estadoActual.correoEmpresa)
        }
    }

    fun onRutEmpresaChange(valor: String) {
        val formateado = ValidadorRut.formatearRut(valor)
        val empresa = _uiState.value.empresaSeleccionada
        val error = when {
            formateado.isBlank() -> "El RUT de la empresa es obligatorio"
            !ValidadorRut.esRutValido(formateado) -> "RUT de empresa inválido (ej: 76.123.456-0)"
            empresa != null && !ValidadorRut.formatearRut(empresa.rut).equals(formateado, ignoreCase = true) ->
                "El RUT no corresponde a ${empresa.nombre}"
            else -> null
        }
        _uiState.update { it.copy(rutEmpresa = formateado, errorRutEmpresa = error) }
    }

    fun onCorreoEmpresaChange(valor: String) {
        val correoTrim = valor.trim()
        val empresa = _uiState.value.empresaSeleccionada
        val error = when {
            correoTrim.isBlank() -> "El correo institucional es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(correoTrim).matches() -> "Formato de correo inválido"
            empresa != null && !empresa.correoContacto.trim().equals(correoTrim, ignoreCase = true) ->
                "El correo institucional no corresponde a ${empresa.nombre}"
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
                correoRecuperacionEnviado = false
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

        if (estado.rolSeleccionado !in ROLES_REGISTRO_PUBLICO) {
            _uiState.update {
                it.copy(
                    rolSeleccionado = "USUARIO",
                    mensajeGlobalError = "El rol seleccionado no está habilitado para registro público."
                )
            }
            return
        }

        onNombreChange(estado.nombre)
        onRutChange(estado.rut)
        onCorreoChange(estado.correo)
        onContrasenaChange(estado.contrasena)
        onConfirmarContrasenaChange(estado.confirmarContrasena)

        if (estado.rolSeleccionado == "EJECUTIVO") {
            if (estado.empresaSeleccionada == null) {
                _uiState.update { it.copy(errorEmpresaSeleccionada = "Selecciona la empresa a la que perteneces") }
            }
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

        val rolFinal = if (act.rolSeleccionado == "EJECUTIVO") "EJECUTIVO" else "USUARIO"

        val empresaIdFinal: Int? = if (rolFinal == "EJECUTIVO") {
            val empresa = act.empresaSeleccionada ?: return
            if (act.errorEmpresaSeleccionada != null ||
                act.errorRutEmpresa != null ||
                act.errorCorreoEmpresa != null
            ) return

            val rutCoincide = ValidadorRut.formatearRut(empresa.rut)
                .equals(ValidadorRut.formatearRut(act.rutEmpresa), ignoreCase = true)
            val correoCoincide = empresa.correoContacto.trim()
                .equals(act.correoEmpresa.trim(), ignoreCase = true)

            if (!rutCoincide || !correoCoincide) {
                _uiState.update {
                    it.copy(
                        errorRutEmpresa = if (!rutCoincide) "El RUT no corresponde a ${empresa.nombre}" else null,
                        errorCorreoEmpresa = if (!correoCoincide) "El correo institucional no corresponde a ${empresa.nombre}" else null
                    )
                }
                return
            }
            empresa.id
        } else {
            null
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null) }

            val nuevoUsuario = UsuarioEntity(
                nombre = act.nombre.trim(),
                correo = act.correo.trim(),
                clave = act.contrasena,
                rol = rolFinal,
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

    fun enviarCorreoRecuperacion() {
        val estado = _uiState.value
        onCorreoChange(estado.correo)

        val act = _uiState.value
        if (act.errorCorreo != null) return

        val correoDestino = act.correo.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeGlobalError = null) }
            try {
                try {
                    apiAutenticacion.solicitarRecuperacionClave(mapOf("correo" to correoDestino))
                } catch (_: Exception) {
                }

                val existeCuenta = repository.solicitarRecuperacionPorCorreo(correoDestino)
                if (existeCuenta) {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            correoRecuperacionEnviado = true,
                            mensajeGlobalExito = "Se ha enviado un enlace de recuperación de contraseña a $correoDestino."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeGlobalError = "No existe ninguna cuenta registrada con ese correo electrónico."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        mensajeGlobalError = e.message ?: "Error al procesar la solicitud de recuperación."
                    )
                }
            }
        }
    }
}