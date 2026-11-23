package cl.pagoflex.app.nucleo.funcionalidades.autenticacion


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PantallaLogin(
    onLoginExitoso: () -> Unit, // <-- Ya no recibe UsuarioEntity, solo avisa que fue exitoso
    onIrARegistro: () -> Unit,
    onIrARecuperarClave: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // <-- Escuchamos el estado 'loginExitoso' del nuevo ViewModel
    LaunchedEffect(uiState.loginExitoso) {
        if (uiState.loginExitoso) {
            onLoginExitoso()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- LOGO E IDENTIDAD DE MARCA ---
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Logo PagoFlex",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PagoFlex",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Gestión inteligente de compromisos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 28.dp)
        )

        // --- CAMPO DE CORREO ---
        OutlinedTextField(
            value = uiState.correo,
            onValueChange = { viewModel.onCorreoChange(it) },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            trailingIcon = {
                if (uiState.errorCorreo != null) {
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                }
            },
            isError = uiState.errorCorreo != null,
            supportingText = {
                uiState.errorCorreo?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.cargando
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- CAMPO DE CONTRASEÑA ---
        OutlinedTextField(
            value = uiState.contrasena,
            onValueChange = { viewModel.onContrasenaChange(it) },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.errorContrasena != null) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { viewModel.toggleMostrarContrasena() }) {
                        Icon(
                            imageVector = if (uiState.mostrarContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar u ocultar contraseña"
                        )
                    }
                }
            },
            isError = uiState.errorContrasena != null,
            supportingText = {
                uiState.errorContrasena?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            visualTransformation = if (uiState.mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.cargando
        )

        // --- BOTÓN OLVIDÉ MI CONTRASEÑA ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                viewModel.limpiarMensajes()
                onIrARecuperarClave()
            }) {
                Text("¿Olvidaste tu contraseña?")
            }
        }

        // --- MENSAJE DE ERROR GLOBAL ---
        AnimatedVisibility(visible = uiState.mensajeGlobalError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = uiState.mensajeGlobalError.orEmpty(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTONES DE ACCIÓN ---
        if (uiState.cargando) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.iniciarSesion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Iniciar Sesión")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    viewModel.limpiarMensajes()
                    onIrARegistro()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Regístrate")
            }
        }
    }
}