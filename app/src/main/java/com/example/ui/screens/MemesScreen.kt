package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TagFaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.MemeEntity
import com.example.ui.components.FancyGlassCard
import com.example.ui.components.FancyPillBadge
import com.example.ui.components.fancyIridescentBorder
import com.example.ui.theme.LocalPastelColors
import com.example.ui.theme.SinhalaCaptionStyle
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemesScreen(
    viewModel: MemesViewModel,
    modifier: Modifier = Modifier,
) {
    val pastel = LocalPastelColors.current
    val memes by viewModel.memes.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var captionInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.autoSyncIfNeeded()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                pendingPhotoUri = uri
                captionInput = ""
            }
        },
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("memes_screen"),
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
                        text = "Reddit • Mastodon • Sinhala",
                        icon = Icons.Rounded.LocalFireDepartment,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.memes_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Live social memes + offline cached Sinhala favourites",
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
                        onClick = { viewModel.refreshFromNetwork() },
                        modifier = Modifier.testTag("memes_refresh_button"),
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.memes_refresh),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == MemeFilter.ALL,
                    onClick = { viewModel.setFilter(MemeFilter.ALL) },
                    label = { Text(stringResource(R.string.memes_filter_all)) },
                    modifier = Modifier.testTag("memes_filter_all"),
                )
                FilterChip(
                    selected = filter == MemeFilter.TRENDING,
                    onClick = { viewModel.setFilter(MemeFilter.TRENDING) },
                    label = { Text("Trending Social") },
                    modifier = Modifier.testTag("memes_filter_trending"),
                )
                FilterChip(
                    selected = filter == MemeFilter.FAVORITES,
                    onClick = { viewModel.setFilter(MemeFilter.FAVORITES) },
                    label = { Text(stringResource(R.string.memes_filter_favorites)) },
                    modifier = Modifier.testTag("memes_filter_favorites"),
                )

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("memes_add_photo_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.memes_add_from_gallery))
                }
            }
        }

        item {
            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                    border = fancyIridescentBorder(1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.clearStatusMessage() }) {
                            Text("OK")
                        }
                    }
                }
            }
        }

        if (memes.isEmpty()) {
            item {
                FancyGlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TagFaces,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                        Text(
                            text = when (filter) {
                                MemeFilter.FAVORITES -> "No favourite memes yet"
                                MemeFilter.TRENDING -> "No trending social memes loaded yet"
                                MemeFilter.ALL -> "No memes cached yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = when (filter) {
                                MemeFilter.FAVORITES -> "Tap the heart on any meme to save it here."
                                MemeFilter.TRENDING -> "Tap the Refresh button above when online to fetch live Reddit & Mastodon memes."
                                MemeFilter.ALL -> "Tap Refresh or add a favourite photo from your gallery."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(memes, key = { it.id }) { meme ->
                MemeCard(
                    meme = meme,
                    onToggleFavorite = { viewModel.toggleFavorite(meme) },
                    onDelete = if (meme.isUserAdded) {
                        { viewModel.deleteMeme(meme) }
                    } else null,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (pendingPhotoUri != null) {
        AlertDialog(
            onDismissRequest = { pendingPhotoUri = null },
            title = { Text("Add Meme from Gallery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = pendingPhotoUri,
                        contentDescription = "Selected gallery meme preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    OutlinedTextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        label = { Text("Caption (Sinhala or English)") },
                        textStyle = SinhalaCaptionStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gallery_meme_caption_input"),
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingPhotoUri?.let { uri ->
                            viewModel.addMemeFromGallery(uri, captionInput)
                        }
                        pendingPhotoUri = null
                    },
                    modifier = Modifier.testTag("confirm_add_meme_button"),
                ) {
                    Text("Save Meme")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPhotoUri = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun MemeCard(
    meme: MemeEntity,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    val drawableResId = remember(meme.drawableResName) {
        when (meme.drawableResName) {
            "img_meme_coffee" -> R.drawable.img_meme_coffee
            "img_meme_study" -> R.drawable.img_meme_study
            "img_meme_cat" -> R.drawable.img_meme_cat
            else -> 0
        }
    }

    FancyGlassCard(
        cornerRadius = 26.dp,
        modifier = Modifier.testTag("meme_card_${meme.id}"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when {
                drawableResId != 0 -> {
                    Image(
                        painter = painterResource(id = drawableResId),
                        contentDescription = meme.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp),
                    )
                }
                meme.imageUrlOrPath.isNotBlank() -> {
                    val modelData: Any = if (meme.imageUrlOrPath.startsWith("/")) {
                        File(meme.imageUrlOrPath)
                    } else {
                        meme.imageUrlOrPath
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(modelData)
                            .crossfade(true)
                            .build(),
                        contentDescription = meme.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
                else -> {
                    val moodTag = meme.sourceName.substringAfter("•", "").trim()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.76f),
                                    )
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (moodTag.isNotEmpty()) {
                            FancyPillBadge(
                                text = moodTag,
                                icon = Icons.Rounded.AutoAwesome,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = meme.title,
                            style = SinhalaCaptionStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if ((drawableResId != 0 || meme.imageUrlOrPath.isNotBlank()) && meme.title.isNotBlank()) {
                    Text(
                        text = meme.title,
                        style = SinhalaCaptionStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val hasLink = meme.sourceUrl.startsWith("http://") || meme.sourceUrl.startsWith("https://")
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .let {
                                if (hasLink) {
                                    it.clickable {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(meme.sourceUrl))
                                            )
                                        }
                                    }
                                } else it
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Source: ${meme.sourceName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (hasLink) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Open meme source link",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onDelete != null) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Remove saved gallery meme",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("meme_fav_button_${meme.id}"),
                        ) {
                            Icon(
                                imageVector = if (meme.isFavorite) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = if (meme.isFavorite) {
                                    "Remove from favourites"
                                } else {
                                    "Save to favourites"
                                },
                                tint = if (meme.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
