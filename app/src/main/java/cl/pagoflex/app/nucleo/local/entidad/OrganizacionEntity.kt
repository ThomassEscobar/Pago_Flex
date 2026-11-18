package cl.pagoflex.app.nucleo.local.entidad

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "organizaciones")
data class OrganizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val tipo: String, // Jardín, Club, Edificio, Empresa
    val rut: String,
    val correoContacto: String
)