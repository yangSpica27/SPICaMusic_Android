package me.spica27.spicamusic.widget.glass

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.intellij.lang.annotations.Language
import kotlin.random.Random

internal data class GlassElement(
    val id: String,
    val position: Offset,
    val size: Size,
    val scale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float,
    val elevation: Float,
    val tint: Color,
    val darkness: Float,
    val warpEdges: Float,
) {
    // Check equality with tolerance for Float values
    fun equalsWithTolerance(other: GlassElement): Boolean {
        if (id != other.id) return false

        val tolerance = 0.01f
        val positionDiff = (position - other.position)
        val positionDistance =
            kotlin.math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y)
        return positionDistance < tolerance &&
            kotlin.math.abs(size.width - other.size.width) < tolerance &&
            kotlin.math.abs(size.height - other.size.height) < tolerance &&
            kotlin.math.abs(scale - other.scale) < tolerance &&
            kotlin.math.abs(blur - other.blur) < tolerance &&
            kotlin.math.abs(centerDistortion - other.centerDistortion) < tolerance &&
            kotlin.math.abs(cornerRadius - other.cornerRadius) < tolerance &&
            kotlin.math.abs(elevation - other.elevation) < tolerance &&
            kotlin.math.abs(darkness - other.darkness) < tolerance &&
            kotlin.math.abs(warpEdges - other.warpEdges) < tolerance &&
            tint == other.tint
    }
}

interface GlassScope {
    fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Transparent,
        darkness: Float = 0f,
        warpEdges: Float = 0f,
    ): Modifier
}

interface GlassBoxScope :
    BoxScope,
    GlassScope

@Composable
fun GlassBoxScope.GlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0)
    scale: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    blur: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    centerDistortion: Float = 0f,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    elevation: Dp = 0.dp,
    tint: Color = Color.Transparent,
    @FloatRange(from = 0.0, to = 1.0)
    darkness: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    warpEdges: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    Box(
        modifier =
            modifier.glassBackground(
                id,
                scale.coerceIn(0f, 1f),
                blur.coerceIn(0f, 1f),
                centerDistortion.coerceIn(0f, 1f),
                shape,
                elevation,
                tint,
                darkness.coerceIn(0f, 1f),
                warpEdges.coerceIn(0f, 1f),
            ),
        contentAlignment,
        propagateMinConstraints,
        content,
    )
}

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope,
) : GlassBoxScope,
    BoxScope by boxScope,
    GlassScope by glassScope

private class GlassScopeImpl(
    private val density: Density,
) : GlassScope {
    var updateCounter by mutableStateOf(0)
    val elements: MutableList<GlassElement> = mutableListOf()
    private val activeElements = mutableSetOf<String>()

    fun markElementAsActive(elementId: String) {
        activeElements.add(elementId)
    }

    fun cleanupInactiveElements() {
        val elementsToRemove = elements.filter { it.id !in activeElements }
        if (elementsToRemove.isNotEmpty()) {
            elements.removeAll { it.id !in activeElements }
            updateCounter++
        }
        activeElements.clear()
    }

    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
    ): Modifier =
        this
            .background(color = Color.Transparent, shape = shape)
            .onGloballyPositioned { coordinates ->
                val elementId = "glass_$id"
                markElementAsActive(elementId)

                val position = coordinates.positionInRoot()
                val size = coordinates.size.toSize()

                val element =
                    GlassElement(
                        id = elementId,
                        position = position,
                        size = size,
                        cornerRadius = shape.topStart.toPx(size, density),
                        scale = scale,
                        blur = blur,
                        centerDistortion = centerDistortion,
                        elevation = with(density) { elevation.toPx() },
                        tint = tint,
                        darkness = darkness,
                        warpEdges = warpEdges,
                    )

                // Find existing element with same ID
                val existingIndex = elements.indexOfFirst { it.id == element.id }

                // Update only if element changed
                if (existingIndex == -1) {
                    elements.add(element)
                    updateCounter++
                } else {
                    // Check if element changed with Float tolerance
                    val existing = elements[existingIndex]
                    if (!existing.equalsWithTolerance(element)) {
                        elements[existingIndex] = element
                        updateCounter++
                    }
                }
            }
}

/**
 * Fallback implementation for Android versions < 13 (API 33)
 * Uses standard Compose modifiers to simulate glass effects
 */
private class GlassScopeFallbackImpl(
    private val density: Density,
) : GlassScope {
    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
    ): Modifier {
        // Create a glass-like effect using available modifiers
        val glassTint =
            if (tint == Color.Transparent) {
                Color.White.copy(alpha = 0.1f)
            } else {
                tint.copy(alpha = (tint.alpha * 0.9f).coerceIn(0f, 1f))
            }

        // Create a darker overlay for the darkness effect
        val darknessOverlay =
            if (darkness > 0f) {
                Color.Black.copy(alpha = darkness * 0.3f)
            } else {
                Color.Transparent
            }

        // Create a gradient for glass-like appearance
        val glassGradient =
            Brush.verticalGradient(
                colors =
                    listOf(
                        glassTint,
                        glassTint.copy(alpha = glassTint.alpha * 0.7f),
                        glassTint.copy(alpha = glassTint.alpha * 0.5f),
                        glassTint,
                    ),
            )

        return this
            // Apply glass gradient background
            .background(
                brush = glassGradient,
                shape = shape,
            )
            // Apply darkness overlay if needed
            .let { modifier ->
                if (darknessOverlay != Color.Transparent) {
                    modifier.background(
                        color = darknessOverlay,
                        shape = shape,
                    )
                } else {
                    modifier
                }
            }
            // Apply scale effect (limited simulation)
            .let { modifier ->
                if (scale > 0f) {
                    modifier.graphicsLayer {
                        scaleX = 1f + (scale * 0.1f)
                        scaleY = 1f + (scale * 0.1f)
                    }
                } else {
                    modifier
                }
            }
            // Apply transparency for warp edges effect
            .let { modifier ->
                if (warpEdges > 0f) {
                    modifier.alpha(1f - (warpEdges * 0.2f).coerceIn(0f, 0.8f))
                } else {
                    modifier
                }
            }
    }
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    // Check if AGSL is supported (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassContainerWithShader(modifier, content, glassContent)
    } else {
        GlassContainerFallback(modifier, content, glassContent)
    }
}

@SuppressLint("NewApi") // Version check is performed in GlassContainer
@Composable
private fun GlassContainerWithShader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeImpl(density) }

    val shader =
        remember(glassScope.updateCounter) {
            RuntimeShader(GLASS_DISPLACEMENT_SHADER)
        }

    SideEffect {
        glassScope.cleanupInactiveElements()
    }

    DisposableEffect(Unit) {
        onDispose {
            glassScope.elements.clear()
        }
    }
    Box(
        modifier =
            modifier
                .graphicsLayer {
                    shader.setFloatUniform("resolution", size.width, size.height)
                    val a = glassScope.updateCounter

                    val elements = glassScope.elements.also { println(it) }

                    val maxElements = 10
                    val positions = FloatArray(maxElements * 2)
                    val sizes = FloatArray(maxElements * 2)
                    val scales = FloatArray(maxElements)
                    val radii = FloatArray(maxElements)
                    val elevations = FloatArray(maxElements)
                    val centerDistortions = FloatArray(maxElements)
                    val tints = FloatArray(maxElements * 4)
                    val darkness = FloatArray(maxElements)
                    val warpEdges = FloatArray(maxElements)
                    val blurs = FloatArray(maxElements)

                    val elementsCount = minOf(elements.size, maxElements)
                    shader.setIntUniform("elementsCount", elementsCount)

                    for (i in 0 until elementsCount) {
                        val element = elements[i]
                        positions[i * 2] = element.position.x
                        positions[i * 2 + 1] = element.position.y
                        sizes[i * 2] = element.size.width
                        sizes[i * 2 + 1] = element.size.height
                        scales[i] = element.scale
                        radii[i] = element.cornerRadius
                        elevations[i] = element.elevation
                        centerDistortions[i] = element.centerDistortion

                        tints[i * 4] = element.tint.red
                        tints[i * 4 + 1] = element.tint.green
                        tints[i * 4 + 2] = element.tint.blue
                        tints[i * 4 + 3] = element.tint.alpha

                        darkness[i] = element.darkness
                        warpEdges[i] = element.warpEdges
                        blurs[i] = element.blur
                    }

                    shader.setFloatUniform("glassPositions", positions)
                    shader.setFloatUniform("glassSizes", sizes)
                    shader.setFloatUniform("glassScales", scales)
                    shader.setFloatUniform("cornerRadii", radii)
                    shader.setFloatUniform("elevations", elevations)
                    shader.setFloatUniform("centerDistortions", centerDistortions)
                    shader.setFloatUniform("glassTints", tints)
                    shader.setFloatUniform("glassDarkness", darkness)
                    shader.setFloatUniform("glassWarpEdges", warpEdges)
                    shader.setFloatUniform("glassBlurs", blurs)

                    renderEffect =
                        RenderEffect
                            .createRuntimeShaderEffect(
                                shader,
                                "contents",
                            ).asComposeRenderEffect()
                },
    ) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

@Composable
private fun GlassContainerFallback(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeFallbackImpl(density) }

    Box(modifier = modifier) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

@Language("AGSL")
private val GLASS_DISPLACEMENT_SHADER =
    """
    uniform float2 resolution;
    uniform shader contents;
    uniform int elementsCount;
    uniform float2 glassPositions[10];
    uniform float2 glassSizes[10];
    uniform float glassScales[10];
    uniform float cornerRadii[10];
    uniform float elevations[10];
    uniform float centerDistortions[10];
    uniform float glassTints[40]; // 10 elements * 4 components (r,g,b,a)
    uniform float glassDarkness[10];
    uniform float glassWarpEdges[10];
    uniform float glassBlurs[10];

    // Calculate signed distance field for rounded rectangle
    float sdfRoundedRect(float2 p, float2 halfSize, float radius) {
        float2 d = abs(p) - halfSize + radius;
        return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
    }

    // Check if pixel is in warp region (0.0 = inner, 1.0 = warp zone)
    float getWarpRegion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return 0.0;
        
        float outerSdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        if (outerSdf >= 0.0) return 0.0;
        
        // Calculate inner boundary
        float inset = warpEdges * min(halfSize.x, halfSize.y) * 0.5;
        float2 innerSize = max(halfSize - inset, 0.1);
        float innerRadius = max(cornerRadius * min(innerSize.x / halfSize.x, innerSize.y / halfSize.y), 0.0);
        
        float innerSdf = sdfRoundedRect(localCoord, innerSize, innerRadius);
        return step(0.0, innerSdf);
    }

    // Apply barrel distortion in warp regions
    float2 applyWarpDistortion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return localCoord;
        
        float inset = warpEdges * min(halfSize.x, halfSize.y) * 0.5;
        float2 innerSize = max(halfSize - inset, 0.1);
        float innerRadius = max(cornerRadius * min(innerSize.x / halfSize.x, innerSize.y / halfSize.y), 0.0);
        
        float innerSdf = sdfRoundedRect(localCoord, innerSize, innerRadius);
        if (innerSdf <= 0.0) return localCoord; // No distortion in inner region
        
        // Normalize distance for smooth distortion
        float normalizedDist = clamp(innerSdf / inset, 0.0, 1.0);
        float warpIntensity = normalizedDist * normalizedDist * warpEdges;
        
        // Apply barrel distortion
        float pullStrength = warpIntensity * 0.8;
        float targetScale = max(0.1, 1.0 - pullStrength);
        float2 pulledCoord = localCoord * targetScale;
        
        // Add radial distortion
        float2 centerDir = normalize(localCoord);
        float2 radialOffset = centerDir * (warpIntensity * 0.03 * length(localCoord));
        
        // Add swirl for strong warp
        if (warpEdges > 0.7 && normalizedDist > 0.8) {
            float angle = atan(localCoord.y, localCoord.x) + normalizedDist * warpEdges * 0.5;
            float r = length(pulledCoord);
            pulledCoord = float2(cos(angle), sin(angle)) * r;
        }
        
        return pulledCoord + radialOffset;
    }

    // Apply lens magnification effect
    float2 applyLensEffect(float2 fragCoord, float2 center, float2 size, float cornerRadius, float scale, float centerDistortion) {
        if (scale <= 0.0) return fragCoord;
        
        float2 localCoord = fragCoord - center;
        float2 halfSize = size * 0.5;
        
        float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        if (sdf >= 0.0) return fragCoord; // Outside lens
        
        // Calculate distortion based on distance from center
        float2 rel = localCoord / halfSize;
        float normalizedDist = length(rel) / 1.414; // Normalize to diagonal
        
        float baseScale = 1.0 + scale;
        float distortionFactor = 1.0;
        
        if (centerDistortion > 0.0) {
            float profile = 1.0 - smoothstep(0.0, 1.0, normalizedDist);
            distortionFactor = 1.0 + centerDistortion * profile;
        }
        
        float finalScale = baseScale * distortionFactor;
        return center + (fragCoord - center) / finalScale;
    }

    // Calculate shadow intensity
    float getShadowIntensity(float2 localCoord, float2 halfSize, float cornerRadius, float elevation) {
        if (elevation <= 0.0) return 0.0;
        
        float shadowOffset = elevation * 0.5;
        float shadowBlur = elevation * 2.0;
        
        float2 shadowCoord = localCoord - float2(0.0, shadowOffset);
        float shadowSdf = sdfRoundedRect(shadowCoord, halfSize, cornerRadius);
        float originalSdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        
        // Shadow only outside original element and within blur range
        if (originalSdf <= 0.0 || shadowSdf > shadowBlur) return 0.0;
        
        return (1.0 - shadowSdf / shadowBlur) * 0.15;
    }

    // Calculate rim highlight intensity
    float getRimHighlight(float2 localCoord, float2 halfSize, float cornerRadius) {
        float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        float rimWidth = 5.0;
        
        if (sdf <= 0.0 || sdf >= rimWidth) return 0.0;
        
        float intensity = (rimWidth - sdf) / rimWidth;
        float verticalPos = localCoord.y / halfSize.y;
        float lightingFactor = mix(1.2, 0.7, (verticalPos + 1.0) * 0.5);
        
        return intensity * 0.8 * lightingFactor;
    }

    float4 main(float2 fragCoord) {
        float2 finalCoord = fragCoord;
        float shadowAlpha = 0.0;
        float rimHighlight = 0.0;
        float4 tintColor = float4(0.0);
        float darknessEffect = 0.0;
        float blurRadius = 0.0;
        float2 surfaceNormal = float2(0.0);
        
        // Process each glass element
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            float2 center = glassPositions[i] + glassSizes[i] * 0.5;
            float2 localCoord = fragCoord - center;
            float2 halfSize = glassSizes[i] * 0.5;
            float cornerRadius = cornerRadii[i];
            
            float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
            
            // Apply blur inside element
            if (sdf < 0.0 && glassBlurs[i] > 0.0) {
                blurRadius = max(blurRadius, glassBlurs[i] * 20.0);
            }
            
            // Apply warp and lens effects
            float warpRegion = getWarpRegion(localCoord, halfSize, cornerRadius, glassWarpEdges[i]);
            if (warpRegion > 0.0) {
                float2 warpedCoord = applyWarpDistortion(localCoord, halfSize, cornerRadius, glassWarpEdges[i]);
                float2 warpedFragCoord = center + warpedCoord;
                finalCoord = applyLensEffect(warpedFragCoord, glassPositions[i] + glassSizes[i] * 0.5, 
                                           glassSizes[i], cornerRadius, glassScales[i], centerDistortions[i]);
            } else {
                finalCoord = applyLensEffect(finalCoord, center, glassSizes[i], cornerRadius, 
                                           glassScales[i], centerDistortions[i]);
            }
            
            // Accumulate effects
            shadowAlpha = max(shadowAlpha, getShadowIntensity(localCoord, halfSize, cornerRadius, elevations[i]));
            rimHighlight = max(rimHighlight, getRimHighlight(localCoord, halfSize, cornerRadius));
            
            // Store surface normal for rim highlight
            if (sdf > 0.0 && sdf < 4.0 && surfaceNormal.x == 0.0 && surfaceNormal.y == 0.0) {
                float epsilon = 1.0;
                float sdfX = sdfRoundedRect(localCoord + float2(epsilon, 0.0), halfSize, cornerRadius);
                float sdfY = sdfRoundedRect(localCoord + float2(0.0, epsilon), halfSize, cornerRadius);
                surfaceNormal = normalize(float2(sdfX - sdf, sdfY - sdf));
            }
            
            // Apply tint and darkness inside element
            if (sdf < 0.0) {
                float4 elementTint = float4(glassTints[i * 4], glassTints[i * 4 + 1], 
                                          glassTints[i * 4 + 2], glassTints[i * 4 + 3]);
                if (elementTint.a > 0.0) {
                    tintColor = mix(tintColor, elementTint, elementTint.a);
                }
                
                // Apply darkness from edges inward
                float currentDarkness = glassDarkness[i];
                if (currentDarkness > 0.0) {
                    float maxRadius = min(halfSize.x, halfSize.y) * 0.8;
                    float distanceFromEdge = abs(sdf);
                    if (distanceFromEdge < maxRadius) {
                        float intensity = smoothstep(0.0, 1.0, (maxRadius - distanceFromEdge) / maxRadius);
                        darknessEffect = max(darknessEffect, currentDarkness * intensity);
                    }
                }
            }
        }
        
        // Sample background
        float4 color = contents.eval(finalCoord);
        
        // Apply  blur
        if (blurRadius > 0.0) {
            float4 blurredColor = float4(0.0);
            float totalWeight = 0.0;
            float invRadius = 1.0 / max(blurRadius, 1.0);
            
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    float2 offset = float2(float(dx), float(dy)) * blurRadius * 0.4;
                    float distance = length(offset) * invRadius;
                    float weight = exp(-distance * distance * 2.0);
                    blurredColor += contents.eval(finalCoord + offset) * weight;
                    totalWeight += weight;
                }
            }
            color = blurredColor / totalWeight;
        }
        
        if (tintColor.a > 0.0) {
            color.rgb = mix(color.rgb, tintColor.rgb, tintColor.a * 0.9);
        }
        
        if (darknessEffect > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), darknessEffect * 0.5);
        }
        
        // Apply rim highlight with reflection
        if (rimHighlight > 0.0) {
            float2 reflectionOffset = surfaceNormal * 24.0;
            float4 reflectedColor = contents.eval(fragCoord + reflectionOffset);
            reflectedColor.rgb = max(reflectedColor.rgb * 1.8 + 0.35, 0.15);
            color = mix(color, reflectedColor, rimHighlight);
        }
        
        if (shadowAlpha > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), shadowAlpha);
        }
        
        return color;
    }
    """.trimIndent()
