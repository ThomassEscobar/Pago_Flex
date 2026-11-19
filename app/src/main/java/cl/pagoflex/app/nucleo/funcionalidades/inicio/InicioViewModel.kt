package cl.pagoflex.app.nucleo.funcionalidades.inicio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Patterns
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import cl.pagoflex.app.nucleo.local.repositorio.PagoFlexRepository
import cl.pagoflex.app.nucleo.util.NotificadorPagoFlex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class AsignacionCobro(
    val usuarioId: Int,
    val usuarioNombre: String
)

data class InicioUiState(
    val usuarioActual: UsuarioEntity? = null,
    val compromisos: List<CompromisoEntity> = emptyList(),
    val organizaciones: List<OrganizacionEntity> = emptyList(),
    val clientesDisponibles: List<UsuarioEntity> = emptyList(),
    val vinculosUsuarioOrgs: Map<Int, Set<Int>> = emptyMap(),
    val asignacionesCobros: Map<Int, AsignacionCobro> = emptyMap(),
    val filtroEstado: String = "TODOS",
    val textoBusqueda: String = "",

    // Datos extra de perfil guardados por usuario
    val avatarPath: String? = null,
    val correoRespaldo: String = "",

    // Formulario Compromiso (Crear / Editar)
    val mostrarDialogoCompromiso: Boolean = false,
    val compromisoEditandoId: Int = 0,
    val orgSeleccionadaId: Int = 0,
    val orgSeleccionadaNombre: String = "",
    val errorOrgCompromiso: String? = null,
    val clienteSeleccionadoId: Int = 0,
    val clienteSeleccionadoNombre: String = "",
    val errorClienteCompromiso: String? = null,
    val conceptoCompromiso: String = "",
    val errorConceptoCompromiso: String? = null,
    val montoCompromiso: String = "",
    val errorMontoCompromiso: String? = null,
    val fechaCompromiso: String = "",
    val errorFechaCompromiso: String? = null,

    // Formulario Organización (Crear / Editar)
    val mostrarDialogoOrg: Boolean = false,
    val orgEditandoId: Int = 0,
    val nombreOrg: String = "",
    val errorNombreOrg: String? = null,
    val tipoOrg: String = "Empresa",
    val rutOrg: String = "",
    val errorRutOrg: String? = null,
    val correoOrg: String = "",
    val errorCorreoOrg: String? = null,

    // Diálogos de Vínculos
    val mostrarDialogoVincularEmpresaUsuario: Boolean = false,
    val orgGestionClientes: OrganizacionEntity? = null,

    // Modal 1: Foto de Perfil
    val mostrarDialogoFotoPerfil: Boolean = false,

    // Modal 2: Datos Personales
    val mostrarDialogoDatosPersonales: Boolean = false,
    val nombrePerfilInput: String = "",
    val errorNombrePerfilInput: String? = null,

    // Modal 3: Seguridad, Correos y Contraseña
    val mostrarDialogoSeguridad: Boolean = false,
    val correoPrincipalInput: String = "",
    val errorCorreoPrincipalInput: String? = null,
    val correoRespaldoInput: String = "",
    val errorCorreoRespaldoInput: String? = null,
    val expandirCambioClavePerfil: Boolean = false,
    val claveActualPerfil: String = "",
    val errorClaveActualPerfil: String? = null,
    val nuevaClavePerfil: String = "",
    val errorClavePerfil: String? = null,
    val confirmarClavePerfil: String = "",
    val errorConfirmarClavePerfil: String? = null,

    val mensajeSnackbar: String? = null,
    val ultimoInformeUri: Uri? = null
)

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val repository: PagoFlexRepository
) : ViewModel() {

    companion object {
        private var seedDemoEjecutado = false
        private const val PREFS_DEMO = "pagoflex_demo_prefs"
        private const val KEY_GALERIA_CREADA = "galeria_comprobantes_creados_v1"
        private const val PREFS_ASIGNACIONES = "pagoflex_asignaciones_cobros"
        private const val PREFS_VINCULOS = "pagoflex_vinculos_usuario_org"
        private const val PREFS_PERFIL_EXTRA = "pagoflex_perfil_extra_v1"
        const val MOTIVO_RECHAZO_GENERAL = "El comprobante no ha podido ser corroborado."
    }

    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observarOrganizaciones().collect { lista ->
                _uiState.update { state ->
                    val primera = lista.firstOrNull()
                    state.copy(
                        organizaciones = lista,
                        orgSeleccionadaId = if (state.orgSeleccionadaId == 0 && primera != null) primera.id else state.orgSeleccionadaId,
                        orgSeleccionadaNombre = if (state.orgSeleccionadaNombre.isBlank() && primera != null) primera.nombre else state.orgSeleccionadaNombre
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observarCompromisos().collect { lista ->
                _uiState.update { it.copy(compromisos = lista) }
            }
        }
    }

    private suspend fun obtenerClientesEnRoom(): List<UsuarioEntity> {
        val encontrados = mutableListOf<UsuarioEntity>()
        for (id in 1..50) {
            val u = repository.observarUsuario(id).firstOrNull()
            if (u != null && u.rol == "USUARIO") {
                encontrados.add(u)
            }
        }
        return encontrados
    }

    fun inicializarEntorno(context: Context) {
        cargarAsignacionesCobros(context)
        cargarDatosExtraPerfil(context)
        viewModelScope.launch {
            val actuales = repository.observarCompromisos().first()
            if (!seedDemoEjecutado && actuales.size <= 2) {
                seedDemoEjecutado = true
                poblarDatosCompletosDemo()
            }

            val orgs = repository.observarOrganizaciones().first()
            val clientes = obtenerClientesEnRoom()
            _uiState.update { it.copy(clientesDisponibles = clientes) }

            sincronizarVinculosPorDefecto(context, clientes, orgs)
            asegurarCompromisoRechazadoParaClientes(context, clientes, orgs)
            asignarImagenesACompromisosEnRevision(context)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_DEMO, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_GALERIA_CREADA, false)) return@launch

            val creadas = crearImagenesComprobantesEnGaleria(context)
            if (creadas) {
                prefs.edit().putBoolean(KEY_GALERIA_CREADA, true).apply()
            }
        }
    }

    private suspend fun asegurarCompromisoRechazadoParaClientes(
        context: Context,
        clientes: List<UsuarioEntity>,
        orgs: List<OrganizacionEntity>
    ) {
        if (orgs.isEmpty() || clientes.isEmpty()) return
        val compromisosActuales = repository.observarCompromisos().first()
        val vinculos = _uiState.value.vinculosUsuarioOrgs
        val asignaciones = _uiState.value.asignacionesCobros

        clientes.forEach { cliente ->
            val orgsCliente = vinculos[cliente.id].orEmpty()
            val yaTieneRechazado = compromisosActuales.any { comp ->
                comp.estado == "RECHAZADO" &&
                        orgsCliente.contains(comp.organizacionId) &&
                        (asignaciones[comp.id] == null || asignaciones[comp.id]?.usuarioId == cliente.id)
            }

            if (!yaTieneRechazado) {
                val orgDestino = orgs.find { orgsCliente.contains(it.id) } ?: orgs.first()
                val conceptoEjemplo = "Reserva Sala Multiuso"
                val nuevoRechazado = CompromisoEntity(
                    organizacionId = orgDestino.id,
                    empresaNombre = orgDestino.nombre,
                    concepto = conceptoEjemplo,
                    monto = 22000.0,
                    fechaVencimiento = "12/11/2026",
                    estado = "RECHAZADO"
                )
                repository.guardarCompromiso(nuevoRechazado)

                val recienCreado = repository.observarCompromisos().first()
                    .filter {
                        it.organizacionId == orgDestino.id &&
                                it.concepto == conceptoEjemplo &&
                                it.estado == "RECHAZADO"
                    }
                    .maxByOrNull { it.id }

                if (recienCreado != null) {
                    guardarAsignacionCobro(
                        context = context,
                        compromisoId = recienCreado.id,
                        usuarioId = cliente.id,
                        usuarioNombre = cliente.nombre
                    )
                }
            }
        }
    }

    fun cargarDatosExtraPerfil(context: Context) {
        val userId = _uiState.value.usuarioActual?.id ?: return
        val prefs = context.getSharedPreferences(PREFS_PERFIL_EXTRA, Context.MODE_PRIVATE)
        val pathGuardado = prefs.getString("avatar_$userId", null)
        val pathValido = if (!pathGuardado.isNullOrBlank() && File(pathGuardado).exists()) pathGuardado else null

        _uiState.update {
            it.copy(
                avatarPath = pathValido,
                correoRespaldo = prefs.getString("correo_resp_$userId", "") ?: ""
            )
        }
    }

    fun guardarFotoPerfilDesdeUri(context: Context, uri: Uri) {
        val userId = _uiState.value.usuarioActual?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bitmapOriginal = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (bitmapOriginal == null) return@launch

                val maxLado = 512
                val escala = minOf(
                    maxLado.toFloat() / bitmapOriginal.width,
                    maxLado.toFloat() / bitmapOriginal.height,
                    1f
                )
                val anchoFinal = (bitmapOriginal.width * escala).toInt().coerceAtLeast(1)
                val altoFinal = (bitmapOriginal.height * escala).toInt().coerceAtLeast(1)
                val bitmapEscalado = Bitmap.createScaledBitmap(bitmapOriginal, anchoFinal, altoFinal, true)
                if (bitmapEscalado != bitmapOriginal) {
                    bitmapOriginal.recycle()
                }

                val archivoDestino = File(context.filesDir, "avatar_user_${userId}_${System.currentTimeMillis()}.jpg")
                context.filesDir
                    .listFiles { f -> f.name.startsWith("avatar_user_${userId}_") }
                    ?.forEach { it.delete() }

                FileOutputStream(archivoDestino).use { out ->
                    bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmapEscalado.recycle()

                val prefs = context.getSharedPreferences(PREFS_PERFIL_EXTRA, Context.MODE_PRIVATE)
                prefs.edit().putString("avatar_$userId", archivoDestino.absolutePath).apply()

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            avatarPath = archivoDestino.absolutePath,
                            mostrarDialogoFotoPerfil = false,
                            mensajeSnackbar = "Foto de perfil actualizada"
                        )
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(mensajeSnackbar = "No se pudo procesar la imagen seleccionada") }
                }
            }
        }
    }

    fun eliminarFotoPerfil(context: Context) {
        val userId = _uiState.value.usuarioActual?.id ?: return
        val prefs = context.getSharedPreferences(PREFS_PERFIL_EXTRA, Context.MODE_PRIVATE)
        val pathActual = prefs.getString("avatar_$userId", null)
        if (!pathActual.isNullOrBlank()) {
            File(pathActual).delete()
        }
        prefs.edit().remove("avatar_$userId").apply()
        _uiState.update {
            it.copy(
                avatarPath = null,
                mostrarDialogoFotoPerfil = false,
                mensajeSnackbar = "Foto de perfil eliminada"
            )
        }
    }

    private fun sincronizarVinculosPorDefecto(
        context: Context,
        clientes: List<UsuarioEntity>,
        orgs: List<OrganizacionEntity>
    ) {
        if (orgs.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_VINCULOS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var huboCambios = false

        val orgMirador = orgs.find { it.nombre.contains("Mirador", ignoreCase = true) } ?: orgs.first()
        val orgJardin = orgs.find { it.nombre.contains("Peques", ignoreCase = true) }
        val orgClub = orgs.find { it.nombre.contains("Club", ignoreCase = true) }
        val orgFibra = orgs.find { it.nombre.contains("FibraNet", ignoreCase = true) }

        clientes.forEach { cliente ->
            val keyInit = "init_user_${cliente.id}"
            if (!prefs.getBoolean(keyInit, false)) {
                val orgsIniciales = if (cliente.nombre.contains("Camila", ignoreCase = true)) {
                    listOfNotNull(orgMirador.id, orgJardin?.id, orgClub?.id)
                } else {
                    listOfNotNull(orgMirador.id, orgFibra?.id ?: orgClub?.id)
                }.map { it.toString() }.toSet()

                editor.putStringSet("orgs_user_${cliente.id}", orgsIniciales)
                editor.putBoolean(keyInit, true)
                huboCambios = true
            }
        }

        if (huboCambios) {
            editor.apply()
        }
        cargarVinculosDesdePrefs(context)
    }

    private fun cargarVinculosDesdePrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_VINCULOS, Context.MODE_PRIVATE)
        val mapa = mutableMapOf<Int, Set<Int>>()
        prefs.all.forEach { (clave, valor) ->
            if (clave.startsWith("orgs_user_") && valor is Set<*>) {
                val userId = clave.removePrefix("orgs_user_").toIntOrNull()
                if (userId != null) {
                    val ids = valor.mapNotNull { it?.toString()?.toIntOrNull() }.toSet()
                    mapa[userId] = ids
                }
            }
        }
        _uiState.update { it.copy(vinculosUsuarioOrgs = mapa) }
    }

    fun contarCompromisosSinPagar(userId: Int, orgId: Int): Int {
        val st = _uiState.value
        return st.compromisos.count { comp ->
            comp.organizacionId == orgId &&
                    comp.estado != "PAGADO" &&
                    (st.asignacionesCobros[comp.id] == null || st.asignacionesCobros[comp.id]?.usuarioId == userId)
        }
    }

    fun alternarVinculoUsuarioEmpresa(context: Context, userId: Int, orgId: Int) {
        val prefs = context.getSharedPreferences(PREFS_VINCULOS, Context.MODE_PRIVATE)
        val actuales = prefs.getStringSet("orgs_user_$userId", emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toMutableSet() ?: mutableSetOf()

        if (actuales.contains(orgId)) {
            val sinPagar = contarCompromisosSinPagar(userId, orgId)
            if (sinPagar > 0) {
                _uiState.update {
                    it.copy(
                        mensajeSnackbar = "No puedes desvincular esta empresa: tienes $sinPagar compromiso(s) sin pagar."
                    )
                }
                return
            }
            actuales.remove(orgId)
            prefs.edit()
                .putStringSet("orgs_user_$userId", actuales.map { it.toString() }.toSet())
                .putBoolean("init_user_$userId", true)
                .apply()
            cargarVinculosDesdePrefs(context)
            _uiState.update { it.copy(mensajeSnackbar = "Empresa desvinculada") }
        } else {
            actuales.add(orgId)
            prefs.edit()
                .putStringSet("orgs_user_$userId", actuales.map { it.toString() }.toSet())
                .putBoolean("init_user_$userId", true)
                .apply()
            cargarVinculosDesdePrefs(context)
            _uiState.update { it.copy(mensajeSnackbar = "Vinculación activada") }
        }
    }

    private fun cargarAsignacionesCobros(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_ASIGNACIONES, Context.MODE_PRIVATE)
        val mapa = mutableMapOf<Int, AsignacionCobro>()
        prefs.all.forEach { (clave, valor) ->
            if (clave.startsWith("uid_") && valor is Int && valor > 0) {
                val compId = clave.removePrefix("uid_").toIntOrNull()
                if (compId != null) {
                    val nombre = prefs.getString("uname_$compId", "Cliente asignado") ?: "Cliente asignado"
                    mapa[compId] = AsignacionCobro(usuarioId = valor, usuarioNombre = nombre)
                }
            }
        }
        _uiState.update { it.copy(asignacionesCobros = mapa) }
    }

    private fun guardarAsignacionCobro(context: Context, compromisoId: Int, usuarioId: Int, usuarioNombre: String) {
        val prefs = context.getSharedPreferences(PREFS_ASIGNACIONES, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("uid_$compromisoId", usuarioId)
            .putString("uname_$compromisoId", usuarioNombre)
            .apply()
        cargarAsignacionesCobros(context)
    }

    private suspend fun asignarImagenesACompromisosEnRevision(context: Context) {
        val formatoClp = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
        val listaActual = repository.observarCompromisos().first()

        listaActual.forEach { comp ->
            val sinArchivoValido = comp.comprobanteUri.isNullOrBlank() ||
                    !comp.comprobanteUri.startsWith("content://")

            if (comp.estado == "REVISION" && sinArchivoValido) {
                val uriGenerada = withContext(Dispatchers.IO) {
                    crearVoucherLocalEnCache(
                        context = context,
                        nombreArchivo = "voucher_revision_${comp.id}.jpg",
                        titulo = "COMPROBANTE EN REVISIÓN",
                        empresa = comp.empresaNombre,
                        concepto = comp.concepto,
                        montoFormateado = formatoClp.format(comp.monto),
                        operacion = "PFX-${1000 + comp.id}"
                    )
                }
                if (uriGenerada != null) {
                    repository.guardarCompromiso(comp.copy(comprobanteUri = uriGenerada))
                }
            }
        }
    }

    private fun crearVoucherLocalEnCache(
        context: Context,
        nombreArchivo: String,
        titulo: String,
        empresa: String,
        concepto: String,
        montoFormateado: String,
        operacion: String
    ): String? {
        return try {
            val archivo = File(context.cacheDir, nombreArchivo)
            val bitmap = dibujarBitmapComprobante(
                titulo = titulo,
                detalle = "Monto: $montoFormateado CLP • Op: $operacion",
                empresa = empresa,
                concepto = concepto
            )
            FileOutputStream(archivo).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            ).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun crearImagenesComprobantesEnGaleria(context: Context): Boolean {
        return try {
            val muestras = listOf(
                Triple("Comprobante_Transferencia_01.jpg", "TRANSFERENCIA ELECTRÓNICA", "Monto: $88.500 CLP • Op: 948271"),
                Triple("Comprobante_Pago_Webpay_02.jpg", "COMPROBANTE DE PAGO DIGITAL", "Monto: $45.000 CLP • Op: 773104")
            )
            var alMenosUna = false
            muestras.forEach { (nombreArchivo, titulo, detalle) ->
                val bitmap = dibujarBitmapComprobante(titulo, detalle)
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PagoFlex")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                    alMenosUna = true
                }
                bitmap.recycle()
            }
            alMenosUna
        } catch (_: Exception) {
            false
        }
    }

    private fun dibujarBitmapComprobante(
        titulo: String,
        detalle: String,
        empresa: String = "Recaudación PagoFlex SpA",
        concepto: String = "Pago de Compromiso"
    ): Bitmap {
        val ancho = 900
        val alto = 1200
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paintHeader = Paint().apply {
            color = Color.rgb(30, 64, 175)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, ancho.toFloat(), 220f, paintHeader)

        val paintTituloHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PagoFlex • Respaldo Oficial", 60f, 130f, paintTituloHeader)

        val paintBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(50f, 270f, ancho - 50f, alto - 120f, paintBorde)

        val paintSubtitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 32f
        }
        val paintVerde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 163, 74)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText(titulo, 90f, 360f, paintSubtitulo)
        canvas.drawText("Estado: TRANSACCIÓN EXITOSA", 90f, 440f, paintVerde)
        canvas.drawText(detalle, 90f, 520f, paintTexto)
        canvas.drawText("Entidad: $empresa", 90f, 600f, paintTexto)
        canvas.drawText("Concepto: $concepto", 90f, 680f, paintTexto)
        canvas.drawText("Banco Origen: Banco Estado Chile", 90f, 760f, paintTexto)
        canvas.drawText("Código Verificación: PFX-2026-88412-CL", 90f, 840f, paintTexto)

        return bitmap
    }

    private suspend fun poblarDatosCompletosDemo() {
        val orgsActuales = repository.observarOrganizaciones().first()

        if (orgsActuales.none { it.nombre.contains("Club", ignoreCase = true) }) {
            repository.guardarOrganizacion(
                OrganizacionEntity(
                    nombre = "Club Deportivo Oriente",
                    tipo = "Club",
                    rut = "65.432.198-K",
                    correoContacto = "pagos@cluboriente.cl"
                )
            )
        }
        if (orgsActuales.none { it.nombre.contains("FibraNet", ignoreCase = true) }) {
            repository.guardarOrganizacion(
                OrganizacionEntity(
                    nombre = "FibraNet Chile",
                    tipo = "Empresa",
                    rut = "76.890.123-4",
                    correoContacto = "cobranza@fibranet.cl"
                )
            )
        }

        val orgs = repository.observarOrganizaciones().first()
        val orgEdificio = orgs.find { it.nombre.contains("Mirador", ignoreCase = true) } ?: orgs.firstOrNull() ?: return
        val orgJardin = orgs.find { it.nombre.contains("Peques", ignoreCase = true) } ?: orgEdificio
        val orgClub = orgs.find { it.nombre.contains("Club", ignoreCase = true) } ?: orgEdificio
        val orgFibra = orgs.find { it.nombre.contains("FibraNet", ignoreCase = true) } ?: orgEdificio

        val compromisosDemo = listOf(
            CompromisoEntity(
                organizacionId = orgEdificio.id,
                empresaNombre = orgEdificio.nombre,
                concepto = "Gasto Común Diciembre",
                monto = 88500.0,
                fechaVencimiento = "30/12/2026",
                estado = "PENDIENTE"
            ),
            CompromisoEntity(
                organizacionId = orgClub.id,
                empresaNombre = orgClub.nombre,
                concepto = "Mensualidad Taller Natación",
                monto = 45000.0,
                fechaVencimiento = "10/12/2026",
                estado = "PENDIENTE"
            ),
            CompromisoEntity(
                organizacionId = orgFibra.id,
                empresaNombre = orgFibra.nombre,
                concepto = "Plan Internet Fibra 600MB",
                monto = 29990.0,
                fechaVencimiento = "15/12/2026",
                estado = "PENDIENTE"
            ),
            CompromisoEntity(
                organizacionId = orgJardin.id,
                empresaNombre = orgJardin.nombre,
                concepto = "Materiales y Actividades",
                monto = 35000.0,
                fechaVencimiento = "25/11/2026",
                estado = "RECHAZADO"
            ),
            CompromisoEntity(
                organizacionId = orgJardin.id,
                empresaNombre = orgJardin.nombre,
                concepto = "Matrícula Anual 2026",
                monto = 240000.0,
                fechaVencimiento = "05/03/2026",
                estado = "PAGADO"
            ),
            CompromisoEntity(
                organizacionId = orgEdificio.id,
                empresaNombre = orgEdificio.nombre,
                concepto = "Gasto Común Octubre",
                monto = 82000.0,
                fechaVencimiento = "31/10/2026",
                estado = "PAGADO"
            )
        )

        compromisosDemo.forEach { repository.guardarCompromiso(it) }
    }

    fun cargarUsuario(usuarioId: Int) {
        viewModelScope.launch {
            repository.observarUsuario(usuarioId).collect { user ->
                if (user != null) {
                    _uiState.update { state ->
                        val cambioUsuario = state.usuarioActual?.id != user.id
                        state.copy(
                            usuarioActual = user,
                            filtroEstado = if (cambioUsuario) {
                                if (user.rol == "EJECUTIVO") "REVISION" else "TODOS"
                            } else {
                                state.filtroEstado
                            }
                        )
                    }
                }
            }
        }
    }

    fun onFiltroEstadoChange(estado: String) {
        _uiState.update { it.copy(filtroEstado = estado) }
    }

    fun onBusquedaChange(texto: String) {
        _uiState.update { it.copy(textoBusqueda = texto) }
    }

    fun limpiarSnackbar() {
        _uiState.update { it.copy(mensajeSnackbar = null, ultimoInformeUri = null) }
    }

    fun mostrarMensaje(mensaje: String) {
        _uiState.update { it.copy(mensajeSnackbar = mensaje, ultimoInformeUri = null) }
    }

    // --- GESTIÓN DIÁLOGOS VÍNCULOS ---
    fun abrirDialogoVincularEmpresaUsuario() {
        _uiState.update { it.copy(mostrarDialogoVincularEmpresaUsuario = true) }
    }

    fun cerrarDialogoVincularEmpresaUsuario() {
        _uiState.update { it.copy(mostrarDialogoVincularEmpresaUsuario = false) }
    }

    fun abrirDialogoClientesDeOrganizacion(org: OrganizacionEntity) {
        _uiState.update { it.copy(orgGestionClientes = org) }
    }

    fun cerrarDialogoClientesDeOrganizacion() {
        _uiState.update { it.copy(orgGestionClientes = null) }
    }

    // --- CRUD COMPROMISOS ---
    fun abrirDialogoNuevoCompromiso() {
        val st = _uiState.value
        val primeraOrg = st.organizaciones.firstOrNull()
        val orgId = primeraOrg?.id ?: 0
        val clientesDeEstaOrg = st.clientesDisponibles.filter { cliente ->
            st.vinculosUsuarioOrgs[cliente.id]?.contains(orgId) == true
        }
        val primerCliente = clientesDeEstaOrg.firstOrNull()

        _uiState.update {
            it.copy(
                mostrarDialogoCompromiso = true,
                compromisoEditandoId = 0,
                orgSeleccionadaId = orgId,
                orgSeleccionadaNombre = primeraOrg?.nombre ?: "",
                errorOrgCompromiso = null,
                clienteSeleccionadoId = primerCliente?.id ?: 0,
                clienteSeleccionadoNombre = primerCliente?.nombre ?: "",
                errorClienteCompromiso = null,
                conceptoCompromiso = "",
                errorConceptoCompromiso = null,
                montoCompromiso = "",
                errorMontoCompromiso = null,
                fechaCompromiso = "30/11/2026",
                errorFechaCompromiso = null
            )
        }
    }

    fun abrirDialogoEditarCompromiso(comp: CompromisoEntity) {
        val st = _uiState.value
        val asignacionActual = st.asignacionesCobros[comp.id]
        val clientesDeEstaOrg = st.clientesDisponibles.filter { cliente ->
            st.vinculosUsuarioOrgs[cliente.id]?.contains(comp.organizacionId) == true
        }
        val clienteFallback = clientesDeEstaOrg.firstOrNull()

        _uiState.update {
            it.copy(
                mostrarDialogoCompromiso = true,
                compromisoEditandoId = comp.id,
                orgSeleccionadaId = comp.organizacionId,
                orgSeleccionadaNombre = comp.empresaNombre,
                errorOrgCompromiso = null,
                clienteSeleccionadoId = asignacionActual?.usuarioId ?: clienteFallback?.id ?: 0,
                clienteSeleccionadoNombre = asignacionActual?.usuarioNombre ?: clienteFallback?.nombre ?: "",
                errorClienteCompromiso = null,
                conceptoCompromiso = comp.concepto,
                errorConceptoCompromiso = null,
                montoCompromiso = comp.monto.toInt().toString(),
                errorMontoCompromiso = null,
                fechaCompromiso = comp.fechaVencimiento,
                errorFechaCompromiso = null
            )
        }
    }

    fun cerrarDialogoCompromiso() {
        _uiState.update { it.copy(mostrarDialogoCompromiso = false) }
    }

    fun onSeleccionarOrgCompromiso(org: OrganizacionEntity) {
        val st = _uiState.value
        val clientesDeEstaOrg = st.clientesDisponibles.filter { cliente ->
            st.vinculosUsuarioOrgs[cliente.id]?.contains(org.id) == true
        }
        val clienteValido = clientesDeEstaOrg.find { it.id == st.clienteSeleccionadoId }
            ?: clientesDeEstaOrg.firstOrNull()

        _uiState.update {
            it.copy(
                orgSeleccionadaId = org.id,
                orgSeleccionadaNombre = org.nombre,
                errorOrgCompromiso = null,
                clienteSeleccionadoId = clienteValido?.id ?: 0,
                clienteSeleccionadoNombre = clienteValido?.nombre ?: "",
                errorClienteCompromiso = null
            )
        }
    }

    fun onSeleccionarClienteCompromiso(cliente: UsuarioEntity) {
        _uiState.update {
            it.copy(
                clienteSeleccionadoId = cliente.id,
                clienteSeleccionadoNombre = cliente.nombre,
                errorClienteCompromiso = null
            )
        }
    }

    fun onConceptoChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El concepto es obligatorio"
            valor.trim().length < 3 -> "Mínimo 3 caracteres"
            else -> null
        }
        _uiState.update { it.copy(conceptoCompromiso = valor, errorConceptoCompromiso = error) }
    }

    fun onMontoChange(valor: String) {
        val soloNumeros = valor.filter { it.isDigit() }
        val montoNum = soloNumeros.toDoubleOrNull() ?: 0.0
        val error = when {
            soloNumeros.isBlank() -> "El monto es obligatorio"
            montoNum <= 0 -> "El monto debe ser mayor a $0"
            else -> null
        }
        _uiState.update { it.copy(montoCompromiso = soloNumeros, errorMontoCompromiso = error) }
    }

    fun onFechaChange(valor: String) {
        val regex = Regex("""^\d{2}/\d{2}/\d{4}$""")
        val error = if (!regex.matches(valor.trim())) "Usa formato DD/MM/AAAA" else null
        _uiState.update { it.copy(fechaCompromiso = valor, errorFechaCompromiso = error) }
    }

    fun guardarCompromiso(context: Context) {
        val st = _uiState.value
        onConceptoChange(st.conceptoCompromiso)
        onMontoChange(st.montoCompromiso)
        onFechaChange(st.fechaCompromiso)

        if (st.orgSeleccionadaId == 0 || st.orgSeleccionadaNombre.isBlank()) {
            _uiState.update { it.copy(errorOrgCompromiso = "Crea o selecciona una empresa primero") }
            return
        }
        if (st.clienteSeleccionadoId == 0 || st.clienteSeleccionadoNombre.isBlank()) {
            _uiState.update { it.copy(errorClienteCompromiso = "Selecciona un cliente vinculado a esta empresa") }
            return
        }

        val act = _uiState.value
        if (act.errorConceptoCompromiso != null || act.errorMontoCompromiso != null || act.errorFechaCompromiso != null) return

        viewModelScope.launch {
            val existente = act.compromisos.find { it.id == act.compromisoEditandoId }
            val entidad = CompromisoEntity(
                id = act.compromisoEditandoId,
                organizacionId = act.orgSeleccionadaId,
                empresaNombre = act.orgSeleccionadaNombre,
                concepto = act.conceptoCompromiso.trim(),
                monto = act.montoCompromiso.toDouble(),
                fechaVencimiento = act.fechaCompromiso.trim(),
                estado = existente?.estado ?: "PENDIENTE",
                comprobanteUri = existente?.comprobanteUri
            )
            repository.guardarCompromiso(entidad)

            val idAsignar = if (act.compromisoEditandoId != 0) {
                act.compromisoEditandoId
            } else {
                repository.observarCompromisos().first()
                    .filter { it.concepto == entidad.concepto && it.empresaNombre == entidad.empresaNombre }
                    .maxByOrNull { it.id }?.id ?: 0
            }

            if (idAsignar != 0) {
                guardarAsignacionCobro(
                    context = context,
                    compromisoId = idAsignar,
                    usuarioId = act.clienteSeleccionadoId,
                    usuarioNombre = act.clienteSeleccionadoNombre
                )
            }

            NotificadorPagoFlex.mostrarNotificacion(
                context,
                "Cobro emitido a ${act.clienteSeleccionadoNombre}",
                "${entidad.empresaNombre}: ${entidad.concepto} ($${entidad.monto.toInt()})"
            )
            _uiState.update {
                it.copy(
                    mostrarDialogoCompromiso = false,
                    mensajeSnackbar = if (act.compromisoEditandoId == 0) {
                        "Cobro asignado a ${act.clienteSeleccionadoNombre}"
                    } else {
                        "Compromiso actualizado"
                    }
                )
            }
        }
    }

    fun registrarComprobante(context: Context, compromisoId: Int, uriString: String?) {
        viewModelScope.launch {
            repository.subirComprobante(compromisoId, uriString)
            NotificadorPagoFlex.mostrarNotificacion(
                context,
                "Comprobante en revisión",
                "Tu comprobante fue enviado al Ejecutivo para validación."
            )
            _uiState.update { it.copy(mensajeSnackbar = "Comprobante enviado a revisión") }
        }
    }

    fun resolverComprobante(context: Context, compromisoId: Int, aprobado: Boolean) {
        val comp = _uiState.value.compromisos.find { it.id == compromisoId }
        val descripcion = comp?.let { "${it.concepto} (${it.empresaNombre})" } ?: "El compromiso"
        viewModelScope.launch {
            repository.resolverComprobante(compromisoId, aprobado)
            val mensajeDetalle = if (aprobado) {
                "$descripcion fue aprobado y conciliado."
            } else {
                "$descripcion fue rechazado. Motivo: $MOTIVO_RECHAZO_GENERAL"
            }
            NotificadorPagoFlex.mostrarNotificacion(
                context,
                if (aprobado) "Comprobante aprobado" else "Comprobante rechazado",
                mensajeDetalle
            )
            _uiState.update { it.copy(mensajeSnackbar = mensajeDetalle) }
        }
    }

    // --- CRUD ORGANIZACIONES / EMPRESAS ---
    fun abrirDialogoNuevaOrg() {
        _uiState.update {
            it.copy(
                mostrarDialogoOrg = true,
                orgEditandoId = 0,
                nombreOrg = "",
                errorNombreOrg = null,
                tipoOrg = "Empresa",
                rutOrg = "",
                errorRutOrg = null,
                correoOrg = "",
                errorCorreoOrg = null
            )
        }
    }

    fun abrirDialogoEditarOrg(org: OrganizacionEntity) {
        _uiState.update {
            it.copy(
                mostrarDialogoOrg = true,
                orgEditandoId = org.id,
                nombreOrg = org.nombre,
                errorNombreOrg = null,
                tipoOrg = org.tipo,
                rutOrg = org.rut,
                errorRutOrg = null,
                correoOrg = org.correoContacto,
                errorCorreoOrg = null
            )
        }
    }

    fun cerrarDialogoOrg() {
        _uiState.update { it.copy(mostrarDialogoOrg = false) }
    }

    fun onNombreOrgChange(valor: String) {
        val error = if (valor.trim().length < 2) "Ingresa el nombre de la organización" else null
        _uiState.update { it.copy(nombreOrg = valor, errorNombreOrg = error) }
    }

    fun onTipoOrgChange(tipo: String) {
        _uiState.update { it.copy(tipoOrg = tipo) }
    }

    fun onRutOrgChange(valor: String) {
        val error = if (valor.trim().length < 8) "RUT inválido (ej: 76.123.456-7)" else null
        _uiState.update { it.copy(rutOrg = valor, errorRutOrg = error) }
    }

    fun onCorreoOrgChange(valor: String) {
        val error = if (!Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches()) "Correo de contacto inválido" else null
        _uiState.update { it.copy(correoOrg = valor, errorCorreoOrg = error) }
    }

    fun guardarOrganizacion() {
        val st = _uiState.value
        onNombreOrgChange(st.nombreOrg)
        onRutOrgChange(st.rutOrg)
        onCorreoOrgChange(st.correoOrg)

        val act = _uiState.value
        if (act.errorNombreOrg != null || act.errorRutOrg != null || act.errorCorreoOrg != null) return

        viewModelScope.launch {
            val org = OrganizacionEntity(
                id = act.orgEditandoId,
                nombre = act.nombreOrg.trim(),
                tipo = act.tipoOrg,
                rut = act.rutOrg.trim(),
                correoContacto = act.correoOrg.trim()
            )
            repository.guardarOrganizacion(org)
            _uiState.update {
                it.copy(
                    mostrarDialogoOrg = false,
                    mensajeSnackbar = if (act.orgEditandoId == 0) "Organización creada" else "Organización actualizada"
                )
            }
        }
    }

    fun eliminarOrganizacion(org: OrganizacionEntity) {
        viewModelScope.launch {
            repository.eliminarOrganizacion(org)
            _uiState.update { it.copy(mensajeSnackbar = "Organización eliminada") }
        }
    }

    // --- MODAL 1: FOTO DE PERFIL ---
    fun abrirDialogoFotoPerfil() {
        _uiState.update { it.copy(mostrarDialogoFotoPerfil = true) }
    }

    fun cerrarDialogoFotoPerfil() {
        _uiState.update { it.copy(mostrarDialogoFotoPerfil = false) }
    }

    // --- MODAL 2: DATOS PERSONALES ---
    fun abrirDialogoDatosPersonales() {
        val user = _uiState.value.usuarioActual ?: return
        _uiState.update {
            it.copy(
                mostrarDialogoDatosPersonales = true,
                nombrePerfilInput = user.nombre,
                errorNombrePerfilInput = null
            )
        }
    }

    fun cerrarDialogoDatosPersonales() {
        _uiState.update { it.copy(mostrarDialogoDatosPersonales = false) }
    }

    fun onNombrePerfilInputChange(valor: String) {
        val error = if (valor.trim().length < 3) "Mínimo 3 caracteres" else null
        _uiState.update { it.copy(nombrePerfilInput = valor, errorNombrePerfilInput = error) }
    }

    fun guardarDatosPersonales() {
        val st = _uiState.value
        val user = st.usuarioActual ?: return
        onNombrePerfilInputChange(st.nombrePerfilInput)
        val act = _uiState.value
        if (act.errorNombrePerfilInput != null) return

        viewModelScope.launch {
            repository.actualizarUsuario(user.copy(nombre = act.nombrePerfilInput.trim()))
            _uiState.update {
                it.copy(
                    mostrarDialogoDatosPersonales = false,
                    mensajeSnackbar = "Nombre actualizado correctamente"
                )
            }
        }
    }

    // --- MODAL 3: SEGURIDAD, CORREOS Y CLAVE ---
    fun abrirDialogoSeguridad() {
        val st = _uiState.value
        val user = st.usuarioActual ?: return
        _uiState.update {
            it.copy(
                mostrarDialogoSeguridad = true,
                correoPrincipalInput = user.correo,
                errorCorreoPrincipalInput = null,
                correoRespaldoInput = st.correoRespaldo,
                errorCorreoRespaldoInput = null,
                expandirCambioClavePerfil = false,
                claveActualPerfil = "",
                errorClaveActualPerfil = null,
                nuevaClavePerfil = "",
                errorClavePerfil = null,
                confirmarClavePerfil = "",
                errorConfirmarClavePerfil = null
            )
        }
    }

    fun cerrarDialogoSeguridad() {
        _uiState.update { it.copy(mostrarDialogoSeguridad = false) }
    }

    fun onCorreoPrincipalInputChange(valor: String) {
        val error = when {
            valor.isBlank() -> "El correo principal no puede quedar vacío"
            !Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches() -> "Formato de correo inválido"
            else -> null
        }
        _uiState.update { it.copy(correoPrincipalInput = valor, errorCorreoPrincipalInput = error) }
    }

    fun onCorreoRespaldoInputChange(valor: String) {
        val error = if (valor.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches()) {
            "Formato de correo de respaldo inválido"
        } else {
            null
        }
        _uiState.update { it.copy(correoRespaldoInput = valor, errorCorreoRespaldoInput = error) }
    }

    fun eliminarCorreoRespaldo(context: Context) {
        val user = _uiState.value.usuarioActual ?: return
        val prefs = context.getSharedPreferences(PREFS_PERFIL_EXTRA, Context.MODE_PRIVATE)
        prefs.edit().remove("correo_resp_${user.id}").apply()
        _uiState.update {
            it.copy(
                correoRespaldo = "",
                correoRespaldoInput = "",
                errorCorreoRespaldoInput = null,
                mensajeSnackbar = "Correo de respaldo eliminado"
            )
        }
    }

    fun onToggleExpandirCambioClave(expandir: Boolean) {
        _uiState.update {
            it.copy(
                expandirCambioClavePerfil = expandir,
                claveActualPerfil = "",
                errorClaveActualPerfil = null,
                nuevaClavePerfil = "",
                errorClavePerfil = null,
                confirmarClavePerfil = "",
                errorConfirmarClavePerfil = null
            )
        }
    }

    fun onClaveActualPerfilChange(valor: String) {
        val claveReal = _uiState.value.usuarioActual?.clave ?: ""
        val error = when {
            valor.isBlank() -> "Ingresa tu contraseña actual para confirmar"
            valor != claveReal -> "La contraseña actual no es correcta"
            else -> null
        }
        _uiState.update { it.copy(claveActualPerfil = valor, errorClaveActualPerfil = error) }
    }

    fun onNuevaClavePerfilChange(valor: String) {
        val claveReal = _uiState.value.usuarioActual?.clave ?: ""
        val errorNueva = when {
            valor.length < 6 -> "Mínimo 6 caracteres"
            valor == claveReal -> "Debe ser distinta a la contraseña actual"
            else -> null
        }
        val confirmar = _uiState.value.confirmarClavePerfil
        val errorConfirmar = when {
            confirmar.isBlank() -> "Confirma tu nueva contraseña"
            confirmar != valor -> "Las contraseñas nuevas no coinciden"
            else -> null
        }
        _uiState.update {
            it.copy(
                nuevaClavePerfil = valor,
                errorClavePerfil = errorNueva,
                errorConfirmarClavePerfil = errorConfirmar
            )
        }
    }

    fun onConfirmarClavePerfilChange(valor: String) {
        val nueva = _uiState.value.nuevaClavePerfil
        val error = when {
            valor.isBlank() -> "Confirma tu nueva contraseña"
            valor != nueva -> "Las contraseñas nuevas no coinciden"
            else -> null
        }
        _uiState.update { it.copy(confirmarClavePerfil = valor, errorConfirmarClavePerfil = error) }
    }

    fun guardarSeguridadCuenta(context: Context) {
        val st = _uiState.value
        val user = st.usuarioActual ?: return

        onCorreoPrincipalInputChange(st.correoPrincipalInput)
        onCorreoRespaldoInputChange(st.correoRespaldoInput)

        val cambiaCorreoPrincipal = st.correoPrincipalInput.trim() != user.correo
        val intentaCambiarClave = st.expandirCambioClavePerfil ||
                st.nuevaClavePerfil.isNotBlank() ||
                st.confirmarClavePerfil.isNotBlank()

        if (cambiaCorreoPrincipal || intentaCambiarClave) {
            onClaveActualPerfilChange(st.claveActualPerfil)
        } else {
            _uiState.update { it.copy(errorClaveActualPerfil = null) }
        }

        if (intentaCambiarClave) {
            onNuevaClavePerfilChange(st.nuevaClavePerfil)
            onConfirmarClavePerfilChange(st.confirmarClavePerfil)
        } else {
            _uiState.update {
                it.copy(
                    errorClavePerfil = null,
                    errorConfirmarClavePerfil = null
                )
            }
        }

        val act = _uiState.value
        if (act.errorCorreoPrincipalInput != null ||
            act.errorCorreoRespaldoInput != null ||
            act.errorClaveActualPerfil != null ||
            act.errorClavePerfil != null ||
            act.errorConfirmarClavePerfil != null
        ) return

        viewModelScope.launch {
            val correoFinal = act.correoPrincipalInput.trim()
            val claveFinal = if (intentaCambiarClave) act.nuevaClavePerfil else user.clave

            repository.actualizarUsuario(
                user.copy(
                    correo = correoFinal,
                    clave = claveFinal
                )
            )

            val prefs = context.getSharedPreferences(PREFS_PERFIL_EXTRA, Context.MODE_PRIVATE)
            val respLimpio = act.correoRespaldoInput.trim()
            if (respLimpio.isBlank()) {
                prefs.edit().remove("correo_resp_${user.id}").apply()
            } else {
                prefs.edit().putString("correo_resp_${user.id}", respLimpio).apply()
            }

            _uiState.update {
                it.copy(
                    mostrarDialogoSeguridad = false,
                    correoRespaldo = respLimpio,
                    claveActualPerfil = "",
                    nuevaClavePerfil = "",
                    confirmarClavePerfil = "",
                    mensajeSnackbar = "Configuración de seguridad y correos actualizada"
                )
            }
        }
    }

    // --- DESCARGA REAL DE INFORME DE GASTOS PROPIOS (USUARIO) / GENERAL (GESTOR) ---
    fun exportarReporteCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val st = _uiState.value
                val user = st.usuarioActual
                val esGestorCobros = user?.rol == "ADMIN" || user?.rol == "EJECUTIVO"
                val asignaciones = st.asignacionesCobros

                val sb = StringBuilder()
                sb.append('\uFEFF')

                val nombreArchivo: String
                if (!esGestorCobros && user != null) {
                    val orgsVinculadas = st.vinculosUsuarioOrgs[user.id].orEmpty()
                    val misGastos = st.compromisos.filter { comp ->
                        val perteneceAEmpresaVinculada = orgsVinculadas.contains(comp.organizacionId)
                        val asignacion = asignaciones[comp.id]
                        val correspondeAlUsuario = asignacion == null || asignacion.usuarioId == user.id
                        perteneceAEmpresaVinculada && correspondeAlUsuario
                    }

                    val nombreSeguro = user.nombre
                        .trim()
                        .replace(Regex("[^A-Za-z0-9]"), "_")
                        .replace(Regex("_+"), "_")
                    nombreArchivo = "Informe_Gastos_${nombreSeguro}_${System.currentTimeMillis() / 1000}.csv"

                    sb.appendLine("INFORME PERSONAL DE GASTOS Y COMPROMISOS - PAGOFLEX")
                    sb.appendLine("Titular;${user.nombre}")
                    sb.appendLine("Correo;${user.correo}")
                    sb.appendLine("")
                    sb.appendLine("Vencimiento;Empresa_o_Entidad;Concepto;Monto_CLP;Estado;Observacion")

                    misGastos.forEach { c ->
                        val obs = when (c.estado) {
                            "PAGADO" -> "Pago verificado y conciliado"
                            "REVISION" -> "Comprobante enviado, pendiente de validacion"
                            "RECHAZADO" -> MOTIVO_RECHAZO_GENERAL
                            else -> "Pendiente de pago"
                        }
                        sb.appendLine("${c.fechaVencimiento};${c.empresaNombre};${c.concepto};${c.monto.toInt()};${c.estado};$obs")
                    }

                    val totalPagado = misGastos.filter { it.estado == "PAGADO" }.sumOf { it.monto.toInt() }
                    val totalPorPagar = misGastos.filter { it.estado == "PENDIENTE" || it.estado == "RECHAZADO" }.sumOf { it.monto.toInt() }
                    val totalRevision = misGastos.filter { it.estado == "REVISION" }.sumOf { it.monto.toInt() }

                    sb.appendLine("")
                    sb.appendLine("RESUMEN DE MIS GASTOS;;;Monto_CLP;;")
                    sb.appendLine("Total Pagado (Conciliado);;;$totalPagado;;")
                    sb.appendLine("Total En Revision;;;$totalRevision;;")
                    sb.appendLine("Total Por Pagar / Regularizar;;;$totalPorPagar;;")
                } else {
                    nombreArchivo = "Reporte_Consolidado_PagoFlex_${System.currentTimeMillis() / 1000}.csv"
                    sb.appendLine("Empresa;Cliente_Asignado;Concepto;Monto_CLP;Vencimiento;Estado;Observacion")
                    st.compromisos.forEach { c ->
                        val cliente = asignaciones[c.id]?.usuarioNombre ?: "Clientes Vinculados"
                        val obs = if (c.estado == "RECHAZADO") MOTIVO_RECHAZO_GENERAL else "-"
                        sb.appendLine("${c.empresaNombre};$cliente;${c.concepto};${c.monto.toInt()};${c.fechaVencimiento};${c.estado};$obs")
                    }
                }

                val contenidoBytes = sb.toString().toByteArray(Charsets.UTF_8)

                val archivoCache = File(context.cacheDir, nombreArchivo)
                archivoCache.writeBytes(contenidoBytes)
                val uriApertura = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    archivoCache
                )

                var guardadoEnDescargas = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
                        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PagoFlex")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (downloadUri != null) {
                        resolver.openOutputStream(downloadUri)?.use { out ->
                            out.write(contenidoBytes)
                        }
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(downloadUri, values, null, null)
                        guardadoEnDescargas = true
                    }
                }

                withContext(Dispatchers.Main) {
                    val destinoTexto = if (guardadoEnDescargas) {
                        "Informe descargado en Descargas/PagoFlex"
                    } else {
                        "Informe de gastos generado"
                    }
                    _uiState.update {
                        it.copy(
                            mensajeSnackbar = destinoTexto,
                            ultimoInformeUri = uriApertura
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            mensajeSnackbar = "No se pudo descargar el informe: ${e.message}",
                            ultimoInformeUri = null
                        )
                    }
                }
            }
        }
    }
}