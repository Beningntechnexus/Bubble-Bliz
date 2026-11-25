package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.engine.Mulberry32
import com.example.game.model.BoosterType
import com.example.game.model.BubbleColor
import com.example.game.model.BubbleKind
import com.example.game.model.CellBubble
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class MascotMood { HAPPY, WINK, SAD }

@Composable
fun SceneBackgroundCanvas(
    isNight: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val skyBrush = if (isNight) {
            Brush.verticalGradient(
                listOf(Color(0xFF150A52), Color(0xFF3A1A85), Color(0xFF6B2FA8))
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFF2A2FD8), Color(0xFF4F7DFF), Color(0xFF93C4FF), Color(0xFFD8B4FF))
            )
        }
        drawRect(brush = skyBrush)

        val rng = Mulberry32(if (isNight) 7 else 3)
        if (isNight) {
            repeat(65) {
                val alpha = 0.3f + rng.nextFloat() * 0.6f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = (rng.nextFloat() * 1.8f + 0.6f) * density * 0.5f,
                    center = Offset(rng.nextFloat() * w, rng.nextFloat() * h * 0.68f)
                )
            }
        } else {
            repeat(6) {
                drawCartoonCloud(
                    cx = rng.nextFloat() * w,
                    cy = h * (0.06f + rng.nextFloat() * 0.36f),
                    scale = (0.75f + rng.nextFloat() * 0.6f) * (w / 380f),
                    alpha = 0.52f + rng.nextFloat() * 0.28f
                )
            }
        }

        val hy = h * 0.72f
        val backHill = Path().apply {
            moveTo(0f, hy)
            cubicTo(w * 0.3f, hy - 80f, w * 0.6f, hy + 25f, w, hy - 55f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(backHill, color = if (isNight) Color(0xFF2A2A78) else Color(0xFF8FE0A0))

        val s = min(1.3f, w / 340f)
        drawFantasyCastle(
            cx = w * 0.14f,
            baseY = hy + 14f,
            scale = s,
            bodyColor = if (isNight) Color(0xFF4A2F94) else Color(0xFFE1D3FF),
            roofColor = if (isNight) Color(0xFF2A1A66) else Color(0xFF7B3FE4),
            windowColor = if (isNight) Color(0xFFFFD93A) else Color(0xFF7B3FE4)
        )
        drawFantasyCastle(
            cx = w * 0.86f,
            baseY = hy - 14f,
            scale = s * 0.82f,
            bodyColor = if (isNight) Color(0xFF4A2F94) else Color(0xFFE1D3FF),
            roofColor = if (isNight) Color(0xFF2A1A66) else Color(0xFF7B3FE4),
            windowColor = if (isNight) Color(0xFFFFD93A) else Color(0xFF7B3FE4)
        )

        val frontHill = Path().apply {
            moveTo(0f, hy + 50f)
            cubicTo(w * 0.35f, hy + 10f, w * 0.65f, hy + 95f, w, hy + 30f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            frontHill,
            brush = Brush.verticalGradient(
                colors = if (isNight) listOf(Color(0xFF1F5A45), Color(0xFF0F2F2C))
                else listOf(Color(0xFF5FD16A), Color(0xFF238F3A)),
                startY = hy,
                endY = h
            )
        )

        repeat(8) {
            val tx = rng.nextFloat() * w
            val ty = hy + 60f + rng.nextFloat() * (h - hy - 60f)
            val ts = (0.8f + rng.nextFloat() * 0.7f) * s
            drawCartoonTree(
                tx = tx,
                ty = ty,
                scale = ts,
                light = if (isNight) Color(0xFF1E6A4A) else Color(0xFF46C25A),
                dark = if (isNight) Color(0xFF134A35) else Color(0xFF2C9A44)
            )
        }

        if (!isNight) {
            val flowerColors = listOf(Color(0xFFFF6FB5), Color(0xFFFFE36A), Color.White, Color(0xFFFF8A3D))
            repeat(26) { i ->
                drawCircle(
                    color = flowerColors[i % flowerColors.size],
                    radius = 3.2f * s,
                    center = Offset(rng.nextFloat() * w, hy + 55f + rng.nextFloat() * (h - hy - 55f))
                )
            }
        }
    }
}

private fun DrawScope.drawCartoonCloud(cx: Float, cy: Float, scale: Float, alpha: Float) {
    val col = Color.White.copy(alpha = alpha)
    val blobs = listOf(
        Triple(0f, 0f, 30f),
        Triple(28f, -8f, 26f),
        Triple(-28f, -4f, 22f),
        Triple(54f, 4f, 20f),
        Triple(-52f, 6f, 18f)
    )
    for ((dx, dy, r) in blobs) {
        drawCircle(col, radius = r * scale, center = Offset(cx + dx * scale, cy + dy * scale))
    }
}

private fun DrawScope.drawFantasyCastle(
    cx: Float,
    baseY: Float,
    scale: Float,
    bodyColor: Color,
    roofColor: Color,
    windowColor: Color
) {
    fun tower(px: Float, tw: Float, th: Float, rh: Float) {
        drawRect(bodyColor, topLeft = Offset(px - tw / 2f, baseY - th), size = Size(tw, th))
        val roof = Path().apply {
            moveTo(px - tw / 2f - 4f * scale, baseY - th)
            lineTo(px, baseY - th - rh)
            lineTo(px + tw / 2f + 4f * scale, baseY - th)
            close()
        }
        drawPath(roof, roofColor)
        drawRect(windowColor, topLeft = Offset(px - 3f * scale, baseY - th + 12f * scale), size = Size(6f * scale, 10f * scale))
    }
    drawRect(bodyColor, topLeft = Offset(cx - 34f * scale, baseY - 60f * scale), size = Size(68f * scale, 60f * scale))
    tower(cx - 34f * scale, 22f * scale, 100f * scale, 40f * scale)
    tower(cx + 34f * scale, 22f * scale, 100f * scale, 40f * scale)
    tower(cx, 26f * scale, 130f * scale, 50f * scale)
    drawRect(windowColor, topLeft = Offset(cx - 16f * scale, baseY - 40f * scale), size = Size(6f * scale, 10f * scale))
    drawRect(windowColor, topLeft = Offset(cx + 10f * scale, baseY - 40f * scale), size = Size(6f * scale, 10f * scale))
}

private fun DrawScope.drawCartoonTree(tx: Float, ty: Float, scale: Float, light: Color, dark: Color) {
    drawRect(Color(0xFF6A3D1A), topLeft = Offset(tx - 4f * scale, ty - 12f * scale), size = Size(8f * scale, 14f * scale))
    drawCircle(dark, radius = 20f * scale, center = Offset(tx, ty - 26f * scale))
    drawCircle(light, radius = 16f * scale, center = Offset(tx - 6f * scale, ty - 30f * scale))
}

@Composable
fun CartoonMascot(
    mood: MascotMood,
    size: Dp,
    contentDescriptionText: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val w = this.size.width
        val s = w / 200f
        fun p(v: Float) = v * s

        // Drop shadow
        drawOval(
            color = Color(0x4D000000),
            topLeft = Offset(p(38f), p(182f)),
            size = Size(p(124f), p(14f))
        )

        // Glossy Purple Body
        val bodyBrush = Brush.radialGradient(
            colors = listOf(Color(0xFFE7B3FF), Color(0xFFA23DF0), Color(0xFF561AA0)),
            center = Offset(p(72f), p(62f)),
            radius = p(135f)
        )
        drawCircle(bodyBrush, radius = p(84f), center = Offset(p(100f), p(100f)))
        drawCircle(Color(0xFF4A1590), radius = p(84f), center = Offset(p(100f), p(100f)), style = Stroke(width = p(4f)))

        // Specular Highlight
        withTransform({ rotate(-30f, Offset(p(62f), p(48f))) }) {
            drawOval(
                color = Color.White.copy(alpha = 0.48f),
                topLeft = Offset(p(36f), p(34f)),
                size = Size(p(52f), p(28f))
            )
        }

        // Eyes
        fun drawOpenEye(cx: Float) {
            drawOval(Color.White, topLeft = Offset(p(cx - 17f), p(69f)), size = Size(p(34f), p(42f)))
            drawOval(Color(0xFF3A0D6E), topLeft = Offset(p(cx - 17f), p(69f)), size = Size(p(34f), p(42f)), style = Stroke(p(3f)))
            val pupilY = if (mood == MascotMood.SAD) p(98f) else p(94f)
            drawCircle(Color(0xFF2A0A50), radius = p(11f), center = Offset(p(cx + 2f), pupilY))
            drawCircle(Color.White, radius = p(4.5f), center = Offset(p(cx + 6f), pupilY - p(6f)))
        }
        if (mood == MascotMood.WINK) {
            val winkPath = Path().apply {
                moveTo(p(55f), p(92f))
                quadraticTo(p(72f), p(76f), p(89f), p(92f))
            }
            drawPath(winkPath, Color(0xFF3A0D6E), style = Stroke(width = p(7f), cap = StrokeCap.Round))
        } else {
            drawOpenEye(72f)
        }
        drawOpenEye(128f)

        // Cheeks
        drawCircle(Color(0x8CFF6FB5), radius = p(11f), center = Offset(p(46f), p(118f)))
        drawCircle(Color(0x8CFF6FB5), radius = p(11f), center = Offset(p(154f), p(118f)))

        // Mouth
        if (mood == MascotMood.SAD) {
            val frown = Path().apply {
                moveTo(p(76f), p(148f))
                quadraticTo(p(100f), p(124f), p(124f), p(148f))
            }
            drawPath(frown, Color(0xFF3A0D6E), style = Stroke(width = p(6f), cap = StrokeCap.Round))
            drawCircle(Color(0xFF5AC8FF), radius = p(6f), center = Offset(p(56f), p(122f)))
        } else {
            val smile = Path().apply {
                moveTo(p(68f), p(122f))
                quadraticTo(p(100f), p(170f), p(132f), p(122f))
                close()
            }
            drawPath(smile, Color(0xFF7A0F3A))
            drawPath(smile, Color(0xFF3A0D6E), style = Stroke(width = p(4f), join = StrokeJoin.Round))
            val tongue = Path().apply {
                moveTo(p(84f), p(142f))
                quadraticTo(p(100f), p(132f), p(116f), p(142f))
                quadraticTo(p(100f), p(156f), p(84f), p(142f))
                close()
            }
            drawPath(tongue, Color(0xFFFF7AA8))
        }

        // Thumbs-up hand for happy/wink
        if (mood != MascotMood.SAD) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(p(146f), p(134f)),
                size = Size(p(34f), p(32f)),
                cornerRadius = CornerRadius(p(12f), p(12f))
            )
            drawRoundRect(
                color = Color(0xFFC9B9E8),
                topLeft = Offset(p(146f), p(134f)),
                size = Size(p(34f), p(32f)),
                cornerRadius = CornerRadius(p(12f), p(12f)),
                style = Stroke(p(3f))
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(p(153f), p(108f)),
                size = Size(p(14f), p(34f)),
                cornerRadius = CornerRadius(p(7f), p(7f))
            )
            drawRoundRect(
                color = Color(0xFFC9B9E8),
                topLeft = Offset(p(153f), p(108f)),
                size = Size(p(14f), p(34f)),
                cornerRadius = CornerRadius(p(7f), p(7f)),
                style = Stroke(p(3f))
            )
        }
    }
}

@Composable
fun PlayerAvatarCanvas(
    size: Dp,
    contentDescriptionText: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val w = this.size.width
        val s = w / 100f
        fun p(v: Float) = v * s

        drawCircle(Color(0xFF7BD1FF), radius = p(50f), center = Offset(p(50f), p(50f)))
        drawCircle(Color(0xFFC98A5B), radius = p(29f), center = Offset(p(50f), p(58f)))
        val hair = Path().apply {
            moveTo(p(19f), p(48f))
            quadraticTo(p(18f), p(12f), p(50f), p(12f))
            quadraticTo(p(82f), p(12f), p(81f), p(48f))
            quadraticTo(p(70f), p(30f), p(50f), p(30f))
            quadraticTo(p(30f), p(30f), p(19f), p(48f))
            close()
        }
        drawPath(hair, Color(0xFF2B1B12))
        drawCircle(Color(0xFF222222), radius = p(4f), center = Offset(p(40f), p(58f)))
        drawCircle(Color(0xFF222222), radius = p(4f), center = Offset(p(60f), p(58f)))
        val smile = Path().apply {
            moveTo(p(38f), p(70f))
            quadraticTo(p(50f), p(82f), p(62f), p(70f))
        }
        drawPath(smile, Color(0xFF7A2A1A), style = Stroke(width = p(4f), cap = StrokeCap.Round))
    }
}

@Composable
fun TreasureChestCanvas(
    width: Dp,
    contentDescriptionText: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(width, width * 0.8f)
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val s = size.width / 100f
        fun p(v: Float) = v * s

        drawOval(Color(0x66000000), topLeft = Offset(p(10f), p(69f)), size = Size(p(80f), p(10f)))
        drawRoundRect(
            color = Color(0xFFA45A1F),
            topLeft = Offset(p(10f), p(34f)),
            size = Size(p(80f), p(38f)),
            cornerRadius = CornerRadius(p(6f), p(6f))
        )
        drawRoundRect(
            color = Color(0xFF5A2C0A),
            topLeft = Offset(p(10f), p(34f)),
            size = Size(p(80f), p(38f)),
            cornerRadius = CornerRadius(p(6f), p(6f)),
            style = Stroke(p(3f))
        )
        val lid = Path().apply {
            moveTo(p(10f), p(38f))
            quadraticTo(p(50f), p(-4f), p(90f), p(38f))
            close()
        }
        drawPath(lid, Color(0xFFC26F2A))
        drawPath(lid, Color(0xFF5A2C0A), style = Stroke(p(3f)))

        drawRect(Color(0xFFFFC400), topLeft = Offset(p(10f), p(38f)), size = Size(p(80f), p(8f)))
        drawRect(Color(0xCCFFC400), topLeft = Offset(p(20f), p(34f)), size = Size(p(8f), p(38f)))
        drawRect(Color(0xCCFFC400), topLeft = Offset(p(72f), p(34f)), size = Size(p(8f), p(38f)))
        drawRoundRect(
            color = Color(0xFFFFD93A),
            topLeft = Offset(p(40f), p(36f)),
            size = Size(p(20f), p(22f)),
            cornerRadius = CornerRadius(p(4f), p(4f))
        )
        drawRoundRect(
            color = Color(0xFF8A5A00),
            topLeft = Offset(p(40f), p(36f)),
            size = Size(p(20f), p(22f)),
            cornerRadius = CornerRadius(p(4f), p(4f)),
            style = Stroke(p(3f))
        )
        drawCircle(Color(0xFF5A2C0A), radius = p(4f), center = Offset(p(50f), p(47f)))
    }
}

fun DrawScope.drawGlossyBubble(
    center: Offset,
    radius: Float,
    cell: CellBubble?,
    colorOverride: BubbleColor? = null,
    booster: BoosterType? = null,
    alpha: Float = 1f
) {
    if (radius <= 0.5f) return

    if (booster == BoosterType.RAINBOW) {
        val segColors = BubbleColor.entries.map { it.mid }
        for (i in 0 until 6) {
            drawArc(
                color = segColors[i],
                startAngle = i * 60f,
                sweepAngle = 60f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                alpha = alpha
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f * alpha), Color.Black.copy(alpha = 0.16f * alpha)),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                radius = radius * 1.1f
            ),
            radius = radius,
            center = center
        )
        drawCircle(Color.White.copy(alpha = alpha), radius = radius - 1f, center = center, style = Stroke(2f))
        drawBubbleGlossOverlay(center, radius, alpha)
        return
    }

    if (booster == BoosterType.BOMB) {
        drawBombBody(center, radius * 1.15f, alpha)
        drawCircle(Color(0xFFFFD93A).copy(alpha = alpha), radius = radius - 1f, center = center, style = Stroke(2.2f))
        return
    }

    if (booster == BoosterType.LIGHTNING) {
        drawColoredBubbleBody(center, radius, Color(0xFFFFF7A8), Color(0xFFFFC400), Color(0xFFC27A00), alpha)
        drawBubbleGlossOverlay(center, radius, alpha)
        val bolt = Path().apply {
            moveTo(center.x + radius * 0.15f, center.y - radius * 0.68f)
            lineTo(center.x - radius * 0.36f, center.y + radius * 0.06f)
            lineTo(center.x - radius * 0.02f, center.y + radius * 0.06f)
            lineTo(center.x - radius * 0.15f, center.y + radius * 0.68f)
            lineTo(center.x + radius * 0.38f, center.y - radius * 0.12f)
            lineTo(center.x + radius * 0.05f, center.y - radius * 0.12f)
            close()
        }
        drawPath(bolt, Color.White.copy(alpha = alpha))
        drawPath(bolt, Color(0xFFE06A00).copy(alpha = alpha), style = Stroke(1.8f))
        return
    }

    if (booster == BoosterType.FIRE) {
        drawColoredBubbleBody(center, radius, Color(0xFFFFD08A), Color(0xFFFF5A00), Color(0xFF8A1A00), alpha)
        drawBubbleGlossOverlay(center, radius, alpha)
        val outerFlame = Path().apply {
            moveTo(center.x, center.y - radius * 0.68f)
            cubicTo(
                center.x + radius * 0.42f, center.y - radius * 0.25f,
                center.x + radius * 0.55f, center.y + radius * 0.12f,
                center.x, center.y + radius * 0.6f
            )
            cubicTo(
                center.x - radius * 0.55f, center.y + radius * 0.12f,
                center.x - radius * 0.42f, center.y - radius * 0.25f,
                center.x, center.y - radius * 0.68f
            )
            close()
        }
        drawPath(outerFlame, Color(0xFFFFE36A).copy(alpha = alpha))
        drawCircle(Color.White.copy(alpha = 0.9f * alpha), radius = radius * 0.24f, center = Offset(center.x, center.y + radius * 0.18f))
        return
    }

    if (cell?.kind == BubbleKind.STONE) {
        drawColoredBubbleBody(center, radius, Color(0xFFC9CCD6), Color(0xFF8A8FA0), Color(0xFF444857), alpha)
        val crack = Path().apply {
            moveTo(center.x - radius * 0.5f, center.y - radius * 0.2f)
            lineTo(center.x - radius * 0.1f, center.y)
            lineTo(center.x - radius * 0.2f, center.y + radius * 0.5f)
            moveTo(center.x - radius * 0.1f, center.y)
            lineTo(center.x + radius * 0.45f, center.y + radius * 0.15f)
        }
        drawPath(crack, Color(0x991E202D), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
        drawBubbleGlossOverlay(center, radius, alpha * 0.55f)
        return
    }

    val col = colorOverride ?: cell?.color ?: BubbleColor.RED
    drawColoredBubbleBody(center, radius, col.light, col.mid, col.dark, alpha)
    drawBubbleGlossOverlay(center, radius, alpha)
    if (cell?.hasBomb == true) {
        drawBombBody(center, radius * 0.62f, alpha)
    }
}

private fun DrawScope.drawColoredBubbleBody(
    center: Offset,
    radius: Float,
    light: Color,
    mid: Color,
    dark: Color,
    alpha: Float
) {
    val brush = Brush.radialGradient(
        0.0f to light.copy(alpha = alpha),
        0.55f to mid.copy(alpha = alpha),
        1.0f to dark.copy(alpha = alpha),
        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
        radius = radius * 1.35f
    )
    drawCircle(brush = brush, radius = radius - 0.5f, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.42f * alpha),
        radius = radius - 0.5f,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawBubbleGlossOverlay(center: Offset, radius: Float, alpha: Float) {
    drawArc(
        color = Color.White.copy(alpha = 0.28f * alpha),
        startAngle = 36f,
        sweepAngle = 108f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
        size = Size(radius * 1.6f, radius * 1.6f),
        style = Stroke(width = radius * 0.12f, cap = StrokeCap.Round)
    )
    withTransform({
        rotate(-34f, pivot = Offset(center.x - radius * 0.33f, center.y - radius * 0.42f))
    }) {
        drawOval(
            color = Color.White.copy(alpha = 0.82f * alpha),
            topLeft = Offset(center.x - radius * 0.67f, center.y - radius * 0.62f),
            size = Size(radius * 0.68f, radius * 0.4f)
        )
    }
    drawCircle(
        color = Color.White.copy(alpha = 0.55f * alpha),
        radius = radius * 0.09f,
        center = Offset(center.x + radius * 0.4f, center.y + radius * 0.4f)
    )
}

private fun DrawScope.drawBombBody(center: Offset, radius: Float, alpha: Float) {
    val bodyBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF777777).copy(alpha = alpha), Color(0xFF111111).copy(alpha = alpha)),
        center = Offset(center.x - radius * 0.22f, center.y - radius * 0.2f),
        radius = radius * 0.9f
    )
    val bodyCenter = Offset(center.x, center.y + radius * 0.1f)
    drawCircle(bodyBrush, radius = radius * 0.6f, center = bodyCenter)
    drawCircle(Color.White.copy(alpha = alpha), radius = radius * 0.6f, center = bodyCenter, style = Stroke(1.5f))

    val fuse = Path().apply {
        moveTo(center.x + radius * 0.2f, center.y - radius * 0.42f)
        quadraticTo(center.x + radius * 0.48f, center.y - radius * 0.78f, center.x + radius * 0.72f, center.y - radius * 0.58f)
    }
    drawPath(fuse, Color(0xFFD8B070).copy(alpha = alpha), style = Stroke(width = 2.6f, cap = StrokeCap.Round))
    drawCircle(Color(0xFFFFB300).copy(alpha = alpha), radius = radius * 0.17f, center = Offset(center.x + radius * 0.75f, center.y - radius * 0.6f))
    drawCircle(Color.White.copy(alpha = alpha), radius = radius * 0.08f, center = Offset(center.x + radius * 0.75f, center.y - radius * 0.6f))
}
