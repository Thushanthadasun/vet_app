package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.GiftTier
import com.example.data.local.OpenedGiftCardEntity
import com.example.ui.components.FancyGlassCard
import com.example.ui.components.FancyGradientPillButton
import com.example.ui.components.FancyPillBadge
import com.example.ui.components.fancyIridescentBorder
import com.example.ui.theme.LocalPastelColors

@Composable
fun GiftsScreen(
    viewModel: GiftsViewModel,
    onPreviewBirthday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pastel = LocalPastelColors.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val giftPool by viewModel.giftPool.collectAsStateWithLifecycle()
    val openedCards by viewModel.openedCards.collectAsStateWithLifecycle()
    val isOpeningBox by viewModel.isOpeningBox.collectAsStateWithLifecycle()
    val justRevealedCard by viewModel.justRevealedCard.collectAsStateWithLifecycle()

    var showPinPromptForSettings by remember { mutableStateOf(false) }
    var showAdminSettings by remember { mutableStateOf(false) }
    var pendingDeliveryCard by remember { mutableStateOf<OpenedGiftCardEntity?>(null) }

    if (showAdminSettings) {
        PinSettingsScreen(
            prefs = prefs,
            giftPool = giftPool,
            onSaveSettings = viewModel::saveSettings,
            onAddGift = viewModel::addGift,
            onDeleteGift = viewModel::deleteGift,
            onPreviewBirthday = {
                showAdminSettings = false
                onPreviewBirthday()
            },
            onClose = { showAdminSettings = false },
        )
        return
    }

    val animatedProgress by animateFloatAsState(
        targetValue = prefs.progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "gifts_progress",
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("gifts_screen"),
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
                        text = "Mystery Treat Vault",
                        icon = Icons.Rounded.AutoAwesome,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.gifts_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Every ${prefs.starsPerBox} ★ unlocks a real surprise treat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = pastel.glassSurface,
                    border = fancyIridescentBorder(1.2.dp),
                ) {
                    IconButton(
                        onClick = { showPinPromptForSettings = true },
                        modifier = Modifier.testTag("open_pin_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Creator settings (PIN protected)",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Boutique Mystery Box Hero Card
        item {
            FancyGlassCard(cornerRadius = 30.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AnimatedMysteryBoxGraphic(isOpening = isOpeningBox)

                    Text(
                        text = when {
                            isOpeningBox -> "Unwrapping your Mystery Box..."
                            prefs.totalUnopenedBoxes > 0 ->
                                "${prefs.totalUnopenedBoxes} Mystery Gift Box${if (prefs.totalUnopenedBoxes > 1) "es" else ""} Ready!"
                            else -> "Collect Stars to Unlock"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("mystery_box_status_text"),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceAtLeast(0.03f))
                                .height(11.dp)
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

                    Text(
                        text = "${prefs.starsIntoCurrentBox} / ${prefs.starsPerBox} ★ toward next box (Total: ${prefs.totalStars} ★)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    FancyGradientPillButton(
                        text = if (prefs.totalUnopenedBoxes > 0) stringResource(R.string.gifts_open_box)
                        else "Need ${(prefs.starsPerBox - prefs.starsIntoCurrentBox).coerceAtLeast(1)} more ★",
                        icon = Icons.Rounded.CardGiftcard,
                        enabled = prefs.totalUnopenedBoxes > 0 && !isOpeningBox,
                        onClick = viewModel::openMysteryBox,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_mystery_box_button"),
                    )
                }
            }
        }

        // Opened Gift Cards section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Your Gift Cards (${openedCards.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (openedCards.isEmpty()) {
            item {
                FancyGlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = "Opened gifts appear here as claimable boutique gift tickets with a code you can send on WhatsApp.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            items(openedCards, key = { it.id }) { card ->
                OpenedGiftCardRow(
                    card = card,
                    onSendMessage = { shareGiftCardMessage(context, card, prefs.nickname) },
                    onRequestMarkDelivered = { pendingDeliveryCard = card },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (justRevealedCard != null) {
        val card = justRevealedCard!!
        AlertDialog(
            onDismissRequest = viewModel::dismissRevealedDialog,
            title = { Text("Mystery Box Opened!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TierBadge(tier = card.giftTier)
                    Text(
                        text = card.giftTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Claim code: ${card.claimCode}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Send this gift card message so your friend knows to deliver it!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareGiftCardMessage(context, card, prefs.nickname)
                        viewModel.dismissRevealedDialog()
                    },
                ) {
                    Text(stringResource(R.string.gifts_send_message))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRevealedDialog) {
                    Text("Keep in Cards")
                }
            },
        )
    }

    if (showPinPromptForSettings) {
        PinVerifyDialog(
            title = "Enter Creator PIN",
            subtitle = "Unlock gift list, star threshold, and birthday settings.",
            expectedPin = prefs.pinCode,
            onDismiss = { showPinPromptForSettings = false },
            onVerified = {
                showPinPromptForSettings = false
                showAdminSettings = true
            },
        )
    }

    if (pendingDeliveryCard != null) {
        val target = pendingDeliveryCard!!
        PinVerifyDialog(
            title = if (target.isDelivered) "Mark as Not Delivered?" else "Confirm Delivery",
            subtitle = "Enter PIN to update delivery status for ${target.claimCode}.",
            expectedPin = prefs.pinCode,
            onDismiss = { pendingDeliveryCard = null },
            onVerified = {
                viewModel.setCardDelivered(target.id, !target.isDelivered)
                pendingDeliveryCard = null
            },
        )
    }
}

@Composable
private fun AnimatedMysteryBoxGraphic(isOpening: Boolean) {
    val pastel = LocalPastelColors.current
    val boxColor = MaterialTheme.colorScheme.primaryContainer
    val boxShade = MaterialTheme.colorScheme.secondaryContainer
    val ribbonColor = MaterialTheme.colorScheme.primary
    val goldAccent = pastel.starGold

    val shake = remember { Animatable(0f) }
    val lidLift = remember { Animatable(0f) }

    LaunchedEffect(isOpening) {
        if (isOpening) {
            repeat(4) { i ->
                shake.animateTo(if (i % 2 == 0) 9f else -9f, tween(80))
            }
            shake.animateTo(0f, tween(70))
            lidLift.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        } else {
            shake.snapTo(0f)
            lidLift.snapTo(0f)
        }
    }

    Canvas(modifier = Modifier.size(144.dp)) {
        val w = size.width
        val h = size.height

        // Soft glowing aura behind gift box
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ribbonColor.copy(alpha = 0.25f),
                    pastel.auraLavender,
                    Color.Transparent,
                ),
                center = Offset(w * 0.5f, h * 0.56f),
                radius = w * 0.48f,
            ),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.56f),
        )

        val bodyTop = h * 0.44f
        val bodyHeight = h * 0.44f
        val bodyLeft = w * 0.20f
        val bodyWidth = w * 0.60f

        rotate(degrees = shake.value) {
            // Gift box body with pastel gradient
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(boxColor, boxShade)),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(18f, 18f),
            )
            // Ribbon band
            drawRect(
                color = ribbonColor,
                topLeft = Offset(w * 0.455f, bodyTop),
                size = Size(w * 0.09f, bodyHeight),
            )

            // Animated lid + silk bow loops
            val liftPx = lidLift.value * h * 0.16f
            translate(top = -liftPx) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(boxShade, boxColor)),
                    topLeft = Offset(w * 0.15f, h * 0.30f),
                    size = Size(w * 0.70f, h * 0.15f),
                    cornerRadius = CornerRadius(14f, 14f),
                )
                drawRect(
                    color = ribbonColor,
                    topLeft = Offset(w * 0.455f, h * 0.30f),
                    size = Size(w * 0.09f, h * 0.15f),
                )

                // Left & right ribbon bow loops
                val bowY = h * 0.29f
                val leftBow = Path().apply {
                    moveTo(w * 0.50f, bowY)
                    cubicTo(w * 0.32f, h * 0.12f, w * 0.26f, h * 0.28f, w * 0.46f, bowY)
                }
                val rightBow = Path().apply {
                    moveTo(w * 0.50f, bowY)
                    cubicTo(w * 0.68f, h * 0.12f, w * 0.74f, h * 0.28f, w * 0.54f, bowY)
                }
                drawPath(leftBow, color = ribbonColor, style = Stroke(width = 7f))
                drawPath(rightBow, color = ribbonColor, style = Stroke(width = 7f))
                drawCircle(color = goldAccent, radius = 7f, center = Offset(w * 0.50f, bowY))
            }
        }

        // Tiny gold sparkle dots
        listOf(
            Offset(w * 0.14f, h * 0.26f) to 4.5f,
            Offset(w * 0.85f, h * 0.24f) to 5f,
            Offset(w * 0.88f, h * 0.68f) to 3.8f,
        ).forEach { (pt, r) ->
            drawCircle(color = goldAccent.copy(alpha = 0.85f), radius = r, center = pt)
        }
    }
}

@Composable
private fun OpenedGiftCardRow(
    card: OpenedGiftCardEntity,
    onSendMessage: () -> Unit,
    onRequestMarkDelivered: () -> Unit,
) {
    val pastel = LocalPastelColors.current
    FancyGlassCard(
        cornerRadius = 24.dp,
        modifier = Modifier.testTag("gift_card_${card.id}"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TierBadge(tier = card.giftTier)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (card.isDelivered) pastel.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (card.isDelivered) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = pastel.success,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (card.isDelivered) "Delivered" else "Code: ${card.claimCode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (card.isDelivered) pastel.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                text = card.giftTitle,
                style = MaterialTheme.typography.titleLarge,
            )
            if (card.isDelivered) {
                Text(
                    text = "Code: ${card.claimCode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onSendMessage,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("send_gift_msg_${card.id}"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.gifts_send_message))
                }

                OutlinedButton(
                    onClick = onRequestMarkDelivered,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_delivered_${card.id}"),
                ) {
                    Text(if (card.isDelivered) "Undo" else stringResource(R.string.gifts_mark_delivered))
                }
            }
        }
    }
}

@Composable
fun TierBadge(tier: GiftTier) {
    val bg = when (tier) {
        GiftTier.SMALL -> MaterialTheme.colorScheme.tertiaryContainer
        GiftTier.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        GiftTier.BIG -> MaterialTheme.colorScheme.primaryContainer
    }
    val fg = when (tier) {
        GiftTier.SMALL -> MaterialTheme.colorScheme.onTertiaryContainer
        GiftTier.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
        GiftTier.BIG -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(shape = RoundedCornerShape(10.dp), color = bg) {
        Text(
            text = "${tier.label} Gift",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PinVerifyDialog(
    title: String,
    subtitle: String,
    expectedPin: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    var inputPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = inputPin,
                    onValueChange = {
                        inputPin = it
                        isError = false
                    },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Incorrect PIN (default is 0306)") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputPin.trim() == expectedPin) {
                        onVerified()
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("pin_confirm_button"),
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun shareGiftCardMessage(
    context: Context,
    card: OpenedGiftCardEntity,
    nickname: String,
) {
    val text = "Hi! $nickname opened a Mystery Gift Box in Pastel Paws and unlocked:\n" +
        "${card.giftTitle} (${card.giftTier.label} Gift)\nClaim Code: ${card.claimCode}"

    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val launchedWhatsApp = runCatching {
        context.startActivity(whatsappIntent)
        true
    }.getOrDefault(false)

    if (!launchedWhatsApp) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Send Gift Card"))
        }
    }
}
