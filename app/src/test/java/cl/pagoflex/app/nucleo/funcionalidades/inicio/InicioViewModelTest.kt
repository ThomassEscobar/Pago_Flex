package cl.pagoflex.app.nucleo.funcionalidades.inicio

import android.content.Context
import android.content.SharedPreferences
import cl.pagoflex.app.nucleo.local.dao.CompromisoDao
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.dao.UsuarioDao
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private lateinit var usuarioDao: UsuarioDao
    private lateinit var organizacionDao: OrganizacionDao
    private lateinit var compromisoDao: CompromisoDao
    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var viewModel: InicioViewModel

    private val orgMirador = OrganizacionEntity(
        id = 1,
        nombre = "Edificio Mirador",
        tipo = "Edificio",
        rut = "76.123.456-0",
        correoContacto = "contacto@mirador.cl"
    )

    private val clienteMatias = UsuarioEntity(
        id = 1,
        nombre = "Matías Pavez",
        correo = "mat.pavezb@gmail.com",
        clave = "123456",
        rol = "USUARIO",
        rut = "12.345.678-5"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        usuarioDao = mockk(relaxed = true)
        organizacionDao = mockk(relaxed = true)
        compromisoDao = mockk(relaxed = true)
        context = mockk(relaxed = true)

        val storage = mutableMapOf<String, Any?>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putString(any(), any()) } answers {
            storage[firstArg()] = secondArg<String?>()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            storage[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { editor.remove(any()) } answers {
            storage.remove(firstArg())
            editor
        }

        sharedPrefs = mockk(relaxed = true)
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.getString(any(), any()) } answers {
            storage[firstArg()] as? String ?: secondArg()
        }
        every { sharedPrefs.getBoolean(any(), any()) } answers {
            storage[firstArg()] as? Boolean ?: secondArg()
        }
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs

        every { usuarioDao.observarTodos() } returns flowOf(listOf(clienteMatias))
        every { organizacionDao.observarTodas() } returns flowOf(listOf(orgMirador))

        viewModel = InicioViewModel(usuarioDao, organizacionDao, compromisoDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `esCompromisoExigibleAhora distingue cuota vigente de cuotas recurrentes futuras`() {
        val hoy = LocalDate.now()
        val cuotaMesActual = CompromisoEntity(
            id = 1,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Mensualidad (Mes 1/12 • Recurrente)",
            monto = 250000.0,
            fechaVencimiento = hoy.plusDays(10).format(formatoFecha),
            estado = "PENDIENTE"
        )
        val cuotaFutura = CompromisoEntity(
            id = 12,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Mensualidad (Mes 12/12 • Recurrente)",
            monto = 250000.0,
            fechaVencimiento = hoy.plusMonths(11).format(formatoFecha),
            estado = "PENDIENTE"
        )
        val cobroUnico = CompromisoEntity(
            id = 20,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Multa reparación",
            monto = 85000.0,
            fechaVencimiento = hoy.plusDays(40).format(formatoFecha),
            estado = "PENDIENTE"
        )

        assertTrue(InicioViewModel.esCompromisoExigibleAhora(cuotaMesActual))
        assertFalse(InicioViewModel.esCompromisoExigibleAhora(cuotaFutura))
        assertTrue(InicioViewModel.esCompromisoExigibleAhora(cobroUnico))
    }

    @Test
    fun `inicializarEntorno ordena los compromisos cronologicamente por fecha de vencimiento ascendente`() = runTest {
        val hoy = LocalDate.now()
        val mes12 = CompromisoEntity(
            id = 12,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Mensualidad (Mes 12/12 • Recurrente)",
            monto = 250000.0,
            fechaVencimiento = hoy.plusMonths(11).format(formatoFecha),
            estado = "PENDIENTE"
        )
        val mes1 = CompromisoEntity(
            id = 1,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Mensualidad (Mes 1/12 • Recurrente)",
            monto = 250000.0,
            fechaVencimiento = hoy.plusDays(15).format(formatoFecha),
            estado = "PENDIENTE"
        )

        // Simulamos Room devolviendo Mes 12 primero y Mes 1 después
        every { compromisoDao.observarTodos() } returns flowOf(listOf(mes12, mes1))

        viewModel.inicializarEntorno(context)
        advanceUntilIdle()

        val listaOrdenada = viewModel.uiState.value.compromisos
        assertEquals(2, listaOrdenada.size)
        assertEquals("Mensualidad (Mes 1/12 • Recurrente)", listaOrdenada[0].concepto)
        assertEquals("Mensualidad (Mes 12/12 • Recurrente)", listaOrdenada[1].concepto)
    }

    @Test
    fun `calcularStrikesCliente contabiliza solo compromisos vencidos no pagados`() = runTest {
        val hoy = LocalDate.now()
        val vencidoImpago = CompromisoEntity(
            id = 1,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Gasto Común Marzo",
            monto = 85000.0,
            fechaVencimiento = hoy.minusDays(5).format(formatoFecha),
            estado = "PENDIENTE"
        )
        val vencidoPeroPagado = CompromisoEntity(
            id = 2,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Gasto Común Febrero",
            monto = 85000.0,
            fechaVencimiento = hoy.minusDays(35).format(formatoFecha),
            estado = "PAGADO"
        )
        val vigente = CompromisoEntity(
            id = 3,
            organizacionId = 1,
            empresaNombre = "Edificio Mirador",
            concepto = "Gasto Común Abril",
            monto = 85000.0,
            fechaVencimiento = hoy.plusDays(10).format(formatoFecha),
            estado = "PENDIENTE"
        )

        every { compromisoDao.observarTodos() } returns flowOf(listOf(vencidoImpago, vencidoPeroPagado, vigente))

        viewModel.inicializarEntorno(context)
        advanceUntilIdle()

        val (cantidadStrikes, detalles) = viewModel.calcularStrikesCliente(clienteMatias.id)
        assertEquals(1, cantidadStrikes)
        assertTrue(detalles.first().contains("Gasto Común Marzo"))
    }
}