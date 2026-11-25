package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BubbleBlueDark
import com.example.ui.theme.BubbleBlueLight
import com.example.ui.theme.BubbleBlueMid
import com.example.ui.theme.BubbleGreenDark
import com.example.ui.theme.BubbleGreenLight
import com.example.ui.theme.BubbleGreenMid
import com.example.ui.theme.BubbleOrangeDark
import com.example.ui.theme.BubbleOrangeLight
import com.example.ui.theme.BubbleOrangeMid
import com.example.ui.theme.BubblePurpleDark
import com.example.ui.theme.BubblePurpleLight
import com.example.ui.theme.BubblePurpleMid
import com.example.ui.theme.BubbleRedDark
import com.example.ui.theme.BubbleRedLight
import com.example.ui.theme.BubbleRedMid
import com.example.ui.theme.BubbleYellowDark
import com.example.ui.theme.BubbleYellowLight
import com.example.ui.theme.BubbleYellowMid

const val GRID_COLS = 10
const val MAX_LEVELS = 100

enum class BubbleColor(
    val id: Int,
    val label: String,
    val light: Color,
    val mid: Color,
    val dark: Color
) {
    RED(0, "red", BubbleRedLight, BubbleRedMid, BubbleRedDark),
    YELLOW(1, "yellow", BubbleYellowLight, BubbleYellowMid, BubbleYellowDark),
    GREEN(2, "green", BubbleGreenLight, BubbleGreenMid, BubbleGreenDark),
    BLUE(3, "blue", BubbleBlueLight, BubbleBlueMid, BubbleBlueDark),
    PURPLE(4, "purple", BubblePurpleLight, BubblePurpleMid, BubblePurpleDark),
    ORANGE(5, "orange", BubbleOrangeLight, BubbleOrangeMid, BubbleOrangeDark);

    companion object {
        fun fromId(id: Int): BubbleColor = entries.getOrElse(id) { RED }
    }
}

enum class BubbleKind {
    NORMAL,
    STONE
}

data class CellBubble(
    val color: BubbleColor,
    val kind: BubbleKind = BubbleKind.NORMAL,
    val hasBomb: Boolean = false
)

enum class BoosterType(
    val key: String,
    val title: String,
    val description: String,
    val price: Int
) {
    BOMB("bomb", "Bomb", "Explodes and removes nearby bubbles.", 150),
    RAINBOW("rainbow", "Rainbow", "Matches any bubble color it touches.", 200),
    LIGHTNING("lightning", "Lightning", "Zaps away a whole column of bubbles.", 250),
    FIRE("fire", "Fire", "Burns through up to 6 bubbles in its path.", 300),
    FREEZE("freeze", "Freeze", "Stops new rows from dropping for 5 shots.", 200);

    companion object {
        fun fromKey(key: String): BoosterType? = entries.firstOrNull { it.key == key }
    }
}

sealed class LevelObjective {
    data object ClearAll : LevelObjective()
    data class TargetScore(val target: Int) : LevelObjective()
    data class PopColor(val color: BubbleColor, val target: Int) : LevelObjective()
    data class PopCount(val target: Int) : LevelObjective()
}

enum class LevelPattern {
    CLUSTERS,
    PYRAMID,
    STRIPES,
    DIAMOND,
    CHECKER,
    WAVES,
    COLUMNS,
    RING
}

data class LevelConfig(
    val levelNumber: Int,
    val grid: List<List<CellBubble?>>,
    val objective: LevelObjective,
    val shots: Int,
    val palette: List<BubbleColor>,
    val dropEvery: Int,
    val bubbleCount: Int,
    val pattern: LevelPattern,
    val rows: Int,
    val dominantColor: BubbleColor,
    val noBooster: Boolean = false
)

enum class EndReason {
    NONE,
    DANGER_LINE,
    OUT_OF_SHOTS
}

data class DailyRewardTier(
    val day: Int,
    val title: String,
    val coins: Int = 0,
    val booster: BoosterType? = null,
    val randomBoosters: Int = 0,
    val allBoosters: Boolean = false
)

val DAILY_REWARD_TIERS = listOf(
    DailyRewardTier(day = 1, title = "100 Coins", coins = 100),
    DailyRewardTier(day = 2, title = "150 Coins", coins = 150),
    DailyRewardTier(day = 3, title = "1 Bomb", booster = BoosterType.BOMB),
    DailyRewardTier(day = 4, title = "250 Coins", coins = 250),
    DailyRewardTier(day = 5, title = "2 Boosters", randomBoosters = 2),
    DailyRewardTier(day = 6, title = "400 Coins", coins = 400),
    DailyRewardTier(day = 7, title = "Treasure!", coins = 500, allBoosters = true)
)

data class SpinSegment(
    val label: String,
    val coins: Int = 0,
    val booster: BoosterType? = null,
    val extraShots: Int = 0,
    val weight: Int,
    val color: Color
)

val SPIN_SEGMENTS = listOf(
    SpinSegment("50", coins = 50, weight = 26, color = Color(0xFF7B3FE4)),
    SpinSegment("Bomb", booster = BoosterType.BOMB, weight = 10, color = Color(0xFFFF7A00)),
    SpinSegment("100", coins = 100, weight = 18, color = Color(0xFF1E7BFF)),
    SpinSegment("Rainbow", booster = BoosterType.RAINBOW, weight = 10, color = Color(0xFF2FBF2F)),
    SpinSegment("Lightning", booster = BoosterType.LIGHTNING, weight = 10, color = Color(0xFFE5212B)),
    SpinSegment("+3 Shots", extraShots = 3, weight = 12, color = Color(0xFF9B3DF0)),
    SpinSegment("200", coins = 200, weight = 6, color = Color(0xFFFFC400)),
    SpinSegment("Fire", booster = BoosterType.FIRE, weight = 8, color = Color(0xFF00A6C8))
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val target: Int
)

val ACHIEVEMENTS = listOf(
    AchievementDef("pop1", "First Pop", "Pop your first bubble", 25, 1),
    AchievementDef("win1", "First Victory", "Complete a level", 50, 1),
    AchievementDef("lv10", "10 Levels Completed", "Complete 10 levels", 100, 10),
    AchievementDef("lv25", "25 Levels Completed", "Complete 25 levels", 250, 25),
    AchievementDef("lv100", "100 Levels Completed", "Conquer Green Valley", 1000, 100),
    AchievementDef("b1000", "Bubble Crusher", "Destroy 1,000 bubbles", 300, 1000),
    AchievementDef("combo10", "10 Combo", "Pop with 10 shots in a row", 150, 10),
    AchievementDef("star3", "3-Star Master", "Earn 3 stars on 10 levels", 250, 10),
    AchievementDef("boost", "Booster Expert", "Use 25 boosters", 200, 25)
)
