package cl.pagoflex.app.nucleo.local.dao

import androidx.room.*
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE correo = :correo AND clave = :clave LIMIT 1")
    suspend fun login(correo: String, clave: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    fun observarUsuario(id: Int): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios ORDER BY id ASC")
    fun observarTodos(): Flow<List<UsuarioEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registrar(usuario: UsuarioEntity): Long

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("UPDATE usuarios SET clave = :nuevaClave WHERE correo = :correo")
    suspend fun actualizarClave(correo: String, nuevaClave: String): Int
}