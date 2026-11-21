package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecuperarClave(
    onVolverALogin: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar Contraseña") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.limpiarMensajes()
                        onVolverALogin()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.correoRecuperacionEnviado) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Correo de recuperación enviado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hemos enviado un enlace seguro de restablecimiento de contraseña a ${uiState.correo.trim()}. Abre el mensaje desde tu bandeja de entrada para cambiar tu contraseña.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val intentCorreo = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_APP_EMAIL)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intentCorreo)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir aplicación de correo")
                }

                Button(
                    onClick = {
                        viewModel.limpiarMensajes()
                        onVolverALogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Volver a Iniciar Sesión")
                }
            } else {
                Text(
                    text = "Por seguridad, te enviaremos un correo electrónico con un enlace único para restablecer tu contraseña directamente desde tu bandeja de entrada.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = uiState.correo,
                    onValueChange = { viewModel.onCorreoChange(it) },
                    label = { Text("Correo electrónico registrado") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.errorCorreo != null) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    isError = uiState.errorCorreo != null,
                    supportingText = {
                        uiState.errorCorreo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !uiState.cargando,
                    modifier = Modifier.fillMaxWidth()
                )

                uiState.mensajeGlobalError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { viewModel.enviarCorreoRecuperacion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.cargando
                ) {
                    Text("Enviar correo de recuperación")
                }
            }
        }
    }
}