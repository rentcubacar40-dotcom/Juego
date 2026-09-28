package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import java.util.Random

enum class SoundEffect {
    M4A1_FIRE,
    AWP_FIRE,
    SHOTGUN_FIRE,
    PISTOL_FIRE,
    KNIFE_SLASH,
    DRY_FIRE,
    RELOAD_START,
    RELOAD_FINISH,
    HIT_BODY,
    HIT_HEADSHOT,
    EXPLOSION,
    PLAYER_HURT,
    HEARTBEAT,
    PICKUP,
    OBJECTIVE_COMPLETE,
    ENEMY_ALERT,
    PLASMA_FIRE,
    CYBER_SLASH,
    SHIELD_DEFLECT
}

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val random = Random()
    private val sampleRate = 22050
    var soundEnabled = true
    var hapticsEnabled = true

    // Cache pre-generated PCM audio buffers for instant zero-latency playback
    private val audioCache = mutableMapOf<SoundEffect, ShortArray>()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        // Pre-generate procedural military sounds
        scope.launch {
            for (effect in SoundEffect.values()) {
                audioCache[effect] = generatePcm(effect)
            }
        }
    }

    private fun generatePcm(effect: SoundEffect): ShortArray {
        return when (effect) {
            SoundEffect.M4A1_FIRE -> generateM4a1()
            SoundEffect.AWP_FIRE -> generateAwp()
            SoundEffect.SHOTGUN_FIRE -> generateShotgun()
            SoundEffect.PISTOL_FIRE -> generatePistol()
            SoundEffect.KNIFE_SLASH -> generateKnife()
            SoundEffect.DRY_FIRE -> generateDryFire()
            SoundEffect.RELOAD_START -> generateReloadStart()
            SoundEffect.RELOAD_FINISH -> generateReloadFinish()
            SoundEffect.HIT_BODY -> generateHitBody()
            SoundEffect.HIT_HEADSHOT -> generateHitHeadshot()
            SoundEffect.EXPLOSION -> generateExplosion()
            SoundEffect.PLAYER_HURT -> generatePlayerHurt()
            SoundEffect.HEARTBEAT -> generateHeartbeat()
            SoundEffect.PICKUP -> generatePickup()
            SoundEffect.OBJECTIVE_COMPLETE -> generateObjectiveComplete()
            SoundEffect.ENEMY_ALERT -> generateEnemyAlert()
            SoundEffect.PLASMA_FIRE -> generatePlasmaFire()
            SoundEffect.CYBER_SLASH -> generateCyberSlash()
            SoundEffect.SHIELD_DEFLECT -> generateShieldDeflect()
        }
    }

    fun play(effect: SoundEffect) {
        if (!soundEnabled) return
        scope.launch {
            try {
                val pcm = audioCache[effect] ?: generatePcm(effect).also { audioCache[effect] = it }
                playPcm(pcm)
            } catch (_: Exception) {}
        }
    }

    fun vibrateRecoil() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(28)
            }
        } catch (_: Exception) {}
    }

    fun vibrateExplosion() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 40, 180), intArrayOf(0, 255, 0, 200), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(250)
            }
        } catch (_: Exception) {}
    }

    fun vibrateHurt() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, 220))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(70)
            }
        } catch (_: Exception) {}
    }

    private fun playPcm(pcm: ShortArray) {
        val audioTrack = AudioTrack.Builder()
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

        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.play()
        // Release track after playback completes
        val durationMs = (pcm.size.toFloat() / sampleRate * 1000).toLong() + 50
        scope.launch {
            kotlinx.coroutines.delay(durationMs)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    // Procedural sound wave synthesizers
    private fun generateM4a1(): ShortArray {
        val duration = 0.22f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 22f)
            val noise = (random.nextFloat() * 2f - 1f) * exp(-t * 30f)
            val tone = sin(2 * PI * (220 - t * 400) * t).toFloat()
            val sample = (noise * 0.7f + tone * 0.3f) * env
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.85f).toInt().toShort()
        }
        return buffer
    }

    private fun generateAwp(): ShortArray {
        val duration = 0.55f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 7f)
            val crack = if (t < 0.04f) (random.nextFloat() * 2f - 1f) * 1.5f else 0f
            val boom = sin(2 * PI * (90 - t * 80) * t).toFloat() * exp(-t * 10f)
            val reverb = (random.nextFloat() * 2f - 1f) * 0.3f * exp(-t * 8f)
            val sample = (crack * 0.5f + boom * 0.6f + reverb * 0.4f) * env
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.95f).toInt().toShort()
        }
        return buffer
    }

    private fun generateShotgun(): ShortArray {
        val duration = 0.35f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 14f)
            val noise = (random.nextFloat() * 2f - 1f) * exp(-t * 18f)
            val low = sin(2 * PI * (120 - t * 180) * t).toFloat() * 0.5f
            val sample = (noise * 0.7f + low * 0.4f) * env
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.9f).toInt().toShort()
        }
        return buffer
    }

    private fun generatePistol(): ShortArray {
        val duration = 0.18f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 28f)
            val noise = (random.nextFloat() * 2f - 1f) * exp(-t * 35f)
            val punch = sin(2 * PI * 180 * t).toFloat()
            val sample = (noise * 0.6f + punch * 0.4f) * env
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.8f).toInt().toShort()
        }
        return buffer
    }

    private fun generateKnife(): ShortArray {
        val duration = 0.12f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = sin(PI * (t / duration)).toFloat()
            val swoosh = sin(2 * PI * (800 + t * 400) * t).toFloat() * 0.3f + (random.nextFloat() * 2f - 1f) * 0.5f
            val sample = swoosh * env
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        return buffer
    }

    private fun generateDryFire(): ShortArray {
        val duration = 0.05f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val click = sin(2 * PI * 1800 * t).toFloat() * exp(-t * 90f)
            buffer[i] = (click * Short.MAX_VALUE * 0.6f).toInt().toShort()
        }
        return buffer
    }

    private fun generateReloadStart(): ShortArray {
        val duration = 0.15f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val click1 = if (t < 0.04f) sin(2 * PI * 950 * t).toFloat() * exp(-t * 80f) else 0f
            val click2 = if (t > 0.08f) sin(2 * PI * 1200 * (t - 0.08f)).toFloat() * exp(-(t - 0.08f) * 90f) else 0f
            val sample = click1 + click2
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.6f).toInt().toShort()
        }
        return buffer
    }

    private fun generateReloadFinish(): ShortArray {
        val duration = 0.2f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val slap = (random.nextFloat() * 2f - 1f) * exp(-t * 40f) * 0.4f
            val lock = if (t > 0.06f) sin(2 * PI * 750 * (t - 0.06f)).toFloat() * exp(-(t - 0.06f) * 50f) else 0f
            val sample = slap + lock
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        return buffer
    }

    private fun generateHitBody(): ShortArray {
        val duration = 0.08f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val thud = sin(2 * PI * 220 * t).toFloat() * exp(-t * 50f)
            val marker = sin(2 * PI * 2200 * t).toFloat() * exp(-t * 70f) * 0.4f
            val sample = thud * 0.6f + marker
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        return buffer
    }

    private fun generateHitHeadshot(): ShortArray {
        val duration = 0.16f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val ping1 = sin(2 * PI * 2600 * t).toFloat() * exp(-t * 25f)
            val ping2 = if (t > 0.04f) sin(2 * PI * 3400 * (t - 0.04f)).toFloat() * exp(-(t - 0.04f) * 30f) else 0f
            val sample = (ping1 * 0.5f + ping2 * 0.6f)
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.85f).toInt().toShort()
        }
        return buffer
    }

    private fun generateExplosion(): ShortArray {
        val duration = 0.8f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 4.5f)
            val lowBoom = sin(2 * PI * (65 - t * 40) * t).toFloat() * 0.8f
            val noise = (random.nextFloat() * 2f - 1f) * 0.7f * exp(-t * 6f)
            val sample = (lowBoom + noise) * env
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.95f).toInt().toShort()
        }
        return buffer
    }

    private fun generatePlayerHurt(): ShortArray {
        val duration = 0.2f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 18f)
            val groan = sin(2 * PI * 130 * t).toFloat() * 0.7f + (random.nextFloat() * 2f - 1f) * 0.3f
            val sample = groan * env
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.75f).toInt().toShort()
        }
        return buffer
    }

    private fun generateHeartbeat(): ShortArray {
        val duration = 0.35f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val pulse1 = if (t < 0.12f) sin(2 * PI * 48 * t).toFloat() * exp(-t * 25f) else 0f
            val pulse2 = if (t in 0.14f..0.28f) sin(2 * PI * 44 * (t - 0.14f)).toFloat() * exp(-(t - 0.14f) * 25f) * 0.8f else 0f
            val sample = pulse1 + pulse2
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.9f).toInt().toShort()
        }
        return buffer
    }

    private fun generatePickup(): ShortArray {
        val duration = 0.18f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = if (t < 0.08f) 880f else 1320f
            val sample = sin(2 * PI * freq * t).toFloat() * exp(-t * 12f)
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.55f).toInt().toShort()
        }
        return buffer
    }

    private fun generateObjectiveComplete(): ShortArray {
        val duration = 0.6f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = when {
                t < 0.15f -> 523.25f // C5
                t < 0.30f -> 659.25f // E5
                t < 0.45f -> 783.99f // G5
                else -> 1046.50f    // C6
            }
            val sample = sin(2 * PI * freq * t).toFloat() * exp(-(t % 0.15f) * 10f) * 0.6f
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        return buffer
    }

    private fun generateEnemyAlert(): ShortArray {
        val duration = 0.14f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val sample = sin(2 * PI * (440 + t * 600) * t).toFloat() * exp(-t * 15f)
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        return buffer
    }

    private fun generatePlasmaFire(): ShortArray {
        val duration = 0.45f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            // High frequency charging sweep dropping to deep electromagnetic sub-bass
            val freq = (1800f * exp(-t * 18f) + 75f)
            val subBass = sin(2 * PI * freq * t).toFloat()
            val electricWhine = sin(2 * PI * (3200 - t * 4000) * t).toFloat() * exp(-t * 22f) * 0.4f
            val sample = (subBass * 0.7f + electricWhine) * exp(-t * 6f)
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.95f).toInt().toShort()
        }
        return buffer
    }

    private fun generateCyberSlash(): ShortArray {
        val duration = 0.16f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val hum = sin(2 * PI * (650 + sin(t * 120f) * 80f) * t).toFloat()
            val whoosh = (random.nextFloat() * 2f - 1f) * exp(-t * 30f)
            val sample = (hum * 0.55f + whoosh * 0.45f) * exp(-t * 16f)
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.85f).toInt().toShort()
        }
        return buffer
    }

    private fun generateShieldDeflect(): ShortArray {
        val duration = 0.25f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val shimmer = sin(2 * PI * (2400 - t * 3000) * t).toFloat() * exp(-t * 12f)
            val resonance = sin(2 * PI * 520 * t).toFloat() * exp(-t * 8f) * 0.6f
            val sample = shimmer * 0.6f + resonance * 0.4f
            buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.75f).toInt().toShort()
        }
        return buffer
    }
}
