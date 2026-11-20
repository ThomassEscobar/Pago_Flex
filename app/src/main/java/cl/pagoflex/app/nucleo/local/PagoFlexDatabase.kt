package cl.pagoflex.app.nucleo.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.pagoflex.app.nucleo.local.dao.CompromisoDao
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.dao.UsuarioDao
import cl.pagoflex.app.nucleo.local.entidad.CompromisoEntity
import cl.pagoflex.app.nucleo.local.entidad.OrganizacionEntity
import cl.pagoflex.app.nucleo.local.entidad.UsuarioEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UsuarioEntity::class, OrganizacionEntity::class, CompromisoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PagoFlexDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun organizacionDao(): OrganizacionDao
    abstract fun compromisoDao(): CompromisoDao

    class SeedCallback(
        private val databaseProvider: () -> PagoFlexDatabase
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = databaseProvider()

                database.organizacionDao().insertar(
                    OrganizacionEntity(
                        id = 1,
                        nombre = "Edificio Mirador",
                        tipo = "Edificio",
                        rut = "76.123.456-0",
                        correoContacto = "admin@mirador.cl"
                    )
                )
                database.organizacionDao().insertar(
                    OrganizacionEntity(
                        id = 2,
                        nombre = "Jardín Los Peques",
                        tipo = "Jardín",
                        rut = "65.987.321-4",
                        correoContacto = "contacto@lospeques.cl"
                    )
                )

                database.usuarioDao().registrar(
                    UsuarioEntity(
                        nombre = "Matías Pavez",
                        correo = "Mat.pavezb@gmail.com",
                        clave = "123456",
                        rol = "USUARIO",
                        rut = "19.456.782-0"
                    )
                )
                database.usuarioDao().registrar(
                    UsuarioEntity(
                        nombre = "Andrés Correa",
                        correo = "ejecutivo@gmail.com",
                        clave = "123456",
                        rol = "EJECUTIVO",
                        rut = "15.834.921-3",
                        empresaId = 1
                    )
                )
                database.usuarioDao().registrar(
                    UsuarioEntity(
                        nombre = "Carlos Ortega",
                        correo = "admin@gmail.com",
                        clave = "123456",
                        rol = "ADMIN",
                        rut = "12.645.890-8"
                    )
                )
                database.usuarioDao().registrar(
                    UsuarioEntity(
                        nombre = "Camila Soto",
                        correo = "usuario@pagoflex.cl",
                        clave = "123456",
                        rol = "USUARIO",
                        rut = "18.765.432-7"
                    )
                )
                database.usuarioDao().registrar(
                    UsuarioEntity(
                        nombre = "María López",
                        correo = "ejecutivo2@pagoflex.cl",
                        clave = "123456",
                        rol = "EJECUTIVO",
                        rut = "16.543.219-3",
                        empresaId = 2
                    )
                )

                database.compromisoDao().insertar(
                    CompromisoEntity(
                        organizacionId = 1,
                        empresaNombre = "Edificio Mirador",
                        concepto = "Gasto Común Noviembre",
                        monto = 85000.0,
                        fechaVencimiento = "30/11/2026",
                        estado = "PENDIENTE"
                    )
                )
                database.compromisoDao().insertar(
                    CompromisoEntity(
                        organizacionId = 2,
                        empresaNombre = "Jardín Los Peques",
                        concepto = "Mensualidad Escolar",
                        monto = 195000.0,
                        fechaVencimiento = "05/12/2026",
                        estado = "REVISION"
                    )
                )
            }
        }
    }
}