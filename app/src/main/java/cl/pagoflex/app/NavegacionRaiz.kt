package cl.pagoflex.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.pagoflex.app.funcionalidades.autenticacion.PantallaLogin
import cl.pagoflex.app.funcionalidades.inicio.PantallaInicio
import cl.pagoflex.app.navegacion.RutaInicio
import cl.pagoflex.app.navegacion.RutaLogin

@Composable
fun NavegacionRaiz() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = RutaLogin // Aquí iniciamos en el Login
    ) {
        composable<RutaLogin> {
            PantallaLogin(
                onLoginExitoso = {
                    navController.navigate(RutaInicio) {
                        popUpTo(RutaLogin) { inclusive = true }
                    }
                }
            )
        }

        composable<RutaInicio> {
            PantallaInicio()
        }
    }
}