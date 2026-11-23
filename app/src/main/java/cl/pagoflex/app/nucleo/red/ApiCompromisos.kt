package cl.pagoflex.app.nucleo.red

import cl.pagoflex.app.nucleo.dto.CompromisoDto
import retrofit2.http.GET

interface ApiCompromisos {
    // Usamos la ruta relativa. El BASE_URL (10.0.2.2:8080) se encargará del resto.
    @GET("api/v1/compromisos")
    suspend fun listarCompromisos(): List<CompromisoDto>
}