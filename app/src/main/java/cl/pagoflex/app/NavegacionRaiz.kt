package cl.pagoflex.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cl.pagoflex.app.navegacion.RutaInicio
import cl.pagoflex.app.navegacion.RutaLogin
import cl.pagoflex.app.navegacion.RutaOlvideContrasena
import cl.pagoflex.app.navegacion.RutaRegistro
import cl.pagoflex.app.nucleo.funcionalidades.autenticacion.PantallaLogin
import cl.pagoflex.app.nucleo.funcionalidades.autenticacion.PantallaRecuperarClave
import cl.pagoflex.app.nucleo.funcionalidades.autenticacion.PantallaRegistro
import cl.pagoflex.app.nucleo.funcionalidades.inicio.PantallaInicio

@Composable
fun NavegacionRaiz() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = RutaLogin
    ) {
        composable<RutaLogin> {
            PantallaLogin(
                onLoginExitoso = { usuario ->
                    navController.navigate(RutaInicio(usuarioId = usuario.id)) {
                        popUpTo(RutaLogin) { inclusive = true }
                    }
                },
                onIrARegistro = {
                    navController.navigate(RutaRegistro)
                },
                onIrARecuperarClave = {
                    navController.navigate(RutaOlvideContrasena)
                }
            )
        }

        composable<RutaRegistro> {
            PantallaRegistro(
                onVolverALogin = {
                    navController.popBackStack()
                }
            )
        }

        composable<RutaOlvideContrasena> {
            PantallaRecuperarClave(
                onVolverALogin = {
                    navController.popBackStack()
                }
            )
        }

        composable<RutaInicio> { backStackEntry ->
            val ruta = backStackEntry.toRoute<RutaInicio>()
            PantallaInicio(
                usuarioId = ruta.usuarioId,
                onCerrarSesion = {
                    navController.navigate(RutaLogin) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}