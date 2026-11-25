package com.example.game.engine

import com.example.game.model.BubbleColor
import com.example.game.model.BubbleKind
import com.example.game.model.CellBubble
import com.example.game.model.GRID_COLS
import com.example.game.model.LevelConfig
import com.example.game.model.LevelObjective
import com.example.game.model.LevelPattern
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class Mulberry32(seed: Int) {
    private var a: Int = seed

    fun nextFloat(): Float {
        a = (a + 0x6D2B79F5)
        var t = (a xor (a ushr 15)) * (1 or a)
        t = t xor (t + (t xor (t ushr 7)) * (61 or t))
        val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
        return unsigned.toFloat() / 4294967296f
    }

    fun nextInt(bound: Int): Int {
        if (bound <= 1) return 0
        return floor(nextFloat() * bound).toInt().coerceIn(0, bound - 1)
    }
}

object HexGridUtils {
    fun neighbors(r: Int, c: Int, parity: Int): List<Pair<Int, Int>> {
        return if (((r + parity) and 1) == 1) {
            listOf(
                r to c - 1,
                r to c + 1,
                r - 1 to c,
                r - 1 to c + 1,
                r + 1 to c,
                r + 1 to c + 1
            )
        } else {
            listOf(
                r to c - 1,
                r to c + 1,
                r - 1 to c - 1,
                r - 1 to c,
                r + 1 to c - 1,
                r + 1 to c
            )
        }
    }

    fun cellAt(grid: List<List<CellBubble?>>, r: Int, c: Int): CellBubble? {
        if (r < 0 || r >= grid.size || c < 0 || c >= GRID_COLS) return null
        return grid[r][c]
    }

    fun findFloatingCells(grid: List<List<CellBubble?>>, parity: Int): List<Pair<Int, Int>> {
        val seen = HashSet<Int>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        if (grid.isNotEmpty()) {
            for (c in 0 until GRID_COLS) {
                if (grid[0][c] != null) {
                    seen.add(c)
                    queue.addLast(0 to c)
                }
            }
        }
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeLast()
            for ((rr, cc) in neighbors(r, c, parity)) {
                if (cellAt(grid, rr, cc) == null) continue
                val key = rr * GRID_COLS + cc
                if (seen.add(key)) {
                    queue.addLast(rr to cc)
                }
            }
        }
        val floating = mutableListOf<Pair<Int, Int>>()
        for (r in grid.indices) {
            for (c in 0 until GRID_COLS) {
                if (grid[r][c] != null && !seen.contains(r * GRID_COLS + c)) {
                    floating.add(r to c)
                }
            }
        }
        return floating
    }
}

object LevelGenerator {
    fun generateLevel(
        levelNumber: Int,
        maxRows: Int = 10,
        seedOverride: Int? = null,
        colorCountOverride: Int? = null,
        rowsOverride: Int? = null,
        patternOverride: LevelPattern? = null,
        plain: Boolean = false,
        dropEveryOverride: Int? = null,
        objectiveOverride: LevelObjective? = null,
        shotsOverride: Int? = null,
        noBooster: Boolean = false
    ): LevelConfig {
        val seed = seedOverride ?: (levelNumber * 7919 + 101)
        val rng = Mulberry32(seed)

        val numColors = colorCountOverride ?: min(6, 4 + (levelNumber - 1) / 8)
        var rows = rowsOverride ?: min(10, 4 + (levelNumber - 1) / 10 + if (levelNumber % 7 == 0) 1 else 0)
        rows = min(rows, maxRows).coerceAtLeast(3)

        val indices = mutableListOf(0, 1, 2, 3, 4, 5)
        for (i in 5 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = indices[i]
            indices[i] = indices[j]
            indices[j] = tmp
        }
        val palette = indices.take(numColors).map { BubbleColor.fromId(it) }

        val patterns = LevelPattern.entries
        val pattern = patternOverride ?: if (levelNumber <= 2) {
            LevelPattern.CLUSTERS
        } else {
            patterns[(levelNumber * 3 + levelNumber / 8) % patterns.size]
        }

        val mid = (GRID_COLS - 1) / 2.0
        fun hasCell(r: Int, c: Int): Boolean = when (pattern) {
            LevelPattern.PYRAMID -> c >= r / 2 && c < GRID_COLS - r / 2
            LevelPattern.DIAMOND -> abs(c - mid) <= min(r, rows - 1 - r) + 2.5
            LevelPattern.CHECKER -> r < 2 || (r + c) % 2 == 0
            LevelPattern.WAVES -> r < rows - 2 + (2.0 * sin(c * 0.8 + levelNumber)).roundToInt()
            LevelPattern.COLUMNS -> r < 2 || c % 3 != 2
            LevelPattern.RING -> !(r in 2..(rows - 2) && c in 3..6)
            LevelPattern.CLUSTERS, LevelPattern.STRIPES -> true
        }

        val grid = MutableList(rows) { MutableList<CellBubble?>(GRID_COLS) { null } }
        for (r in 0 until rows) {
            for (c in 0 until GRID_COLS) {
                if (!hasCell(r, c)) continue
                val color = if (pattern == LevelPattern.STRIPES) {
                    val base = palette[(r + levelNumber / 3) % numColors]
                    if (rng.nextFloat() < 0.12f) palette[rng.nextInt(numColors)] else base
                } else {
                    val left = if (c > 0) grid[r][c - 1] else null
                    val up = if (r > 0) grid[r - 1][c] else null
                    val p = rng.nextFloat()
                    when {
                        left != null && p < 0.30f -> left.color
                        up != null && p < 0.48f -> up.color
                        else -> palette[rng.nextInt(numColors)]
                    }
                }
                grid[r][c] = CellBubble(color = color, kind = BubbleKind.NORMAL, hasBomb = false)
            }
        }

        for ((r, c) in HexGridUtils.findFloatingCells(grid, 0)) {
            grid[r][c] = null
        }

        fun editableCells(): List<Pair<Int, Int>> {
            val res = mutableListOf<Pair<Int, Int>>()
            for (r in 1 until rows) {
                for (c in 0 until GRID_COLS) {
                    val cell = grid[r][c]
                    if (cell != null && cell.kind == BubbleKind.NORMAL && !cell.hasBomb) {
                        res.add(r to c)
                    }
                }
            }
            return res
        }

        if (!plain && levelNumber >= 5) {
            var bombs = min(4, 1 + levelNumber / 25)
            while (bombs-- > 0) {
                val list = editableCells()
                if (list.isEmpty()) break
                val (r, c) = list[rng.nextInt(list.size)]
                grid[r][c] = grid[r][c]?.copy(hasBomb = true)
            }
        }

        if (!plain && levelNumber >= 8) {
            var stones = min(10, levelNumber / 9)
            while (stones-- > 0) {
                val list = editableCells()
                if (list.isEmpty()) break
                val (r, c) = list[rng.nextInt(list.size)]
                grid[r][c] = CellBubble(color = BubbleColor.RED, kind = BubbleKind.STONE, hasBomb = false)
            }
        }

        for ((r, c) in HexGridUtils.findFloatingCells(grid, 0)) {
            grid[r][c] = null
        }

        var count = 0
        val colorCounts = IntArray(6)
        for (row in grid) {
            for (cell in row) {
                if (cell != null && cell.kind != BubbleKind.STONE) {
                    count++
                    colorCounts[cell.color.id]++
                }
            }
        }

        var topId = 0
        for (i in 1 until 6) {
            if (colorCounts[i] > colorCounts[topId]) topId = i
        }
        val dominantColor = BubbleColor.fromId(topId)

        val kindIdx = levelNumber % 6
        var kindName = when {
            levelNumber <= 2 -> "clear"
            kindIdx == 1 || kindIdx == 4 -> "score"
            kindIdx == 2 -> "color"
            else -> "clear"
        }
        if (kindName == "color" && colorCounts[topId] < 8) {
            kindName = "clear"
        }

        var shots = ceil(count / (1.55 + 0.6 * min(levelNumber, 100) / 100.0)).toInt() + 6 + if (levelNumber <= 10) 4 else 0
        val objective: LevelObjective = objectiveOverride ?: when (kindName) {
            "score" -> {
                val target = max(100, ((count * 9.5) / 50.0).roundToInt() * 50)
                shots = ceil(target / 32.0).toInt() + 7
                LevelObjective.TargetScore(target)
            }
            "color" -> LevelObjective.PopColor(dominantColor, max(6, ceil(colorCounts[topId] * 0.75).toInt()))
            else -> LevelObjective.ClearAll
        }
        if (shotsOverride != null) shots = shotsOverride

        val dropEvery = dropEveryOverride ?: if (levelNumber < 5) 0 else max(4, 9 - levelNumber / 12)

        return LevelConfig(
            levelNumber = levelNumber,
            grid = grid,
            objective = objective,
            shots = shots,
            palette = palette,
            dropEvery = dropEvery,
            bubbleCount = count,
            pattern = pattern,
            rows = rows,
            dominantColor = dominantColor,
            noBooster = noBooster
        )
    }

    fun generateDailyChallenge(dateKey: String, maxRows: Int = 9): LevelConfig {
        val numericSeed = dateKey.replace("-", "").toIntOrNull() ?: 20261124
        val variant = numericSeed % 4
        val rows = when (variant) {
            1, 2 -> 7
            3 -> 4
            else -> 5
        }
        val base = generateLevel(
            levelNumber = 0,
            maxRows = maxRows,
            seedOverride = numericSeed,
            colorCountOverride = 4,
            rowsOverride = rows,
            patternOverride = LevelPattern.CLUSTERS,
            plain = true,
            dropEveryOverride = 0,
            objectiveOverride = LevelObjective.ClearAll,
            noBooster = variant == 3
        )
        return when (variant) {
            0 -> base.copy(objective = LevelObjective.PopCount(20), shots = 15)
            1 -> base.copy(objective = LevelObjective.TargetScore(650), shots = 20)
            2 -> base.copy(objective = LevelObjective.PopColor(base.dominantColor, 10), shots = 25)
            else -> base.copy(
                objective = LevelObjective.ClearAll,
                shots = ceil(base.bubbleCount / 1.5).toInt() + 8,
                noBooster = true
            )
        }
    }

    fun describeObjective(config: LevelConfig): String {
        return when (val o = config.objective) {
            is LevelObjective.PopCount -> "Clear ${o.target} bubbles within ${config.shots} shots"
            is LevelObjective.TargetScore -> "Score ${o.target} points within ${config.shots} shots"
            is LevelObjective.PopColor -> "Clear ${o.target} ${o.color.label} bubbles within ${config.shots} shots"
            is LevelObjective.ClearAll -> if (config.noBooster) {
                "Clear the board without using boosters"
            } else {
                "Clear all bubbles"
            }
        }
    }
}
