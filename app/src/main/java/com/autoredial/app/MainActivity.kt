package com.autoredial.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.autoredial.app.service.CallService
import com.autoredial.app.ui.theme.AutoRedialTheme
import com.google.accompanist.permissions.*

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoRedialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val permissions = mutableListOf(
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CALL_LOG
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

                        if (permissionsState.allPermissionsGranted) {
                            MainScreen()
                        } else {
                            PermissionScreen(
                                onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.permission_required))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.grant_permissions))
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var maxAttempts by remember { mutableFloatStateOf(0f) }
    var waitTime by remember { mutableFloatStateOf(5f) }
    var bruteForceMode by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    // Listen to service state via a simple global state or BroadcastReceiver
    // For simplicity in this UI, we just toggle isRunning locally and start/stop the service.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text(stringResource(R.string.phone_number_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "${stringResource(R.string.max_attempts)}: ${maxAttempts.toInt()}")
        Slider(
            value = maxAttempts,
            onValueChange = { maxAttempts = it },
            valueRange = 0f..50f,
            steps = 50
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "${stringResource(R.string.wait_time)}: ${waitTime.toInt()}")
        Slider(
            value = waitTime,
            onValueChange = { waitTime = it },
            valueRange = 1f..30f,
            steps = 29
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(
                checked = bruteForceMode,
                onCheckedChange = { bruteForceMode = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "وضع الاستمرار العنيف (لا يتوقف حتى لو تم الرد)")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                if (isRunning) {
                    val intent = Intent(context, CallService::class.java).apply {
                        action = CallService.ACTION_STOP
                    }
                    context.startService(intent)
                    isRunning = false
                } else {
                    if (phoneNumber.isNotBlank()) {
                        val intent = Intent(context, CallService::class.java).apply {
                            action = CallService.ACTION_START
                            putExtra(CallService.EXTRA_PHONE_NUMBER, phoneNumber)
                            putExtra(CallService.EXTRA_MAX_ATTEMPTS, maxAttempts.toInt())
                            putExtra(CallService.EXTRA_WAIT_TIME, waitTime.toLong() * 1000L)
                            putExtra(CallService.EXTRA_BRUTE_FORCE, bruteForceMode)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isRunning = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = if (isRunning) stringResource(R.string.stop_call) else stringResource(R.string.start_call))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
