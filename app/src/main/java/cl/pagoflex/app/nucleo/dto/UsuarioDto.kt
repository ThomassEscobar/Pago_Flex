package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class UsuarioDto(
    @Json(name = "id") val id: String,
    @Json(name = "correo") val correo: String,
    @Json(name = "nombreCompleto") val nombreCompleto: String,
    // Rol de aplicación: ADMIN | EJECUTIVO | USUARIO
    @Json(name = "rol") val rol: String? = null,
    @Json(name = "rut") val rut: String? = null,
    @Json(name = "empresaId") val empresaId: String? = null,
    // Roles de seguridad de la API (USER, SERVICE); no definen la UI
    @Json(name = "roles") val roles: List<String> = emptyList()
)
