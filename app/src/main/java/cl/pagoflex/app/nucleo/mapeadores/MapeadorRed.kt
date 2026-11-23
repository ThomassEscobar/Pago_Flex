package cl.pagoflex.app.nucleo.mapeadores

import cl.pagoflex.app.nucleo.dto.CompromisoDto
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity

fun CompromisoDto.aEntidadLocal(): CompromisoEntity {
    return CompromisoEntity(
        // Usamos el hash del UUID para simular un ID numérico, o simplemente 0
        id = this.id.hashCode(),
        organizacionId = this.organizacion?.id?.hashCode() ?: 0,
        empresaNombre = this.organizacion?.nombre ?: "Empresa PagoFlex",
        concepto = this.descripcion,
        monto = this.montoClp.toDouble(),
        fechaVencimiento = this.fechaVencimiento,
        estado = this.estado,
        comprobanteUri = null
    )
}

fun crearUsuarioDesdeApi(id: String, nombre: String, correo: String): UsuarioEntity {
    return UsuarioEntity(
        id = id.hashCode(),
        nombre = nombre,
        correo = correo,
        clave = "",
        rol = "USUARIO",
        rut = "11.111.111-1",
        empresaId = null
    )
}