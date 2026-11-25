package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class RegistroRequestDto(
    @Json(name = "nombreCompleto") val nombreCompleto: String,
    @Json(name = "correo") val correo: String,
    @Json(name = "contrasena") val contrasena: String,
    @Json(name = "rut") val rut: String?,
    @Json(name = "rol") val rol: String?,
    @Json(name = "empresaId") val empresaId: String?
)