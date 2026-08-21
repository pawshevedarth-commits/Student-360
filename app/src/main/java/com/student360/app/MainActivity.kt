@file:OptIn(ExperimentalMaterial3Api::class)

package com.student360.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.Screen
import com.student360.app.ui.components.*
import com.student360.app.ui.screens.*
import com.student360.app.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Student360Theme {
                val repository = remember { StudentRepository(applicationContext) }
                val profile by repository.profileFlow.collectAsState(initial = null)
                var showSplash by remember { mutableStateOf(true) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    if (showSplash) {
                        SplashScreen {
                            showSplash = false
                        }
                    } else if (profile == null) {
                        var isChecking by remember { mutableStateOf(true) }
                        var hasProfile by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            val prof = repository.getProfile()
                            hasProfile = prof != null && prof.onboarded
                            isChecking = false
                        }

                        if (isChecking) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryPurple)
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
    val profile by repository.profileFlow.collectAsState(initial = null)
    val alerts by repository.alertsFlow.collectAsState(initial = emptyList())
    val unreadAlertsCount = remember(alerts) { alerts.count { !it.isRead } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDark,
                drawerContentColor = PrimaryText,
                modifier = Modifier.width(300.dp)
            ) {
                // Official Student360 Branding & Profile in Drawer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark)
                        .padding(20.dp)
                ) {
                    Student360Logo(emblemSize = 34.dp, showWordmark = true)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (profile?.name?.take(1) ?: "S").uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = profile?.name ?: "Student",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = "${profile?.branch ?: "Engineering"} • Sem ${profile?.semester ?: 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    }
                }

                Divider(color = BorderDark)
                Spacer(modifier = Modifier.height(8.dp))

                val items = listOf(
                    Screen.HOME to Icons.Default.Home,
                    Screen.ATTENDANCE to Icons.Default.CheckCircle,
                    Screen.SCHEDULE to Icons.Default.DateRange,
                    Screen.MY_DAY to Icons.Default.Check,
                    Screen.STUDY to Icons.Default.PlayArrow,
                    Screen.ASSISTANT to Icons.Default.Star,
                    Screen.EXAMS to Icons.Default.Info,
                    Screen.PROGRESS to Icons.Default.List,
                    Screen.SETTINGS to Icons.Default.Settings
                )

                items.forEach { (screen, icon) ->
                    val isSelected = currentScreen == screen
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) LightPurple else SecondaryText
                            )
                        },
                        label = {
                            Text(
                                screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryText else SecondaryText
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.25f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider(color = BorderDark)
                Text(
                    text = "Student360 v1.0.0 • 100% Offline-First",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = BgDark,
            topBar = {
                TopAppBar(
                    title = {
                        if (currentScreen == Screen.HOME) {
                            Student360Logo(emblemSize = 30.dp, showWordmark = true)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Student360Emblem(size = 24.dp)
                                Text(
                                    text = currentScreen.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = PrimaryText
                            )
                        }
                    },
                    actions = {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { currentScreen = Screen.ALERTS }) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (currentScreen == Screen.ALERTS) LightPurple else PrimaryText
                                )
                            }
                            if (unreadAlertsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(8.dp)
                                        .background(PrimaryPurple, CircleShape)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceDark,
                        titleContentColor = PrimaryText
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BgDark)
            ) {
                Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                    when (screen) {
                        Screen.HOME -> HomeScreen(repository = repository, onNavigate = { currentScreen = it })
                        Screen.ATTENDANCE -> AttendanceScreen(repository)
                        Screen.SCHEDULE -> ScheduleScreen(repository)
                        Screen.MY_DAY -> MyDayScreen(repository)
                        Screen.STUDY -> StudyScreen(repository)
                        Screen.ASSISTANT -> AssistantScreen(repository)
                        Screen.EXAMS -> ExamsScreen(repository)
                        Screen.PROGRESS -> ProgressScreen(repository)
                        Screen.SETTINGS -> SettingsScreen(repository)
                        Screen.ALERTS -> AlertsScreen(repository)
                    }
                }
            }
        }
    }
}
