package cl.pagoflex.app.inyeccion

import android.content.Context
import androidx.room.Room
import cl.pagoflex.app.nucleo.local.PagoFlexDatabase
import cl.pagoflex.app.nucleo.local.dao.CompromisoDao
import cl.pagoflex.app.nucleo.local.dao.OrganizacionDao
import cl.pagoflex.app.nucleo.local.dao.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseDatosModulo {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        dbProvider: Provider<PagoFlexDatabase>
    ): PagoFlexDatabase {
        return Room.databaseBuilder(
            context,
            PagoFlexDatabase::class.java,
            "pagoflex_local.db"
        )
            .addCallback(PagoFlexDatabase.SeedCallback { dbProvider.get() })
            .build()
    }

    @Provides
    fun provideUsuarioDao(db: PagoFlexDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideOrganizacionDao(db: PagoFlexDatabase): OrganizacionDao = db.organizacionDao()

    @Provides
    fun provideCompromisoDao(db: PagoFlexDatabase): CompromisoDao = db.compromisoDao()
}