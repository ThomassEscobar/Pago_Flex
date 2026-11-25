package cl.pagoflex.app.nucleo.funcionalidades.autenticacion


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
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
    val context = LocalContext.current

    val logoAppBitmap = remember(context) {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
            ?.toBitmap(width = 256, height = 256)
            ?.asImageBitmap()
    }

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
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DimensPagoflex.EspacioMediano, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
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
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                elevation = CardDefaults.cardElevation(defaultElevation = DimensPagoflex.ElevacionDestacada),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GradienteMarca)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.65f)),
                        modifier = Modifier.size(84.dp)
                    ) {
                        if (logoAppBitmap != null) {
                            Image(
                                bitmap = logoAppBitmap,
                                contentDescription = "Logo institucional de PagoFlex",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Logo institucional de PagoFlex",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
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

            // Tarjeta de inicio de sesión
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = DimensPagoflex.ElevacionTarjeta),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = uiState.correo,
                        onValueChange = { viewModel.onCorreoChange(it) },
                        label = { Text("Correo electrónico") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.errorCorreo != null) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error en correo electrónico",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = uiState.errorCorreo != null,
                        supportingText = {
                            uiState.errorCorreo?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.cargando
                    )

                    OutlinedTextField(
                        value = uiState.contrasena,
                        onValueChange = { viewModel.onContrasenaChange(it) },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.errorContrasena != null) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error en contraseña",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleMostrarContrasena() },
                                    modifier = Modifier.size(DimensPagoflex.TamanoToqueMinimo)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.mostrarContrasena) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (uiState.mostrarContrasena) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Mostrar contraseña"
                                        },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        isError = uiState.errorContrasena != null,
                        supportingText = {
                            uiState.errorContrasena?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        visualTransformation = if (uiState.mostrarContrasena) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.cargando
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.limpiarMensajes()
                                onIrARecuperarClave()
                            },
                            enabled = !uiState.cargando,
                            modifier = Modifier.defaultMinSize(minHeight = DimensPagoflex.TamanoToqueMinimo)
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    AnimatedVisibility(visible = uiState.mensajeGlobalError != null) {
                        Surface(
                            color = RojoContenedorClaro,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = RojoOscuro,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = uiState.mensajeGlobalError.orEmpty(),
                                    color = RojoOscuro,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.mensajeGlobalExito != null) {
                        Surface(
                            color = VerdeContenedorClaro,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VerdeOscuro,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = uiState.mensajeGlobalExito.orEmpty(),
                                    color = VerdeOscuro,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.iniciarSesion() },
                        shape = RoundedCornerShape(DimensPagoflex.RadioBoton),
                        enabled = !uiState.cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DimensPagoflex.AlturaBoton)
                    ) {
                        if (uiState.cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Iniciando sesión...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Acceso a creación de cuenta
            OutlinedButton(
                onClick = {
                    viewModel.limpiarMensajes()
                    onIrARegistro()
                },
                enabled = !uiState.cargando,
                shape = RoundedCornerShape(DimensPagoflex.RadioBoton),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DimensPagoflex.AlturaBoton)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear cuenta en PagoFlex", fontWeight = FontWeight.Bold)
            }
        }
    }
}