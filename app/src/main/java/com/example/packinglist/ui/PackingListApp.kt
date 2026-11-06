package com.example.packinglist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.packinglist.ui.screens.DatabaseScreen
import com.example.packinglist.ui.screens.GitHubScreen
import com.example.packinglist.ui.screens.PackScreen
import com.example.packinglist.ui.screens.SetupScreen
import com.example.packinglist.ui.theme.PackingListTheme

private enum class AppTab {
    PACK,
    DATABASE,
    GITHUB
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingListApp(viewModel: PackingViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val githubSettings by viewModel.githubSettings.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.PACK) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = selectedTab != AppTab.PACK) {
        selectedTab = AppTab.PACK
    }
    BackHandler(enabled = selectedTab == AppTab.PACK && uiState.activeTrip != null) {
        viewModel.leaveTrip()
    }

    PackingListTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Luggage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Packing List",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectedTab = AppTab.GITHUB }) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "GitHub-Einstellungen"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.PACK,
                        onClick = { selectedTab = AppTab.PACK },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Luggage,
                                contentDescription = null
                            )
                        },
                        label = { Text("Packen") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.DATABASE,
                        onClick = { selectedTab = AppTab.DATABASE },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null
                            )
                        },
                        label = { Text("Datenbank") }
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedTab) {
                    AppTab.PACK -> {
                        val activeTrip = uiState.activeTrip
                        if (activeTrip == null) {
                            SetupScreen(
                                trips = uiState.trips,
                                counts = uiState.counts,
                                onOpenTrip = viewModel::openTrip,
                                onCreateTrip = viewModel::createTrip,
                                onDeleteTrip = viewModel::deleteTrip,
                                onOpenDatabase = { selectedTab = AppTab.DATABASE }
                            )
                        } else {
                            PackScreen(
                                trip = activeTrip,
                                items = uiState.activeItems,
                                onLeaveTrip = viewModel::leaveTrip,
                                onPackedChange = viewModel::setPacked,
                                onCountChange = viewModel::setCount,
                                onResetPacked = { viewModel.resetPacked(activeTrip.id) },
                                onDeleteTrip = { viewModel.deleteTrip(activeTrip) },
                                onOpenDatabase = {
                                    viewModel.useActiveTripInDatabase()
                                    selectedTab = AppTab.DATABASE
                                }
                            )
                        }
                    }

                    AppTab.DATABASE -> {
                        DatabaseScreen(
                            trips = uiState.trips,
                            counts = uiState.counts,
                            dataTrip = uiState.dataTrip,
                            articles = uiState.articles,
                            onSelectTrip = viewModel::selectDataTrip,
                            onCreateTrip = viewModel::createTrip,
                            onUpdateTrip = viewModel::updateTrip,
                            onDeleteTrip = viewModel::deleteTrip,
                            onAddArticle = viewModel::addArticle,
                            onUpdateArticle = viewModel::updateArticle,
                            onDeleteArticle = viewModel::deleteArticle,
                            onOpenPackList = { trip ->
                                viewModel.openTrip(trip)
                                selectedTab = AppTab.PACK
                            }
                        )
                    }

                    AppTab.GITHUB -> {
                        GitHubScreen(
                            settings = githubSettings,
                            onSave = viewModel::saveGitHubSettings,
                            onTest = viewModel::testGitHubConnection,
                            onUploadLocalPhotos = viewModel::uploadLocalPhotos
                        )
                    }
                }
            }
        }
    }
}
