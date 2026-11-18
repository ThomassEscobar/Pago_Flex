package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class UsuarioDto(
    @Json(name = "id") val id: String,
    @Json(name = "correo") val correo: String,
    @Json(name = "nombreCompleto") val nombreCompleto: String,
    @Json(name = "roles") val roles: List<String>
)