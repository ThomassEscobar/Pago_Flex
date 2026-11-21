package cl.pagoflex.app.nucleo.funcionalidades.inicio

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.util.PatternsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.local.dao.CompromisoDao
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.dao.UsuarioDao
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import cl.pagoflex.app.nucleo.util.NotificadorPagoFlex
import cl.pagoflex.app.nucleo.util.ValidadorRut
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

enum class EstadoVinculo {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    BLOQUEADA
}

data class SolicitudVinculacion(
    val usuarioId: Int,
    val organizacionId: Int,
    val estado: EstadoVinculo,
    val fechaSolicitud: String,
    val motivoRechazo: String? = null
)

data class AsignacionCobro(
    val compromisoId: Int,
    val usuarioId: Int,
    val usuarioNombre: String
)

data class PlantillaCobroRecurrente(
    val id: String,
    val organizacionId: Int,
    val empresaNombre: String,
    val usuarioId: Int,
    val usuarioNombre: String,
    val concepto: String,
    val monto: Double,
    val diaVencimiento: Int,
    val mesesProgramados: Int,
    val fechaRegistro: String
)

data class PerfilFinancieroCliente(
    val cliente: UsuarioEntity,
    val solicitud: SolicitudVinculacion,
    val strikesAcumulados: Int,
    val detalleStrikes: List<String>,
    val porcentajeCumplimiento: Int,
    val pagados: Int,
    val montoTotalPagado: Int,
    val pendientes: Int,
    val enRevision: Int,
    val compromisosPorVencerCount: Int,
    val rechazados: Int,
    val montoDeudaActual: Int,
    val nivelRiesgo: String,
    val bloqueadoEnEstaEmpresa: Boolean
)

data class TicketSoporte(
    val id: String,
    val usuarioId: Int,
    val usuarioNombre: String,
    val usuarioRol: String,
    val usuarioRut: String,
    val empresaNombre: String?,
    val categoria: String,
    val asunto: String,
    val mensaje: String,
    val fechaCreacion: String,
    val estado: String,
    val respuestaAdmin: String? = null,
    val fechaRespuesta: String? = null
)

data class InicioUiState(
    val cargando: Boolean = false,
    val usuarioActual: UsuarioEntity? = null,
    val todosUsuarios: List<UsuarioEntity> = emptyList(),
    val clientesDisponibles: List<UsuarioEntity> = emptyList(),
    val ejecutivosDisponibles: List<UsuarioEntity> = emptyList(),
    val organizaciones: List<OrganizacionEntity> = emptyList(),
    val compromisos: List<CompromisoEntity> = emptyList(),

    // Filtros y búsquedas
    val filtroEstado: String = "TODOS",
    val textoBusqueda: String = "",
    val busquedaClientesEmpresa: String = "",
    val busquedaAdmin: String = "",
    val busquedaPlataformaAdmin: String = "",
    val filtroEmpresasAdmin: String = "TODAS",
    val filtroSoporteAdmin: String = "PENDIENTES",
    val filtroStrikesAdmin: String = "TODOS",

    // Vínculos, solicitudes, recurrencia y gobernanza
    val vinculosUsuarioOrgs: Map<Int, Set<Int>> = emptyMap(),
    val solicitudesVinculacion: List<SolicitudVinculacion> = emptyList(),
    val asignacionesCobros: Map<Int, AsignacionCobro> = emptyMap(),
    val plantillasRecurrentes: List<PlantillaCobroRecurrente> = emptyList(),
    val bloqueosGlobalesApp: Map<Int, String> = emptyMap(),
    val empresasSuspendidas: Map<Int, String> = emptyMap(),

    // Soporte
    val ticketsSoporte: List<TicketSoporte> = emptyList(),

    // Detalle expandido de compromiso y pago
    val compromisoDetalleSeleccionado: CompromisoEntity? = null,
    val comprobanteBorradorUri: String? = null,

    // Diálogo nuevo compromiso (Ejecutivo - registro único / recurrente)
    val mostrarDialogoCompromiso: Boolean = false,
    val orgSeleccionadaId: Int = 0,
    val orgSeleccionadaNombre: String = "",
    val clienteSeleccionadoId: Int? = null,
    val clienteSeleccionadoNombre: String = "",
    val conceptoCompromiso: String = "",
    val montoCompromiso: String = "",
    val fechaCompromiso: String = "",
    val esCobroRecurrenteMensual: Boolean = true,
    val mesesRecurrencia: Int = 12,
    val errorClienteCompromiso: String? = null,
    val errorConceptoCompromiso: String? = null,
    val errorMontoCompromiso: String? = null,
    val errorFechaCompromiso: String? = null,

    // Diálogo Solicitar Vinculación (Usuario)
    val mostrarDialogoVincularEmpresaUsuario: Boolean = false,

    // Diálogo Registrar Nueva Empresa (Usuario)
    val mostrarDialogoRegistrarEmpresaUsuario: Boolean = false,
    val nuevaEmpresaNombreInput: String = "",
    val nuevaEmpresaTipoInput: String = "Empresa",
    val nuevaEmpresaRutInput: String = "",
    val nuevaEmpresaCorreoInput: String = "",
    val errorNuevaEmpresaNombre: String? = null,
    val errorNuevaEmpresaRut: String? = null,
    val errorNuevaEmpresaCorreo: String? = null,

    // Diálogo Agregar Nuevo Cliente (Ejecutivo)
    val mostrarDialogoAgregarClienteEjecutivo: Boolean = false,
    val nuevoClienteNombreInput: String = "",
    val nuevoClienteRutInput: String = "",
    val nuevoClienteCorreoInput: String = "",
    val errorNuevoClienteNombre: String? = null,
    val errorNuevoClienteRut: String? = null,
    val errorNuevoClienteCorreo: String? = null,

    // Diálogo Revisión Perfil Financiero (Ejecutivo)
    val perfilFinancieroEnRevision: PerfilFinancieroCliente? = null,
    val motivoRechazoSolicitudInput: String = "",
    val errorMotivoRechazoSolicitud: String? = null,

    // Diálogo Suspender Empresa (Admin)
    val orgSuspendiendoAdmin: OrganizacionEntity? = null,
    val motivoSuspensionOrgInput: String = "",
    val errorMotivoSuspensionOrg: String? = null,

    // Diálogo Gestionar Cuenta / Strikes (Admin)
    val usuarioEditandoAdmin: UsuarioEntity? = null,
    val motivoBloqueoAppInput: String = "",
    val errorMotivoBloqueoApp: String? = null,

    // Diálogo Soporte (Usuario / Ejecutivo)
    val mostrarDialogoSoporteUsuario: Boolean = false,
    val categoriaSoporteInput: String = "Problema con pago",
    val asuntoSoporteInput: String = "",
    val mensajeSoporteInput: String = "",
    val errorAsuntoSoporte: String? = null,
    val errorMensajeSoporte: String? = null,

    // Diálogo Responder Ticket (Admin)
    val ticketEnRevisionAdmin: TicketSoporte? = null,
    val respuestaTicketAdminInput: String = "",
    val errorRespuestaTicketAdmin: String? = null,

    // Perfil y Seguridad
    val avatarPath: String? = null,
    val mostrarDialogoFotoPerfil: Boolean = false,
    val mostrarDialogoDatosPersonales: Boolean = false,
    val nombrePerfilInput: String = "",
    val errorNombrePerfilInput: String? = null,
    val mostrarDialogoSeguridad: Boolean = false,
    val claveActualPerfil: String = "",
    val nuevaClavePerfil: String = "",
    val confirmarClavePerfil: String = "",
    val errorClaveActualPerfil: String? = null,
    val errorClavePerfil: String? = null,
    val errorConfirmarClavePerfil: String? = null,

    // Feedback
    val mensajeSnackbar: String? = null,
    val ultimoInformeUri: Uri? = null
)

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val organizacionDao: OrganizacionDao,
    private val compromisoDao: CompromisoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    companion object {
        const val MOTIVO_RECHAZO_GENERAL = "El comprobante no ha podido ser corroborado."
        const val STRIKES_UMBRAL_APP = 3
        private const val PREFS_NAME = "pagoflex_estado_ext"
        private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun parseFecha(fechaVencimiento: String): LocalDate? {
            return try {
                if (fechaVencimiento.contains("/")) {
                    LocalDate.parse(fechaVencimiento.trim(), FORMATO_FECHA)
                } else {
                    LocalDate.parse(fechaVencimiento.trim())
                }
            } catch (_: Exception) {
                null
            }
        }

        fun calcularDiasParaVencimiento(fechaVencimiento: String): Int? {
            val fecha = parseFecha(fechaVencimiento) ?: return null
            return ChronoUnit.DAYS.between(LocalDate.now(), fecha).toInt()
        }

        fun esCompromisoExigibleAhora(compromiso: CompromisoEntity): Boolean {
            if (compromiso.estado != "PENDIENTE" && compromiso.estado != "RECHAZADO") return false
            val esRecurrente = compromiso.concepto.contains("Recurrente", ignoreCase = true)
            if (!esRecurrente) return true
            val fecha = parseFecha(compromiso.fechaVencimiento) ?: return true
            val hoy = LocalDate.now()
            val mesActual = YearMonth.from(hoy)
            val mesVencimiento = YearMonth.from(fecha)
            if (!mesVencimiento.isAfter(mesActual)) return true
            val dias = ChronoUnit.DAYS.between(hoy, fecha).toInt()
            return dias <= 25
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun inicializarEntorno(context: Context) {
        viewModelScope.launch {
            cargarDesdePersistencia(context)
            val sp = prefs(context)
            if (!sp.getBoolean("entorno_demo_v2", false)) {
                poblarDatosCompletosDemo(context)
                sp.edit().putBoolean("entorno_demo_v2", true).apply()
            }
        }
    }

    private suspend fun cargarDesdePersistencia(context: Context) {
        val usuarios: List<UsuarioEntity> = usuarioDao.observarTodos().first()
        val orgs: List<OrganizacionEntity> = organizacionDao.observarTodas().first()
        val comps: List<CompromisoEntity> = compromisoDao.observarTodos().first()
            .sortedWith(
                compareBy<CompromisoEntity> { parseFecha(it.fechaVencimiento) ?: LocalDate.MAX }
                    .thenBy { it.id }
            )

        val sp = prefs(context)
        val solicitudes = leerSolicitudesPrefs(sp)
        val asignaciones = leerAsignacionesPrefs(sp)
        val plantillas = leerPlantillasRecurrentesPrefs(sp)
        val bloqueosApp = leerMapaIntStringPrefs(sp, "bloqueos_app")
        val empresasSusp = leerMapaIntStringPrefs(sp, "empresas_suspendidas")
        val tickets = leerTicketsPrefs(sp)

        val vinculos = solicitudes
            .filter { it.estado == EstadoVinculo.APROBADA }
            .groupBy { it.usuarioId }
            .mapValues { entry -> entry.value.map { it.organizacionId }.toSet() }

        _uiState.update { state ->
            val userRefrescado = state.usuarioActual?.let { actual ->
                usuarios.find { it.id == actual.id } ?: actual
            }
            val detalleRefrescado = state.compromisoDetalleSeleccionado?.let { det ->
                comps.find { it.id == det.id }
            }
            state.copy(
                usuarioActual = userRefrescado,
                todosUsuarios = usuarios,
                clientesDisponibles = usuarios.filter { it.rol == "USUARIO" },
                ejecutivosDisponibles = usuarios.filter { it.rol == "EJECUTIVO" },
                organizaciones = orgs,
                compromisos = comps,
                compromisoDetalleSeleccionado = detalleRefrescado,
                solicitudesVinculacion = solicitudes,
                vinculosUsuarioOrgs = vinculos,
                asignacionesCobros = asignaciones,
                plantillasRecurrentes = plantillas,
                bloqueosGlobalesApp = bloqueosApp,
                empresasSuspendidas = empresasSusp,
                ticketsSoporte = tickets
            )
        }
    }

    private suspend fun poblarDatosCompletosDemo(context: Context) {
        val sp = prefs(context)
        val orgsActuales: List<OrganizacionEntity> = organizacionDao.observarTodas().first()
        val usuariosActuales: List<UsuarioEntity> = usuarioDao.observarTodos().first()
        val compsActuales: List<CompromisoEntity> = compromisoDao.observarTodos().first()

        val hoy = LocalDate.now()
        val primerCliente = usuariosActuales.firstOrNull { it.rol == "USUARIO" }
        val segundoCliente = usuariosActuales.filter { it.rol == "USUARIO" }.getOrNull(1)
        val primerEjecutivo = usuariosActuales.firstOrNull { it.rol == "EJECUTIVO" }
        val primeraOrg = orgsActuales.firstOrNull()

        if (leerSolicitudesPrefs(sp).isEmpty() && primerCliente != null && orgsActuales.isNotEmpty()) {
            val solicitudesDemo = mutableListOf<SolicitudVinculacion>()
            orgsActuales.take(2).forEach { org ->
                solicitudesDemo.add(
                    SolicitudVinculacion(
                        usuarioId = primerCliente.id,
                        organizacionId = org.id,
                        estado = EstadoVinculo.APROBADA,
                        fechaSolicitud = hoy.minusDays(20).format(FORMATO_FECHA)
                    )
                )
            }
            if (segundoCliente != null && primeraOrg != null) {
                solicitudesDemo.add(
                    SolicitudVinculacion(
                        usuarioId = segundoCliente.id,
                        organizacionId = primeraOrg.id,
                        estado = EstadoVinculo.PENDIENTE,
                        fechaSolicitud = hoy.minusDays(1).format(FORMATO_FECHA)
                    )
                )
            }
            guardarSolicitudesPrefs(sp, solicitudesDemo)
        }

        if (primerCliente != null && compsActuales.isNotEmpty()) {
            val asigActuales = leerAsignacionesPrefs(sp).toMutableMap()
            compsActuales.forEach { comp ->
                if (!asigActuales.containsKey(comp.id)) {
                    asigActuales[comp.id] = AsignacionCobro(
                        compromisoId = comp.id,
                        usuarioId = primerCliente.id,
                        usuarioNombre = primerCliente.nombre
                    )
                }
            }
            guardarAsignacionesPrefs(sp, asigActuales)
        }

        if (leerTicketsPrefs(sp).isEmpty()) {
            val ticketsIniciales = buildList {
                if (primerEjecutivo != null) {
                    add(
                        TicketSoporte(
                            id = "TCK-1001",
                            usuarioId = primerEjecutivo.id,
                            usuarioNombre = primerEjecutivo.nombre,
                            usuarioRol = "EJECUTIVO",
                            usuarioRut = primerEjecutivo.rut,
                            empresaNombre = primeraOrg?.nombre,
                            categoria = "Validación de comprobante",
                            asunto = "Validación de transferencia duplicada",
                            mensaje = "Un residente adjuntó dos veces el mismo comprobante de gasto común. Necesitamos confirmar si el sistema lo bloquea.",
                            fechaCreacion = hoy.minusDays(1).format(FORMATO_FECHA),
                            estado = "PENDIENTE"
                        )
                    )
                }
                if (primerCliente != null) {
                    add(
                        TicketSoporte(
                            id = "TCK-1002",
                            usuarioId = primerCliente.id,
                            usuarioNombre = primerCliente.nombre,
                            usuarioRol = "USUARIO",
                            usuarioRut = primerCliente.rut,
                            empresaNombre = null,
                            categoria = "Apelación de Strike",
                            asunto = "Consulta sobre fecha de vencimiento",
                            mensaje = "Realicé el pago dentro del plazo pero quedó en revisión hasta el día siguiente. ¿Me sumará strike?",
                            fechaCreacion = hoy.minusDays(3).format(FORMATO_FECHA),
                            estado = "RESUELTO",
                            respuestaAdmin = "Mientras el comprobante sea aprobado por el ejecutivo, el strike asociado al cobro se elimina automáticamente.",
                            fechaRespuesta = hoy.minusDays(2).format(FORMATO_FECHA)
                        )
                    )
                }
            }
            guardarTicketsPrefs(sp, ticketsIniciales)
        }

        cargarDesdePersistencia(context)
    }

    fun cargarUsuario(usuarioId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val user: UsuarioEntity? = usuarioDao.observarUsuario(usuarioId).first()
            _uiState.update { state ->
                state.copy(
                    cargando = false,
                    usuarioActual = user,
                    filtroEstado = "TODOS"
                )
            }
        }
    }

    fun cargarDatosExtraPerfil(context: Context) {
        val userId = _uiState.value.usuarioActual?.id ?: return
        val sp = prefs(context)
        val avatar = sp.getString("avatar_$userId", null)
        _uiState.update { it.copy(avatarPath = avatar) }
    }

    // ---------------------------------------------------------
    // DETALLE EXPANDIDO DE COMPROMISO Y PAGO CON COMPROBANTE
    // ---------------------------------------------------------

    fun abrirDetalleCompromiso(compromiso: CompromisoEntity) {
        _uiState.update {
            it.copy(
                compromisoDetalleSeleccionado = compromiso,
                comprobanteBorradorUri = compromiso.comprobanteUri
            )
        }
    }

    fun cerrarDetalleCompromiso() {
        _uiState.update {
            it.copy(
                compromisoDetalleSeleccionado = null,
                comprobanteBorradorUri = null
            )
        }
    }

    fun seleccionarComprobanteBorrador(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        _uiState.update { it.copy(comprobanteBorradorUri = uriString) }
    }

    fun confirmarPagoConComprobante(context: Context) {
        val state = _uiState.value
        val compromiso = state.compromisoDetalleSeleccionado ?: return
        val uriComprobante = state.comprobanteBorradorUri
        if (uriComprobante.isNullOrBlank()) {
            mostrarMensaje("Adjunta primero una foto o PDF del comprobante de pago.")
            return
        }

        viewModelScope.launch {
            compromisoDao.registrarComprobante(compromiso.id, "REVISION", uriComprobante)
            cargarDesdePersistencia(context)
            cerrarDetalleCompromiso()
            mostrarMensaje("Pago registrado y comprobante enviado a revisión del Ejecutivo.")
        }
    }

    // ---------------------------------------------------------
    // CÁLCULO DE STRIKES
    // ---------------------------------------------------------

    fun calcularStrikesCliente(usuarioId: Int): Pair<Int, List<String>> {
        val state = _uiState.value
        val orgsAprobadas = state.vinculosUsuarioOrgs[usuarioId].orEmpty()

        val compromisosCliente = state.compromisos.filter { comp ->
            val asig = state.asignacionesCobros[comp.id]
            if (asig != null) {
                asig.usuarioId == usuarioId
            } else {
                orgsAprobadas.contains(comp.organizacionId)
            }
        }

        val detalles = compromisosCliente.mapNotNull { comp ->
            if (comp.estado == "PAGADO") return@mapNotNull null
            val dias = calcularDiasParaVencimiento(comp.fechaVencimiento) ?: return@mapNotNull null
            if (dias < 0) {
                "Strike por mora: ${comp.empresaNombre} - ${comp.concepto} (Venció hace ${abs(dias)} día(s))"
            } else {
                null
            }
        }
        return detalles.size to detalles
    }

    // ---------------------------------------------------------
    // FILTROS Y BÚSQUEDAS
    // ---------------------------------------------------------

    fun onFiltroEstadoChange(nuevoFiltro: String) {
        _uiState.update { it.copy(filtroEstado = nuevoFiltro) }
    }

    fun onBusquedaChange(texto: String) {
        _uiState.update { it.copy(textoBusqueda = texto) }
    }

    fun onBusquedaClientesEmpresaChange(texto: String) {
        _uiState.update { it.copy(busquedaClientesEmpresa = texto) }
    }

    fun onBusquedaAdminChange(texto: String) {
        _uiState.update { it.copy(busquedaAdmin = texto) }
    }

    fun onBusquedaPlataformaAdminChange(texto: String) {
        _uiState.update { it.copy(busquedaPlataformaAdmin = texto) }
    }

    fun onFiltroEmpresasAdminChange(filtro: String) {
        _uiState.update { it.copy(filtroEmpresasAdmin = filtro) }
    }

    fun onFiltroSoporteAdminChange(filtro: String) {
        _uiState.update { it.copy(filtroSoporteAdmin = filtro) }
    }

    fun onFiltroStrikesAdminChange(filtro: String) {
        _uiState.update { it.copy(filtroStrikesAdmin = filtro) }
    }

    // ---------------------------------------------------------
    // COMPROMISOS (REGISTRO ÚNICO / RECURRENTE MENSUAL)
    // ---------------------------------------------------------

    fun abrirDialogoNuevoCompromiso() {
        val state = _uiState.value
        val user = state.usuarioActual ?: return
        val org = state.organizaciones.find { it.id == user.empresaId } ?: return

        if (state.empresasSuspendidas.containsKey(org.id)) {
            mostrarMensaje("Tu empresa está suspendida por Administración y no puede emitir cobros.")
            return
        }

        val clientesActivos = state.clientesDisponibles.filter { cliente ->
            state.vinculosUsuarioOrgs[cliente.id]?.contains(org.id) == true &&
                    !state.bloqueosGlobalesApp.containsKey(cliente.id)
        }
        val primerCliente = clientesActivos.firstOrNull()

        _uiState.update {
            it.copy(
                mostrarDialogoCompromiso = true,
                orgSeleccionadaId = org.id,
                orgSeleccionadaNombre = org.nombre,
                clienteSeleccionadoId = primerCliente?.id,
                clienteSeleccionadoNombre = primerCliente?.nombre ?: "",
                conceptoCompromiso = "",
                montoCompromiso = "",
                fechaCompromiso = LocalDate.now().plusDays(15).format(FORMATO_FECHA),
                esCobroRecurrenteMensual = true,
                mesesRecurrencia = 12,
                errorClienteCompromiso = null,
                errorConceptoCompromiso = null,
                errorMontoCompromiso = null,
                errorFechaCompromiso = null
            )
        }
    }

    fun cerrarDialogoCompromiso() {
        _uiState.update { it.copy(mostrarDialogoCompromiso = false) }
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
        _uiState.update { it.copy(conceptoCompromiso = valor, errorConceptoCompromiso = null) }
    }

    fun onMontoChange(valor: String) {
        val limpio = valor.filter { it.isDigit() }
        _uiState.update { it.copy(montoCompromiso = limpio, errorMontoCompromiso = null) }
    }

    fun onFechaChange(valor: String) {
        _uiState.update { it.copy(fechaCompromiso = valor, errorFechaCompromiso = null) }
    }

    fun onToggleCobroRecurrenteChange(recurrente: Boolean) {
        _uiState.update { it.copy(esCobroRecurrenteMensual = recurrente) }
    }

    fun onMesesRecurrenciaChange(meses: Int) {
        _uiState.update { it.copy(mesesRecurrencia = meses.coerceIn(2, 24)) }
    }

    fun guardarCompromiso(context: Context) {
        val state = _uiState.value
        val clienteId = state.clienteSeleccionadoId
        val concepto = state.conceptoCompromiso.trim()
        val monto = state.montoCompromiso.toDoubleOrNull() ?: 0.0
        val fechaInicio = parseFecha(state.fechaCompromiso.trim())

        var hayError = false
        if (clienteId == null) {
            _uiState.update { it.copy(errorClienteCompromiso = "Selecciona un cliente activo") }
            hayError = true
        }
        if (concepto.length < 3) {
            _uiState.update { it.copy(errorConceptoCompromiso = "Ingresa un concepto válido (mín. 3 car.)") }
            hayError = true
        }
        if (monto <= 0.0) {
            _uiState.update { it.copy(errorMontoCompromiso = "Ingresa un monto mayor a $0") }
            hayError = true
        }
        if (fechaInicio == null) {
            _uiState.update { it.copy(errorFechaCompromiso = "Usa formato DD/MM/AAAA válido") }
            hayError = true
        }
        if (hayError || clienteId == null || fechaInicio == null) return

        val totalCuotas = if (state.esCobroRecurrenteMensual) state.mesesRecurrencia else 1

        viewModelScope.launch {
            val sp = prefs(context)
            val asigMap = leerAsignacionesPrefs(sp).toMutableMap()

            for (indice in 0 until totalCuotas) {
                val fechaCuota = fechaInicio.plusMonths(indice.toLong()).format(FORMATO_FECHA)
                val conceptoCuota = if (state.esCobroRecurrenteMensual) {
                    "$concepto (Mes ${indice + 1}/$totalCuotas • Recurrente)"
                } else {
                    concepto
                }

                val nuevo = CompromisoEntity(
                    organizacionId = state.orgSeleccionadaId,
                    empresaNombre = state.orgSeleccionadaNombre,
                    concepto = conceptoCuota,
                    monto = monto,
                    fechaVencimiento = fechaCuota,
                    estado = "PENDIENTE",
                    comprobanteUri = null
                )
                compromisoDao.insertar(nuevo)

                val compsActualizados: List<CompromisoEntity> = compromisoDao.observarTodos().first()
                val creado = compsActualizados.maxByOrNull { it.id }
                if (creado != null) {
                    asigMap[creado.id] = AsignacionCobro(
                        compromisoId = creado.id,
                        usuarioId = clienteId,
                        usuarioNombre = state.clienteSeleccionadoNombre
                    )
                }
            }
            guardarAsignacionesPrefs(sp, asigMap)

            if (state.esCobroRecurrenteMensual) {
                val plantillas = leerPlantillasRecurrentesPrefs(sp).toMutableList()
                plantillas.add(
                    0,
                    PlantillaCobroRecurrente(
                        id = "REC-${System.currentTimeMillis().toString().takeLast(4)}",
                        organizacionId = state.orgSeleccionadaId,
                        empresaNombre = state.orgSeleccionadaNombre,
                        usuarioId = clienteId,
                        usuarioNombre = state.clienteSeleccionadoNombre,
                        concepto = concepto,
                        monto = monto,
                        diaVencimiento = fechaInicio.dayOfMonth,
                        mesesProgramados = totalCuotas,
                        fechaRegistro = LocalDate.now().format(FORMATO_FECHA)
                    )
                )
                guardarPlantillasRecurrentesPrefs(sp, plantillas)
            }

            cargarDesdePersistencia(context)
            _uiState.update {
                it.copy(
                    mostrarDialogoCompromiso = false,
                    mensajeSnackbar = if (state.esCobroRecurrenteMensual) {
                        "Cobro recurrente registrado una sola vez ($totalCuotas meses) para ${state.clienteSeleccionadoNombre}."
                    } else {
                        "Cobro emitido a ${state.clienteSeleccionadoNombre}."
                    }
                )
            }
        }
    }

    fun resolverComprobante(context: Context, compromisoId: Int, aprobado: Boolean) {
        viewModelScope.launch {
            val comp = _uiState.value.compromisos.find { it.id == compromisoId } ?: return@launch
            val nuevoEstado = if (aprobado) "PAGADO" else "RECHAZADO"
            compromisoDao.cambiarEstado(compromisoId, nuevoEstado)

            cargarDesdePersistencia(context)
            cerrarDetalleCompromiso()
            NotificadorPagoFlex.mostrarNotificacion(
                context,
                if (aprobado) "Pago aprobado" else "Comprobante rechazado",
                "${comp.empresaNombre}: ${comp.concepto}"
            )
            mostrarMensaje(if (aprobado) "Pago verificado y conciliado." else "Comprobante rechazado.")
        }
    }

    // ---------------------------------------------------------
    // VINCULACIÓN Y REGISTRO DE EMPRESAS (CLIENTE)
    // ---------------------------------------------------------

    fun abrirDialogoVincularEmpresaUsuario() {
        _uiState.update { it.copy(mostrarDialogoVincularEmpresaUsuario = true) }
    }

    fun cerrarDialogoVincularEmpresaUsuario() {
        _uiState.update { it.copy(mostrarDialogoVincularEmpresaUsuario = false) }
    }

    fun abrirDialogoRegistrarEmpresaUsuario() {
        _uiState.update {
            it.copy(
                mostrarDialogoRegistrarEmpresaUsuario = true,
                nuevaEmpresaNombreInput = "",
                nuevaEmpresaTipoInput = "Empresa",
                nuevaEmpresaRutInput = "",
                nuevaEmpresaCorreoInput = "",
                errorNuevaEmpresaNombre = null,
                errorNuevaEmpresaRut = null,
                errorNuevaEmpresaCorreo = null
            )
        }
    }

    fun cerrarDialogoRegistrarEmpresaUsuario() {
        _uiState.update { it.copy(mostrarDialogoRegistrarEmpresaUsuario = false) }
    }

    fun onNuevaEmpresaNombreChange(valor: String) {
        _uiState.update { it.copy(nuevaEmpresaNombreInput = valor, errorNuevaEmpresaNombre = null) }
    }

    fun onNuevaEmpresaTipoChange(tipo: String) {
        _uiState.update { it.copy(nuevaEmpresaTipoInput = tipo) }
    }

    fun onNuevaEmpresaRutChange(valor: String) {
        val formateado = ValidadorRut.formatearRut(valor)
        _uiState.update { it.copy(nuevaEmpresaRutInput = formateado, errorNuevaEmpresaRut = null) }
    }

    fun onNuevaEmpresaCorreoChange(valor: String) {
        _uiState.update { it.copy(nuevaEmpresaCorreoInput = valor, errorNuevaEmpresaCorreo = null) }
    }

    fun registrarNuevaEmpresaUsuario(context: Context) {
        val state = _uiState.value
        val user = state.usuarioActual ?: return
        val nombre = state.nuevaEmpresaNombreInput.trim()
        val tipo = state.nuevaEmpresaTipoInput
        val rutFormateado = ValidadorRut.formatearRut(state.nuevaEmpresaRutInput)
        val correo = state.nuevaEmpresaCorreoInput.trim()

        var hayError = false
        if (nombre.length < 3) {
            _uiState.update { it.copy(errorNuevaEmpresaNombre = "Ingresa el nombre de la empresa (mín. 3 car.)") }
            hayError = true
        }
        if (rutFormateado.isBlank() || !ValidadorRut.esRutValido(rutFormateado)) {
            _uiState.update { it.copy(errorNuevaEmpresaRut = "RUT de empresa inválido (ej: 76.123.456-0)") }
            hayError = true
        } else if (state.organizaciones.any { ValidadorRut.formatearRut(it.rut).equals(rutFormateado, ignoreCase = true) }) {
            _uiState.update { it.copy(errorNuevaEmpresaRut = "Ya existe una empresa registrada con ese RUT") }
            hayError = true
        }
        if (correo.isBlank() || !PatternsCompat.EMAIL_ADDRESS.matcher(correo).matches()) {
            _uiState.update { it.copy(errorNuevaEmpresaCorreo = "Ingresa un correo institucional válido") }
            hayError = true
        }
        if (hayError) return

        viewModelScope.launch {
            organizacionDao.insertar(
                OrganizacionEntity(
                    nombre = nombre,
                    tipo = tipo,
                    rut = rutFormateado,
                    correoContacto = correo
                )
            )
            val orgsActualizadas = organizacionDao.observarTodas().first()
            val creada = orgsActualizadas.find {
                ValidadorRut.formatearRut(it.rut).equals(rutFormateado, ignoreCase = true)
            } ?: orgsActualizadas.maxByOrNull { it.id }

            if (creada != null) {
                val sp = prefs(context)
                val lista = leerSolicitudesPrefs(sp).toMutableList()
                lista.removeAll { it.usuarioId == user.id && it.organizacionId == creada.id }
                lista.add(
                    SolicitudVinculacion(
                        usuarioId = user.id,
                        organizacionId = creada.id,
                        estado = EstadoVinculo.APROBADA,
                        fechaSolicitud = LocalDate.now().format(FORMATO_FECHA)
                    )
                )
                guardarSolicitudesPrefs(sp, lista)
            }

            cargarDesdePersistencia(context)
            cerrarDialogoRegistrarEmpresaUsuario()
            mostrarMensaje("Empresa '$nombre' registrada y asociada correctamente.")
        }
    }

    fun solicitarVinculacionEmpresa(context: Context, usuarioId: Int, organizacionId: Int) {
        if (_uiState.value.empresasSuspendidas.containsKey(organizacionId)) {
            mostrarMensaje("Esta empresa se encuentra suspendida temporalmente por Administración.")
            return
        }
        viewModelScope.launch {
            val sp = prefs(context)
            val lista = leerSolicitudesPrefs(sp).toMutableList()
            lista.removeAll { it.usuarioId == usuarioId && it.organizacionId == organizacionId }
            lista.add(
                SolicitudVinculacion(
                    usuarioId = usuarioId,
                    organizacionId = organizacionId,
                    estado = EstadoVinculo.PENDIENTE,
                    fechaSolicitud = LocalDate.now().format(FORMATO_FECHA),
                    motivoRechazo = null
                )
            )
            guardarSolicitudesPrefs(sp, lista)
            cargarDesdePersistencia(context)
            _uiState.update {
                it.copy(
                    mostrarDialogoVincularEmpresaUsuario = false,
                    mensajeSnackbar = "Solicitud de vinculación enviada."
                )
            }
        }
    }

    // ---------------------------------------------------------
    // AGREGAR CLIENTES Y REVISIÓN FINANCIERA (EJECUTIVO)
    // ---------------------------------------------------------

    fun abrirDialogoAgregarClienteEjecutivo() {
        _uiState.update {
            it.copy(
                mostrarDialogoAgregarClienteEjecutivo = true,
                nuevoClienteNombreInput = "",
                nuevoClienteRutInput = "",
                nuevoClienteCorreoInput = "",
                errorNuevoClienteNombre = null,
                errorNuevoClienteRut = null,
                errorNuevoClienteCorreo = null
            )
        }
    }

    fun cerrarDialogoAgregarClienteEjecutivo() {
        _uiState.update { it.copy(mostrarDialogoAgregarClienteEjecutivo = false) }
    }

    fun onNuevoClienteNombreChange(valor: String) {
        _uiState.update { it.copy(nuevoClienteNombreInput = valor, errorNuevoClienteNombre = null) }
    }

    fun onNuevoClienteRutChange(valor: String) {
        val formateado = ValidadorRut.formatearRut(valor)
        _uiState.update { it.copy(nuevoClienteRutInput = formateado, errorNuevoClienteRut = null) }
    }

    fun onNuevoClienteCorreoChange(valor: String) {
        _uiState.update { it.copy(nuevoClienteCorreoInput = valor, errorNuevoClienteCorreo = null) }
    }

    fun agregarClienteAEmpresaEjecutivo(context: Context) {
        val state = _uiState.value
        val ejecutivo = state.usuarioActual ?: return
        val miOrgId = ejecutivo.empresaId ?: return

        if (state.empresasSuspendidas.containsKey(miOrgId)) {
            mostrarMensaje("Tu empresa está suspendida y no puede agregar nuevos clientes.")
            return
        }

        val nombre = state.nuevoClienteNombreInput.trim()
        val rutFormateado = ValidadorRut.formatearRut(state.nuevoClienteRutInput)
        val correo = state.nuevoClienteCorreoInput.trim()

        var hayError = false
        if (nombre.length < 3) {
            _uiState.update { it.copy(errorNuevoClienteNombre = "Ingresa el nombre completo (mín. 3 car.)") }
            hayError = true
        }
        if (rutFormateado.isBlank() || !ValidadorRut.esRutValido(rutFormateado)) {
            _uiState.update { it.copy(errorNuevoClienteRut = "RUT inválido (ej: 12.345.678-9)") }
            hayError = true
        }
        if (correo.isBlank() || !PatternsCompat.EMAIL_ADDRESS.matcher(correo).matches()) {
            _uiState.update { it.copy(errorNuevoClienteCorreo = "Ingresa un correo electrónico válido") }
            hayError = true
        }
        if (hayError) return

        viewModelScope.launch {
            val usuariosActuales = usuarioDao.observarTodos().first()
            val existente = usuariosActuales.find {
                it.correo.trim().equals(correo, ignoreCase = true) ||
                        ValidadorRut.formatearRut(it.rut).equals(rutFormateado, ignoreCase = true)
            }

            if (existente != null && existente.rol != "USUARIO") {
                _uiState.update {
                    it.copy(errorNuevoClienteCorreo = "El correo o RUT pertenece a una cuenta ${existente.rol}, no a un Cliente.")
                }
                return@launch
            }

            val clienteId: Int = if (existente != null) {
                existente.id
            } else {
                usuarioDao.registrar(
                    UsuarioEntity(
                        nombre = nombre,
                        correo = correo,
                        clave = "123456",
                        rol = "USUARIO",
                        rut = rutFormateado
                    )
                )
                val refrescados = usuarioDao.observarTodos().first()
                refrescados.find { it.correo.trim().equals(correo, ignoreCase = true) }?.id ?: return@launch
            }

            val sp = prefs(context)
            val lista = leerSolicitudesPrefs(sp).toMutableList()
            lista.removeAll { it.usuarioId == clienteId && it.organizacionId == miOrgId }
            lista.add(
                SolicitudVinculacion(
                    usuarioId = clienteId,
                    organizacionId = miOrgId,
                    estado = EstadoVinculo.APROBADA,
                    fechaSolicitud = LocalDate.now().format(FORMATO_FECHA)
                )
            )
            guardarSolicitudesPrefs(sp, lista)

            cargarDesdePersistencia(context)
            cerrarDialogoAgregarClienteEjecutivo()
            mostrarMensaje(
                if (existente != null) {
                    "Cliente ${existente.nombre} vinculado a tu empresa."
                } else {
                    "Cliente $nombre creado y vinculado (clave inicial: 123456)."
                }
            )
        }
    }

    fun abrirRevisionPerfilCliente(solicitud: SolicitudVinculacion) {
        val state = _uiState.value
        val cliente = state.clientesDisponibles.find { it.id == solicitud.usuarioId } ?: return
        val (strikes, detalleStrikes) = calcularStrikesCliente(cliente.id)

        val orgsAprobadas = state.vinculosUsuarioOrgs[cliente.id].orEmpty()
        val historial = state.compromisos.filter { comp ->
            val asig = state.asignacionesCobros[comp.id]
            if (asig != null) asig.usuarioId == cliente.id else orgsAprobadas.contains(comp.organizacionId)
        }

        val pagadosList = historial.filter { it.estado == "PAGADO" }
        val pendientesList = historial.filter { it.estado == "PENDIENTE" }
        val revisionList = historial.filter { it.estado == "REVISION" }
        val rechazadosList = historial.filter { it.estado == "RECHAZADO" }
        val porVencerCount = (pendientesList + rechazadosList).count {
            (calcularDiasParaVencimiento(it.fechaVencimiento) ?: 99) in 0..3
        }
        val deudaTotal = historial.filter { it.estado != "PAGADO" }.sumOf { it.monto }.toInt()
        val cumplimiento = if (historial.isEmpty()) 100 else (pagadosList.size * 100) / historial.size

        val riesgo = when {
            strikes > 0 -> "ALTO ($strikes Strike(s) por mora • Apto para bloqueo de Cliente)"
            rechazadosList.isNotEmpty() -> "MEDIO (Comprobantes rechazados en historial)"
            pendientesList.size >= 2 -> "MODERADO (Múltiples compromisos abiertos)"
            else -> "BAJO (Buen comportamiento histórico)"
        }

        val perfil = PerfilFinancieroCliente(
            cliente = cliente,
            solicitud = solicitud,
            strikesAcumulados = strikes,
            detalleStrikes = detalleStrikes,
            porcentajeCumplimiento = cumplimiento,
            pagados = pagadosList.size,
            montoTotalPagado = pagadosList.sumOf { it.monto }.toInt(),
            pendientes = pendientesList.size,
            enRevision = revisionList.size,
            compromisosPorVencerCount = porVencerCount,
            rechazados = rechazadosList.size,
            montoDeudaActual = deudaTotal,
            nivelRiesgo = riesgo,
            bloqueadoEnEstaEmpresa = solicitud.estado == EstadoVinculo.BLOQUEADA
        )

        _uiState.update {
            it.copy(
                perfilFinancieroEnRevision = perfil,
                motivoRechazoSolicitudInput = "",
                errorMotivoRechazoSolicitud = null
            )
        }
    }

    fun cerrarRevisionPerfilCliente() {
        _uiState.update {
            it.copy(
                perfilFinancieroEnRevision = null,
                motivoRechazoSolicitudInput = "",
                errorMotivoRechazoSolicitud = null
            )
        }
    }

    fun onMotivoRechazoSolicitudChange(texto: String) {
        _uiState.update {
            it.copy(
                motivoRechazoSolicitudInput = texto,
                errorMotivoRechazoSolicitud = null
            )
        }
    }

    fun resolverSolicitudVinculacion(context: Context, aprobar: Boolean) {
        val state = _uiState.value
        val perfil = state.perfilFinancieroEnRevision ?: return
        val motivo = state.motivoRechazoSolicitudInput.trim()

        if (!aprobar && motivo.length < 5) {
            _uiState.update { it.copy(errorMotivoRechazoSolicitud = "Indica un motivo de rechazo (mín. 5 caracteres)") }
            return
        }

        viewModelScope.launch {
            val sp = prefs(context)
            val lista = leerSolicitudesPrefs(sp).toMutableList()
            val idx = lista.indexOfFirst {
                it.usuarioId == perfil.solicitud.usuarioId && it.organizacionId == perfil.solicitud.organizacionId
            }
            if (idx >= 0) {
                lista[idx] = lista[idx].copy(
                    estado = if (aprobar) EstadoVinculo.APROBADA else EstadoVinculo.RECHAZADA,
                    motivoRechazo = if (aprobar) null else motivo
                )
                guardarSolicitudesPrefs(sp, lista)
            }

            cargarDesdePersistencia(context)
            cerrarRevisionPerfilCliente()
            mostrarMensaje(if (aprobar) "Cliente aprobado y vinculado." else "Solicitud rechazada.")
        }
    }

    fun cambiarBloqueoClienteEmpresa(context: Context, bloquear: Boolean) {
        val state = _uiState.value
        val perfil = state.perfilFinancieroEnRevision ?: return
        val motivo = state.motivoRechazoSolicitudInput.trim()

        if (bloquear && motivo.length < 5) {
            _uiState.update { it.copy(errorMotivoRechazoSolicitud = "Ingresa el motivo de bloqueo (mín. 5 caracteres)") }
            return
        }

        viewModelScope.launch {
            val sp = prefs(context)
            val lista = leerSolicitudesPrefs(sp).toMutableList()
            val idx = lista.indexOfFirst {
                it.usuarioId == perfil.solicitud.usuarioId && it.organizacionId == perfil.solicitud.organizacionId
            }
            if (idx >= 0) {
                lista[idx] = lista[idx].copy(
                    estado = if (bloquear) EstadoVinculo.BLOQUEADA else EstadoVinculo.APROBADA,
                    motivoRechazo = if (bloquear) motivo else null
                )
                guardarSolicitudesPrefs(sp, lista)
            }

            cargarDesdePersistencia(context)
            cerrarRevisionPerfilCliente()
            mostrarMensaje(if (bloquear) "Cliente bloqueado en tu empresa." else "Cliente desbloqueado.")
        }
    }

    // ---------------------------------------------------------
    // ADMINISTRACIÓN: SUSPENSIÓN DE EMPRESAS Y BLOQUEO GLOBAL DE CUENTAS
    // ---------------------------------------------------------

    fun abrirDialogoSuspenderEmpresaAdmin(org: OrganizacionEntity) {
        _uiState.update {
            it.copy(
                orgSuspendiendoAdmin = org,
                motivoSuspensionOrgInput = "",
                errorMotivoSuspensionOrg = null
            )
        }
    }

    fun cerrarDialogoSuspenderEmpresaAdmin() {
        _uiState.update {
            it.copy(
                orgSuspendiendoAdmin = null,
                motivoSuspensionOrgInput = "",
                errorMotivoSuspensionOrg = null
            )
        }
    }

    fun onMotivoSuspensionOrgChange(texto: String) {
        _uiState.update {
            it.copy(
                motivoSuspensionOrgInput = texto,
                errorMotivoSuspensionOrg = null
            )
        }
    }

    fun toggleSuspensionEmpresaAdmin(context: Context, suspender: Boolean) {
        val state = _uiState.value
        val org = state.orgSuspendiendoAdmin ?: return
        val motivo = state.motivoSuspensionOrgInput.trim()

        if (suspender && motivo.length < 5) {
            _uiState.update { it.copy(errorMotivoSuspensionOrg = "Ingresa el motivo de suspensión (mín. 5 car.)") }
            return
        }

        viewModelScope.launch {
            val sp = prefs(context)
            val mapa = leerMapaIntStringPrefs(sp, "empresas_suspendidas").toMutableMap()
            if (suspender) {
                mapa[org.id] = motivo
            } else {
                mapa.remove(org.id)
            }
            guardarMapaIntStringPrefs(sp, "empresas_suspendidas", mapa)

            cargarDesdePersistencia(context)
            cerrarDialogoSuspenderEmpresaAdmin()
            mostrarMensaje(
                if (suspender) "Empresa ${org.nombre} suspendida." else "Empresa ${org.nombre} reactivada."
            )
        }
    }

    fun abrirDialogoGestionarUsuarioAdmin(usuario: UsuarioEntity) {
        _uiState.update {
            it.copy(
                usuarioEditandoAdmin = usuario,
                motivoBloqueoAppInput = "",
                errorMotivoBloqueoApp = null
            )
        }
    }

    fun cerrarDialogoGestionarUsuarioAdmin() {
        _uiState.update {
            it.copy(
                usuarioEditandoAdmin = null,
                motivoBloqueoAppInput = "",
                errorMotivoBloqueoApp = null
            )
        }
    }

    fun onMotivoBloqueoAppChange(texto: String) {
        _uiState.update { it.copy(motivoBloqueoAppInput = texto, errorMotivoBloqueoApp = null) }
    }

    fun toggleBloqueoGlobalUsuarioApp(context: Context, usuario: UsuarioEntity, bloquear: Boolean) {
        val motivo = _uiState.value.motivoBloqueoAppInput.trim()
        if (bloquear && motivo.length < 5) {
            _uiState.update { it.copy(errorMotivoBloqueoApp = "Indica el motivo de suspensión global (mín. 5 car.)") }
            return
        }

        viewModelScope.launch {
            val sp = prefs(context)
            val mapa = leerMapaIntStringPrefs(sp, "bloqueos_app").toMutableMap()
            val prefsSeguridadGlobal = context.getSharedPreferences("pagoflex_seguridad_global", Context.MODE_PRIVATE)

            if (bloquear) {
                mapa[usuario.id] = motivo
                prefsSeguridadGlobal.edit().putString("bloqueo_motivo_${usuario.id}", motivo).apply()
            } else {
                mapa.remove(usuario.id)
                prefsSeguridadGlobal.edit().remove("bloqueo_motivo_${usuario.id}").apply()
            }
            guardarMapaIntStringPrefs(sp, "bloqueos_app", mapa)

            cargarDesdePersistencia(context)
            cerrarDialogoGestionarUsuarioAdmin()
            mostrarMensaje(
                if (bloquear) "Cuenta de ${usuario.nombre} suspendida de la app."
                else "Acceso reactivado para ${usuario.nombre}."
            )
        }
    }

    // ---------------------------------------------------------
    // BUZÓN DE SOPORTE
    // ---------------------------------------------------------

    fun abrirDialogoSoporteUsuario() {
        val rol = _uiState.value.usuarioActual?.rol
        val categoriaInicial = if (rol == "EJECUTIVO") {
            "Validación de comprobante"
        } else {
            "Problema con pago"
        }
        _uiState.update {
            it.copy(
                mostrarDialogoSoporteUsuario = true,
                categoriaSoporteInput = categoriaInicial,
                asuntoSoporteInput = "",
                mensajeSoporteInput = "",
                errorAsuntoSoporte = null,
                errorMensajeSoporte = null
            )
        }
    }

    fun cerrarDialogoSoporteUsuario() {
        _uiState.update { it.copy(mostrarDialogoSoporteUsuario = false) }
    }

    fun onCategoriaSoporteChange(categoria: String) {
        _uiState.update { it.copy(categoriaSoporteInput = categoria) }
    }

    fun onAsuntoSoporteChange(asunto: String) {
        _uiState.update { it.copy(asuntoSoporteInput = asunto, errorAsuntoSoporte = null) }
    }

    fun onMensajeSoporteChange(mensaje: String) {
        _uiState.update { it.copy(mensajeSoporteInput = mensaje, errorMensajeSoporte = null) }
    }

    fun enviarTicketSoporte(context: Context) {
        val state = _uiState.value
        val user = state.usuarioActual ?: return
        val asunto = state.asuntoSoporteInput.trim()
        val mensaje = state.mensajeSoporteInput.trim()

        var error = false
        if (asunto.length < 4) {
            _uiState.update { it.copy(errorAsuntoSoporte = "Escribe un asunto breve (mín. 4 caracteres)") }
            error = true
        }
        if (mensaje.length < 10) {
            _uiState.update { it.copy(errorMensajeSoporte = "Detalla tu consulta o problema (mín. 10 caracteres)") }
            error = true
        }
        if (error) return

        val empresaNombre = if (user.rol == "EJECUTIVO") {
            state.organizaciones.find { it.id == user.empresaId }?.nombre
        } else {
            null
        }

        val categoriaFinal = if (user.rol == "EJECUTIVO" && state.categoriaSoporteInput == "Apelación de Strike") {
            "Validación de comprobante"
        } else {
            state.categoriaSoporteInput
        }

        val nuevoTicket = TicketSoporte(
            id = "TCK-${System.currentTimeMillis().toString().takeLast(4)}",
            usuarioId = user.id,
            usuarioNombre = user.nombre,
            usuarioRol = user.rol,
            usuarioRut = user.rut,
            empresaNombre = empresaNombre,
            categoria = categoriaFinal,
            asunto = asunto,
            mensaje = mensaje,
            fechaCreacion = LocalDate.now().format(FORMATO_FECHA),
            estado = "PENDIENTE"
        )

        viewModelScope.launch {
            val sp = prefs(context)
            val lista = listOf(nuevoTicket) + leerTicketsPrefs(sp)
            guardarTicketsPrefs(sp, lista)

            cargarDesdePersistencia(context)
            _uiState.update {
                it.copy(
                    asuntoSoporteInput = "",
                    mensajeSoporteInput = "",
                    mensajeSnackbar = "Mensaje enviado a Administración (${nuevoTicket.id})."
                )
            }
        }
    }

    fun abrirDialogoResponderTicketAdmin(ticket: TicketSoporte) {
        if (ticket.estado == "RESUELTO") {
            mostrarMensaje("Este ticket ya fue resuelto y su respuesta es definitiva.")
            return
        }
        _uiState.update {
            it.copy(
                ticketEnRevisionAdmin = ticket,
                respuestaTicketAdminInput = "",
                errorRespuestaTicketAdmin = null
            )
        }
    }

    fun cerrarDialogoResponderTicketAdmin() {
        _uiState.update {
            it.copy(
                ticketEnRevisionAdmin = null,
                respuestaTicketAdminInput = "",
                errorRespuestaTicketAdmin = null
            )
        }
    }

    fun onRespuestaTicketAdminChange(texto: String) {
        _uiState.update { it.copy(respuestaTicketAdminInput = texto, errorRespuestaTicketAdmin = null) }
    }

    fun resolverTicketSoporteAdmin(context: Context) {
        val state = _uiState.value
        val ticket = state.ticketEnRevisionAdmin ?: return
        if (ticket.estado == "RESUELTO") {
            cerrarDialogoResponderTicketAdmin()
            mostrarMensaje("Este ticket ya fue resuelto y no admite modificaciones.")
            return
        }
        val respuesta = state.respuestaTicketAdminInput.trim()

        if (respuesta.length < 5) {
            _uiState.update { it.copy(errorRespuestaTicketAdmin = "Escribe una respuesta para el usuario (mín. 5 car.)") }
            return
        }

        viewModelScope.launch {
            val sp = prefs(context)
            val lista = leerTicketsPrefs(sp).map {
                if (it.id == ticket.id && it.estado != "RESUELTO") {
                    it.copy(
                        estado = "RESUELTO",
                        respuestaAdmin = respuesta,
                        fechaRespuesta = LocalDate.now().format(FORMATO_FECHA)
                    )
                } else {
                    it
                }
            }
            guardarTicketsPrefs(sp, lista)

            cargarDesdePersistencia(context)
            cerrarDialogoResponderTicketAdmin()
            mostrarMensaje("Respuesta enviada y ticket ${ticket.id} marcado como resuelto.")
        }
    }

    // ---------------------------------------------------------
    // PERFIL, FOTO Y SEGURIDAD (AUTENTICACIÓN Y CONTRASEÑA)
    // ---------------------------------------------------------

    fun abrirDialogoFotoPerfil() {
        _uiState.update { it.copy(mostrarDialogoFotoPerfil = true) }
    }

    fun cerrarDialogoFotoPerfil() {
        _uiState.update { it.copy(mostrarDialogoFotoPerfil = false) }
    }

    fun guardarFotoPerfilDesdeUri(context: Context, uri: Uri) {
        val userId = _uiState.value.usuarioActual?.id ?: return
        viewModelScope.launch {
            val ruta = withContext(Dispatchers.IO) {
                try {
                    val destino = File(context.filesDir, "avatar_user_$userId.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destino).use { output -> input.copyTo(output) }
                    }
                    destino.absolutePath
                } catch (_: Exception) {
                    null
                }
            }
            if (ruta != null) {
                prefs(context).edit().putString("avatar_$userId", ruta).apply()
                _uiState.update {
                    it.copy(
                        avatarPath = ruta,
                        mostrarDialogoFotoPerfil = false,
                        mensajeSnackbar = "Foto de perfil actualizada."
                    )
                }
            } else {
                mostrarMensaje("No se pudo procesar la imagen seleccionada.")
            }
        }
    }

    fun eliminarFotoPerfil(context: Context) {
        val userId = _uiState.value.usuarioActual?.id ?: return
        prefs(context).edit().remove("avatar_$userId").apply()
        _uiState.update {
            it.copy(
                avatarPath = null,
                mostrarDialogoFotoPerfil = false,
                mensajeSnackbar = "Foto de perfil eliminada."
            )
        }
    }

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
        _uiState.update { it.copy(nombrePerfilInput = valor, errorNombrePerfilInput = null) }
    }

    fun guardarDatosPersonales() {
        val user = _uiState.value.usuarioActual ?: return
        val nuevoNombre = _uiState.value.nombrePerfilInput.trim()
        if (nuevoNombre.length < 3) {
            _uiState.update { it.copy(errorNombrePerfilInput = "Ingresa un nombre válido (mín. 3 caracteres)") }
            return
        }
        viewModelScope.launch {
            val actualizado = user.copy(nombre = nuevoNombre)
            usuarioDao.actualizar(actualizado)
            _uiState.update {
                it.copy(
                    usuarioActual = actualizado,
                    mostrarDialogoDatosPersonales = false,
                    mensajeSnackbar = "Nombre actualizado correctamente."
                )
            }
        }
    }

    fun abrirDialogoSeguridad() {
        if (_uiState.value.usuarioActual == null) return
        _uiState.update {
            it.copy(
                mostrarDialogoSeguridad = true,
                claveActualPerfil = "",
                nuevaClavePerfil = "",
                confirmarClavePerfil = "",
                errorClaveActualPerfil = null,
                errorClavePerfil = null,
                errorConfirmarClavePerfil = null
            )
        }
    }

    fun cerrarDialogoSeguridad() {
        _uiState.update { it.copy(mostrarDialogoSeguridad = false) }
    }

    fun onClaveActualPerfilChange(valor: String) {
        _uiState.update { it.copy(claveActualPerfil = valor, errorClaveActualPerfil = null) }
    }

    fun onNuevaClavePerfilChange(valor: String) {
        _uiState.update { it.copy(nuevaClavePerfil = valor, errorClavePerfil = null) }
    }

    fun onConfirmarClavePerfilChange(valor: String) {
        _uiState.update { it.copy(confirmarClavePerfil = valor, errorConfirmarClavePerfil = null) }
    }

    fun actualizarContrasenaSegura() {
        val state = _uiState.value
        val user = state.usuarioActual ?: return

        var hayError = false
        if (state.claveActualPerfil.isBlank()) {
            _uiState.update { it.copy(errorClaveActualPerfil = "Ingresa tu contraseña actual") }
            hayError = true
        }
        if (state.nuevaClavePerfil.length < 6) {
            _uiState.update { it.copy(errorClavePerfil = "La nueva contraseña debe tener al menos 6 caracteres") }
            hayError = true
        } else if (state.nuevaClavePerfil == state.claveActualPerfil) {
            _uiState.update { it.copy(errorClavePerfil = "La nueva contraseña debe ser distinta a la actual") }
            hayError = true
        }
        if (state.nuevaClavePerfil != state.confirmarClavePerfil) {
            _uiState.update { it.copy(errorConfirmarClavePerfil = "Las contraseñas no coinciden") }
            hayError = true
        }
        if (hayError) return

        viewModelScope.launch {
            val valido = user.clave == state.claveActualPerfil ||
                    usuarioDao.login(user.correo, state.claveActualPerfil) != null
            if (!valido) {
                _uiState.update { it.copy(errorClaveActualPerfil = "La contraseña actual es incorrecta") }
                return@launch
            }

            usuarioDao.actualizarClave(user.correo, state.nuevaClavePerfil)
            val userRefrescado = usuarioDao.observarUsuario(user.id).first() ?: user.copy(clave = state.nuevaClavePerfil)

            _uiState.update {
                it.copy(
                    usuarioActual = userRefrescado,
                    mostrarDialogoSeguridad = false,
                    mensajeSnackbar = "Contraseña actualizada de forma segura."
                )
            }
        }
    }

    // ---------------------------------------------------------
    // EXPORTACIÓN CSV
    // ---------------------------------------------------------

    fun exportarReporteCsv(context: Context) {
        viewModelScope.launch {
            val state = _uiState.value
            val user = state.usuarioActual ?: return@launch
            val rol = user.rol

            val contenidoCsv = buildString {
                if (rol == "ADMIN") {
                    appendLine("Empresa,RUT,Tipo,Estado,Ejecutivos,Clientes Activos,Total Emitido CLP,Conciliado CLP,Pendiente CLP,Vencido Mora CLP")
                    state.organizaciones.forEach { org ->
                        val compsOrg = state.compromisos.filter { it.organizacionId == org.id }
                        val emitido = compsOrg.sumOf { it.monto }.toInt()
                        val pagado = compsOrg.filter { it.estado == "PAGADO" }.sumOf { it.monto }.toInt()
                        val vencido = compsOrg.filter {
                            it.estado != "PAGADO" && (calcularDiasParaVencimiento(it.fechaVencimiento) ?: 0) < 0
                        }.sumOf { it.monto }.toInt()
                        val pendiente = emitido - pagado
                        val ejecutivos = state.ejecutivosDisponibles.filter { it.empresaId == org.id }.joinToString(" / ") { it.nombre }.ifBlank { "Sin asignar" }
                        val clientesAct = state.clientesDisponibles.count { state.vinculosUsuarioOrgs[it.id]?.contains(org.id) == true }
                        val estadoOrg = if (state.empresasSuspendidas.containsKey(org.id)) "SUSPENDIDA" else "ACTIVA"
                        appendLine("\"${org.nombre}\",\"${org.rut}\",\"${org.tipo}\",\"$estadoOrg\",\"$ejecutivos\",$clientesAct,$emitido,$pagado,$pendiente,$vencido")
                    }
                } else {
                    appendLine("ID,Empresa,Concepto,Monto CLP,Vencimiento,Estado")
                    val lista = if (rol == "EJECUTIVO") {
                        state.compromisos.filter { it.organizacionId == user.empresaId }
                    } else {
                        val orgs = state.vinculosUsuarioOrgs[user.id].orEmpty()
                        state.compromisos.filter { orgs.contains(it.organizacionId) }
                    }
                    lista.forEach { c ->
                        appendLine("${c.id},\"${c.empresaNombre}\",\"${c.concepto}\",${c.monto.toInt()},\"${c.fechaVencimiento}\",\"${c.estado}\"")
                    }
                }
            }

            val nombreArchivo = "PagoFlex_Informe_${rol}_${System.currentTimeMillis()}.csv"
            val uriGuardado = withContext(Dispatchers.IO) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
                            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(contenidoCsv.toByteArray(Charsets.UTF_8))
                            }
                        }
                        uri
                    } else {
                        val file = File(context.cacheDir, nombreArchivo)
                        file.writeText(contenidoCsv, Charsets.UTF_8)
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (uriGuardado != null) {
                _uiState.update {
                    it.copy(
                        ultimoInformeUri = uriGuardado,
                        mensajeSnackbar = "Informe guardado en Descargas ($nombreArchivo)."
                    )
                }
            } else {
                mostrarMensaje("No se pudo generar el archivo CSV.")
            }
        }
    }

    fun mostrarMensaje(mensaje: String) {
        _uiState.update { it.copy(mensajeSnackbar = mensaje) }
    }

    fun limpiarSnackbar() {
        _uiState.update { it.copy(mensajeSnackbar = null, ultimoInformeUri = null) }
    }

    // ---------------------------------------------------------
    // SERIALIZACIÓN SHAREDPREFERENCES
    // ---------------------------------------------------------

    private fun leerSolicitudesPrefs(sp: android.content.SharedPreferences): List<SolicitudVinculacion> {
        val raw = sp.getString("solicitudes_vinculacion", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SolicitudVinculacion(
                    usuarioId = obj.getInt("usuarioId"),
                    organizacionId = obj.getInt("organizacionId"),
                    estado = EstadoVinculo.valueOf(obj.getString("estado")),
                    fechaSolicitud = obj.getString("fechaSolicitud"),
                    motivoRechazo = if (obj.isNull("motivoRechazo")) null else obj.getString("motivoRechazo")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun guardarSolicitudesPrefs(sp: android.content.SharedPreferences, lista: List<SolicitudVinculacion>) {
        val arr = JSONArray()
        lista.forEach { s ->
            arr.put(JSONObject().apply {
                put("usuarioId", s.usuarioId)
                put("organizacionId", s.organizacionId)
                put("estado", s.estado.name)
                put("fechaSolicitud", s.fechaSolicitud)
                put("motivoRechazo", s.motivoRechazo ?: JSONObject.NULL)
            })
        }
        sp.edit().putString("solicitudes_vinculacion", arr.toString()).apply()
    }

    private fun leerAsignacionesPrefs(sp: android.content.SharedPreferences): Map<Int, AsignacionCobro> {
        val raw = sp.getString("asignaciones_cobros", null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val map = mutableMapOf<Int, AsignacionCobro>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val compId = obj.getInt("compromisoId")
                map[compId] = AsignacionCobro(
                    compromisoId = compId,
                    usuarioId = obj.getInt("usuarioId"),
                    usuarioNombre = obj.getString("usuarioNombre")
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun guardarAsignacionesPrefs(sp: android.content.SharedPreferences, map: Map<Int, AsignacionCobro>) {
        val arr = JSONArray()
        map.values.forEach { a ->
            arr.put(JSONObject().apply {
                put("compromisoId", a.compromisoId)
                put("usuarioId", a.usuarioId)
                put("usuarioNombre", a.usuarioNombre)
            })
        }
        sp.edit().putString("asignaciones_cobros", arr.toString()).apply()
    }

    private fun leerPlantillasRecurrentesPrefs(sp: android.content.SharedPreferences): List<PlantillaCobroRecurrente> {
        val raw = sp.getString("plantillas_recurrentes", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                PlantillaCobroRecurrente(
                    id = obj.getString("id"),
                    organizacionId = obj.getInt("organizacionId"),
                    empresaNombre = obj.getString("empresaNombre"),
                    usuarioId = obj.getInt("usuarioId"),
                    usuarioNombre = obj.getString("usuarioNombre"),
                    concepto = obj.getString("concepto"),
                    monto = obj.getDouble("monto"),
                    diaVencimiento = obj.getInt("diaVencimiento"),
                    mesesProgramados = obj.getInt("mesesProgramados"),
                    fechaRegistro = obj.getString("fechaRegistro")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun guardarPlantillasRecurrentesPrefs(sp: android.content.SharedPreferences, lista: List<PlantillaCobroRecurrente>) {
        val arr = JSONArray()
        lista.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("organizacionId", p.organizacionId)
                put("empresaNombre", p.empresaNombre)
                put("usuarioId", p.usuarioId)
                put("usuarioNombre", p.usuarioNombre)
                put("concepto", p.concepto)
                put("monto", p.monto)
                put("diaVencimiento", p.diaVencimiento)
                put("mesesProgramados", p.mesesProgramados)
                put("fechaRegistro", p.fechaRegistro)
            })
        }
        sp.edit().putString("plantillas_recurrentes", arr.toString()).apply()
    }

    private fun leerMapaIntStringPrefs(sp: android.content.SharedPreferences, key: String): Map<Int, String> {
        val raw = sp.getString(key, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<Int, String>()
            obj.keys().forEach { k ->
                k.toIntOrNull()?.let { id -> map[id] = obj.getString(k) }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun guardarMapaIntStringPrefs(sp: android.content.SharedPreferences, key: String, map: Map<Int, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.toString(), v) }
        sp.edit().putString(key, obj.toString()).apply()
    }

    private fun leerTicketsPrefs(sp: android.content.SharedPreferences): List<TicketSoporte> {
        val raw = sp.getString("tickets_soporte", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TicketSoporte(
                    id = obj.getString("id"),
                    usuarioId = obj.getInt("usuarioId"),
                    usuarioNombre = obj.getString("usuarioNombre"),
                    usuarioRol = obj.getString("usuarioRol"),
                    usuarioRut = obj.optString("usuarioRut", ""),
                    empresaNombre = if (obj.isNull("empresaNombre")) null else obj.getString("empresaNombre"),
                    categoria = obj.getString("categoria"),
                    asunto = obj.getString("asunto"),
                    mensaje = obj.getString("mensaje"),
                    fechaCreacion = obj.getString("fechaCreacion"),
                    estado = obj.getString("estado"),
                    respuestaAdmin = if (obj.isNull("respuestaAdmin")) null else obj.getString("respuestaAdmin"),
                    fechaRespuesta = if (obj.isNull("fechaRespuesta")) null else obj.getString("fechaRespuesta")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun guardarTicketsPrefs(sp: android.content.SharedPreferences, lista: List<TicketSoporte>) {
        val arr = JSONArray()
        lista.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("usuarioId", t.usuarioId)
                put("usuarioNombre", t.usuarioNombre)
                put("usuarioRol", t.usuarioRol)
                put("usuarioRut", t.usuarioRut)
                put("empresaNombre", t.empresaNombre ?: JSONObject.NULL)
                put("categoria", t.categoria)
                put("asunto", t.asunto)
                put("mensaje", t.mensaje)
                put("fechaCreacion", t.fechaCreacion)
                put("estado", t.estado)
                put("respuestaAdmin", t.respuestaAdmin ?: JSONObject.NULL)
                put("fechaRespuesta", t.fechaRespuesta ?: JSONObject.NULL)
            })
        }
        sp.edit().putString("tickets_soporte", arr.toString()).apply()
    }
}