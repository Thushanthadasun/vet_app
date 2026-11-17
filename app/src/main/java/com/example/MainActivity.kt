package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notifications.NotificationHelper
import com.example.ui.BottomTab
import com.example.ui.MainViewModel
import com.example.ui.components.PastelGradientBackground
import com.example.ui.components.fancyIridescentBorder
import com.example.ui.screens.BirthdaySurpriseOverlay
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.GamesViewModel
import com.example.ui.screens.GiftsScreen
import com.example.ui.screens.GiftsViewModel
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.GoalsViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemesScreen
import com.example.ui.screens.MemesViewModel
import com.example.ui.theme.LocalPastelColors
import com.example.ui.theme.PastelPawsTheme

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PastelPawsApp).container
        mainViewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(container.repository),
        )[MainViewModel::class.java]

        handleLaunchIntent(intent)

        setContent {
            val prefs by mainViewModel.prefsState.collectAsStateWithLifecycle()
            val currentTab by mainViewModel.currentTab.collectAsStateWithLifecycle()
            val showBirthday by mainViewModel.showBirthdayOverlay.collectAsStateWithLifecycle()
            val isBirthdayPreview by mainViewModel.isBirthdayPreview.collectAsStateWithLifecycle()
            val justGrantedBirthdayBox by mainViewModel.birthdayJustGrantedBox.collectAsStateWithLifecycle()

            val memesViewModel: MemesViewModel = viewModel(
                factory = MemesViewModel.Factory(container.repository)
            )
            val gamesViewModel: GamesViewModel = viewModel(
                factory = GamesViewModel.Factory(container.repository, container.soundPlayer)
            )
            val goalsViewModel: GoalsViewModel = viewModel(
                factory = GoalsViewModel.Factory(applicationContext, container.repository)
            )
            val giftsViewModel: GiftsViewModel = viewModel(
                factory = GiftsViewModel.Factory(container.repository, container.soundPlayer)
            )

            PastelPawsTheme(themeMode = prefs.themeMode) {
                PastelGradientBackground {
                    PastelPawsShell(
                        currentTab = currentTab,
                        unopenedBoxesCount = prefs.totalUnopenedBoxes,
                        onSelectTab = mainViewModel::selectTab,
                    ) { innerModifier ->
                        when (currentTab) {
                            BottomTab.HOME -> HomeScreen(
                                prefs = prefs,
                                onCycleTheme = mainViewModel::cycleThemeMode,
                                onOpenGiftBoxes = { mainViewModel.selectTab(BottomTab.GIFTS) },
                                modifier = innerModifier,
                            )
                            BottomTab.MEMES -> MemesScreen(
                                viewModel = memesViewModel,
                                modifier = innerModifier,
                            )
                            BottomTab.GAMES -> GamesScreen(
                                viewModel = gamesViewModel,
                                modifier = innerModifier,
                            )
                            BottomTab.GOALS -> GoalsScreen(
                                viewModel = goalsViewModel,
                                modifier = innerModifier,
                            )
                            BottomTab.GIFTS -> GiftsScreen(
                                viewModel = giftsViewModel,
                                onPreviewBirthday = mainViewModel::previewBirthdaySurprise,
                                modifier = innerModifier,
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showBirthday,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        BirthdaySurpriseOverlay(
                            nickname = prefs.nickname,
                            birthdayMessage = prefs.birthdayMessage,
                            isPreview = isBirthdayPreview,
                            justGrantedFreeBox = justGrantedBirthdayBox,
                            onOpenFreeGiftBox = {
                                mainViewModel.dismissBirthdayOverlay()
                                mainViewModel.selectTab(BottomTab.GIFTS)
                            },
                            onDismiss = mainViewModel::dismissBirthdayOverlay,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_BIRTHDAY, false)) {
            mainViewModel.checkTodayBirthday(forceOpen = true)
        } else if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_GOALS, false)) {
            mainViewModel.selectTab(BottomTab.GOALS)
        }
    }
}

@Composable
fun PastelPawsShell(
    currentTab: BottomTab,
    unopenedBoxesCount: Int,
    onSelectTab: (BottomTab) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val pastel = LocalPastelColors.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        if (useRail) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    NavigationRail(
                        containerColor = pastel.glassSurface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        BottomTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    TabIcon(
                                        tab = tab,
                                        selected = selected,
                                        unopenedBoxesCount = unopenedBoxesCount,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content(Modifier.fillMaxSize())
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    val navShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Surface(
                            shape = navShape,
                            color = pastel.glassSurface,
                            border = fancyIridescentBorder(1.2.dp),
                            modifier = Modifier.shadow(
                                elevation = 12.dp,
                                shape = navShape,
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                            ),
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                tonalElevation = 0.dp,
                            ) {
                                BottomTab.entries.forEach { tab ->
                                    val selected = tab == currentTab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { onSelectTab(tab) },
                                        icon = {
                                            TabIcon(
                                                tab = tab,
                                                selected = selected,
                                                unopenedBoxesCount = unopenedBoxesCount,
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.route}"),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                content(Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
private fun TabIcon(
    tab: BottomTab,
    selected: Boolean,
    unopenedBoxesCount: Int,
) {
    val icon = when (tab) {
        BottomTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
        BottomTab.MEMES -> if (selected) Icons.Rounded.PhotoLibrary else Icons.Outlined.PhotoLibrary
        BottomTab.GAMES -> if (selected) Icons.Rounded.SportsEsports else Icons.Outlined.SportsEsports
        BottomTab.GOALS -> if (selected) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle
        BottomTab.GIFTS -> if (selected) Icons.Rounded.CardGiftcard else Icons.Outlined.CardGiftcard
    }

    if (tab == BottomTab.GIFTS && unopenedBoxesCount > 0) {
        BadgedBox(
            badge = {
                Badge { Text(unopenedBoxesCount.toString()) }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tab.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}
