package cl.pagoflex.app.inyeccion

import cl.pagoflex.app.nucleo.red.ApiAutenticacion
import cl.pagoflex.app.nucleo.red.ApiCompromisos
import cl.pagoflex.app.nucleo.red.ApiSoporte
import cl.pagoflex.app.nucleo.red.InterceptorAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RedModulo {

    // Usa "http://10.0.2.2:8080/" en el emulador o "http://TU_IP_LOCAL:8080/" en un teléfono físico
    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(interceptorAuth: InterceptorAuth): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(interceptorAuth)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiAutenticacion(retrofit: Retrofit): ApiAutenticacion {
        return retrofit.create(ApiAutenticacion::class.java)
    }

    @Provides
    @Singleton
    fun provideApiCompromisos(retrofit: Retrofit): ApiCompromisos {
        return retrofit.create(ApiCompromisos::class.java)
    }

    @Provides
    @Singleton
    fun provideApiSoporte(retrofit: Retrofit): ApiSoporte {
        return retrofit.create(ApiSoporte::class.java)
    }
}