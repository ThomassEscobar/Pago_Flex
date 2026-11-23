package cl.pagoflex.app.nucleo.almacen

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext // <--- IMPORTANTE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "pagoflex_session")

@Singleton
class AlmacenSesion @Inject constructor(
    @ApplicationContext private val context: Context // <--- AGREGA ESTO
) {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val NOMBRE_KEY = stringPreferencesKey("nombre_usuario")

    val tokenSesion: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[TOKEN_KEY] }

    suspend fun guardarSesion(token: String, nombre: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[NOMBRE_KEY] = nombre
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}