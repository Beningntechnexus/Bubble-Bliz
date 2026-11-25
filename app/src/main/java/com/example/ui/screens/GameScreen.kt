package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSaveState
import com.example.game.engine.BubbleGameSession
import com.example.game.engine.GamePhase
import com.example.game.model.BoosterType
import com.example.game.model.GRID_COLS
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.drawGlossyBubble
import com.example.ui.theme.FrostCyan
import com.example.ui.theme.GoldYellow
import com.example.ui.viewmodel.HudState
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun GameScreen(
    session: BubbleGameSession?,
    saveState: GameSaveState,
    hudState: HudState,
    isPaused: Boolean,
    onBoardSized: (Float, Float) -> Unit,
    onFireShot: () -> Unit,
    onSwapBubbles: () -> Unit,
    onBoosterClick: (BoosterType) -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var frameTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(session, isPaused) {
        if (session == null || isPaused) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    session.update(dt)
                }
                lastNanos = now
                frameTick++
            }
        }
    }

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    val strokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            style = Paint.Style.STROKE
            color = android.graphics.Color.parseColor("#3A1A80")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlossyCircleButton(
                icon = Icons.Rounded.Pause,
                contentDescriptionText = "Pause game",
                onClick = onPauseClick,
                tag = "pause_button"
            )

            HudStatBox(label = "Score", value = "%,d".format(hudState.score))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..3) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (i <= hudState.stars) GoldYellow else Color(0xFF3B2A7A),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            HudStatBox(label = "Shots", value = "${hudState.shots}", tag = "hud_shots")
        }

        // Objective & Drop Counter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF7B3FE4))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = hudState.levelLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                hudState.objectiveBubbleColor?.let { col ->
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(col.mid)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = hudState.objectiveText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hudState.dropStatusText.isNotEmpty()) {
                Text(
                    text = hudState.dropStatusText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = FrostCyan
                )
            }
        }

        // Playfield Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        onBoardSized(size.width.toFloat(), size.height.toFloat())
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("game_canvas")
                    .pointerInput(session, isPaused) {
                        if (session == null) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (isPaused || session.ended) return@awaitEachGesture
                            val m = session.metrics
                            val downPos = down.position
                            val nearShooter = hypot(downPos.x - m.shooterX, downPos.y - m.shooterY) < m.radius * 1.5f
                            val nearNext = hypot(
                                downPos.x - (m.shooterX - m.radius * 3.7f),
                                downPos.y - (m.shooterY + m.radius * 0.9f)
                            ) < m.radius * 1.3f

                            if (nearShooter || nearNext) {
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    if (!change.pressed) {
                                        onSwapBubbles()
                                        break
                                    }
                                }
                                return@awaitEachGesture
                            }

                            session.isAiming = true
                            session.aimAt(downPos.x, downPos.y)
                            down.consume()

                            var lastPos = downPos
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                lastPos = change.position
                                session.aimAt(lastPos.x, lastPos.y)
                                change.consume()
                                if (!change.pressed) {
                                    session.isAiming = false
                                    if (lastPos.y < m.shooterY - m.radius * 1.1f) {
                                        onFireShot()
                                    }
                                    break
                                }
                            }
                        }
                    }
            ) {
                // Read frameTick so Compose invalidates the Canvas each frame
                val tick = frameTick
                val s = session ?: return@Canvas
                val m = s.metrics
                val r = m.radius
                @Suppress("UNUSED_EXPRESSION")
                tick

                // Playfield Frame
                val boardBg = if (s.freezeTurns > 0) Color(0x3878C8FF) else Color(0x61140A50)
                val boardBorder = if (s.freezeTurns > 0) Color(0xB3AAE6FF) else Color(0x24FFFFFF)
                drawRoundRect(
                    color = boardBg,
                    topLeft = Offset(2f, 0f),
                    size = Size(m.width - 4f, m.dangerY + r * 0.7f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                drawRoundRect(
                    color = boardBorder,
                    topLeft = Offset(2f, 0f),
                    size = Size(m.width - 4f, m.dangerY + r * 0.7f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(2.dp.toPx())
                )

                // Grid Bubbles clipped inside playfield
                clipRect(left = 0f, top = 0f, right = m.width, bottom = m.dangerY + r * 0.7f) {
                    for (row in s.grid.indices) {
                        for (col in 0 until GRID_COLS) {
                            val cell = s.grid[row][col] ?: continue
                            drawGlossyBubble(
                                center = Offset(s.cellX(row, col), s.cellY(row)),
                                radius = r,
                                cell = cell
                            )
                        }
                    }
                }

                // Dashed Danger Line
                drawLine(
                    color = Color(0xBFFF5A6E),
                    start = Offset(8f, m.dangerY),
                    end = Offset(m.width - 8f, m.dangerY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
                )

                // Sparkles
                for (sp in s.sparkles) {
                    val alpha = (1f - sp.elapsed / 0.5f).coerceIn(0f, 1f)
                    val z = r * 0.35f * alpha
                    val starPath = Path().apply {
                        moveTo(sp.x, sp.y - z * 1.6f)
                        lineTo(sp.x + z * 0.4f, sp.y - z * 0.4f)
                        lineTo(sp.x + z * 1.6f, sp.y)
                        lineTo(sp.x + z * 0.4f, sp.y + z * 0.4f)
                        lineTo(sp.x, sp.y + z * 1.6f)
                        lineTo(sp.x - z * 0.4f, sp.y + z * 0.4f)
                        lineTo(sp.x - z * 1.6f, sp.y)
                        lineTo(sp.x - z * 0.4f, sp.y - z * 0.4f)
                        close()
                    }
                    drawPath(starPath, Color.White.copy(alpha = alpha))
                }

                // Falling Detached Bubbles
                for (fall in s.falls) {
                    drawGlossyBubble(
                        center = Offset(fall.x, fall.y),
                        radius = r,
                        cell = fall.bubble
                    )
                }

                // Popping Bubbles
                for (pop in s.pops) {
                    if (pop.delay > 0f) {
                        drawGlossyBubble(Offset(pop.x, pop.y), r, pop.bubble)
                    } else {
                        val k = (pop.elapsed / 0.2f).coerceIn(0f, 1f)
                        drawGlossyBubble(
                            center = Offset(pop.x, pop.y),
                            radius = r * (1f + k * 0.35f),
                            cell = pop.bubble,
                            alpha = 1f - k
                        )
                    }
                }

                // Trajectory Guide
                if (s.phase == GamePhase.READY && !isPaused && !s.ended) {
                    val pts = s.computeTrajectory()
                    val count = pts.size.coerceAtLeast(1)
                    pts.forEachIndexed { idx, (px, py) ->
                        val alpha = (1f - idx.toFloat() / count * 0.7f).coerceAtLeast(0.25f)
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = r * 0.17f,
                            center = Offset(px, py)
                        )
                    }
                }

                // Shooter Base & Cannon
                drawOval(
                    color = Color(0x8C1E0A5A),
                    topLeft = Offset(m.shooterX - r * 2.6f, m.shooterY + r * 0.5f),
                    size = Size(r * 5.2f, r * 2.0f)
                )
                drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xFFA45CFF), Color(0xFF4A1BA8))),
                    topLeft = Offset(m.shooterX - r * 2.2f, m.shooterY + r * 0.35f),
                    size = Size(r * 4.4f, r * 1.9f)
                )
                drawOval(
                    color = GoldYellow,
                    topLeft = Offset(m.shooterX - r * 2.2f, m.shooterY + r * 0.35f),
                    size = Size(r * 4.4f, r * 1.9f),
                    style = Stroke(2.5.dp.toPx())
                )

                val angleDeg = ((PI / 2.0 - s.aimAngle) * 180.0 / PI).toFloat()
                rotate(degrees = angleDeg, pivot = Offset(m.shooterX, m.shooterY)) {
                    drawRoundRect(
                        color = Color(0xFF7B3FE4),
                        topLeft = Offset(m.shooterX - r * 0.55f, m.shooterY - r * 2.1f),
                        size = Size(r * 1.1f, r * 2.3f),
                        cornerRadius = CornerRadius(r * 0.4f, r * 0.4f)
                    )
                    drawRoundRect(
                        color = GoldYellow,
                        topLeft = Offset(m.shooterX - r * 0.55f, m.shooterY - r * 2.1f),
                        size = Size(r * 1.1f, r * 2.3f),
                        cornerRadius = CornerRadius(r * 0.4f, r * 0.4f),
                        style = Stroke(2.dp.toPx())
                    )
                }

                // Armed Halo + Current Loaded Bubble
                if (s.armedBooster != null) {
                    val pulseR = r * 1.45f + sin(s.timeElapsed * 8f) * 3f
                    drawCircle(
                        color = Color(0x59FFE65A),
                        radius = pulseR,
                        center = Offset(m.shooterX, m.shooterY)
                    )
                }
                drawGlossyBubble(
                    center = Offset(m.shooterX, m.shooterY),
                    radius = r,
                    cell = null,
                    colorOverride = s.curColor,
                    booster = s.armedBooster
                )

                // Next Bubble Preview
                val nextCenter = Offset(m.shooterX - r * 3.7f, m.shooterY + r * 0.9f)
                drawGlossyBubble(
                    center = nextCenter,
                    radius = r * 0.72f,
                    cell = null,
                    colorOverride = s.nextColor
                )
                drawContext.canvas.nativeCanvas.apply {
                    val labelPx = r * 0.52f
                    strokePaint.textSize = labelPx
                    strokePaint.strokeWidth = 4f
                    strokePaint.alpha = 200
                    textPaint.textSize = labelPx
                    textPaint.color = android.graphics.Color.WHITE
                    textPaint.alpha = 255
                    drawText("NEXT", nextCenter.x, m.shooterY - r * 0.15f, strokePaint)
                    drawText("NEXT", nextCenter.x, m.shooterY - r * 0.15f, textPaint)
                }

                // Flying Projectile
                s.projectile?.let { proj ->
                    drawGlossyBubble(
                        center = Offset(proj.x, proj.y),
                        radius = r,
                        cell = null,
                        colorOverride = proj.color,
                        booster = proj.booster
                    )
                }

                // Lightning & Explosion Beams
                for (beam in s.beams) {
                    val alpha = (1f - beam.elapsed / 0.45f).coerceIn(0f, 1f)
                    if (beam.isRing) {
                        drawCircle(
                            color = Color(0xFFFFC83C).copy(alpha = alpha),
                            radius = beam.maxRadius * (0.4f + beam.elapsed * 2.2f),
                            center = Offset(beam.x, beam.y),
                            style = Stroke(width = (r * 0.5f * alpha + 2f))
                        )
                    } else {
                        val bw = r * alpha + 2f
                        drawRect(
                            color = Color(0xFFFFF078).copy(alpha = alpha * 0.85f),
                            topLeft = Offset(beam.x - bw / 2f, 0f),
                            size = Size(bw, m.dangerY + r)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = alpha),
                            topLeft = Offset(beam.x - 2f, 0f),
                            size = Size(4f, m.dangerY + r)
                        )
                    }
                }

                // Burst Particles
                for (pt in s.particles) {
                    val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
                    drawCircle(
                        color = pt.color.copy(alpha = alpha),
                        radius = pt.radius,
                        center = Offset(pt.x, pt.y)
                    )
                }

                // Floating Score & Combo Popups
                drawContext.canvas.nativeCanvas.apply {
                    for (ft in s.floatingTexts) {
                        val maxT = if (ft.big) 1.4f else 1.0f
                        val alpha = ((1f - ft.elapsed / maxT) * 255).toInt().coerceIn(0, 255)
                        val px = r * if (ft.big) 1.45f else 1.05f
                        strokePaint.textSize = px
                        strokePaint.strokeWidth = 6f
                        strokePaint.alpha = alpha
                        textPaint.textSize = px
                        textPaint.color = if (ft.big) android.graphics.Color.parseColor("#FFE36A") else android.graphics.Color.WHITE
                        textPaint.alpha = alpha
                        drawText(ft.text, ft.x, ft.y, strokePaint)
                        drawText(ft.text, ft.x, ft.y, textPaint)
                    }
                }
            }
        }

        // Bottom Booster Tray
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoosterTrayButton(
                type = BoosterType.BOMB,
                icon = Icons.Rounded.Whatshot,
                count = saveState.profile.bombCount,
                armed = hudState.armedBooster == BoosterType.BOMB,
                disabled = hudState.noBooster,
                onClick = { onBoosterClick(BoosterType.BOMB) }
            )
            BoosterTrayButton(
                type = BoosterType.RAINBOW,
                icon = Icons.Rounded.AutoAwesome,
                count = saveState.profile.rainbowCount,
                armed = hudState.armedBooster == BoosterType.RAINBOW,
                disabled = hudState.noBooster,
                onClick = { onBoosterClick(BoosterType.RAINBOW) }
            )
            BoosterTrayButton(
                type = BoosterType.LIGHTNING,
                icon = Icons.Rounded.Bolt,
                count = saveState.profile.lightningCount,
                armed = hudState.armedBooster == BoosterType.LIGHTNING,
                disabled = hudState.noBooster,
                onClick = { onBoosterClick(BoosterType.LIGHTNING) }
            )
            BoosterTrayButton(
                type = BoosterType.FIRE,
                icon = Icons.Rounded.LocalFireDepartment,
                count = saveState.profile.fireCount,
                armed = hudState.armedBooster == BoosterType.FIRE,
                disabled = hudState.noBooster,
                onClick = { onBoosterClick(BoosterType.FIRE) }
            )
            BoosterTrayButton(
                type = BoosterType.FREEZE,
                icon = Icons.Rounded.AcUnit,
                count = saveState.profile.freezeCount,
                armed = hudState.isFreezeActive,
                disabled = hudState.noBooster,
                onClick = { onBoosterClick(BoosterType.FREEZE) }
            )
            // Swap bubble button
            Box(
                modifier = Modifier
                    .testTag("swap_button")
                    .size(54.dp)
                    .minimumInteractiveComponentSize()
                    .shadow(6.dp, CircleShape)
                    .background(Color(0xFF1E0A66), CircleShape)
                    .padding(bottom = 3.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF5AA6FF), Color(0xFF1B4FD0))))
                    .border(2.5.dp, Color(0xFFB48CFF), CircleShape)
                    .clickable(onClick = onSwapBubbles),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Swap current and next bubble",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun HudStatBox(label: String, value: String, tag: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC1A0D5A))
            .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            color = Color.White
        )
    }
}

@Composable
private fun BoosterTrayButton(
    type: BoosterType,
    icon: ImageVector,
    count: Int,
    armed: Boolean,
    disabled: Boolean,
    onClick: () -> Unit
) {
    val active = count > 0 && !disabled
    Box(
        modifier = Modifier
            .testTag("booster_${type.key}")
            .offset(y = if (armed) (-5).dp else 0.dp)
            .size(54.dp)
            .minimumInteractiveComponentSize()
            .alpha(if (active) 1f else 0.52f)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(if (armed) 12.dp else 5.dp, CircleShape)
                .background(if (armed) Color(0xFF8A5A00) else Color(0xFF1E0A66), CircleShape)
                .padding(bottom = 3.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF7A52E8), Color(0xFF3A1D9A))))
                .border(if (armed) 3.dp else 2.5.dp, if (armed) GoldYellow else Color(0xFFB48CFF), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "${type.title} booster, $count left",
                tint = if (armed) GoldYellow else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(10.dp))
                .background(GoldYellow)
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = "x$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4A1A00)
            )
        }
    }
}
