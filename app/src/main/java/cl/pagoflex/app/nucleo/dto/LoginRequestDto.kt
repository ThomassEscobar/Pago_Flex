package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class LoginRequestDto(
    @Json(name = "correo") val correo: String,
    @Json(name = "contrasena") val contrasena: String
)