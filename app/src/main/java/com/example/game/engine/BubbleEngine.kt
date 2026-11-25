package com.example.game.engine

import com.example.game.model.BoosterType
import com.example.game.model.BubbleColor
import com.example.game.model.BubbleKind
import com.example.game.model.CellBubble
import com.example.game.model.EndReason
import com.example.game.model.GRID_COLS
import com.example.game.model.LevelConfig
import com.example.game.model.LevelObjective
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class GamePhase {
    READY,
    BUSY,
    ENDED
}

data class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val speed: Float,
    val booster: BoosterType?,
    val color: BubbleColor,
    val piercedCells: MutableList<RemovedBubbleInfo> = mutableListOf(),
    var pierceCount: Int = 0
)

data class RemovedBubbleInfo(
    val row: Int,
    val col: Int,
    val bubble: CellBubble,
    val x: Float,
    val y: Float
)

data class PopAnim(
    val x: Float,
    val y: Float,
    val bubble: CellBubble,
    var delay: Float,
    var elapsed: Float = 0f,
    var bursted: Boolean = false
)

data class FallAnim(
    var x: Float,
    var y: Float,
    val bubble: CellBubble,
    var delay: Float,
    var vx: Float,
    var vy: Float
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: androidx.compose.ui.graphics.Color,
    val radius: Float
)

data class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    var elapsed: Float = 0f,
    val big: Boolean = false
)

data class BeamEffect(
    val x: Float,
    val y: Float = 0f,
    val maxRadius: Float = 0f,
    val isRing: Boolean = false,
    var elapsed: Float = 0f
)

data class SparkleEffect(
    val x: Float,
    val y: Float,
    var elapsed: Float = 0f
)

enum class EngineEvent {
    SHOOT,
    BOUNCE,
    STICK,
    POP,
    DROP_FLOAT,
    BOMB_BOOM,
    LIGHTNING_ZAP,
    FIRE_WHOOSH,
    FREEZE_CHIME,
    RAINBOW_CHIME,
    ROW_DROP,
    COMBO,
    WIN,
    LOSE
}

data class BoardMetrics(
    val width: Float = 360f,
    val height: Float = 600f,
    val radius: Float = 17f,
    val gridLeft: Float = 8f,
    val gridTop: Float = 8f,
    val rowHeight: Float = 29.4f,
    val shooterX: Float = 180f,
    val shooterY: Float = 540f,
    val dangerY: Float = 470f,
    val dangerRow: Int = 14
) {
    companion object {
        fun compute(width: Float, height: Float): BoardMetrics {
            if (width <= 0f || height <= 0f) return BoardMetrics()
            val radius = max(9f, floor(width / (2f * GRID_COLS + 1f) * 10f) / 10f)
            val gridLeft = (width - (2f * radius * GRID_COLS + radius)) / 2f
            val gridTop = 8f
            val rowHeight = radius * 1.73205f
            val shooterX = width / 2f
            val shooterY = height - radius * 2.65f
            val dangerY = shooterY - radius * 3.55f
            val dangerRow = max(6, floor((dangerY - gridTop - radius) / rowHeight).toInt())
            return BoardMetrics(
                width = width,
                height = height,
                radius = radius,
                gridLeft = gridLeft,
                gridTop = gridTop,
                rowHeight = rowHeight,
                shooterX = shooterX,
                shooterY = shooterY,
                dangerY = dangerY,
                dangerRow = dangerRow
            )
        }
    }
}

class BubbleGameSession(
    val config: LevelConfig,
    val isDaily: Boolean,
    bonusShots: Int = 0
) {
    val grid: MutableList<MutableList<CellBubble?>> =
        config.grid.map { it.toMutableList() }.toMutableList()

    var metrics: BoardMetrics = BoardMetrics()
    var parity: Int = 0
    var shots: Int = config.shots + if (isDaily) 0 else bonusShots
    var shotsMax: Int = shots
    var score: Int = 0
    var removedCount: Int = 0
    var colorCleared: Int = 0
    var combo: Int = 0
    var maxCombo: Int = 0
    var missCount: Int = 0
    var freezeTurns: Int = 0
    var curColor: BubbleColor = config.palette.first()
    var nextColor: BubbleColor = config.palette.first()
    var armedBooster: BoosterType? = null
    var usedBooster: Boolean = false
    var phase: GamePhase = GamePhase.READY
    var projectile: Projectile? = null
    val pops = mutableListOf<PopAnim>()
    val falls = mutableListOf<FallAnim>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()
    val beams = mutableListOf<BeamEffect>()
    val sparkles = mutableListOf<SparkleEffect>()
    var gridShift: Float = 0f
    var aimAngle: Float = (PI / 2.0).toFloat()
    var isAiming: Boolean = false
    var timeElapsed: Float = 0f
    var ended: Boolean = false
    var won: Boolean = false
    var endReason: EndReason = EndReason.NONE
    var continuesUsed: Int = 0
    var doubledCoins: Boolean = false
    var lastCoinsEarned: Int = 0
    private var sparkleTimer: Float = 0.6f

    var onEvent: ((EngineEvent, Int) -> Unit)? = null
    var onShotComplete: (() -> Unit)? = null
    var onGameEnded: ((Boolean, EndReason) -> Unit)? = null

    init {
        curColor = pickRandomPresentColor()
        nextColor = pickRandomPresentColor()
    }

    fun cellX(r: Int, c: Int): Float {
        val offset = if (((r + parity) and 1) == 1) metrics.radius else 0f
        return metrics.gridLeft + metrics.radius + c * 2f * metrics.radius + offset
    }

    fun cellY(r: Int): Float {
        return metrics.gridTop + metrics.radius + r * metrics.rowHeight + gridShift
    }

    fun presentColors(): List<BubbleColor> {
        val set = LinkedHashSet<BubbleColor>()
        for (row in grid) {
            for (cell in row) {
                if (cell != null && cell.kind != BubbleKind.STONE) {
                    set.add(cell.color)
                }
            }
        }
        return set.toList()
    }

    fun pickRandomPresentColor(): BubbleColor {
        val present = presentColors()
        val pool = present.ifEmpty { config.palette }
        return pool[Random.nextInt(pool.size)]
    }

    fun fixShooterColors() {
        val present = presentColors()
        if (present.isEmpty()) return
        if (curColor !in present) curColor = present[Random.nextInt(present.size)]
        if (nextColor !in present) nextColor = present[Random.nextInt(present.size)]
    }

    fun starsEarnedNow(): Int {
        val ratio = if (shotsMax > 0) shots.toFloat() / shotsMax.toFloat() else 0f
        return when {
            ratio >= 0.45f -> 3
            ratio >= 0.25f -> 2
            else -> 1
        }
    }

    fun remainingNonStoneCount(): Int {
        var count = 0
        for (row in grid) {
            for (cell in row) {
                if (cell != null && cell.kind != BubbleKind.STONE) count++
            }
        }
        return count
    }

    fun aimAt(x: Float, y: Float) {
        val dx = x - metrics.shooterX
        val dy = metrics.shooterY - y
        var a = atan2(dy, dx)
        if (dy < metrics.radius * 0.4f) {
            a = if (dx >= 0f) 0.2f else (PI - 0.2).toFloat()
        }
        aimAngle = a.coerceIn(0.2f, (PI - 0.2).toFloat())
    }

    fun swapBubbles(): Boolean {
        if (phase != GamePhase.READY || armedBooster != null || ended) return false
        val tmp = curColor
        curColor = nextColor
        nextColor = tmp
        return true
    }

    fun activateFreeze(onConsumed: () -> Unit): Boolean {
        if (phase != GamePhase.READY || ended || config.noBooster || freezeTurns > 0) return false
        onConsumed()
        usedBooster = true
        freezeTurns = 5
        floatingTexts.add(FloatingText(metrics.width / 2f, metrics.height * 0.4f, "Frozen!", big = true))
        onEvent?.invoke(EngineEvent.FREEZE_CHIME, 0)
        onShotComplete?.invoke()
        return true
    }

    fun toggleArmedBooster(type: BoosterType): Boolean {
        if (phase != GamePhase.READY || ended || config.noBooster) return false
        armedBooster = if (armedBooster == type) null else type
        return true
    }

    fun fireShot(onConsumeBooster: (BoosterType) -> Unit): Boolean {
        if (phase != GamePhase.READY || projectile != null || ended || shots <= 0) return false
        val r = metrics.radius
        val speed = r * 52f
        val booster = armedBooster
        val shotColor = curColor
        if (booster != null) {
            onConsumeBooster(booster)
            usedBooster = true
            armedBooster = null
        } else {
            curColor = nextColor
            nextColor = pickRandomPresentColor()
        }
        projectile = Projectile(
            x = metrics.shooterX,
            y = metrics.shooterY,
            vx = cos(aimAngle),
            vy = -sin(aimAngle),
            speed = speed,
            booster = booster,
            color = shotColor
        )
        shots--
        phase = GamePhase.BUSY
        when (booster) {
            BoosterType.FIRE -> onEvent?.invoke(EngineEvent.FIRE_WHOOSH, 0)
            BoosterType.RAINBOW -> onEvent?.invoke(EngineEvent.RAINBOW_CHIME, 0)
            else -> onEvent?.invoke(EngineEvent.SHOOT, 0)
        }
        return true
    }

    fun computeTrajectory(): List<Pair<Float, Float>> {
        val r = metrics.radius
        var x = metrics.shooterX
        var y = metrics.shooterY
        var vx = cos(aimAngle)
        val vy = -sin(aimAngle)
        var bounces = 0
        val pts = mutableListOf<Pair<Float, Float>>()
        val step = r * 0.5f
        for (i in 0 until 300) {
            x += vx * step
            y += vy * step
            if (x < r) {
                x = r
                vx = -vx
                bounces++
            } else if (x > metrics.width - r) {
                x = metrics.width - r
                vx = -vx
                bounces++
            }
            if (y <= metrics.gridTop + r) break
            if (findHit(x, y) != null) break
            if (i % 2 == 0 && i > 2) pts.add(x to y)
            if (bounces > 4) break
        }
        return pts
    }

    fun update(dt: Float) {
        val clampedDt = dt.coerceIn(0f, 0.033f)
        timeElapsed += clampedDt

        if (gridShift != 0f) {
            gridShift += (-gridShift) * min(1f, clampedDt * 9f)
            if (abs(gridShift) < 0.3f) gridShift = 0f
        }

        if (projectile != null) {
            stepProjectile(clampedDt)
        }

        for (i in pops.indices.reversed()) {
            val p = pops[i]
            if (p.delay > 0f) {
                p.delay -= clampedDt
                continue
            }
            if (!p.bursted) {
                p.bursted = true
                val col = if (p.bubble.kind == BubbleKind.STONE) {
                    androidx.compose.ui.graphics.Color(0xFFAAB0C0)
                } else {
                    p.bubble.color.mid
                }
                spawnBurst(p.x, p.y, col, 6)
            }
            p.elapsed += clampedDt
            if (p.elapsed > 0.2f) pops.removeAt(i)
        }

        val r = metrics.radius
        for (i in falls.indices.reversed()) {
            val f = falls[i]
            if (f.delay > 0f) {
                f.delay -= clampedDt
                continue
            }
            f.vy += r * 95f * clampedDt
            f.y += f.vy * clampedDt
            f.x += f.vx * clampedDt
            if (f.y > metrics.height + r * 2f) falls.removeAt(i)
        }

        for (i in particles.indices.reversed()) {
            val pt = particles[i]
            pt.life -= clampedDt
            if (pt.life <= 0f) {
                particles.removeAt(i)
                continue
            }
            pt.x += pt.vx * clampedDt
            pt.y += pt.vy * clampedDt
            pt.vy += r * 30f * clampedDt
        }

        for (i in floatingTexts.indices.reversed()) {
            val ft = floatingTexts[i]
            ft.elapsed += clampedDt
            ft.y -= r * (if (ft.big) 0.5f else 1.3f) * clampedDt
            if (ft.elapsed > (if (ft.big) 1.4f else 1f)) floatingTexts.removeAt(i)
        }

        for (i in beams.indices.reversed()) {
            val b = beams[i]
            b.elapsed += clampedDt
            if (b.elapsed > 0.45f) beams.removeAt(i)
        }

        sparkleTimer -= clampedDt
        if (sparkleTimer <= 0f) {
            sparkleTimer = 0.5f + Random.nextFloat() * 0.6f
            if (grid.isNotEmpty()) {
                val rr = Random.nextInt(grid.size)
                val cc = Random.nextInt(GRID_COLS)
                if (HexGridUtils.cellAt(grid, rr, cc) != null) {
                    sparkles.add(SparkleEffect(cellX(rr, cc) - r * 0.3f, cellY(rr) - r * 0.4f))
                }
            }
        }
        for (i in sparkles.indices.reversed()) {
            sparkles[i].elapsed += clampedDt
            if (sparkles[i].elapsed > 0.5f) sparkles.removeAt(i)
        }
    }

    private fun findHit(x: Float, y: Float): Pair<Int, Int>? {
        val r = metrics.radius
        val limitSq = (r * 1.8f) * (r * 1.8f)
        val r0 = max(0, floor((y - metrics.gridTop - gridShift) / metrics.rowHeight).toInt() - 2)
        val r1 = min(grid.size - 1, r0 + 5)
        var best: Pair<Int, Int>? = null
        var bestDist = Float.MAX_VALUE
        for (row in r0..r1) {
            for (col in 0 until GRID_COLS) {
                if (grid[row][col] == null) continue
                val dx = cellX(row, col) - x
                val dy = cellY(row) - y
                val distSq = dx * dx + dy * dy
                if (distSq < limitSq && distSq < bestDist) {
                    bestDist = distSq
                    best = row to col
                }
            }
        }
        return best
    }

    private fun stepProjectile(dt: Float) {
        val p = projectile ?: return
        val r = metrics.radius
        var rem = p.speed * dt
        while (rem > 0f && projectile != null) {
            val step = min(rem, r * 0.4f)
            rem -= step
            p.x += p.vx * step
            p.y += p.vy * step
            if (p.x < r) {
                p.x = r
                p.vx = abs(p.vx)
                onEvent?.invoke(EngineEvent.BOUNCE, 0)
            } else if (p.x > metrics.width - r) {
                p.x = metrics.width - r
                p.vx = -abs(p.vx)
                onEvent?.invoke(EngineEvent.BOUNCE, 0)
            }
            if (p.booster == BoosterType.FIRE && Random.nextFloat() < 0.55f && particles.size < 200) {
                particles.add(
                    Particle(
                        x = p.x,
                        y = p.y,
                        vx = (Random.nextFloat() - 0.5f) * r * 4f,
                        vy = r * 3f,
                        life = 0.4f,
                        maxLife = 0.4f,
                        color = androidx.compose.ui.graphics.Color(0xFFFFB300),
                        radius = r * 0.2f
                    )
                )
            }
            if (p.y <= metrics.gridTop + r) {
                p.y = metrics.gridTop + r
                landProjectile(null)
                return
            }
            val hit = findHit(p.x, p.y)
            if (hit != null) {
                if (p.booster == BoosterType.FIRE) {
                    val popped = popGroup(listOf(hit))
                    p.piercedCells.addAll(popped)
                    p.pierceCount++
                    if (p.pierceCount >= 6) {
                        projectile = null
                        finishShot(p.piercedCells)
                        return
                    }
                    continue
                }
                landProjectile(hit)
                return
            }
        }
    }

    private fun landProjectile(hit: Pair<Int, Int>?) {
        val p = projectile ?: return
        projectile = null
        val r = metrics.radius

        when (p.booster) {
            BoosterType.LIGHTNING -> {
                beams.add(BeamEffect(x = p.x))
                onEvent?.invoke(EngineEvent.LIGHTNING_ZAP, 0)
                val list = mutableListOf<Pair<Int, Int>>()
                for (row in grid.indices) {
                    for (col in 0 until GRID_COLS) {
                        if (grid[row][col] != null && abs(cellX(row, col) - p.x) < r * 1.05f) {
                            list.add(row to col)
                        }
                    }
                }
                finishShot(popGroup(list))
                return
            }
            BoosterType.BOMB -> {
                triggerBoomFx(p.x, p.y, r * 3.2f)
                val list = mutableListOf<Pair<Int, Int>>()
                val limitSq = (r * 3.2f) * (r * 3.2f)
                for (row in grid.indices) {
                    for (col in 0 until GRID_COLS) {
                        if (grid[row][col] != null) {
                            val dx = cellX(row, col) - p.x
                            val dy = cellY(row) - p.y
                            if (dx * dx + dy * dy <= limitSq) list.add(row to col)
                        }
                    }
                }
                finishShot(popGroup(list))
                return
            }
            BoosterType.FIRE -> {
                finishShot(p.piercedCells)
                return
            }
            else -> Unit
        }

        val pos = (if (hit != null) bestAdjacentEmptyCell(hit.first, hit.second, p.x, p.y) else topEmptyCell(p.x))
            ?: nearestEmptyCell(p.x, p.y)
        val (row, col) = pos
        while (grid.size <= row) {
            grid.add(MutableList(GRID_COLS) { null })
        }

        var finalColor = p.color
        if (p.booster == BoosterType.RAINBOW) {
            var bestColor: BubbleColor? = null
            var bestSize = 0
            for (candidate in presentColors()) {
                grid[row][col] = CellBubble(color = candidate)
                val size = findCluster(row, col).size
                if (size > bestSize) {
                    bestSize = size
                    bestColor = candidate
                }
            }
            finalColor = bestColor ?: BubbleColor.fromId(Random.nextInt(6))
        }

        grid[row][col] = CellBubble(color = finalColor)
        onEvent?.invoke(EngineEvent.STICK, 0)

        val cluster = findCluster(row, col)
        val popped = if (cluster.size >= 3) popGroup(cluster) else emptyList()
        finishShot(popped)
    }

    private fun bestAdjacentEmptyCell(hr: Int, hc: Int, x: Float, y: Float): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestDist = Float.MAX_VALUE
        for ((rr, cc) in HexGridUtils.neighbors(hr, hc, parity)) {
            if (rr < 0 || cc < 0 || cc >= GRID_COLS) continue
            if (HexGridUtils.cellAt(grid, rr, cc) != null) continue
            val dx = cellX(rr, cc) - x
            val dy = cellY(rr) - y
            val d = dx * dx + dy * dy
            if (d < bestDist) {
                bestDist = d
                best = rr to cc
            }
        }
        return best
    }

    private fun topEmptyCell(x: Float): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestDist = Float.MAX_VALUE
        for (c in 0 until GRID_COLS) {
            if (HexGridUtils.cellAt(grid, 0, c) != null) continue
            val d = abs(cellX(0, c) - x)
            if (d < bestDist) {
                bestDist = d
                best = 0 to c
            }
        }
        return best
    }

    private fun nearestEmptyCell(x: Float, y: Float): Pair<Int, Int> {
        var best = 0 to 0
        var bestDist = Float.MAX_VALUE
        for (r in 0..grid.size) {
            for (c in 0 until GRID_COLS) {
                if (HexGridUtils.cellAt(grid, r, c) != null) continue
                var anchored = r == 0
                if (!anchored) {
                    for ((rr, cc) in HexGridUtils.neighbors(r, c, parity)) {
                        if (HexGridUtils.cellAt(grid, rr, cc) != null) {
                            anchored = true
                            break
                        }
                    }
                }
                if (!anchored) continue
                val dx = cellX(r, c) - x
                val dy = cellY(r) - y
                val d = dx * dx + dy * dy
                if (d < bestDist) {
                    bestDist = d
                    best = r to c
                }
            }
        }
        return best
    }

    fun findCluster(startR: Int, startC: Int): List<Pair<Int, Int>> {
        val origin = HexGridUtils.cellAt(grid, startR, startC)
        if (origin == null || origin.kind == BubbleKind.STONE) return emptyList()
        val seen = HashSet<Int>().apply { add(startR * GRID_COLS + startC) }
        val queue = ArrayDeque<Pair<Int, Int>>().apply { addLast(startR to startC) }
        val out = mutableListOf<Pair<Int, Int>>()
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeLast()
            out.add(r to c)
            for ((rr, cc) in HexGridUtils.neighbors(r, c, parity)) {
                val n = HexGridUtils.cellAt(grid, rr, cc) ?: continue
                if (n.kind == BubbleKind.STONE || n.color != origin.color) continue
                val key = rr * GRID_COLS + cc
                if (seen.add(key)) queue.addLast(rr to cc)
            }
        }
        return out
    }

    private fun removeCell(r: Int, c: Int, isPop: Boolean, delay: Float): RemovedBubbleInfo? {
        val cell = HexGridUtils.cellAt(grid, r, c) ?: return null
        grid[r][c] = null
        val x = cellX(r, c)
        val y = cellY(r)
        if (isPop) {
            pops.add(PopAnim(x, y, cell, delay))
        } else {
            val rad = metrics.radius
            falls.add(
                FallAnim(
                    x = x,
                    y = y,
                    bubble = cell,
                    delay = delay,
                    vx = (Random.nextFloat() - 0.5f) * rad * 5f,
                    vy = -rad * (5f + Random.nextFloat() * 6f)
                )
            )
        }
        return RemovedBubbleInfo(r, c, cell, x, y)
    }

    private fun popGroup(initial: List<Pair<Int, Int>>): List<RemovedBubbleInfo> {
        val seen = HashSet<Int>(initial.map { (r, c) -> r * GRID_COLS + c })
        val all = initial.toMutableList()
        var idx = 0
        while (idx < all.size) {
            val (r, c) = all[idx++]
            val cell = HexGridUtils.cellAt(grid, r, c)
            if (cell != null && cell.hasBomb) {
                triggerBoomFx(cellX(r, c), cellY(r), metrics.radius * 2.2f)
                for ((rr, cc) in HexGridUtils.neighbors(r, c, parity)) {
                    val adjCell = HexGridUtils.cellAt(grid, rr, cc)
                    val key = rr * GRID_COLS + cc
                    if (adjCell != null && seen.add(key)) {
                        all.add(rr to cc)
                    }
                }
            }
        }
        all.sortBy { it.first }
        return all.mapIndexedNotNull { i, (r, c) ->
            removeCell(r, c, isPop = true, delay = i * 0.025f)
        }
    }

    private fun triggerBoomFx(x: Float, y: Float, maxRadius: Float) {
        beams.add(BeamEffect(x = x, y = y, maxRadius = maxRadius, isRing = true))
        onEvent?.invoke(EngineEvent.BOMB_BOOM, 0)
    }

    private fun spawnBurst(x: Float, y: Float, color: androidx.compose.ui.graphics.Color, count: Int) {
        val r = metrics.radius
        for (i in 0 until count) {
            if (particles.size >= 220) break
            val angle = Random.nextFloat() * (2f * PI.toFloat())
            val speed = r * (3f + Random.nextFloat() * 6f)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - r * 3f,
                    life = 0.5f,
                    maxLife = 0.5f,
                    color = color,
                    radius = r * (0.12f + Random.nextFloat() * 0.14f)
                )
            )
        }
    }

    private fun finishShot(popped: List<RemovedBubbleInfo>) {
        val floatingCoords = HexGridUtils.findFloatingCells(grid, parity)
        val fell = floatingCoords.mapIndexedNotNull { i, (r, c) ->
            removeCell(r, c, isPop = false, delay = 0.12f + i * 0.02f)
        }
        val np = popped.size
        val nf = fell.size
        val wasFrozen = freezeTurns > 0
        if (wasFrozen) freezeTurns--

        if (np + nf > 0) {
            combo++
            if (combo > maxCombo) maxCombo = combo
            val baseScore = np * 10 + max(0, np - 3) * 10 + nf * 25
            val gained = (baseScore * (1f + 0.15f * min(combo - 1, 8))).roundToInt()
            score += gained

            val allRemoved = popped + fell
            removedCount += allRemoved.size
            val obj = config.objective
            if (obj is LevelObjective.PopColor) {
                colorCleared += allRemoved.count {
                    it.bubble.kind != BubbleKind.STONE && it.bubble.color == obj.color
                }
            }

            val midX = allRemoved.map { it.x }.average().toFloat()
            val midY = allRemoved.map { it.y }.average().toFloat()
            floatingTexts.add(FloatingText(midX, midY, "+$gained"))
            if (combo >= 2) {
                floatingTexts.add(FloatingText(metrics.width / 2f, midY - metrics.radius * 2f, "Combo x$combo!", big = true))
                onEvent?.invoke(EngineEvent.COMBO, combo)
            }
            if (np > 0) onEvent?.invoke(EngineEvent.POP, min(4, np))
            if (nf > 0) onEvent?.invoke(EngineEvent.DROP_FLOAT, nf)
        } else {
            combo = 0
            if (config.dropEvery > 0 && !wasFrozen) {
                missCount++
                if (missCount >= config.dropEvery) {
                    missCount = 0
                    dropNewTopRow()
                }
            }
        }

        fixShooterColors()
        evaluateEndState()
    }

    private fun dropNewTopRow() {
        val present = presentColors()
        val pool = present.ifEmpty { config.palette }
        val newRow = MutableList<CellBubble?>(GRID_COLS) {
            CellBubble(color = pool[Random.nextInt(pool.size)])
        }
        grid.add(0, newRow)
        parity = parity xor 1
        gridShift = -metrics.rowHeight
        onEvent?.invoke(EngineEvent.ROW_DROP, 0)
        floatingTexts.add(FloatingText(metrics.width / 2f, metrics.height * 0.3f, "New row!", big = true))
    }

    private fun isDangerReached(): Boolean {
        for (r in metrics.dangerRow until grid.size) {
            for (c in 0 until GRID_COLS) {
                if (grid[r][c] != null) return true
            }
        }
        return false
    }

    private fun evaluateEndState() {
        val nonStone = remainingNonStoneCount()
        var hasWon = nonStone == 0
        if (!hasWon) {
            hasWon = when (val obj = config.objective) {
                is LevelObjective.TargetScore -> score >= obj.target
                is LevelObjective.PopColor -> colorCleared >= obj.target
                is LevelObjective.PopCount -> removedCount >= obj.target
                is LevelObjective.ClearAll -> false
            }
        }
        if (hasWon) {
            finishGame(win = true, reason = EndReason.NONE)
            return
        }
        if (isDangerReached()) {
            finishGame(win = false, reason = EndReason.DANGER_LINE)
            return
        }
        if (shots <= 0) {
            finishGame(win = false, reason = EndReason.OUT_OF_SHOTS)
            return
        }
        phase = GamePhase.READY
        onShotComplete?.invoke()
    }

    private fun finishGame(win: Boolean, reason: EndReason) {
        ended = true
        won = win
        phase = GamePhase.ENDED
        endReason = reason
        onEvent?.invoke(if (win) EngineEvent.WIN else EngineEvent.LOSE, 0)
        onGameEnded?.invoke(win, reason)
    }

    fun grantExtraShotsAfterAd() {
        if (!ended || won) return
        continuesUsed++
        ended = false
        shots += 5
        if (endReason == EndReason.DANGER_LINE) {
            val clearFrom = max(0, metrics.dangerRow - 4)
            val toPop = mutableListOf<Pair<Int, Int>>()
            for (r in clearFrom until grid.size) {
                for (c in 0 until GRID_COLS) {
                    if (grid[r][c] != null) toPop.add(r to c)
                }
            }
            toPop.forEachIndexed { idx, (r, c) -> removeCell(r, c, isPop = true, delay = idx * 0.015f) }
            HexGridUtils.findFloatingCells(grid, parity).forEachIndexed { idx, (r, c) ->
                removeCell(r, c, isPop = false, delay = 0.1f + idx * 0.02f)
            }
        }
        phase = GamePhase.READY
        endReason = EndReason.NONE
        fixShooterColors()
        onShotComplete?.invoke()
    }

    fun exportSnapshot(dailyDateKey: String): SessionSnapshot {
        return SessionSnapshot(
            levelNumber = if (isDaily) 0 else config.levelNumber,
            isDaily = isDaily,
            dailyDateKey = dailyDateKey,
            grid = grid.map { row ->
                row.map { cell ->
                    if (cell == null) null else CellSnapshot(cell.color.id, cell.kind == BubbleKind.STONE, cell.hasBomb)
                }
            },
            parity = parity,
            shots = shots,
            shotsMax = shotsMax,
            score = score,
            removedCount = removedCount,
            colorCleared = colorCleared,
            combo = combo,
            maxCombo = maxCombo,
            missCount = missCount,
            freezeTurns = freezeTurns,
            curColorId = curColor.id,
            nextColorId = nextColor.id,
            usedBooster = usedBooster
        )
    }

    fun restoreFromSnapshot(snap: SessionSnapshot) {
        grid.clear()
        for (row in snap.grid) {
            grid.add(
                row.map { cell ->
                    if (cell == null) null else CellBubble(
                        color = BubbleColor.fromId(cell.colorId),
                        kind = if (cell.isStone) BubbleKind.STONE else BubbleKind.NORMAL,
                        hasBomb = cell.hasBomb
                    )
                }.toMutableList()
            )
        }
        parity = snap.parity
        shots = snap.shots
        shotsMax = snap.shotsMax
        score = snap.score
        removedCount = snap.removedCount
        colorCleared = snap.colorCleared
        combo = snap.combo
        maxCombo = snap.maxCombo
        missCount = snap.missCount
        freezeTurns = snap.freezeTurns
        curColor = BubbleColor.fromId(snap.curColorId)
        nextColor = BubbleColor.fromId(snap.nextColorId)
        usedBooster = snap.usedBooster
        fixShooterColors()
    }
}

data class CellSnapshot(val colorId: Int, val isStone: Boolean, val hasBomb: Boolean)

data class SessionSnapshot(
    val levelNumber: Int,
    val isDaily: Boolean,
    val dailyDateKey: String,
    val grid: List<List<CellSnapshot?>>,
    val parity: Int,
    val shots: Int,
    val shotsMax: Int,
    val score: Int,
    val removedCount: Int,
    val colorCleared: Int,
    val combo: Int,
    val maxCombo: Int,
    val missCount: Int,
    val freezeTurns: Int,
    val curColorId: Int,
    val nextColorId: Int,
    val usedBooster: Boolean
)
