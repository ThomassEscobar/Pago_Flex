package cl.pagoflex.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.pagoflex.app.navegacion.*
import cl.pagoflex.app.nucleo.funcionalidades.autenticacion.*
import cl.pagoflex.app.nucleo.funcionalidades.inicio.PantallaInicio

@Composable
fun NavegacionRaiz(estaLogueado: Boolean) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        // Si está logueado va a Inicio, si no, a Login
        startDestination = if (estaLogueado) RutaInicio else RutaLogin
    ) {
        composable<RutaLogin> {
            PantallaLogin(
                onLoginExitoso = {
                    navController.navigate(RutaInicio) {
                        popUpTo(RutaLogin) { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate(RutaRegistro) },
                onIrARecuperarClave = { navController.navigate(RutaOlvideContrasena) }
            )
        }

        composable<RutaRegistro> {
            PantallaRegistro(
                onVolverALogin = { navController.popBackStack() }
            )
        }

        composable<RutaOlvideContrasena> {
            PantallaRecuperarClave(
                onVolverALogin = { navController.popBackStack() }
            )
        }

        composable<RutaInicio> {
            PantallaInicio(
                onCerrarSesion = {
                    // Al cerrar sesión, limpiamos el historial y volvemos al login
                    navController.navigate(RutaLogin) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}