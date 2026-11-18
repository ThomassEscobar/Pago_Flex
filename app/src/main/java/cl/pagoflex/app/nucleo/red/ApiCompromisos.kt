package cl.pagoflex.app.nucleo.red


import cl.pagoflex.app.nucleo.dto.CompromisoDto
import retrofit2.http.GET

interface ApiCompromisos {
    // Ponemos la URL completa apuntando al puerto 8082
    @GET("http://10.0.2.2:8082/api/v1/compromisos")
    suspend fun listarCompromisos(): List<CompromisoDto>
}
