package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class TicketResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "usuarioId") val usuarioId: String,
    @Json(name = "usuarioNombre") val usuarioNombre: String,
    @Json(name = "tipo") val tipo: String,
    @Json(name = "asunto") val asunto: String,
    @Json(name = "descripcion") val descripcion: String,
    @Json(name = "estado") val estado: String,
    @Json(name = "motivoRespuesta") val motivoRespuesta: String? = null,
    @Json(name = "creadoEn") val creadoEn: String
)