package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.pagoflex.app.nucleo.diseno.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecuperarClave(
    onVolverALogin: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                        text = "Recuperar Contraseña",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.limpiarMensajes()
                            onVolverALogin()
                        },
                        modifier = Modifier.size(DimensPagoflex.TamanoToqueMinimo)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a iniciar sesión"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DimensPagoflex.EspacioMediano, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera institucional PagoFlex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                elevation = CardDefaults.cardElevation(defaultElevation = DimensPagoflex.ElevacionDestacada),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GradienteMarca)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restablecimiento Seguro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Protegemos el acceso a tus compromisos y comprobantes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            if (uiState.correoRecuperacionEnviado) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                    shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                    border = BorderStroke(1.dp, VerdeExito.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = VerdeContenedorClaro)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = VerdeExito,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            text = "Correo de recuperación enviado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VerdeOscuro,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Hemos enviado un enlace seguro de restablecimiento a ${uiState.correo.trim()}. Abre el mensaje desde tu bandeja de entrada para cambiar tu contraseña.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VerdeOscuro,
                            textAlign = TextAlign.Center
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
                    shape = RoundedCornerShape(DimensPagoflex.RadioBoton),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DimensPagoflex.AlturaBoton)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir aplicación de correo", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.limpiarMensajes()
                        onVolverALogin()
                    },
                    shape = RoundedCornerShape(DimensPagoflex.RadioBoton),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DimensPagoflex.AlturaBoton)
                ) {
                    Text("Volver a Iniciar Sesión", fontWeight = FontWeight.Bold)
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
                    onClick = {
                        // Le pasamos el correo que el usuario escribió en el input
                        viewModel.enviarCorreoRecuperacion(uiState.correo)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.cargando
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Ingresa el correo asociado a tu cuenta y te enviaremos un enlace único para crear una nueva contraseña.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.correo,
                            onValueChange = { viewModel.onCorreoChange(it) },
                            label = { Text("Correo electrónico registrado") },
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
                                        contentDescription = "Error en el correo",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            isError = uiState.errorCorreo != null,
                            supportingText = {
                                uiState.errorCorreo?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            enabled = !uiState.cargando,
                            modifier = Modifier.fillMaxWidth()
                        )

                        uiState.mensajeGlobalError?.let { errorMsg ->
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
                                        text = errorMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RojoOscuro,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.enviarCorreoRecuperacion() },
                            shape = RoundedCornerShape(DimensPagoflex.RadioBoton),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DimensPagoflex.AlturaBoton),
                            enabled = !uiState.cargando
                        ) {
                            if (uiState.cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Enviando enlace...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Email, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Enviar correo de recuperación", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}