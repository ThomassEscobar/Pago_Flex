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
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private fun normalizarTextoBusqueda(texto: String): String {
    val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return sinAcentos.lowercase(Locale.getDefault()).trim()
}

private fun compartirInformeCsv(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Informe PagoFlex")
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

    var fotoTemporalUri by remember { mutableStateOf<Uri?>(null) }

    val userActual = uiState.usuarioActual
    val rolActual = userActual?.rol ?: "USUARIO"
    val esAdmin = rolActual == "ADMIN"
    val esEjecutivo = rolActual == "EJECUTIVO"
    val miEmpresaEjecutivo = remember(uiState.organizaciones, userActual) {
        if (esEjecutivo) uiState.organizaciones.find { it.id == userActual?.empresaId } else null
    }
    val empresaEjecutivoSuspendida = miEmpresaEjecutivo?.let { uiState.empresasSuspendidas[it.id] }
    val ticketsPendientesCount = remember(uiState.ticketsSoporte, rolActual, userActual) {
        when (rolActual) {
            "ADMIN" -> uiState.ticketsSoporte.count {
                it.estado == "PENDIENTE" && it.destino == "PAGOFLEX"
            }
            "EJECUTIVO" -> uiState.ticketsSoporte.count {
                it.estado == "PENDIENTE" && it.destino == "EJECUTIVO" &&
                        it.empresaId == userActual?.empresaId
            }
            else -> uiState.ticketsSoporte.count {
                it.estado == "PENDIENTE" && it.usuarioId == userActual?.id
            }
        }
    }

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
        if (exito) {
            viewModel.seleccionarComprobanteBorrador(fotoTemporalUri?.toString())
            viewModel.mostrarMensaje("Foto del comprobante adjuntada. Presiona 'Confirmar Pago' para enviar.")
        } else {
            viewModel.mostrarMensaje("Captura de foto cancelada")
        }
    }

    val selectorArchivoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.seleccionarComprobanteBorrador(uri.toString())
            viewModel.mostrarMensaje("Archivo adjuntado. Presiona 'Confirmar Pago' para enviar.")
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
            viewModel.mostrarMensaje("Permiso de cámara denegado. Usa 'Galería / PDF' para elegir desde archivos.")
        }
    }

    val permisoNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            NotificadorPagoFlex.mostrarNotificacion(
                context,
                "Alertas activadas",
                "Recibirás avisos de vencimientos, strikes y solicitudes en PagoFlex."
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
                        userActual?.let { user ->
                            val subtituloTop = when (user.rol) {
                                "EJECUTIVO" -> "${user.nombre} • Ejecutivo ${miEmpresaEjecutivo?.nombre ?: "(Sin empresa)"}"
                                "ADMIN" -> "${user.nombre} • Admin Plataforma"
                                else -> "${user.nombre} (Cliente)"
                            }
                            Text(
                                text = subtituloTop,
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
                            contentDescription = "Exportar informe CSV"
                        )
                    }
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permisoNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificadorPagoFlex.mostrarNotificacion(
                                context,
                                "Recordatorio PagoFlex",
                                "Revisa tus compromisos, vencimientos y solicitudes en PagoFlex."
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
                if (esAdmin) {
                    NavigationBarItem(
                        selected = tabSeleccionado == 0,
                        onClick = { tabSeleccionado = 0 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Plataforma") }
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
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (ticketsPendientesCount > 0) {
                                        Badge { Text(ticketsPendientesCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null)
                            }
                        },
                        label = { Text("Soporte") }
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 3,
                        onClick = { tabSeleccionado = 3 },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Perfil") }
                    )
                } else {
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
                        label = { Text(if (esEjecutivo) "Mi Empresa" else "Empresas") }
                    )
                    // El ejecutivo tiene bandeja de soporte: recibe los tickets de sus clientes
                    if (esEjecutivo) {
                        NavigationBarItem(
                            selected = tabSeleccionado == 2,
                            onClick = { tabSeleccionado = 2 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (ticketsPendientesCount > 0) {
                                            Badge { Text(ticketsPendientesCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                }
                            },
                            label = { Text("Soporte") }
                        )
                    }
                    val indicePerfil = if (esEjecutivo) 3 else 2
                    NavigationBarItem(
                        selected = tabSeleccionado == indicePerfil,
                        onClick = { tabSeleccionado = indicePerfil },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Perfil") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tabSeleccionado == 0 && esEjecutivo && miEmpresaEjecutivo != null && empresaEjecutivoSuspendida == null) {
                FloatingActionButton(onClick = { viewModel.abrirDialogoNuevoCompromiso() }) {
                    Icon(Icons.Default.Add, contentDescription = "Registrar Cobro")
                }
            } else if (tabSeleccionado == 1 && !esAdmin && !esEjecutivo) {
                FloatingActionButton(onClick = { viewModel.abrirDialogoVincularEmpresaUsuario() }) {
                    Icon(Icons.Default.Add, contentDescription = "Solicitar Vinculación")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (esAdmin) {
                when (tabSeleccionado) {
                    0 -> VistaAdminResumenEmpresas(
                        uiState = uiState,
                        calcularStrikes = { viewModel.calcularStrikesCliente(it).first },
                        onBusquedaChange = { viewModel.onBusquedaPlataformaAdminChange(it) },
                        onFiltroEmpresasChange = { viewModel.onFiltroEmpresasAdminChange(it) },
                        onSuspenderEmpresa = { viewModel.abrirDialogoSuspenderEmpresaAdmin(it) },
                        onGestionarUsuario = { viewModel.abrirDialogoGestionarUsuarioAdmin(it) }
                    )
                    1 -> VistaAdminPlataforma(
                        uiState = uiState,
                        calcularStrikes = { viewModel.calcularStrikesCliente(it).first },
                        onBusquedaChange = { viewModel.onBusquedaAdminChange(it) },
                        onFiltroStrikesChange = { viewModel.onFiltroStrikesAdminChange(it) },
                        onSuspenderOrg = { viewModel.abrirDialogoSuspenderEmpresaAdmin(it) },
                        onGestionarUsuario = { viewModel.abrirDialogoGestionarUsuarioAdmin(it) }
                    )
                    2 -> VistaAdminSoporte(
                        uiState = uiState,
                        titulo = "Mesa de Ayuda PagoFlex",
                        subtitulo = "Reportes enviados por los ejecutivos de las empresas",
                        // La administración solo atiende lo que va dirigido a PagoFlex
                        tickets = uiState.ticketsSoporte.filter { it.destino == "PAGOFLEX" },
                        onFiltroChange = { viewModel.onFiltroSoporteAdminChange(it) },
                        onResponderTicket = { viewModel.abrirDialogoResponderTicket(it) }
                    )
                    3 -> VistaPerfil(
                        uiState = uiState,
                        strikesPropios = 0,
                        onCambiarFotoPerfil = { viewModel.abrirDialogoFotoPerfil() },
                        onAbrirDatosPersonales = { viewModel.abrirDialogoDatosPersonales() },
                        onAbrirSeguridad = { viewModel.abrirDialogoSeguridad() },
                        onAbrirSoporte = { viewModel.abrirDialogoSoporteUsuario() },
                        onAvisoColorFuturo = {
                            viewModel.mostrarMensaje("Personalización de colores del tema disponible en la etapa visual final.")
                        },
                        onExportarCsv = { viewModel.exportarReporteCsv(context) },
                        onCerrarSesion = onCerrarSesion
                    )
                }
            } else {
                when (tabSeleccionado) {
                    0 -> VistaCompromisos(
                        uiState = uiState,
                        motivoSuspensionEmpresa = empresaEjecutivoSuspendida,
                        onFiltroChange = { viewModel.onFiltroEstadoChange(it) },
                        onBusquedaChange = { viewModel.onBusquedaChange(it) },
                        onAbrirDetalle = { viewModel.abrirDetalleCompromiso(it) },
                        onVerComprobante = abrirComprobante,
                        onResolverComprobante = { compId, aprobado ->
                            viewModel.resolverComprobante(context, compId, aprobado)
                        }
                    )
                    1 -> when {
                        esEjecutivo -> VistaEmpresaEjecutivo(
                            uiState = uiState,
                            miEmpresa = miEmpresaEjecutivo,
                            motivoSuspensionEmpresa = empresaEjecutivoSuspendida,
                            calcularStrikes = { viewModel.calcularStrikesCliente(it).first },
                            onBusquedaChange = { viewModel.onBusquedaClientesEmpresaChange(it) },
                            onAbrirAgregarCliente = { viewModel.abrirDialogoAgregarClienteEjecutivo() },
                            onRevisarSolicitud = { viewModel.abrirRevisionPerfilCliente(it) }
                        )
                        else -> VistaEmpresasUsuario(
                            uiState = uiState,
                            onAbrirRegistrarEmpresa = { viewModel.abrirDialogoRegistrarEmpresaUsuario() },
                            onReintentarSolicitud = { orgId ->
                                userActual?.let { user ->
                                    viewModel.solicitarVinculacionEmpresa(context, user.id, orgId)
                                }
                            }
                        )
                    }
                    2, 3 -> when {
                        // Bandeja del ejecutivo: tickets que le envían sus clientes
                        tabSeleccionado == 2 && esEjecutivo -> VistaAdminSoporte(
                            uiState = uiState,
                            titulo = "Soporte de mis clientes",
                            subtitulo = "Tickets enviados por los clientes de " +
                                    (miEmpresaEjecutivo?.nombre ?: "tu empresa"),
                            tickets = uiState.ticketsSoporte.filter { ticket ->
                                ticket.destino == "EJECUTIVO" &&
                                        ticket.empresaId == miEmpresaEjecutivo?.id
                            },
                            onFiltroChange = { viewModel.onFiltroSoporteAdminChange(it) },
                            onResponderTicket = { viewModel.abrirDialogoResponderTicket(it) }
                        )
                        else -> VistaPerfil(
                            uiState = uiState,
                            strikesPropios = userActual?.let { viewModel.calcularStrikesCliente(it.id).first } ?: 0,
                            onCambiarFotoPerfil = { viewModel.abrirDialogoFotoPerfil() },
                            onAbrirDatosPersonales = { viewModel.abrirDialogoDatosPersonales() },
                            onAbrirSeguridad = { viewModel.abrirDialogoSeguridad() },
                            onAbrirSoporte = { viewModel.abrirDialogoSoporteUsuario() },
                            onAvisoColorFuturo = {
                                viewModel.mostrarMensaje("Personalización de colores del tema disponible en la etapa visual final.")
                            },
                            onExportarCsv = { viewModel.exportarReporteCsv(context) },
                            onCerrarSesion = onCerrarSesion
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // DIÁLOGOS MODALES
        // ---------------------------------------------------------

        val compromisoDetalle = uiState.compromisoDetalleSeleccionado
        if (compromisoDetalle != null) {
            DialogoDetalleCompromiso(
                compromiso = compromisoDetalle,
                clienteNombre = uiState.asignacionesCobros[compromisoDetalle.id]?.usuarioNombre,
                esEjecutivo = esEjecutivo,
                comprobanteBorradorUri = uiState.comprobanteBorradorUri,
                onTomarFoto = {
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
                onAdjuntarDigital = {
                    selectorArchivoLauncher.launch(arrayOf("image/*", "application/pdf"))
                },
                onVerComprobante = abrirComprobante,
                onConfirmarPago = { viewModel.confirmarPagoConComprobante(context) },
                onResolverComprobante = { aprobado ->
                    viewModel.resolverComprobante(context, compromisoDetalle.id, aprobado)
                },
                onDismiss = { viewModel.cerrarDetalleCompromiso() }
            )
        }

        if (uiState.mostrarDialogoCompromiso && esEjecutivo) {
            DialogoCompromiso(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoCompromiso() },
                onSelectCliente = { viewModel.onSeleccionarClienteCompromiso(it) },
                onConceptoChange = { viewModel.onConceptoChange(it) },
                onMontoChange = { viewModel.onMontoChange(it) },
                onFechaChange = { viewModel.onFechaChange(it) },
                onToggleRecurrente = { viewModel.onToggleCobroRecurrenteChange(it) },
                onMesesRecurrenciaChange = { viewModel.onMesesRecurrenciaChange(it) },
                onGuardar = { viewModel.guardarCompromiso(context) }
            )
        }

        if (uiState.mostrarDialogoRegistrarEmpresaUsuario && !esAdmin && !esEjecutivo) {
            DialogoRegistrarEmpresaUsuario(
                uiState = uiState,
                onNombreChange = { viewModel.onNuevaEmpresaNombreChange(it) },
                onTipoChange = { viewModel.onNuevaEmpresaTipoChange(it) },
                onRutChange = { viewModel.onNuevaEmpresaRutChange(it) },
                onCorreoChange = { viewModel.onNuevaEmpresaCorreoChange(it) },
                onGuardar = { viewModel.registrarNuevaEmpresaUsuario(context) },
                onDismiss = { viewModel.cerrarDialogoRegistrarEmpresaUsuario() }
            )
        }

        if (uiState.mostrarDialogoAgregarClienteEjecutivo && esEjecutivo) {
            DialogoAgregarClienteEjecutivo(
                uiState = uiState,
                onNombreChange = { viewModel.onNuevoClienteNombreChange(it) },
                onRutChange = { viewModel.onNuevoClienteRutChange(it) },
                onCorreoChange = { viewModel.onNuevoClienteCorreoChange(it) },
                onGuardar = { viewModel.agregarClienteAEmpresaEjecutivo(context) },
                onDismiss = { viewModel.cerrarDialogoAgregarClienteEjecutivo() }
            )
        }

        val orgSusp = uiState.orgSuspendiendoAdmin
        if (orgSusp != null && esAdmin) {
            DialogoSuspenderEmpresaAdmin(
                organizacion = orgSusp,
                motivoActual = uiState.empresasSuspendidas[orgSusp.id],
                motivoInput = uiState.motivoSuspensionOrgInput,
                errorMotivo = uiState.errorMotivoSuspensionOrg,
                onMotivoChange = { viewModel.onMotivoSuspensionOrgChange(it) },
                onConfirmar = { suspender -> viewModel.toggleSuspensionEmpresaAdmin(context, suspender) },
                onDismiss = { viewModel.cerrarDialogoSuspenderEmpresaAdmin() }
            )
        }

        if (uiState.mostrarDialogoVincularEmpresaUsuario && !esAdmin && !esEjecutivo) {
            DialogoSolicitarEmpresasCliente(
                uiState = uiState,
                onDismiss = { viewModel.cerrarDialogoVincularEmpresaUsuario() },
                onSolicitarEmpresa = { orgId ->
                    userActual?.let { user ->
                        viewModel.solicitarVinculacionEmpresa(context, user.id, orgId)
                    }
                }
            )
        }

        val perfilRevision = uiState.perfilFinancieroEnRevision
        if (perfilRevision != null && esEjecutivo) {
            DialogoRevisionPerfilCliente(
                perfil = perfilRevision,
                motivoRechazo = uiState.motivoRechazoSolicitudInput,
                errorMotivo = uiState.errorMotivoRechazoSolicitud,
                onMotivoChange = { viewModel.onMotivoRechazoSolicitudChange(it) },
                onAprobar = { viewModel.resolverSolicitudVinculacion(context, true) },
                onRechazar = { viewModel.resolverSolicitudVinculacion(context, false) },
                onBloquearCliente = { viewModel.cambiarBloqueoClienteEmpresa(context, true) },
                onDesbloquearCliente = { viewModel.cambiarBloqueoClienteEmpresa(context, false) },
                onDismiss = { viewModel.cerrarRevisionPerfilCliente() }
            )
        }

        val usuarioEditAdmin = uiState.usuarioEditandoAdmin
        if (usuarioEditAdmin != null && esAdmin) {
            val (strikesUsuario, detalleStrikes) = viewModel.calcularStrikesCliente(usuarioEditAdmin.id)
            DialogoGestionarUsuarioAdmin(
                usuario = usuarioEditAdmin,
                strikesCount = strikesUsuario,
                detalleStrikes = detalleStrikes,
                motivoBloqueoActual = uiState.bloqueosGlobalesApp[usuarioEditAdmin.id],
                motivoBloqueoInput = uiState.motivoBloqueoAppInput,
                errorMotivoBloqueo = uiState.errorMotivoBloqueoApp,
                onMotivoBloqueoChange = { viewModel.onMotivoBloqueoAppChange(it) },
                onToggleBloqueoApp = { bloquear ->
                    viewModel.toggleBloqueoGlobalUsuarioApp(context, usuarioEditAdmin, bloquear)
                },
                onDismiss = { viewModel.cerrarDialogoGestionarUsuarioAdmin() }
            )
        }

        if (uiState.mostrarDialogoSoporteUsuario && !esAdmin) {
            DialogoSoporteUsuario(
                uiState = uiState,
                onCategoriaChange = { viewModel.onCategoriaSoporteChange(it) },
                onAsuntoChange = { viewModel.onAsuntoSoporteChange(it) },
                onMensajeChange = { viewModel.onMensajeSoporteChange(it) },
                onEnviar = { viewModel.enviarTicketSoporte(context) },
                onDismiss = { viewModel.cerrarDialogoSoporteUsuario() }
            )
        }

        val ticketRevision = uiState.ticketEnRevisionAdmin
        if (ticketRevision != null && ticketRevision.estado == "PENDIENTE" && esAdmin) {
            DialogoResponderTicketAdmin(
                ticket = ticketRevision,
                respuestaInput = uiState.respuestaTicketAdminInput,
                errorRespuesta = uiState.errorRespuestaTicketAdmin,
                onRespuestaChange = { viewModel.onRespuestaTicketAdminChange(it) },
                onConfirmar = { viewModel.resolverTicketSoporte(context) },
                onDismiss = { viewModel.cerrarDialogoResponderTicket() }
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
                onClaveActualChange = { viewModel.onClaveActualPerfilChange(it) },
                onNuevaClaveChange = { viewModel.onNuevaClavePerfilChange(it) },
                onConfirmarClaveChange = { viewModel.onConfirmarClavePerfilChange(it) },
                onGuardar = { viewModel.actualizarContrasenaSegura() }
            )
        }
    }
}

// =========================================================================
// VISTAS EXCLUSIVAS ADMINISTRADOR
// =========================================================================

@Composable
private fun VistaAdminResumenEmpresas(
    uiState: InicioUiState,
    calcularStrikes: (Int) -> Int,
    onBusquedaChange: (String) -> Unit,
    onFiltroEmpresasChange: (String) -> Unit,
    onSuspenderEmpresa: (OrganizacionEntity) -> Unit,
    onGestionarUsuario: (UsuarioEntity) -> Unit
) {
    val formatoClp = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val queryNorm = remember(uiState.busquedaPlataformaAdmin) {
        normalizarTextoBusqueda(uiState.busquedaPlataformaAdmin)
    }

    val volumenGlobal = remember(uiState.compromisos) { uiState.compromisos.sumOf { it.monto } }
    val conciliadoGlobal = remember(uiState.compromisos) {
        uiState.compromisos.filter { it.estado == "PAGADO" }.sumOf { it.monto }
    }
    val moraGlobal = remember(uiState.compromisos) {
        uiState.compromisos.filter {
            it.estado != "PAGADO" && (InicioViewModel.calcularDiasParaVencimiento(it.fechaVencimiento) ?: 0) < 0
        }.sumOf { it.monto }
    }

    val clientesConStrikes = remember(uiState.clientesDisponibles, uiState.compromisos, uiState.asignacionesCobros, uiState.vinculosUsuarioOrgs) {
        uiState.clientesDisponibles
            .map { it to calcularStrikes(it.id) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
    }

    val filtrosEmpresa = listOf("TODAS", "ACTIVAS", "CON MORA", "SUSPENDIDAS")

    val empresasConsolidadas = remember(
        uiState.organizaciones,
        uiState.compromisos,
        uiState.ejecutivosDisponibles,
        uiState.clientesDisponibles,
        uiState.vinculosUsuarioOrgs,
        uiState.solicitudesVinculacion,
        uiState.empresasSuspendidas,
        uiState.filtroEmpresasAdmin,
        queryNorm
    ) {
        uiState.organizaciones.filter { org ->
            val compsOrg = uiState.compromisos.filter { it.organizacionId == org.id }
            val tieneMora = compsOrg.any {
                it.estado != "PAGADO" && (InicioViewModel.calcularDiasParaVencimiento(it.fechaVencimiento) ?: 0) < 0
            }
            val ejecutivos = uiState.ejecutivosDisponibles.filter { it.empresaId == org.id }
            val estaSuspendida = uiState.empresasSuspendidas.containsKey(org.id)

            val coincideFiltro = when (uiState.filtroEmpresasAdmin) {
                "ACTIVAS" -> !estaSuspendida
                "CON MORA" -> tieneMora
                "SUSPENDIDAS" -> estaSuspendida
                else -> true
            }

            val coincideTexto = queryNorm.isEmpty() ||
                    normalizarTextoBusqueda(org.nombre).contains(queryNorm) ||
                    normalizarTextoBusqueda(org.rut).contains(queryNorm) ||
                    normalizarTextoBusqueda(org.tipo).contains(queryNorm) ||
                    ejecutivos.any { normalizarTextoBusqueda(it.nombre).contains(queryNorm) }

            coincideFiltro && coincideTexto
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Control Consolidado por Empresas",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.organizaciones.size} empresas (${uiState.empresasSuspendidas.size} susp.) • ${uiState.clientesDisponibles.size} clientes • ${uiState.ejecutivosDisponibles.size} ejecutivos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Volumen Emitido", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatoClp.format(volumenGlobal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mora: ${formatoClp.format(moraGlobal)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (moraGlobal > 0.0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Conciliado Global", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatoClp.format(conciliadoGlobal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val tasaGlobal = if (volumenGlobal > 0.0) ((conciliadoGlobal * 100.0) / volumenGlobal).toInt() else 100
                        Text(
                            text = "Recaudación: $tasaGlobal%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (clientesConStrikes.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Radar de Strikes (${clientesConStrikes.size} cliente(s) en riesgo)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        clientesConStrikes.take(3).forEach { (cliente, strikes) ->
                            val suspendido = uiState.bloqueosGlobalesApp.containsKey(cliente.id)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${cliente.nombre} • $strikes/${InicioViewModel.STRIKES_UMBRAL_APP} Strike(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = if (suspendido) "Cuenta suspendida en la app"
                                        else if (strikes >= InicioViewModel.STRIKES_UMBRAL_APP) "Umbral alcanzado: listo para bloqueo global"
                                        else "RUT: ${cliente.rut.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onGestionarUsuario(cliente) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (suspendido) "Gestionar" else "Sancionar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.busquedaPlataformaAdmin,
                onValueChange = onBusquedaChange,
                placeholder = { Text("Buscar empresa por nombre, RUT, rubro o ejecutivo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.busquedaPlataformaAdmin.isNotBlank()) {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtrosEmpresa.forEach { filtro ->
                    FilterChip(
                        selected = uiState.filtroEmpresasAdmin == filtro,
                        onClick = { onFiltroEmpresasChange(filtro) },
                        label = { Text(filtro) }
                    )
                }
            }
        }

        if (empresasConsolidadas.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No hay empresas que coincidan con el filtro seleccionado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(empresasConsolidadas, key = { "resumen_org_${it.id}" }) { org ->
                val compsOrg = uiState.compromisos.filter { it.organizacionId == org.id }
                val totalEmitido = compsOrg.sumOf { it.monto }
                val totalConciliado = compsOrg.filter { it.estado == "PAGADO" }.sumOf { it.monto }
                val totalPendiente = compsOrg.filter { it.estado != "PAGADO" }.sumOf { it.monto }
                val compsVencidos = compsOrg.filter {
                    it.estado != "PAGADO" && (InicioViewModel.calcularDiasParaVencimiento(it.fechaVencimiento) ?: 0) < 0
                }
                val totalVencido = compsVencidos.sumOf { it.monto }
                val porcentajeRecaudacion = if (totalEmitido > 0.0) (totalConciliado / totalEmitido).toFloat() else 1f

                val ejecutivosOrg = uiState.ejecutivosDisponibles.filter { it.empresaId == org.id }
                val clientesActivosOrg = uiState.clientesDisponibles.count {
                    uiState.vinculosUsuarioOrgs[it.id]?.contains(org.id) == true
                }
                val solicitudesPendientesOrg = uiState.solicitudesVinculacion.count {
                    it.organizacionId == org.id && it.estado == EstadoVinculo.PENDIENTE
                }
                val motivoSusp = uiState.empresasSuspendidas[org.id]
                val estaSuspendida = motivoSusp != null

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (estaSuspendida) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = org.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${org.tipo} • RUT: ${org.rut}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (estaSuspendida) Color(0xFFDC2626).copy(alpha = 0.16f) else Color(0xFF16A34A).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (estaSuspendida) "SUSPENDIDA" else "ACTIVA",
                                    color = if (estaSuspendida) Color(0xFFDC2626) else Color(0xFF16A34A),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (motivoSusp != null) {
                            Text(
                                text = "Motivo suspensión: $motivoSusp",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Ejecutivo(s): " + if (ejecutivosOrg.isEmpty()) "Sin ejecutivo" else ejecutivosOrg.joinToString(", ") { it.nombre },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Emitido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatoClp.format(totalEmitido), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Conciliado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatoClp.format(totalConciliado), fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Por Cobrar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatoClp.format(totalPendiente),
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalVencido > 0.0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tasa de recaudación: ${(porcentajeRecaudacion * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (totalVencido > 0.0) {
                                    Text(
                                        text = "Mora vencida: ${formatoClp.format(totalVencido)} (${compsVencidos.size} strike(s))",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { porcentajeRecaudacion.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$clientesActivosOrg cliente(s) • $solicitudesPendientesOrg solicitud(es) • ${compsOrg.size} cobro(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { onSuspenderEmpresa(org) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (estaSuspendida) "Reactivar" else "Suspender",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (estaSuspendida) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaAdminPlataforma(
    uiState: InicioUiState,
    calcularStrikes: (Int) -> Int,
    onBusquedaChange: (String) -> Unit,
    onFiltroStrikesChange: (String) -> Unit,
    onSuspenderOrg: (OrganizacionEntity) -> Unit,
    onGestionarUsuario: (UsuarioEntity) -> Unit
) {
    val queryNorm = normalizarTextoBusqueda(uiState.busquedaAdmin)

    val empresasFiltradas = remember(uiState.organizaciones, queryNorm) {
        if (queryNorm.isEmpty()) uiState.organizaciones
        else uiState.organizaciones.filter {
            normalizarTextoBusqueda(it.nombre).contains(queryNorm) ||
                    normalizarTextoBusqueda(it.rut).contains(queryNorm) ||
                    normalizarTextoBusqueda(it.tipo).contains(queryNorm)
        }
    }

    val usuariosFiltrados = remember(uiState.todosUsuarios, uiState.filtroStrikesAdmin, uiState.bloqueosGlobalesApp, queryNorm) {
        uiState.todosUsuarios.filter { u ->
            val strikes = if (u.rol == "USUARIO") calcularStrikes(u.id) else 0
            val suspendido = uiState.bloqueosGlobalesApp.containsKey(u.id)
            val coincideFiltroStrikes = when (uiState.filtroStrikesAdmin) {
                "CON STRIKES" -> strikes > 0
                "CRITICOS (≥3)" -> strikes >= InicioViewModel.STRIKES_UMBRAL_APP
                "SUSPENDIDOS" -> suspendido
                else -> true
            }
            val coincideTexto = queryNorm.isEmpty() ||
                    normalizarTextoBusqueda(u.nombre).contains(queryNorm) ||
                    normalizarTextoBusqueda(u.rut).contains(queryNorm) ||
                    normalizarTextoBusqueda(u.correo).contains(queryNorm) ||
                    normalizarTextoBusqueda(u.rol).contains(queryNorm)
            coincideFiltroStrikes && coincideTexto
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
            OutlinedTextField(
                value = uiState.busquedaAdmin,
                onValueChange = onBusquedaChange,
                placeholder = { Text("Buscar empresa o cuenta por nombre, RUT, correo o rol...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.busquedaAdmin.isNotBlank()) {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Column {
                Text(
                    text = "Empresas Registradas (${empresasFiltradas.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supervisa las entidades inscritas y suspende su operación si es necesario",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(empresasFiltradas, key = { it.id }) { org ->
            val ejecutivosDeEstaOrg = uiState.ejecutivosDisponibles.filter { it.empresaId == org.id }
            val clientesAprobados = uiState.clientesDisponibles.filter { cliente ->
                uiState.vinculosUsuarioOrgs[cliente.id]?.contains(org.id) == true
            }
            val motivoSusp = uiState.empresasSuspendidas[org.id]

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (motivoSusp != null) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(org.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (motivoSusp != null) {
                                Surface(color = Color(0xFFDC2626), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = "SUSPENDIDA",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text("Tipo: ${org.tipo} • RUT: ${org.rut}", style = MaterialTheme.typography.bodySmall)
                        Text(org.correoContacto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ejecutivo(s): " + if (ejecutivosDeEstaOrg.isEmpty()) "Sin ejecutivo" else ejecutivosDeEstaOrg.joinToString(", ") { it.nombre },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Clientes activos: ${clientesAprobados.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(onClick = { onSuspenderOrg(org) }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Suspender o reactivar empresa",
                            tint = if (motivoSusp != null) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Directorio de Cuentas, Strikes y Bloqueos (${usuariosFiltrados.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Filtra por historial de strikes o toca una cuenta para gestionar su suspensión global",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODOS", "CON STRIKES", "CRITICOS (≥3)", "SUSPENDIDOS").forEach { f ->
                    FilterChip(
                        selected = uiState.filtroStrikesAdmin == f,
                        onClick = { onFiltroStrikesChange(f) },
                        label = { Text(f) }
                    )
                }
            }
        }

        items(usuariosFiltrados, key = { "user_${it.id}" }) { usuario ->
            val nombreEmpresaAsignada = if (usuario.rol == "EJECUTIVO") {
                uiState.organizaciones.find { it.id == usuario.empresaId }?.nombre
            } else {
                null
            }
            val strikes = if (usuario.rol == "USUARIO") calcularStrikes(usuario.id) else 0
            val suspendidoApp = uiState.bloqueosGlobalesApp.containsKey(usuario.id)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGestionarUsuario(usuario) },
                colors = CardDefaults.cardColors(
                    containerColor = if (suspendidoApp) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(usuario.nombre, fontWeight = FontWeight.Bold)
                        Text(
                            text = "RUT: ${usuario.rut.ifBlank { "No registrado" }} • ${usuario.correo}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (nombreEmpresaAsignada != null) {
                            Text(
                                text = "Empresa: $nombreEmpresaAsignada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (strikes > 0) {
                            Text(
                                text = "⚠️ $strikes / ${InicioViewModel.STRIKES_UMBRAL_APP} Strike(s) acumulado(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = usuario.rol,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (suspendidoApp) {
                            Surface(
                                color = Color(0xFF991B1B),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "SUSPENDIDO APP",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaAdminSoporte(
    uiState: InicioUiState,
    titulo: String = "Mesa de Ayuda y Soporte",
    subtitulo: String = "Mensajes y reportes de la plataforma",
    tickets: List<TicketSoporte> = uiState.ticketsSoporte,
    onFiltroChange: (String) -> Unit,
    onResponderTicket: (TicketSoporte) -> Unit
) {
    val ticketsFiltrados = remember(tickets, uiState.filtroSoporteAdmin) {
        when (uiState.filtroSoporteAdmin) {
            "PENDIENTES" -> tickets.filter { it.estado == "PENDIENTE" }
            "RESUELTOS" -> tickets.filter { it.estado == "RESUELTO" }
            else -> tickets
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
                    text = "$titulo (${ticketsFiltrados.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PENDIENTES", "RESUELTOS", "TODOS").forEach { estado ->
                    FilterChip(
                        selected = uiState.filtroSoporteAdmin == estado,
                        onClick = { onFiltroChange(estado) },
                        label = { Text(estado) }
                    )
                }
            }
        }

        if (ticketsFiltrados.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No hay tickets de soporte en esta categoría.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(ticketsFiltrados, key = { it.id }) { ticket ->
                val esPendiente = ticket.estado == "PENDIENTE"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ticket.id} • ${ticket.categoria}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (esPendiente) Color(0xFFD97706).copy(alpha = 0.16f) else Color(0xFF16A34A).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = ticket.estado,
                                    color = if (esPendiente) Color(0xFFB45309) else Color(0xFF16A34A),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = ticket.asunto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "De: ${ticket.usuarioNombre} (${ticket.usuarioRol})" +
                                    (ticket.empresaNombre?.let { " • $it" } ?: "") +
                                    " • ${ticket.fechaCreacion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ticket.mensaje,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        if (!ticket.respuestaAdmin.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (ticket.destino == "EJECUTIVO")
                                            "Respuesta del ejecutivo (${ticket.fechaRespuesta ?: ""}):"
                                        else
                                            "Respuesta Administración (${ticket.fechaRespuesta ?: ""}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = ticket.respuestaAdmin,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        if (esPendiente) {
                            Button(
                                onClick = { onResponderTicket(ticket) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Responder y Resolver")
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// VISTAS CLIENTE Y EJECUTIVO
// =========================================================================

@Composable
private fun VistaCompromisos(
    uiState: InicioUiState,
    motivoSuspensionEmpresa: String?,
    onFiltroChange: (String) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onAbrirDetalle: (CompromisoEntity) -> Unit,
    onVerComprobante: (String) -> Unit,
    onResolverComprobante: (Int, Boolean) -> Unit
) {
    val formatoClp = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val userActual = uiState.usuarioActual
    val rol = userActual?.rol ?: "USUARIO"
    val esEjecutivo = rol == "EJECUTIVO"

    val clientesAprobadosPorOrg = remember(
        uiState.organizaciones,
        uiState.clientesDisponibles,
        uiState.vinculosUsuarioOrgs
    ) {
        val mapa = mutableMapOf<Int, String>()
        uiState.organizaciones.forEach { org ->
            val nombres = uiState.clientesDisponibles
                .filter { cliente -> uiState.vinculosUsuarioOrgs[cliente.id]?.contains(org.id) == true }
                .joinToString(", ") { it.nombre }
            if (nombres.isNotBlank()) {
                mapa[org.id] = nombres
            }
        }
        mapa
    }

    val compromisosVisiblesPorRol = remember(
        uiState.compromisos,
        uiState.vinculosUsuarioOrgs,
        uiState.asignacionesCobros,
        userActual
    ) {
        when {
            userActual == null -> emptyList()
            esEjecutivo -> {
                val miOrgId = userActual.empresaId ?: -1
                uiState.compromisos.filter { it.organizacionId == miOrgId }
            }
            else -> {
                val orgsAprobadas = uiState.vinculosUsuarioOrgs[userActual.id].orEmpty()
                uiState.compromisos.filter { comp ->
                    val perteneceAEmpresaAprobada = orgsAprobadas.contains(comp.organizacionId)
                    val asignacion = uiState.asignacionesCobros[comp.id]
                    val correspondeAlUsuario = asignacion == null || asignacion.usuarioId == userActual.id
                    perteneceAEmpresaAprobada && correspondeAlUsuario
                }
            }
        }
    }

    val cantidadPorVencer = remember(compromisosVisiblesPorRol) {
        compromisosVisiblesPorRol.count { c ->
            (c.estado == "PENDIENTE" || c.estado == "RECHAZADO") &&
                    (InicioViewModel.calcularDiasParaVencimiento(c.fechaVencimiento) ?: 99) in 0..3
        }
    }

    val cantidadVencidosStrike = remember(compromisosVisiblesPorRol) {
        compromisosVisiblesPorRol.count { c ->
            c.estado != "PAGADO" &&
                    (InicioViewModel.calcularDiasParaVencimiento(c.fechaVencimiento) ?: 0) < 0
        }
    }

    val estados = if (esEjecutivo) {
        listOf("POR VENCER", "VENCIDOS", "REVISION", "PENDIENTE", "RECHAZADO", "PAGADO", "TODOS")
    } else {
        listOf("TODOS", "POR VENCER", "VENCIDOS", "PENDIENTE", "REVISION", "PAGADO", "RECHAZADO")
    }

    val tituloTarjetaIzquierda = if (esEjecutivo) "Por Validar" else "Por Pagar"
    val montoTarjetaIzquierda = remember(compromisosVisiblesPorRol, esEjecutivo) {
        if (esEjecutivo) {
            compromisosVisiblesPorRol.filter { it.estado == "REVISION" }.sumOf { it.monto }
        } else {
            compromisosVisiblesPorRol
                .filter { InicioViewModel.esCompromisoExigibleAhora(it) }
                .sumOf { it.monto }
        }
    }
    val montoFuturoRecurrente = remember(compromisosVisiblesPorRol, esEjecutivo) {
        if (esEjecutivo) {
            0.0
        } else {
            compromisosVisiblesPorRol
                .filter {
                    (it.estado == "PENDIENTE" || it.estado == "RECHAZADO") &&
                            !InicioViewModel.esCompromisoExigibleAhora(it)
                }
                .sumOf { it.monto }
        }
    }

    val tituloTarjetaDerecha = if (esEjecutivo) "Conciliado" else "Pagado"
    val montoTarjetaDerecha = compromisosVisiblesPorRol.filter { it.estado == "PAGADO" }.sumOf { it.monto }

    val queryNorm = remember(uiState.textoBusqueda) {
        normalizarTextoBusqueda(uiState.textoBusqueda)
    }

    val filtrados = compromisosVisiblesPorRol.filter { c ->
        val clienteNombre = uiState.asignacionesCobros[c.id]?.usuarioNombre
            ?: clientesAprobadosPorOrg[c.organizacionId].orEmpty()
        val diasVence = InicioViewModel.calcularDiasParaVencimiento(c.fechaVencimiento)
        val coincideEstado = when (uiState.filtroEstado) {
            "TODOS" -> true
            "POR VENCER" -> (c.estado == "PENDIENTE" || c.estado == "RECHAZADO") && (diasVence != null && diasVence in 0..3)
            "VENCIDOS" -> c.estado != "PAGADO" && (diasVence != null && diasVence < 0)
            else -> c.estado == uiState.filtroEstado
        }
        val coincideTexto = queryNorm.isEmpty() ||
                normalizarTextoBusqueda(c.empresaNombre).contains(queryNorm) ||
                normalizarTextoBusqueda(c.concepto).contains(queryNorm) ||
                normalizarTextoBusqueda(clienteNombre).contains(queryNorm)
        coincideEstado && coincideTexto
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (esEjecutivo && motivoSuspensionEmpresa != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Tu empresa está suspendida por Administración: $motivoSuspensionEmpresa. No puedes registrar nuevos cobros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else if (esEjecutivo && userActual?.empresaId == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Tu cuenta Ejecutivo no tiene una empresa vinculada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if ((cantidadPorVencer > 0 || cantidadVencidosStrike > 0) && userActual != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable {
                        onFiltroChange(if (cantidadPorVencer > 0) "POR VENCER" else "VENCIDOS")
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (cantidadVencidosStrike > 0) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    } else {
                        Color(0xFFFEF3C7)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (cantidadVencidosStrike > 0) MaterialTheme.colorScheme.error else Color(0xFFD97706)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (esEjecutivo) "Radar de Vencimientos y Strikes" else "Aviso de Vencimientos",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        val resumen = buildList {
                            if (cantidadPorVencer > 0) add("$cantidadPorVencer por vencer (≤ 3 días)")
                            if (cantidadVencidosStrike > 0) add("$cantidadVencidosStrike vencido(s) con Strike")
                        }.joinToString(" • ")
                        Text(
                            text = "$resumen. Toca para filtrar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }

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
                    if (!esEjecutivo && montoFuturoRecurrente > 0.0) {
                        Text(
                            text = "Futuro programado: ${formatoClp.format(montoFuturoRecurrente)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
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
                Text(
                    if (esEjecutivo) "Buscar cobro por concepto o cliente..."
                    else "Buscar por empresa o concepto..."
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.textoBusqueda.isNotBlank()) {
                    IconButton(onClick = { onBusquedaChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
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
                    text = if (esEjecutivo) {
                        "No hay cobros de tu empresa en esta categoría."
                    } else {
                        "No tienes compromisos en esta categoría para tus empresas aprobadas."
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
                    val clienteMostrar = uiState.asignacionesCobros[compromiso.id]?.usuarioNombre
                        ?: clientesAprobadosPorOrg[compromiso.organizacionId]
                    TarjetaCompromisoItem(
                        compromiso = compromiso,
                        clienteAsignado = clienteMostrar,
                        formatoClp = formatoClp,
                        esEjecutivo = esEjecutivo,
                        onAbrirDetalle = { onAbrirDetalle(compromiso) },
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
    esEjecutivo: Boolean,
    onAbrirDetalle: () -> Unit,
    onVerComprobante: (String) -> Unit,
    onResolverComprobante: (Boolean) -> Unit
) {
    val diasParaVencer = remember(compromiso.fechaVencimiento) {
        InicioViewModel.calcularDiasParaVencimiento(compromiso.fechaVencimiento)
    }
    val estaVencidoImpago = compromiso.estado != "PAGADO" && diasParaVencer != null && diasParaVencer < 0
    val estaPorVencer = (compromiso.estado == "PENDIENTE" || compromiso.estado == "RECHAZADO") &&
            diasParaVencer != null && diasParaVencer in 0..3
    val esCuotaFuturaRecurrente = (compromiso.estado == "PENDIENTE" || compromiso.estado == "RECHAZADO") &&
            !InicioViewModel.esCompromisoExigibleAhora(compromiso)

    val colorEstado = when {
        compromiso.estado == "PAGADO" -> Color(0xFF16A34A)
        estaVencidoImpago -> Color(0xFFDC2626)
        compromiso.estado == "RECHAZADO" -> Color(0xFFDC2626)
        compromiso.estado == "REVISION" -> Color(0xFF2563EB)
        else -> Color(0xFFD97706)
    }
    val tieneArchivoReal = !compromiso.comprobanteUri.isNullOrBlank() &&
            compromiso.comprobanteUri.startsWith("content://")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAbrirDetalle() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (estaVencidoImpago && compromiso.estado == "PENDIENTE") "VENCIDO" else compromiso.estado,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorEstado,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Ver detalle expandido",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vence: ${compromiso.fechaVencimiento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (estaVencidoImpago) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (estaVencidoImpago || estaPorVencer) FontWeight.SemiBold else FontWeight.Normal
                )

                if (estaVencidoImpago && diasParaVencer != null) {
                    Surface(
                        color = Color(0xFFDC2626).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Vencido (${abs(diasParaVencer)}d) • +1 Strike",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (estaPorVencer && diasParaVencer != null) {
                    val textoUrgencia = when (diasParaVencer) {
                        0 -> "Vence HOY"
                        1 -> "Vence MAÑANA"
                        else -> "Vence en $diasParaVencer días"
                    }
                    Surface(
                        color = Color(0xFFD97706).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = textoUrgencia,
                            color = Color(0xFFB45309),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (esCuotaFuturaRecurrente) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Cuota futura programada",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (compromiso.estado) {
                "PENDIENTE" -> {
                    if (!esEjecutivo) {
                        Button(
                            onClick = onAbrirDetalle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Pagar Compromiso", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (estaVencidoImpago) {
                                "Plazo expirado sin pago • Toca para ver detalle"
                            } else {
                                "Pendiente de pago del cliente • Toca para ver detalle"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (estaVencidoImpago) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
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

                    if (esEjecutivo) {
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
                                text = "Comprobante en revisión • Toca para ver detalle",
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
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "Comprobante rechazado: ${InicioViewModel.MOTIVO_RECHAZO_GENERAL}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (!esEjecutivo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onAbrirDetalle,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reintentar Pago", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "PAGADO" -> {
                    Text(
                        text = "Pago verificado y conciliado • Toca para ver detalle",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoDetalleCompromiso(
    compromiso: CompromisoEntity,
    clienteNombre: String?,
    esEjecutivo: Boolean,
    comprobanteBorradorUri: String?,
    onTomarFoto: () -> Unit,
    onAdjuntarDigital: () -> Unit,
    onVerComprobante: (String) -> Unit,
    onConfirmarPago: () -> Unit,
    onResolverComprobante: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val formatoClp = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val diasParaVencer = remember(compromiso.fechaVencimiento) {
        InicioViewModel.calcularDiasParaVencimiento(compromiso.fechaVencimiento)
    }
    val estaVencido = compromiso.estado != "PAGADO" && diasParaVencer != null && diasParaVencer < 0
    val puedePagar = !esEjecutivo && (compromiso.estado == "PENDIENTE" || compromiso.estado == "RECHAZADO")
    val tieneBorrador = !comprobanteBorradorUri.isNullOrBlank()
    val tieneAdjuntoGuardado = !compromiso.comprobanteUri.isNullOrBlank() &&
            compromiso.comprobanteUri.startsWith("content://")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Detalle del Compromiso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = compromiso.empresaNombre,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Monto a pagar", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatoClp.format(compromiso.monto),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Concepto: ${compromiso.concepto}", fontWeight = FontWeight.SemiBold)
                        if (!clienteNombre.isNullOrBlank()) {
                            Text("Titular / Cliente: $clienteNombre", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "Fecha de vencimiento: ${compromiso.fechaVencimiento}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (estaVencido) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Estado actual: ${if (estaVencido && compromiso.estado == "PENDIENTE") "VENCIDO" else compromiso.estado}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (compromiso.concepto.contains("Recurrente", ignoreCase = true)) {
                            Text(
                                text = "Modalidad: Cobro mensual recurrente (programado automáticamente)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (compromiso.estado == "RECHAZADO") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Motivo de rechazo anterior: ${InicioViewModel.MOTIVO_RECHAZO_GENERAL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (puedePagar) {
                    HorizontalDivider()
                    Text(
                        text = "Adjuntar Comprobante de Pago",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sube una foto o archivo PDF de tu transferencia o recibo para completar el pago:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onTomarFoto,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tomar Foto")
                        }
                        OutlinedButton(
                            onClick = onAdjuntarDigital,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Galería / PDF")
                        }
                    }

                    if (tieneBorrador) {
                        Surface(
                            color = Color(0xFF16A34A).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Comprobante adjuntado",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                                if (comprobanteBorradorUri!!.startsWith("content://")) {
                                    TextButton(
                                        onClick = { onVerComprobante(comprobanteBorradorUri) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Vista previa")
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onConfirmarPago,
                        enabled = tieneBorrador,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Confirmar Pago y Enviar", fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (tieneAdjuntoGuardado) {
                        OutlinedButton(
                            onClick = { onVerComprobante(compromiso.comprobanteUri!!) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Ver Comprobante Adjunto")
                        }
                    }

                    if (esEjecutivo && compromiso.estado == "REVISION") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onResolverComprobante(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aprobar Pago")
                            }
                            OutlinedButton(
                                onClick = { onResolverComprobante(false) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Rechazar", color = MaterialTheme.colorScheme.error)
                            }
                        }
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
private fun VistaEmpresasUsuario(
    uiState: InicioUiState,
    onAbrirRegistrarEmpresa: () -> Unit,
    onReintentarSolicitud: (Int) -> Unit
) {
    val userActual = uiState.usuarioActual ?: return
    val solicitudesUsuario = remember(uiState.solicitudesVinculacion, userActual) {
        uiState.solicitudesVinculacion.filter { it.usuarioId == userActual.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mis Empresas y Solicitudes (${solicitudesUsuario.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Registra una nueva empresa o solicita vinculación a entidades existentes en PagoFlex.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAbrirRegistrarEmpresa,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Registrar Nueva Empresa")
                }
            }
        }

        if (solicitudesUsuario.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Aún no tienes empresas asociadas. Registra una nueva empresa o presiona '+' para solicitar vinculación a una entidad existente.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(solicitudesUsuario, key = { "${it.usuarioId}_${it.organizacionId}" }) { solicitud ->
            val org = uiState.organizaciones.find { it.id == solicitud.organizacionId } ?: return@items
            val compromisosSinPagar = if (solicitud.estado == EstadoVinculo.APROBADA) {
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(org.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tipo: ${org.tipo} • RUT: ${org.rut}", style = MaterialTheme.typography.bodySmall)
                            Text(org.correoContacto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        val (badgeColor, badgeText) = when (solicitud.estado) {
                            EstadoVinculo.APROBADA -> Color(0xFF16A34A) to "APROBADA"
                            EstadoVinculo.PENDIENTE -> Color(0xFFD97706) to "EN REVISIÓN"
                            EstadoVinculo.RECHAZADA -> Color(0xFFDC2626) to "RECHAZADA"
                            EstadoVinculo.BLOQUEADA -> Color(0xFF991B1B) to "BLOQUEADA"
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    when (solicitud.estado) {
                        EstadoVinculo.APROBADA -> {
                            Text(
                                text = if (compromisosSinPagar > 0) {
                                    "Vinculación activa • $compromisosSinPagar compromiso(s) pendiente(s)"
                                } else {
                                    "Vinculación activa • Al día sin compromisos pendientes"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (compromisosSinPagar > 0) Color(0xFFD97706) else Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        EstadoVinculo.PENDIENTE -> {
                            Text(
                                text = "Solicitud enviada el ${solicitud.fechaSolicitud} • En espera de aprobación del Ejecutivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EstadoVinculo.RECHAZADA -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Motivo: ${solicitud.motivoRechazo ?: "No cumple requisitos internos."}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(onClick = { onReintentarSolicitud(org.id) }) {
                                    Text("Volver a solicitar")
                                }
                            }
                        }
                        EstadoVinculo.BLOQUEADA -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Acceso suspendido por esta entidad",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Motivo: ${solicitud.motivoRechazo ?: "Incumplimiento reiterado (Strikes)."}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaEmpresaEjecutivo(
    uiState: InicioUiState,
    miEmpresa: OrganizacionEntity?,
    motivoSuspensionEmpresa: String?,
    calcularStrikes: (Int) -> Int,
    onBusquedaChange: (String) -> Unit,
    onAbrirAgregarCliente: () -> Unit,
    onRevisarSolicitud: (SolicitudVinculacion) -> Unit
) {
    if (miEmpresa == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Text("Sin Empresa Vinculada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tu cuenta de Ejecutivo no tiene una entidad asociada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val formatoClp = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val queryNorm = normalizarTextoBusqueda(uiState.busquedaClientesEmpresa)

    val plantillasEmpresa = remember(uiState.plantillasRecurrentes, miEmpresa) {
        uiState.plantillasRecurrentes.filter { it.organizacionId == miEmpresa.id }
    }

    val solicitudesPendientes = remember(uiState.solicitudesVinculacion, uiState.clientesDisponibles, miEmpresa, queryNorm) {
        uiState.solicitudesVinculacion.filter { sol ->
            val esPendiente = sol.organizacionId == miEmpresa.id && sol.estado == EstadoVinculo.PENDIENTE
            if (!esPendiente) return@filter false
            if (queryNorm.isEmpty()) return@filter true
            val cliente = uiState.clientesDisponibles.find { it.id == sol.usuarioId }
            cliente != null && (
                    normalizarTextoBusqueda(cliente.nombre).contains(queryNorm) ||
                            normalizarTextoBusqueda(cliente.rut).contains(queryNorm) ||
                            normalizarTextoBusqueda(cliente.correo).contains(queryNorm)
                    )
        }
    }

    val clientesGestionados = remember(uiState.solicitudesVinculacion, uiState.clientesDisponibles, miEmpresa, queryNorm) {
        uiState.solicitudesVinculacion
            .filter { sol ->
                sol.organizacionId == miEmpresa.id &&
                        (sol.estado == EstadoVinculo.APROBADA || sol.estado == EstadoVinculo.BLOQUEADA)
            }
            .mapNotNull { sol ->
                val cliente = uiState.clientesDisponibles.find { it.id == sol.usuarioId } ?: return@mapNotNull null
                if (queryNorm.isNotEmpty() &&
                    !normalizarTextoBusqueda(cliente.nombre).contains(queryNorm) &&
                    !normalizarTextoBusqueda(cliente.rut).contains(queryNorm) &&
                    !normalizarTextoBusqueda(cliente.correo).contains(queryNorm)
                ) {
                    return@mapNotNull null
                }
                cliente to sol
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mi Entidad Emisora",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = miEmpresa.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RUT: ${miEmpresa.rut} • Tipo: ${miEmpresa.tipo}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = miEmpresa.correoContacto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (motivoSuspensionEmpresa != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "⚠️ ENTIDAD SUSPENDIDA: $motivoSuspensionEmpresa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onAbrirAgregarCliente,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Agregar Nuevo Cliente")
                        }
                    }
                }
            }
        }

        if (plantillasEmpresa.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Cobros Recurrentes Activos (${plantillasEmpresa.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Registrados una sola vez con programación mensual automática:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        plantillasEmpresa.forEach { p ->
                            Text(
                                text = "• ${p.concepto} → ${p.usuarioNombre} (${formatoClp.format(p.monto)}/mes • ${p.mesesProgramados} meses)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.busquedaClientesEmpresa,
                onValueChange = onBusquedaChange,
                placeholder = { Text("Buscar cliente por nombre, RUT o correo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.busquedaClientesEmpresa.isNotBlank()) {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Solicitudes de Integración Pendientes (${solicitudesPendientes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (solicitudesPendientes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (queryNorm.isEmpty()) {
                            "No tienes solicitudes de vinculación pendientes por revisar."
                        } else {
                            "No se encontraron solicitudes que coincidan con \"${uiState.busquedaClientesEmpresa.trim()}\"."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(solicitudesPendientes, key = { "pend_${it.usuarioId}_${it.organizacionId}" }) { solicitud ->
                val cliente = uiState.clientesDisponibles.find { it.id == solicitud.usuarioId }
                if (cliente != null) {
                    val strikes = calcularStrikes(cliente.id)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cliente.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "RUT: ${cliente.rut.ifBlank { "No registrado" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(cliente.correo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "Solicitado el ${solicitud.fechaSolicitud}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (strikes > 0) {
                                    Surface(
                                        color = Color(0xFFDC2626).copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "⚠️ $strikes Strike(s)",
                                            color = Color(0xFFDC2626),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { onRevisarSolicitud(solicitud) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Revisar Perfil Financiero y Resolver")
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Clientes de ${miEmpresa.nombre} (${clientesGestionados.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Toca un cliente para ver sus Strikes, historial o gestionar bloqueo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (clientesGestionados.isEmpty()) {
            item {
                Text(
                    text = if (queryNorm.isEmpty()) {
                        "Aún no hay clientes vinculados en ${miEmpresa.nombre}."
                    } else {
                        "Ningún cliente vinculado coincide con \"${uiState.busquedaClientesEmpresa.trim()}\"."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(clientesGestionados, key = { "gest_${it.first.id}" }) { (cliente, solicitud) ->
                val deudaEmpresa = uiState.compromisos.count { comp ->
                    comp.organizacionId == miEmpresa.id &&
                            comp.estado != "PAGADO" &&
                            (uiState.asignacionesCobros[comp.id] == null ||
                                    uiState.asignacionesCobros[comp.id]?.usuarioId == cliente.id)
                }
                val strikes = calcularStrikes(cliente.id)
                val estaBloqueado = solicitud.estado == EstadoVinculo.BLOQUEADA

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRevisarSolicitud(solicitud) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (estaBloqueado) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(cliente.nombre, fontWeight = FontWeight.Bold)
                                if (estaBloqueado) {
                                    Surface(
                                        color = Color(0xFF991B1B),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "BLOQUEADO",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text("RUT: ${cliente.rut.ifBlank { "-" }} • ${cliente.correo}", style = MaterialTheme.typography.bodySmall)
                            if (strikes > 0) {
                                Text(
                                    text = "⚠️ $strikes Strike(s) acumulado(s) por mora",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (deudaEmpresa > 0) "$deudaEmpresa pendiente(s)" else "Al día",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (deudaEmpresa > 0) Color(0xFFD97706) else Color(0xFF16A34A)
                            )
                            Text(
                                text = "Ver ficha ›",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
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
    strikesPropios: Int,
    onCambiarFotoPerfil: () -> Unit,
    onAbrirDatosPersonales: () -> Unit,
    onAbrirSeguridad: () -> Unit,
    onAbrirSoporte: () -> Unit,
    onAvisoColorFuturo: () -> Unit,
    onExportarCsv: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val user = uiState.usuarioActual
    val rol = user?.rol ?: "USUARIO"
    val esEjecutivo = rol == "EJECUTIVO"
    val esAdmin = rol == "ADMIN"
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
        when {
            user == null -> emptyList()
            esAdmin -> uiState.compromisos
            esEjecutivo -> uiState.compromisos.filter { it.organizacionId == user.empresaId }
            else -> {
                val ids = uiState.vinculosUsuarioOrgs[user.id].orEmpty()
                uiState.compromisos.filter { comp ->
                    val asignacion = uiState.asignacionesCobros[comp.id]
                    ids.contains(comp.organizacionId) && (asignacion == null || asignacion.usuarioId == user.id)
                }
            }
        }
    }

    val stat1Valor = if (esEjecutivo || esAdmin) {
        compromisosRelevantes.count { it.estado == "REVISION" }
    } else {
        compromisosRelevantes.count { it.estado == "PENDIENTE" || it.estado == "RECHAZADO" }
    }
    val stat1Label = if (esEjecutivo || esAdmin) "Por Validar" else "Pendientes"

    val stat2Valor = if (esEjecutivo || esAdmin) {
        compromisosRelevantes.count { it.estado == "PAGADO" }
    } else {
        compromisosRelevantes.count { it.estado == "REVISION" }
    }
    val stat2Label = if (esEjecutivo || esAdmin) "Conciliados" else "En Revisión"

    val stat3Valor = when {
        esAdmin -> uiState.organizaciones.size
        esEjecutivo -> uiState.solicitudesVinculacion.count { it.organizacionId == user?.empresaId && it.estado == EstadoVinculo.PENDIENTE }
        else -> strikesPropios
    }
    val stat3Label = when {
        esAdmin -> "Empresas"
        esEjecutivo -> "Solicitudes"
        else -> "Strikes"
    }

    val misTickets = remember(uiState.ticketsSoporte, user) {
        if (user == null) emptyList() else uiState.ticketsSoporte.filter { it.usuarioId == user.id }
    }
    val subtituloSoporte = when {
        misTickets.any { it.estado == "RESUELTO" } -> "${misTickets.size} consulta(s) • Ver respuestas"
        misTickets.isNotEmpty() -> "${misTickets.size} consulta(s) en revisión"
        user?.rol == "EJECUTIVO" -> "Reportar un problema a PagoFlex"
        else -> "Contactar a tu ejecutivo"
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

            if (!user?.rut.isNullOrBlank()) {
                Text(
                    text = "RUT: ${user?.rut}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = when (rol) {
                        "EJECUTIVO" -> "Cuenta Ejecutivo"
                        "ADMIN" -> "Administrador Plataforma"
                        else -> "Cliente"
                    },
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stat1Valor.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stat1Label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stat2Valor.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Text(
                        text = stat2Label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stat3Valor.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!esEjecutivo && !esAdmin && strikesPropios > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stat3Label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Cuenta y Configuración",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    FilaOpcionPerfil(
                        icono = Icons.Default.Person,
                        titulo = "Datos personales",
                        subtitulo = "Editar nombre visible",
                        onClick = onAbrirDatosPersonales
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    FilaOpcionPerfil(
                        icono = Icons.Default.Lock,
                        titulo = "Seguridad de la cuenta",
                        subtitulo = "Sesión verificada y cambio de contraseña",
                        onClick = onAbrirSeguridad
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    FilaOpcionPerfil(
                        icono = Icons.Default.Edit,
                        titulo = "Apariencia",
                        subtitulo = "Paleta de colores del tema (próximamente)",
                        onClick = onAvisoColorFuturo
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    FilaOpcionPerfil(
                        icono = Icons.Default.Share,
                        titulo = if (esAdmin) "Exportar consolidado plataforma" else "Exportar historial CSV",
                        subtitulo = "Guardar en Descargas y compartir",
                        onClick = onExportarCsv
                    )
                    if (!esAdmin) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        FilaOpcionPerfil(
                            icono = Icons.Default.Email,
                            titulo = "Centro de Soporte y Ayuda",
                            subtitulo = subtituloSoporte,
                            onClick = onAbrirSoporte
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FilaOpcionPerfil(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            shape = CircleShape,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
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

// =========================================================================
// DIÁLOGOS DE GESTIÓN, REGISTRO Y SOPORTE
// =========================================================================

@Composable
private fun DialogoRegistrarEmpresaUsuario(
    uiState: InicioUiState,
    onNombreChange: (String) -> Unit,
    onTipoChange: (String) -> Unit,
    onRutChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit
) {
    val tipos = listOf("Empresa", "Edificio", "Jardín", "Club")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Nueva Empresa",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Crea una nueva entidad en PagoFlex y vincúlala automáticamente a tu perfil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uiState.nuevaEmpresaNombreInput,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre de la empresa o entidad") },
                    isError = uiState.errorNuevaEmpresaNombre != null,
                    supportingText = {
                        uiState.errorNuevaEmpresaNombre?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo de entidad:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tipos.forEach { tipo ->
                        FilterChip(
                            selected = uiState.nuevaEmpresaTipoInput == tipo,
                            onClick = { onTipoChange(tipo) },
                            label = { Text(tipo) }
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.nuevaEmpresaRutInput,
                    onValueChange = onRutChange,
                    label = { Text("RUT Empresa (ej: 76.123.456-0)") },
                    isError = uiState.errorNuevaEmpresaRut != null,
                    supportingText = {
                        uiState.errorNuevaEmpresaRut?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.nuevaEmpresaCorreoInput,
                    onValueChange = onCorreoChange,
                    label = { Text("Correo de contacto institucional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = uiState.errorNuevaEmpresaCorreo != null,
                    supportingText = {
                        uiState.errorNuevaEmpresaCorreo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) {
                Text("Registrar Empresa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DialogoAgregarClienteEjecutivo(
    uiState: InicioUiState,
    onNombreChange: (String) -> Unit,
    onRutChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agregar Nuevo Cliente",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Si el cliente ya tiene cuenta en PagoFlex se vinculará de inmediato; si es nuevo, se creará su perfil de cliente y quedará asociado a tu empresa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uiState.nuevoClienteNombreInput,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre completo del cliente") },
                    isError = uiState.errorNuevoClienteNombre != null,
                    supportingText = {
                        uiState.errorNuevoClienteNombre?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.nuevoClienteRutInput,
                    onValueChange = onRutChange,
                    label = { Text("RUT (ej: 12.345.678-9)") },
                    isError = uiState.errorNuevoClienteRut != null,
                    supportingText = {
                        uiState.errorNuevoClienteRut?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.nuevoClienteCorreoInput,
                    onValueChange = onCorreoChange,
                    label = { Text("Correo electrónico del cliente") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = uiState.errorNuevoClienteCorreo != null,
                    supportingText = {
                        uiState.errorNuevoClienteCorreo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) {
                Text("Agregar y Vincular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DialogoSuspenderEmpresaAdmin(
    organizacion: OrganizacionEntity,
    motivoActual: String?,
    motivoInput: String,
    errorMotivo: String?,
    onMotivoChange: (String) -> Unit,
    onConfirmar: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val estaSuspendida = motivoActual != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (estaSuspendida) "Reactivar Empresa" else "Suspender Operación de Empresa",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${organizacion.nombre} (RUT: ${organizacion.rut})",
                    fontWeight = FontWeight.SemiBold
                )
                if (estaSuspendida) {
                    Text(
                        text = "Motivo actual de suspensión: $motivoActual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Al reactivar la empresa, sus ejecutivos volverán a emitir cobros y los clientes podrán solicitar vinculación.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Al suspender esta entidad, sus ejecutivos no podrán emitir nuevos cobros ni aceptar solicitudes hasta su reactivación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = motivoInput,
                        onValueChange = onMotivoChange,
                        label = { Text("Motivo de la suspensión") },
                        placeholder = { Text("Ej: Revisión documental o incumplimiento") },
                        isError = errorMotivo != null,
                        supportingText = {
                            errorMotivo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (estaSuspendida) {
                Button(
                    onClick = { onConfirmar(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Reactivar Empresa")
                }
            } else {
                Button(
                    onClick = { onConfirmar(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Suspender Empresa")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoSoporteUsuario(
    uiState: InicioUiState,
    onCategoriaChange: (String) -> Unit,
    onAsuntoChange: (String) -> Unit,
    onMensajeChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onDismiss: () -> Unit
) {
    val user = uiState.usuarioActual
    val categorias = remember(user?.rol) {
        if (user?.rol == "EJECUTIVO") {
            listOf(
                "Validación de comprobante",
                "Problema con cliente",
                "Problema técnico",
                "Consulta general"
            )
        } else {
            listOf(
                "Problema con pago",
                "Apelación de Strike",
                "Vinculación de empresa",
                "Problema técnico",
                "Consulta general"
            )
        }
    }
    val misTickets = remember(uiState.ticketsSoporte, user) {
        if (user == null) emptyList() else uiState.ticketsSoporte.filter { it.usuarioId == user.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Centro de Soporte PagoFlex", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (user?.rol == "EJECUTIVO")
                        "Envía un reporte a la administración de PagoFlex y revisa las respuestas a tus casos."
                    else
                        "Envía un mensaje al ejecutivo de tu empresa y revisa las respuestas a tus consultas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Categoría:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categorias.forEach { cat ->
                        FilterChip(
                            selected = uiState.categoriaSoporteInput == cat,
                            onClick = { onCategoriaChange(cat) },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.asuntoSoporteInput,
                    onValueChange = onAsuntoChange,
                    label = { Text("Asunto") },
                    placeholder = { Text("Ej: Error al adjuntar comprobante") },
                    isError = uiState.errorAsuntoSoporte != null,
                    supportingText = {
                        uiState.errorAsuntoSoporte?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.mensajeSoporteInput,
                    onValueChange = onMensajeChange,
                    label = { Text("Detalle de tu mensaje") },
                    placeholder = { Text("Explica brevemente lo ocurrido...") },
                    isError = uiState.errorMensajeSoporte != null,
                    supportingText = {
                        uiState.errorMensajeSoporte?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onEnviar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (user?.rol == "EJECUTIVO") "Enviar Reporte a PagoFlex"
                        else "Enviar Mensaje a mi Ejecutivo"
                    )
                }

                if (misTickets.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Mis Consultas Enviadas (${misTickets.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    misTickets.forEach { t ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${t.id} • ${t.asunto}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = t.estado,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (t.estado == "RESUELTO") Color(0xFF16A34A) else Color(0xFFD97706)
                                    )
                                }
                                Text(text = t.mensaje, style = MaterialTheme.typography.bodySmall)
                                if (!t.respuestaAdmin.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Admin: ${t.respuestaAdmin}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
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
private fun DialogoResponderTicketAdmin(
    ticket: TicketSoporte,
    respuestaInput: String,
    errorRespuesta: String?,
    onRespuestaChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Responder Ticket ${ticket.id}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${ticket.usuarioNombre} (${ticket.usuarioRol}) • ${ticket.categoria}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Consulta: ${ticket.mensaje}",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = respuestaInput,
                    onValueChange = onRespuestaChange,
                    label = { Text("Respuesta oficial") },
                    placeholder = { Text("Escribe la solución o indicación para el usuario...") },
                    isError = errorRespuesta != null,
                    supportingText = {
                        errorRespuesta?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("Enviar Respuesta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoSolicitarEmpresasCliente(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onSolicitarEmpresa: (Int) -> Unit
) {
    val userId = uiState.usuarioActual?.id ?: return
    val solicitudesUsuario = uiState.solicitudesVinculacion.filter { it.usuarioId == userId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Vinculación a Empresa") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Selecciona una empresa para enviar tu solicitud de integración. El ejecutivo revisará tu historial financiero antes de aprobar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                uiState.organizaciones.forEach { org ->
                    val solicitudExistente = solicitudesUsuario.find { it.organizacionId == org.id }
                    val suspendida = uiState.empresasSuspendidas.containsKey(org.id)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                            }
                            when {
                                suspendida -> Text(
                                    "Suspendida",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                solicitudExistente?.estado == EstadoVinculo.APROBADA -> Text(
                                    "Aprobada",
                                    color = Color(0xFF16A34A),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                solicitudExistente?.estado == EstadoVinculo.PENDIENTE -> Text(
                                    "En revisión",
                                    color = Color(0xFFD97706),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                solicitudExistente?.estado == EstadoVinculo.BLOQUEADA -> Text(
                                    "Bloqueada",
                                    color = Color(0xFF991B1B),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                else -> Button(onClick = { onSolicitarEmpresa(org.id) }) {
                                    Text("Solicitar")
                                }
                            }
                        }
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
private fun DialogoRevisionPerfilCliente(
    perfil: PerfilFinancieroCliente,
    motivoRechazo: String,
    errorMotivo: String?,
    onMotivoChange: (String) -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    onBloquearCliente: () -> Unit,
    onDesbloquearCliente: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatoClp = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val esPendiente = perfil.solicitud.estado == EstadoVinculo.PENDIENTE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (esPendiente) "Evaluación de Solicitud" else "Ficha Financiera del Cliente",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${perfil.cliente.nombre} • RUT: ${perfil.cliente.rut.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (perfil.strikesAcumulados > 0) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Strikes Acumulados (Mora): ${perfil.strikesAcumulados} / ${InicioViewModel.STRIKES_UMBRAL_APP}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (perfil.strikesAcumulados > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Riesgo: ${perfil.nivelRiesgo}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (perfil.strikesAcumulados > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (perfil.detalleStrikes.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            perfil.detalleStrikes.forEach { detalle ->
                                Text(
                                    text = "• $detalle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Resumen Financiero en Plataforma",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("• Cumplimiento histórico: ${perfil.porcentajeCumplimiento}%")
                        Text("• Compromisos pagados: ${perfil.pagados} (${formatoClp.format(perfil.montoTotalPagado)})")
                        Text("• Pendientes / En revisión: ${perfil.pendientes} / ${perfil.enRevision}")
                        if (perfil.compromisosPorVencerCount > 0) {
                            Text(
                                text = "• Próximos a vencer (≤ 3 días): ${perfil.compromisosPorVencerCount}",
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("• Comprobantes rechazados: ${perfil.rechazados}")
                        Text("• Deuda vigente total: ${formatoClp.format(perfil.montoDeudaActual)}")
                    }
                }

                if (perfil.bloqueadoEnEstaEmpresa) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Este cliente se encuentra bloqueado en tu empresa. Motivo: ${perfil.solicitud.motivoRechazo ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = motivoRechazo,
                        onValueChange = onMotivoChange,
                        label = {
                            Text(if (esPendiente) "Motivo de rechazo (si rechazas)" else "Motivo para bloquear cliente")
                        },
                        placeholder = {
                            Text(if (esPendiente) "Ej: No figura como residente activo" else "Ej: Acumulación de strikes por atraso")
                        },
                        isError = errorMotivo != null,
                        supportingText = {
                            errorMotivo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (esPendiente) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRechazar) {
                        Text("Rechazar", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onAprobar,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Aprobar")
                    }
                }
            } else {
                if (perfil.bloqueadoEnEstaEmpresa) {
                    Button(
                        onClick = onDesbloquearCliente,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Desbloquear Cliente")
                    }
                } else {
                    Button(
                        onClick = onBloquearCliente,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Bloquear Cliente")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DialogoGestionarUsuarioAdmin(
    usuario: UsuarioEntity,
    strikesCount: Int,
    detalleStrikes: List<String>,
    motivoBloqueoActual: String?,
    motivoBloqueoInput: String,
    errorMotivoBloqueo: String?,
    onMotivoBloqueoChange: (String) -> Unit,
    onToggleBloqueoApp: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val estaSuspendido = motivoBloqueoActual != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Gobernanza de Cuenta", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("${usuario.nombre} (${usuario.rol})", fontWeight = FontWeight.Bold)
                Text("RUT: ${usuario.rut.ifBlank { "-" }} • ${usuario.correo}", style = MaterialTheme.typography.bodySmall)

                if (usuario.rol == "USUARIO") {
                    Surface(
                        color = if (strikesCount >= InicioViewModel.STRIKES_UMBRAL_APP) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Historial de Strikes: $strikesCount / ${InicioViewModel.STRIKES_UMBRAL_APP}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (detalleStrikes.isEmpty()) {
                                Text("Sin strikes por atraso en la plataforma.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                detalleStrikes.forEach { d ->
                                    Text("• $d", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                if (estaSuspendido) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cuenta suspendida de PagoFlex. Motivo: $motivoBloqueoActual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = motivoBloqueoInput,
                        onValueChange = onMotivoBloqueoChange,
                        label = { Text("Motivo de suspensión global en App") },
                        placeholder = { Text("Ej: 3 Strikes acumulados por incumplimiento reiterado") },
                        isError = errorMotivoBloqueo != null,
                        supportingText = {
                            errorMotivoBloqueo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (estaSuspendido) {
                Button(
                    onClick = { onToggleBloqueoApp(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Reactivar Cuenta")
                }
            } else {
                Button(
                    onClick = { onToggleBloqueoApp(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Bloquear de la App")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DialogoFotoPerfil(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onSeleccionarFoto: () -> Unit,
    onQuitarFoto: () -> Unit
) {
    val avatarBitmap = rememberAvatarBitmap(uiState.avatarPath)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto de perfil") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(46.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = "Elige una imagen desde tu galería para personalizar tu cuenta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onSeleccionarFoto) {
                Text("Elegir de galería")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.avatarPath != null) {
                    TextButton(onClick = onQuitarFoto) {
                        Text("Quitar foto", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datos personales") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                OutlinedTextField(
                    value = uiState.usuarioActual?.rut ?: "",
                    onValueChange = {},
                    label = { Text("RUT asociado") },
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = when (uiState.usuarioActual?.rol) {
                        "EJECUTIVO" -> "Cuenta Ejecutivo"
                        "ADMIN" -> "Administrador Plataforma"
                        else -> "Cliente"
                    },
                    onValueChange = {},
                    label = { Text("Tipo de cuenta") },
                    enabled = false,
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
private fun DialogoSeguridadCuenta(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onClaveActualChange: (String) -> Unit,
    onNuevaClaveChange: (String) -> Unit,
    onConfirmarClaveChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    var verClaveActual by remember { mutableStateOf(false) }
    var verNuevaClave by remember { mutableStateOf(false) }
    var verConfirmarClave by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seguridad de la Cuenta", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Identidad y Sesión Verificada",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Correo titular: ${uiState.usuarioActual?.correo.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• El correo titular está protegido y no puede modificarse sin verificación externa.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• El cambio de contraseña exige validar tu clave actual.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Actualizar Contraseña",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.claveActualPerfil,
                    onValueChange = onClaveActualChange,
                    label = { Text("Contraseña actual") },
                    visualTransformation = if (verClaveActual) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { verClaveActual = !verClaveActual }) {
                            IconoOjitoContrasena(
                                visible = verClaveActual,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    isError = uiState.errorClaveActualPerfil != null,
                    supportingText = {
                        uiState.errorClaveActualPerfil?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.nuevaClavePerfil,
                    onValueChange = onNuevaClaveChange,
                    label = { Text("Nueva contraseña (mín. 6 car.)") },
                    visualTransformation = if (verNuevaClave) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { verNuevaClave = !verNuevaClave }) {
                            IconoOjitoContrasena(
                                visible = verNuevaClave,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    isError = uiState.errorClavePerfil != null,
                    supportingText = {
                        uiState.errorClavePerfil?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.confirmarClavePerfil,
                    onValueChange = onConfirmarClaveChange,
                    label = { Text("Confirmar nueva contraseña") },
                    visualTransformation = if (verConfirmarClave) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { verConfirmarClave = !verConfirmarClave }) {
                            IconoOjitoContrasena(
                                visible = verConfirmarClave,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    isError = uiState.errorConfirmarClavePerfil != null,
                    supportingText = {
                        uiState.errorConfirmarClavePerfil?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) { Text("Actualizar Contraseña") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DialogoCompromiso(
    uiState: InicioUiState,
    onDismiss: () -> Unit,
    onSelectCliente: (UsuarioEntity) -> Unit,
    onConceptoChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFechaChange: (String) -> Unit,
    onToggleRecurrente: (Boolean) -> Unit,
    onMesesRecurrenciaChange: (Int) -> Unit,
    onGuardar: () -> Unit
) {
    var expandedClientes by remember { mutableStateOf(false) }

    val clientesActivos = remember(uiState.clientesDisponibles, uiState.vinculosUsuarioOrgs, uiState.bloqueosGlobalesApp, uiState.orgSeleccionadaId) {
        uiState.clientesDisponibles.filter { cliente ->
            uiState.vinculosUsuarioOrgs[cliente.id]?.contains(uiState.orgSeleccionadaId) == true &&
                    !uiState.bloqueosGlobalesApp.containsKey(cliente.id)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Registrar Cobro a Cliente")
                Text(
                    text = "Entidad emisora: ${uiState.orgSeleccionadaNombre}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (clientesActivos.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No tienes clientes activos en ${uiState.orgSeleccionadaNombre}. Agrega un cliente en 'Mi Empresa' para emitir cobros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Box {
                        OutlinedTextField(
                            value = uiState.clienteSeleccionadoNombre,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cliente destinatario") },
                            isError = uiState.errorClienteCompromiso != null,
                            trailingIcon = {
                                IconButton(onClick = { expandedClientes = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedClientes = true }
                        )
                        DropdownMenu(
                            expanded = expandedClientes,
                            onDismissRequest = { expandedClientes = false }
                        ) {
                            clientesActivos.forEach { cliente ->
                                DropdownMenuItem(
                                    text = { Text("${cliente.nombre} (${cliente.rut.ifBlank { cliente.correo }})") },
                                    onClick = {
                                        onSelectCliente(cliente)
                                        expandedClientes = false
                                    }
                                )
                            }
                        }
                    }
                    uiState.errorClienteCompromiso?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = uiState.conceptoCompromiso,
                    onValueChange = onConceptoChange,
                    label = { Text("Concepto (ej: Gasto Común, Mensualidad)") },
                    isError = uiState.errorConceptoCompromiso != null,
                    supportingText = {
                        uiState.errorConceptoCompromiso?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.montoCompromiso,
                    onValueChange = onMontoChange,
                    label = { Text("Monto mensual CLP (ej: 85000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.errorMontoCompromiso != null,
                    supportingText = {
                        uiState.errorMontoCompromiso?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.fechaCompromiso,
                    onValueChange = onFechaChange,
                    label = { Text("Primer vencimiento (DD/MM/AAAA)") },
                    isError = uiState.errorFechaCompromiso != null,
                    supportingText = {
                        uiState.errorFechaCompromiso?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Modalidad de registro:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.esCobroRecurrenteMensual,
                        onClick = { onToggleRecurrente(true) },
                        label = { Text("Mensual (1 registro)") }
                    )
                    FilterChip(
                        selected = !uiState.esCobroRecurrenteMensual,
                        onClick = { onToggleRecurrente(false) },
                        label = { Text("Pago único") }
                    )
                }

                if (uiState.esCobroRecurrenteMensual) {
                    Text(
                        text = "Se registra una sola vez y programa automáticamente los cobros mensuales sin tener que re-emitirlos cada fin de mes:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 6, 12).forEach { meses ->
                            FilterChip(
                                selected = uiState.mesesRecurrencia == meses,
                                onClick = { onMesesRecurrenciaChange(meses) },
                                label = { Text("$meses meses") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onGuardar, enabled = clientesActivos.isNotEmpty()) {
                Text(if (uiState.esCobroRecurrenteMensual) "Registrar Plan" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}