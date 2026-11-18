package cl.pagoflex.app.nucleo.local.dao

import androidx.room.*
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompromisoDao {
    @Query("SELECT * FROM compromisos ORDER BY id DESC")
    fun observarTodos(): Flow<List<CompromisoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(compromiso: CompromisoEntity)

    @Update
    suspend fun actualizar(compromiso: CompromisoEntity)

    @Query("UPDATE compromisos SET estado = :estado, comprobanteUri = :uri WHERE id = :id")
    suspend fun registrarComprobante(id: Int, estado: String, uri: String?)

    @Query("UPDATE compromisos SET estado = :nuevoEstado WHERE id = :id")
    suspend fun cambiarEstado(id: Int, nuevoEstado: String)
}