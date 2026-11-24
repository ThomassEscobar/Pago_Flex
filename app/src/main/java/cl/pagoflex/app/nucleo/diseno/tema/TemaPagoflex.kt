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
    primaryContainer = AzulContenedorClaro,
    onPrimaryContainer = AzulPrimario,
    secondary = VerdeExito,
    onSecondary = Color.White,
    secondaryContainer = VerdeContenedorClaro,
    onSecondaryContainer = VerdeOscuro,
    tertiary = AzulAcento,
    onTertiary = Color.White,
    error = RojoError,
    onError = Color.White,
    errorContainer = RojoContenedorClaro,
    onErrorContainer = RojoOscuro,
    background = GrisFondo,
    onBackground = GrisTexto,
    surface = GrisSuperficie,
    onSurface = GrisTexto,
    surfaceVariant = GrisSuperficieVariante,
    onSurfaceVariant = GrisTextoSuave,
    outline = GrisBorde,
    outlineVariant = GrisSuperficieVariante
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = AzulPrimario,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color.White,
    secondary = VerdeAcento,
    onSecondary = AzulPrimario,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = VerdeContenedorClaro,
    tertiary = Color(0xFFBFDBFE),
    onTertiary = AzulPrimario,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = RojoContenedorClaro,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = SuperficieOscura,
    onSurface = TextoOscuro,
    surfaceVariant = SuperficieVarianteOscura,
    onSurfaceVariant = TextoOscuroSuave,
    outline = Color(0xFF64748B),
    outlineVariant = SuperficieVarianteOscura
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