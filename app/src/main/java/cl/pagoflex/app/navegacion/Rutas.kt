package cl.pagoflex.app.navegacion

import kotlinx.serialization.Serializable

// Grafos
@Serializable object GrafoAutenticacion
@Serializable object GrafoPrincipal

// Autenticación
@Serializable object RutaLogin
@Serializable object RutaRegistro
@Serializable object RutaOlvideContrasena
@Serializable data class RutaRestablecer(val correo: String)

// Principal
@Serializable object RutaInicio // <--- Ahora es un object simple (sin usuarioId)
@Serializable object RutaListaCompromisos
@Serializable data class RutaDetalleCompromiso(val id: String)
@Serializable data class RutaPago(val compromisoId: String)
@Serializable data class RutaComprobante(val pagoId: String)
@Serializable object RutaHistorial
@Serializable object RutaNotificaciones
@Serializable object RutaPerfil // <--- Corregido el error de tipeo