package cl.pagoflex.app.nucleo.funcionalidades.inicio

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import cl.pagoflex.app.nucleo.util.NotificadorPagoFlex
import java.io.File
import java.text.NumberFormat
import java.util.Locale

private fun compartirInformeCsv(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Informe de Gastos PagoFlex")
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("Informe PagoFlex", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartir informe CSV"))
}

@Composable
private fun rememberAvatarBitmap(path: String?): ImageBitmap? {
    return remember(path) {
        if (path.isNullOrBlank()) {
            null
        } else {
            try {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Composable
private fun IconoOjitoContrasena(visible: Boolean, tint: Color) {
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
fun PantallaInicio(
    usuarioId: Int = 1,
    onCerrarSesion: () -> Unit = {},
    viewModel: InicioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var tabSeleccionado by remember { mutableIntStateOf(0) }

    var compromisoParaFotoId by remember { mutableStateOf<Int?>(null) }
    var compromisoParaArchivoId by remember { mutableStateOf<Int?>(null) }
    var fotoTemporalUri by remember { mutableStateOf<Uri?>(null) }

    val rolActual = uiState.usuarioActual?.rol ?: "USUARIO"
    val esAdmin = rolActual == "ADMIN"
    val esEjecutivo = rolActual == "EJECUTIVO"
    val esGestorCobros = esAdmin || esEjecutivo

    LaunchedEffect(Unit) {
        viewModel.inicializarEntorno(context)
    }

    LaunchedEffect(usuarioId) {
        viewModel.cargarUsuario(usuarioId)
    }

    LaunchedEffect(uiState.usuarioActual?.id) {
        if (uiState.usuarioActual != null) {
            viewModel.cargarDatosExtraPerfil(context)
        }
    }

    LaunchedEffect(uiState.mensajeSnackbar, uiState.ultimoInformeUri) {
        uiState.mensajeSnackbar?.let { msg ->
            val informeUri = uiState.ultimoInformeUri
            if (informeUri != null) {
                compartirInformeCsv(context, informeUri)
            }
            val resultado = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (informeUri != null) "Compartir" else null,
                duration = if (informeUri != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (resultado == SnackbarResult.ActionPerformed && informeUri != null) {
                compartirInformeCsv(context, informeUri)
            }
            viewModel.limpiarSnackbar()
        }
    }

    val avatarGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.guardarFotoPerfilDesdeUri(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val id = compromisoParaFotoId
        if (exito && id != null) {
            viewModel.registrarComprobante(context, id, fotoTemporalUri?.toString())
        } else {
            viewModel.mostrarMensaje("Captura de foto cancelada")
        }
    }

    val selectorArchivoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val id = compromisoParaArchivoId
        if (uri != null && id != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.registrarComprobante(context, id, uri.toString())
        } else {
            viewModel.mostrarMensaje("Selección de archivo cancelada")
        }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val archivoFoto = File(context.cacheDir, "comprobante_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivoFoto
            )
            fotoTemporalUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.mostrarMensaje("Permiso de cámara denegado. Usa 'Galería / PDF' para elegir desde galería.")
        }
    }

    val permisoNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            NotificadorPagoFlex.mostrarNotificacion(
                context,
                "Alertas activadas",
                "Recibirás avisos de tus compromisos y comprobantes en PagoFlex."
            )
        } else {
            viewModel.mostrarMensaje("Permiso de notificaciones denegado en el sistema.")
        }
    }

    val abrirComprobante: (String) -> Unit = { uriString ->
        try {
            val uri = Uri.parse(uriString)
            val mimeType = context.contentResolver.getType(uri) ?: "image/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir comprobante"))
        } catch (_: Exception) {
            viewModel.mostrarMensaje("No se pudo abrir vista previa del archivo en este dispositivo.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PagoFlex", fontWeight = FontWeight.Bold)
                        uiState.usuarioActual?.let {
                            Text(
                                text = "${it.nombre} (${it.rol})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportarReporteCsv(context) }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = if (esGestorCobros) "Compartir reporte general" else "Compartir informe de mis gastos"
                        )
                    }
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permisoNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificadorPagoFlex.mostrarNotificacion(
                                context,
                                "Recordatorio PagoFlex",
                                "Tienes ${uiState.compromisos.count { it.estado == "PENDIENTE" }} compromisos pendientes."
                            )
                        }
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tabSeleccionado == 0,
                    onClick = { tabSeleccionado = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Compromisos") }
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 1,
                    onClick = { tabSeleccionado = 1 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Empresas") }
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 2,
                    onClick = { tabSeleccionado = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Perfil") }
                )
            }
        },
        floatingActionButton = {
            if (tabSeleccionado == 0 && esGestorCobros) {
                FloatingActionButton(onClick = { viewModel.abrirDialogoNuevoCompromiso() }) {
                    Icon(Icons.Default.Add, contentDescription = "Emitir Cobro")
                }
            } else if (tabSeleccionado == 1 && esAdmin) {
                FloatingActionButton(onClick = { viewModel.abrirDialogoNuevaOrg() }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva Empresa")
                }
            } else if (tabSeleccionado == 1 && !esGestorCobros) {
                FloatingActionButton(onClick = { viewModel.abrirDialogoVincularEmpresaUsuario() }) {
                    Icon(Icons.Default.Add, contentDescription = "Vincular Empresa")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tabSeleccionado) {
                0 -> VistaCompromisos(
                    uiState = uiState,
                    onFiltroChange = { viewModel.onFiltroEstadoChange(it) },
                    onBusquedaChange = { viewModel.onBusquedaChange(it) },
                    onEditarCompromiso = { viewModel.abrirDialogoEditarCompromiso(it) },
                    onTomarFotoComprobante = { compId ->
                        compromisoParaFotoId = compId
                        val tienePermiso = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (tienePermiso) {
                            val archivoFoto = File(context.cacheDir, "comprobante_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                archivoFoto
                            )
                            fotoTemporalUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onAdjuntarDigital = { compId ->
                        compromisoParaArchivoId = compId
                        selectorArchivoLauncher.launch(arrayOf("image/*", "application/pdf"))
                    },
                    onVerComprobante = abrirComprobante,
                    onResolverComprobante = { compId, aprobado ->
                        viewModel.resolverComprobante(context, compId, aprobado)
                    }
                )
                1 -> VistaOrganizaciones(
                    uiState = uiState,
                    esAdmin = esAdmin,
                    esGestorCobros = esGestorCobros,
                    onEditar = { viewModel.abrirDialogoEditarOrg(it) },
                    onEliminar = { viewModel.eliminarOrganizacion(it) },
                    onGestionarClientes = { viewModel.abrirDialogoClientesDeOrganizacion(it) },
                    onDesvincularUsuario = { orgId ->
                        uiState.usuarioActual?.let { user ->
                            viewModel.alternarVinculoUsuarioEmpresa(context, user.id, orgId)
                        }
                    }
                )
                2 -> VistaPerfil(
                    uiState = uiState,
                    onCambiarFotoPerfil = { viewModel.abrirDialogoFotoPerfil() },
                    onAbrirDatosPersonales = { viewModel.abrirDialogoDatosPersonales() },
                    onAbrirSeguridad = { viewModel.abrirDialogoSeguridad() },
                    onAvisoColorFuturo = {
                        viewModel.mostrarMensaje("Personalización de colores del tema disponible en la etapa visual final.")
                    },
                    onExportarCsv = { viewModel.exportarReporteCsv(context) },
                    onCerrarSesion = onCerrarSesion
                )
            }
        }

        if (uiState.mostrarDialogoCompromiso && esGestorCobros) {
            DialogoCompromiso(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoCompromiso() },
                onSelectOrg = { viewModel.onSeleccionarOrgCompromiso(it) },
                onSelectCliente = { viewModel.onSeleccionarClienteCompromiso(it) },
                onConceptoChange = { viewModel.onConceptoChange(it) },
                onMontoChange = { viewModel.onMontoChange(it) },
                onFechaChange = { viewModel.onFechaChange(it) },
                onGuardar = { viewModel.guardarCompromiso(context) }
            )
        }

        if (uiState.mostrarDialogoOrg && esAdmin) {
            DialogoOrganizacion(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoOrg() },
                onNombreChange = { viewModel.onNombreOrgChange(it) },
                onTipoChange = { viewModel.onTipoOrgChange(it) },
                onRutChange = { viewModel.onRutOrgChange(it) },
                onCorreoChange = { viewModel.onCorreoOrgChange(it) },
                onGuardar = { viewModel.guardarOrganizacion() }
            )
        }

        if (uiState.mostrarDialogoVincularEmpresaUsuario && !esGestorCobros) {
            DialogoVincularEmpresasCliente(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoVincularEmpresaUsuario() },
                onAlternarVinculo = { orgId ->
                    uiState.usuarioActual?.let { user ->
                        viewModel.alternarVinculoUsuarioEmpresa(context, user.id, orgId)
                    }
                }
            )
        }

        val orgClientes = uiState.orgGestionClientes
        if (orgClientes != null && esGestorCobros) {
            DialogoClientesDeOrganizacion(
                organizacion = orgClientes,
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoClientesDeOrganizacion() },
                onAlternarCliente = { userId ->
                    viewModel.alternarVinculoUsuarioEmpresa(context, userId, orgClientes.id)
                }
            )
        }

        if (uiState.mostrarDialogoFotoPerfil) {
            DialogoFotoPerfil(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoFotoPerfil() },
                onSeleccionarFoto = { avatarGalleryLauncher.launch("image/*") },
                onQuitarFoto = { viewModel.eliminarFotoPerfil(context) }
            )
        }

        if (uiState.mostrarDialogoDatosPersonales) {
            DialogoDatosPersonales(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoDatosPersonales() },
                onNombreChange = { viewModel.onNombrePerfilInputChange(it) },
                onGuardar = { viewModel.guardarDatosPersonales() }
            )
        }

        if (uiState.mostrarDialogoSeguridad) {
            DialogoSeguridadCuenta(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoSeguridad() },
                onCorreoPrincipalChange = { viewModel.onCorreoPrincipalInputChange(it) },
                onCorreoRespaldoChange = { viewModel.onCorreoRespaldoInputChange(it) },
                onEliminarCorreoRespaldo = { viewModel.eliminarCorreoRespaldo(context) },
                onToggleCambioClave = { viewModel.onToggleExpandirCambioClave(it) },
                onClaveActualChange = { viewModel.onClaveActualPerfilChange(it) },
                onNuevaClaveChange = { viewModel.onNuevaClavePerfilChange(it) },
                onConfirmarClaveChange = { viewModel.onConfirmarClavePerfilChange(it) },
                onGuardar = { viewModel.guardarSeguridadCuenta(context) }
            )
        }
    }
}

@Composable
private fun VistaCompromisos(
    uiState: InicioUiState,
    onFiltroChange: (String) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onEditarCompromiso: (CompromisoEntity) -> Unit,
    onTomarFotoComprobante: (Int) -> Unit,
    onAdjuntarDigital: (Int) -> Unit,
    onVerComprobante: (String) -> Unit,
    onResolverComprobante: (Int, Boolean) -> Unit
) {
    val formatoClp = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val userActual = uiState.usuarioActual
    val rol = userActual?.rol ?: "USUARIO"
    val esEjecutivo = rol == "EJECUTIVO"
    val esGestorCobros = esEjecutivo || rol == "ADMIN"

    val compromisosVisiblesPorRol = remember(
        uiState.compromisos,
        uiState.vinculosUsuarioOrgs,
        uiState.asignacionesCobros,
        userActual
    ) {
        if (esGestorCobros || userActual == null) {
            uiState.compromisos
        } else {
            val orgsVinculadas = uiState.vinculosUsuarioOrgs[userActual.id].orEmpty()
            uiState.compromisos.filter { comp ->
                val perteneceAEmpresaVinculada = orgsVinculadas.contains(comp.organizacionId)
                val asignacion = uiState.asignacionesCobros[comp.id]
                val correspondeAlUsuario = asignacion == null || asignacion.usuarioId == userActual.id
                perteneceAEmpresaVinculada && correspondeAlUsuario
            }
        }
    }

    val estados = if (esEjecutivo) {
        listOf("REVISION", "RECHAZADO", "TODOS")
    } else {
        listOf("TODOS", "PENDIENTE", "REVISION", "PAGADO", "RECHAZADO")
    }

    val tituloTarjetaIzquierda = if (esGestorCobros) "Por Validar" else "Por Pagar"
    val montoTarjetaIzquierda = if (esGestorCobros) {
        compromisosVisiblesPorRol.filter { it.estado == "REVISION" }.sumOf { it.monto }
    } else {
        compromisosVisiblesPorRol.filter { it.estado == "PENDIENTE" || it.estado == "RECHAZADO" }.sumOf { it.monto }
    }

    val tituloTarjetaDerecha = if (esGestorCobros) "Conciliado" else "Pagado"
    val montoTarjetaDerecha = compromisosVisiblesPorRol.filter { it.estado == "PAGADO" }.sumOf { it.monto }

    val filtrados = compromisosVisiblesPorRol.filter { c ->
        val clienteNombre = uiState.asignacionesCobros[c.id]?.usuarioNombre.orEmpty()
        val coincideEstado = uiState.filtroEstado == "TODOS" || c.estado == uiState.filtroEstado
        val coincideTexto = uiState.textoBusqueda.isBlank() ||
                c.empresaNombre.contains(uiState.textoBusqueda, ignoreCase = true) ||
                c.concepto.contains(uiState.textoBusqueda, ignoreCase = true) ||
                clienteNombre.contains(uiState.textoBusqueda, ignoreCase = true)
        coincideEstado && coincideTexto
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(tituloTarjetaIzquierda, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatoClp.format(montoTarjetaIzquierda),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(tituloTarjetaDerecha, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatoClp.format(montoTarjetaDerecha),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.textoBusqueda,
            onValueChange = onBusquedaChange,
            placeholder = {
                Text(if (esGestorCobros) "Buscar por empresa, concepto o cliente..." else "Buscar por empresa o concepto...")
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            estados.forEach { estado ->
                FilterChip(
                    selected = uiState.filtroEstado == estado,
                    onClick = { onFiltroChange(estado) },
                    label = { Text(estado) }
                )
            }
        }

        if (filtrados.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (!esGestorCobros) {
                        "No tienes compromisos en esta categoría para tus empresas vinculadas."
                    } else {
                        "No hay compromisos que coincidan con el filtro actual."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filtrados, key = { it.id }) { compromiso ->
                    TarjetaCompromisoItem(
                        compromiso = compromiso,
                        clienteAsignado = uiState.asignacionesCobros[compromiso.id]?.usuarioNombre,
                        formatoClp = formatoClp,
                        esGestorCobros = esGestorCobros,
                        onEditar = { onEditarCompromiso(compromiso) },
                        onTomarFoto = { onTomarFotoComprobante(compromiso.id) },
                        onAdjuntarDigital = { onAdjuntarDigital(compromiso.id) },
                        onVerComprobante = onVerComprobante,
                        onResolverComprobante = { onResolverComprobante(compromiso.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaCompromisoItem(
    compromiso: CompromisoEntity,
    clienteAsignado: String?,
    formatoClp: NumberFormat,
    esGestorCobros: Boolean,
    onEditar: () -> Unit,
    onTomarFoto: () -> Unit,
    onAdjuntarDigital: () -> Unit,
    onVerComprobante: (String) -> Unit,
    onResolverComprobante: (Boolean) -> Unit
) {
    val colorEstado = when (compromiso.estado) {
        "PAGADO" -> Color(0xFF16A34A)
        "RECHAZADO" -> Color(0xFFDC2626)
        "REVISION" -> Color(0xFF2563EB)
        else -> Color(0xFFD97706)
    }
    val tieneArchivoReal = !compromiso.comprobanteUri.isNullOrBlank() &&
            compromiso.comprobanteUri.startsWith("content://")

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
                    text = compromiso.empresaNombre,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = compromiso.estado,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorEstado,
                        fontWeight = FontWeight.Bold
                    )
                    if (esGestorCobros) {
                        IconButton(onClick = onEditar) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar cobro")
                        }
                    }
                }
            }

            if (!clienteAsignado.isNullOrBlank()) {
                Text(
                    text = "Cliente: $clienteAsignado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(text = compromiso.concepto, style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatoClp.format(compromiso.monto),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vence: ${compromiso.fechaVencimiento}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            when (compromiso.estado) {
                "PENDIENTE" -> {
                    if (!esGestorCobros) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onTomarFoto,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Foto Comprobante")
                            }
                            OutlinedButton(
                                onClick = onAdjuntarDigital,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Galería / PDF")
                            }
                        }
                    } else {
                        Text(
                            text = "Sin comprobante adjunto — Pendiente de pago del cliente",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                "REVISION" -> {
                    if (tieneArchivoReal) {
                        TextButton(
                            onClick = { onVerComprobante(compromiso.comprobanteUri!!) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Ver comprobante adjunto")
                        }
                    }

                    if (esGestorCobros) {
                        Text(
                            text = "Comprobante listo para validación:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onResolverComprobante(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Aprobar")
                            }
                            OutlinedButton(
                                onClick = { onResolverComprobante(false) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Rechazar")
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Comprobante enviado — En espera de validación por un Ejecutivo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                "RECHAZADO" -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = "Comprobante rechazado",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Motivo: ${InicioViewModel.MOTIVO_RECHAZO_GENERAL}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (!esGestorCobros) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onTomarFoto,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reintentar Foto")
                            }
                            OutlinedButton(
                                onClick = onAdjuntarDigital,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Galería / PDF")
                            }
                        }
                    }
                }
                "PAGADO" -> {
                    Text(
                        text = "Pago verificado y conciliado",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}

@Composable
private fun VistaOrganizaciones(
    uiState: InicioUiState,
    esAdmin: Boolean,
    esGestorCobros: Boolean,
    onEditar: (OrganizacionEntity) -> Unit,
    onEliminar: (OrganizacionEntity) -> Unit,
    onGestionarClientes: (OrganizacionEntity) -> Unit,
    onDesvincularUsuario: (Int) -> Unit
) {
    val userActual = uiState.usuarioActual
    val orgsVisibles = remember(uiState.organizaciones, uiState.vinculosUsuarioOrgs, userActual, esGestorCobros) {
        if (esGestorCobros || userActual == null) {
            uiState.organizaciones
        } else {
            val idsVinculados = uiState.vinculosUsuarioOrgs[userActual.id].orEmpty()
            uiState.organizaciones.filter { idsVinculados.contains(it.id) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (esGestorCobros) {
                        "Empresas y Organizaciones (${orgsVisibles.size})"
                    } else {
                        "Mis Empresas Vinculadas (${orgsVisibles.size})"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (esGestorCobros) {
                        "Gestiona entidades emisoras y sus clientes vinculados"
                    } else {
                        "Solo puedes desvincular una empresa cuando todos sus compromisos estén pagados"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (orgsVisibles.isEmpty() && !esGestorCobros) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Aún no estás vinculado a ninguna empresa. Presiona el botón '+' abajo a la derecha para afiliarte.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(orgsVisibles, key = { it.id }) { org ->
            val clientesVinculadosAEstaOrg = uiState.clientesDisponibles.filter { cliente ->
                uiState.vinculosUsuarioOrgs[cliente.id]?.contains(org.id) == true
            }
            val compromisosSinPagarUsuario = if (!esGestorCobros && userActual != null) {
                uiState.compromisos.count { comp ->
                    comp.organizacionId == org.id &&
                            comp.estado != "PAGADO" &&
                            (uiState.asignacionesCobros[comp.id] == null ||
                                    uiState.asignacionesCobros[comp.id]?.usuarioId == userActual.id)
                }
            } else {
                0
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(org.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Tipo: ${org.tipo} • RUT: ${org.rut}", style = MaterialTheme.typography.bodySmall)
                        Text(org.correoContacto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        if (esGestorCobros) {
                            Text(
                                text = "Clientes vinculados (${clientesVinculadosAEstaOrg.size}): " +
                                        if (clientesVinculadosAEstaOrg.isEmpty()) "Ninguno"
                                        else clientesVinculadosAEstaOrg.joinToString(", ") { it.nombre },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else if (compromisosSinPagarUsuario > 0) {
                            Text(
                                text = "Bloqueado: $compromisosSinPagarUsuario compromiso(s) sin pagar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Al día • Sin compromisos pendientes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (esGestorCobros) {
                        Row {
                            IconButton(onClick = { onGestionarClientes(org) }) {
                                Icon(Icons.Default.Person, contentDescription = "Clientes vinculados")
                            }
                            if (esAdmin) {
                                IconButton(onClick = { onEditar(org) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar empresa")
                                }
                                IconButton(onClick = { onEliminar(org) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar empresa",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    } else if (compromisosSinPagarUsuario > 0) {
                        OutlinedButton(
                            onClick = { onDesvincularUsuario(org.id) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Con deuda", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        TextButton(onClick = { onDesvincularUsuario(org.id) }) {
                            Text("Desvincular", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaPerfil(
    uiState: InicioUiState,
    onCambiarFotoPerfil: () -> Unit,
    onAbrirDatosPersonales: () -> Unit,
    onAbrirSeguridad: () -> Unit,
    onAvisoColorFuturo: () -> Unit,
    onExportarCsv: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val user = uiState.usuarioActual
    val rol = user?.rol ?: "USUARIO"
    val esGestorCobros = rol == "ADMIN" || rol == "EJECUTIVO"
    val avatarBitmap = rememberAvatarBitmap(uiState.avatarPath)

    val iniciales = remember(user?.nombre) {
        user?.nombre
            ?.trim()
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.take(2)
            ?.joinToString("") { it.first().uppercase() }
            ?.ifBlank { "PF" } ?: "PF"
    }

    val compromisosRelevantes = remember(uiState.compromisos, uiState.vinculosUsuarioOrgs, uiState.asignacionesCobros, user) {
        if (esGestorCobros || user == null) {
            uiState.compromisos
        } else {
            val ids = uiState.vinculosUsuarioOrgs[user.id].orEmpty()
            uiState.compromisos.filter { comp ->
                val asignacion = uiState.asignacionesCobros[comp.id]
                ids.contains(comp.organizacionId) && (asignacion == null || asignacion.usuarioId == user.id)
            }
        }
    }

    val stat1Valor = if (esGestorCobros) {
        compromisosRelevantes.count { it.estado == "REVISION" }
    } else {
        compromisosRelevantes.count { it.estado == "PENDIENTE" || it.estado == "RECHAZADO" }
    }
    val stat1Label = if (esGestorCobros) "Por Validar" else "Pendientes"

    val stat2Valor = if (esGestorCobros) {
        compromisosRelevantes.count { it.estado == "PAGADO" }
    } else {
        compromisosRelevantes.count { it.estado == "REVISION" }
    }
    val stat2Label = if (esGestorCobros) "Conciliados" else "En Revisión"

    val stat3Valor = if (esGestorCobros) {
        uiState.organizaciones.size
    } else {
        compromisosRelevantes.count { it.estado == "PAGADO" }
    }
    val stat3Label = if (esGestorCobros) "Empresas" else "Pagados"

    val subtituloSeguridad = if (uiState.correoRespaldo.isNotBlank()) {
        "Respaldo: ${uiState.correoRespaldo}"
    } else {
        "Correo principal, respaldo y clave"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .clickable { onCambiarFotoPerfil() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = iniciales,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Cambiar foto de perfil",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = user?.nombre ?: "Usuario PagoFlex",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = user?.correo ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Cuenta $rol",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColumnaMetricaPerfil(valor = stat1Valor.toString(), etiqueta = stat1Label)
                VerticalDivider(modifier = Modifier.height(32.dp))
                ColumnaMetricaPerfil(valor = stat2Valor.toString(), etiqueta = stat2Label)
                VerticalDivider(modifier = Modifier.height(32.dp))
                ColumnaMetricaPerfil(valor = stat3Valor.toString(), etiqueta = stat3Label)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                FilaMenuPerfil(
                    icono = Icons.Default.Person,
                    titulo = "Datos personales",
                    subtitulo = user?.nombre ?: "Editar nombre visible",
                    onClick = onAbrirDatosPersonales
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                FilaMenuPerfil(
                    icono = Icons.Default.Lock,
                    titulo = "Seguridad, correo y contraseña",
                    subtitulo = subtituloSeguridad,
                    onClick = onAbrirSeguridad
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                FilaMenuPerfil(
                    icono = Icons.Default.Star,
                    titulo = "Apariencia y colores",
                    subtitulo = "Tema institucional (configurable próximamente)",
                    onClick = onAvisoColorFuturo
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                FilaMenuPerfil(
                    icono = Icons.Default.Share,
                    titulo = if (esGestorCobros) "Compartir y descargar reporte" else "Informe de mis gastos (CSV)",
                    subtitulo = if (esGestorCobros) "Guarda en Descargas y abre menú compartir" else "Guarda en Descargas y comparte tu detalle",
                    onClick = onExportarCsv
                )
            }
        }

        OutlinedButton(
            onClick = onCerrarSesion,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar Sesión", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ColumnaMetricaPerfil(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilaMenuPerfil(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DialogoFotoPerfil(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onSeleccionarFoto: () -> Unit,
    onQuitarFoto: () -> Unit
) {
    val avatarBitmap = rememberAvatarBitmap(uiState.avatarPath)
    val nombreActual = uiState.usuarioActual?.nombre ?: "PF"
    val iniciales = remember(nombreActual) {
        nombreActual
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "PF" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto de Perfil", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = "Vista previa foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = iniciales,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Button(
                    onClick = onSeleccionarFoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (avatarBitmap != null) "Cambiar foto desde galería" else "Elegir foto desde galería")
                }

                if (avatarBitmap != null) {
                    OutlinedButton(
                        onClick = onQuitarFoto,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quitar foto actual")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DialogoDatosPersonales(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onNombreChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    val user = uiState.usuarioActual
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datos Personales", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (user != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Rol ${user.rol}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = user.correo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.nombrePerfilInput,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre completo") },
                    isError = uiState.errorNombrePerfilInput != null,
                    supportingText = {
                        uiState.errorNombrePerfilInput?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) { Text("Guardar Nombre") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoSeguridadCuenta(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onCorreoPrincipalChange: (String) -> Unit,
    onCorreoRespaldoChange: (String) -> Unit,
    onEliminarCorreoRespaldo: () -> Unit,
    onToggleCambioClave: (Boolean) -> Unit,
    onClaveActualChange: (String) -> Unit,
    onNuevaClaveChange: (String) -> Unit,
    onConfirmarClaveChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    val correoActual = uiState.usuarioActual?.correo ?: ""
    val cambiaCorreoPrincipal = uiState.correoPrincipalInput.trim() != correoActual
    val coincidenNuevas = uiState.nuevaClavePerfil.length >= 6 &&
            uiState.nuevaClavePerfil == uiState.confirmarClavePerfil &&
            uiState.errorClavePerfil == null

    var verClaveActual by remember { mutableStateOf(false) }
    var verNuevaClave by remember { mutableStateOf(false) }
    var verConfirmarClave by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seguridad y Correos", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Correo electrónico principal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.correoPrincipalInput,
                    onValueChange = onCorreoPrincipalChange,
                    label = { Text("Correo de inicio de sesión") },
                    isError = uiState.errorCorreoPrincipalInput != null,
                    supportingText = {
                        uiState.errorCorreoPrincipalInput?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Correo de respaldo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.correoRespaldo.isNotBlank() || uiState.correoRespaldoInput.isNotBlank()) {
                        TextButton(
                            onClick = onEliminarCorreoRespaldo,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.correoRespaldoInput,
                    onValueChange = onCorreoRespaldoChange,
                    label = { Text("Correo secundario (opcional)") },
                    placeholder = { Text("Ej: respaldo@correo.cl") },
                    isError = uiState.errorCorreoRespaldoInput != null,
                    supportingText = {
                        uiState.errorCorreoRespaldoInput?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cambiar contraseña",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Doble verificación obligatoria",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.expandirCambioClavePerfil,
                        onCheckedChange = onToggleCambioClave
                    )
                }

                if (cambiaCorreoPrincipal || uiState.expandirCambioClavePerfil) {
                    OutlinedTextField(
                        value = uiState.claveActualPerfil,
                        onValueChange = onClaveActualChange,
                        label = { Text("Contraseña actual (verificación)") },
                        isError = uiState.errorClaveActualPerfil != null,
                        supportingText = {
                            uiState.errorClaveActualPerfil?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (verClaveActual) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verClaveActual = !verClaveActual }) {
                                IconoOjitoContrasena(
                                    visible = verClaveActual,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.expandirCambioClavePerfil) {
                    OutlinedTextField(
                        value = uiState.nuevaClavePerfil,
                        onValueChange = onNuevaClaveChange,
                        label = { Text("Nueva contraseña (mín. 6 caracteres)") },
                        isError = uiState.errorClavePerfil != null,
                        supportingText = {
                            uiState.errorClavePerfil?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (verNuevaClave) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verNuevaClave = !verNuevaClave }) {
                                IconoOjitoContrasena(
                                    visible = verNuevaClave,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.confirmarClavePerfil,
                        onValueChange = onConfirmarClaveChange,
                        label = { Text("Repetir nueva contraseña") },
                        isError = uiState.errorConfirmarClavePerfil != null,
                        supportingText = {
                            when {
                                uiState.errorConfirmarClavePerfil != null -> {
                                    Text(uiState.errorConfirmarClavePerfil, color = MaterialTheme.colorScheme.error)
                                }
                                coincidenNuevas -> {
                                    Text("Las contraseñas coinciden", color = Color(0xFF16A34A))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (verConfirmarClave) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verConfirmarClave = !verConfirmarClave }) {
                                IconoOjitoContrasena(
                                    visible = verConfirmarClave,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) { Text("Guardar Seguridad") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoVincularEmpresasCliente(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onAlternarVinculo: (Int) -> Unit
) {
    val userId = uiState.usuarioActual?.id ?: return
    val idsVinculados = uiState.vinculosUsuarioOrgs[userId].orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Empresas Disponibles") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Activa empresas para recibir sus cobros. No puedes desvincular empresas con compromisos sin pagar:",
                    style = MaterialTheme.typography.bodySmall
                )
                uiState.organizaciones.forEach { org ->
                    val estaVinculada = idsVinculados.contains(org.id)
                    val sinPagar = uiState.compromisos.count { comp ->
                        comp.organizacionId == org.id &&
                                comp.estado != "PAGADO" &&
                                (uiState.asignacionesCobros[comp.id] == null ||
                                        uiState.asignacionesCobros[comp.id]?.usuarioId == userId)
                    }
                    val bloqueadaPorDeuda = estaVinculada && sinPagar > 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlternarVinculo(org.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (estaVinculada) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(org.nombre, fontWeight = FontWeight.Bold)
                                Text("${org.tipo} • ${org.rut}", style = MaterialTheme.typography.bodySmall)
                                if (bloqueadaPorDeuda) {
                                    Text(
                                        text = "$sinPagar compromiso(s) sin pagar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (bloqueadaPorDeuda) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueado por compromisos pendientes",
                                    tint = Color(0xFFD97706)
                                )
                            } else {
                                Checkbox(
                                    checked = estaVinculada,
                                    onCheckedChange = { onAlternarVinculo(org.id) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Listo") }
        }
    )
}

@Composable
private fun DialogoClientesDeOrganizacion(
    organizacion: OrganizacionEntity,
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onAlternarCliente: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clientes de ${organizacion.nombre}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selecciona qué usuarios están vinculados a esta empresa para recibir cobros:",
                    style = MaterialTheme.typography.bodySmall
                )
                if (uiState.clientesDisponibles.isEmpty()) {
                    Text("No hay usuarios con rol USUARIO registrados.")
                } else {
                    uiState.clientesDisponibles.forEach { cliente ->
                        val vinculado = uiState.vinculosUsuarioOrgs[cliente.id]?.contains(organizacion.id) == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAlternarCliente(cliente.id) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cliente.nombre, fontWeight = FontWeight.SemiBold)
                                Text(cliente.correo, style = MaterialTheme.typography.bodySmall)
                            }
                            Checkbox(
                                checked = vinculado,
                                onCheckedChange = { onAlternarCliente(cliente.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DialogoCompromiso(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onSelectOrg: (OrganizacionEntity) -> Unit,
    onSelectCliente: (UsuarioEntity) -> Unit,
    onConceptoChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFechaChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    val clientesVinculadosAEmpresa = remember(
        uiState.clientesDisponibles,
        uiState.vinculosUsuarioOrgs,
        uiState.orgSeleccionadaId
    ) {
        uiState.clientesDisponibles.filter { cliente ->
            uiState.vinculosUsuarioOrgs[cliente.id]?.contains(uiState.orgSeleccionadaId) == true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (uiState.compromisoEditandoId == 0) "Emitir Nuevo Cobro" else "Editar Cobro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("1. Selecciona empresa emisora:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.organizaciones.forEach { org ->
                        FilterChip(
                            selected = uiState.orgSeleccionadaId == org.id,
                            onClick = { onSelectOrg(org) },
                            label = { Text(org.nombre) }
                        )
                    }
                }
                uiState.errorOrgCompromiso?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Text("2. Cliente vinculado a esta empresa:", style = MaterialTheme.typography.labelMedium)
                if (clientesVinculadosAEmpresa.isEmpty()) {
                    Text(
                        text = "Esta empresa no tiene clientes vinculados. Vincúlalos en la pestaña Empresas.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        clientesVinculadosAEmpresa.forEach { cliente ->
                            FilterChip(
                                selected = uiState.clienteSeleccionadoId == cliente.id,
                                onClick = { onSelectCliente(cliente) },
                                label = { Text(cliente.nombre) }
                            )
                        }
                    }
                }
                uiState.errorClienteCompromiso?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = uiState.conceptoCompromiso,
                    onValueChange = onConceptoChange,
                    label = { Text("Concepto (ej: Mensualidad)") },
                    isError = uiState.errorConceptoCompromiso != null,
                    supportingText = { uiState.errorConceptoCompromiso?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.montoCompromiso,
                    onValueChange = onMontoChange,
                    label = { Text("Monto CLP") },
                    isError = uiState.errorMontoCompromiso != null,
                    supportingText = { uiState.errorMontoCompromiso?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.fechaCompromiso,
                    onValueChange = onFechaChange,
                    label = { Text("Fecha vencimiento (DD/MM/AAAA)") },
                    isError = uiState.errorFechaCompromiso != null,
                    supportingText = { uiState.errorFechaCompromiso?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoOrganizacion(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onNombreChange: (String) -> Unit,
    onTipoChange: (String) -> Unit,
    onRutChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    val tipos = listOf("Empresa", "Edificio", "Jardín", "Club")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (uiState.orgEditandoId == 0) "Nueva Organización" else "Editar Organización") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.nombreOrg,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre") },
                    isError = uiState.errorNombreOrg != null,
                    supportingText = { uiState.errorNombreOrg?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tipos.forEach { tipo ->
                        FilterChip(
                            selected = uiState.tipoOrg == tipo,
                            onClick = { onTipoChange(tipo) },
                            label = { Text(tipo) }
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.rutOrg,
                    onValueChange = onRutChange,
                    label = { Text("RUT (ej: 76.123.456-7)") },
                    isError = uiState.errorRutOrg != null,
                    supportingText = { uiState.errorRutOrg?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.correoOrg,
                    onValueChange = onCorreoChange,
                    label = { Text("Correo de contacto") },
                    isError = uiState.errorCorreoOrg != null,
                    supportingText = { uiState.errorCorreoOrg?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}