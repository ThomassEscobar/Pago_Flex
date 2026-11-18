package cl.pagoflex.app.nucleo.local.dao

import androidx.room.*
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizacionDao {
    @Query("SELECT * FROM organizaciones ORDER BY nombre ASC")
    fun observarTodas(): Flow<List<OrganizacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(organizacion: OrganizacionEntity)

    @Update
    suspend fun actualizar(organizacion: OrganizacionEntity)

    @Delete
    suspend fun eliminar(organizacion: OrganizacionEntity)
}