package cl.pagoflex.app.nucleo.red

import cl.pagoflex.app.nucleo.almacen.AlmacenSesion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class InterceptorAuth @Inject constructor(
    private val almacenSesion: AlmacenSesion
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Obtenemos el token guardado (bloqueando el hilo brevemente porque OkHttp es síncrono)
        val token = runBlocking { almacenSesion.tokenSesion.first() }

        val newRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }
}