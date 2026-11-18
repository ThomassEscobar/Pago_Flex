package cl.pagoflex.app.nucleo.diseno.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = AzulPrimario,
    onPrimary = Color.White,
    primaryContainer = AzulSecundario,
    onPrimaryContainer = Color.White,
    secondary = VerdeExito,
    onSecondary = Color.White,
    error = RojoError,
    onError = Color.White,
    background = GrisFondo,
    onBackground = GrisTexto,
    surface = GrisSuperficie,
    onSurface = GrisTexto,
    surfaceVariant = GrisTextoSuave
)

private val EsquemaOscuro = darkColorScheme(
    primary = AzulSecundario,
    onPrimary = Color.Black,
    primaryContainer = AzulPrimario,
    onPrimaryContainer = Color.White,
    secondary = VerdeExito,
    onSecondary = Color.Black,
    error = RojoError,
    onError = Color.White
)

@Composable
fun TemaPagoflex(
    modoOscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit
) {
    val esquema = if (modoOscuro) EsquemaOscuro else EsquemaClaro
    MaterialTheme(
        colorScheme = esquema,
        typography = TipografiaPagoflex,
        content = contenido
    )
}