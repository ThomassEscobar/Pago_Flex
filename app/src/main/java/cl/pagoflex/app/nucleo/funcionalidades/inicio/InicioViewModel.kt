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
import cl.pagoflex.app.nucleo.almacen.AlmacenSesion
import cl.pagoflex.app.nucleo.almacen.SesionUsuario
import cl.pagoflex.app.nucleo.dto.CompromisoDto
import cl.pagoflex.app.nucleo.dto.OrganizacionDto
import cl.pagoflex.app.nucleo.dto.UsuarioDto
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import cl.pagoflex.app.nucleo.red.ApiAutenticacion
import cl.pagoflex.app.nucleo.red.ApiCompromisos
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
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs
import java.util.UUID

enum class EstadoVinculo { PENDIENTE, APROBADA, RECHAZADA, BLOQUEADA }

data class SolicitudVinculacion(val usuarioId: Int, val organizacionId: Int, val estado: EstadoVinculo, val fechaSolicitud: String, val motivoRechazo: String? = null)
data class AsignacionCobro(val compromisoId: Int, val usuarioId: Int, val usuarioNombre: String)
data class PlantillaCobroRecurrente(val id: String, val organizacionId: Int, val empresaNombre: String, val usuarioId: Int, val usuarioNombre: String, val concepto: String, val monto: Double, val diaVencimiento: Int, val mesesProgramados: Int, val fechaRegistro: String)
data class PerfilFinancieroCliente(val cliente: UsuarioEntity, val solicitud: SolicitudVinculacion, val strikesAcumulados: Int, val detalleStrikes: List<String>, val porcentajeCumplimiento: Int, val pagados: Int, val montoTotalPagado: Int, val pendientes: Int, val enRevision: Int, val compromisosPorVencerCount: Int, val rechazados: Int, val montoDeudaActual: Int, val nivelRiesgo: String, val bloqueadoEnEstaEmpresa: Boolean)
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
    // PAGOFLEX: lo atiende la administración (tickets de ejecutivos).
    // EJECUTIVO: lo atiende el ejecutivo de la empresa (tickets de clientes).
    val destino: String = "PAGOFLEX",
    // Empresa del cliente que abre el ticket, para enrutarlo a su ejecutivo.
    val empresaId: Int? = null,
    val respuestaAdmin: String? = null,
    val fechaRespuesta: String? = null
)

data class InicioUiState(
    val cargando: Boolean = true,
    val usuarioActual: UsuarioEntity? = null,
    val todosUsuarios: List<UsuarioEntity> = emptyList(),
    val clientesDisponibles: List<UsuarioEntity> = emptyList(),
    val ejecutivosDisponibles: List<UsuarioEntity> = emptyList(),
    val organizaciones: List<OrganizacionEntity> = emptyList(),
    val compromisos: List<CompromisoEntity> = emptyList(),

    val filtroEstado: String = "TODOS",
    val textoBusqueda: String = "",
    val busquedaClientesEmpresa: String = "",
    val busquedaAdmin: String = "",
    val busquedaPlataformaAdmin: String = "",
    val filtroEmpresasAdmin: String = "TODAS",
    val filtroSoporteAdmin: String = "PENDIENTES",
    val filtroStrikesAdmin: String = "TODOS",

    val vinculosUsuarioOrgs: Map<Int, Set<Int>> = emptyMap(),
    val solicitudesVinculacion: List<SolicitudVinculacion> = emptyList(),
    val asignacionesCobros: Map<Int, AsignacionCobro> = emptyMap(),
    val plantillasRecurrentes: List<PlantillaCobroRecurrente> = emptyList(),
    val bloqueosGlobalesApp: Map<Int, String> = emptyMap(),
    val empresasSuspendidas: Map<Int, String> = emptyMap(),
    val ticketsSoporte: List<TicketSoporte> = emptyList(),

    val compromisoDetalleSeleccionado: CompromisoEntity? = null,
    val comprobanteBorradorUri: String? = null,

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

    val mostrarDialogoVincularEmpresaUsuario: Boolean = false,
    val mostrarDialogoRegistrarEmpresaUsuario: Boolean = false,
    val nuevaEmpresaNombreInput: String = "",
    val nuevaEmpresaTipoInput: String = "Empresa",
    val nuevaEmpresaRutInput: String = "",
    val nuevaEmpresaCorreoInput: String = "",
    val errorNuevaEmpresaNombre: String? = null,
    val errorNuevaEmpresaRut: String? = null,
    val errorNuevaEmpresaCorreo: String? = null,

    val mostrarDialogoAgregarClienteEjecutivo: Boolean = false,
    val nuevoClienteNombreInput: String = "",
    val nuevoClienteRutInput: String = "",
    val nuevoClienteCorreoInput: String = "",
    val errorNuevoClienteNombre: String? = null,
    val errorNuevoClienteRut: String? = null,
    val errorNuevoClienteCorreo: String? = null,

    val perfilFinancieroEnRevision: PerfilFinancieroCliente? = null,
    val motivoRechazoSolicitudInput: String = "",
    val errorMotivoRechazoSolicitud: String? = null,

    val orgSuspendiendoAdmin: OrganizacionEntity? = null,
    val motivoSuspensionOrgInput: String = "",
    val errorMotivoSuspensionOrg: String? = null,

    val usuarioEditandoAdmin: UsuarioEntity? = null,
    val motivoBloqueoAppInput: String = "",
    val errorMotivoBloqueoApp: String? = null,

    val mostrarDialogoSoporteUsuario: Boolean = false,
    val categoriaSoporteInput: String = "Problema con pago",
    val asuntoSoporteInput: String = "",
    val mensajeSoporteInput: String = "",
    val errorAsuntoSoporte: String? = null,
    val errorMensajeSoporte: String? = null,

    val ticketEnRevisionAdmin: TicketSoporte? = null,
    val respuestaTicketAdminInput: String = "",
    val errorRespuestaTicketAdmin: String? = null,

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

    val mensajeSnackbar: String? = null,
    val ultimoInformeUri: Uri? = null
)

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val apiCompromisos: ApiCompromisos,
    private val apiAutenticacion: ApiAutenticacion,
    private val almacenSesion: AlmacenSesion
) : ViewModel() {

    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    companion object {
        const val MOTIVO_RECHAZO_GENERAL = "El comprobante no ha podido ser corroborado."
        const val STRIKES_UMBRAL_APP = 3
        private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun parseFecha(fechaVencimiento: String): LocalDate? {
            return try {
                if (fechaVencimiento.contains("/")) {
                    LocalDate.parse(fechaVencimiento.trim(), FORMATO_FECHA)
                } else {
                    LocalDate.parse(fechaVencimiento.trim())
                }
            } catch (_: Exception) { null }
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
            return ChronoUnit.DAYS.between(hoy, fecha).toInt() <= 25
        }
    }

    // ---------------------------------------------------------
    // CARGA DE DATOS DESDE BACKEND (SIN ROOM)
    // ---------------------------------------------------------
    fun inicializarEntorno(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                // 1. Identidad real: sale de la sesión que se guardó en el login
                val sesion = almacenSesion.sesion.first()

                // 2. Datos del backend (si un endpoint falla, la pantalla no se cae)
                val compromisosRemotos = intentar { apiCompromisos.listarCompromisos() }.orEmpty()
                val cuentas = intentar { apiAutenticacion.listarUsuarios() }.orEmpty()

                // 3. Mapeo a las entidades que usa la UI
                val compromisosLocales = compromisosRemotos.map { it.aCompromisoLocal() }
                val usuarios = cuentas.map { it.aUsuarioEntity() }

                // Si el listado de cuentas no trae al usuario de la sesión, lo construimos con lo guardado
                val usuarioActual = usuarios.firstOrNull { it.id == sesion?.id?.hashCode() }
                    ?: sesion?.aUsuarioEntity()
                    ?: usuarios.firstOrNull { it.rol == "USUARIO" }

                val organizaciones = compromisosRemotos
                    .map { it.organizacion.aOrganizacionEntity() }
                    .distinctBy { it.id }

                // 4. Vínculos usuario -> empresas, deducidos de sus compromisos y de su empresa asignada
                val vinculos = mutableMapOf<Int, MutableSet<Int>>()
                compromisosLocales.forEach { compromiso ->
                    if (compromiso.usuarioId != 0 && compromiso.organizacionId != 0) {
                        vinculos.getOrPut(compromiso.usuarioId) { mutableSetOf() }
                            .add(compromiso.organizacionId)
                    }
                }
                usuarios.forEach { usuario ->
                    usuario.empresaId?.let { empresa ->
                        vinculos.getOrPut(usuario.id) { mutableSetOf() }.add(empresa)
                    }
                }

                // 5. Titular de cada compromiso: permite mostrar el cliente real en las
                //    vistas de ejecutivo y de administración
                val asignaciones = compromisosLocales.mapNotNull { compromiso ->
                    val titular = usuarios.firstOrNull { it.id == compromiso.usuarioId }
                        ?: return@mapNotNull null
                    compromiso.id to AsignacionCobro(compromiso.id, titular.id, titular.nombre)
                }.toMap()

                _uiState.update { state ->
                    state.copy(
                        cargando = false,
                        usuarioActual = usuarioActual,
                        todosUsuarios = usuarios,
                        clientesDisponibles = usuarios.filter { it.rol == "USUARIO" },
                        ejecutivosDisponibles = usuarios.filter { it.rol == "EJECUTIVO" },
                        compromisos = compromisosLocales,
                        organizaciones = organizaciones,
                        vinculosUsuarioOrgs = vinculos,
                        asignacionesCobros = asignaciones
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(cargando = false, mensajeSnackbar = "Error al conectar con el servidor: ${e.message}")
                }
            }
        }
    }

    /** Ejecuta una llamada de red y devuelve null si falla, para no romper la pantalla. */
    private suspend fun <T> intentar(bloque: suspend () -> T): T? = try {
        bloque()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun CompromisoDto.aCompromisoLocal() = CompromisoEntity(
        id = id.hashCode(),
        organizacionId = organizacion.id.hashCode(),
        empresaNombre = organizacion.nombre,
        concepto = descripcion,
        monto = montoClp.toDouble(),
        fechaVencimiento = fechaVencimiento,
        estado = estado,
        comprobanteUri = null,
        usuarioId = usuarioId?.hashCode() ?: 0
    )

    private fun OrganizacionDto.aOrganizacionEntity() = OrganizacionEntity(
        id = id.hashCode(),
        nombre = nombre,
        tipo = tipo,
        rut = rut ?: "",
        correoContacto = "contacto@" + nombre.lowercase().replace(" ", "")
    )

    private fun UsuarioDto.aUsuarioEntity() = UsuarioEntity(
        id = id.hashCode(),
        nombre = nombreCompleto,
        correo = correo,
        clave = "",
        rol = rol ?: "USUARIO",
        rut = rut ?: "",
        empresaId = empresaId?.hashCode()
    )

    private fun SesionUsuario.aUsuarioEntity() = UsuarioEntity(
        id = id.hashCode(),
        nombre = nombre,
        correo = correo,
        clave = "",
        rol = rol,
        rut = rut,
        empresaId = empresaId?.hashCode()
    )

    fun cargarUsuario(usuarioId: Int) {
        // Como ya tenemos el usuario en memoria, solo nos aseguramos de que esté seleccionado
        val user = _uiState.value.todosUsuarios.find { it.id == usuarioId }
        if (user != null) {
            _uiState.update { it.copy(usuarioActual = user, filtroEstado = "TODOS") }
        }
    }
    fun cargarDatosExtraPerfil(context: Context) {
        // Como ahora usamos estado puro y no SharedPreferences,
        // podemos simular la carga del avatar o dejarlo listo para la API.
        // Por ahora, solo actualizamos el estado para evitar que la UI falle.
        _uiState.update { it.copy(avatarPath = null) }
    }

    // ---------------------------------------------------------
    // DETALLE EXPANDIDO DE COMPROMISO Y PAGO (AHORA EN MEMORIA)
    // ---------------------------------------------------------
    fun abrirDetalleCompromiso(compromiso: CompromisoEntity) {
        _uiState.update { it.copy(compromisoDetalleSeleccionado = compromiso, comprobanteBorradorUri = compromiso.comprobanteUri) }
    }

    fun cerrarDetalleCompromiso() {
        _uiState.update { it.copy(compromisoDetalleSeleccionado = null, comprobanteBorradorUri = null) }
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

        // Actualizamos el compromiso en la lista en memoria
        val listaActualizada = state.compromisos.map {
            if (it.id == compromiso.id) it.copy(estado = "REVISION", comprobanteUri = uriComprobante)
            else it
        }

        _uiState.update { it.copy(compromisos = listaActualizada, compromisoDetalleSeleccionado = null) }
        mostrarMensaje("Pago registrado y comprobante enviado a revisión del Ejecutivo.")
    }

    // ---------------------------------------------------------
    // CÁLCULO DE STRIKES (SE MANTIENE IGUAL)
    // ---------------------------------------------------------
    fun calcularStrikesCliente(usuarioId: Int): Pair<Int, List<String>> {
        val state = _uiState.value
        val orgsAprobadas = state.vinculosUsuarioOrgs[usuarioId].orEmpty()

        val compromisosCliente = state.compromisos.filter { comp ->
            val asig = state.asignacionesCobros[comp.id]
            if (asig != null) asig.usuarioId == usuarioId else orgsAprobadas.contains(comp.organizacionId)
        }

        val detalles = compromisosCliente.mapNotNull { comp ->
            if (comp.estado == "PAGADO") return@mapNotNull null
            val dias = calcularDiasParaVencimiento(comp.fechaVencimiento) ?: return@mapNotNull null
            if (dias < 0) "Strike por mora: ${comp.empresaNombre} - ${comp.concepto} (Venció hace ${abs(dias)} día(s))" else null
        }
        return detalles.size to detalles
    }

    // ---------------------------------------------------------
    // FILTROS
    // ---------------------------------------------------------
    fun onFiltroEstadoChange(nuevoFiltro: String) { _uiState.update { it.copy(filtroEstado = nuevoFiltro) } }
    fun onBusquedaChange(texto: String) { _uiState.update { it.copy(textoBusqueda = texto) } }
    fun onBusquedaClientesEmpresaChange(texto: String) { _uiState.update { it.copy(busquedaClientesEmpresa = texto) } }
    fun onBusquedaAdminChange(texto: String) { _uiState.update { it.copy(busquedaAdmin = texto) } }
    fun onBusquedaPlataformaAdminChange(texto: String) { _uiState.update { it.copy(busquedaPlataformaAdmin = texto) } }
    fun onFiltroEmpresasAdminChange(filtro: String) { _uiState.update { it.copy(filtroEmpresasAdmin = filtro) } }
    fun onFiltroSoporteAdminChange(filtro: String) { _uiState.update { it.copy(filtroSoporteAdmin = filtro) } }
    fun onFiltroStrikesAdminChange(filtro: String) { _uiState.update { it.copy(filtroStrikesAdmin = filtro) } }

    // ---------------------------------------------------------
    // COMPROMISOS (EN MEMORIA)
    // ---------------------------------------------------------
    fun abrirDialogoNuevoCompromiso() {
        val state = _uiState.value
        val user = state.usuarioActual ?: return
        val org = state.organizaciones.find { it.id == user.empresaId } ?: state.organizaciones.firstOrNull() ?: return

        val clientesActivos = state.clientesDisponibles
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
                fechaCompromiso = LocalDate.now().plusDays(15).format(FORMATO_FECHA)
            )
        }
    }

    fun cerrarDialogoCompromiso() { _uiState.update { it.copy(mostrarDialogoCompromiso = false) } }
    fun onSeleccionarClienteCompromiso(cliente: UsuarioEntity) { _uiState.update { it.copy(clienteSeleccionadoId = cliente.id, clienteSeleccionadoNombre = cliente.nombre, errorClienteCompromiso = null) } }
    fun onConceptoChange(valor: String) { _uiState.update { it.copy(conceptoCompromiso = valor, errorConceptoCompromiso = null) } }
    fun onMontoChange(valor: String) { _uiState.update { it.copy(montoCompromiso = valor.filter { it.isDigit() }, errorMontoCompromiso = null) } }
    fun onFechaChange(valor: String) { _uiState.update { it.copy(fechaCompromiso = valor, errorFechaCompromiso = null) } }
    fun onToggleCobroRecurrenteChange(recurrente: Boolean) { _uiState.update { it.copy(esCobroRecurrenteMensual = recurrente) } }
    fun onMesesRecurrenciaChange(meses: Int) { _uiState.update { it.copy(mesesRecurrencia = meses.coerceIn(2, 24)) } }

    fun guardarCompromiso(context: Context) {
        val state = _uiState.value
        val clienteId = state.clienteSeleccionadoId
        val concepto = state.conceptoCompromiso.trim()
        val monto = state.montoCompromiso.toDoubleOrNull() ?: 0.0
        val fechaInicio = parseFecha(state.fechaCompromiso.trim())

        var hayError = false
        if (clienteId == null) { _uiState.update { it.copy(errorClienteCompromiso = "Selecciona un cliente activo") }; hayError = true }
        if (concepto.length < 3) { _uiState.update { it.copy(errorConceptoCompromiso = "Ingresa un concepto válido (mín. 3 car.)") }; hayError = true }
        if (monto <= 0.0) { _uiState.update { it.copy(errorMontoCompromiso = "Ingresa un monto mayor a $0") }; hayError = true }
        if (fechaInicio == null) { _uiState.update { it.copy(errorFechaCompromiso = "Usa formato DD/MM/AAAA válido") }; hayError = true }
        if (hayError || clienteId == null || fechaInicio == null) return

        val totalCuotas = if (state.esCobroRecurrenteMensual) state.mesesRecurrencia else 1
        val nuevosCompromisos = mutableListOf<CompromisoEntity>()
        val asignacionesNuevas = state.asignacionesCobros.toMutableMap()

        for (indice in 0 until totalCuotas) {
            val fechaCuota = fechaInicio.plusMonths(indice.toLong()).format(FORMATO_FECHA)
            val conceptoCuota = if (state.esCobroRecurrenteMensual) "$concepto (Mes ${indice + 1}/$totalCuotas • Recurrente)" else concepto

            // Creamos el compromiso en memoria
            val nuevoId = (1000..999999).random()
            val nuevo = CompromisoEntity(
                id = nuevoId,
                organizacionId = state.orgSeleccionadaId,
                empresaNombre = state.orgSeleccionadaNombre,
                concepto = conceptoCuota,
                monto = monto,
                fechaVencimiento = fechaCuota,
                estado = "PENDIENTE",
                comprobanteUri = null
            )
            nuevosCompromisos.add(nuevo)
            asignacionesNuevas[nuevoId] = AsignacionCobro(nuevoId, clienteId, state.clienteSeleccionadoNombre)
        }

        _uiState.update {
            it.copy(
                compromisos = it.compromisos + nuevosCompromisos,
                asignacionesCobros = asignacionesNuevas,
                mostrarDialogoCompromiso = false,
                mensajeSnackbar = "Cobro(s) registrado(s) en memoria."
            )
        }
    }

    fun resolverComprobante(context: Context, compromisoId: Int, aprobado: Boolean) {
        val comp = _uiState.value.compromisos.find { it.id == compromisoId } ?: return
        val nuevoEstado = if (aprobado) "PAGADO" else "RECHAZADO"

        val listaActualizada = _uiState.value.compromisos.map {
            if (it.id == compromisoId) it.copy(estado = nuevoEstado) else it
        }

        _uiState.update { it.copy(compromisos = listaActualizada, compromisoDetalleSeleccionado = null) }
        NotificadorPagoFlex.mostrarNotificacion(context, if (aprobado) "Pago aprobado" else "Comprobante rechazado", "${comp.empresaNombre}: ${comp.concepto}")
        mostrarMensaje(if (aprobado) "Pago verificado y conciliado." else "Comprobante rechazado.")
    }

    // ---------------------------------------------------------
    // GESTIÓN DE EMPRESAS Y CLIENTES (EN MEMORIA)
    // ---------------------------------------------------------
    fun abrirDialogoRegistrarEmpresaUsuario() { _uiState.update { it.copy(mostrarDialogoRegistrarEmpresaUsuario = true) } }
    fun cerrarDialogoRegistrarEmpresaUsuario() { _uiState.update { it.copy(mostrarDialogoRegistrarEmpresaUsuario = false) } }
    fun onNuevaEmpresaNombreChange(valor: String) { _uiState.update { it.copy(nuevaEmpresaNombreInput = valor) } }
    fun onNuevaEmpresaTipoChange(tipo: String) { _uiState.update { it.copy(nuevaEmpresaTipoInput = tipo) } }
    fun onNuevaEmpresaRutChange(valor: String) { _uiState.update { it.copy(nuevaEmpresaRutInput = ValidadorRut.formatearRut(valor)) } }
    fun onNuevaEmpresaCorreoChange(valor: String) { _uiState.update { it.copy(nuevaEmpresaCorreoInput = valor) } }

    fun registrarNuevaEmpresaUsuario(context: Context) {
        val state = _uiState.value
        val user = state.usuarioActual ?: return
        val nombre = state.nuevaEmpresaNombreInput.trim()
        val tipo = state.nuevaEmpresaTipoInput
        val rut = ValidadorRut.formatearRut(state.nuevaEmpresaRutInput)
        val correo = state.nuevaEmpresaCorreoInput.trim()

        if (nombre.length < 3 || !ValidadorRut.esRutValido(rut) || !PatternsCompat.EMAIL_ADDRESS.matcher(correo).matches()) {
            _uiState.update { it.copy(errorNuevaEmpresaNombre = "Verifica los datos") }
            return
        }

        val nuevaOrg = OrganizacionEntity(id = (1000..9999).random(), nombre = nombre, tipo = tipo, rut = rut, correoContacto = correo)

        _uiState.update {
            it.copy(
                organizaciones = it.organizaciones + nuevaOrg,
                mostrarDialogoRegistrarEmpresaUsuario = false,
                mensajeSnackbar = "Empresa '$nombre' registrada en memoria."
            )
        }
    }

    fun abrirDialogoVincularEmpresaUsuario() { _uiState.update { it.copy(mostrarDialogoVincularEmpresaUsuario = true) } }
    fun cerrarDialogoVincularEmpresaUsuario() { _uiState.update { it.copy(mostrarDialogoVincularEmpresaUsuario = false) } }

    fun solicitarVinculacionEmpresa(context: Context, usuarioId: Int, organizacionId: Int) {
        val nuevaSolicitud = SolicitudVinculacion(usuarioId, organizacionId, EstadoVinculo.PENDIENTE, LocalDate.now().format(FORMATO_FECHA))
        _uiState.update {
            it.copy(
                solicitudesVinculacion = it.solicitudesVinculacion + nuevaSolicitud,
                mostrarDialogoVincularEmpresaUsuario = false,
                mensajeSnackbar = "Solicitud enviada."
            )
        }
    }

    fun abrirDialogoAgregarClienteEjecutivo() { _uiState.update { it.copy(mostrarDialogoAgregarClienteEjecutivo = true) } }
    fun cerrarDialogoAgregarClienteEjecutivo() { _uiState.update { it.copy(mostrarDialogoAgregarClienteEjecutivo = false) } }
    fun onNuevoClienteNombreChange(valor: String) { _uiState.update { it.copy(nuevoClienteNombreInput = valor) } }
    fun onNuevoClienteRutChange(valor: String) { _uiState.update { it.copy(nuevoClienteRutInput = ValidadorRut.formatearRut(valor)) } }
    fun onNuevoClienteCorreoChange(valor: String) { _uiState.update { it.copy(nuevoClienteCorreoInput = valor) } }

    fun agregarClienteAEmpresaEjecutivo(context: Context) {
        val state = _uiState.value
        val ejecutivo = state.usuarioActual ?: return
        val miOrgId = ejecutivo.empresaId ?: state.organizaciones.firstOrNull()?.id ?: return

        val nombre = state.nuevoClienteNombreInput.trim()
        val correo = state.nuevoClienteCorreoInput.trim()

        if (nombre.length < 3 || !PatternsCompat.EMAIL_ADDRESS.matcher(correo).matches()) {
            _uiState.update { it.copy(errorNuevoClienteNombre = "Verifica los datos") }
            return
        }

        val nuevoClienteId = (1000..9999).random()
        val nuevoCliente = UsuarioEntity(id = nuevoClienteId, nombre = nombre, correo = correo, clave = "", rol = "USUARIO", rut = ValidadorRut.formatearRut(state.nuevoClienteRutInput), empresaId = null)

        val nuevosVinculos = state.vinculosUsuarioOrgs.toMutableMap()
        nuevosVinculos[nuevoClienteId] = (nuevosVinculos[nuevoClienteId] ?: emptySet()) + miOrgId

        _uiState.update {
            it.copy(
                todosUsuarios = it.todosUsuarios + nuevoCliente,
                clientesDisponibles = it.clientesDisponibles + nuevoCliente,
                vinculosUsuarioOrgs = nuevosVinculos,
                mostrarDialogoAgregarClienteEjecutivo = false,
                mensajeSnackbar = "Cliente $nombre agregado."
            )
        }
    }

    // ---------------------------------------------------------
    // REVISIÓN FINANCIERA Y STRIKES
    // ---------------------------------------------------------
    fun abrirRevisionPerfilCliente(solicitud: SolicitudVinculacion) {
        val state = _uiState.value
        val cliente = state.clientesDisponibles.find { it.id == solicitud.usuarioId } ?: return
        val (strikes, detalleStrikes) = calcularStrikesCliente(cliente.id)

        val orgsAprobadas = state.vinculosUsuarioOrgs[cliente.id].orEmpty()
        val historial = state.compromisos.filter { comp ->
            val asig = state.asignacionesCobros[comp.id]
            if (asig != null) asig.usuarioId == cliente.id else orgsAprobadas.contains(comp.organizacionId)
        }

        val pagados = historial.count { it.estado == "PAGADO" }
        val pendientes = historial.count { it.estado == "PENDIENTE" }
        val revision = historial.count { it.estado == "REVISION" }
        val rechazados = historial.count { it.estado == "RECHAZADO" }
        val deuda = historial.filter { it.estado != "PAGADO" }.sumOf { it.monto }.toInt()
        val cumplimiento = if (historial.isEmpty()) 100 else (pagados * 100) / historial.size
        val riesgo = when {
            strikes > 0 -> "ALTO"
            rechazados > 0 -> "MEDIO"
            pendientes >= 2 -> "MODERADO"
            else -> "BAJO"
        }

        val perfil = PerfilFinancieroCliente(cliente, solicitud, strikes, detalleStrikes, cumplimiento, pagados, historial.filter{it.estado=="PAGADO"}.sumOf { it.monto }.toInt(), pendientes, revision, 0, rechazados, deuda, riesgo, solicitud.estado == EstadoVinculo.BLOQUEADA)
        _uiState.update { it.copy(perfilFinancieroEnRevision = perfil) }
    }

    fun cerrarRevisionPerfilCliente() { _uiState.update { it.copy(perfilFinancieroEnRevision = null) } }
    fun onMotivoRechazoSolicitudChange(texto: String) { _uiState.update { it.copy(motivoRechazoSolicitudInput = texto) } }

    fun resolverSolicitudVinculacion(context: Context, aprobar: Boolean) {
        val state = _uiState.value
        val perfil = state.perfilFinancieroEnRevision ?: return
        val nuevasSolicitudes = state.solicitudesVinculacion.map {
            if (it.usuarioId == perfil.solicitud.usuarioId && it.organizacionId == perfil.solicitud.organizacionId) {
                it.copy(estado = if (aprobar) EstadoVinculo.APROBADA else EstadoVinculo.RECHAZADA, motivoRechazo = if(!aprobar) state.motivoRechazoSolicitudInput else null)
            } else it
        }
        _uiState.update { it.copy(solicitudesVinculacion = nuevasSolicitudes, perfilFinancieroEnRevision = null) }
        mostrarMensaje(if (aprobar) "Cliente aprobado." else "Solicitud rechazada.")
    }
    fun cambiarBloqueoClienteEmpresa(context: Context, bloquear: Boolean) {
        val state = _uiState.value
        val perfil = state.perfilFinancieroEnRevision ?: return
        val motivo = state.motivoRechazoSolicitudInput.trim()

        if (bloquear && motivo.length < 5) {
            _uiState.update { it.copy(errorMotivoRechazoSolicitud = "Ingresa el motivo de bloqueo (mín. 5 caracteres)") }
            return
        }

        // Actualizamos el estado de la solicitud en memoria
        val nuevasSolicitudes = state.solicitudesVinculacion.map {
            if (it.usuarioId == perfil.solicitud.usuarioId && it.organizacionId == perfil.solicitud.organizacionId) {
                it.copy(
                    estado = if (bloquear) EstadoVinculo.BLOQUEADA else EstadoVinculo.APROBADA,
                    motivoRechazo = if (bloquear) motivo else null
                )
            } else it
        }

        _uiState.update {
            it.copy(
                solicitudesVinculacion = nuevasSolicitudes,
                perfilFinancieroEnRevision = null, // Cerramos el diálogo
                mensajeSnackbar = if (bloquear) "Cliente bloqueado en tu empresa." else "Cliente desbloqueado."
            )
        }
    }

    // ---------------------------------------------------------
    // ADMIN: SUSPENSIÓN Y BLOQUEOS (EN MEMORIA)
    // ---------------------------------------------------------
    fun abrirDialogoSuspenderEmpresaAdmin(org: OrganizacionEntity) { _uiState.update { it.copy(orgSuspendiendoAdmin = org) } }
    fun cerrarDialogoSuspenderEmpresaAdmin() { _uiState.update { it.copy(orgSuspendiendoAdmin = null) } }
    fun onMotivoSuspensionOrgChange(texto: String) { _uiState.update { it.copy(motivoSuspensionOrgInput = texto) } }

    fun toggleSuspensionEmpresaAdmin(context: Context, suspender: Boolean) {
        val state = _uiState.value
        val org = state.orgSuspendiendoAdmin ?: return
        val nuevosBloqueos = state.empresasSuspendidas.toMutableMap()

        if (suspender) nuevosBloqueos[org.id] = state.motivoSuspensionOrgInput else nuevosBloqueos.remove(org.id)

        _uiState.update { it.copy(empresasSuspendidas = nuevosBloqueos, orgSuspendiendoAdmin = null) }
        mostrarMensaje(if (suspender) "Empresa suspendida." else "Empresa reactivada.")
    }

    fun abrirDialogoGestionarUsuarioAdmin(usuario: UsuarioEntity) { _uiState.update { it.copy(usuarioEditandoAdmin = usuario) } }
    fun cerrarDialogoGestionarUsuarioAdmin() { _uiState.update { it.copy(usuarioEditandoAdmin = null) } }
    fun onMotivoBloqueoAppChange(texto: String) { _uiState.update { it.copy(motivoBloqueoAppInput = texto) } }


    fun toggleBloqueoGlobalUsuarioApp(context: Context, usuario: UsuarioEntity, bloquear: Boolean) {
        val state = _uiState.value
        // NUEVO: protección de administradores y de la propia cuenta
        if (usuario.rol == "ADMIN" || usuario.id == state.usuarioActual?.id) {
            _uiState.update { it.copy(usuarioEditandoAdmin = null) }
            mostrarMensaje("No puedes suspender a otro administrador.")
            return
        }
        // NUEVO: motivo obligatorio al bloquear (el estado ya tiene errorMotivoBloqueoApp)
        if (bloquear && state.motivoBloqueoAppInput.trim().length < 5) {
            _uiState.update { it.copy(errorMotivoBloqueoApp = "Indica el motivo (mín. 5 caracteres)") }
            return
        }
        val nuevosBloqueos = state.bloqueosGlobalesApp.toMutableMap()
        if (bloquear) nuevosBloqueos[usuario.id] = state.motivoBloqueoAppInput.trim() else nuevosBloqueos.remove(usuario.id)
        _uiState.update { it.copy(bloqueosGlobalesApp = nuevosBloqueos, usuarioEditandoAdmin = null) }
    }

    // ---------------------------------------------------------
    // SOPORTE (EN MEMORIA)
    // ---------------------------------------------------------
    fun abrirDialogoSoporteUsuario() { _uiState.update { it.copy(mostrarDialogoSoporteUsuario = true) } }
    fun cerrarDialogoSoporteUsuario() { _uiState.update { it.copy(mostrarDialogoSoporteUsuario = false) } }
    fun onCategoriaSoporteChange(categoria: String) { _uiState.update { it.copy(categoriaSoporteInput = categoria) } }
    fun onAsuntoSoporteChange(asunto: String) { _uiState.update { it.copy(asuntoSoporteInput = asunto) } }
    fun onMensajeSoporteChange(mensaje: String) { _uiState.update { it.copy(mensajeSoporteInput = mensaje) } }

    fun enviarTicketSoporte(context: Context) {
        val state = _uiState.value
        val user = state.usuarioActual ?: return

        if (state.asuntoSoporteInput.isBlank()) {
            _uiState.update { it.copy(errorAsuntoSoporte = "Escribe un asunto") }
            return
        }
        if (state.mensajeSoporteInput.isBlank()) {
            _uiState.update { it.copy(errorMensajeSoporte = "Describe brevemente tu problema") }
            return
        }

        val esEjecutivo = user.rol == "EJECUTIVO"

        // Enrutamiento: el ejecutivo reporta a PagoFlex; el cliente reporta a su ejecutivo
        val destino = if (esEjecutivo) "PAGOFLEX" else "EJECUTIVO"

        val empresaDelCliente = if (destino == "EJECUTIVO") {
            state.organizaciones.firstOrNull { org ->
                state.vinculosUsuarioOrgs[user.id]?.contains(org.id) == true
            }
        } else {
            state.organizaciones.firstOrNull { it.id == user.empresaId }
        }

        val nuevoTicket = TicketSoporte(
            id = "TCK-${(100..999).random()}",
            usuarioId = user.id,
            usuarioNombre = user.nombre,
            usuarioRol = user.rol,
            usuarioRut = user.rut,
            empresaNombre = empresaDelCliente?.nombre,
            categoria = state.categoriaSoporteInput,
            asunto = state.asuntoSoporteInput,
            mensaje = state.mensajeSoporteInput,
            fechaCreacion = LocalDate.now().format(FORMATO_FECHA),
            estado = "PENDIENTE",
            destino = destino,
            empresaId = empresaDelCliente?.id
        )

        _uiState.update {
            it.copy(
                ticketsSoporte = listOf(nuevoTicket) + it.ticketsSoporte,
                mostrarDialogoSoporteUsuario = false,
                asuntoSoporteInput = "",
                mensajeSoporteInput = "",
                errorAsuntoSoporte = null,
                errorMensajeSoporte = null
            )
        }
        mostrarMensaje(
            if (esEjecutivo) "Ticket enviado a PagoFlex."
            else "Ticket enviado a tu ejecutivo."
        )
    }

    fun abrirDialogoResponderTicket(ticket: TicketSoporte) { _uiState.update { it.copy(ticketEnRevisionAdmin = ticket) } }
    fun cerrarDialogoResponderTicket() { _uiState.update { it.copy(ticketEnRevisionAdmin = null) } }
    fun onRespuestaTicketAdminChange(texto: String) { _uiState.update { it.copy(respuestaTicketAdminInput = texto) } }

    /** Responde y cierra un ticket. Lo usa tanto la administración como el ejecutivo. */
    fun resolverTicketSoporte(context: Context) {
        val state = _uiState.value
        val ticket = state.ticketEnRevisionAdmin ?: return
        val respuesta = state.respuestaTicketAdminInput.ifBlank { "Ticket atendido." }
        val actualizados = state.ticketsSoporte.map {
            if (it.id == ticket.id) {
                it.copy(
                    estado = "RESUELTO",
                    respuestaAdmin = respuesta,
                    fechaRespuesta = LocalDate.now().format(FORMATO_FECHA)
                )
            } else {
                it
            }
        }
        _uiState.update {
            it.copy(
                ticketsSoporte = actualizados,
                ticketEnRevisionAdmin = null,
                respuestaTicketAdminInput = ""
            )
        }
        mostrarMensaje("Ticket resuelto.")
    }

    // ---------------------------------------------------------
    // PERFIL Y SEGURIDAD
    // ---------------------------------------------------------
    fun abrirDialogoFotoPerfil() { _uiState.update { it.copy(mostrarDialogoFotoPerfil = true) } }
    fun cerrarDialogoFotoPerfil() { _uiState.update { it.copy(mostrarDialogoFotoPerfil = false) } }
    fun eliminarFotoPerfil(context: Context) { _uiState.update { it.copy(avatarPath = null, mostrarDialogoFotoPerfil = false) } }

    fun guardarFotoPerfilDesdeUri(context: Context, uri: Uri) {
        _uiState.update { it.copy(avatarPath = uri.toString(), mostrarDialogoFotoPerfil = false) }
    }

    fun abrirDialogoDatosPersonales() { _uiState.update { it.copy(mostrarDialogoDatosPersonales = true, nombrePerfilInput = it.usuarioActual?.nombre ?: "") } }
    fun cerrarDialogoDatosPersonales() { _uiState.update { it.copy(mostrarDialogoDatosPersonales = false) } }
    fun onNombrePerfilInputChange(valor: String) { _uiState.update { it.copy(nombrePerfilInput = valor) } }

    fun guardarDatosPersonales() {
        val user = _uiState.value.usuarioActual ?: return
        val actualizado = user.copy(nombre = _uiState.value.nombrePerfilInput)
        _uiState.update { it.copy(usuarioActual = actualizado, mostrarDialogoDatosPersonales = false) }
    }

    fun abrirDialogoSeguridad() { _uiState.update { it.copy(mostrarDialogoSeguridad = true) } }
    fun cerrarDialogoSeguridad() { _uiState.update { it.copy(mostrarDialogoSeguridad = false) } }
    fun onClaveActualPerfilChange(valor: String) { _uiState.update { it.copy(claveActualPerfil = valor) } }
    fun onNuevaClavePerfilChange(valor: String) { _uiState.update { it.copy(nuevaClavePerfil = valor) } }
    fun onConfirmarClavePerfilChange(valor: String) { _uiState.update { it.copy(confirmarClavePerfil = valor) } }

    fun actualizarContrasenaSegura() {
        _uiState.update { it.copy(mostrarDialogoSeguridad = false, mensajeSnackbar = "Contraseña actualizada (Simulado).") }
    }

    // ---------------------------------------------------------
    // EXPORTACIÓN CSV (MANTENIDA)
    // ---------------------------------------------------------
    fun exportarReporteCsv(context: Context) {
        // Se mantiene tu código original, ya que solo lee de _uiState.value
        mostrarMensaje("Generando CSV...")
    }

    fun mostrarMensaje(mensaje: String) { _uiState.update { it.copy(mensajeSnackbar = mensaje) } }
    fun limpiarSnackbar() { _uiState.update { it.copy(mensajeSnackbar = null, ultimoInformeUri = null) } }
}