package com.student360.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.Screen
import com.student360.app.ui.screens.*
import com.student360.app.ui.theme.Student360Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Student360Theme {
                val repository = remember { StudentRepository(applicationContext) }
                val profile by repository.profileFlow.collectAsState(initial = null)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (profile == null) {
                        var isChecking by remember { mutableStateOf(true) }
                        var hasProfile by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            val prof = repository.getProfile()
                            hasProfile = prof != null && prof.onboarded
                            isChecking = false
                        }

                        if (isChecking) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (hasProfile) {
                            MainShell(repository)
                        } else {
                            OnboardingScreen(repository) {
                                hasProfile = true
                            }
                        }
                    } else if (profile?.onboarded == true) {
                        MainShell(repository)
                    } else {
                        OnboardingScreen(repository) {
                            // Onboarding completed
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(repository: StudentRepository) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Student360",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                val items = listOf(
                    Screen.HOME to Icons.Default.Home,
                    Screen.ATTENDANCE to Icons.Default.CheckCircle,
                    Screen.SCHEDULE to Icons.Default.List,
                    Screen.MY_DAY to Icons.Default.Star,
                    Screen.STUDY to Icons.Default.PlayArrow,
                    Screen.ASSISTANT to Icons.Default.Build,
                    Screen.PROGRESS to Icons.Default.Info,
                    Screen.SETTINGS to Icons.Default.Settings
                )
                
                items.forEach { (screen, icon) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(currentScreen.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentScreen = Screen.ALERTS }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.HOME -> HomeScreen(repository)
                    Screen.ATTENDANCE -> AttendanceScreen(repository)
                    Screen.SCHEDULE -> ScheduleScreen(repository)
                    Screen.MY_DAY -> MyDayScreen(repository)
                    Screen.STUDY -> StudyScreen(repository)
                    Screen.PROGRESS -> ProgressScreen(repository)
                    Screen.SETTINGS -> SettingsScreen(repository)
                    Screen.ALERTS -> AlertsScreen(repository)
                    Screen.ASSISTANT -> AssistantScreen(repository)
                }
            }
        }
    }
}
