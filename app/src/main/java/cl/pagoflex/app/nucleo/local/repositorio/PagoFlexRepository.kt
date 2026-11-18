package cl.pagoflex.app.nucleo.local.repositorio

import cl.pagoflex.app.nucleo.local.dao.CompromisoDao
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.dao.UsuarioDao
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagoFlexRepository @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val organizacionDao: OrganizacionDao,
    private val compromisoDao: CompromisoDao
) {
    // Usuarios
    suspend fun iniciarSesion(correo: String, clave: String): UsuarioEntity? =
        usuarioDao.login(correo.trim(), clave)

    suspend fun registrarUsuario(usuario: UsuarioEntity): Result<Long> = try {
        if (usuarioDao.buscarPorCorreo(usuario.correo.trim()) != null) {
            Result.failure(Exception("El correo ya está registrado"))
        } else {
            Result.success(usuarioDao.registrar(usuario.copy(correo = usuario.correo.trim())))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun recuperarClave(correo: String, nuevaClave: String): Boolean {
        return usuarioDao.actualizarClave(correo.trim(), nuevaClave) > 0
    }

    suspend fun actualizarUsuario(usuario: UsuarioEntity) = usuarioDao.actualizar(usuario)

    fun observarUsuario(id: Int): Flow<UsuarioEntity?> = usuarioDao.observarUsuario(id)

    // Organizaciones
    fun observarOrganizaciones(): Flow<List<OrganizacionEntity>> = organizacionDao.observarTodas()
    suspend fun guardarOrganizacion(org: OrganizacionEntity) {
        if (org.id == 0) organizacionDao.insertar(org) else organizacionDao.actualizar(org)
    }
    suspend fun eliminarOrganizacion(org: OrganizacionEntity) = organizacionDao.eliminar(org)

    // Compromisos
    fun observarCompromisos(): Flow<List<CompromisoEntity>> = compromisoDao.observarTodos()
    suspend fun guardarCompromiso(comp: CompromisoEntity) {
        if (comp.id == 0) compromisoDao.insertar(comp) else compromisoDao.actualizar(comp)
    }
    suspend fun subirComprobante(id: Int, uri: String?) =
        compromisoDao.registrarComprobante(id, "REVISION", uri)
    suspend fun resolverComprobante(id: Int, aprobado: Boolean) =
        compromisoDao.cambiarEstado(id, if (aprobado) "PAGADO" else "RECHAZADO")
}