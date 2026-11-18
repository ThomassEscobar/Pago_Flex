package cl.pagoflex.app.nucleo.funcionalidades.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.pagoflex.app.nucleo.dto.CompromisoDto
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PantallaInicio(
    viewModel: InicioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Mis Compromisos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val state = uiState) {
            is InicioUiState.Cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is InicioUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.mensaje, color = MaterialTheme.colorScheme.error)
                }
            }
            is InicioUiState.Exito -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.compromisos) { compromiso ->
                        TarjetaCompromiso(compromiso)
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaCompromiso(compromiso: CompromisoDto) {
    // Formatear el monto en pesos chilenos
    val formatoClp = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    val montoFormateado = formatoClp.format(compromiso.montoClp)

    val colorEstado = when (compromiso.estado) {
        "PAGADO" -> Color(0xFF16A34A) // Verde
        "VENCIDO" -> Color(0xFFDC2626) // Rojo
        else -> Color(0xFFF59E0B) // Amarillo (Pendiente)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = compromiso.organizacion.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = compromiso.estado,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorEstado,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = compromiso.descripcion,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = montoFormateado,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Vence: ${compromiso.fechaVencimiento}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}