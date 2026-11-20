package cl.pagoflex.app.nucleo.local.entidad

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val correo: String,
    val clave: String,
    val rol: String = "USUARIO", // USUARIO, ADMIN, EJECUTIVO
    val fotoUri: String? = null,
    val rut: String = "",
    val empresaId: Int? = null
)