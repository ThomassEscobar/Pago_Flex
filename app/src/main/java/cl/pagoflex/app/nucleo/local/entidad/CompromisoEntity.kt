package cl.pagoflex.app.nucleo.local.entidad

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compromisos")
data class CompromisoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val organizacionId: Int,
    val empresaNombre: String,
    val concepto: String,
    val monto: Double,
    val fechaVencimiento: String, // Formato DD/MM/AAAA
    val estado: String = "PENDIENTE", // PENDIENTE, REVISION, PAGADO, RECHAZADO
    val comprobanteUri: String? = null
)