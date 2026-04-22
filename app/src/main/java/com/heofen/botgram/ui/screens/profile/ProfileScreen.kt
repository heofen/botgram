package com.heofen.botgram.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil.compose.AsyncImage
import com.heofen.botgram.ChatType
import com.heofen.botgram.R
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.ui.theme.botgramBackdropSource
import com.heofen.botgram.ui.theme.botgramLiquidGlass
import com.heofen.botgram.ui.theme.rememberBotgramBackdrop
import com.heofen.botgram.utils.extensions.getInitials
import java.io.File
import java.util.Locale

private data class ProfileLayoutMetrics(
    val screenWidth: Dp,
    val scale: Float,
    val expandedHeaderHeight: Dp,
    val collapsedHeaderHeight: Dp,
    val expandedImageHeight: Dp,
    val collapsedAvatarSize: Dp,
    val collapsedAvatarTop: Dp,
    val expandedNameTop: Dp,
    val collapsedNameTop: Dp,
    val buttonTopExpanded: Dp,
    val buttonTopCollapsed: Dp,
    val buttonWidth: Dp,
    val buttonHeight: Dp,
    val buttonCornerRadius: Dp,
    val buttonHorizontalPadding: Dp,
    val cardHeight: Dp,
    val cardCornerRadius: Dp,
    val cardHorizontalPadding: Dp,
    val cardContentHorizontalPadding: Dp,
    val cardContentTopPadding: Dp,
    val cardSectionSpacing: Dp,
    val cardToButtonSpacing: Dp,
    val heroScrimHeight: Dp
)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val backdrop = rememberBotgramBackdrop()
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val metrics = remember(maxWidth) { profileLayoutMetrics(maxWidth) }
        val density = LocalDensity.current
        val collapseRangePx = with(density) {
            (metrics.expandedHeaderHeight - metrics.collapsedHeaderHeight).toPx()
        }
        var headerOffsetPx by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(collapseRangePx) {
            headerOffsetPx = headerOffsetPx.coerceIn(0f, collapseRangePx)
        }

        val nestedScrollConnection = remember(listState, collapseRangePx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y >= 0f || collapseRangePx <= 0f) return Offset.Zero

                    val previous = headerOffsetPx
                    headerOffsetPx = (headerOffsetPx - available.y).coerceIn(0f, collapseRangePx)
                    val consumed = headerOffsetPx - previous
                    return Offset(x = 0f, y = -consumed)
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y <= 0f || headerOffsetPx <= 0f) return Offset.Zero

                    val previous = headerOffsetPx
                    headerOffsetPx = (headerOffsetPx - available.y).coerceIn(0f, collapseRangePx)
                    val consumedByHeader = previous - headerOffsetPx
                    return Offset(x = 0f, y = consumedByHeader)
                }
            }
        }

        val collapseProgress by remember(collapseRangePx, headerOffsetPx) {
            derivedStateOf {
                if (collapseRangePx <= 0f) 1f else (headerOffsetPx / collapseRangePx).coerceIn(0f, 1f)
            }
        }
        val headerHeight = lerp(
            metrics.expandedHeaderHeight,
            metrics.collapsedHeaderHeight,
            collapseProgress
        )
        val profileInfo = remember(uiState.chat, uiState.user) {
            ProfileInfo.from(uiState.chat, uiState.user)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(nestedScrollConnection)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .botgramBackdropSource(backdrop)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        top = headerHeight,
                        bottom = 24.dp
                    )
                ) {
                    item {
                        ProfileInfoCard(
                            info = profileInfo,
                            metrics = metrics
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                ProfileHeaderContent(
                    info = profileInfo,
                    metrics = metrics,
                    collapseProgress = collapseProgress
                )
            }

            ProfileHeaderControls(
                backdrop = backdrop,
                metrics = metrics,
                collapseProgress = collapseProgress,
                notificationsEnabled = notificationsEnabled,
                onNotificationsClick = { notificationsEnabled = !notificationsEnabled },
                onBackClick = onBackClick
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderContent(
    info: ProfileInfo,
    metrics: ProfileLayoutMetrics,
    collapseProgress: Float
) {
    val headerHeight = lerp(metrics.expandedHeaderHeight, metrics.collapsedHeaderHeight, collapseProgress)
    val nameTop = lerp(metrics.expandedNameTop, metrics.collapsedNameTop, collapseProgress)
    val heroAlpha = 1f - collapseProgress
    val nameHorizontalBias = lerp(-0.92f, 0f, collapseProgress)
    val nameFontSize = lerp(25f, 20f, collapseProgress).sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        ProfileAnimatedAvatar(
            info = info,
            metrics = metrics,
            collapseProgress = collapseProgress
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(lerp(metrics.heroScrimHeight, 0.dp, collapseProgress))
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.68f)
                        )
                    )
                )
                .alpha(heroAlpha)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = nameTop)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = info.displayName,
                modifier = Modifier.align(BiasAlignment(nameHorizontalBias, 0f)),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = nameFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = if (collapseProgress > 0.85f) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@Composable
private fun ProfileHeaderControls(
    backdrop: com.heofen.botgram.ui.theme.BotgramBackdrop,
    metrics: ProfileLayoutMetrics,
    collapseProgress: Float,
    notificationsEnabled: Boolean,
    onNotificationsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val buttonTop = lerp(metrics.buttonTopExpanded, metrics.buttonTopCollapsed, collapseProgress)
    val buttonLabel = if (notificationsEnabled) {
        stringResource(R.string.profile_disable_notifications)
    } else {
        stringResource(R.string.profile_enable_notifications)
    }
    val buttonIcon = if (notificationsEnabled) {
        R.drawable.ic_profile_notifications_on
    } else {
        R.drawable.ic_profile_notifications_off
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(lerp(metrics.expandedHeaderHeight, metrics.collapsedHeaderHeight, collapseProgress))
    ) {
        ProfileNotificationsButton(
            backdrop = backdrop,
            label = buttonLabel,
            iconRes = buttonIcon,
            top = buttonTop,
            metrics = metrics,
            onClick = onNotificationsClick
        )

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(42.dp)
                .clip(CircleShape)
                .botgramLiquidGlass(backdrop = backdrop, shape = CircleShape, blurRadius = 1.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileAnimatedAvatar(
    info: ProfileInfo,
    metrics: ProfileLayoutMetrics,
    collapseProgress: Float
) {
    val imageFile = remember(info.avatarPath) {
        info.avatarPath?.let(::File)?.takeIf(File::exists)
    }
    val animatedWidth = lerp(metrics.screenWidth, metrics.collapsedAvatarSize, collapseProgress)
    val animatedHeight = lerp(metrics.expandedImageHeight, metrics.collapsedAvatarSize, collapseProgress)
    val animatedTop = lerp(0.dp, metrics.collapsedAvatarTop, collapseProgress)
    val animatedCornerRadius = lerp(0.dp, metrics.collapsedAvatarSize / 2, collapseProgress)
    val avatarShape = RoundedCornerShape(animatedCornerRadius)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = animatedTop),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .graphicsLayer {
                    clip = true
                    shape = avatarShape
                }
        ) {
            if (imageFile != null) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = stringResource(R.string.profile_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = info.initials,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 96.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.96f * (1f - collapseProgress))
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun ProfileNotificationsButton(
    backdrop: com.heofen.botgram.ui.theme.BotgramBackdrop,
    label: String,
    iconRes: Int,
    top: Dp,
    metrics: ProfileLayoutMetrics,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = top)
            .padding(horizontal = metrics.buttonHorizontalPadding)
            .height(metrics.buttonHeight)
            .clip(RoundedCornerShape(metrics.buttonCornerRadius))
            .botgramLiquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(metrics.buttonCornerRadius),
                blurRadius = 25.dp
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileInfoCard(
    info: ProfileInfo,
    metrics: ProfileLayoutMetrics
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = metrics.cardHorizontalPadding)
            .heightIn(min = metrics.cardHeight),
        shape = RoundedCornerShape(metrics.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = metrics.cardContentHorizontalPadding,
                    end = metrics.cardContentHorizontalPadding,
                    top = metrics.cardContentTopPadding,
                    bottom = metrics.cardContentTopPadding
                ),
            verticalArrangement = Arrangement.spacedBy(metrics.cardSectionSpacing)
        ) {
            ProfileInfoRow(
                value = info.id,
                label = stringResource(R.string.profile_id_label),
                copyValue = info.id.takeUnless { it == "--" }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            if (info.type != null) {
                ProfileInfoRow(
                    value = info.type,
                    label = stringResource(R.string.profile_type_label),
                    copyValue = null
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            ProfileInfoRow(
                value = info.username,
                label = stringResource(R.string.profile_username_label),
                copyValue = info.username.takeUnless { it == "--" }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            ProfileInfoRow(
                value = info.bio,
                label = stringResource(R.string.profile_description_label),
                copyValue = info.bio.takeUnless { it == "--" }
            )

            if (info.languageCode != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ProfileInfoRow(
                    value = info.languageCode,
                    label = stringResource(R.string.profile_language_label),
                    copyValue = info.languageCode.takeUnless { it == "--" }
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    value: String,
    label: String,
    copyValue: String? = null
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (copyValue != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            val clipboardManager = context.getSystemService(ClipboardManager::class.java)
                            clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, copyValue))
                            Toast.makeText(
                                context,
                                context.getString(R.string.profile_value_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ProfileInfo(
    val displayName: String,
    val initials: String,
    val id: String,
    val type: String?,
    val username: String,
    val bio: String,
    val languageCode: String?,
    val avatarPath: String?
) {
    companion object {
        fun from(chat: Chat?, user: User?): ProfileInfo {
            val displayName = listOfNotNull(
                user?.firstName ?: chat?.firstName,
                user?.lastName ?: chat?.lastName
            ).joinToString(" ").ifBlank {
                chat?.title ?: "--"
            }
            val initials = when {
                user != null -> user.getInitials()
                !chat?.firstName.isNullOrBlank() || !chat?.lastName.isNullOrBlank() -> {
                    listOfNotNull(chat?.firstName?.firstOrNull(), chat?.lastName?.firstOrNull())
                        .joinToString("")
                        .uppercase()
                }
                chat?.title != null -> {
                    chat.title.split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull() }
                        .joinToString("")
                        .uppercase()
                }
                else -> "?"
            }
            val type = when (chat?.type) {
                ChatType.GROUP -> "Группа"
                ChatType.SUPERGROUP -> "Супергруппа"
                ChatType.CHANNEL -> "Канал"
                else -> null
            }
            val username = (user?.username ?: chat?.username)
                ?.takeIf { it.isNotBlank() }
                ?.let { if (it.startsWith("@")) it else "@$it" }
                ?: "--"
            val bio = (chat?.description ?: user?.bio)
                ?.takeIf { it.isNotBlank() }
                ?: "--"
            
            val languageCode = if (chat?.type == ChatType.PRIVATE || user != null) {
                user?.languageCode
                    ?.takeIf { it.isNotBlank() }
                    ?.uppercase(Locale.getDefault())
                    ?: Locale.getDefault().language
                        .takeIf { it.isNotBlank() }
                        ?.uppercase(Locale.getDefault())
                        ?: "--"
            } else null

            return ProfileInfo(
                displayName = displayName,
                initials = initials,
                id = chat?.id?.toString() ?: user?.id?.toString() ?: "--",
                type = type,
                username = username,
                bio = bio,
                languageCode = languageCode,
                avatarPath = user?.avatarLocalPath ?: chat?.avatarLocalPath
            )
        }
    }
}

private fun profileLayoutMetrics(screenWidth: Dp): ProfileLayoutMetrics {
    val scale = screenWidth.value / 1080f
    fun scaled(value: Float): Dp = (value * scale).dp

    return ProfileLayoutMetrics(
        screenWidth = screenWidth,
        scale = scale,
        expandedHeaderHeight = scaled(1323f),
        collapsedHeaderHeight = scaled(947f),
        expandedImageHeight = scaled(1080f),
        collapsedAvatarSize = scaled(272f),
        collapsedAvatarTop = scaled(212f),
        expandedNameTop = scaled(826f),
        collapsedNameTop = scaled(552f),
        buttonTopExpanded = scaled(1035f),
        buttonTopCollapsed = scaled(674f),
        buttonWidth = scaled(877f),
        buttonHeight = scaled(197f),
        buttonCornerRadius = scaled(98.5f),
        buttonHorizontalPadding = scaled(101f),
        cardHeight = scaled(505f),
        cardCornerRadius = scaled(43f),
        cardHorizontalPadding = scaled(37f),
        cardContentHorizontalPadding = scaled(62f),
        cardContentTopPadding = scaled(40f),
        cardSectionSpacing = scaled(26f),
        cardToButtonSpacing = scaled(91f),
        heroScrimHeight = scaled(307f)
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
