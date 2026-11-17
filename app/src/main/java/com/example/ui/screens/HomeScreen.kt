package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SeedData
import com.example.data.preferences.AppPreferencesState
import com.example.ui.components.FancyGlassCard
import com.example.ui.components.FancyPillBadge
import com.example.ui.components.fancyIridescentBorder
import com.example.ui.theme.LocalPastelColors
import com.example.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    prefs: AppPreferencesState,
    onCycleTheme: () -> Unit,
    onOpenGiftBoxes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastel = LocalPastelColors.current
    val daySeed = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    var noteOffset by remember { mutableIntStateOf(0) }
    val encouragingNote = remember(daySeed, noteOffset) {
        val notes = SeedData.dailyEncouragements
        notes[(daySeed + noteOffset) % notes.size]
    }
    val greetingPrefix = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Quiet night"
        }
    }
    val todayPill = remember {
        SimpleDateFormat("EEE, MMM d", Locale.ENGLISH).format(Date())
    }

    val animatedProgress by animateFloatAsState(
        targetValue = prefs.progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "box_progress",
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FancyPillBadge(
                        text = "$todayPill • Vet Era",
                        icon = Icons.Rounded.Pets,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$greetingPrefix, ${prefs.nickname}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag("home_greeting"),
                    )
                }

                val themeIcon = when (prefs.themeMode) {
                    ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                    ThemeMode.LIGHT -> Icons.Outlined.LightMode
                    ThemeMode.DARK -> Icons.Outlined.DarkMode
                }
                val themeLabel = when (prefs.themeMode) {
                    ThemeMode.SYSTEM -> "Auto theme"
                    ThemeMode.LIGHT -> "Light theme"
                    ThemeMode.DARK -> "Dark theme"
                }
                Surface(
                    shape = CircleShape,
                    color = pastel.glassSurface,
                    border = fancyIridescentBorder(1.2.dp),
                    tonalElevation = 4.dp,
                ) {
                    IconButton(
                        onClick = onCycleTheme,
                        modifier = Modifier.testTag("theme_toggle_button"),
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = themeLabel,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Boutique Pastel Hero Banner
        item {
            FancyGlassCard(cornerRadius = 28.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_home_banner),
                        contentDescription = "Pastel veterinary study corner illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                                    )
                                )
                            )
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = pastel.glassSurface,
                        border = fancyIridescentBorder(1.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pastel Paws • Cozy break zone",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Luminous Star Vault & Next Mystery Gift Box Card
        item {
            FancyGlassCard(
                onClick = onOpenGiftBoxes,
                modifier = Modifier.testTag("home_stars_card"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                pastel.starGoldSoft,
                                                MaterialTheme.colorScheme.primaryContainer,
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = "Stars earned",
                                    tint = pastel.starGold,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.home_stars_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${prefs.totalStars} ★",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("home_star_count"),
                                )
                            }
                        }

                        if (prefs.totalUnopenedBoxes > 0) {
                            FancyPillBadge(
                                text = "${prefs.totalUnopenedBoxes} Gift Ready!",
                                icon = Icons.Rounded.CardGiftcard,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.home_next_box_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${prefs.starsIntoCurrentBox} / ${prefs.starsPerBox} ★",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("home_box_progress_text"),
                            )
                        }

                        // Gradient Progress Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(13.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress.coerceAtLeast(0.03f))
                                    .height(13.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.tertiary,
                                            )
                                        )
                                    ),
                            )
                        }

                        val remaining = (prefs.starsPerBox - prefs.starsIntoCurrentBox).coerceAtLeast(0)
                        Text(
                            text = if (prefs.totalUnopenedBoxes > 0) {
                                "Tap to unwrap your Mystery Gift Box!"
                            } else {
                                "$remaining more ★ to unlock your next Mystery Gift Box"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Encouraging Bestie Note Card
        item {
            FancyGlassCard(
                containerColor = Color.Transparent,
                modifier = Modifier.testTag("home_encouragement_card"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                                )
                            )
                        )
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.home_note_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            TextButton(
                                onClick = { noteOffset++ },
                                modifier = Modifier.testTag("next_note_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Show another encouraging note",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Another")
                            }
                        }

                        Text(
                            text = "“$encouragingNote”",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("home_encouragement_text"),
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
