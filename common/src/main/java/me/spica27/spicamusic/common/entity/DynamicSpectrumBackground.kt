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

    object FluidWarp : DynamicSpectrumBackground(
        "fluid_warp",
        "动态流体封面",
    )

    object BlurCover : DynamicSpectrumBackground(
        "blur_cover",
        "模糊封面",
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
            is FluidWarp -> "动态流体封面"
            is BlurCover -> "模糊封面"
            is OFF -> "关闭"
        }
    }

    companion object {
        fun fromString(value: String): DynamicSpectrumBackground {
            return when (value) {
                TopGlow.value -> TopGlow
                LiquidAurora.value -> LiquidAurora
                EffectShader.value -> EffectShader
                FluidWarp.value -> FluidWarp
                BlurCover.value -> BlurCover
                OFF.value -> OFF
                else -> OFF
            }
        }

        val presets: List<DynamicSpectrumBackground>
            get() = listOf(TopGlow, LiquidAurora, EffectShader, FluidWarp, BlurCover, OFF)
    }

}