package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
private fun IconoOjitoRegistro(visible: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val centro = Offset(w / 2f, h / 2f)

        val contornoOjo = Path().apply {
            moveTo(w * 0.08f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.12f, w * 0.92f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.88f, w * 0.08f, h * 0.5f)
            close()
        }
        drawPath(
            path = contornoOjo,
            color = tint,
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        drawCircle(
            color = tint,
            radius = w * 0.14f,
            center = centro
        )
        if (!visible) {
            drawLine(
                color = tint,
                start = Offset(w * 0.16f, h * 0.84f),
                end = Offset(w * 0.84f, h * 0.16f),
                strokeWidth = w * 0.085f,
                cap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistro(
    onVolverALogin: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val roles = listOf("USUARIO", "EJECUTIVO")

    var verContrasena by remember { mutableStateOf(false) }
    var verConfirmarContrasena by remember { mutableStateOf(false) }
    var menuEmpresasExpandido by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.registroExitoso) {
        if (uiState.registroExitoso) {
            viewModel.limpiarMensajes()
            onVolverALogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onVolverALogin) {
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = { viewModel.onNombreChange(it) },
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingIcon = {
                    if (uiState.errorNombre != null) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    }
                },
                isError = uiState.errorNombre != null,
                supportingText = {
                    uiState.errorNombre?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.rut,
                onValueChange = { viewModel.onRutChange(it) },
                label = { Text("RUT personal (ej: 12.345.678-9)") },
                leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                trailingIcon = {
                    if (uiState.errorRut != null) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    }
                },
                isError = uiState.errorRut != null,
                supportingText = {
                    uiState.errorRut?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.correo,
                onValueChange = { viewModel.onCorreoChange(it) },
                label = { Text("Correo electrónico") },
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
                modifier = Modifier.fillMaxWidth()
            )

            Text("Selecciona tu rol en PagoFlex:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEach { rol ->
                    FilterChip(
                        selected = uiState.rolSeleccionado == rol,
                        onClick = { viewModel.onRolChange(rol) },
                        label = { Text(if (rol == "USUARIO") "Cliente" else "Ejecutivo") }
                    )
                }
            }

            if (uiState.rolSeleccionado == "EJECUTIVO") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Validación de Empresa Asociada",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Selecciona tu empresa y verifica su RUT y correo institucional registrados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ExposedDropdownMenuBox(
                            expanded = menuEmpresasExpandido,
                            onExpandedChange = { menuEmpresasExpandido = !menuEmpresasExpandido }
                        ) {
                            val textoSeleccionado = uiState.empresaSeleccionada?.let {
                                "${it.nombre} (${it.tipo})"
                            } ?: ""

                            OutlinedTextField(
                                value = textoSeleccionado,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Empresa asociada (A - Z)") },
                                placeholder = { Text("Selecciona una empresa") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuEmpresasExpandido)
                                },
                                isError = uiState.errorEmpresaSeleccionada != null,
                                supportingText = {
                                    uiState.errorEmpresaSeleccionada?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = menuEmpresasExpandido,
                                onDismissRequest = { menuEmpresasExpandido = false }
                            ) {
                                if (uiState.empresasDisponibles.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hay empresas asociadas disponibles") },
                                        onClick = { menuEmpresasExpandido = false },
                                        enabled = false
                                    )
                                } else {
                                    uiState.empresasDisponibles.forEach { empresa ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = empresa.nombre,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = empresa.tipo,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.onEmpresaSeleccionadaChange(empresa)
                                                menuEmpresasExpandido = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.rutEmpresa,
                            onValueChange = { viewModel.onRutEmpresaChange(it) },
                            label = { Text("RUT Empresa (ej: 76.123.456-0)") },
                            leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.errorRutEmpresa != null) {
                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            isError = uiState.errorRutEmpresa != null,
                            supportingText = {
                                uiState.errorRutEmpresa?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.correoEmpresa,
                            onValueChange = { viewModel.onCorreoEmpresaChange(it) },
                            label = { Text("Correo institucional de la empresa") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.errorCorreoEmpresa != null) {
                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            isError = uiState.errorCorreoEmpresa != null,
                            supportingText = {
                                uiState.errorCorreoEmpresa?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = { viewModel.onContrasenaChange(it) },
                label = { Text("Contraseña (mín. 6 caracteres)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.errorContrasena != null) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { verContrasena = !verContrasena }) {
                            IconoOjitoRegistro(
                                visible = verContrasena,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                isError = uiState.errorContrasena != null,
                supportingText = {
                    uiState.errorContrasena?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (verContrasena) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.confirmarContrasena,
                onValueChange = { viewModel.onConfirmarContrasenaChange(it) },
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.errorConfirmarContrasena != null) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { verConfirmarContrasena = !verConfirmarContrasena }) {
                            IconoOjitoRegistro(
                                visible = verConfirmarContrasena,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                isError = uiState.errorConfirmarContrasena != null,
                supportingText = {
                    uiState.errorConfirmarContrasena?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (verConfirmarContrasena) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            uiState.mensajeGlobalError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.registrarCuenta() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.cargando
            ) {
                Text("Registrarme")
            }
        }
    }
}