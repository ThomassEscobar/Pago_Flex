package cl.pagoflex.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.pagoflex.app.nucleo.almacen.AlmacenSesion
import cl.pagoflex.app.nucleo.diseno.tema.TemaPagoflex
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel para verificar si hay sesión guardada
@HiltViewModel
class SesionViewModel @Inject constructor(
    private val almacenSesion: AlmacenSesion
) : ViewModel() {
    var cargando by mutableStateOf(true)
        private set
    var estaLogueado by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val token = almacenSesion.tokenSesion.first()
            estaLogueado = !token.isNullOrBlank()
            cargando = false
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaPagoflex {
                val context = LocalContext.current
                val sesionViewModel: SesionViewModel = hiltViewModel()

                val permisosObligatorios = remember {
                    buildList {
                        add(Manifest.permission.CAMERA)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                            add(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }.toTypedArray()
                }

                fun tienePermisosCriticos(): Boolean {
                    val camaraOk = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else true
                    return camaraOk && notifOk
                }

                var mostrarDialogoPermisos by remember { mutableStateOf(!tienePermisosCriticos()) }

                val launcherPermisos = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    mostrarDialogoPermisos = !tienePermisosCriticos()
                }

                LaunchedEffect(Unit) {
                    if (!tienePermisosCriticos()) {
                        launcherPermisos.launch(permisosObligatorios)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding()
                ) {
                    // Si está cargando el DataStore, mostramos un spinner
                    if (sesionViewModel.cargando) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Navegación con Auto-Login
                        NavegacionRaiz(estaLogueado = sesionViewModel.estaLogueado)
                    }

                    // Diálogo de permisos
                    if (mostrarDialogoPermisos) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = {
                                Text(text = "Permisos obligatorios requeridos", fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Para operar en PagoFlex es obligatorio conceder los siguientes permisos desde el inicio:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "• Cámara y Archivos: para capturar y adjuntar comprobantes de pago.\n• Notificaciones: para alertas de vencimientos, aprobaciones y strikes.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { launcherPermisos.launch(permisosObligatorios) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Conceder Permisos")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Abrir Ajustes de la App")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}