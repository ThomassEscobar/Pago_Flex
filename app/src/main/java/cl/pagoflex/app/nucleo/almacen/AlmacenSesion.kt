package cl.pagoflex.app.nucleo.almacen

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "pagoflex_session")

/**
 * Identidad guardada tras el inicio de sesión.
 * rol: ADMIN | EJECUTIVO | USUARIO
 */
data class SesionUsuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: String = "USUARIO",
    val rut: String = "",
    val empresaId: String? = null
)

@Singleton
class AlmacenSesion @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val NOMBRE_KEY = stringPreferencesKey("nombre_usuario")
    private val CORREO_KEY = stringPreferencesKey("correo_usuario")
    private val ROL_KEY = stringPreferencesKey("rol_usuario")
    private val ID_KEY = stringPreferencesKey("id_usuario")
    private val RUT_KEY = stringPreferencesKey("rut_usuario")
    private val EMPRESA_KEY = stringPreferencesKey("empresa_usuario")

    val tokenSesion: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[TOKEN_KEY] }

    /** Sesión completa (null si no hay usuario identificado). */
    val sesion: Flow<SesionUsuario?> = context.dataStore.data
        .map { preferences ->
            val id = preferences[ID_KEY]
            if (id.isNullOrBlank()) {
                null
            } else {
                SesionUsuario(
                    id = id,
                    nombre = preferences[NOMBRE_KEY] ?: "",
                    correo = preferences[CORREO_KEY] ?: "",
                    rol = preferences[ROL_KEY] ?: "USUARIO",
                    rut = preferences[RUT_KEY] ?: "",
                    empresaId = preferences[EMPRESA_KEY]
                )
            }
        }

    suspend fun guardarSesion(token: String, usuario: SesionUsuario) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[ID_KEY] = usuario.id
            preferences[NOMBRE_KEY] = usuario.nombre
            preferences[CORREO_KEY] = usuario.correo
            preferences[ROL_KEY] = usuario.rol
            preferences[RUT_KEY] = usuario.rut
            val empresa = usuario.empresaId
            if (empresa.isNullOrBlank()) {
                preferences.remove(EMPRESA_KEY)
            } else {
                preferences[EMPRESA_KEY] = empresa
            }
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
