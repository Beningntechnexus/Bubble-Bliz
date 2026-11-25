package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Original procedural PCM sound-effect and background music synthesizer using native Android AudioTrack.
 * Produces warm, cheerful, non-copyrighted casual game audio optimized for mobile playback.
 */
class SoundEngine {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile var sfxEnabled: Boolean = true
    @Volatile var musicEnabled: Boolean = true
    @Volatile var sfxVolume: Float = 0.85f
    @Volatile var musicVolume: Float = 0.55f
    @Volatile private var isForeground: Boolean = true

    private var musicJob: Job? = null
    private val activeTracks = mutableListOf<AudioTrack>()
    private val trackLock = Any()

    // Cached synthesized PCM buffers
    private val clickBuf by lazy { synthTone(540f, 0.065f, Wave.SINE, 0.25f, slideHz = 220f) }
    private val shootBuf by lazy { synthTone(290f, 0.13f, Wave.TRIANGLE, 0.35f, slideHz = 420f) }
    private val bounceBuf by lazy { synthTone(740f, 0.045f, Wave.SINE, 0.18f, slideHz = 120f) }
    private val stickBuf by lazy { synthTone(220f, 0.085f, Wave.SINE, 0.28f, slideHz = -75f) }
    private val popBufs by lazy {
        List(5) { idx ->
            synthTone(480f + idx * 75f, 0.12f, Wave.SINE, 0.36f, slideHz = 310f)
        }
    }
    private val dropBuf by lazy { synthTone(320f, 0.28f, Wave.SINE, 0.24f, slideHz = -160f) }
    private val comboBuf by lazy {
        synthChord(floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f), 0.11f, 0.055f, Wave.TRIANGLE, 0.28f)
    }
    private val bombBuf by lazy { synthExplosion(0.42f) }
    private val rainbowBuf by lazy {
        synthChord(floatArrayOf(523.25f, 587.33f, 659.25f, 783.99f, 987.77f, 1174.66f), 0.09f, 0.04f, Wave.SINE, 0.26f)
    }
    private val lightningBuf by lazy { synthTone(1150f, 0.30f, Wave.SAW, 0.26f, slideHz = -880f) }
    private val fireBuf by lazy { synthWhoosh(0.32f) }
    private val freezeBuf by lazy {
        synthChord(floatArrayOf(1318.5f, 1567.98f, 2093f), 0.16f, 0.045f, Wave.SINE, 0.22f)
    }
    private val coinBuf by lazy {
        synthChord(floatArrayOf(1120f, 1540f), 0.10f, 0.07f, Wave.SQUARE, 0.16f)
    }
    private val treasureBuf by lazy {
        synthChord(floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f, 1318.5f), 0.18f, 0.07f, Wave.TRIANGLE, 0.30f)
    }
    private val unlockBuf by lazy {
        synthChord(floatArrayOf(440f, 554.37f, 659.25f, 880f), 0.15f, 0.06f, Wave.SINE, 0.26f)
    }
    private val victoryBuf by lazy {
        synthChord(floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f), 0.24f, 0.11f, Wave.TRIANGLE, 0.32f)
    }
    private val gameOverBuf by lazy {
        synthChord(floatArrayOf(392f, 349.23f, 311.13f, 246.94f), 0.26f, 0.14f, Wave.TRIANGLE, 0.28f)
    }
    private val rewardBuf by lazy {
        synthChord(floatArrayOf(659.25f, 783.99f, 987.77f, 1318.5f), 0.16f, 0.055f, Wave.SINE, 0.28f)
    }

    private enum class Wave { SINE, TRIANGLE, SQUARE, SAW }

    fun setForeground(foreground: Boolean) {
        isForeground = foreground
        if (foreground && musicEnabled) {
            startMusicIfNeeded()
        } else if (!foreground) {
            stopMusic()
        }
    }

    fun updatePreferences(sfxOn: Boolean, musicOn: Boolean, sfxVol: Float, musicVol: Float) {
        sfxEnabled = sfxOn
        musicEnabled = musicOn
        sfxVolume = sfxVol.coerceIn(0f, 1f)
        musicVolume = musicVol.coerceIn(0f, 1f)
        if (musicEnabled && isForeground) {
            startMusicIfNeeded()
        } else {
            stopMusic()
        }
    }

    fun playButton() = playBuffer(clickBuf)
    fun playShoot() = playBuffer(shootBuf)
    fun playBounce() = playBuffer(bounceBuf)
    fun playStick() = playBuffer(stickBuf)
    fun playPop(cascadeIndex: Int = 0) = playBuffer(popBufs[cascadeIndex.coerceIn(0, popBufs.lastIndex)])
    fun playDropFloat() = playBuffer(dropBuf)
    fun playCombo() = playBuffer(comboBuf)
    fun playBomb() = playBuffer(bombBuf)
    fun playRainbow() = playBuffer(rainbowBuf)
    fun playLightning() = playBuffer(lightningBuf)
    fun playFire() = playBuffer(fireBuf)
    fun playFreeze() = playBuffer(freezeBuf)
    fun playCoin() = playBuffer(coinBuf)
    fun playTreasure() = playBuffer(treasureBuf)
    fun playLevelUnlock() = playBuffer(unlockBuf)
    fun playVictory() = playBuffer(victoryBuf)
    fun playGameOver() = playBuffer(gameOverBuf)
    fun playReward() = playBuffer(rewardBuf)

    private fun playBuffer(pcm: ShortArray) {
        if (!sfxEnabled || sfxVolume <= 0.01f || !isForeground) return
        scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.setVolume(sfxVolume)
                track.play()

                synchronized(trackLock) {
                    activeTracks.add(track)
                    if (activeTracks.size > 10) {
                        val oldest = activeTracks.removeAt(0)
                        runCatching { oldest.stop(); oldest.release() }
                    }
                }
            } catch (_: Throwable) {
                // Safe no-op in headless/Robolectric environments
            }
        }
    }

    fun startMusicIfNeeded() {
        if (musicJob?.isActive == true || !musicEnabled || !isForeground) return
        musicJob = scope.launch {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()

                // Cheerful, gentle 16-step C-major / A-minor / F-major / G-major casual celesta loop
                val roots = floatArrayOf(261.63f, 220.00f, 174.61f, 196.00f) // C4, A3, F3, G3
                val intervals = intArrayOf(0, 4, 7, 12, 7, 4, 9, 5)
                val stepSeconds = 0.29f
                val stepSamples = (sampleRate * stepSeconds).toInt()
                val buf = ShortArray(stepSamples)
                var stepIdx = 0

                while (isActive && musicEnabled && isForeground) {
                    val chordIdx = (stepIdx / 8) % roots.size
                    val root = roots[chordIdx]
                    val semitone = intervals[stepIdx % intervals.size]
                    val melodyHz = root * 2f * 2f.pow(semitone / 12f)
                    val bassHz = root * 0.5f

                    val vol = musicVolume * 0.16f
                    for (i in 0 until stepSamples) {
                        val t = i.toFloat() / sampleRate
                        val env = exp(-t * 6.5f) * min(1f, t / 0.015f)
                        val padEnv = exp(-t * 2.2f) * min(1f, t / 0.03f)
                        val mel = sin(2.0 * PI * melodyHz * t).toFloat() * 0.7f +
                            sin(4.0 * PI * melodyHz * t).toFloat() * 0.12f
                        val bass = sin(2.0 * PI * bassHz * t).toFloat() * 0.45f
                        val sample = ((mel * env + bass * padEnv) * vol).coerceIn(-0.95f, 0.95f)
                        buf[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.setVolume(musicVolume.coerceIn(0f, 1f))
                    track.write(buf, 0, buf.size)
                    stepIdx++
                }
            } catch (_: Throwable) {
                // Safe fallback when AudioTrack is unavailable
            } finally {
                runCatching {
                    track?.stop()
                    track?.release()
                }
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun release() {
        stopMusic()
        synchronized(trackLock) {
            activeTracks.forEach { runCatching { it.stop(); it.release() } }
            activeTracks.clear()
        }
    }

    private fun synthTone(
        startHz: Float,
        durationSec: Float,
        wave: Wave,
        amp: Float,
        slideHz: Float = 0f
    ): ShortArray {
        val count = max(1, (sampleRate * durationSec).toInt())
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toFloat() / count
            val t = i.toFloat() / sampleRate
            val freq = max(40f, startHz + slideHz * progress)
            phase += 2.0 * PI * freq / sampleRate
            val env = min(1f, t / 0.008f) * (1f - progress).pow(1.4f)
            val raw = sampleWave(wave, phase)
            out[i] = ((raw * amp * env).coerceIn(-0.95f, 0.95f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthChord(
        notes: FloatArray,
        noteDur: Float,
        stepOffset: Float,
        wave: Wave,
        amp: Float
    ): ShortArray {
        val totalDur = noteDur + stepOffset * max(0, notes.size - 1) + 0.04f
        val count = (sampleRate * totalDur).toInt()
        val acc = FloatArray(count)
        for ((idx, hz) in notes.withIndex()) {
            val startIdx = (idx * stepOffset * sampleRate).toInt()
            val noteSamples = (noteDur * sampleRate).toInt()
            var phase = 0.0
            for (j in 0 until noteSamples) {
                val target = startIdx + j
                if (target >= count) break
                val p = j.toFloat() / noteSamples
                phase += 2.0 * PI * hz / sampleRate
                val env = min(1f, (j.toFloat() / sampleRate) / 0.01f) * (1f - p).pow(1.6f)
                acc[target] += sampleWave(wave, phase) * amp * env
            }
        }
        return ShortArray(count) { i ->
            (acc[i].coerceIn(-0.95f, 0.95f) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun synthExplosion(durationSec: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        var lp = 0f
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val freq = 125f - 85f * p
            phase += 2.0 * PI * freq / sampleRate
            val noise = (Random.nextFloat() * 2f - 1f)
            lp = lp * 0.78f + noise * 0.22f
            val sub = sin(phase).toFloat() * 0.55f
            val env = (1f - p).pow(1.7f)
            val sample = ((sub + lp * 0.75f) * 0.45f * env).coerceIn(-0.95f, 0.95f)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthWhoosh(durationSec: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var lp = 0f
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val noise = Random.nextFloat() * 2f - 1f
            lp = lp * 0.65f + noise * 0.35f
            val env = sin(PI * p).toFloat()
            val sample = (lp * 0.38f * env).coerceIn(-0.95f, 0.95f)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun sampleWave(wave: Wave, phase: Double): Float {
        val norm = ((phase % (2.0 * PI)) / (2.0 * PI)).toFloat()
        return when (wave) {
            Wave.SINE -> sin(phase).toFloat()
            Wave.SQUARE -> if (norm < 0.5f) 1f else -1f
            Wave.SAW -> 2f * norm - 1f
            Wave.TRIANGLE -> if (norm < 0.5f) 4f * norm - 1f else 3f - 4f * norm
        }
    }
}
