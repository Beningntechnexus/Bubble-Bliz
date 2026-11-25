package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class RewardedPlacement(val id: String) {
    EXTRA_SHOTS("extra_shots"),
    DOUBLE_COINS("double_coins"),
    FREE_BOOSTER("free_booster"),
    DAILY_BONUS("daily_bonus"),
    EXTRA_SPIN("extra_spin")
}

interface RewardedAdGateway {
    val isAdReady: StateFlow<Boolean>
    val isAdLoading: StateFlow<Boolean>
    fun initialize(context: Context)
    fun preload(activity: Activity)
    fun showRewardedAd(
        activity: Activity,
        placement: RewardedPlacement,
        onRewardEarned: () -> Unit,
        onAdFailed: (String) -> Unit
    )
}

/**
 * Official Start.io (StartApp) In-App SDK integration for Bubble Blitz.
 * - Uses App ID 209961859.
 * - Disables forced launch splash and return ads so gameplay is never interrupted.
 * - Validates rewarded video completion strictly via VideoListener + AdDisplayListener with duplicate-reward protection.
 */
class StartIoAdManager : RewardedAdGateway {
    companion object {
        const val START_IO_APP_ID = "209961859"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val initialized = AtomicBoolean(false)

    private val _isAdReady = MutableStateFlow(false)
    override val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isAdLoading = MutableStateFlow(false)
    override val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    private var rewardedAd: StartAppAd? = null
    private var showInProgress = false

    override fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        runCatching {
            StartAppSDK.initParams(context.applicationContext, START_IO_APP_ID)
                .setReturnAdsEnabled(false)
                .setCallback {
                    // SDK ready; ads will be preloaded lazily when an Activity attaches.
                }
                .init()
            StartAppAd.disableSplash()
        }
    }

    override fun preload(activity: Activity) {
        initialize(activity)
        if (_isAdLoading.value || _isAdReady.value || activity.isFinishing || activity.isDestroyed) return

        _isAdLoading.value = true
        val adInstance = StartAppAd(activity)
        rewardedAd = adInstance

        runCatching {
            adInstance.loadAd(
                StartAppAd.AdMode.REWARDED_VIDEO,
                object : AdEventListener {
                    override fun onReceiveAd(ad: Ad) {
                        mainHandler.post {
                            _isAdLoading.value = false
                            _isAdReady.value = true
                        }
                    }

                    override fun onFailedToReceiveAd(ad: Ad?) {
                        mainHandler.post {
                            _isAdLoading.value = false
                            _isAdReady.value = false
                        }
                    }
                }
            )
        }.onFailure {
            _isAdLoading.value = false
            _isAdReady.value = false
        }
    }

    override fun showRewardedAd(
        activity: Activity,
        placement: RewardedPlacement,
        onRewardEarned: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        initialize(activity)
        if (showInProgress) {
            onAdFailed("An ad is already in progress.")
            return
        }

        val currentAd = rewardedAd
        if (currentAd != null && _isAdReady.value) {
            displayLoadedAd(activity, currentAd, onRewardEarned, onAdFailed)
            return
        }

        // Load on demand if not preloaded yet; fail gracefully if unavailable
        _isAdLoading.value = true
        val freshAd = StartAppAd(activity)
        rewardedAd = freshAd
        runCatching {
            freshAd.loadAd(
                StartAppAd.AdMode.REWARDED_VIDEO,
                object : AdEventListener {
                    override fun onReceiveAd(ad: Ad) {
                        mainHandler.post {
                            _isAdLoading.value = false
                            _isAdReady.value = true
                            displayLoadedAd(activity, freshAd, onRewardEarned, onAdFailed)
                        }
                    }

                    override fun onFailedToReceiveAd(ad: Ad?) {
                        mainHandler.post {
                            _isAdLoading.value = false
                            _isAdReady.value = false
                            onAdFailed("No rewarded ad available right now.")
                        }
                    }
                }
            )
        }.onFailure {
            _isAdLoading.value = false
            _isAdReady.value = false
            onAdFailed("Unable to load rewarded ad.")
        }
    }

    private fun displayLoadedAd(
        activity: Activity,
        ad: StartAppAd,
        onRewardEarned: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onAdFailed("Activity unavailable.")
            return
        }

        showInProgress = true
        _isAdReady.value = false

        val videoCompleted = AtomicBoolean(false)
        val callbackDispatched = AtomicBoolean(false)

        ad.setVideoListener {
            videoCompleted.set(true)
        }

        val shown = runCatching {
            ad.showAd(object : AdDisplayListener {
                override fun adHidden(hiddenAd: Ad) {
                    mainHandler.post {
                        showInProgress = false
                        if (callbackDispatched.compareAndSet(false, true)) {
                            if (videoCompleted.get()) {
                                onRewardEarned()
                            } else {
                                onAdFailed("Ad closed before completion. No reward granted.")
                            }
                        }
                        preload(activity)
                    }
                }

                override fun adDisplayed(displayedAd: Ad) {
                    // Reward is strictly gated on video completion
                }

                override fun adClicked(clickedAd: Ad) {
                    // No reward for click alone
                }

                override fun adNotDisplayed(failedAd: Ad) {
                    mainHandler.post {
                        showInProgress = false
                        if (callbackDispatched.compareAndSet(false, true)) {
                            onAdFailed("Ad could not be displayed. No reward granted.")
                        }
                        preload(activity)
                    }
                }
            })
        }.getOrDefault(false)

        if (!shown) {
            showInProgress = false
            if (callbackDispatched.compareAndSet(false, true)) {
                onAdFailed("Rewarded ad is not ready yet.")
            }
            preload(activity)
        }
    }
}
