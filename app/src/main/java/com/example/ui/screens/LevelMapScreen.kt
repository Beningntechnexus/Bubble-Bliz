package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSaveState
import com.example.game.engine.Mulberry32
import com.example.game.model.MAX_LEVELS
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.TreasureChestCanvas
import com.example.ui.theme.GoldYellow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LevelMapScreen(
    saveState: GameSaveState,
    onBackClick: () -> Unit,
    onLevelClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val unlocked = saveState.profile.unlockedLevel
    val scrollState = rememberScrollState()
    val localDensity = LocalDensity.current
    val gapDp = 104.dp
    val totalHeightDp = (MAX_LEVELS * 104 + 340).dp

    val pulseTransition = rememberInfiniteTransition(label = "curPulse")
    val curScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "curNodeScale"
    )

    val trees = remember {
        val rng = Mulberry32(99)
        List(110) { Triple(rng.nextFloat(), rng.nextFloat(), 16f + rng.nextFloat() * 10f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF3AA84A))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            val mapWidthPx = with(localDensity) { maxWidth.toPx() }
            val gapPx = with(localDensity) { gapDp.toPx() }
            val totalHeightPx = with(localDensity) { totalHeightDp.toPx() }

            fun nodeX(level: Int): Float = mapWidthPx / 2f + sin((level - 1) * 0.6f) * mapWidthPx * 0.27f
            fun nodeY(level: Int): Float = totalHeightPx - with(localDensity) { 130.dp.toPx() } - (level - 1) * gapPx

            LaunchedEffect(unlocked, totalHeightPx) {
                val targetY = (nodeY(unlocked.coerceIn(1, MAX_LEVELS)) - with(localDensity) { maxHeight.toPx() } * 0.55f)
                    .roundToInt()
                    .coerceIn(0, totalHeightPx.roundToInt())
                scrollState.scrollTo(targetY)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeightDp)
            ) {
                // World background, rivers, winding road, trees
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color(0xFFA9E3FF),
                            0.04f to Color(0xFFBFF0A0),
                            0.08f to Color(0xFF7FD66B),
                            0.40f to Color(0xFF4CBF4C),
                            1.0f to Color(0xFF3AA84A)
                        )
                    )

                    // Rivers
                    val riverLevels = listOf(17, 37, 57, 77, 95)
                    for (rl in riverLevels) {
                        val ry = nodeY(rl) - gapPx / 2f
                        val riverPath = Path().apply {
                            moveTo(-20f, ry)
                            quadraticTo(size.width * 0.25f, ry - 26f, size.width * 0.5f, ry)
                            quadraticTo(size.width * 0.75f, ry + 26f, size.width + 20f, ry)
                        }
                        drawPath(riverPath, Color(0xFF2F8FD8), style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(riverPath, Color(0xFF58C4F5), style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round))
                    }

                    // Winding Path
                    val roadPath = Path().apply {
                        moveTo(nodeX(1), nodeY(1) + 90f)
                        lineTo(nodeX(1), nodeY(1))
                        for (i in 2..MAX_LEVELS) {
                            val px = nodeX(i - 1)
                            val py = nodeY(i - 1)
                            val cx = (px + nodeX(i)) / 2f
                            val cy = (py + nodeY(i)) / 2f
                            quadraticTo(px, py, cx, cy)
                        }
                        lineTo(nodeX(MAX_LEVELS), nodeY(MAX_LEVELS))
                    }
                    drawPath(roadPath, Color(0xFFA86F2E), style = Stroke(width = 38.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(roadPath, Color(0xFFECC784), style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(
                        roadPath,
                        Color(0xB3FFF3CF),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 18f))
                        )
                    )

                    // Decorative Trees
                    for ((fx, fy, rad) in trees) {
                        val tx = fx * size.width
                        val ty = fy * (size.height - 160f) + 80f
                        val approxLevel = (((totalHeightPx - 130.dp.toPx() - ty) / gapPx) + 1f)
                        val roadX = mapWidthPx / 2f + sin((approxLevel - 1f) * 0.6f) * mapWidthPx * 0.27f
                        if (kotlin.math.abs(tx - roadX) < 58.dp.toPx()) continue
                        drawCircle(Color(0xFF1F7A34), radius = rad.dp.toPx() * 0.55f, center = Offset(tx, ty))
                        drawCircle(Color(0xFF47C45B), radius = rad.dp.toPx() * 0.42f, center = Offset(tx - 4f, ty - 5f))
                    }
                }

                // World 2 Castle Teaser at top
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 84.dp)
                ) {
                    Text(
                        text = "World 2 Coming Soon",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF2B1A66)
                    )
                }

                // Milestone Chests every 10 levels
                for (m in 10..MAX_LEVELS step 10) {
                    val mx = nodeX(m) + if (nodeX(m) < mapWidthPx / 2f) 60.dp.value * localDensity.density else -60.dp.value * localDensity.density
                    val my = nodeY(m)
                    Box(
                        modifier = Modifier.offset {
                            IntOffset((mx - 26.dp.toPx()).roundToInt(), (my - 22.dp.toPx()).roundToInt())
                        }
                    ) {
                        TreasureChestCanvas(width = 52.dp, contentDescriptionText = "Level $m milestone chest")
                    }
                }

                // 100 Level Nodes
                for (i in 1..MAX_LEVELS) {
                    val stars = saveState.levelStars[i] ?: 0
                    val isCurrent = i == unlocked && !(unlocked == MAX_LEVELS && stars > 0)
                    val isLocked = i > unlocked
                    val isMilestone = i % 10 == 0
                    val sizeDp = if (isCurrent) 68.dp else 56.dp

                    val cx = nodeX(i)
                    val cy = nodeY(i)
                    val nodeSizePx = with(localDensity) { sizeDp.toPx() }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (cx - nodeSizePx / 2f).roundToInt(),
                                    (cy - nodeSizePx / 2f).roundToInt()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Stars above unlocked/completed nodes
                        if (!isLocked && !isCurrent) {
                            Row(
                                modifier = Modifier.offset(y = (-34).dp),
                                horizontalArrangement = Arrangement.spacedBy((-2).dp)
                            ) {
                                for (k in 1..3) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = if (k <= stars) GoldYellow else Color(0xFF3A2A66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        val nodeGradient = when {
                            isCurrent -> Brush.radialGradient(listOf(Color(0xFFFFF4A0), Color(0xFFFFC400), Color(0xFFE07B00)))
                            isLocked -> Brush.radialGradient(listOf(Color(0xFFD0D0D8), Color(0xFF8A8A99), Color(0xFF5A5A6A)))
                            else -> Brush.radialGradient(listOf(Color(0xFF8FD0FF), Color(0xFF2A7CFF), Color(0xFF1646B8)))
                        }
                        val rimColor = when {
                            isCurrent -> Color.White
                            isLocked -> Color(0xFFAAAAAA)
                            isMilestone -> Color(0xFFFF9D00)
                            else -> Color(0xFFFFD54A)
                        }

                        Box(
                            modifier = Modifier
                                .testTag("level_node_$i")
                                .size(sizeDp)
                                .graphicsLayer {
                                    if (isCurrent) {
                                        scaleX = curScale
                                        scaleY = curScale
                                    }
                                }
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(nodeGradient)
                                .border(if (isCurrent || isMilestone) 4.dp else 3.dp, rimColor, CircleShape)
                                .clickable { onLevelClick(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLocked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Level $i locked",
                                        tint = Color(0xFFE6E6EE),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$i",
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                        color = Color(0xFFE6E6EE)
                                    )
                                }
                            } else {
                                Text(
                                    text = "$i",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = if (isCurrent) 24.sp else 20.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fixed Top Header Sign
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlossyCircleButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescriptionText = "Back to home",
                onClick = onBackClick,
                tag = "map_back_button"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFC98A45), Color(0xFF8A5222))))
                    .border(3.dp, Color(0xFF5D3513), RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("World 1", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFE9B8))
                    Text("Green Valley", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }

        // Floating Total Stars Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(14.dp)
                .shadow(6.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF6B3FD9), Color(0xFF3D1CA0))))
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = "Total stars",
                tint = GoldYellow,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "${saveState.totalStars} / ${MAX_LEVELS * 3}",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = GoldYellow
            )
        }
    }
}
