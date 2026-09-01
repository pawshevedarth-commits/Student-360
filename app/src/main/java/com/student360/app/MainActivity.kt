@file:OptIn(ExperimentalMaterial3Api::class)

package com.student360.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.Subject
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
    val colors = LocalAppColors.current
    var currentScreen by remember { mutableStateOf(Screen.TODAY) }
    var activeDetailSubject by remember { mutableStateOf<Subject?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val profile by repository.profileFlow.collectAsState(initial = null)
    val alerts by repository.alertsFlow.collectAsState(initial = emptyList())
    val unreadAlertsCount = remember(alerts) { alerts.count { !it.isRead } }

    val attendanceViewModel: AttendanceViewModel = viewModel()
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val studyViewModel: StudyViewModel = viewModel()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.card)
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
                                    .background(colors.accent),
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
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${profile?.branch ?: "Engineering"} • Sem ${profile?.semester ?: 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Divider(color = colors.border)
                    Spacer(modifier = Modifier.height(8.dp))

                    val drawerItems = listOf(
                        Screen.HOME to Icons.Default.Home,
                        Screen.MY_DAY to Icons.Default.Check,
                        Screen.STUDY to Icons.Default.PlayArrow,
                        Screen.ASSISTANT to Icons.Default.Star,
                        Screen.EXAMS to Icons.Default.Info,
                        Screen.PROGRESS to Icons.Default.List,
                        Screen.SETTINGS to Icons.Default.Settings
                    )

                    drawerItems.forEach { (screen, icon) ->
                        val isSelected = currentScreen == screen && activeDetailSubject == null
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) colors.accent else colors.textSecondary
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                activeDetailSubject = null
                                currentScreen = screen
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = colors.activePill.copy(alpha = 0.35f),
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Divider(color = colors.border)
                    Text(
                        text = "Student360 v1.1.0 • 100% Offline-First",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = colors.bg,
            topBar = {
                TopAppBar(
                    title = {
                        Student360Wordmark(fontSize = 21.sp)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    actions = {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(
                                onClick = {
                                    activeDetailSubject = null
                                    currentScreen = Screen.ALERTS
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Alerts",
                                    tint = if (currentScreen == Screen.ALERTS) colors.accent else colors.textPrimary
                                )
                            }
                            if (unreadAlertsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 10.dp, end = 10.dp)
                                        .size(8.dp)
                                        .background(colors.accent, CircleShape)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.textPrimary
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReferenceBottomNavItem(
                            icon = Icons.Default.Home,
                            label = "Today",
                            isSelected = (currentScreen == Screen.TODAY && activeDetailSubject == null),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeDetailSubject = null
                                currentScreen = Screen.TODAY
                            }
                        )
                        ReferenceBottomNavItem(
                            icon = Icons.Default.Menu,
                            label = "Timetable",
                            isSelected = (currentScreen == Screen.ATTENDANCE && activeDetailSubject == null),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeDetailSubject = null
                                currentScreen = Screen.ATTENDANCE
                            }
                        )
                        ReferenceBottomNavItem(
                            icon = Icons.Default.DateRange,
                            label = "Calendar",
                            isSelected = (currentScreen == Screen.CALENDAR && activeDetailSubject == null),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeDetailSubject = null
                                currentScreen = Screen.CALENDAR
                            }
                        )
                        ReferenceBottomNavItem(
                            icon = Icons.Default.List,
                            label = "Subjects",
                            isSelected = (currentScreen == Screen.SUBJECTS || activeDetailSubject != null),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeDetailSubject = null
                                currentScreen = Screen.SUBJECTS
                            }
                        )
                        ReferenceBottomNavItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            isSelected = (currentScreen == Screen.SETTINGS && activeDetailSubject == null),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeDetailSubject = null
                                currentScreen = Screen.SETTINGS
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.bg)
            ) {
                if (activeDetailSubject != null) {
                    SubjectDetailScreen(
                        subject = activeDetailSubject!!,
                        repository = repository,
                        viewModel = attendanceViewModel,
                        onBack = { activeDetailSubject = null },
                        onNavigateToExams = {
                            activeDetailSubject = null
                            currentScreen = Screen.EXAMS
                        }
                    )
                } else {
                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "screen_crossfade"
                    ) { screen ->
                        when (screen) {
                            Screen.TODAY -> TodayScreen(
                                repository = repository,
                                viewModel = attendanceViewModel,
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                onStartStudySession = { subjectId, topic, durationMins ->
                                    studyViewModel.startTimer(subjectId, topic, durationMins)
                                    currentScreen = Screen.STUDY
                                }
                            )
                            Screen.ATTENDANCE -> AttendanceScreen(
                                repository = repository,
                                viewModel = attendanceViewModel,
                                scheduleViewModel = scheduleViewModel,
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                            )
                            Screen.CALENDAR -> CalendarScreen(
                                repository = repository,
                                viewModel = attendanceViewModel,
                                onNavigateToToday = { date ->
                                    attendanceViewModel.selectDate(date)
                                    currentScreen = Screen.TODAY
                                }
                            )
                            Screen.SUBJECTS -> SubjectsScreen(
                                repository = repository,
                                viewModel = attendanceViewModel,
                                onNavigateToSubjectDetail = { sub -> activeDetailSubject = sub }
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                repository = repository,
                                viewModel = settingsViewModel,
                                attendanceViewModel = attendanceViewModel
                            )
                            Screen.HOME -> HomeScreen(repository, onNavigate = { s -> currentScreen = s })
                            Screen.MY_DAY -> MyDayScreen(repository)
                            Screen.STUDY -> StudyScreen(
                                repository = repository,
                                viewModel = studyViewModel,
                                onNavigateToProgress = { currentScreen = Screen.PROGRESS }
                            )
                            Screen.ASSISTANT -> AssistantScreen(
                                repository = repository,
                                onStartSession = { subjectId, topic, durationMins ->
                                    studyViewModel.startTimer(subjectId, topic, durationMins)
                                    currentScreen = Screen.STUDY
                                }
                            )
                            Screen.EXAMS -> ExamsScreen(
                                repository = repository,
                                onNavigateToStudy = { subjectId, topic, durationMins ->
                                    studyViewModel.startTimer(subjectId, topic, durationMins)
                                    currentScreen = Screen.STUDY
                                }
                            )
                            Screen.PROGRESS -> ProgressScreen(repository)
                            Screen.ALERTS -> AlertsScreen(repository)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReferenceBottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) colors.accent.copy(alpha = 0.15f) else Color.Transparent,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
