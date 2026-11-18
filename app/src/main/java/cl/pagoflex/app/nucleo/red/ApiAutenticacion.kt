package cl.pagoflex.app.nucleo.red

import cl.pagoflex.app.nucleo.dto.LoginRequestDto
import cl.pagoflex.app.nucleo.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiAutenticacion {
    @POST("api/v1/autenticacion/inicio-sesion")
    suspend fun iniciarSesion(@Body request: LoginRequestDto): LoginResponseDto
}