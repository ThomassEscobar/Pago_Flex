package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.pagoflex.app.nucleo.diseno.tema.*

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
                    IconButton(
                        onClick = onVolverALogin,
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
                .padding(horizontal = DimensPagoflex.EspacioMediano, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Únete a PagoFlex",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Gestiona pagos y compromisos financieros con respaldo verificado",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            // Bloque 1: Identidad Personal
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = DimensPagoflex.ElevacionTarjeta),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Datos Personales",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = uiState.nombre,
                        onValueChange = { viewModel.onNombreChange(it) },
                        label = { Text("Nombre completo") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.errorNombre != null) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error en el nombre",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = uiState.errorNombre != null,
                        supportingText = {
                            uiState.errorNombre?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.rut,
                        onValueChange = { viewModel.onRutChange(it) },
                        label = { Text("RUT personal (ej: 12.345.678-9)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.errorRut != null) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error en el RUT",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = uiState.errorRut != null,
                        supportingText = {
                            uiState.errorRut?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                                    contentDescription = "Error en el correo",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = uiState.errorCorreo != null,
                        supportingText = {
                            uiState.errorCorreo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bloque 2: Selección de Rol Accesible
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = DimensPagoflex.ElevacionTarjeta),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tipo de Perfil en PagoFlex",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Elige cómo utilizarás la plataforma:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        roles.forEach { rol ->
                            val seleccionado = uiState.rolSeleccionado == rol
                            val tituloRol = if (rol == "USUARIO") "Cliente" else "Ejecutivo"
                            FilterChip(
                                selected = seleccionado,
                                onClick = { viewModel.onRolChange(rol) },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = DimensPagoflex.TamanoToqueMinimo),
                                shape = RoundedCornerShape(DimensPagoflex.RadioChip),
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (seleccionado) {
                                            Icons.Default.Check
                                        } else if (rol == "USUARIO") {
                                            Icons.Default.Person
                                        } else {
                                            Icons.Default.Business
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tituloRol,
                                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }

                    Text(
                        text = if (uiState.rolSeleccionado == "USUARIO") {
                            "• Cliente: visualiza tus compromisos por empresa, sube comprobantes de pago y lleva tu historial al día."
                        } else {
                            "• Ejecutivo: administra clientes de tu entidad, emite cobros recurrentes o únicos y valida comprobantes."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bloque 2B: Validación Empresarial (solo si es EJECUTIVO)
            if (uiState.rolSeleccionado == "EJECUTIVO") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                    border = BorderStroke(1.dp, AzulPrimario.copy(alpha = 0.35f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Validación de Empresa Asociada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

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
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuEmpresasExpandido)
                                },
                                isError = uiState.errorEmpresaSeleccionada != null,
                                supportingText = {
                                    uiState.errorEmpresaSeleccionada?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
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
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (uiState.errorRutEmpresa != null) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error en RUT de empresa",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            isError = uiState.errorRutEmpresa != null,
                            supportingText = {
                                uiState.errorRutEmpresa?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.correoEmpresa,
                            onValueChange = { viewModel.onCorreoEmpresaChange(it) },
                            label = { Text("Correo institucional de la empresa") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (uiState.errorCorreoEmpresa != null) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error en correo de empresa",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            isError = uiState.errorCorreoEmpresa != null,
                            supportingText = {
                                uiState.errorCorreoEmpresa?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bloque 3: Seguridad
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DimensPagoflex.RadioTarjeta),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = DimensPagoflex.ElevacionTarjeta),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Seguridad de Acceso",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = uiState.contrasena,
                        onValueChange = { viewModel.onContrasenaChange(it) },
                        label = { Text("Contraseña (mín. 6 caracteres)") },
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
                                    onClick = { verContrasena = !verContrasena },
                                    modifier = Modifier.size(DimensPagoflex.TamanoToqueMinimo)
                                ) {
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
                        shape = RoundedCornerShape(DimensPagoflex.RadioInput),
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
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.errorConfirmarContrasena != null) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error al confirmar contraseña",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(
                                    onClick = { verConfirmarContrasena = !verConfirmarContrasena },
                                    modifier = Modifier.size(DimensPagoflex.TamanoToqueMinimo)
                                ) {
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
                        shape = RoundedCornerShape(DimensPagoflex.RadioInput),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (verConfirmarContrasena) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                onClick = { viewModel.registrarCuenta() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeExito,
                    contentColor = Color.White
                ),
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
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Creando cuenta...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Registrarme en PagoFlex", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}