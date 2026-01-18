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
        OFF.value -> OFF
        else -> OFF
      }
    }

    val presets: List<DynamicSpectrumBackground>
      get() = listOf(TopGlow, LiquidAurora, BubblePulse, NeonGrid, OFF)
  }

}