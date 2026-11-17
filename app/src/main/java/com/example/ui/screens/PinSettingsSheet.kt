package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.GiftItemEntity
import com.example.data.local.GiftTier
import com.example.data.preferences.AppPreferencesState

@Composable
fun PinSettingsScreen(
    prefs: AppPreferencesState,
    giftPool: List<GiftItemEntity>,
    onSaveSettings: (
        nickname: String,
        pinCode: String,
        starsPerBox: Int,
        birthdayMessage: String,
        memeFeedUrl: String,
        customQuizUrl: String,
    ) -> Unit,
    onAddGift: (String, GiftTier) -> Unit,
    onDeleteGift: (Long) -> Unit,
    onPreviewBirthday: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler { onClose() }

    var nickname by remember(prefs.nickname) { mutableStateOf(prefs.nickname) }
    var pinCode by remember(prefs.pinCode) { mutableStateOf(prefs.pinCode) }
    var starsPerBoxText by remember(prefs.starsPerBox) { mutableStateOf(prefs.starsPerBox.toString()) }
    var birthdayMessage by remember(prefs.birthdayMessage) { mutableStateOf(prefs.birthdayMessage) }
    var memeFeedUrl by remember(prefs.memeFeedUrl) { mutableStateOf(prefs.memeFeedUrl) }
    var customQuizUrl by remember(prefs.customQuizUrl) { mutableStateOf(prefs.customQuizUrl) }

    var newGiftTitle by remember { mutableStateOf("") }
    var newGiftTier by remember { mutableStateOf(GiftTier.SMALL) }
    var savedBanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pin_settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("settings_back_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close Settings")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Creator Settings", style = MaterialTheme.typography.headlineSmall)
            }
        }

        // Core settings card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Profile, Stars & Feeds", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it; savedBanner = false },
                        label = { Text("Her Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = starsPerBoxText,
                            onValueChange = {
                                starsPerBoxText = it.filter(Char::isDigit)
                                savedBanner = false
                            },
                            label = { Text("Stars per Box") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_stars_per_box_input"),
                        )
                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = {
                                pinCode = it.filter(Char::isDigit).take(8)
                                savedBanner = false
                            },
                            label = { Text("Settings PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    OutlinedTextField(
                        value = birthdayMessage,
                        onValueChange = { birthdayMessage = it; savedBanner = false },
                        label = { Text("March 6 Birthday Message") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_birthday_message_input"),
                    )

                    OutlinedTextField(
                        value = memeFeedUrl,
                        onValueChange = { memeFeedUrl = it; savedBanner = false },
                        label = { Text("Sinhala Meme JSON or RSS URL (optional)") },
                        placeholder = { Text("https://.../memes.json or RSS bridge URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = customQuizUrl,
                        onValueChange = { customQuizUrl = it; savedBanner = false },
                        label = { Text("Custom Vet Quiz JSON URL (optional)") },
                        placeholder = { Text("https://raw.githubusercontent.com/.../quiz.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                nickname,
                                pinCode,
                                starsPerBoxText.toIntOrNull() ?: 50,
                                birthdayMessage,
                                memeFeedUrl,
                                customQuizUrl,
                            )
                            savedBanner = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_button"),
                    ) {
                        Text(if (savedBanner) "Saved!" else "Save Settings")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onPreviewBirthday,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("preview_birthday_button"),
            ) {
                Icon(Icons.Rounded.Cake, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Birthday Surprise")
            }
        }

        // Gift Pool editor
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Mystery Gift Pool", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Small gifts are common (65%), Medium uncommon (27%), Big rare (8%).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = newGiftTitle,
                        onValueChange = { newGiftTitle = it },
                        label = { Text("Add a gift idea") },
                        placeholder = { Text("e.g., Iced latte, Vet pocket guide") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_gift_title_input"),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GiftTier.entries.forEach { tier ->
                            FilterChip(
                                selected = newGiftTier == tier,
                                onClick = { newGiftTier = tier },
                                label = { Text(tier.label) },
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newGiftTitle.isNotBlank()) {
                                onAddGift(newGiftTitle, newGiftTier)
                                newGiftTitle = ""
                            }
                        },
                        enabled = newGiftTitle.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_gift_pool_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Gift List")
                    }
                }
            }
        }

        items(giftPool, key = { it.id }) { gift ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(gift.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        TierBadge(tier = gift.giftTier)
                    }
                    IconButton(onClick = { onDeleteGift(gift.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete gift item",
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
