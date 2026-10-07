package com.sameerasw.overcast.weather.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sameerasw.overcast.ui.features.weather.WeatherPalette
import com.sameerasw.overcast.ui.features.weather.skyState
import com.sameerasw.overcast.weather.effects.WeatherEffectSpec
import com.sameerasw.overcast.weather.effects.drawWeatherEffectsStill
import com.sameerasw.overcast.weather.model.WeatherSnapshot
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// The app's sky background (palette gradient plus the sun or moon glow), drawn for the widget.
object WidgetAmbientRenderer {
    private const val MAX_SIDE_PX = 600
    private const val REFERENCE_AREA_DP = 400f * 850f
    private const val EFFECT_STRENGTH = 1.7f
    private const val SUN = 0xFFFFE2A8.toInt()
    private const val MOON = 0xFFE6ECFF.toInt()

    fun render(
        snapshot: WeatherSnapshot?,
        now: Long,
        timeOverride: String?,
        spec: WeatherEffectSpec,
        widthPx: Int,
        heightPx: Int,
        cornerPx: Float,
        density: Float,
    ): Bitmap {
        val scale = min(1f, MAX_SIDE_PX.toFloat() / maxOf(widthPx, heightPx, 1))
        val w = (widthPx * scale).toInt().coerceAtLeast(1)
        val h = (heightPx * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = cornerPx * scale
        canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Path.Direction.CW) })

        val palette = WeatherPalette.from(snapshot, now, timeOverride)
        val sky = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(palette.glow.toArgb(), palette.glowSecondary.toArgb(), palette.base.toArgb(), palette.base.toArgb()),
                floatArrayOf(0f, 0.4f, 0.8f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sky)

        snapshot?.let { skyState(it, now) }?.let { (t, isSun) -> drawBody(canvas, w, h, t, isSun) }

        if (!spec.isEmpty) {
            val areaDp = (widthPx / density) * (heightPx / density)
            val areaScale = (areaDp / REFERENCE_AREA_DP).coerceIn(0.2f, 1f)
            CanvasDrawScope().draw(
                Density(density * scale),
                LayoutDirection.Ltr,
                ComposeCanvas(canvas),
                Size(w.toFloat(), h.toFloat()),
            ) {
                drawWeatherEffectsStill(spec, EFFECT_STRENGTH, areaScale)
            }
        }
        return bitmap
    }

    private fun drawBody(canvas: Canvas, w: Int, h: Int, t: Float, isSun: Boolean) {
        val horizonFade = (min(t, 1f - t) / 0.1f).coerceIn(0f, 1f)
        val alpha = 0.5f * horizonFade
        if (alpha <= 0f) return
        val x = w * (0.9f - 0.8f * t)
        val y = h * (0.66f - 0.4f * sin(PI.toFloat() * t))
        val color = if (isSun) SUN else MOON
        val glowRadius = min(w, h) * 0.55f
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x, y, glowRadius, withAlpha(color, alpha * 0.55f), withAlpha(color, 0f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(x, y, glowRadius, glow)
        val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = withAlpha(color, alpha * 0.7f) }
        canvas.drawCircle(x, y, min(w, h) * (if (isSun) 0.09f else 0.07f), core)
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24)
}
