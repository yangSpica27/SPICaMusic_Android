@file:Suppress("ktlint:standard:package-name")

package me.spica27.spicamusic.ui.widget.audio_seekbar

import kotlin.math.min

private const val MAX_PARTICLES = 16
private const val SEED_PARTICLES = 7
private const val EMISSION_INTERVAL_NANOS = 140_000_000L

/** 保存粒子的独立轨迹，坐标单位为 dp。 */
internal class GlowParticleSystem {
    val particles = Array(MAX_PARTICLES) { GlowParticle() }
    private var cursor = 0
    private var serial = 0
    private var direction = 1f
    private var initialized = false
    private var nextEmission = 0L

    fun update(
        timeNanos: Long,
        emitterX: Float,
        trackWidth: Float,
        emissionDirection: Float,
        emitting: Boolean,
    ) {
        if (trackWidth <= 0f) return
        val hasPlayedSpace = if (emissionDirection > 0f) emitterX > 1f else trackWidth - emitterX > 1f
        direction = emissionDirection
        if (!initialized) {
            initialized = true
            if (hasPlayedSpace) {
                repeat(SEED_PARTICLES) { index ->
                    emit(timeNanos - (index + 1) * 80_000_000L, emitterX)
                }
            }
            nextEmission = timeNanos + EMISSION_INTERVAL_NANOS
        }
        if (timeNanos >= nextEmission) {
            if (emitting && hasPlayedSpace) emit(timeNanos, emitterX)
            nextEmission = timeNanos + EMISSION_INTERVAL_NANOS
        }
        particles.forEach { it.update(timeNanos) }
    }

    private fun emit(
        timeNanos: Long,
        emitterX: Float,
    ) {
        particles[cursor].launch(++serial, timeNanos, emitterX, direction)
        cursor = (cursor + 1) % particles.size
    }
}

internal class GlowParticle {
    var id = 0
        private set
    var alive = false
        private set
    var x = 0f
        private set
    var y = 0f
        private set
    var radius = 0f
        private set
    var opacity = 0f
        private set

    private var bornAt = 0L
    private var originX = 0f
    private var velocityX = 0f
    private var velocityY = 0f
    private var lifetime = 0f

    fun clear() {
        alive = false
        opacity = 0f
    }

    fun launch(
        serial: Int,
        timeNanos: Long,
        emitterX: Float,
        direction: Float,
    ) {
        id = serial
        alive = true
        bornAt = timeNanos
        // 出生位置和速度只在发射时确定。
        originX = emitterX
        velocityX = -direction * (18f + variation(serial, 1) * 18f)
        velocityY = (variation(serial, 2) - 0.5f) * 10f - 2f
        lifetime = 0.85f + variation(serial, 3) * 0.55f
        radius = 0.55f + variation(serial, 4) * 0.45f
    }

    fun update(timeNanos: Long) {
        if (!alive) return
        val age = (timeNanos - bornAt) / 1_000_000_000f
        if (age >= lifetime) {
            clear()
            return
        }
        x = originX + velocityX * age
        y = velocityY * age + 4f * age * age
        val remaining = 1f - age / lifetime
        opacity = min(age / 0.08f, 1f) * remaining * remaining
    }

    private fun variation(
        serial: Int,
        salt: Int,
    ): Float = ((serial * 37 + salt * 17) % 101) / 100f
}
