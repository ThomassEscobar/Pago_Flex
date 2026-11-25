package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.pagoflex.app.nucleo.diseno.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistro(
    onVolverALogin: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Si el registro es exitoso, volvemos al login
    LaunchedEffect(uiState.registroExitoso) {
        if (uiState.registroExitoso) {
            onVolverALogin()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        text = "Crear Cuenta",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Únete a PagoFlex",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Campo Nombre
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = { viewModel.onNombreChange(it) },
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = uiState.errorNombre != null,
                supportingText = { uiState.errorNombre?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.cargando
            )

            // Campo Correo
            OutlinedTextField(
                value = uiState.correo,
                onValueChange = { viewModel.onCorreoChange(it) },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = uiState.errorCorreo != null,
                supportingText = { uiState.errorCorreo?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.cargando
            )

            // Campo Contraseña
            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = { viewModel.onContrasenaChange(it) },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                isError = uiState.errorContrasena != null,
                supportingText = { uiState.errorContrasena?.let { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.cargando
            )

            // Campo Confirmar Contraseña
            OutlinedTextField(
                value = uiState.confirmarContrasena,
                onValueChange = { viewModel.onConfirmarContrasenaChange(it) },
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                isError = uiState.errorConfirmarContrasena != null,
                supportingText = { uiState.errorConfirmarContrasena?.let { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.cargando
            )

            // Mensaje de Error Global
            uiState.mensajeGlobalError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.cargando) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.registrarCuenta() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Registrarse")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}