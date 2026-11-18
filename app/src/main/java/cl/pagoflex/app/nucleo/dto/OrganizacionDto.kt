package cl.pagoflex.app.nucleo.dto

import com.squareup.moshi.Json

data class OrganizacionDto(
    @Json(name = "id") val id: String,
    @Json(name = "nombre") val nombre: String,
    @Json(name = "tipo") val tipo: String
)