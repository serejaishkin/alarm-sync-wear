package com.sysadmindoc.alarmclock.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.alarmedit.AlarmEditScreen
import com.sysadmindoc.alarmclock.ui.alarmlist.GoogleStyleAlarmScreen
import com.sysadmindoc.alarmclock.ui.bedtime.BedtimeScreen
import com.sysadmindoc.alarmclock.ui.components.BottomNavContainer
import com.sysadmindoc.alarmclock.ui.onboarding.OnboardingScreen
import com.sysadmindoc.alarmclock.ui.settings.SettingsScreen
import com.sysadmindoc.alarmclock.ui.share.SharedAlarmImportScreen
import com.sysadmindoc.alarmclock.ui.stopwatch.StopwatchScreen
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.timer.TimerScreen
import com.sysadmindoc.alarmclock.util.ReliabilityDoctor

sealed class Screen(val route: String) {
    data object AlarmList : Screen("alarm_list")
    data object AlarmEdit : Screen("alarm_edit/{alarmId}") {
        fun createRoute(alarmId: Long) = "alarm_edit/$alarmId"
    }
    data object Timer : Screen("timer")
    data object Stopwatch : Screen("stopwatch")
    data object Settings : Screen("settings")
    data object Bedtime : Screen("bedtime")
    data object Onboarding : Screen("onboarding")
    data object SharedAlarmImport : Screen("shared_alarm_import")
}

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector
)

private val primaryNavItems = listOf(
    BottomNavItem(Screen.AlarmList, R.string.nav_alarms, Icons.Default.Alarm),
    BottomNavItem(Screen.Bedtime, R.string.nav_bedtime, Icons.Default.Bedtime),
    BottomNavItem(Screen.Timer, R.string.nav_timer, Icons.Default.Timer),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Default.Settings),
)

private const val ONBOARDING_VERSION = 1
private const val ONBOARDING_DONE_KEY = "onboarding_complete_v$ONBOARDING_VERSION"

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    sharedAlarmDraft: Alarm? = null,
    onSharedAlarmConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val windowWidth = (context as? ComponentActivity)?.let {
        calculateWindowSizeClass(it).widthSizeClass
    } ?: WindowWidthSizeClass.Compact
    val useNavigationRail = windowWidth != WindowWidthSizeClass.Compact

    val prefs = remember { context.getSharedPreferences("app_prefs", 0) }
    val hasCompletedOnboarding = remember { prefs.getBoolean(ONBOARDING_DONE_KEY, false) }
    val reliabilityChecklistDue = remember { ReliabilityDoctor.isChecklistDue(context) }
    val startDest = if (hasCompletedOnboarding && !reliabilityChecklistDue) {
        Screen.AlarmList.route
    } else {
        Screen.Onboarding.route
    }

    val showPrimaryNav = currentDestination?.route in primaryNavItems.map { it.screen.route }
    val onTabClick: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = SurfaceDark,
        bottomBar = {
            if (showPrimaryNav && !useNavigationRail) {
                BottomNavContainer {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        primaryNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                                label = { Text(label, maxLines = 1) },
                                selected = selected,
                                alwaysShowLabel = true,
                                onClick = { onTabClick(item.screen) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (showPrimaryNav && useNavigationRail) {
            Row(Modifier.padding(padding).fillMaxSize()) {
                NavigationRail(containerColor = SurfaceDark, contentColor = TextPrimary) {
                    primaryNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        val label = stringResource(item.labelRes)
                        NavigationRailItem(
                            icon = { Icon(item.icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                            label = { Text(label, maxLines = 1) },
                            selected = selected,
                            alwaysShowLabel = true,
                            onClick = { onTabClick(item.screen) },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    WakeSyncNavHost(navController, startDest, prefs, reliabilityChecklistDue, sharedAlarmDraft, onSharedAlarmConsumed)
                }
            }
        } else {
            WakeSyncNavHost(
                navController = navController,
                startDest = startDest,
                prefs = prefs,
                openReadinessChecklist = reliabilityChecklistDue,
                sharedAlarmDraft = sharedAlarmDraft,
                onSharedAlarmConsumed = onSharedAlarmConsumed,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun WakeSyncNavHost(
    navController: NavHostController,
    startDest: String,
    prefs: android.content.SharedPreferences,
    openReadinessChecklist: Boolean,
    sharedAlarmDraft: Alarm?,
    onSharedAlarmConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(sharedAlarmDraft) {
        if (sharedAlarmDraft != null) {
            navController.navigate(Screen.SharedAlarmImport.route) { launchSingleTop = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier,
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 6 }) + fadeOut(targetAlpha = 0.72f) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut(targetAlpha = 0.72f) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                openReadinessChecklist = openReadinessChecklist,
                onComplete = {
                    ReliabilityDoctor.markChecklistReviewed(context)
                    prefs.edit().putBoolean(ONBOARDING_DONE_KEY, true).apply()
                    navController.navigate(Screen.AlarmList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.AlarmList.route) {
            GoogleStyleAlarmScreen(
                onAddAlarm = { navController.navigate(Screen.AlarmEdit.createRoute(-1)) },
                onEditAlarm = { id -> navController.navigate(Screen.AlarmEdit.createRoute(id)) },
                onOpenBedtime = { navController.navigate(Screen.Bedtime.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.AlarmEdit.route,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) {
            AlarmEditScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.SharedAlarmImport.route) {
            val draft = sharedAlarmDraft
            if (draft == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                SharedAlarmImportScreen(
                    alarm = draft,
                    onCancel = {
                        onSharedAlarmConsumed()
                        navController.popBackStack()
                    },
                    onSaved = { savedId ->
                        onSharedAlarmConsumed()
                        navController.navigate(Screen.AlarmEdit.createRoute(savedId)) {
                            popUpTo(Screen.SharedAlarmImport.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        composable(Screen.Bedtime.route) { BedtimeScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Screen.Timer.route) { TimerScreen(onOpenStopwatch = { navController.navigate(Screen.Stopwatch.route) }) }
        composable(Screen.Stopwatch.route) { StopwatchScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToStats = {},
                onNavigateToStopwatch = { navController.navigate(Screen.Stopwatch.route) },
                onNavigateToBedtime = { navController.navigate(Screen.Bedtime.route) },
                onOpenOnboardingChecklist = { navController.navigate(Screen.Onboarding.route) { launchSingleTop = true } }
            )
        }
    }
}
