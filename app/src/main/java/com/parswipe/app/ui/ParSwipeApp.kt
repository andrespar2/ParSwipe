package com.parswipe.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parswipe.app.ParSwipeViewModel
import com.parswipe.app.data.Permissions
import com.parswipe.app.data.PhotoItem
import com.parswipe.app.ui.theme.ParColors

@Composable
fun ParSwipeApp(viewModel: ParSwipeViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasPermission by remember { mutableStateOf(Permissions.isGranted(context)) }
    var tab by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasPermission = Permissions.isGranted(context)
        if (hasPermission) viewModel.loadPhotos()
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteResult(result.resultCode == Activity.RESULT_OK)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onRestoreResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadPhotos()
        } else {
            permissionLauncher.launch(Permissions.required())
        }
    }

    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    val applyDeletions: (List<PhotoItem>) -> Unit = { items ->
        val sender = viewModel.requestDelete(items)
        if (sender != null) {
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    val restoreLast: () -> Unit = {
        val sender = viewModel.restoreLastApplied()
        if (sender != null) {
            restoreLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    Scaffold(
        containerColor = ParColors.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = ParColors.Black) {
                TabItem(
                    selected = tab == 0,
                    text = "Revisar",
                    icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                    onClick = { tab = 0 },
                )
                TabItem(
                    selected = tab == 1,
                    text = "Duplicadas",
                    icon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = { tab = 1 },
                )
                TabItem(
                    selected = tab == 2,
                    text = "Estadísticas",
                    icon = { Icon(Icons.Rounded.Insights, contentDescription = null) },
                    onClick = { tab = 2 },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!hasPermission) {
                PermissionGate(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    onRequest = { permissionLauncher.launch(Permissions.required()) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                )
            } else {
                when (tab) {
                    0 -> ReviewScreen(
                        vm = viewModel,
                        onApplyDeletions = applyDeletions,
                        onRestoreLast = restoreLast,
                    )

                    1 -> DuplicatesScreen(
                        vm = viewModel,
                        onDeleteSelected = applyDeletions,
                    )

                    else -> StatsScreen(vm = viewModel)
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    selected: Boolean,
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ParColors.Black,
            selectedTextColor = ParColors.TextPrimary,
            indicatorColor = ParColors.TextPrimary,
            unselectedIconColor = ParColors.TextSecondary,
            unselectedTextColor = ParColors.TextSecondary,
        ),
    )
}

@Composable
private fun PermissionGate(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(
            title = "Permiso necesario",
            description = "ParSwipe necesita acceso a tus fotos para poder mostrarlas y " +
                "organizarlas. Todo el procesamiento ocurre en tu dispositivo, sin internet.",
            action = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ActionButton(
                        label = "Conceder acceso",
                        container = ParColors.TextPrimary,
                        contentColor = ParColors.Black,
                        enabled = true,
                        onClick = onRequest,
                    )
                    Spacer(Modifier.height(10.dp))
                    ActionButton(
                        label = "Abrir ajustes",
                        container = ParColors.SurfaceVariant,
                        contentColor = ParColors.TextPrimary,
                        enabled = true,
                        onClick = onOpenSettings,
                    )
                }
            },
        )
    }
}
