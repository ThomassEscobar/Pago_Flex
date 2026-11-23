package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json
data class OlvideRequestDto(
    @Json(name = "correo") val correo: String
)