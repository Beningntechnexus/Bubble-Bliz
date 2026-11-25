package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBgBottom
import com.example.ui.theme.CardBgTop
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.SoftLavender

enum class GlossyStyle(
    val top: Color,
    val bottom: Color,
    val lip: Color,
    val rim: Color
) {
    GREEN(Color(0xFF7BE34A), Color(0xFF2FA51D), Color(0xFF1B6E12), Color(0x66C4F8A0)),
    YELLOW(Color(0xFFFFD93A), Color(0xFFFF9D00), Color(0xFFB86400), Color(0x88FFF3A0)),
    PURPLE(Color(0xFFA45CFF), Color(0xFF6A2BD6), Color(0xFF3F1594), Color(0x55FFFFFF)),
    RED(Color(0xFFFF6A6A), Color(0xFFD3202C), Color(0xFF7D0F19), Color(0x55FFFFFF)),
    BLUE(Color(0xFF4FA3FF), Color(0xFF1659D8), Color(0xFF0C2F8A), Color(0x55FFFFFF))
}

@Composable
fun GlossyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlossyStyle = GlossyStyle.GREEN,
    enabled: Boolean = true,
    fontSize: TextUnit = 19.sp,
    minHeight: Dp = 52.dp,
    subtitle: String? = null,
    tag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(),
        label = "btnScale"
    )
    val shape = RoundedCornerShape(999.dp)
    val baseModifier = modifier
        .let { if (tag != null) it.testTag(tag) else it }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.52f
        }
        .minimumInteractiveComponentSize()

    Box(
        modifier = baseModifier
            .shadow(elevation = 8.dp, shape = shape)
            .background(style.lip, shape)
            .padding(bottom = 5.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(style.top, style.bottom)))
            .border(2.5.dp, style.rim, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .heightIn(min = minHeight)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top glossy highlight band
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.86f)
                .height(14.dp)
                .offset(y = 3.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.48f), Color.White.copy(alpha = 0.04f))
                    )
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = fontSize),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GlossyCircleButton(
    icon: ImageVector,
    contentDescriptionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "circleScale")

    Box(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .size(size)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(6.dp, CircleShape)
            .background(Color(0xFF2A0F78), CircleShape)
            .padding(bottom = 3.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFFB88CFF), Color(0xFF6A2FE0), Color(0xFF3D17A0))
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescriptionText,
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun GlossyMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    style: GlossyStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    tag: String? = null
) {
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "rowScale")

    Box(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(6.dp, shape)
            .background(style.lip, shape)
            .padding(bottom = 4.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(style.top, style.bottom)))
            .border(2.dp, style.rim, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun CoinBadgeIcon(size: Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFFFFF7A8), Color(0xFFFFC400), Color(0xFFB87800))
                )
            )
            .border(1.8.dp, Color(0xFFFFF3A0), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$",
            fontSize = (size.value * 0.56f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF7A4800)
        )
    }
}

@Composable
fun CoinPill(
    coins: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .testTag("coin_pill")
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF6B3FD9), Color(0xFF3D1CA0))))
            .border(2.dp, Color.White.copy(alpha = 0.38f), shape)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
            .semantics { contentDescription = "$coins coins, open shop" }
    ) {
        CoinBadgeIcon(24.dp)
        Text(
            text = "%,d".format(coins),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF38C82E)),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
fun GlossyModalCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .shadow(16.dp, shape)
            .background(Color(0xFF1A0A5C), shape)
            .padding(bottom = 6.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CardBgTop, CardBgBottom)))
            .border(3.dp, SoftLavender, shape)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun ToastBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    if (message == null) return
    Box(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEE1D0E62))
            .border(2.dp, GoldYellow, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}
