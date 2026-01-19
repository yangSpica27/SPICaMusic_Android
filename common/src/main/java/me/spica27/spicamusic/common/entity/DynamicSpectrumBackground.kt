package me.spica27.spicamusic.common.entity

sealed class DynamicSpectrumBackground(
  val value: String,
  val name: String,
) {


  object TopGlow : DynamicSpectrumBackground(
    "top_glow",
    "顶部流光",
  )

  object LiquidAurora : DynamicSpectrumBackground(
    "liquid_aurora",
    "液态极光",
  )

  object BubblePulse : DynamicSpectrumBackground(
    "bubble_pulse",
    "浮泡律动",
  )

  object NeonGrid : DynamicSpectrumBackground(
    "neon_grid",
    "霓虹栅格",
  )

  object ParticleStarfield : DynamicSpectrumBackground(
    "particle_starfield",
    "粒子星空",
  )

  object RippleWave : DynamicSpectrumBackground(
    "ripple_wave",
    "波纹涟漪",
  )

  object SpectrumHelix : DynamicSpectrumBackground(
    "spectrum_helix",
    "光谱螺旋",
  )

  object EnergyPulse : DynamicSpectrumBackground(
    "energy_pulse",
    "能量脉冲",
  )

  object FluidVortex : DynamicSpectrumBackground(
    "fluid_vortex",
    "流体漩涡",
  )

  object OFF : DynamicSpectrumBackground(
    "off",
    "关闭",
  )


  override fun toString(): String {
    return when (this) {
      is TopGlow -> "顶部流光"
      is LiquidAurora -> "液态极光"
      is BubblePulse -> "浮泡律动"
      is NeonGrid -> "霓虹栅格"
      is ParticleStarfield -> "粒子星空"
      is RippleWave -> "波纹涟漪"
      is SpectrumHelix -> "光谱螺旋"
      is EnergyPulse -> "能量脉冲"
      is FluidVortex -> "流体漩涡"
      is OFF -> "关闭"
    }
  }

  companion object {
    fun fromString(value: String): DynamicSpectrumBackground {
      return when (value) {
        TopGlow.value -> TopGlow
        LiquidAurora.value -> LiquidAurora
        BubblePulse.value -> BubblePulse
        NeonGrid.value -> NeonGrid
        ParticleStarfield.value -> ParticleStarfield
        RippleWave.value -> RippleWave
        SpectrumHelix.value -> SpectrumHelix
        EnergyPulse.value -> EnergyPulse
        FluidVortex.value -> FluidVortex
        OFF.value -> OFF
        else -> OFF
      }
    }

    val presets: List<DynamicSpectrumBackground>
      get() = listOf(TopGlow, LiquidAurora, BubblePulse, EnergyPulse, OFF)
  }

}