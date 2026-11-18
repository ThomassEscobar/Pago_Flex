package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.red.ApiAutenticacion
import cl.pagoflex.app.nucleo.dto.LoginRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Inicial : LoginUiState
    data object Cargando : LoginUiState
    data object Exito : LoginUiState
    data class Error(val mensaje: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiAutenticacion: ApiAutenticacion
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Inicial)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun iniciarSesion(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            _uiState.value = LoginUiState.Error("Por favor, completa todos los campos")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Cargando
            try {
                val respuesta = apiAutenticacion.iniciarSesion(LoginRequestDto(correo, contrasena))
                // Aquí luego guardaremos el token en DataStore
                println("Token recibido: ${respuesta.tokenAcceso}")
                _uiState.value = LoginUiState.Exito
            } catch (e: Exception) {
                e.printStackTrace() // Esto imprimirá el error exacto en Logcat
                _uiState.value = LoginUiState.Error(e.message ?: "Error de red o credenciales")
            }
        }
    }
}