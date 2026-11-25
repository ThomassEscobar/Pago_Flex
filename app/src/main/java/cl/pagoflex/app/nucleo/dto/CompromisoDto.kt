package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class CompromisoDto(
    @Json(name = "id") val id: String,
    @Json(name = "descripcion") val descripcion: String,
    @Json(name = "montoClp") val montoClp: Long,
    @Json(name = "fechaVencimiento") val fechaVencimiento: String, // La recibimos como String para que Moshi no falle
    @Json(name = "estado") val estado: String,
    // Permite saber a quién pertenece el compromiso en las vistas de ejecutivo y administración
    @Json(name = "usuarioId") val usuarioId: String? = null,
    @Json(name = "organizacion") val organizacion: OrganizacionDto
)
