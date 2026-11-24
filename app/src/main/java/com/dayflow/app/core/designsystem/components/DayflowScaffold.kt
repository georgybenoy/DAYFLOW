package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dayflow.app.core.designsystem.DayflowTheme

@Composable
fun DayflowScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars,
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = DayflowTheme.colors

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        contentWindowInsets = contentWindowInsets,
        content = content
    )
}

@Preview(name = "DayflowScaffold Preview", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun DayflowScaffoldPreview() {
    DayflowTheme {
        DayflowScaffold(
            bottomBar = {
                DayflowBottomNavigation(
                    currentTab = DayflowTab.TODAY,
                    onTabSelected = {}
                )
            }
        ) { padding ->
            // preview content
        }
    }
}
