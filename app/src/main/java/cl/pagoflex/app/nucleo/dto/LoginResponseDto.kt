package cl.pagoflex.app.nucleo.dto


import com.squareup.moshi.Json

data class LoginResponseDto(
    @Json(name = "tokenAcceso") val tokenAcceso: String,
    @Json(name = "tokenRenovacion") val tokenRenovacion: String,
    @Json(name = "expiraEn") val expiraEn: Long,
    @Json(name = "usuario") val usuario: UsuarioDto
)