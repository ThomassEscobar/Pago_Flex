package cl.pagoflex.app.nucleo.local.repositorio

import cl.pagoflex.app.nucleo.local.dao.CompromisoDao
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.dao.UsuarioDao
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PagoFlexRepositoryTest {

    private lateinit var usuarioDao: UsuarioDao
    private lateinit var organizacionDao: OrganizacionDao
    private lateinit var compromisoDao: CompromisoDao
    private lateinit var repository: PagoFlexRepository

    private val usuarioDemo = UsuarioEntity(
        id = 1,
        nombre = "Matías Pavez",
        correo = "Mat.pavezb@gmail.com",
        clave = "123456",
        rol = "USUARIO",
        rut = "18.765.432-1"
    )

    @Before
    fun setUp() {
        usuarioDao = mockk(relaxed = true)
        organizacionDao = mockk(relaxed = true)
        compromisoDao = mockk(relaxed = true)

        every { usuarioDao.observarTodos() } returns flowOf(listOf(usuarioDemo))
        repository = PagoFlexRepository(usuarioDao, organizacionDao, compromisoDao)
    }

    @Test
    fun `iniciarSesion encuentra usuario sin importar mayusculas en el correo`() = runTest {
        coEvery { usuarioDao.login("mat.pavezb@gmail.com", "123456") } returns null

        val resultado = repository.iniciarSesion("mat.pavezb@gmail.com", "123456")

        assertNotNull(resultado)
        assertEquals("Matías Pavez", resultado?.nombre)
    }

    @Test
    fun `iniciarSesion retorna null si la clave no coincide`() = runTest {
        coEvery { usuarioDao.login(any(), any()) } returns null

        val resultado = repository.iniciarSesion("mat.pavezb@gmail.com", "claveIncorrecta")

        assertNull(resultado)
    }

    @Test
    fun `registrarUsuario rechaza correo duplicado sin importar mayusculas`() = runTest {
        coEvery { usuarioDao.buscarPorCorreo(any()) } returns null

        val nuevoDuplicado = usuarioDemo.copy(id = 0, correo = "MAT.PAVEZB@GMAIL.COM")
        val resultado = repository.registrarUsuario(nuevoDuplicado)

        assertTrue(resultado.isFailure)
        assertEquals("El correo ya está registrado", resultado.exceptionOrNull()?.message)
        coVerify(exactly = 0) { usuarioDao.registrar(any()) }
    }

    @Test
    fun `solicitarRecuperacionPorCorreo detecta cuenta existente en minusculas`() = runTest {
        coEvery { usuarioDao.buscarPorCorreo(any()) } returns null

        assertTrue(repository.solicitarRecuperacionPorCorreo("mat.pavezb@gmail.com"))
        assertFalse(repository.solicitarRecuperacionPorCorreo("noexiste@pagoflex.cl"))
    }
}