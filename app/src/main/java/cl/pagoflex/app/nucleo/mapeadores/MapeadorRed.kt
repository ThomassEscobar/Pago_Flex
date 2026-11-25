package cl.pagoflex.app.nucleo.mapeadores

import cl.pagoflex.app.nucleo.almacen.SesionUsuario
import cl.pagoflex.app.nucleo.dto.CompromisoDto
import cl.pagoflex.app.nucleo.dto.OrganizacionDto
import cl.pagoflex.app.nucleo.dto.UsuarioDto
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity

// 1. Mapeador de Compromiso (Para que coincida con el nombre que usaron ayer)
fun CompromisoDto.aCompromisoLocal(): CompromisoEntity {
    return CompromisoEntity(
        id = this.id.hashCode(),
        usuarioId = 0, // Se llenará en la lógica del ViewModel
        organizacionId = this.organizacion?.id?.hashCode() ?: 0,
        empresaNombre = this.organizacion?.nombre ?: "Empresa PagoFlex",
        concepto = this.descripcion,
        monto = this.montoClp.toDouble(),
        fechaVencimiento = this.fechaVencimiento,
        estado = this.estado,
        comprobanteUri = null
    )
}

// 2. Mapeador de Organización
fun OrganizacionDto.aOrganizacionEntity(): OrganizacionEntity {
    return OrganizacionEntity(
        id = this.id.hashCode(),
        nombre = this.nombre,
        tipo = this.tipo ?: "EMPRESA",
        rut = "76.123.456-7",
        correoContacto = "contacto@empresa.cl"
    )
}

// 3. Mapeador de Usuario desde la API
fun UsuarioDto.aUsuarioEntity(): UsuarioEntity {
    return UsuarioEntity(
        id = this.id.hashCode(),
        nombre = this.nombreCompleto,
        correo = this.correo,
        clave = "",
        rol = this.rol ?: "USUARIO", // Si el backend no manda rol, por defecto USUARIO
        rut = this.rut ?: "",
        empresaId = this.empresaId?.hashCode()
    )
}

// 4. Mapeador de Usuario desde la Sesión Local (por si falla la red)
fun SesionUsuario.aUsuarioEntity(): UsuarioEntity {
    return UsuarioEntity(
        id = this.id.hashCode(),
        nombre = this.nombre,
        correo = this.correo,
        clave = "",
        rol = this.rol,
        rut = this.rut,
        empresaId = this.empresaId?.hashCode()
    )
}