package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.Mulberry32
import com.example.game.model.BubbleColor
import com.example.ui.components.CartoonMascot
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlossyStyle
import com.example.ui.components.MascotMood
import com.example.ui.components.drawGlossyBubble
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldYellow

private data class FloatingBubbleSpec(
    val xFraction: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val phaseOffset: Float,
    val color: BubbleColor
)

@Composable
fun SplashScreen(
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "splashAnim")
    val bobOffset by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val riseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rise"
    )

    val floatingBubbles = remember {
        val rng = Mulberry32(42)
        List(14) { i ->
            FloatingBubbleSpec(
                xFraction = 0.06f + rng.nextFloat() * 0.88f,
                radiusDp = 14f + rng.nextFloat() * 20f,
                speedFactor = 1f + (i % 3) * 0.5f,
                phaseOffset = rng.nextFloat(),
                color = BubbleColor.fromId(i % 6)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Rising background glossy bubbles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (spec in floatingBubbles) {
                val p = (riseProgress * spec.speedFactor + spec.phaseOffset) % 1f
                val y = h + 60f - p * (h + 160f)
                val x = w * spec.xFraction + kotlin.math.sin(p * 6.28f + spec.phaseOffset * 6f) * 18f
                drawGlossyBubble(
                    center = Offset(x, y),
                    radius = spec.radiusDp * density,
                    cell = null,
                    colorOverride = spec.color,
                    alpha = 0.85f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Bitogin Tech publisher badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF7A52FF), Color(0xFF4326C9))))
                        .border(2.5.dp, Color.White, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        text = "Bitogin Tech",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        color = Color.White
                    )
                    Text(
                        text = "Play • Enjoy • Win",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Stylized Title & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.rotate(-3f)
            ) {
                OutlinedTitleWord(
                    text = "BUBBLE",
                    fontSize = 62.sp,
                    fillBrush = Brush.verticalGradient(listOf(Color(0xFFFFF56A), Color(0xFFFFB300)))
                )
                OutlinedTitleWord(
                    text = "BLITZ",
                    fontSize = 74.sp,
                    fillBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFF8FD0FF))),
                    modifier = Modifier.offset(y = (-8).dp)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF7B3FE4), Color(0xFF4A1BA8))))
                        .border(2.5.dp, GoldYellow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Pop. Strategize. Conquer.",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        color = Color(0xFFFFE36A)
                    )
                }
            }

            // Bobbing Mascot
            CartoonMascot(
                mood = MascotMood.HAPPY,
                size = 172.dp,
                contentDescriptionText = "Bubble Blitz mascot smiling",
                modifier = Modifier.offset(y = bobOffset.dp)
            )

            // Big PLAY button
            GlossyButton(
                text = "▶  PLAY",
                onClick = onPlayClick,
                style = GlossyStyle.GREEN,
                fontSize = 28.sp,
                minHeight = 64.dp,
                tag = "play_button",
                modifier = Modifier
                    .widthIn(min = 230.dp, max = 320.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun OutlinedTitleWord(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fillBrush: Brush,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 1.sp,
                drawStyle = Stroke(width = 16f, join = StrokeJoin.Round)
            ),
            color = Color(0xFF3C1AA6)
        )
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 1.sp,
                brush = fillBrush
            )
        )
    }
}
