package cl.pagoflex.app.nucleo.funcionalidades.autenticacion

import android.content.Context
import android.content.SharedPreferences
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import cl.pagoflex.app.nucleo.local.repositorio.PagoFlexRepository
import cl.pagoflex.app.nucleo.red.ApiAutenticacion
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var apiAutenticacion: ApiAutenticacion
    private lateinit var repository: PagoFlexRepository
    private lateinit var organizacionDao: OrganizacionDao
    private lateinit var viewModel: LoginViewModel

    private val empresaDemo = OrganizacionEntity(
        id = 10,
        nombre = "Edificio Mirador",
        tipo = "Edificio",
        rut = "76.123.456-0",
        correoContacto = "contacto@mirador.cl"
    )

    private val clienteDemo = UsuarioEntity(
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

        context = mockk(relaxed = true)
        sharedPrefs = mockk(relaxed = true)
        apiAutenticacion = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        organizacionDao = mockk(relaxed = true)

        every { context.getSharedPreferences("pagoflex_seguridad_global", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.getString(any(), any()) } returns null
        every { repository.observarOrganizaciones() } returns flowOf(listOf(empresaDemo))
        coEvery { apiAutenticacion.iniciarSesion(any()) } throws RuntimeException("Backend offline")
        coEvery { apiAutenticacion.solicitarRecuperacionClave(any()) } throws RuntimeException("Backend offline")

        viewModel = LoginViewModel(context, apiAutenticacion, repository, organizacionDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `iniciarSesion autentica usuario cuando credenciales son validas y no esta bloqueado`() = runTest {
        coEvery { repository.iniciarSesion("mat.pavezb@gmail.com", "123456") } returns clienteDemo

        viewModel.onCorreoChange("mat.pavezb@gmail.com")
        viewModel.onContrasenaChange("123456")
        viewModel.iniciarSesion()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(clienteDemo, state.usuarioAutenticado)
        assertNull(state.mensajeGlobalError)
    }

    @Test
    fun `iniciarSesion bloquea acceso si la cuenta fue suspendida por el administrador`() = runTest {
        coEvery { repository.iniciarSesion("mat.pavezb@gmail.com", "123456") } returns clienteDemo
        every { sharedPrefs.getString("bloqueo_motivo_1", null) } returns "3 Strikes por mora"

        viewModel.onCorreoChange("mat.pavezb@gmail.com")
        viewModel.onContrasenaChange("123456")
        viewModel.iniciarSesion()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.usuarioAutenticado)
        assertTrue(state.mensajeGlobalError?.contains("Cuenta suspendida") == true)
    }

    @Test
    fun `onRolChange bloquea seleccion de rol ADMIN en registro publico`() {
        viewModel.onRolChange("ADMIN")
        assertEquals("USUARIO", viewModel.uiState.value.rolSeleccionado)
    }

    @Test
    fun `registrarCuenta de EJECUTIVO valida RUT y correo institucional de la empresa`() = runTest {
        advanceUntilIdle()
        coEvery { repository.registrarUsuario(any()) } returns Result.success(2L)

        viewModel.onRolChange("EJECUTIVO")
        viewModel.onNombreChange("Carlos Ejecutivo")
        viewModel.onRutChange("12.345.678-5")
        viewModel.onCorreoChange("carlos@mirador.cl")
        viewModel.onContrasenaChange("123456")
        viewModel.onConfirmarContrasenaChange("123456")
        viewModel.onEmpresaSeleccionadaChange(empresaDemo)

        // Intentar primero con correo institucional equivocado
        viewModel.onCorreoEmpresaChange("otro@empresa.cl")
        viewModel.onRutEmpresaChange("76.123.456-0")
        viewModel.registrarCuenta()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.registroExitoso)
        assertNotNull(viewModel.uiState.value.errorCorreoEmpresa)

        // Corregir correo institucional al de la empresa
        viewModel.onCorreoEmpresaChange("contacto@mirador.cl")
        viewModel.registrarCuenta()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registroExitoso)
        coVerify(exactly = 1) { repository.registrarUsuario(match { it.rol == "EJECUTIVO" && it.empresaId == 10 }) }
    }

    @Test
    fun `enviarCorreoRecuperacion confirma envio si correo existe y falla si no existe`() = runTest {
        coEvery { repository.solicitarRecuperacionPorCorreo("mat.pavezb@gmail.com") } returns true
        coEvery { repository.solicitarRecuperacionPorCorreo("desconocido@pagoflex.cl") } returns false

        viewModel.onCorreoChange("mat.pavezb@gmail.com")
        viewModel.enviarCorreoRecuperacion()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.correoRecuperacionEnviado)
        assertNull(viewModel.uiState.value.mensajeGlobalError)

        viewModel.limpiarMensajes()
        viewModel.onCorreoChange("desconocido@pagoflex.cl")
        viewModel.enviarCorreoRecuperacion()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.correoRecuperacionEnviado)
        assertNotNull(viewModel.uiState.value.mensajeGlobalError)
    }
}