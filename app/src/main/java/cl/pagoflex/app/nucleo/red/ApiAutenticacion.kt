package cl.pagoflex.app.nucleo.red

import cl.pagoflex.app.nucleo.dto.LoginRequestDto
import cl.pagoflex.app.nucleo.dto.LoginResponseDto
import cl.pagoflex.app.nucleo.dto.OlvideRequestDto
import cl.pagoflex.app.nucleo.dto.RegistroRequestDto
import cl.pagoflex.app.nucleo.dto.UsuarioDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiAutenticacion {
    @POST("api/v1/autenticacion/inicio-sesion")
    suspend fun iniciarSesion(@Body request: LoginRequestDto): LoginResponseDto

    @POST("api/v1/autenticacion/registro")
    suspend fun registrar(@Body request: RegistroRequestDto)

    @POST("api/v1/autenticacion/contrasena/olvide")
    suspend fun olvidarContrasena(@Body request: OlvideRequestDto)

    // Cuentas de la plataforma: alimenta las vistas de administración y de ejecutivo
    @GET("api/v1/autenticacion/usuarios")
    suspend fun listarUsuarios(): List<UsuarioDto>
}
