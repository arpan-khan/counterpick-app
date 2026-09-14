package com.counterpick.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counterpick.app.CounterPickApp
import com.counterpick.app.ui.overlay.OverlayPermission
import com.counterpick.app.ui.overlay.OverlayService
import com.counterpick.app.ui.theme.Green
import com.counterpick.app.ui.theme.Red
import com.counterpick.app.ui.theme.TextDim

@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as CounterPickApp
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.repository, app.settingsPreferences)
    )
    val state by viewModel.uiState.collectAsState()

    var pendingEnableAfterPermission by remember { mutableStateOf(false) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {

        if (OverlayPermission.isGranted(context) && pendingEnableAfterPermission) {
            startOverlay(context)
            viewModel.setOverlayEnabled(true)
        }
        pendingEnableAfterPermission = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentPending by rememberUpdatedState(pendingEnableAfterPermission)
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && currentPending && OverlayPermission.isGranted(context)) {
                startOverlay(context)
                viewModel.setOverlayEnabled(true)
                pendingEnableAfterPermission = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    fun onOverlayToggle(wantsEnabled: Boolean) {
        if (wantsEnabled) {
            if (OverlayPermission.isGranted(context)) {
                startOverlay(context)
                viewModel.setOverlayEnabled(true)
            } else {
                pendingEnableAfterPermission = true
                context.startActivity(OverlayPermission.requestIntent(context))
            }
        } else {
            stopOverlay(context)
            viewModel.setOverlayEnabled(false)
        }
    }

    LaunchedEffect(Unit) {
        if (state.overlayEnabled) {
            if (OverlayPermission.isGranted(context)) {
                startOverlay(context)
            } else {
                viewModel.setOverlayEnabled(false)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Data source", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Currently cached: ${state.cachedGeneratedDate ?: "nothing yet — tap Update data below"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = state.dataJsonUrl,
                        onValueChange = viewModel::setDataJsonUrl,
                        label = { Text("data.json URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.metaJsonUrl,
                        onValueChange = viewModel::setMetaJsonUrl,
                        label = { Text("meta.json URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Use the raw.githubusercontent.com link, not a github.com/…/blob/… page link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = viewModel::updateData,
                        enabled = state.updateStatus !is UpdateStatus.InProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.updateStatus is UpdateStatus.InProgress) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Update data")
                        }
                    }

                    when (val status = state.updateStatus) {
                        is UpdateStatus.Success -> {
                            Spacer(Modifier.height(8.dp))
                            Text(status.message, color = Green, style = MaterialTheme.typography.bodySmall)
                        }
                        is UpdateStatus.Error -> {
                            Spacer(Modifier.height(8.dp))
                            Text("Update failed: ${status.message}", color = Red, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> Unit
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating overlay", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Off by default.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDim
                            )
                        }
                        Switch(checked = state.overlayEnabled, onCheckedChange = { onOverlayToggle(it) })
                    }
                    if (state.overlayEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Running — look for the floating icon on your screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Green
                        )
                    }
                }
            }
        }
    }
}

private fun startOverlay(context: Context) {
    val intent = Intent(context, OverlayService::class.java)
    ContextCompat.startForegroundService(context, intent)
}

private fun stopOverlay(context: Context) {
    context.stopService(Intent(context, OverlayService::class.java))
}
