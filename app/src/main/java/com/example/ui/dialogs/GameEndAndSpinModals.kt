package com.example.ui.dialogs

import android.app.Activity
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.SPIN_SEGMENTS
import com.example.ui.components.CartoonMascot
import com.example.ui.components.CoinBadgeIcon
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlossyModalCard
import com.example.ui.components.GlossyStyle
import com.example.ui.components.MascotMood
import com.example.ui.theme.GoldYellow
import com.example.ui.viewmodel.BubbleBlitzViewModel
import com.example.ui.viewmodel.GameOverDisplayData
import com.example.ui.viewmodel.SpinUiState
import com.example.ui.viewmodel.VictoryDisplayData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VictoryModal(
    data: VictoryDisplayData,
    activity: Activity?,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onWatchAdDoubleCoins: (Activity) -> Unit
) {
    GlossyModalCard {
        // Star Rating Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..3) {
                val earned = i <= data.stars
                val starScale by animateFloatAsState(
                    targetValue = if (earned) 1.1f else 0.9f,
                    animationSpec = spring(dampingRatio = 0.45f),
                    label = "star$i"
                )
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (earned) GoldYellow else Color(0xFF3B2A7A),
                    modifier = Modifier.size((46 * starScale).dp)
                )
            }
        }

        // Ribbon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF8A4DFF), Color(0xFF5320C0))))
                .border(2.5.dp, GoldYellow, RoundedCornerShape(14.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Congratulations!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFFE36A)
            )
        }

        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (data.isDaily) "Daily Challenge Completed!" else "Level ${data.levelNumber} Completed!",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = "%,d".format(data.score),
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFFFFE36A)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoinBadgeIcon(26.dp)
                    Text("+${data.coinsEarned}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                for (b in data.unlockedBoosters) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(boosterIcon(b), contentDescription = b.title, tint = GoldYellow, modifier = Modifier.size(24.dp))
                        Text("+1", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                }
            }
            if (data.isFinalLevel) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "You conquered Green Valley! More worlds coming soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(10.dp))
            GlossyButton(
                text = if (data.isDaily || data.isFinalLevel) "⌂  HOME" else "▶  NEXT LEVEL",
                onClick = onNextLevel,
                style = GlossyStyle.GREEN,
                tag = "victory_next_button",
                modifier = Modifier.fillMaxWidth()
            )
            if (activity != null) {
                Spacer(Modifier.height(8.dp))
                GlossyButton(
                    text = if (data.doubledCoins) "COINS DOUBLED ✔" else "WATCH AD – DOUBLE YOUR COINS",
                    onClick = { onWatchAdDoubleCoins(activity) },
                    enabled = !data.doubledCoins,
                    style = GlossyStyle.PURPLE,
                    fontSize = 14.sp,
                    tag = "victory_double_coins_ad_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!data.isDaily) {
                Spacer(Modifier.height(8.dp))
                GlossyButton(
                    text = "↻  REPLAY LEVEL",
                    onClick = onReplay,
                    style = GlossyStyle.BLUE,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(6.dp))
            CartoonMascot(
                mood = MascotMood.WINK,
                size = 86.dp,
                contentDescriptionText = "Celebrating mascot"
            )
        }
    }
}

@Composable
fun GameOverModal(
    data: GameOverDisplayData,
    activity: Activity?,
    onWatchAdExtraShots: (Activity) -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    GlossyModalCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFF5A5A), Color(0xFFC01A2A))))
                .border(2.5.dp, Color(0xFFFFB3B3), RoundedCornerShape(14.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Game Over!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val why = if (data.reason == com.example.game.model.EndReason.DANGER_LINE) {
                "The bubbles reached the danger line!"
            } else {
                "You ran out of shots!"
            }
            Text(why, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Your Score", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Text("%,d".format(data.score), style = MaterialTheme.typography.displayMedium, color = Color(0xFFFFE36A))
            Text(
                text = "Best Score: %,d  •  %s".format(
                    data.bestScore,
                    if (data.isDaily) "Daily Challenge" else "Level ${data.levelNumber}"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            if (data.canOfferExtraShots && activity != null) {
                GlossyButton(
                    text = "WATCH AD – GET 5 EXTRA SHOTS",
                    onClick = { onWatchAdExtraShots(activity) },
                    style = GlossyStyle.GREEN,
                    fontSize = 14.sp,
                    tag = "gameover_extra_shots_ad_button",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            GlossyButton(
                text = "↻  TRY AGAIN",
                onClick = onRetry,
                style = GlossyStyle.PURPLE,
                tag = "gameover_retry_button",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlossyButton(
                text = "⌂  HOME",
                onClick = onHome,
                style = GlossyStyle.BLUE,
                tag = "gameover_home_button",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            CartoonMascot(
                mood = MascotMood.SAD,
                size = 82.dp,
                contentDescriptionText = "Sad mascot"
            )
        }
    }
}

@Composable
fun LuckySpinModal(
    spinState: SpinUiState,
    activity: Activity?,
    viewModel: BubbleBlitzViewModel,
    onClose: () -> Unit
) {
    val animatedRot by animateFloatAsState(
        targetValue = spinState.targetRotationDeg,
        animationSpec = tween(durationMillis = 4200, easing = CubicBezierEasing(0.12f, 0.7f, 0.1f, 1f)),
        label = "wheelSpin"
    )
    val wheelTextPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    GlossyModalCard {
        ModalHeader("Lucky Spin", onClose = onClose)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                // Wheel Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f - 8.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(Color(0xFF7A3FE0), radius = r + 7.dp.toPx(), center = center)
                    drawCircle(GoldYellow, radius = r + 3.dp.toPx(), center = center)

                    rotate(degrees = animatedRot, pivot = center) {
                        val sweep = 360f / SPIN_SEGMENTS.size
                        for ((i, seg) in SPIN_SEGMENTS.withIndex()) {
                            // -90 puts index 0 centered near top pointer
                            val startAngle = -90f + i * sweep
                            drawArc(
                                color = seg.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = Size(r * 2f, r * 2f)
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.25f),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = Size(r * 2f, r * 2f),
                                style = Stroke(2f)
                            )
                            val midAngleRad = ((startAngle + sweep / 2f) * PI / 180.0).toFloat()
                            val tx = center.x + cos(midAngleRad) * (r * 0.66f)
                            val ty = center.y + sin(midAngleRad) * (r * 0.66f) + 5.dp.toPx()
                            drawContext.canvas.nativeCanvas.apply {
                                wheelTextPaint.textSize = 13.sp.toPx()
                                drawText(seg.label, tx, ty, wheelTextPaint)
                            }
                        }
                    }

                    // Center Gold Hub
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFFFF4A0), Color(0xFFFFC400))),
                        radius = 30.dp.toPx(),
                        center = center
                    )
                    drawCircle(Color.White, radius = 30.dp.toPx(), center = center, style = Stroke(3.dp.toPx()))
                }

                // Red Pointer Triangle at Top
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)
                        .size(30.dp)
                ) {
                    val p = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    }
                    drawPath(p, Color(0xFFFF3B3B))
                    drawPath(p, Color.White, style = Stroke(2f))
                }
            }

            Spacer(Modifier.height(10.dp))
            spinState.lastPrizeLabel?.let { prize ->
                Text(
                    text = "Prize Won: $prize!",
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldYellow
                )
                Spacer(Modifier.height(6.dp))
            }

            if (spinState.canFreeSpin) {
                GlossyButton(
                    text = "FREE SPIN",
                    onClick = { viewModel.spinWheelFree() },
                    enabled = !spinState.isSpinning,
                    style = GlossyStyle.YELLOW,
                    tag = "free_spin_button",
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (spinState.remainingAdSpins > 0 && activity != null) {
                GlossyButton(
                    text = "WATCH AD – EXTRA SPIN (${spinState.remainingAdSpins} left)",
                    onClick = { viewModel.watchAdForExtraSpin(activity) },
                    enabled = !spinState.isSpinning,
                    style = GlossyStyle.PURPLE,
                    fontSize = 15.sp,
                    tag = "ad_spin_button",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                GlossyButton(
                    text = "COME BACK TOMORROW",
                    onClick = {},
                    enabled = false,
                    style = GlossyStyle.PURPLE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "1 free spin per day • up to 2 extra spins with rewarded ads",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
