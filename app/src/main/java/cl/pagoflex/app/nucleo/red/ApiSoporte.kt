package cl.pagoflex.app.nucleo.red

import cl.pagoflex.app.nucleo.dto.TicketRequestDto
import cl.pagoflex.app.nucleo.dto.TicketResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiSoporte {
    @GET("api/v1/soporte/tickets")
    suspend fun listarTickets(): List<TicketResponseDto>

    @POST("api/v1/soporte/tickets")
    suspend fun crearTicket(@Body request: TicketRequestDto): TicketResponseDto

    @PUT("api/v1/soporte/tickets/{id}/resolver")
    suspend fun resolverTicket(
        @Path("id") id: String,
        @Body request: Map<String, String> // { "estado": "APROBADO", "motivoRespuesta": "..." }
    ): TicketResponseDto
}