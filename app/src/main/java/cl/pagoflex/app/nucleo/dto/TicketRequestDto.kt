package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class TicketRequestDto(
    @Json(name = "usuarioId") val usuarioId: String,
    @Json(name = "usuarioNombre") val usuarioNombre: String,
    @Json(name = "tipo") val tipo: String, // SOPORTE, REGISTRO_EMPRESA
    @Json(name = "asunto") val asunto: String,
    @Json(name = "descripcion") val descripcion: String
)