package cl.pagoflex.app.nucleo.funcionalidades.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.red.ApiCompromisos
import cl.pagoflex.app.nucleo.dto.CompromisoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InicioUiState {
    data object Cargando : InicioUiState
    data class Exito(val compromisos: List<CompromisoDto>) : InicioUiState
    data class Error(val mensaje: String) : InicioUiState
}

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val apiCompromisos: ApiCompromisos
) : ViewModel() {

    private val _uiState = MutableStateFlow<InicioUiState>(InicioUiState.Cargando)
    val uiState: StateFlow<InicioUiState> = _uiState

    init {
        cargarCompromisos()
    }

    fun cargarCompromisos() {
        viewModelScope.launch {
            _uiState.value = InicioUiState.Cargando
            try {
                val lista = apiCompromisos.listarCompromisos()
                _uiState.value = InicioUiState.Exito(lista)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = InicioUiState.Error("Error al cargar compromisos: ${e.message}")
            }
        }
    }
}