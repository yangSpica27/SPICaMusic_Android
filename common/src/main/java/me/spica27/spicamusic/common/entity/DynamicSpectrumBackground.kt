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

  object EffectShader : DynamicSpectrumBackground(
    "effect_shader",
    "流体效果",
  )

  object OFF : DynamicSpectrumBackground(
    "off",
    "关闭",
  )


  override fun toString(): String {
    return when (this) {
      is TopGlow -> "顶部流光"
      is LiquidAurora -> "液态极光"
      is EffectShader -> "流体效果"
      is OFF -> "关闭"
    }
  }

  companion object {
    fun fromString(value: String): DynamicSpectrumBackground {
      return when (value) {
        TopGlow.value -> TopGlow
        LiquidAurora.value -> LiquidAurora
        EffectShader.value -> EffectShader
        OFF.value -> OFF
        else -> OFF
      }
    }

    val presets: List<DynamicSpectrumBackground>
      get() = listOf(TopGlow, LiquidAurora,EffectShader, OFF)
  }

}